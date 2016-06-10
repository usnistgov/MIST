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
// Date: Aug 1, 2013 3:39:44 PM EST
//
// Time-stamp: <Aug 1, 2013 3:39:44 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.tilegrid.traverser;


import java.util.Iterator;

import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;

/**
 * Traversal type for traversing a grid column chained
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TileGridColumnChainedTraverser<T extends ImageTile<?>> implements TileGridTraverser<T>, Iterator<T> {

  private TileGrid<T> subGrid;
  private int currentRowPosition;
  private int currentColumnPosition;

  private int linear;
  private int dir;

  /**
   * Initializes a column-chained traverser given a subgrid
   */
  public TileGridColumnChainedTraverser(TileGrid<T> subgrid) {
    this.subGrid = subgrid;
    this.currentRowPosition = 0; // subGrid.getStartRow();
    this.currentColumnPosition = 0; // subGrid.getStartCol();
    this.linear = 0;
    this.dir = 0;
  }

  @Override
  public Iterator<T> iterator() {
    return this;
  }

  /**
   * Gets the current row for the traverser
   */
  @Override
  public int getCurrentRow() {
    return this.currentRowPosition;
  }

  /**
   * Gets the current column for the traverser
   */
  @Override
  public int getCurrentColumn() {
    return this.currentColumnPosition;
  }

  @Override
  public String toString() {
    return "Traversing by column chained: " + this.subGrid;
  }

  @Override
  public boolean hasNext() {
    return this.linear < this.subGrid.getSubGridSize();
  }

  @Override
  public T next() {
    int tempRowPos = this.currentRowPosition;

    T actualTile =
        this.subGrid.getTile(this.currentRowPosition + this.subGrid.getStartRow(), this.currentColumnPosition
            + this.subGrid.getStartCol());

    this.linear++;

    this.currentRowPosition += this.dir;

    if (this.currentRowPosition < 0 || this.currentRowPosition >= this.subGrid.getExtentHeight()) {
      this.dir = -this.dir;
      this.currentColumnPosition++;
      this.currentRowPosition = tempRowPos;
    }

    return actualTile;
  }

  @Override
  public void remove() {
    // Not implemented/not needed

  }
}
