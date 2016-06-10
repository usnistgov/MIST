// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Jul 2, 2014 1:07:21 PM EST
//
// Time-stamp: <Jul 2, 2014 1:07:21 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.tilegrid.loader;

import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;

/**
 * Sequential tile grid loader
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class SequentialTileGridLoader extends TileGridLoader {
  /**
   * The position regex pattern
   */
  public static final String positionPattern = "(.*)(\\{[p]+\\})(.*)";

  /**
   * The pattern example
   */
  public static final String patternExample =
      "<html>Format example:<br> File name = img_pos1234_time1234.tif"
          + "<br>Format = img_pos{pppp}_time{tttt}.tif"
          + "<br>{pppp} = position; {tttt} = timeslice(optional)</html>";


  private GridOrigin origin;
  private GridDirection direction;
  private String nameMatcher;

  /**
   * Creates a sequential tile grid loader
   *
   * @param gridWidth   the width of the grid
   * @param gridHeight  the height of the grid
   * @param startTile   the start tile index
   * @param filePattern the file pattern
   * @param origin      the grid origin
   * @param direction   the grid traversal direction
   */
  public SequentialTileGridLoader(int gridWidth, int gridHeight, int startTile, String filePattern,
                                  GridOrigin origin, GridDirection direction) {
    super(gridWidth, gridHeight, startTile, filePattern);
    this.origin = origin;
    this.direction = direction;

    initNameMatcher();

    buildGrid();

  }

  @Override
  public String toString() {
    String ret = super.toString();

    return ret + " Grid origin: " + this.origin + " direction: " + this.direction + " name matcher: "
        + this.nameMatcher;
  }

  private void initNameMatcher() {
    this.nameMatcher = getPositionPattern(super.getFilePattern(), false);
  }

  @Override
  public void buildGrid() {


    switch (this.direction) {
      case HORIZONTALCOMBING:
        fillNumberingByRow();
        break;
      case HORIZONTALCONTINUOUS:
        fillNumberingByRowChained();
        break;
      case VERTICALCOMBING:
        fillNumberingByColumn();
        break;
      case VERTICALCONTINUOUS:
        fillNumberingByColumnChained();
    }

    switch (this.origin) {
      case UR:
        reflectNumberingVertical();
        break;
      case LL:
        reflectNumberingHorizontal();
        break;
      case LR:
        reflectNumberingVertical();
        reflectNumberingHorizontal();
        break;
      case UL:
    }

  }


  /**
   * Reflects grid along vertical axis
   */
  private void reflectNumberingVertical() {
    for (int r = 0; r < super.getGridHeight(); r++) {

      for (int c = 0; c < super.getGridWidth() / 2; c++) {
        String temp = super.getTileName(r, c);

        super.setTileName(r, c, super.getTileName(r, super.getGridWidth() - c - 1));
        super.setTileName(r, super.getGridWidth() - c - 1, temp);

      }
    }
  }

  /**
   * Reflects grid along horizontal axis
   */
  private void reflectNumberingHorizontal() {
    for (int r = 0; r < super.getGridHeight() / 2; r++) {
      int rowOffset = (super.getGridHeight() - r - 1);

      for (int c = 0; c < super.getGridWidth(); c++) {
        String temp = super.getTileName(r, c);

        super.setTileName(r, c, super.getTileName(rowOffset, c));
        super.setTileName(rowOffset, c, temp);
      }
    }
  }

  /**
   * Fill numbering by row chained
   */
  private void fillNumberingByRowChained() {
    fillNumberingByRow();

    for (int r = 1; r < super.getGridHeight(); r += 2) {
      for (int c = 0; c < super.getGridWidth() / 2; c++) {
        String temp = super.getTileName(r, c);

        super.setTileName(r, c, super.getTileName(r, super.getGridWidth() - c - 1));
        super.setTileName(r, super.getGridWidth() - c - 1, temp);

      }
    }
  }

  /**
   * Fill numbering by column chained
   */
  private void fillNumberingByColumnChained() {
    fillNumberingByColumn();

    for (int r = 0; r < super.getGridHeight() / 2; r++) {
      int rowOffset = (super.getGridHeight() - r - 1);

      for (int c = 1; c < super.getGridWidth(); c += 2) {
        String temp = super.getTileName(r, c);

        super.setTileName(r, c, super.getTileName(rowOffset, c));
        super.setTileName(rowOffset, c, temp);
      }
    }
  }

  /**
   * Fill numbering by column
   */
  private void fillNumberingByColumn() {
    for (int r = 0; r < super.getGridHeight(); r++) {
      int val = r + super.getStartTile();

      for (int c = 0; c < super.getGridWidth(); c++) {
        String fileName = String.format(this.nameMatcher, val);
        super.setTileName(r, c, fileName);
        val += super.getGridHeight();
      }
    }
  }

  /**
   * Fill numbering by row
   */
  private void fillNumberingByRow() {
    for (int r = 0; r < super.getGridHeight(); r++) {
      for (int c = 0; c < super.getGridWidth(); c++) {
        int index = r * super.getGridWidth() + c;

        String fileName = String.format(this.nameMatcher, index + super.getStartTile());
        super.setTileName(r, c, fileName);
      }
    }
  }


  /**
   * Tests the sequential tile grid loader
   *
   * @param args not used
   */
  public static void main(String[] args) {
    Log.setLogLevel(LogType.HELPFUL);

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

  }

  /**
   * Gets the position pattern
   *
   * @param filePattern the file pattern
   * @param silent      whether to show error or not
   * @return the position pattern
   */
  public static String getPositionPattern(String filePattern, boolean silent) {
    return TileGridLoaderUtils.getPattern(filePattern, positionPattern, silent);
  }


}
