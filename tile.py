import os
import numpy as np
import skimage.io
from dataclasses import dataclass


# class Peak():
#     def __init__(self, ncc: float, x: int, y:int):
#         self.ncc = ncc
#         self.x = x
#         self.y = y

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

        self.west_translation_pre_optimization = None
        self.north_translation_pre_optimization = None

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


