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
// Date: Aug 1, 2013 1:12:06 PM EST
//
// Time-stamp: <Aug 1, 2013 1:12:06 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.common;

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
