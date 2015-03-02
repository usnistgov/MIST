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
// Date: Jul 2, 2014 1:07:21 PM EST
//
// Time-stamp: <Jul 2, 2014 1:07:21 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.tilegrid.loader;

import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;

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
      "<html>Format example:<br> File name = F_pos1234_time1234.tif"
          + "<br>Format = F_pos{pppp}_time{tttt}.tif"
          + "<br>{pppp} = position; {tttt} = timeslice(optional)</html>";



  private GridOrigin origin;
  private GridDirection direction;
  private String nameMatcher;

  /**
   * Creates a sequential tile grid loader
   * 
   * @param gridWidth the width of the grid
   * @param gridHeight the height of the grid
   * @param startTile the start tile index
   * @param filePattern the file pattern
   * @param origin the grid origin
   * @param direction the grid traversal direction
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
   * @param silent whether to show error or not
   * @return the position pattern
   */
  public static String getPositionPattern(String filePattern, boolean silent) {
    return TileGridLoaderUtils.getPattern(filePattern, positionPattern, silent);    
    }



}
