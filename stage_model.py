import argparse
import os
import numpy as np
import logging
import time

# local imports
import grid
import translation_refinement
import utils
import mle
import tile



class StageModel():
    NUMBER_STABLE_MLE_ITERATIONS = 20

    def __init__(self, args: argparse.Namespace, tile_grid: grid.TileGrid):
        self.args = args
        self.tile_grid = tile_grid

        self.horizontal_overlap: float = None
        self.vertical_overlap: float = None

        self.stats = dict()

        self.horizontal_repeatability: int = None
        self.vertical_repeatability: int = None
        self.repeatability: int = None

        self.valid_translations_vertical: set[tile.Tile] = None
        self.valid_translations_horizontal: set[tile.Tile] = None

        self.missing_rows = None
        self.missing_cols = None

    def get_translations(self, direction: str):
        assert direction in ['VERTICAL', 'HORIZONTAL']
        img_shape = self.tile_grid.get_image_shape()
        h_or_w = img_shape[0] if direction == 'VERTICAL' else img_shape[1]
        translations = list()

        for r in range(self.args.grid_height):
            for c in range(self.args.grid_width):
                tile = self.tile_grid.get_tile(r, c)
                if tile is None:
                    continue

                t = tile.get_translation(direction)
                if t is None:
                    continue
                val = t.x if direction == 'HORIZONTAL' else t.y
                if val > 1 and val < h_or_w - 1:
                    translations.append(val)

        return translations

    def compute_overlap(self, direction: str):
        assert direction in ['VERTICAL', 'HORIZONTAL']
        translations = self.get_translations(direction)
        if len(translations) == 0:
            raise RuntimeError("No translations found in direction: {}".format(direction))

        img_shape = self.tile_grid.get_image_shape()
        h_or_w = img_shape[0] if direction == 'VERTICAL' else img_shape[1]
        # convert translations into a percentage [0, 100] of the range
        translations = [100.0 * float(t) / h_or_w for t in translations]
        translations = np.asarray(translations, dtype=np.float32)

        # setup cache for mle likelihoods
        self.cache = mle.MleLikelihoodCache()
        num_stable_iterations = 0

        best_points = list()
        best_point = mle.MlePoint()
        while num_stable_iterations < self.NUMBER_STABLE_MLE_ITERATIONS:
            # create the current starting point
            point = mle.MlePoint.getRandomPoint()

            # perform hill climb search for that point
            point = mle.hillClimbSearch(point, self.cache, translations)
            best_points.append(point)
            # check if the new point is better than the best point
            if point.likelihood > best_point.likelihood:
                # new best point found, resetting the number of stable iterations
                best_point = point
                num_stable_iterations = 0
            else:
                # increment the number of stable iterations (where the optimally found answer has not changed)
                num_stable_iterations += 1

        # determine the number of converged hill climbs
        num_converged = sum([1 for p in best_points if p.mu == best_point.mu and p.sigma == best_point.sigma])
        logging.info("{}/{} hill climbs converged".format(num_converged, len(best_points)))

        # set overlap
        overlap = 100.0 - best_point.mu
        logging.info("MLE model {} parameters: mu={}%, sigma={}%".format(direction, best_point.mu, best_point.sigma))
        logging.info("Overlap ({}): {}".format(direction, overlap))

        return overlap

    def filer_translations_remove_outliers(self, direction: str, valid_tiles: set[tile.Tile]) -> set[tile.Tile]:
        assert direction in ['VERTICAL', 'HORIZONTAL']

        # only filter if there are more than 3 translations
        if len(valid_tiles) < 3:
            return valid_tiles

        # filter the translations to remove outliers
        # compute the statistics required to determine which translations are outliers
        # q1 is first quartile
        # q2 is second quartile (median)
        # q3 is third quartile
        # filter based on (>q3 + w(q3-q1)) and (<q1 - w(q3-q1))
        t_vals = [t.west_translation.x if direction == 'HORIZONTAL' else t.north_translation.y for t in valid_tiles]
        percs = np.percentile(t_vals, [25, 75])
        q1 = percs[0]
        q3 = percs[1]
        iqr = np.abs(q3 - q1)
        w = 1.5  # default statistical outlier w (1.5)
        # keep only those tiles within the interquartile range
        idx = np.logical_and(t_vals >= (q1 - w * iqr), t_vals <= (q3 + w * iqr))
        valid_tiles = set([t for t, i in zip(valid_tiles, idx) if i])
        return valid_tiles

    def filter_translations(self, direction) -> set[tile.Tile]:
        """
        Filters grid of image tiles based on calculated overlap, correlation, and standard deviation. A set of valid image tiles after filtering is returned. This modifies the tile_grid translation values.
        :return: list of valid image tiles
        """

        # filter the image tiles by overlap (using percent overlap uncertainty) and correlation
        img_shape = self.tile_grid.get_image_shape()
        height = img_shape[0]
        width = img_shape[1]

        if direction == 'VERTICAL':
            overlap = self.vertical_overlap
            t_min = height - (overlap + self.args.overlap_uncertainty) * height / 100.0
            t_max = height - (overlap - self.args.overlap_uncertainty) * height / 100.0
            overlap_error = self.args.overlap_uncertainty * height / 100.0
        else:
            overlap = self.horizontal_overlap
            t_min = width - (overlap + self.args.overlap_uncertainty) * width / 100.0
            t_max = width - (overlap - self.args.overlap_uncertainty) * width / 100.0
            overlap_error = self.args.overlap_uncertainty * width / 100.0

        self.stats['{}_min_filter_threshold'.format(direction.lower())] = t_min
        self.stats['{}_max_filter_threshold'.format(direction.lower())] = t_max
        logging.info("{} translation filter min={:0.2f}, max={:0.2f}".format(direction, t_min, t_max))
        # Filter based on t_min, t_max, and minCorrelation, and orthogonal direction

        valid_tiles = set()
        for r in range(self.args.grid_height):
            for c in range(self.args.grid_width):
                tile = self.tile_grid.get_tile(r, c)
                if tile is None:
                    continue

                t = tile.get_translation(direction)
                if t is None:
                    continue
                if t.ncc < self.args.valid_correlation_threshold:
                    # correlation is below valid threshold
                    continue

                if direction == 'VERTICAL':
                    # limit the valid translations to those within the t_min to t_max range
                    if t.y < t_min or t.y > t_max:
                        continue
                    # limit the valid translations to within percent overlap error of 0 on the orthogonal direction
                    if t.x < -overlap_error or t.x > overlap_error:
                        continue
                else:
                    if t.x < t_min or t.x > t_max:
                        continue
                    # limit the valid translations to within percent overlap error of 0 on the orthogonal direction
                    if t.y < -overlap_error or t.y > overlap_error:
                        continue

                valid_tiles.add(tile)
        valid_tiles = self.filer_translations_remove_outliers(direction, valid_tiles)
        return valid_tiles

    def compute_repeatability(self, direction: str):
        assert direction in ['VERTICAL', 'HORIZONTAL']

        translations = self.get_translations(direction)
        if len(translations) == 0:
            raise RuntimeError("No translations found in direction: {}".format(direction))

        # Filter the translations of a given direction using the percent overlap uncertainty and the correlation
        valid_tiles = self.filter_translations(direction)
        if direction == 'VERTICAL':
            self.vertical_valid_tiles = valid_tiles
        else:
            self.horizontal_valid_tiles = valid_tiles

        if len(valid_tiles) == 0:
            # if no valid translations have been found
            logging.warning("No good translations found for direction: {}. Estimated translations generated from the overlap.".format(direction))
            if self.args.stage_repeatability is not None:
                logging.warning("No good translations found for direction: {}. Repeatability has been set to {} (advanced options value).".format(direction, self.args.stage_repeatability))
            else:
                logging.warning("No good translations found for direction: {}. Repeatability has been set to 0. Please define a valid stage repeatability if possible.".format(direction))

            stage_repeatability = 0
        else:
            # the valid translations list was not empty
            logging.info("Computing min/max combinations using {} valid translations".format(len(valid_tiles)))
            logging.info("Computing Repeatability for direction: {}".format(direction))

            # Compute the repeatability as: ceil( (max - min) / 2.0) on the orthogonal translation direction
            # i.e. for North translations, look at the x coordinate
            t_orthogonal_vals = [t.west_translation.y if direction == 'HORIZONTAL' else t.north_translation.x for t in valid_tiles]
            repeatability1 = np.ceil((np.max(t_orthogonal_vals) - np.min(t_orthogonal_vals)) / 2.0)
            logging.info("Computed {} Repeatability over all translations = {}".format(direction, repeatability1))

            min_t_list = list()
            max_t_list = list()
            if direction == 'HORIZONTAL':
                # Compute the repeatability column-wise for the primary translation direction
                c_vals = [t.c for t in valid_tiles]
                for c in c_vals:
                    t_vals = [t.west_translation.x for t in valid_tiles if t.c == c]
                    min_t_list.append(np.min(t_vals))
                    max_t_list.append(np.max(t_vals))
            else:
                # Compute the repeatability column-wise for the primary translation direction
                r_vals = [t.r for t in valid_tiles]
                for r in r_vals:
                    t_vals = [t.north_translation.y for t in valid_tiles if t.r == r]
                    min_t_list.append(np.min(t_vals))
                    max_t_list.append(np.max(t_vals))
            repeatability2 = np.abs(np.asarray(max_t_list) - np.asarray(min_t_list)) / 2.0
            repeatability2 = np.ceil(np.max(repeatability2))
            logging.info("Computed {} Repeatability as max per {} = {}".format(direction, 'row' if direction == 'VERTICAL' else 'col', repeatability2))

            stage_repeatability = np.max([repeatability1, repeatability2])
            logging.info("Computed {} Repeatability: {} = max({}, {})".format(direction, stage_repeatability, repeatability1, repeatability2))
            if self.args.stage_repeatability is not None:
                logging.info("Overridden by user specified repeatability: {}".format(self.args.stage_repeatability))
            if stage_repeatability > 10:
                logging.warning("The computed Repeatability ({}) is unusually large. Consider manually specifying the repeatability in the Advanced Parameters.".format(stage_repeatability))

        return stage_repeatability

    def remove_invalid_translations_per_row_col(self, direction: str):
        # Remove invalid translations that are less than 0.5 and not within the median per row for X and Y
        # All translations that are not in range or have a correlation less than 0.5 have their correlations set to NaN.
        # This operates per row or column depending on the direction
        if direction == 'VERTICAL':
            valid_tiles = self.vertical_valid_tiles
        else:
            valid_tiles = self.horizontal_valid_tiles

        # compute median x and y values per row or col over the valid tiles
        med_x_vals = dict()
        med_y_vals = dict()
        for tile in valid_tiles:
            key = tile.r if direction == 'VERTICAL' else tile.c
            if key not in med_x_vals:
                med_x_vals[key] = list()
                med_y_vals[key] = list()
            t = tile.get_translation(direction)
            if t is None:
                continue
            med_x_vals[key].append(t.x if direction == 'VERTICAL' else t.x)
            med_y_vals[key].append(t.y if direction == 'VERTICAL' else t.y)
        for key in med_x_vals.keys():
            med_x_vals[key] = np.median(med_x_vals[key])
            med_y_vals[key] = np.median(med_y_vals[key])

        # fill in missing values with nans
        for key in range(self.args.grid_height if direction == 'VERTICAL' else self.args.grid_width):
            if key not in med_x_vals:
                med_x_vals[key] = np.nan
                med_y_vals[key] = np.nan

        # loop over the grid, deleting translations that are not within the median +- repeatability, or have a low correlation
        for r in range(self.args.grid_height):
            for c in range(self.args.grid_width):
                tile = self.tile_grid.get_tile(r, c)
                if tile is None:
                    continue
                t = tile.get_translation(direction)
                if t is None:
                    continue

                key = tile.r if direction == 'VERTICAL' else tile.c
                if np.isnan(med_x_vals[key]) or np.isnan(med_y_vals[key]):
                    t.ncc = np.nan
                    continue

                x_min = med_x_vals[key] - self.repeatability
                x_max = med_x_vals[key] + self.repeatability
                y_min = med_y_vals[key] - self.repeatability
                y_max = med_y_vals[key] + self.repeatability
                # If correlation is less than CorrelationThreshold or outside x range or outside y range, then throw away
                if t.ncc < 0.5 or t.x < x_min or t.x > x_max or t.y < y_min or t.y > y_max:
                    if tile in valid_tiles:
                        # remove it from the valid tiles list if present
                        valid_tiles.remove(tile)
                    t.ncc = np.nan
                else:
                    valid_tiles.add(tile)  # set add wont have duplicates

            self.stats['{}_valid_tiles'.format(direction.lower())] = len(valid_tiles)


    def replace_invalid_translations_per_row_col(self, direction: str):
        assert direction in ['VERTICAL', 'HORIZONTAL']

        # compute the median x and y values per row or col over the whole grid
        med_x_vals = dict()
        med_y_vals = dict()
        for r in range(1, self.args.grid_height):  # always start at 1, as the north/west edge tiles are always None
            for c in range(1, self.args.grid_width):  # always start at 1, as the north/west edge tiles are always None
                tile = self.tile_grid.get_tile(r, c)
                if tile is None:
                    continue

                key = tile.r if direction == 'VERTICAL' else tile.c
                if key not in med_x_vals:
                    med_x_vals[key] = list()
                    med_y_vals[key] = list()

                t = tile.get_translation(direction)
                if t is None:
                    continue
                med_x_vals[key].append(t.x if direction == 'VERTICAL' else t.x)
                med_y_vals[key].append(t.y if direction == 'VERTICAL' else t.y)

        for key in med_x_vals.keys():
            med_x_vals[key] = np.median(med_x_vals[key])
            med_y_vals[key] = np.median(med_y_vals[key])

        empty_rows_cols = list()
        for key in med_x_vals.keys():
            if np.isnan(med_x_vals[key]) or np.isnan(med_y_vals[key]):
                empty_rows_cols.append(key)
        if direction == 'VERTICAL':
            self.missing_cols = empty_rows_cols
        else:
            self.missing_rows = empty_rows_cols

        for r in range(self.args.grid_height):
            for c in range(self.args.grid_width):
                tile = self.tile_grid.get_tile(r, c)
                if tile is None:
                    continue
                key = tile.r if direction == 'VERTICAL' else tile.c

                t = tile.get_translation(direction)
                if t is None:
                    continue
                if np.isnan(t.ncc):
                    t.x = med_x_vals[key]
                    t.y = med_y_vals[key]

    def apply_model_per_direction(self, direction: str):
        assert direction in ['VERTICAL', 'HORIZONTAL']

        valid_tiles = self.vertical_valid_tiles if direction == 'VERTICAL' else self.horizontal_valid_tiles
        img_shape = self.tile_grid.get_image_shape()
        h_or_w = img_shape[0] if direction == 'VERTICAL' else img_shape[1]

        if len(valid_tiles) == 0:
            logging.info("No valid translations found for direction: {}. All translations will be replaced with image overlap.".format(direction))
            overlap = self.vertical_overlap if direction == 'VERTICAL' else self.horizontal_overlap
            est_translation = int(h_or_w * (1 - overlap / 100.0))
            for r in range(self.args.grid_height):
                for c in range(self.args.grid_width):
                    tile = self.tile_grid.get_tile(r, c)
                    if tile is None:
                        continue
                    t = tile.get_translation(direction)
                    if direction == 'VERTICAL':
                        t.y = est_translation
                        t.x = 0
                    else:
                        t.y = 0
                        t.x = est_translation
            return

        logging.info("Fixing translations for direction: {}".format(direction))

        # Remove invalid translations that are less than 0.5 and not within the median per row for X and Y
        # All translations that are not in range or have a correlation less than 0.5 have their correlations set to NaN.
        # This operates per row or column depending on the direction
        self.remove_invalid_translations_per_row_col(direction)

        # fill in the invalid (translations that have NaN in their correlation) translations per row/col
        # If an entire row/col is empty, then it is added to the list of empty rows/cols
        self.replace_invalid_translations_per_row_col(direction)

        empty_rows_cols = self.missing_cols if direction == 'VERTICAL' else self.missing_rows
        if len(empty_rows_cols) > 0:
            logging.info("Missing rows/cols: {}".format(empty_rows_cols))
        self.stats['{}_missing_rows_cols'.format(direction.lower())] = empty_rows_cols
        val = len(empty_rows_cols) / self.args.grid_height if direction == 'VERTICAL' else len(empty_rows_cols) / self.args.grid_width
        self.stats['{}_missing_rows_cols_percentage'.format(direction.lower())] = val

        valid_tiles = self.vertical_valid_tiles if direction == 'VERTICAL' else self.horizontal_valid_tiles
        if len(valid_tiles) > 0:
            # compute the median translation in the primary direction of travel
            direction_of_travel_estimate = int(np.median([t.north_translation.y if direction == 'VERTICAL' else t.west_translation.x for t in valid_tiles]))
        else:
            logging.warning("No valid translations found at all for direction: {}, replacing any missing translations with estimated translation based on the stageModel overlap: (x,y) = (0, overlap*imageHeight)".format(direction))
            overlap = self.vertical_overlap if direction == 'VERTICAL' else self.horizontal_overlap
            overlap = overlap / 100.0  # convert [0,100] to [0,1]
            overlap = 1.0 - overlap  # invert from overlap to non-overlapping distance
            direction_of_travel_estimate = int(h_or_w * overlap)

        # replace any invalid (ncc = nan) translations with the direction_of_travel_estimate
        for r in range(self.args.grid_height):
            for c in range(self.args.grid_width):
                tile = self.tile_grid.get_tile(r, c)
                if tile is None:
                    continue
                t = tile.get_translation(direction)
                if t is None:
                    continue
                if np.isnan(t.ncc):
                    if direction == 'VERTICAL':
                        t.y = int(direction_of_travel_estimate)
                        t.x = 0
                    else:
                        t.y = 0
                        t.x = int(direction_of_travel_estimate)

    def build(self):
        # build stage model
        start_time = time.time()
        self.horizontal_overlap = self.compute_overlap("HORIZONTAL")
        self.stats['horizontal_overlap'] = self.horizontal_overlap
        self.vertical_overlap = self.compute_overlap("VERTICAL")
        self.stats['vertical_overlap'] = self.vertical_overlap

        if self.args.horizontal_overlap is not None:
            logging.info("Overriding horizontal overlap with user provided value: {}".format(self.args.horizontal_overlap))
            self.horizontal_overlap = self.args.horizontal_overlap
            self.stats['horizontal_overlap'] = self.horizontal_overlap

        if self.args.vertical_overlap is not None:
            logging.info("Overriding vertical overlap with user provided value: {}".format(self.args.vertical_overlap))
            self.vertical_overlap = self.args.vertical_overlap
            self.stats['vertical_overlap'] = self.vertical_overlap

        if not np.isfinite(self.horizontal_overlap):
            raise RuntimeError("Compute horizontal image grid overlap is not finite: {}. Please provide the appropriate overlap via the command line".format(self.horizontal_overlap))
        if not np.isfinite(self.vertical_overlap):
            raise RuntimeError("Compute image grid vertical overlap is not finite: {}. Please provide the appropriate overlap via the command line".format(self.vertical_overlap))

        # compute stage repeatability
        self.horizontal_repeatability = self.compute_repeatability("HORIZONTAL")
        self.stats['horizontal_repeatability'] = self.horizontal_repeatability
        self.vertical_repeatability = self.compute_repeatability("VERTICAL")
        self.stats['vertical_repeatability'] = self.vertical_repeatability
        self.repeatability = int(max(self.horizontal_repeatability, self.vertical_repeatability))
        self.stats['repeatability'] = self.repeatability
        logging.info("Global Stage Model Repeatability = {}".format(self.repeatability))

        self.apply_model_per_direction("HORIZONTAL")
        self.apply_model_per_direction("VERTICAL")
        # update the repeatability to reflect the search range (to encompass +- r)
        self.repeatability = int(2 * self.repeatability + 1)
        logging.info("Calculated Repeatability = {} pixels".format(self.repeatability))
        elapsed_time = time.time() - start_time
        logging.info("Stage Model Computation Time = {} seconds".format(elapsed_time))

    def save_stats(self, output_filepath):
        key_list = self.stats.keys()
        n_keys = [k for k in key_list if k.startswith('vertical')]
        h_keys = [k for k in key_list if k.startswith('horizontal')]
        other_keys = [k for k in key_list if k not in n_keys and k not in h_keys]
        with open(output_filepath, 'w') as f:
            for key in other_keys:
                f.write("{}: {}\n".format(key, self.stats[key]))
            f.write("\n")
            for key in n_keys:
                f.write("{}: {}\n".format(key, self.stats[key]))
            f.write("\n")
            for key in h_keys:
                f.write("{}: {}\n".format(key, self.stats[key]))
            f.write("\n")





