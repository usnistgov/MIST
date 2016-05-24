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
// Date: May 10, 2013 2:58:58 PM EST
//
// Time-stamp: <May 10, 2013 2:58:58 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.imagetile.utilfns;

public class IndexValuePair implements Comparable<IndexValuePair> {

  private int index;
  private double value;


  /**
   * @param index
   * @param value
   */
  public IndexValuePair(int index, double value) {
    super();
    this.index = index;
    this.value = value;
  }

  /**
   * @return the index
   */
  public int getIndex() {
    return this.index;
  }

  /**
   * @param index the index to set
   */
  public void setIndex(int index) {
    this.index = index;
  }

  /**
   * @return the value
   */
  public double getValue() {
    return this.value;
  }

  /**
   * @param value the value to set
   */
  public void setValue(double value) {
    this.value = value;
  }

  @Override
  public int compareTo(IndexValuePair arg0) {
    int val = Double.compare(arg0.getValue(), this.getValue());

    if (val == 0) {
      return new Integer(this.getIndex()).compareTo(arg0.getIndex());
    }
    return val;

  }


}
