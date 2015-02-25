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

package test.gov.nist.isg.mist.fftw;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;

import main.gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import main.gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import main.gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwImageTile;
import main.gov.nist.isg.mist.stitching.lib.imagetile.utilfns.UtilFnsStitching;
import main.gov.nist.isg.mist.stitching.lib.log.Log;
import main.gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import main.gov.nist.isg.mist.stitching.lib.parallel.cpu.CPUStitchingThreadExecutor;
import main.gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;
import main.gov.nist.isg.mist.stitching.lib.tilegrid.loader.SequentialTileGridLoader;
import main.gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader;
import main.gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridDirection;
import main.gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridOrigin;

import org.bridj.Pointer;

import test.gov.nist.isg.mist.timing.TimeUtil;

/**
 * Test case for stitching a grid of tiles with multithreading using FFTW.
 * 
 * @author Tim Blattner
 * @version 1.0
 */
public class TestFFTWGridPhaseCorrelationMultiThreaded {

  /**
   * Computes the phase correlation using a multiple thread on a grid of tiles using FFTW
   */
  public static void runTestGridPhaseCorrelation() throws FileNotFoundException {
    // UtilFnsStitching.disableUtilFnsNativeLibrary();
    UtilFnsStitching.enableUtilFnsNativeLibrary();
    int startRow = 0;
    int startCol = 0;
    int extentWidth = 10;
    int extentHeight = 10;

    Log.setLogLevel(LogType.HELPFUL);
    // Debug.setDebugLevel(DebugType.INFO);
    Log.msg(LogType.MANDATORY, "Running Test Grid Phase Correlation Multithreaded FFTW");

    File tileDir = new File("F:\\StitchingData\\70perc_input_images");
    // File tileDir = new File("F:\\StitchingData\\joe_bad_data");
    FftwImageTile.initLibrary(System.getProperty("user.dir") + File.separator + "libs"
        + File.separator + "fftw", System.getProperty("user.dir") + File.separator + "util-fns",
        "libfftw3");
    // {

    Log.msg(LogType.INFO, "Generating tile grid");
    TileGrid<ImageTile<Pointer<Double>>> grid = null;
    try {
      TileGridLoader loader =
          new SequentialTileGridLoader(14, 18, 1, "F_{pppp}.tif", GridOrigin.UR,
              GridDirection.VERTICALCOMBING);


      grid =
          new TileGrid<ImageTile<Pointer<Double>>>(startRow, startCol, extentWidth, extentHeight,
              loader, tileDir, FftwImageTile.class);
      // new TileGrid<ImageTile<Pointer<Double>>>(startRow, startCol, extentWidth, extentHeight,
      // 16, 22, GridOrigin.UL, GridDirection.HorizontalCombing, 1, tileDir,
      // "KB_H9Oct4GFP_20130518_p000{iii}t00000102z001c01.tif", FftwImageTile.class);
    } catch (InvalidClassException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }

    if (grid == null)
      return;
    
    ImageTile<Pointer<Double>> tile = grid.getSubGridTile(0, 0);
    tile.readTile();

    Log.msg(LogType.INFO, "Loading FFTW plan");

    FftwImageTile.initPlans(tile.getWidth(), tile.getHeight(), 0x21, true, "test.dat");

    FftwImageTile.savePlan("test.dat");

    // 4, 34 -> 5,34
    CPUStitchingThreadExecutor<Pointer<Double>> executor =
        new CPUStitchingThreadExecutor<Pointer<Double>>(1, 8, tile, grid);

    Log.msg(LogType.INFO, "Computing translations");
    TimeUtil.tick();
    executor.execute();

    Log.msg(LogType.INFO, "Computing global optimization");

    // GlobalOptimization.execute(grid, 0.9, OptimizationType.GLOBAL,
    // OP_TYPE.MEDIAN);
    //
    // Log.msg(LogType.INFO,"Computing absolute positions");
    // GraphUtils.generateGraphComposition(0.9, grid);

    // Stitching.printAbsolutePositions(grid);
    // Stitching.printRelativeDisplacements(grid);

    // SingleImageGUI.runGUI(grid, "F:\\output_pyramids",
    // tileDir.getAbsolutePath());

    // }
    Stitching.outputRelativeDisplacements(grid, new File(
        "F:\\StitchingData\\70perc_input_images\\OutData",
        "FFTWNoUtilFNSrelativePositionsWorkflowWithOptimization.txt"));

    Log.msg(LogType.MANDATORY, "Completed Test in " + TimeUtil.tock() + " ms");


    Stitching.printRelativeDisplacements(grid);

  }

  /**
   * Executes the test case
   * 
   * @param args not used
   */
  public static void main(String args[]) {
    try {
        TestFFTWGridPhaseCorrelationMultiThreaded.runTestGridPhaseCorrelation();
    }
    catch (FileNotFoundException e)
    {
        Log.msg(LogType.MANDATORY, "Unable to find file: " + e.getMessage());
    }
  }
}
