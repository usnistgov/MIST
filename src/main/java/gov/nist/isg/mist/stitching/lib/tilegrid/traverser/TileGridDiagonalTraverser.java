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
// Date: Aug 1, 2013 3:39:26 PM EST
//
// Time-stamp: <Aug 1, 2013 3:39:26 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.tilegrid.traverser;


import java.util.Iterator;

import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;

/**
 * Traversal type for traversing by diagonal
 *
 * @param <T>
 * @author Tim Blattner
 * @version 1.0
 */
public class TileGridDiagonalTraverser<T extends ImageTile<?>> implements TileGridTraverser<T>, Iterator<T> {

    private TileGrid<T> subGrid;
    private int currentRowPosition;
    private int currentColumnPosition;

    private int linear;

    private int diagRow;
    private int diagCol;
    private int diagDirRow;
    private int diagDirCol;

    /**
     * Initializes a diagonal traverser given a subgrid
     *
     * @param subgrid
     */
    public TileGridDiagonalTraverser(TileGrid<T> subgrid) {
        this.subGrid = subgrid;
        this.currentRowPosition = 0;
        this.currentColumnPosition = 0;
        this.linear = 0;

        this.diagRow = 0;
        this.diagCol = 0;
        this.diagDirRow = 0;
        this.diagDirCol = 1;
    }

    @Override
    public Iterator<T> iterator() {
        return this;
    }

    @Override
    public int getCurrentRow() {
        return this.currentRowPosition;
    }

    @Override
    public int getCurrentColumn() {
        return this.currentColumnPosition;
    }

    @Override
    public String toString() {
        return "Traversing by diagonal: " + this.subGrid;
    }


    @Override
    public boolean hasNext() {
        return this.linear < this.subGrid.getSubGridSize();
    }

    @Override
    public T next() {
        T actualTile =
                this.subGrid.getTile(this.currentRowPosition + this.subGrid.getStartRow(), this.currentColumnPosition
                        + this.subGrid.getStartCol());

        this.linear++;

        this.currentRowPosition++;
        this.currentColumnPosition--;

        if (hasNext()) {
            if (this.currentColumnPosition < 0 || this.currentRowPosition >= this.subGrid.getExtentHeight()) {
                int diagNextRow = this.diagRow + this.diagDirRow;
                int diagNextCol = this.diagCol + this.diagDirCol;

                if (diagNextCol == this.subGrid.getExtentWidth()) {
                    this.diagDirRow = 1;
                    this.diagDirCol = 0;

                    diagNextRow = this.diagRow + this.diagDirRow;
                    diagNextCol = this.diagCol + this.diagDirCol;
                }

                this.diagRow = diagNextRow;
                this.diagCol = diagNextCol;

                this.currentRowPosition = this.diagRow;
                this.currentColumnPosition = this.diagCol;

            }
        }

        return actualTile;
    }

    @Override
    public void remove() {
        // Not implemented/not needed
    }
}
