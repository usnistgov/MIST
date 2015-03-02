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
// Date: Aug 1, 2013 3:39:51 PM EST
//
// Time-stamp: <Aug 1, 2013 3:39:51 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.tilegrid.traverser;

import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;

import java.util.Iterator;

/**
 * Traversal type for traversing a grid row chained
 * 
 * @author Tim Blattner
 * @version 1.0
 * @param <T>
 */
public class TileGridRowChainedTraverser<T> implements TileGridTraverser<ImageTile<T>>, Iterator<ImageTile<T>> {

  private TileGrid<ImageTile<T>> subGrid;
  private int currentRowPosition;
  private int currentColumnPosition;
  private int linear;

  private int dir;
  /**
   * Initializes a row-chained traverser given a subgrid
   * 
   * @param subgrid
   */
  public TileGridRowChainedTraverser(TileGrid<ImageTile<T>> subgrid) {
    this.subGrid = subgrid;
    this.currentRowPosition = 0;
    this.currentColumnPosition = 0;
    this.linear = 0;

    this.dir = 1;
  }

  @Override
  public Iterator<ImageTile<T>> iterator() {
    return this;
  }

  /**
   * Gets the current row of the traverser
   */
  @Override
  public int getCurrentRow() {
    return this.currentRowPosition;
  }

  /**
   * Gets the current column of the traverser
   */
  @Override
  public int getCurrentColumn() {
    return this.currentColumnPosition;
  }

  @Override
  public String toString() {
    return "Traversing by row chained: " + this.subGrid;
  }
  

  @Override
  public boolean hasNext() {
    return this.linear < this.subGrid.getSubGridSize();
  }

  @Override
  public ImageTile<T> next() {
    int tempColPos = this.currentColumnPosition;

    ImageTile<T> actualTile =
        this.subGrid.getTile(this.currentRowPosition + this.subGrid.getStartRow(), this.currentColumnPosition
            + this.subGrid.getStartCol());

    this.linear++;

    this.currentColumnPosition += this.dir;

    if (this.currentColumnPosition < 0 || this.currentColumnPosition >= this.subGrid.getExtentWidth()) {
      this.dir = -this.dir;
      this.currentRowPosition++;
      this.currentColumnPosition = tempColPos;
    }

    return actualTile;
  }

  @Override
  public void remove() {
    // Not implemented/not needed
  }

}
