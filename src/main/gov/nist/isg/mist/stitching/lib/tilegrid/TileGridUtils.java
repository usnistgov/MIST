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
// Date: Aug 1, 2013 3:47:45 PM EST
//
// Time-stamp: <Aug 1, 2013 3:47:45 PM tjb3>
//
//
// ================================================================

package main.gov.nist.isg.mist.stitching.lib.tilegrid;

import java.util.List;
import java.util.TreeSet;

import main.gov.nist.isg.mist.stitching.lib.common.CorrelationTriple;
import main.gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import main.gov.nist.isg.mist.stitching.lib.log.Debug;
import main.gov.nist.isg.mist.stitching.lib.log.Debug.DebugType;

/**
 * This class provides various utility functions for working with ImageTiles. For example:
 * traverseMaximumSpanningTree updates the absolute positions of a grid based on the correlation
 * coefficients.
 * <p>
 * Also provided are utility functions for initializing a numbering grid.
 * 
 * @author Tim Blattner
 * @version 1.0
 */
public class TileGridUtils {

  /**
   * Traverses the maximum spanning tree of the grid based on correlation coefficient. Each each
   * step it computes the absolute position relative to the edge taken.
   * 
   * @param grid the grid of image tiles to traverse
   */
  public static <T> void traverseMaximumSpanningTree(TileGrid<ImageTile<T>> grid) {
    ImageTile<T> startTile = null;

    // Find tile that has highest correlation
    for (int row = 0; row < grid.getExtentHeight(); row++) {
      for (int col = 0; col < grid.getExtentWidth(); col++) {
        ImageTile<T> tile = grid.getSubGridTile(row, col);        
        tile.setAbsXPos(0);
        tile.setAbsYPos(0);
        if (startTile == null)
          startTile = tile;
        else if (tile.getTileCorrelation() > startTile.getTileCorrelation())
          startTile = tile;

      }
    }

    // Traverse until all vertices are visited
    TreeSet<ImageTile<T>> visitedTiles = new TreeSet<ImageTile<T>>();

    visitedTiles.add(startTile);

    while (visitedTiles.size() < grid.getExtentHeight() * grid.getExtentWidth()) {
      traverseNextMSPTile(visitedTiles, grid);
    }

    translateTranslations(grid);

  }

  /**
   * Traverses to the next tile in the minimum spanning tree
   * 
   * @param visitedTiles the set of visited tiles
   * @param grid the grid of image tiles
   */
  public static <T> void traverseNextMSPTile(TreeSet<ImageTile<T>> visitedTiles,
      TileGrid<ImageTile<T>> grid) {
    ImageTile<T> origin = null;
    ImageTile<T> next = null;
    double bestCorr = Double.NEGATIVE_INFINITY;

    for (ImageTile<T> visitedTile : visitedTiles) {
      // check all neighbors and find best neighbor
      List<ImageTile<T>> neighbors = grid.getNeighbors(visitedTile);

      for (ImageTile<T> neighbor : neighbors) {
        if (visitedTiles.contains(neighbor))
          continue;

        double edgeWeight = visitedTile.getCorr(neighbor);

        if (edgeWeight > bestCorr) {
          bestCorr = edgeWeight;
          origin = visitedTile;
          next = neighbor;
        }

      }
    }

    if (origin == null)
      return;
    
    if (next == null)
      return;

    next.updateAbsolutePosition(origin);
    visitedTiles.add(next);

    Debug.msg(DebugType.VERBOSE,
        "Origin: " + origin.getFileName() + " visited: " + next.getFileName());
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
   * 
   * @return the highest correlation
   */
  public static <T> double getMaxCorrelation(TileGrid<ImageTile<T>> grid) {

    double maxCorr = Double.MIN_VALUE;
    for (int r = 0; r < grid.getExtentHeight(); r++) {
      for (int c = 0; c < grid.getExtentWidth(); c++) {
        ImageTile<?> tile = grid.getSubGridTile(r, c);

        CorrelationTriple north = tile.getNorthTranslation();

        if (north != null && north.getCorrelation() > maxCorr)
          maxCorr = tile.getNorthTranslation().getCorrelation();

        CorrelationTriple west = tile.getNorthTranslation();

        if (west != null && west.getCorrelation() > maxCorr)
          maxCorr = tile.getWestTranslation().getCorrelation();

      }

    }

    return maxCorr;
  }

  /**
   * Gets the width of the entire grid
   * 
   * @param grid the grid of image tiles
   * @param imgWidth the width of an image
   * 
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
   * @param grid the grid of image tiles
   * @param imgHeight the height of an image
   * 
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

}
