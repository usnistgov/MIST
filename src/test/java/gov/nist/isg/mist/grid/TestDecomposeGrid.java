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
// Date: Aug 1, 2013 4:18:35 PM EST
//
// Time-stamp: <Aug 1, 2013 4:18:35 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.grid;

import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid.GridDecomposition;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.SequentialTileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridDirection;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridOrigin;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverser;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverser.Traversals;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverserFactory;
import org.bridj.Pointer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InvalidClassException;
import java.util.Iterator;
import java.util.List;

/**
 * Test case for traversing a grid of tiles in different styles.
 * 
 * @author Tim Blattner
 * @version 1.0
 */
public class TestDecomposeGrid {

  /**
   * Tests creating a grid and running every possible traverser on that grid.
   */
  public static void runTestCreateGridAndDecompose() {
    Log.setLogLevel(LogType.INFO);

    int startRow = 0;
    int startCol = 0;
    int extentWidth = 70;
    int extentHeight = 93;
    int plateWidth = extentWidth + startRow;
    int plateHeight = extentHeight + startCol;
    int numSlices = 4;
    Log.msg(LogType.MANDATORY, "Running Test Create and Decompose Grid");

    GridDirection numbering = GridDirection.HORIZONTALCOMBING;
    GridOrigin origin = GridOrigin.UL;

    Traversals traversal = Traversals.ROW;
    File imageDir = new File("");

    TileGrid<ImageTile<Pointer<Double>>> grid = null;
    try {
      TileGridLoader loader =
          new SequentialTileGridLoader(plateWidth, plateHeight, 1, "{pppp}", origin, numbering);

      grid =
          new TileGrid<ImageTile<Pointer<Double>>>(startRow, startCol, extentWidth, extentHeight,
              loader, imageDir, FftwImageTile.class);
    } catch (InvalidClassException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }
    if (grid == null)
      return;
    
    Log.msg(LogType.HELPFUL, "Printing: " + grid.getGridStats());
    grid.printNumberGrid();

    Log.msgNoTime(LogType.HELPFUL, "\nBeginning Grid Decomposition with " + numSlices + " slices\n");

    File f = new File("test.txt");
    FileWriter fw;
    try {
      fw = new FileWriter(f);

      for (GridDecomposition decompType : GridDecomposition.values()) {

        Log.msg(LogType.HELPFUL, "Decomposition type: " + decompType);
        List<TileGrid<ImageTile<Pointer<Double>>>> grids =
            grid.partitionGrid(numSlices, decompType);

        if (grids.size() != numSlices) {
          Log.msg(LogType.HELPFUL,
              "A better decomposition for your grid has been found. Reducing numSlices from "
                  + numSlices + " to " + grids.size());
        }

        Log.msg(LogType.HELPFUL, "Printing subgrids");
        for (TileGrid<ImageTile<Pointer<Double>>> subgrid : grids) {
          TileGridTraverser<ImageTile<Pointer<Double>>> traverser =
              TileGridTraverserFactory.makeTraverser(traversal, subgrid);
          Log.msg(LogType.HELPFUL, traverser.toString());
          Iterator<ImageTile<Pointer<Double>>> it = traverser.iterator();
          while (it.hasNext()) {
            ImageTile<?> tile = it.next();

            if (it.hasNext())
              Log.msgnonlNoTime(LogType.HELPFUL, tile.getFileName() + "->");
            else
              Log.msgnonlNoTime(LogType.HELPFUL, tile.getFileName() + "->DONE");
          }
          Log.msgNoTime(LogType.HELPFUL, "");
        }



        for (int row = startRow; row < extentHeight + startRow; row++) {
          for (int col = startCol; col < extentWidth + startCol; col++) {
            for (int i = 0; i < grids.size(); i++) {
              if (grids.get(i).hasTile(row, col)) {
                fw.write(Integer.toString(i));
                Log.msgnonlNoTime(LogType.HELPFUL, Integer.toString(i));
              }
            }
          }
          Log.msgNoTime(LogType.HELPFUL, "");
          fw.write("\n");
        }

        fw.write("\n");

        Log.msgNoTime(LogType.HELPFUL, "");

      }

      fw.close();

    } catch (IOException e) {
      e.printStackTrace();
    }

    Log.msg(LogType.MANDATORY, "Test Complete");
  }

  /**
   * Executes the test case
   * 
   * @param args not used
   */
  public static void main(String args[]) {
    TestDecomposeGrid.runTestCreateGridAndDecompose();
  }
}
