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
// Date: Aug 1, 2013 4:18:17 PM EST
//
// Time-stamp: <Aug 1, 2013 4:18:17 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.grid;

import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.SequentialTileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridDirection;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridOrigin;

/**
 * Test case for generating different image acquisition strategies.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TestCreateGrid {

  /**
   * Tests creating every possible type of grid and numbering scheme
   */
  public static void runTestCreateGrid() {
    Log.setLogLevel(LogType.HELPFUL);
    Log.msg(LogType.MANDATORY, "Running Test Create Grid");
    for (GridOrigin origin : GridOrigin.values()) {
      for (GridDirection dir : GridDirection.values()) {
        System.out.println("Origin: " + origin + " Direction: " + dir);
        SequentialTileGridLoader loader =
            new SequentialTileGridLoader(4, 4, 1, "F_{ppp}.tif", origin, dir);
        loader.printNumberGrid();
        System.out.println();
      }
      System.out.println();
    }


    Log.msg(LogType.MANDATORY, "Test Complete");
  }

  /**
   * Executes the test case
   *
   * @param args not used
   */
  public static void main(String args[]) {
    TestCreateGrid.runTestCreateGrid();
  }
}
