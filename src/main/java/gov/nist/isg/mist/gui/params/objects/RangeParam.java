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
// Date: Jul 2, 2014 11:32:59 AM EST
//
// Time-stamp: <Jul 2, 2014 11:32:59 AM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui.params.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Range param that specifies a min max range.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class RangeParam {

  private int min;
  private int max;

  /**
   * @param min
   * @param max
   */
  public RangeParam(int min, int max) {
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

  @Override
  public String toString() {
    return (this.min == this.max ? Integer.toString(this.min) : Integer.toString(this.min) + "-"
        + Integer.toString(this.max));
  }

  /**
   * Converts a string representation of a range (1-15) into a range param
   *
   * @param param the string representation of the range
   * @return the range param
   */
  public static RangeParam parseParam(String param) {
    if (param.equals(""))
      return null;

    int min = 0;
    int max = 0;
    String[] split = param.split("-");

    try {
      if (split.length == 1) {
        min = max = Integer.parseInt(split[0]);
      } else if (split.length == 2) {
        min = Integer.parseInt(split[0]);
        max = Integer.parseInt(split[1]);

        if (min > max) {
          int temp = min;
          min = max;
          max = temp;
        }

      } else {
        return null;
      }
    } catch (NumberFormatException e) {
      return null;
    }

    return new RangeParam(min, max);

  }

  /**
   * Parses a timeSlice string to its appropriate range parameters
   *
   * @param val the time slice string
   * @return a list of range parameters, or an empty list if an error occurred
   */
  public static List<RangeParam> parseTimeSlices(String val) {
    List<RangeParam> rangeParams = new ArrayList<RangeParam>();
    String[] timeSlices = val.split(",");

    for (String timeSlice : timeSlices) {
      RangeParam rangeParam = RangeParam.parseParam(timeSlice);
      if (rangeParam == null)
        return new ArrayList<RangeParam>();
      rangeParams.add(rangeParam);
    }

    return rangeParams;
  }

}
