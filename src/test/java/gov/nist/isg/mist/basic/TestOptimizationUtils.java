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
// characteristic. We would appreciate acknowledgment if the software
// is used. This software can be redistributed and/or modified freely
// provided that any derivative works bear some notice that they are
// derived from it, and any modified versions bear some notice that
// they have been modified.
//
// ================================================================

// ================================================================
//
// Author: tjb3
// Date: May 10, 2013 2:59:20 PM EST
//
// Time-stamp: <May 10, 2013 2:59:20 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.basic;

import gov.nist.isg.mist.stitching.lib.common.CorrelationTriple;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.optimization.OptimizationUtils;
import gov.nist.isg.mist.stitching.lib.optimization.OptimizationUtils.Direction;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.SequentialTileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridDirection;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridOrigin;

import org.bridj.Pointer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Test case for reading an image.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TestOptimizationUtils {

  /**
   * Tests reading an image
   */
  public static void testGetTopCorrelation() throws FileNotFoundException {
    Log.msg(LogType.MANDATORY, "Running Finding Top Correlations");

    int startRow = 0;
    int startCol = 0;
    int extentWidth = 42;
    int extentHeight = 59;

    GridDirection numbering = GridDirection.HORIZONTALCOMBING;
    GridOrigin origin = GridOrigin.UL;

    File imageDir = new File("");

    TileGrid<ImageTile<Pointer<Double>>> subGrid = null;
    try {
      TileGridLoader loader = new SequentialTileGridLoader(42, 59, 1, "{pppp}", origin, numbering);

      subGrid =
          new TileGrid<ImageTile<Pointer<Double>>>(startRow, startCol, extentWidth, extentHeight,
              loader, imageDir, FftwImageTile.class);
    } catch (InvalidClassException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }

    if (subGrid == null)
      return;

    Random rand = new Random(1000);

    List<CorrelationTriple> northList = new ArrayList<CorrelationTriple>();
    List<CorrelationTriple> westList = new ArrayList<CorrelationTriple>();

    Log.msg(LogType.MANDATORY, "Initializing with random correlations");
    for (int r = 0; r < subGrid.getExtentHeight(); r++) {
      for (int c = 0; c < subGrid.getExtentWidth(); c++) {
        CorrelationTriple north =
            new CorrelationTriple(rand.nextDouble(), rand.nextInt(), rand.nextInt());
        CorrelationTriple west =
            new CorrelationTriple(rand.nextDouble(), rand.nextInt(), rand.nextInt());
        subGrid.getSubGridTile(r, c).setNorthTranslation(north);
        subGrid.getSubGridTile(r, c).setWestTranslation(west);

        northList.add(north);
        westList.add(west);

      }
    }

    Log.msg(LogType.MANDATORY, "Getting top 5 correlations");
    List<CorrelationTriple> topFiveNorth =
        OptimizationUtils.getTopCorrelations(subGrid, Direction.North, 5);
    List<CorrelationTriple> topFiveWest =
        OptimizationUtils.getTopCorrelations(subGrid, Direction.West, 5);


    Collections.sort(northList);
    Collections.sort(westList);

    Log.msg(LogType.MANDATORY, "Real Top 5 North");
    for (int i = 0; i < 5; i++)
      Log.msg(LogType.MANDATORY, topFiveNorth.get(i).toString());

    Log.msg(LogType.MANDATORY, "Computed Top 5 North");
    for (CorrelationTriple t : topFiveNorth) {
      Log.msg(LogType.MANDATORY, t.toString());
    }

    Log.msg(LogType.MANDATORY, "Real Top 5 West");
    for (int i = 0; i < 5; i++)
      Log.msg(LogType.MANDATORY, topFiveWest.get(i).toString());

    Log.msg(LogType.MANDATORY, "Computed Top 5 West");
    for (CorrelationTriple t : topFiveWest) {
      Log.msg(LogType.MANDATORY, t.toString());
    }

    Log.msg(LogType.MANDATORY, "Test Completed.");
  }

  /**
   * Executes the test case
   *
   * @param args not used
   */
  public static void main(String[] args) {
    try {
      TestOptimizationUtils.testGetTopCorrelation();
    } catch (FileNotFoundException e) {
      Log.msg(LogType.MANDATORY, "Unable to find file: " + e.getMessage());
    }
  }
}
