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

package gov.nist.isg.mist.fftw;

import gov.nist.isg.mist.timing.TimeUtil;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.SequentialTileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridDirection;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridOrigin;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverser;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverser.Traversals;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverserFactory;
import org.bridj.Pointer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;

/**
 * Test case for stitching a grid of tiles using FFTW.
 * 
 * @author Tim Blattner
 * @version 1.0
 */
public class TestFFTWGridPhaseCorrelation {

  /**
   * Computes the phase correlation using a single thread on a grid of tiles using FFTW
   */
  public static void runTestGridPhaseCorrelation() throws FileNotFoundException{

    Log.setLogLevel(LogType.HELPFUL);
    // Debug.setDebugLevel(DebugType.VERBOSE);
    int startRow = 0;
    int startCol = 0;
    int extentWidth = 14;
    int extentHeight = 18;

    Log.msg(LogType.MANDATORY, "Running Test Grid Phase Correlation FFTW");

    File tileDir = new File("F:\\StitchingData\\worms1\\");
    FftwImageTile.initLibrary(System.getProperty("user.dir") + File.separator + "libs"
        + File.separator + "fftw",
        System.getProperty("user.dir") + File.separator + "util-fns--no", "libfftw3");

    Log.msg(LogType.INFO, "Generating tile grid");
    TileGrid<ImageTile<Pointer<Double>>> grid = null;
    try {

      TileGridLoader loader =
          new SequentialTileGridLoader(14, 18, 1, "worm_img_{pppp}.tif", GridOrigin.UL,
              GridDirection.HORIZONTALCOMBING);

      grid =
          new TileGrid<ImageTile<Pointer<Double>>>(startRow, startCol, extentWidth, extentHeight,
              loader, tileDir, FftwImageTile.class);
    } catch (InvalidClassException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }

    if (grid == null)
      return;
    
    ImageTile<Pointer<Double>> tile = grid.getSubGridTile(0, 0);

    tile.readTile();

    Log.msg(LogType.INFO, "Loading FFTW plan");

    FftwImageTile.initPlans(tile.getWidth(), tile.getHeight(), 0x42, true, "test.dat");

    FftwImageTile.savePlan("test.dat");

    Log.msg(LogType.INFO, "Creating sub-grid");
    TileGridTraverser<ImageTile<Pointer<Double>>> gridTraverser =
        TileGridTraverserFactory.makeTraverser(Traversals.DIAGONAL, grid);

    Log.msg(LogType.INFO, "Computing translations");
    TimeUtil.tick();
    Stitching.stitchGridFftw(gridTraverser, grid);

    // GlobalOptimization.execute(grid, 0.9, OptimizationType.GLOBAL,
    // OP_TYPE.MEDIAN);

    // GraphUtils.generateGraphComposition(0.9, grid);

    // GraphUtils.printTranslations(grid);

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
          TestFFTWGridPhaseCorrelation.runTestGridPhaseCorrelation();
      }
      catch (FileNotFoundException e)
      {
        Log.msg(LogType.MANDATORY, "Unable to find file: " + e.getMessage());
      }
  }
}
