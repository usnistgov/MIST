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


// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 3:47:52 PM EST
//
// Time-stamp: <Aug 1, 2013 3:47:52 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.tilegrid.traverser;


import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;

/**
 * Traversal utility function for creating a traversal based on a type bound to a grid of tiles.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TileGridTraverserFactory {

  /**
   * Generates a traversal surrounding a tile grid
   *
   * @param type the type of traversal
   * @param grid the subgrid to traverse
   * @return the traverser
   */
  public static <T extends ImageTile<?>> TileGridTraverser<T> makeTraverser(
      TileGridTraverser.Traversals type, TileGrid<T> grid) {
    switch (type) {
      case ROW:
        return new TileGridRowTraverser<T>(grid);
      case COLUMN:
        return new TileGridColumnTraverser<T>(grid);
      case COLUMN_CHAINED:
        return new TileGridColumnChainedTraverser<T>(grid);
      case DIAGONAL:
        return new TileGridDiagonalTraverser<T>(grid);
      case DIAGONAL_CHAINED:
        return new TileGridDiagonalChainedTraverser<T>(grid);
      case ROW_CHAINED:
        return new TileGridRowChainedTraverser<T>(grid);
      default:
        return new TileGridRowTraverser<T>(grid);

    }
  }

}
