// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 3:41:17 PM EST
//
// Time-stamp: <Aug 1, 2013 3:41:17 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.tilegrid.traverser;

/**
 * Traversal interface, describing a traverser
 *
 * @author Tim Blattner
 * @version 1.0
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
