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

package gov.nist.isg.mist.jcuda;

import gov.nist.isg.mist.timing.TimeUtil;
import jcuda.driver.CUcontext;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.JCudaDriver;
import jcuda.jcufft.JCufft;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaUtils;
import gov.nist.isg.mist.stitching.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.stitching.lib.log.Debug;
import gov.nist.isg.mist.stitching.lib.log.Debug.DebugType;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.optimization.OptimizationUtils;
import gov.nist.isg.mist.stitching.lib.optimization.OptimizationUtils.Direction;
import gov.nist.isg.mist.stitching.lib.optimization.OptimizationUtils.DisplacementValue;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.SequentialTileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridDirection;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridOrigin;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverser;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverser.Traversals;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverserFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;

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

    int startRow = 0;
    int startCol = 0;
    int extentWidth = 23;
    int extentHeight = 30;

    File tileDir = new File("C:\\majurski\\image-data\\1h_Wet_10Perc\\");

    Log.msg(LogType.MANDATORY, "Running Test Grid Phase Correlation JCUDA");

    CudaImageTile tile = new CudaImageTile(new File(tileDir, "KB_2012_04_13_1hWet_10Perc_IR_00001.tif"));

    Log.msg(LogType.INFO, "Loading CUFFT plan");

    boolean enableCudaExceptions = true;
    CUcontext[] contexts = CudaUtils.initJCUDA(1, new int[]{0}, tile, enableCudaExceptions);

    Log.msg(LogType.INFO, "Generating tile grid");
    TileGrid<ImageTile<CUdeviceptr>> grid = null;
    try {
      TileGridLoader loader =
          new SequentialTileGridLoader(extentWidth, extentHeight, 1, "KB_2012_04_13_1hWet_10Perc_IR_0{pppp}.tif", GridOrigin.UR,
              GridDirection.VERTICALCOMBING);

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

    OptimizationUtils.printGrid(grid, Direction.West, DisplacementValue.X);
    OptimizationUtils.printGrid(grid, Direction.West, DisplacementValue.Y);


    Log.msg(LogType.MANDATORY, "Completed Test in " + TimeUtil.tock() + " ms");

  }

  /**
   * Executes the test case
   *
   * @param args not used
   */
  public static void main(String args[]) {
    try {
      TestJCUDAGridPhaseCorrelation.runTestGridPhaseCorrelation();
    } catch (FileNotFoundException e) {
      Log.msg(LogType.MANDATORY, "Unable to find file: " + e.getMessage());
    }
  }
}
