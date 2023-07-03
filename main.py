import os

import translation_refinement

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
import grid
import utils
import pciam
import stage_model
import translation_refinement
import assemble




def mist_single_threaded(args: argparse.Namespace, tile_grid: grid.TileGrid):
    tile_grid.print_names()

    logging.info("Computing all pairwise translations for between images")
    # TODO this will need parallelization
    translation_computation = pciam.SequentialPciam(args)
    translation_computation.execute(tile_grid)

    tile_grid.print_peaks('north', 'ncc')
    tile_grid.print_peaks('west', 'ncc')
    tile_grid.print_peaks('west', 'x')

    # write pre-optimization translations to file
    output_filename = "{}relative-positions-no-optimization-{}.txt".format(args.output_prefix, args.time_slice)
    tile_grid.write_translations_to_file(os.path.join(args.output_dirpath, output_filename))

    # build the stage model
    sm = stage_model.StageModel(args, tile_grid)
    sm.build()

    # refine the translations
    # TODO this translation refinement needs parallelization
    translation_refiner = translation_refinement.RefineSequential(args, tile_grid, sm)
    translation_refiner.execute()

    # TODO compose global positions
    # resume from GlobalOptimization.java line 106
    global_positions = translation_refinement.GlobalPositions(tile_grid)
    global_positions.traverse_minimum_spanning_tree()

    output_filename = "{}global-positions-{}.txt".format(args.output_prefix, args.time_slice)
    global_positions_filepath = os.path.join(args.output_dirpath, output_filename)
    tile_grid.write_global_positions_to_file(global_positions_filepath)

    if args.save_image:
        img_output_filepath = os.path.join(args.output_dirpath, "{}stitched-{}.tiff".format(args.output_prefix, args.time_slice))
        assemble.assemble_image(global_positions_filepath, args.image_dirpath, img_output_filepath)


def mist_multi_threaded(args: argparse.Namespace, tile_grid: grid.TileGrid):
    raise NotImplementedError

def mist(args: argparse.Namespace):
    if os.path.exists(args.output_dirpath):
        import shutil
        shutil.rmtree(args.output_dirpath)
    os.makedirs(args.output_dirpath)

    # add the file based handler to the logger
    fh = logging.FileHandler(filename=os.path.join(args.output_dirpath, '{}log.txt'.format(args.output_prefix)))
    fh.setFormatter(logging.Formatter("%(asctime)s [%(levelname)s] [%(filename)s:%(lineno)d] %(message)s"))
    logging.getLogger().addHandler(fh)

    # build the grid representation
    if args.filename_pattern_type == 'SQEUENTIAL':
        tile_grid = grid.TileGridSequential(args)
    elif args.filename_pattern_type == 'ROWCOL':
        tile_grid = grid.TileGridRowCol(args)
    else:
        raise RuntimeError("Unknown filename pattern type: {}".format(args.filename_pattern_type))

    if args.disable_mem_cache:
        mist_single_threaded(args, tile_grid)
    else:
        mist_multi_threaded(args, tile_grid)




# define main function if name is main
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
    parser.add_argument('--filename-pattern-type', type=str, required=True, choices=['SQEUENTIAL', 'ROWCOL'])
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
    parser.add_argument('--translation-refinement-method', type=str, default='SINGLEHILLCLIMB', choices=['SINGLEHILLCLIMB', 'MULTIPOINTHILLCLIMB', 'EXHAUSTIVE'])
    parser.add_argument('--num-hill-climbs', type=int, default=16)
    parser.add_argument('--num-fft-peaks', type=int, default=2)

    args = parser.parse_args()

    mist(args)