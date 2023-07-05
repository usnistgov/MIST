import os
# enforce single threading for libraries to allow for multithreading across image instances.
os.environ['MKL_NUM_THREADS'] = '1'
os.environ['NUMEXPR_NUM_THREADS'] = '1'
os.environ['NUMEXPR_MAX_THREADS'] = '1'
os.environ['OMP_NUM_THREADS'] = '1'

import logging
logging.basicConfig(level=logging.INFO,
                        format="%(asctime)s [%(levelname)s] [%(filename)s:%(lineno)d] %(message)s",
                        handlers=[logging.StreamHandler()])
import argparse
import time

# local imports
import translation_refinement
import img_grid
import pciam
import stage_model
import assemble
import utils



def mist(args: argparse.Namespace):
    if utils.is_ide_debug_mode():
        logging.info("IDE in debug mode, automatic output overwriting enabled.")
        if os.path.exists(args.output_dirpath):
            import shutil
            shutil.rmtree(args.output_dirpath)

    if os.path.exists(args.output_dirpath):
        raise RuntimeError("Output directory already exists: {}".format(args.output_dirpath))

    os.makedirs(args.output_dirpath)

    # add the file based handler to the logger
    fh = logging.FileHandler(filename=os.path.join(args.output_dirpath, '{}log.txt'.format(args.output_prefix)))
    fh.setFormatter(logging.Formatter("%(asctime)s [%(levelname)s] [%(filename)s:%(lineno)d] %(message)s"))
    logging.getLogger().addHandler(fh)

    # build the grid representation
    if args.filename_pattern_type == 'SEQUENTIAL':
        tile_grid = img_grid.TileGridSequential(args)
    elif args.filename_pattern_type == 'ROWCOL':
        tile_grid = img_grid.TileGridRowCol(args)
    else:
        raise RuntimeError("Unknown filename pattern type: {}".format(args.filename_pattern_type))

    tile_grid.print_names()

    mist_start_time = time.time()
    logging.info("Computing all pairwise translations for between images")
    if args.disable_mem_cache:
        # if mem cache is off, only use single threaded version to save memory
        translation_computation = pciam.PciamSequential(args)
    else:
        translation_computation = pciam.PciamParallel(args)
    translation_computation.execute(tile_grid)

    # tile_grid.print_peaks('north', 'ncc')
    # tile_grid.print_peaks('west', 'ncc')
    # tile_grid.print_peaks('west', 'x')

    # write pre-optimization translations to file
    output_filename = "{}relative-positions-no-optimization-{}.txt".format(args.output_prefix, args.time_slice)
    tile_grid.write_translations_to_file(os.path.join(args.output_dirpath, output_filename))

    # build the stage model
    sm = stage_model.StageModel(args, tile_grid)
    sm.build()
    output_filename = "{}statistics-{}.txt".format(args.output_prefix, args.time_slice)
    sm.save_stats(os.path.join(args.output_dirpath, output_filename))

    # refine the translations
    if args.disable_mem_cache:
        translation_refiner = translation_refinement.RefineSequential(args, tile_grid, sm)
    else:
        translation_refiner = translation_refinement.RefineParallel(args, tile_grid, sm)
    translation_refiner.execute()

    # compose the pairwise translations into global positions using MST
    global_positions = translation_refinement.GlobalPositions(tile_grid)
    global_positions.traverse_minimum_spanning_tree()

    output_filename = "{}relative-positions-{}.txt".format(args.output_prefix, args.time_slice)
    tile_grid.write_translations_to_file(os.path.join(args.output_dirpath, output_filename))

    output_filename = "{}global-positions-{}.txt".format(args.output_prefix, args.time_slice)
    global_positions_filepath = os.path.join(args.output_dirpath, output_filename)
    tile_grid.write_global_positions_to_file(global_positions_filepath)

    if args.save_image:
        img_output_filepath = os.path.join(args.output_dirpath, "{}stitched-{}.tif".format(args.output_prefix, args.time_slice))
        assemble.assemble_image(global_positions_filepath, args.image_dirpath, img_output_filepath)

    elapsed_time = time.time() - mist_start_time
    logging.info("MIST took {} seconds".format(elapsed_time))



if __name__ == "__main__":

    # TODO add ability to load/parse the stitching-params file
    # TODO add support for loading a csv file of image names as the grid

    parser = argparse.ArgumentParser(description='Runs MIST stitching')
    parser.add_argument('--image-dirpath', type=str, required=True)
    parser.add_argument('--output-dirpath', type=str, required=True)
    parser.add_argument('--grid-width', type=int, required=True)
    parser.add_argument('--grid-height', type=int, required=True)
    parser.add_argument('--start-tile', type=int, default=0, help='The tile number to start at when using sequential tile numbering')
    parser.add_argument('--start-row', type=int, default=0, help='The row number to start at when using row/col tile numbering')
    parser.add_argument('--start-col', type=int, default=0, help='The col number to start at when using row/col tile numbering')
    parser.add_argument('--filename-pattern', type=str, required=True)
    parser.add_argument('--filename-pattern-type', type=str, required=True, choices=['SEQUENTIAL', 'ROWCOL'])
    parser.add_argument('--grid-origin', type=str, required=True, choices=['UL', 'UR', 'LL', 'LR'])
    parser.add_argument('--numbering-pattern', type=str, required=True, choices=['HORIZONTALCOMBING', 'VERTICALCOMBING', 'HORIZONTALCONTINUOUS', 'VERTICALCONTINUOUS'])
    parser.add_argument('--output-prefix', type=str, default='img-')
    parser.add_argument('--save-image', action="store_true", default=False)
    parser.add_argument('--disable-mem-cache', action="store_true", default=False)

    # stage model parameters
    parser.add_argument('--stage-repeatability', type=float, default=None)
    parser.add_argument('--horizontal-overlap', type=float, default=None)
    parser.add_argument('--vertical-overlap', type=float, default=None)
    parser.add_argument('--overlap-uncertainty', type=float, default=3.0)
    parser.add_argument('--valid-correlation-threshold', type=float, default=0.5, help='The minimum normalized cross correlation value to consider a translation valid')
    parser.add_argument('--time-slice', type=int, default=0)  # optional, sets the time slice to stitch when timeslice is present in the filename pattern

    # advanced parameters
    parser.add_argument('--translation-refinement-method', type=str, default='SINGLEHILLCLIMB', choices=['SINGLEHILLCLIMB', 'MULTIPOINTHILLCLIMB'])
    parser.add_argument('--num-hill-climbs', type=int, default=16)
    parser.add_argument('--num-fft-peaks', type=int, default=2)

    args = parser.parse_args()

    mist(args)