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
// Date: Aug 1, 2013 3:41:17 PM EST
//
// Time-stamp: <Aug 1, 2013 3:41:17 PM tjb3>
//
//
// ================================================================

package main.gov.nist.isg.mist.stitching.lib.tilegrid.traverser;

/**
 * Traversal interface, describing a traverser
 * 
 * @author Tim Blattner
 * @version 1.0
 * @param <T>
 */
public interface TileGridTraverser<T> extends Iterable<T> {

  /**
   * Different types of traversers
   */
  public static enum Traversals {
    /**
     * Row traversal (combed)
     */
    ROW,

    /**
     * Row chained traversal
     */
    ROW_CHAINED,

    /**
     * Column traversal (combed)
     */
    COLUMN,

    /**
     * Column chained traversal
     */
    COLUMN_CHAINED,

    /**
     * Diagonal traversal
     */
    DIAGONAL,

    /**
     * Diagonal chained traversal
     */
    DIAGONAL_CHAINED
  }

  /**
   * Gets the current row of the traverser
   * 
   * @return the current row of the traverser
   */
  public int getCurrentRow();

  /**
   * Gets the current column of the traverser
   * 
   * @return the current column of hte traverser
   */
  public int getCurrentColumn();

}
