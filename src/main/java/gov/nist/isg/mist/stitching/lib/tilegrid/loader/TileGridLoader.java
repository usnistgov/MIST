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
// Date: Jul 2, 2014 1:09:02 PM EST
//
// Time-stamp: <Jul 2, 2014 1:09:02 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.tilegrid.loader;

import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;

/**
 * Tile grid loader abstract class
 * 
 * @author Tim Blattner
 * @version 1.0
 */
public abstract class TileGridLoader {

  /**
   * Tile grid loader types
   * 
   * @author Tim Blattner
   * @version 1.0
   */
  public enum LoaderType {
    /**
     * Sequential loader
     */
    SEQUENTIAL("Sequential"),

    /**
     * Row-column loader
     */
    ROWCOL("Row-Column");

    private String name;

    private LoaderType(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }


  }

  /**
   * Gets the grid origin, setup during image acquisition
   */
  public static enum GridOrigin {
    /**
     * Acquisition starts in the upper left
     */
    UL("Upper Left"),

    /**
     * Acquisition starts in the upper right
     */
    UR("Upper Right"),

    /**
     * Acquisition starts in the lower left
     */
    LL("Lower Left"),

    /**
     * Acquisition starts in the lower right
     */
    LR("Lower Right");
    private GridOrigin(final String text) {
      this.text = text;
    }

    private final String text;

    @Override
    public String toString() {
      return this.text;
    }

  }

  /**
   * Gets the grid numbering scheme, setup during image acquisition
   */
  public static enum GridDirection {
    /**
     * Acquisition numbers based on column (combed)
     */
    VERTICALCOMBING("Vertical Combing"),

    /**
     * Acquisition numbers based on column chained
     */
    VERTICALCONTINUOUS("Vertical Continuous"),

    /**
     * Acquisition numbers based on row (combed)
     */

    HORIZONTALCOMBING("Horizontal Combing"),
    /**
     * Acquisition numbers based on row chained
     */
    HORIZONTALCONTINUOUS("Horizontal Continuous"),


    ;

    private GridDirection(final String text) {
      this.text = text;
    }

    private final String text;

    @Override
    public String toString() {
      return this.text;
    }
  }


  private String[][] tileNames;
  private int gridWidth;
  private int gridHeight;
  private int startTile;
  private String filePattern;


  /**
   * Constructs a tile grid loader
   * 
   * @param gridWidth the width of the grid
   * @param gridHeight the height of the grid
   * @param startTile the start tile index
   * @param filePattern the file pattern
   */
  public TileGridLoader(int gridWidth, int gridHeight, int startTile, String filePattern) {
    this.gridWidth = gridWidth;
    this.gridHeight = gridHeight;
    this.startTile = startTile;
    this.filePattern = filePattern;
    this.tileNames = new String[gridHeight][gridWidth];
  }

  @Override
  public String toString() {
    return "Grid width: " + this.gridWidth + " gridHeight: " + this.gridHeight + " startTile: " + this.startTile
        + " filePattern: " + this.filePattern;
  }


  /**
   * Gets the tile name for a given row, column
   * 
   * @param row the row in the grid
   * @param col the column in the grid
   * @return the name of the tile
   */
  public String getTileName(int row, int col) {
    return this.tileNames[row][col];
  }

  /**
   * Sets the tile name for a given row, column
   * 
   * @param row the row in the grid
   * @param col the column in the grid
   * @param name the name of the tile
   */
  public void setTileName(int row, int col, String name) {
    this.tileNames[row][col] = name;
  }

  /**
   * Prints the grid of tiles
   */
  public void printNumberGrid() {
    for (int i = 0; i < this.gridHeight; i++) {
      for (int j = 0; j < this.gridWidth; j++) {
        Log.msgnonlNoTime(LogType.HELPFUL, getTileName(i, j) + " ");
      }
      Log.msgNoTime(LogType.HELPFUL, "");
    }
  }

  /**
   * @return the gridWidth
   */
  public int getGridWidth() {
    return this.gridWidth;
  }

  /**
   * @return the gridHeight
   */
  public int getGridHeight() {
    return this.gridHeight;
  }

  /**
   * @return the startTile
   */
  public int getStartTile() {
    return this.startTile;
  }

  /**
   * @return the filePattern
   */
  public String getFilePattern() {
    return this.filePattern;
  }

  /**
   * Constructs the grid of tiles
   */
  public abstract void buildGrid();


}
