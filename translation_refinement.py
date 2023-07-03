import argparse
import os
import numpy as np
import logging
import time
from abc import ABC
import copy
import enum

# local imports
import grid
import tile
import mle
import stage_model
import pciam


class HillClimbDirection(enum.Enum):
    """
    Defines hill climbing direction using cartesian coordinates when observing a two dimensional
    grid where the upper left corner is 0,0. Moving north -1 in the y-direction, south +1 in the
    y-direction, west -1 in the x-direction, and east +1 in the x-direction.
    """
    NORTH = (0, -1)
    SOUTH = (0, 1)
    EAST = (1, 0)
    WEST = (-1, 0)
    NoMove = (0, 0)

    def __init__(self, x: int, y: int):
        self.x = x
        self.y = y

class Refine(ABC):
    @staticmethod
    def hill_climb_worker(i1: np.ndarray, i2: np.ndarray, x_min: int, x_max: int, y_min: int, y_max: int, start_x: int, start_y: int, cache: np.ndarray) -> tile.Peak:
        """
        Computes cross correlation search with hill climbing
        :param i1: image 1 (ego)
        :param i2: image 1 (north or west neigbor)
        :param x_min: min x boundary
        :param x_max: max x boundary
        :param y_min: min y boundary
        :param y_max: max y boundary
        :param start_x: start x position for the hill climb
        :param start_y: start y position for the hill climb
        :param cache: 2d array of np.float32 storing the ncc values for each x,y
        :return:
        """

        best_peak = tile.Peak(ncc=np.nan, x=start_x, y=start_y)

        # walk hill climb until we reach a top
        while True:
            cur_direction = HillClimbDirection.NoMove

            # translate to 0-based coordinates
            cur_x_idx = best_peak.x - x_min
            cur_y_idx = best_peak.y - y_min

            # check the current location
            best_peak.ncc = cache[cur_y_idx, cur_x_idx]
            if np.isnan(best_peak.ncc):
                best_peak.ncc = pciam.PCIAM.compute_cross_correlation(i1, i2, best_peak.x, best_peak.y)
                cache[cur_y_idx, cur_x_idx] = best_peak.ncc

            # Check each direction and move based on highest correlation
            for d in HillClimbDirection._member_names_:
                dir = HillClimbDirection[d]
                if dir == HillClimbDirection.NoMove:
                    continue

                # Check if moving dir is in bounds
                new_x = best_peak.x + dir.x
                new_y = best_peak.y + dir.y
                if new_y >= y_min and new_y <= y_max and new_x >= x_min and new_x <= x_max:
                    # Check if we have already computed the peak at dir
                    ncc = cache[cur_y_idx + dir.y, cur_x_idx + dir.x]
                    if np.isnan(ncc):
                        ncc = pciam.PCIAM.compute_cross_correlation(i1, i2, new_x, new_y)
                        cache[cur_y_idx + dir.y, cur_x_idx + dir.x] = ncc
                    if ncc > best_peak.ncc:
                        best_peak.ncc = ncc
                        best_peak.x = new_x
                        best_peak.y = new_y
                        cur_direction = dir

            if cur_direction == HillClimbDirection.NoMove:
                # if the direction was a NoMove, then we are done
                break

        if np.isnan(best_peak.ncc):
            best_peak.x = int((x_max + x_min) / 2)
            best_peak.y = int((y_max + y_min) / 2)
            best_peak.ncc = -1.0
        return best_peak




class RefineSequential(Refine):
    def __init__(self, args: argparse.Namespace, tile_grid: grid.TileGrid, stage_model: stage_model.StageModel):
        self.args = args
        self.tile_grid = tile_grid
        self.stage_model = stage_model


    @staticmethod
    def multipoint_hill_climb(n: int, t1: tile.Tile, t2: tile.Tile, x_min: int, x_max: int, y_min: int, y_max: int, start_x: int, start_y: int) -> tile.Peak:
        i1 = t1.get_image()
        i2 = t2.get_image()
        img_shape = i1.shape
        height = img_shape[0]
        width = img_shape[1]

        # clamp bounds to valid range
        x_min = np.clip(x_min, -(width - 1), width - 1)
        x_max = np.clip(x_max, -(width - 1), width - 1)
        y_min = np.clip(y_min, -(height - 1), height - 1)
        y_max = np.clip(y_max, -(height - 1), height - 1)

        # create array of peaks +1 for inclusive
        cache = np.nan * np.ones((x_max - x_min + 1, y_max - y_min + 1), dtype=np.float32)

        peak_results = list()
        # evaluate the starting point hill climb
        peak = Refine.hill_climb_worker(i1, i2, x_min, x_max, y_min, y_max, start_x, start_y, cache)
        peak_results.append(peak)

        # perform the random starting point multipoint hill climbing
        for i in range(n - 1):
            # generate random starting point
            start_x = np.random.randint(x_min, x_max + 1)
            start_y = np.random.randint(y_min, y_max + 1)

            peak = Refine.hill_climb_worker(i1, i2, x_min, x_max, y_min, y_max, start_x, start_y, cache)
            peak_results.append(peak)

        # find the best correlation and translation from the hill climb ending points
        best_index = np.argmax([peak.ncc for peak in peak_results])
        best_peak = peak_results[best_index]

        # determine how many converged
        converged = np.sum([1 for peak in peak_results if peak.x == best_peak.x and peak.y == best_peak.y])
        logging.info("Translation Hill Climb ({}, ({}) had {}/{} hill climbs converge with best ncc = {}".format(t1.name, t2.name, converged, n, best_peak.ncc))
        return best_peak


    def optimize_direction(self, tile: tile.Tile, other: tile.Tile, direction: str):
        assert direction in ['west', 'north']
        relevant_translation = tile.west_translation if direction == 'west' else tile.north_translation
        x_min = relevant_translation.x - self.stage_model.repeatability
        x_max = relevant_translation.x + self.stage_model.repeatability
        y_min = relevant_translation.y - self.stage_model.repeatability
        y_max = relevant_translation.y + self.stage_model.repeatability

        orig_peak = copy.deepcopy(relevant_translation)
        if self.args.translation_refinement_method == 'SINGLEHILLCLIMB':
            new_peak = self.multipoint_hill_climb(1, other, tile, x_min, x_max, y_min, y_max, orig_peak.x, orig_peak.y)
        elif self.args.translation_refinement_method == 'MULTIPOINTHILLCLIMB':
            new_peak = self.multipoint_hill_climb(self.args.num_hill_climbs, other, tile, x_min, x_max, y_min, y_max, orig_peak.x, orig_peak.y)
        else:
            raise RuntimeError("Unknown translation refinement method: {}".format(self.args.translation_refinement_method))

        if direction == 'west':
            tile.west_translation = new_peak
        else:
            tile.north_translation = new_peak

        if not np.isnan(orig_peak.ncc):
            # If the old correlation was a number, then it was a good translation.
            # Increment the new translation by the value of the old correlation to increase beyond 1
            # This will enable these tiles to have higher priority in minimum spanning tree search
            tile.west_translation.ncc += 3.0

    def execute(self):
        # TODO this translation refinement needs parallelization

        # iterate over the tile grid

        for r in range(self.args.grid_height):
            for c in range(self.args.grid_width):
                tile = self.tile_grid.get_tile(r, c)
                if tile is None:
                    continue
                west = self.tile_grid.get_tile(r, c - 1)
                if west is None:
                    continue

                # optimize with west neighbor
                self.optimize_direction(tile, west, 'west')

                north = self.tile_grid.get_tile(r - 1, c)
                if north is None:
                    continue

                # optimize with west neighbor
                self.optimize_direction(tile, north, 'north')

