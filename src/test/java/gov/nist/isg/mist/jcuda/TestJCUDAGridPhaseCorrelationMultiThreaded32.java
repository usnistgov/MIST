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
// Date: Aug 1, 2013 4:18:09 PM EST
//
// Time-stamp: <Aug 1, 2013 4:18:09 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.jcuda;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;

import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaUtils;
import gov.nist.isg.mist.stitching.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.stitching.lib.log.Debug;
import gov.nist.isg.mist.stitching.lib.log.Debug.DebugType;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.SequentialTileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridDirection;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridOrigin;
import gov.nist.isg.mist.stitching.lib32.imagetile.jcuda.CudaImageTile32;
import gov.nist.isg.mist.stitching.lib32.parallel.gpu.GPUStitchingThreadExecutor32;
import gov.nist.isg.mist.timing.TimeUtil;
import jcuda.driver.CUcontext;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.JCudaDriver;

/**
 * Test case for stitching a grid of tiles with multithreading using FFTW.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TestJCUDAGridPhaseCorrelationMultiThreaded32 {

  static {

    // Initialize libraries
    LibraryUtils.initalize();
  }

  /**
   * Computes the phase correlation using a multiple thread on a grid of tiles using FFTW
   */
  public static void runTestGridPhaseCorrelation() throws FileNotFoundException {
    int startRow = 0;
    int startCol = 0;
    int extentWidth = 23;
    int extentHeight = 30;

    File tileDir = new File("C:\\majurski\\image-data\\1h_Wet_10Perc\\");

    JCudaDriver.setExceptionsEnabled(false);
    Debug.setDebugLevel(DebugType.NONE);
    Log.setLogLevel(LogType.MANDATORY);
    // Debug.setDebugLevel(LogType.VERBOSE);
    Log.msg(LogType.MANDATORY, "Running Test Grid Phase Correlation Multithreaded JCUDA");


    Log.msg(LogType.INFO, "Generating tile grid");
    TileGrid<ImageTile<CUdeviceptr>> grid = null;
    try {
      TileGridLoader loader =
          new SequentialTileGridLoader(extentWidth, extentHeight, 1, "KB_2012_04_13_1hWet_10Perc_IR_0{pppp}.tif", GridOrigin.UR,
              GridDirection.VERTICALCOMBING);

      grid =
          new TileGrid<ImageTile<CUdeviceptr>>(startRow, startCol, extentWidth, extentHeight,
              loader, tileDir, CudaImageTile32.class);
    } catch (InvalidClassException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }

    if (grid == null)
      return;

    ImageTile<CUdeviceptr> tile = grid.getSubGridTile(0, 0);
    tile.readTile();

    int[] devIDs = new int[]{0, 1, 2};
    CUcontext[] contexts = CudaUtils.initJCUDA(1, devIDs, tile);

    GPUStitchingThreadExecutor32<CUdeviceptr> executor =
        new GPUStitchingThreadExecutor32<CUdeviceptr>(contexts.length, 12, tile, grid, contexts,
            devIDs);

    tile.releasePixels();

    Log.msg(LogType.INFO, "Computing translations");
    TimeUtil.tick();
    executor.execute();

    Log.msg(LogType.INFO, "Computing global optimization");

    Log.msg(LogType.INFO, "Computing absolute positions");

    Log.msg(LogType.MANDATORY, "Completed Test in " + TimeUtil.tock() + " ms");

  }

  /**
   * Executes the test case
   *
   * @param args not used
   */
  public static void main(String args[]) {
    try {
      TestJCUDAGridPhaseCorrelationMultiThreaded32.runTestGridPhaseCorrelation();
    } catch (FileNotFoundException e) {
      Log.msg(LogType.MANDATORY, "Unable to find file: " + e.getMessage());
    }
  }
}
