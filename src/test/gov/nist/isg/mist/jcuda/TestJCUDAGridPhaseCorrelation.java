// ================================================================
//
// Disclaimer: IMPORTANT: This software was developed at the National
// Institute of Standards and Technology by employees of the Federal
// Government in the course of their official duties. Pursuant to
// title 17 Section 105 of the United States Code this software is not
// subject to copyright protection and is in the public domain. This
// is an experimental system. NIST assumes no responsibility
// whatsoever for its use by other parties, and makes no guarantees,
// expressed or implied, about its quality, reliability, or any other
// characteristic. We would appreciate acknowledgement if the software
// is used. This software can be redistributed and/or modified freely
// provided that any derivative works bear some notice that they are
// derived from it, and any modified versions bear some notice that
// they have been modified.
//
// ================================================================

// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 4:16:50 PM EST
//
// Time-stamp: <Aug 1, 2013 4:16:50 PM tjb3>
//
//
// ================================================================

package test.gov.nist.isg.mist.jcuda;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;

import test.gov.nist.isg.mist.timing.TimeUtil;
import jcuda.driver.CUcontext;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.JCudaDriver;
import jcuda.jcufft.JCufft;
import main.gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import main.gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import main.gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaImageTile;
import main.gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaUtils;
import main.gov.nist.isg.mist.stitching.lib.libraryloader.LibraryUtils;
import main.gov.nist.isg.mist.stitching.lib.log.Debug;
import main.gov.nist.isg.mist.stitching.lib.log.Log;
import main.gov.nist.isg.mist.stitching.lib.log.Debug.DebugType;
import main.gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import main.gov.nist.isg.mist.stitching.lib.optimization.OptimizationUtils;
import main.gov.nist.isg.mist.stitching.lib.optimization.OptimizationUtils.Direction;
import main.gov.nist.isg.mist.stitching.lib.optimization.OptimizationUtils.DisplacementValue;
import main.gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;
import main.gov.nist.isg.mist.stitching.lib.tilegrid.loader.SequentialTileGridLoader;
import main.gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader;
import main.gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridDirection;
import main.gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridOrigin;
import main.gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverser;
import main.gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverserFactory;
import main.gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverser.Traversals;

/**
 * Test case for stitching a grid of tiles using FFTW.
 * 
 * @author Tim Blattner
 * @version 1.0
 */
public class TestJCUDAGridPhaseCorrelation {
  static {

    // Initialize libraries
    LibraryUtils.initalize();
  }

  /**
   * Computes the phase correlation using a single thread on a grid of tiles using FFTW
   */
  public static void runTestGridPhaseCorrelation() throws FileNotFoundException {
    JCudaDriver.setExceptionsEnabled(true);
    JCufft.setExceptionsEnabled(true);

    Log.setLogLevel(LogType.INFO);
    Debug.setDebugLevel(DebugType.VERBOSE);

    int startRow = 0;
    int startCol = 0;
    int extentWidth = 14;
    int extentHeight = 18;



    Log.msg(LogType.MANDATORY, "Running Test Grid Phase Correlation JCUDA");

    File tileDir = new File("F:\\StitchingData\\worms1\\");
    CudaImageTile tile = new CudaImageTile(new File(tileDir, "worm_img_0001.tif"));

    Log.msg(LogType.INFO, "Loading CUFFT plan");

    CUcontext[] contexts = CudaUtils.initJCUDA(1, new int[] {0}, tile);

    Log.msg(LogType.INFO, "Generating tile grid");
    TileGrid<ImageTile<CUdeviceptr>> grid = null;
    try {
      TileGridLoader loader =
          new SequentialTileGridLoader(14, 18, 1, "worm_img_{pppp}.tif", GridOrigin.UL,
              GridDirection.HORIZONTALCOMBING);

      grid =
          new TileGrid<ImageTile<CUdeviceptr>>(startRow, startCol, extentWidth, extentHeight,
              loader, tileDir, CudaImageTile.class);
    } catch (InvalidClassException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }

    Log.msg(LogType.INFO, "Creating grid traverser");
    TileGridTraverser<ImageTile<CUdeviceptr>> gridTraverser =
        TileGridTraverserFactory.makeTraverser(Traversals.DIAGONAL, grid);

    Log.msg(LogType.INFO, "Computing translations");
    TimeUtil.tick();

    Stitching.stitchGridCuda(gridTraverser, grid, contexts[0]);

    // GlobalOptimization.execute(grid, 0.9, OptimizationType.GLOBAL,
    // OP_TYPE.MEDIAN);

    // GraphUtils.generateGraphComposition(0.9, grid);

    Stitching.outputRelativeDisplacements(grid, new File(
        "F:\\StitchingData\\70perc_input_images\\OutData",
        "CUDA4x4relativePositionsSequentialNoOptimization.txt"));

    OptimizationUtils.printGrid(grid, Direction.West, DisplacementValue.X);
    OptimizationUtils.printGrid(grid, Direction.West, DisplacementValue.Y);

    // GraphUtils.printTranslations(grid);

    // Stitching.outputAbsolutePositions(grid, new File(tileDir,
    // "absolutePositions.txt"));

    // SingleImageGUI.runGUI(grid, "F:\\output_pyramids",
    // tileDir.getAbsolutePath());

    Log.msg(LogType.MANDATORY, "Completed Test in " + TimeUtil.tock() + " ms");

    // }
    // else
    // {
    // Log.msg(LogType.MANDATORY,"Test Failed: Failed to load libraries");
    // }

  }

  /**
   * Executes the test case
   * 
   * @param args not used
   */
  public static void main(String args[]) {
    try {
        TestJCUDAGridPhaseCorrelation.runTestGridPhaseCorrelation();
    }catch (FileNotFoundException e)
    {
        Log.msg(LogType.MANDATORY, "Unable to find file: " + e.getMessage());
    }
  }
}
