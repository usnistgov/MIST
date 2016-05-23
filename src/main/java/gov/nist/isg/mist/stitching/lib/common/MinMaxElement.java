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
// Date: Apr 11, 2014 11:08:33 AM EST
//
// Time-stamp: <Apr 11, 2014 11:08:33 AM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.common;

/**
 * Class for storing the min and max into a single object.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class MinMaxElement {

  private int min;
  private int max;

  /**
   * Creates a min-max element
   *
   * @param min the min value
   * @param max the max value
   */
  public MinMaxElement(int min, int max) {
    this.min = min;
    this.max = max;
  }

  /**
   * @return the min
   */
  public int getMin() {
    return this.min;
  }

  /**
   * @param min the min to set
   */
  public void setMin(int min) {
    this.min = min;
  }

  /**
   * @return the max
   */
  public int getMax() {
    return this.max;
  }

  /**
   * @param max the max to set
   */
  public void setMax(int max) {
    this.max = max;
  }

}
