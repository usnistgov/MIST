// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 3:39:26 PM EST
//
// Time-stamp: <Aug 1, 2013 3:39:26 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.tilegrid.traverser;


import java.util.Iterator;

import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;

/**
 * Traversal type for traversing by diagonal
 *
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
