// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Jul 2, 2014 1:04:09 PM EST
//
// Time-stamp: <Jul 2, 2014 1:04:09 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.tilegrid.loader;

import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;

/**
 * Row column tile grid loader
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class RowColTileGridLoader extends TileGridLoader {
  /**
   * The row regex pattern
   */
  public static final String rowPattern = "(.*)(\\{[r]+\\})(.*)";

  /**
   * The column regex pattern
   */
  public static final String colPattern = "(.*)(\\{[c]+\\})(.*)";

  /**
   * Pattern example text
   */
  public static final String patternExample =
      "<html>Format example:<br> File name = img_r01_c01_time02.tif"
          + "<br>Format = img_r{rr}_c{cc}_time{tt}.tif"
          + "<br>{rr} = row; {cc} = column; {tt} = timeslice (optional)</html>";


  private GridOrigin origin;
  private String rowMatcher;

  /**
   * Initializes the row column tile grid loader
   *
   * @param gridWidth   the width of the grid
   * @param gridHeight  the height of the grid
   * @param startTile   the start tile number
   * @param startTileRow   the start tile number
   * @param startTileCol   the start tile number
   * @param filePattern the file pattern
   * @param origin      the grid origin
   */
  public RowColTileGridLoader(int gridWidth, int gridHeight, int startTile, int startTileRow, int startTileCol, String filePattern,
                              GridOrigin origin) {
    super(gridWidth, gridHeight, startTile, startTileRow, startTileCol, filePattern);
    this.origin = origin;

    initRowMatcher();

    buildGrid();

  }

  @Override
  public String toString() {
    String ret = super.toString();

    return ret + " Grid origin: " + this.origin + " row matcher: " + this.rowMatcher;
  }

  private void initRowMatcher() {
    this.rowMatcher = getRowMatcher(super.getFilePattern(), false);
  }


  @Override
  public void buildGrid() {
    int startRow = 0;
    int startCol = 0;

    int rowIncrementer = 0;
    int colIncrementer = 0;

    switch (this.origin) {
      case UR:
        startCol = super.getGridWidth() - 1;
        startRow = 0;

        colIncrementer = -1;
        rowIncrementer = 1;
        break;
      case LL:
        startCol = 0;
        startRow = super.getGridHeight() - 1;

        colIncrementer = 1;
        rowIncrementer = -1;
        break;
      case LR:
        startCol = super.getGridWidth() - 1;
        startRow = super.getGridHeight() - 1;

        colIncrementer = -1;
        rowIncrementer = -1;
        break;
      case UL:
        startCol = 0;
        startRow = 0;

        colIncrementer = 1;
        rowIncrementer = 1;
        break;
    }

    int gridRow = startRow;


    for (int row = 0; row < super.getGridHeight(); row++) {
      String colPattern = String.format(this.rowMatcher, row + super.getStartTileRow());

      String colMatcher = getColMatcher(colPattern, false);

      int gridCol = startCol;
      for (int col = 0; col < super.getGridWidth(); col++) {
        String fileName = String.format(colMatcher, col + super.getStartTileCol());

        super.setTileName(gridRow, gridCol, fileName);

        gridCol += colIncrementer;


      }

      gridRow += rowIncrementer;

    }

  }


  /**
   * Gets the row matcher string
   *
   * @param filePattern the file pattern
   * @param silent      whether to display errors or not
   * @return the file pattern string
   */
  public static String getRowMatcher(String filePattern, boolean silent) {
    return TileGridLoaderUtils.getPattern(filePattern, rowPattern, silent);
  }

  /**
   * Gets the column matcher string
   *
   * @param filePattern the file pattern
   * @param silent      whether to show errors or not
   * @return the column matcher string
   */
  public static String getColMatcher(String filePattern, boolean silent) {
    return TileGridLoaderUtils.getPattern(filePattern, colPattern, silent);
  }

  /**
   * Row column tile grid tester
   *
   * @param args not used
   */
  public static void main(String[] args) {
    Log.setLogLevel(LogType.HELPFUL);

    for (GridOrigin origin : GridOrigin.values()) {
      System.out.println("Origin: " + origin);
      RowColTileGridLoader loader = new RowColTileGridLoader(10, 10, 0, 0, 0, "F_{rr}_{cc}.tif", origin);
      loader.printNumberGrid();
      System.out.println();
    }
    System.out.println();
  }

}
