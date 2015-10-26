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
// Date: Jul 2, 2014 11:51:06 AM EST
//
// Time-stamp: <Jul 2, 2014 11:51:06 AM tjb3>
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
 * Test case for stitching a grid of tiles using Java.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TestJavaGridPhaseCorrelation {

  private static void runTestGridPhaseCorrelation() throws FileNotFoundException {
    int startRow = 0;
    int startCol = 0;
    int extentWidth = 4;
    int extentHeight = 4;

    Log.setLogLevel(LogType.HELPFUL);
    // Debug.setDebugLevel(DebugType.VERBOSE);
    Log.msg(LogType.MANDATORY, "Running Test Grid Phase Correlation Java");

    File tileDir = new File("F:\\StitchingData\\joe_bad_data");

    Log.msg(LogType.HELPFUL, "Generating tile grid");

    TileGrid<ImageTile<float[][]>> grid = null;
    try {
      TileGridLoader loader =
          new SequentialTileGridLoader(16, 22, 1,
              "KB_H9Oct4GFP_20130518_p000{ppp}t00000102z001c01.tif", GridOrigin.UL,
              GridDirection.HORIZONTALCOMBING);

      grid =
          new TileGrid<ImageTile<float[][]>>(startRow, startCol, extentWidth, extentHeight, loader,
              tileDir, JavaImageTile.class);
    } catch (InvalidClassException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }
    TileGridTraverser<ImageTile<float[][]>> gridTraverser =
        TileGridTraverserFactory.makeTraverser(Traversals.DIAGONAL, grid);

    if (grid == null)
      return;

    ImageTile<?> tile = grid.getSubGridTile(0, 0);
    JavaImageTile.initJavaPlan(tile);

    Log.msg(LogType.HELPFUL, "Computing translations");
    TimeUtil.tick();

    // TODO setup the SequentialExecutor in place of this function call
//    Stitching.stitchGridJava(gridTraverser, grid);

    Log.msg(LogType.MANDATORY, "Completed Stitching in " + TimeUtil.tock() + " ms");
    Stitching.outputRelativeDisplacements(grid, new File(
        "F:\\StitchingData\\70perc_input_images\\OutData",
        "JAVA4x4relativePositionsWorkflowNoOptimization.txt"));

  }

  /**
   * Executes the test case
   *
   * @param args not used
   */
  public static void main(String args[]) {
    try {
      TestJavaGridPhaseCorrelation.runTestGridPhaseCorrelation();
    } catch (FileNotFoundException e) {
      Log.msg(LogType.MANDATORY, "Unable to find file: " + e.getMessage());
    }
  }
}
