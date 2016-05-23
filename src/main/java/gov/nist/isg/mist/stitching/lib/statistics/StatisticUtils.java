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
// Date: Aug 1, 2013 4:05:29 PM EST
//
// Time-stamp: <Aug 1, 2013 4:05:29 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.statistics;

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
