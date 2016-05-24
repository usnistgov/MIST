
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


// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 4:18:35 PM EST
//
// Time-stamp: <Aug 1, 2013 4:18:35 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.grid;

import org.bridj.Pointer;

import java.io.File;
import java.io.InvalidClassException;
import java.util.Iterator;

import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
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

/**
 * Test case for traversing a grid of tiles in different styles.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TestTraverseGrid {

  /**
   * Tests creating a grid and running every possible traverser on that grid.
   */
  public static void runTestCreateGridAndTraverse() {
    Log.setLogLevel(LogType.INFO);

    int startRow = 4;
    int startCol = 4;
    int extentWidth = 2;
    int extentHeight = 2;
    Log.msg(LogType.MANDATORY, "Running Test Create and Traverse Grid");

    GridDirection numbering = GridDirection.HORIZONTALCOMBING;
    GridOrigin origin = GridOrigin.UL;
    File imageDir = new File("");

    TileGrid<ImageTile<Pointer<Double>>> subGrid = null;
    try {
      TileGridLoader loader = new SequentialTileGridLoader(10, 10, 1, "{pppp}", origin, numbering);

      subGrid =
          new TileGrid<ImageTile<Pointer<Double>>>(startRow, startCol, extentWidth, extentHeight,
              loader, imageDir, FftwImageTile.class);
    } catch (InvalidClassException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }

    if (subGrid == null)
      return;

    Log.msg(LogType.HELPFUL, "Printing: " + subGrid.getGridStats());
    subGrid.printNumberGrid();
    Log.msg(LogType.HELPFUL, "Beginning Grid Traversals...");
    for (Traversals traversal : Traversals.values()) {
      TileGridTraverser<ImageTile<Pointer<Double>>> traverser =
          TileGridTraverserFactory.makeTraverser(traversal, subGrid);
      Log.msg(LogType.HELPFUL, traverser.toString());
      Iterator<ImageTile<Pointer<Double>>> it = traverser.iterator();
      while (it.hasNext()) {
        ImageTile<?> tile = it.next();

        if (it.hasNext())
          Log.msgnonl(LogType.HELPFUL, tile.getFileName() + "->");
        else
          Log.msgnonl(LogType.HELPFUL, tile.getFileName() + "->DONE");
      }
      Log.msg(LogType.HELPFUL, "");
    }

    Log.msg(LogType.MANDATORY, "Test Complete");
  }

  /**
   * Executes the test case
   *
   * @param args not used
   */
  public static void main(String args[]) {
    TestTraverseGrid.runTestCreateGridAndTraverse();
  }
}
