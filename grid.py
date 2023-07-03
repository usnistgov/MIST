import argparse
import os
import re
import numpy as np
import skimage.io
import logging

# local imports
import tile




class TileGrid():
    timePattern = "(.*)(\\{[t]+\\})(.*)"

    def __init__(self, args: argparse.Namespace):
        self.args = args
        self.img_height = None
        self.img_width = None

        # init a 2d list to hold Tiles
        self.tiles = [[None for _ in range(self.args.grid_width)] for _ in range(self.args.grid_height)]

    def get_tile(self, r: int, c: int) -> tile.Tile:
        if r >= 0 and c >= 0 and r < self.args.grid_height and c < self.args.grid_width:
            return self.tiles[r][c]
        else:
            # tile request is invalid
            return None

    def get_image_shape(self) -> tuple[int, int]:
        if self.img_height is None or self.img_width is None:
            for r in range(self.args.grid_height):
                for c in range(self.args.grid_width):
                    t = self.get_tile(r, c)
                    if t is not None:
                        if t.exists():  # if the file exists
                            s = t.get_image().shape
                            self.img_height = s[0]
                            self.img_width = s[1]

        return self.img_height, self.img_width


    def print_names(self):
        str = "Tile grid:\n"
        for r in range(self.args.grid_height):
            for c in range(self.args.grid_width):
                if self.tiles[r][c] is None:
                    str += "None\t"
                else:
                    str += self.tiles[r][c].name + "\t"
            str += "\n"
        logging.info(str)

    def print_peaks(self, dir: str, key: str):
        assert dir in ['north', 'west']
        assert key in ['ncc', 'x', 'y']

        str = "{} {} matrix:\n".format(dir, key)
        for r in range(self.args.grid_height):
            for c in range(self.args.grid_width):
                tile = self.get_tile(r, c)
                if tile is None:
                    str += "None\t"
                    continue

                t = tile.west_translation if dir == 'west' else tile.north_translation
                if t is not None:
                    str += "{:0.2f}\t".format(getattr(t, key))
                else:
                    str += "None\t"
            str += "\n"
        logging.info(str)

    def write_to_file(self, output_filepath: str):
        with open(output_filepath, 'w') as f:
            for r in range(self.args.grid_height):
                for c in range(self.args.grid_width):
                    tile = self.get_tile(r, c)
                    if tile is None:
                        continue
                    west = self.get_tile(r, c - 1)
                    north = self.get_tile(r - 1, c)

                    if west is not None:
                        t = tile.west_translation
                        if t is not None:
                            f.write("west, {}, {}, {}, {}, {}\n".format(tile.name, west.name, t.ncc, t.x, t.y))

                    if north is not None:
                        t = tile.north_translation
                        if t is not None:
                            f.write("north, {}, {}, {}, {}, {}\n".format(tile.name, north.name, t.ncc, t.x, t.y))


class TileGridRowCol(TileGrid):
    colPattern = "(.*)(\\{[c]+\\})(.*)"
    rowPattern = "(.*)(\\{[r]+\\})(.*)"

    def __init__(self, args: argparse.Namespace):
        super().__init__(args)

        if self.args.grid_origin == 'UL':
            start_row = 0
            start_col = 0
            row_incrementer = 1
            col_incrementer = 1
        elif self.args.grid_origin == 'UR':
            start_row = 0
            start_col = self.args.grid_width - 1
            row_incrementer = 1
            col_incrementer = -1
        elif self.args.grid_origin == 'LL':
            start_row = self.args.grid_height - 1
            start_col = 0
            row_incrementer = -1
            col_incrementer = 1
        elif self.args.grid_origin == 'LR':
            start_row = self.args.grid_height - 1
            start_col = self.args.grid_width - 1
            row_incrementer = -1
            col_incrementer = -1
        else:
            raise RuntimeError("Unknown grid origin: {}".format(self.args.grid_origin))

        filename_pattern = self.args.filename_pattern
        time_matcher = re.compile(self.timePattern)
        time_match = time_matcher.match(filename_pattern)
        if time_match is not None:
            if self.args.time_slice is None:
                raise RuntimeError("Filename pattern has a time component \"{t+}\", so a time slice is required.")
            fmt_str = "{:0" + str(len(time_match.group(2)) - 2) + "d}"
            filename_pattern = "{}{}{}".format(time_match.group(1), fmt_str.format(self.args.time_slice), time_match.group(3))

        row_matcher = re.compile(self.rowPattern)
        col_matcher = re.compile(self.colPattern)

        grid_row = start_row
        for row in range(0, self.args.grid_height):
            fn = filename_pattern
            row_match = row_matcher.match(fn)
            fmt_str = "{:0" + str(len(row_match.group(2)) - 2) + "d}"
            fn_row = "{}{}{}".format(row_match.group(1), fmt_str.format(row + self.args.start_row), row_match.group(3))

            grid_col = start_col
            for col in range(0, self.args.grid_width):
                col_match = col_matcher.match(fn_row)
                fmt_str = "{:0" + str(len(col_match.group(2)) - 2) + "d}"
                fn = "{}{}{}".format(col_match.group(1), fmt_str.format(col + self.args.start_col), col_match.group(3))

                t = tile.Tile(grid_row, grid_col, os.path.join(self.args.image_dirpath, fn), disable_cache=self.args.disable_mem_cache)
                self.tiles[grid_row][grid_col] = t
                grid_col += col_incrementer
            grid_row += row_incrementer


class TileGridSequential(TileGrid):

    positionPattern = "(.*)(\\{[p]+\\})(.*)"

    def __init__(self, args: argparse.Namespace):
        super().__init__(args)

        raise NotImplementedError()