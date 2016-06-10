// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 3:47:45 PM EST
//
// Time-stamp: <Aug 1, 2013 3:47:45 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.tilegrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gov.nist.isg.mist.lib.common.CorrelationTriple;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.log.Debug;
import gov.nist.isg.mist.lib.log.Debug.DebugType;
import gov.nist.isg.mist.lib.log.Log;

/**
 * This class provides various utility functions for working with ImageTiles. For example:
 * traverseMaximumSpanningTree updates the absolute positions of a grid based on the correlation
 * coefficients. <p> Also provided are utility functions for initializing a numbering grid.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TileGridUtils {

  private static int[] dx = {0,-1,1,0};
  private static int[] dy = {-1,0,0,1};

  /**
   * Traverses the maximum spanning tree of the grid based on correlation coefficient. Each each
   * step it computes the absolute position relative to the edge taken.
   *
   * @param grid the grid of image tiles to traverse
   */
  public static <T> void traverseMaximumSpanningTree(TileGrid<ImageTile<T>> grid) {
    ImageTile<T> startTile = null;


    Log.msg(Log.LogType.INFO, "Starting MST traversal");
    boolean[][] visitedTilesArray = new boolean[grid.getExtentHeight()][grid.getExtentWidth()];

    // Find tile that has highest correlation
    for (int row = 0; row < grid.getExtentHeight(); row++) {
      for (int col = 0; col < grid.getExtentWidth(); col++) {
        // init visited tiles array to false
        visitedTilesArray[row][col] = false;

        ImageTile<T> tile = grid.getSubGridTile(row, col);

        // init each image tiles MST connection count to its required value
        tile.setMstReleaseCount(tile.getReleaseCount(grid.getFullWidth(), grid
            .getFullHeight(), grid.getStartRow(), grid.getStartCol()));

        tile.setAbsXPos(0);
        tile.setAbsYPos(0);
        if (startTile == null)
          startTile = tile;
        else if (tile.getTileCorrelation() > startTile.getTileCorrelation())
          startTile = tile;
      }
    }


    List<ImageTile<T>> frontierTiles = new ArrayList<ImageTile<T>>();
    frontierTiles.add(startTile);
    // increment MST counter for all adjacent tiles so we can skip those tiles that have no
    // non-connected neighbors
    for (int k = 0; k < dx.length; k++) {
      int i = startTile.getRow() - grid.getStartRow() + dy[k];
      int j = startTile.getCol() - grid.getStartCol() + dx[k];
      if (i >= 0 && i < grid.getExtentHeight() && j >= 0 && j < grid.getExtentWidth()) {
        grid.getSubGridTile(i,j).decrementMstConnectedNeighborCount();
      }
    }

    // set the flag to indicate that the start tile has been added to the MST
    visitedTilesArray[startTile.getRow() - grid.getStartRow()][startTile.getCol() - grid.getStartCol()] = true;
    int mstSize = 1; // current size is 1 b/c startTile has been added

    while (mstSize < grid.getExtentHeight() * grid.getExtentWidth()) {
      mstSize = traverseNextMSTTile(grid, frontierTiles, visitedTilesArray, mstSize);
    }

    Log.msg(Log.LogType.INFO, "Completed MST traversal");
    translateTranslations(grid);

  }

  /**
   * Traverses to the next tile in the minimum spanning tree
   *
   * @param grid         the grid of image tiles
   * @param frontierTiles the set of visited tiles
   * @param visitedTilesArray 2D array of booleans indicating which tiles have been added to MSR
   */
  public static <T> int traverseNextMSTTile(TileGrid<ImageTile<T>> grid,
                                              List<ImageTile<T>> frontierTiles, boolean[][]
                                                  visitedTilesArray, int mstSize) {

    ImageTile<T> origin = null;
    ImageTile<T> next = null;
    double bestCorr = Double.NEGATIVE_INFINITY;

    int gridWidth = grid.getExtentWidth();
    int gridHeight = grid.getExtentHeight();

    // loop over all tiles currently in the MST and find the neighbor with the highest correlation
    for (ImageTile<T> tile : frontierTiles) {
      int row = tile.getRow() - grid.getStartRow();
      int col = tile.getCol() - grid.getStartCol();

      // check all neighbors and find best neighbor
      for (int k = 0; k < dx.length; k++) {
        int i = row + dy[k];
        int j = col + dx[k];

        if (i >= 0 && i < gridHeight && j >= 0 && j < gridWidth) {
          // If this tile is not in visited tiles
          if (!visitedTilesArray[i][j]) {
            ImageTile<T> neighbor = grid.getSubGridTile(i, j);
            double edgeWeight = tile.getCorr(neighbor);

            if (edgeWeight > bestCorr) {
              bestCorr = edgeWeight;
              origin = tile;
              next = neighbor;
            }
          }
        }
      }
    }

    if (origin == null)
      return mstSize;

    if (next == null)
      return mstSize;

    next.updateAbsolutePosition(origin);
    frontierTiles.add(next);
    mstSize++;
    // increment MST counter for all adjacent tiles so we can skip those tiles that have no
    // non-connected neighbors (update the frontier)
    int row = next.getRow() - grid.getStartRow();
    int col = next.getCol() - grid.getStartCol();
    for (int k = 0; k < dx.length; k++) {
      int i = row + dy[k];
      int j = col + dx[k];
      if (i >= 0 && i < gridHeight && j >= 0 && j < gridWidth) {
        grid.getSubGridTile(i,j).decrementMstConnectedNeighborCount();
      }
    }
    // set the flag to indicate that this tile has been added to the MST, this avoids performing
    // a contains operation on visitedTiles
    visitedTilesArray[row][col] = true;

    // purge visited tiles list of entries that are no longer on the frontier
    List<Integer> toRemove = new ArrayList<Integer>();
    for(int i = 0; i < frontierTiles.size(); i++) {
      if(frontierTiles.get(i).getMstReleaseCount() == 0)
        toRemove.add(i); // if there are no potential connections left, remove from the frontier
    }
    Collections.reverse(toRemove); // reverse order to enable removal one at a time
    for(int val : toRemove) {
      frontierTiles.remove(val);
    }

    Debug.msg(DebugType.VERBOSE,
        "Origin: " + origin.getFileName() + " visited: " + next.getFileName());

    return mstSize;
  }


  /**
   * Translates all vertices in the grid by the minX and minY values of the entire grid.
   *
   * @param grid the grid of image tiles
   */
  public static <T> void translateTranslations(TileGrid<ImageTile<T>> grid) {
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    for (int r = 0; r < grid.getExtentHeight(); r++) {
      for (int c = 0; c < grid.getExtentWidth(); c++) {
        ImageTile<T> t = grid.getSubGridTile(r, c);
        if (minX > t.getAbsXPos())
          minX = t.getAbsXPos();

        if (minY > t.getAbsYPos())
          minY = t.getAbsYPos();
      }
    }

    for (int r = 0; r < grid.getExtentHeight(); r++) {
      for (int c = 0; c < grid.getExtentWidth(); c++) {
        ImageTile<T> t = grid.getSubGridTile(r, c);

        t.subractAbsPos(minX, minY);
      }
    }
  }

  /**
   * Gets the max correlation inside of the entire tile grid
   *
   * @param grid the grid of image tiles
   * @return the highest correlation
   */
  public static <T> double getMaxCorrelation(TileGrid<ImageTile<T>> grid) {

    double maxCorr = Double.MIN_VALUE;
    for (int r = 0; r < grid.getExtentHeight(); r++) {
      for (int c = 0; c < grid.getExtentWidth(); c++) {
        ImageTile<?> tile = grid.getSubGridTile(r, c);

        CorrelationTriple north = tile.getNorthTranslation();

        if (north != null && north.getCorrelation() > maxCorr)
          maxCorr = north.getCorrelation();

        CorrelationTriple west = tile.getNorthTranslation();

        if (west != null && west.getCorrelation() > maxCorr)
          maxCorr = west.getCorrelation();

      }

    }

    return maxCorr;
  }

  /**
   * Gets the width of the entire grid
   *
   * @param grid     the grid of image tiles
   * @param imgWidth the width of an image
   * @return the width of the full grid
   */
  public static <T> int getFullImageWidth(TileGrid<ImageTile<T>> grid, int imgWidth) {
    int width = Integer.MIN_VALUE;
    for (int r = 0; r < grid.getExtentHeight(); r++) {
      for (int c = 0; c < grid.getExtentWidth(); c++) {
        ImageTile<?> tile = grid.getSubGridTile(r, c);
        int tileEndX = tile.getAbsXPos() + imgWidth;
        if (tileEndX > width)
          width = tileEndX;
      }

    }

    return width;
  }

  /**
   * Gets the height of the entire grid
   *
   * @param grid      the grid of image tiles
   * @param imgHeight the height of an image
   * @return the height of the full grid
   */
  public static <T> int getFullImageHeight(TileGrid<ImageTile<T>> grid, int imgHeight) {
    int height = Integer.MIN_VALUE;
    for (int r = 0; r < grid.getExtentHeight(); r++) {
      for (int c = 0; c < grid.getExtentWidth(); c++) {
        ImageTile<?> tile = grid.getSubGridTile(r, c);
        int tileEndY = tile.getAbsYPos() + imgHeight;

        if (tileEndY > height)
          height = tileEndY;
      }
    }
    return height;
  }

  /**
   * Releases the entire grid of tiles.
   *
   * @param grid the grid of image tiles
   */
  public static <T> void releaseTiles(TileGrid<ImageTile<T>> grid) {
    for (ImageTile<T>[] tileA : grid.getTiles()) {
      for (ImageTile<T> tile : tileA) {
        tile.releasePixelsNow();
      }

    }

    System.gc();
  }

  /**
   * Prints the values of a tile grid in a grid format similar to how Matlab displays values. Used
   * for comparison of Matlab and Java
   *
   * @param grid    the grid of image tiles
   * @param dir     the direction (north or west) you want to display
   * @param dispVal the displacement value that is to be printed (X, Y, C)
   */
  public static <T> void printGrid(TileGrid<ImageTile<T>> grid, TileGrid.Direction dir,
                                   TileGrid.DisplacementValue dispVal) {
    Log.msgNoTime(Log.LogType.MANDATORY, dir.name() + " : " + dispVal.name());

    for (int row = 0; row < grid.getExtentHeight(); row++) {
      for (int col = 0; col < grid.getExtentWidth(); col++) {
        ImageTile<T> tile = grid.getSubGridTile(row, col);

        CorrelationTriple triple = tile.getTranslation(dir);

        String val = "0.0";

        if (triple != null) {
          switch (dispVal) {
            case X:
              val = triple.getMatlabFormatStrX();
              break;
            case Y:
              val = triple.getMatlabFormatStrY();
              break;
            default:
              break;

          }
        }

        Log.msgnonlNoTime(Log.LogType.MANDATORY, val + (col == grid.getExtentWidth() - 1 ? "" : ","));

      }
      Log.msgNoTime(Log.LogType.MANDATORY, "");
    }

  }

  /**
   * Copies north and western translations into 'preOptimization' north and western translations.
   * This saves the translations before optimization
   *
   * @param grid the grid of image tiles
   */
  public static <T> void backupTranslations(TileGrid<ImageTile<T>> grid) {
    Log.msg(Log.LogType.VERBOSE, "Backing up translations");

    for (int r = 0; r < grid.getExtentHeight(); r++) {
      for (int c = 0; c < grid.getExtentWidth(); c++) {
        ImageTile<T> tile = grid.getSubGridTile(r, c);
        if (tile.getNorthTranslation() != null)
          tile.setPreOptimizationNorthTranslation(tile.getNorthTranslation().clone());
        if (tile.getWestTranslation() != null)
          tile.setPreOptimizationWestTranslation(tile.getWestTranslation().clone());
      }
    }
  }
}
