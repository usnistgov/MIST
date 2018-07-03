// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Jul 2, 2014 1:09:02 PM EST
//
// Time-stamp: <Jul 2, 2014 1:09:02 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.tilegrid.loader;

import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;

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
    HORIZONTALCONTINUOUS("Horizontal Continuous"),;

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
  private int startTileRow;
  private int startTileCol;
  private String filePattern;


  /**
   * Constructs a tile grid loader
   *
   * @param gridWidth   the width of the grid
   * @param gridHeight  the height of the grid
   * @param startTile   the start tile index
   * @param filePattern the file pattern
   */
  public TileGridLoader(int gridWidth, int gridHeight, int startTile, int startTileRow, int startTileCol, String filePattern) {
    this.gridWidth = gridWidth;
    this.gridHeight = gridHeight;
    this.startTile = startTile;
    this.startTileRow = startTileRow;
    this.startTileCol = startTileCol;
    this.filePattern = filePattern;
    this.tileNames = new String[gridHeight][gridWidth];
  }

  @Override
  public String toString() {
    return "Grid width: " + this.gridWidth + " gridHeight: " + this.gridHeight + " startTile: " + this.startTile
    + " startTileRow: " + this.startTileRow + " startTileCol: " + this.startTileCol + " filePattern: " + this.filePattern;
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
   * @param row  the row in the grid
   * @param col  the column in the grid
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
   * @return the startTileRow
   */
  public int getStartTileRow() {
    return this.startTileRow;
  }

  /**
   * @return the startTileCol
   */
  public int getStartTileCol() {
    return this.startTileCol;
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
