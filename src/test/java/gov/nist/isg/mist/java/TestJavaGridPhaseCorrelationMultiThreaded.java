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
// Date: Jul 2, 2014 11:51:33 AM EST
//
// Time-stamp: <Jul 2, 2014 11:51:33 AM tjb3>
//
//
// ================================================================
package gov.nist.isg.mist.java;


import gov.nist.isg.mist.timing.TimeUtil;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.imagetile.java.JavaImageTile;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.parallel.cpu.CPUStitchingThreadExecutor;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.SequentialTileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridDirection;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridOrigin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;

/**
 * Test case for stitching a grid of tiles with multithreading using Java.
 * 
 * @author Tim Blattner
 * @version 1.0
 */
public class TestJavaGridPhaseCorrelationMultiThreaded {

  private static void runTestGridPhaseCorrelation() throws FileNotFoundException {
    Log.setLogLevel(LogType.HELPFUL);
    int startRow = 0;
    int startCol = 0;
    int extentWidth = 10;
    int extentHeight = 10;

    Log.msg(LogType.MANDATORY, "Running Test Grid Phase Correlation Multithreaded Java");

    File tileDir = new File("F:\\StitchingData\\70perc_input_images");

    Log.msg(LogType.HELPFUL, "Generating tile grid");
    TileGrid<ImageTile<float[][]>> grid = null;
    try {

      TileGridLoader loader =
          new SequentialTileGridLoader(42, 59, 1, "F_{pppp}.tif", GridOrigin.UR,
              GridDirection.VERTICALCOMBING);

      grid =
          new TileGrid<ImageTile<float[][]>>(startRow, startCol, extentWidth, extentHeight, loader,
              tileDir, JavaImageTile.class);
    } catch (InvalidClassException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }

    Log.msg(LogType.HELPFUL, "Loading Java plan");
    
    if (grid == null)
      return;
    
    ImageTile<float[][]> tile = grid.getSubGridTile(0, 0);
    JavaImageTile.initJavaPlan(tile);

    CPUStitchingThreadExecutor<float[][]> executor =
        new CPUStitchingThreadExecutor<float[][]>(1, 8, tile, grid, null);

    Log.msg(LogType.HELPFUL, "Computing translations");
    TimeUtil.tick();
    executor.execute();

    Log.msg(LogType.MANDATORY, "Completed Stitching in " + TimeUtil.tock() + " ms");

    Stitching.printRelativeDisplacements(grid);

  }

  /**
   * Executes the test case
   * 
   * @param args not used
   */
  public static void main(String args[]) {
      try {
          TestJavaGridPhaseCorrelationMultiThreaded.runTestGridPhaseCorrelation();
      } catch (FileNotFoundException e)
      {
          Log.msg(LogType.MANDATORY, "File not found: " + e.getMessage());
      }
  }
}
