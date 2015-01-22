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
// Date: Jul 2, 2014 1:04:09 PM EST
//
// Time-stamp: <Jul 2, 2014 1:04:09 PM tjb3>
//
//
// ================================================================

package main.gov.nist.isg.mist.stitching.lib.tilegrid.loader;

import main.gov.nist.isg.mist.stitching.lib.log.Log;
import main.gov.nist.isg.mist.stitching.lib.log.Log.LogType;

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
      "<html>Format example:<br> File name = F_r01_c01_time02.tif"
          + "<br>Format = F_r{rr}_c{cc}_time{tt}.tif"
          + "<br>{rr} = row; {cc} = column; {tt} = timeslice (optional)</html>";


  private GridOrigin origin;
  private String rowMatcher;

  /**
   * Initializes the row column tile grid loader
   * 
   * @param gridWidth the width of the grid
   * @param gridHeight the height of the grid
   * @param startTile the start tile number
   * @param filePattern the file pattern
   * @param origin the grid origin
   */
  public RowColTileGridLoader(int gridWidth, int gridHeight, int startTile, String filePattern,
      GridOrigin origin) {
    super(gridWidth, gridHeight, startTile, filePattern);
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
      String colPattern = String.format(this.rowMatcher, row + super.getStartTile());

      String colMatcher = getColMatcher(colPattern, false);

      int gridCol = startCol;
      for (int col = 0; col < super.getGridWidth(); col++) {
        String fileName = String.format(colMatcher, col + super.getStartTile());

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
   * @param silent whether to display errors or not
   * @return the file pattern string
   */
  public static String getRowMatcher(String filePattern, boolean silent) {
    return TileGridLoaderUtils.getPattern(filePattern, rowPattern, silent);    
  }

  /**
   * Gets the column matcher string
   * 
   * @param filePattern the file pattern
   * @param silent whether to show errors or not
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
      RowColTileGridLoader loader = new RowColTileGridLoader(10, 10, 0, "F_{rr}_{cc}.tif", origin);
      loader.printNumberGrid();
      System.out.println();
    }
    System.out.println();
  }

}
