// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 4:05:29 PM EST
//
// Time-stamp: <Aug 1, 2013 4:05:29 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.statistics;

import java.util.Collections;
import java.util.List;

/**
 * Statistic utility functions for aiding in doing global optimizations.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class StatisticUtils {

  /**
   * Determines the operation type for statistic operations
   */
  public static enum OP_TYPE {
    /**
     * Uses the median as the operation type
     */
    MEDIAN("Median"),

    /**
     * Uses the mean as the operation type
     */
    MEAN("Mean"),

    /**
     * Uses the mode as the operation type
     */
    MODE("Mode"),

    /**
     * Uses the minimum as the operation type
     */
    MIN("Min"),

    /**
     * Uses the maximum as the operation type
     */
    MAX("Max");

    private OP_TYPE(final String text) {
      this.text = text;
    }

    private final String text;

    @Override
    public String toString() {
      return this.text;
    }

  }

  /**
   * Gets the index at the percentile location (percentile 0.5 = median).
   *
   * @param percentile the percentile from 0 - 1.0
   * @param list       the list of elements
   * @return the index into the list for a given percentile
   */
  public static <T> int percentileIdx(double percentile, List<T> list) {
    double k = (list.size() + 1.0) * percentile;
    double f = Math.floor(k);
    double c = Math.ceil(k);

    if (f == c) {
      return (int) k;
    }
    return (int) ((f + c) / 2);
  }

  /**
   * Computes the median of a list
   *
   * @param m the list to find the median from
   * @return the median
   */
  public static <T extends Number & Comparable<? super T>> double median(List<T> m) {
    Collections.sort(m);

    int middle = m.size() / 2;

    if (m.size() % 2 == 1)
      return m.get(middle).doubleValue();
    return (m.get(middle - 1).doubleValue() + m.get(middle).doubleValue()) / 2.0;
  }

  /**
   * Compute the mode for a list of numbers
   *
   * @param m the list of numbers
   * @return the mode
   */
  public static <T extends Number & Comparable<? super T>> double mode(List<T> m) {
    double maxValue = Double.MIN_VALUE, maxCount = 0;

    for (int i = 0; i < m.size(); i++) {
      int count = 0;
      for (int j = 0; j < m.size(); j++) {
        if (m.get(j) == m.get(i))
          ++count;
      }

      if (count > maxCount) {
        maxCount = count;
        maxValue = m.get(i).doubleValue();
      }
    }

    return maxValue;
  }

  /**
   * Computes the mean of a list
   *
   * @param m the list to find the mean from
   * @return the mean
   */
  public static <T extends Number & Comparable<? super T>> double mean(List<T> m) {
    double sum = 0;

    for (int i = 0; i < m.size(); i++) {
      sum += m.get(i).doubleValue();
    }

    return sum / m.size();
  }

  /**
   * Computes the minimum of a list of numbers
   *
   * @param m the list to find the min
   * @return the minimum of the list
   */
  public static <T extends Number & Comparable<? super T>> double min(List<T> m) {
    double min = Double.MAX_VALUE;

    for (T i : m) {
      if (min > i.doubleValue())
        min = i.doubleValue();
    }

    return min;

  }

  /**
   * computes the maximum of a list of numbers
   *
   * @param m the list to find the max
   * @return the maximum of the list
   */
  public static <T extends Number & Comparable<? super T>> double max(List<T> m) {
    double max = Double.MIN_VALUE;

    for (T i : m) {
      if (max < i.doubleValue())
        max = i.doubleValue();
    }

    return max;
  }
}
