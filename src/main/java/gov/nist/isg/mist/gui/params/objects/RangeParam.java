// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



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
