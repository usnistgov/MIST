// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.




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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InvalidClassException;
import java.util.Iterator;
import java.util.List;

import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.lib.tilegrid.TileGrid.GridDecomposition;
import gov.nist.isg.mist.lib.tilegrid.loader.SequentialTileGridLoader;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoader;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoader.GridDirection;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoader.GridOrigin;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverser;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverser.Traversals;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverserFactory;

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
          new SequentialTileGridLoader(plateWidth, plateHeight, 1, 0,0,"{pppp}", origin, numbering);

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
