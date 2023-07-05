import os
import numpy as np
import skimage.io
from dataclasses import dataclass
import typing

@dataclass
class Peak:
    ncc: float
    x: int
    y: int

    def __repr__(self):
        return f"Peak(ncc={self.ncc}, x={self.x}, y={self.y})"


class Tile():
    def __init__(self, r, c, filepath, disable_cache=False):
        self.r = r
        self.c = c
        self.filepath = filepath
        self.name = os.path.basename(filepath)
        self.data = None
        self.disable_cache = disable_cache

        self.west_translation = None
        self.north_translation = None

        self.abs_x = 0
        self.abs_y = 0

    def get_image(self) -> np.ndarray:
        if not self.disable_cache:
            if self.data is not None:
                return self.data
            else:
                if self.exists():
                    self.data = skimage.io.imread(self.filepath)
                return self.data
        if self.exists():
            return skimage.io.imread(self.filepath)

    def exists(self):
        return os.path.exists(self.filepath)

    def get_translation(self, direction: str) -> Peak:
        assert direction in ['HORIZONTAL', 'VERTICAL']
        return self.west_translation if direction == 'HORIZONTAL' else self.north_translation

    def get_max_translation_ncc(self) -> float:
        wt = self.west_translation
        nt = self.north_translation
        if wt is not None and nt is not None:
            max_ncc = max(wt.ncc, nt.ncc)
        elif wt is not None:
            max_ncc = wt.ncc
        elif nt is not None:
            max_ncc = nt.ncc
        else:
            return np.nan
        return max_ncc

    def north_of(self, other: typing.Self) -> bool:
        if other is None:
            raise RuntimeError("Other Tile is None")
        return self.r + 1 == other.r and self.c == other.c

    def south_of(self, other: typing.Self) -> bool:
        if other is None:
            raise RuntimeError("Other Tile is None")
        return self.r - 1 == other.r and self.c == other.c

    def east_of(self, other: typing.Self) -> bool:
        if other is None:
            raise RuntimeError("Other Tile is None")
        return self.r == other.r and self.c - 1 == other.c

    def west_of(self, other: typing.Self) -> bool:
        if other is None:
            raise RuntimeError("Other Tile is None")
        return self.r == other.r and self.c + 1 == other.c

    def get_peak(self, other: typing.Self) -> Peak:
        """
        Gets the correlation associated with the neighbor image tile
        """
        if self.north_of(other):
            return other.north_translation

        if self.south_of(other):
            return self.north_translation

        if self.east_of(other):
            return self.west_translation

        if self.west_of(other):
            return other.west_translation

        raise RuntimeError("Tile {} and {} are not adjacent".format(self.name, other.name))

    def update_absolute_position(self, other: typing.Self):
        """
        Updates the absolute position of this tile relative to another tile
        """
        if other is None:
            raise RuntimeError("Other Tile is None")

        x = other.abs_x
        y = other.abs_y

        if self.north_of(other):
            peak = other.north_translation
            self.abs_x = x - peak.x
            self.abs_y = y - peak.y

        if self.south_of(other):
            peak = self.north_translation
            self.abs_x = x + peak.x
            self.abs_y = y + peak.y

        if self.west_of(other):
            peak = other.west_translation
            self.abs_x = x - peak.x
            self.abs_y = y - peak.y

        if self.east_of(other):
            peak = self.west_translation
            self.abs_x = x + peak.x
            self.abs_y = y + peak.y