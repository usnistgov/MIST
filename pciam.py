import argparse

import numpy as np
import scipy.fft
import time
import logging
from abc import ABC

# local imports
import tile
import grid


class PCIAM(ABC):

    @staticmethod
    def extract_subregion(tile: np.ndarray, x: int, y: int) -> np.ndarray:
        """
        Extracts the sub-region visible if the image view window is translated the given (x,y) distance).
        :param tile: The image tile a sub-region is being extracted from. The translation (x,y) is relative to the upper left corner of this image.
        :param x: the x component of the translation.
        :param y: the y component of the translation.
        :return: the portion of tile shown if the view is translated (x,y) pixels.
        """
        w = tile.shape[1]
        h = tile.shape[0]

        x_start = x
        x_end = x + w - 1
        y_start = y
        y_end = y + h - 1

        # constrain to valid coordinates
        x_start = np.clip(x_start, 0, w - 1)
        x_end = np.clip(x_end, 0, w - 1)
        y_start = np.clip(y_start, 0, h - 1)
        y_end = np.clip(y_end, 0, h - 1)

        # if the translations (x,y) would leave no overlap between the images, return None
        if abs(x) >= w or abs(y) >= h:
            return None

        sub_tile = tile[y_start:y_end + 1, x_start:x_end + 1]
        return sub_tile

    @staticmethod
    def cross_correlation(a1: np.ndarray, a2: np.ndarray) -> float:
        """
        Computes the cross correlation between two arrays.
        :param a1: the first array
        :param a2: the second array
        :return: the normalized cross correlation between the two arrays.
        """
        a1 = a1.ravel().astype(np.float32)
        a2 = a2.ravel().astype(np.float32)

        a1 -= np.mean(a1)
        a2 -= np.mean(a2)

        neumerator = np.matmul(a1.transpose(), a2)
        denominator = np.sqrt(np.matmul(a1.transpose(), a1) * np.matmul(a2.transpose(), a2))
        cr = neumerator / denominator
        if not np.isfinite(cr):
            cr = -1

        return cr

    @staticmethod
    def compute_cross_correlation(t1: np.ndarray, t2: np.ndarray, x: int, y: int) -> float:
        """
        Computes the cross correlation between two ImageTiles given the offset (x,y) from the first to the second.
        :param t1: the first tile
        :param t2: the second tile
        :param x: the x component of the translation from i1 to i2.
        :param y: the y component of the translation from i1 to i2.
        :return: the normalized cross correlation between the overlapping pixels given the translation between t1 and t2 (x,y).
        """
        a1 = PCIAM.extract_subregion(t1, x, y)
        if a1 is None:
            return -1.0
        a2 = PCIAM.extract_subregion(t2, -x, -y)
        if a2 is None:
            return -1.0
        return PCIAM.cross_correlation(a1, a2)

    @staticmethod
    def peak_cross_correlation_worker(t1: np.ndarray, t2: np.ndarray, dims: list[tuple[int, int]]) -> tile.Peak:
        # remove duplicate dim values to prevent redundant computation
        dims = list(set(dims))

        ncc_list = list()
        x_list = list()
        y_list = list()
        for i in range(len(dims)):
            nr = dims[i][0]
            nc = dims[i][1]

            peak = PCIAM.compute_cross_correlation(t1, t2, nc, nr)
            if np.isnan(peak):
                peak = -1
            ncc_list.append(peak)
            x_list.append(nc)
            y_list.append(nr)

        idx = np.argmax(ncc_list)
        peak = tile.Peak(ncc_list[idx], x_list[idx], y_list[idx])

        return peak

    @staticmethod
    def peak_cross_correlation_lr(t1: np.ndarray, t2: np.ndarray, x: int, y: int) -> tile.Peak:
        """
        Computes the peak cross correlation between two images t1 and t2, where t1 is the left image
        """

        w = t1.shape[1]
        h = t1.shape[0]

        # a given correlation triple between two images can have multiple interpretations
        # In the general case the translation from t1 to t2 can be any (x,y) so long as the two
        # images overlap. Therefore, given an input (x,y) where x and y are positive by definition
        # of the translation, we need to check 16 possible translations to find the correct
        # interpretation of the translation offset magnitude (x,y). The general case of 16
        # translations arise from the four Fourier transform possibilities, [(x, y); (x, H-y); (W-x,
        # y); (W-x,H-y)] and the four direction possibilities (+-x, +-y) = [(x,y); (x,-y); (-x,y);
        # (-x,-y)].
        # Because we know t1 and t2 form a left right pair, we can limit this search to the 8
        # possible combinations by only considering (x,+-y).
        dims = [(y, x), (y, w - x), (h - y, x), (h - y, w - x),
                ((-y), x), ((-y), w - x), (-(h - y), x), (-(h - y), w - x)]

        return PCIAM.peak_cross_correlation_worker(t1, t2, dims)

    @staticmethod
    def peak_cross_correlation_ud(t1: np.ndarray, t2: np.ndarray, x: int, y: int) -> tile.Peak:
        """
        Computes the peak cross correlation between two images t1 and t2, where t1 is the top image
        """
        w = t1.shape[1]
        h = t1.shape[0]

        # a given correlation triple between two images can have multiple interpretations
        # In the general case the translation from t1 to t2 can be any (x,y) so long as the two
        # images overlap. Therefore, given an input (x,y) where x and y are positive by definition
        # of the translation, we need to check 16 possible translations to find the correct
        # interpretation of the translation offset magnitude (x,y). The general case of 16
        # translations arise from the four Fourier transform possibilities, [(x, y); (x, H-y); (W-x,
        # y); (W-x,H-y)] and the four direction possibilities (+-x, +-y) = [(x,y); (x,-y); (-x,y);
        # (-x,-y)].
        # Because we know t1 and t2 form an up down pair, we can limit this search to the 8
        # possible combinations by only considering (+-x,y).
        dims = [(y, x), (y, w - x), (h - y, x), (h - y, w - x),
                (y, (-x)), (y, -(w - x)), (h - y, (-x)), (h - y, -(w - x))]

        return PCIAM.peak_cross_correlation_worker(t1, t2, dims)

    @staticmethod
    def compute_pciam(t1: tile.Tile, t2: tile.Tile, n_peaks: int) -> tile.Peak:
        t1_img = t1.get_image()
        t2_img = t2.get_image()

        # fc = np.fft.fft2(t1_img.astype(np.float32)) * np.conj(np.fft.fft2(t2_img.astype(np.float32)))
        # fc = np.clip(fc, a_min=1e-16, a_max=None)
        # fc = np.nan_to_num(fc, nan=1e-16, copy=False)  # replace nans with min value
        # fcn = fc / np.abs(fc)
        # pcm = np.real(np.fft.ifft2(fcn))

        # use scipy over np to do fft in 32bit (for complex64 result)
        fc = scipy.fft.fft2(t1_img.astype(np.float32)) * np.conj(scipy.fft.fft2(t2_img.astype(np.float32)))
        fc = np.clip(fc, a_min=1e-16, a_max=None)
        fc = np.nan_to_num(fc, nan=1e-16, copy=False)  # replace nans with min value
        fcn = fc / np.abs(fc)
        pcm = np.real(scipy.fft.ifft2(fcn))

        # get the n_peaks largest values using argpartition to avoid sort
        indices = pcm.argpartition(pcm.size - n_peaks, axis=None)[-n_peaks:]
        # y, x = np.unravel_index(indices, pcm.shape)
        # peak_vals = pcm[y, x]  # if you want the actual peak values

        peak_list = list()

        for ind in indices:
            y, x = np.unravel_index(ind, pcm.shape)
            if t1.r == t2.r:
                # same row, so compute NCC along Left-Right
                peak = PCIAM.peak_cross_correlation_lr(t1_img, t2_img, x, y)
            else:
                # different row, so compute NCC along Up-Down
                peak = PCIAM.peak_cross_correlation_ud(t1_img, t2_img, x, y)
            peak_list.append(peak)

        peak = max(peak_list, key=lambda p: p.ncc)
        return peak


class SequentialPciam(PCIAM):
    def __init__(self, args: argparse.Namespace):
        self.args = args

    def execute(self, tile_grid: grid.TileGrid):
        start_time = time.time()
        # iterate over the rows and columns of the grid
        for r in range(self.args.grid_height):
            for c in range(self.args.grid_width):
                tile = tile_grid.get_tile(r, c)
                if tile is None:
                    continue

                west = tile_grid.get_tile(r, c - 1)
                if west is not None:
                    peak = self.compute_pciam(west, tile, self.args.num_fft_peaks)
                    tile.west_translation = peak

                north = tile_grid.get_tile(r - 1, c)
                if north is not None:
                    peak = self.compute_pciam(north, tile, self.args.num_fft_peaks)
                    tile.north_translation = peak

        elapsed_time = time.time() - start_time
        logging.info("Finished computing all pairwise translations in {} seconds".format(elapsed_time))
