// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 1:12:06 PM EST
//
// Time-stamp: <Aug 1, 2013 1:12:06 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.common;

import org.bridj.Pointer;

import java.util.Comparator;

/**
 * A comparator used for sorting a one-dimensional array of double pointers in ascending order. Used
 * to obtain the index of the larger elements.
 *
 * <pre>
 * <code>
 * Pointer<Double> ptr;
 * Integer [] indices = new Integer[size];
 * for (i = 0; i < size; i++)
 * 	indices[i] = i;
 *
 * DoublePointerComparator cmp = new DoublePointerComparator(ptr);
 *
 * Arrays.sort(indices, cmp);
 * </code>
 * </pre>
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class DoublePointerComparator implements Comparator<Integer> {

  private Pointer<Double> pointer;

  /**
   * Creates a double pointer comparator that is bound to a pointer
   *
   * @param pointer the pointer that the object is bound to
   */
  public DoublePointerComparator(Pointer<Double> pointer) {
    this.pointer = pointer;
  }

  /**
   * Compares two values at indices i1 and i2 inside of the pointer that was initialized with the
   * DoublePointerComparator constructor
   *
   * @param o1 the first index inside of the pointer to compare
   * @param o2 the second index inside of the pointer to compare
   */
  @Override
  public int compare(Integer o1, Integer o2) {
    return Double.compare(this.pointer.getDoubleAtIndex(o2), this.pointer.getDoubleAtIndex(o1));
  }

}
