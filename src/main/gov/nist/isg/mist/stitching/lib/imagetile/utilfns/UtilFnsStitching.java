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
// Date: Aug 1, 2013 4:06:41 PM EST
//
// Time-stamp: <Aug 1, 2013 4:06:41 PM tjb3>
//
//
// ================================================================

package main.gov.nist.isg.mist.stitching.lib.imagetile.utilfns;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import main.gov.nist.isg.mist.stitching.lib.common.CorrelationTriple;
import main.gov.nist.isg.mist.stitching.lib.imagetile.java.JavaImageTile;
import main.gov.nist.isg.mist.stitching.lib.log.Debug;
import main.gov.nist.isg.mist.stitching.lib.log.Debug.DebugType;

import org.bridj.Pointer;

/**
 * Utility functions for hooking into utility function native library bindings.
 * 
 * @author Tim Blattner
 * @version 1.0
 */
public class UtilFnsStitching {

  private static final double DBL_EPSILON = 2.22044604925031308085e-16d;
  private static boolean hasUtilFnsStitching = false;
  private static final double MAX_DISTANCE = 1.0;
  private static final double MAX_DISTANCE_SQ = MAX_DISTANCE * MAX_DISTANCE;

  /**
   * Disables using the utilfns native library
   */
  public static void disableUtilFnsNativeLibrary() {
    hasUtilFnsStitching = false;
  }

  /**
   * Enables using the utilfns native library
   */
  public static void enableUtilFnsNativeLibrary() {
    hasUtilFnsStitching = true;
  }

  /**
   * Gets whether the utilfns native library is enabled or not
   * 
   * @return true if the utilfns library is enabled, otherwise false
   */
  public static boolean hasUtilFnsStitchingNativeLibrary() {
    return hasUtilFnsStitching;
  }

  /**
   * Computes the phase correlation using native libraries.
   * 
   * @param c1 the pointers to the first piece of memory
   * @param c2 the pointer to the second piece of memory
   * @param result the pointer to the result piece of memory
   * @param size the size of that data
   */
  public static void computePhaseCorrelation(Pointer<Double> c1, Pointer<Double> c2,
      Pointer<Double> result, int size) {
    if (hasUtilFnsStitching)
      UtilFnsLibrary.elt_prod_conj_norm(result, c1, c2, size);
    else
      computePhaseCorrelationJava(c1, c2, result, size);
  }

  /**
   * Computes the phase correlation between two complex arrays
   * 
   * @param c1 complex array 1
   * @param c2 complex array 2
   * @param ncc the normalized cross correlation matrix
   * @return the phase correlated matrix
   */
  public static float[][] computePhaseCorrelationJava(float[][] c1, float[][] c2, float[][] ncc) {
    float c1_real, c1_imag, c2_real, c2_imag;
    float r, im, temp;

    for (int row = 0; row < JavaImageTile.fftPlan.getFrequencySampling2().getCount(); row++) {
      for (int col = 0; col < JavaImageTile.fftPlan.getFrequencySampling1().getCount(); col++) {
        c1_real = c1[row][col * 2];
        c1_imag = c1[row][col * 2 + 1];

        c2_real = c2[row][col * 2];
        c2_imag = c2[row][col * 2 + 1] * -1.0f;

        r = c1_real * c2_real - c1_imag * c2_imag;
        im = c1_real * c2_imag + c2_real * c1_imag;

        temp = (float) Math.sqrt(r * r + im * im);

        if (Double.isNaN(temp) || temp == 0) {
          r = (float) DBL_EPSILON;
          temp = (float) DBL_EPSILON;
        }


        r /= temp;
        im /= temp;
        ncc[row][col * 2] = r;
        ncc[row][col * 2 + 1] = im;
      }
    }

    return ncc;
  }

  /**
   * Computes the phase correlation between two complex arrays
   * 
   * @param c1 complex array 1
   * @param c2 complex array 2
   * @param ncc the normalized cross correlation matrix
   * @param sz the size of the array
   */
  public static void computePhaseCorrelationJava(Pointer<Double> c1, Pointer<Double> c2,
      Pointer<Double> ncc, int sz) {

    double r, im, temp, c1_r, c1_im, c2_r, c2_im;

    for (int i = 0; i < sz; i++) {
      c1_r = c1.getDoubleAtIndex(i * 2);
      c1_im = c1.getDoubleAtIndex(i * 2 + 1);

      c2_r = c2.getDoubleAtIndex(i * 2);
      c2_im = c2.getDoubleAtIndex(i * 2 + 1) * -1.0;

      r = c1_r * c2_r - c1_im * c2_im;
      im = c1_r * c2_im + c2_r * c1_im;

      temp = Math.sqrt(r * r + im * im);
      if (Double.isNaN(temp) || temp == 0) {
        r = DBL_EPSILON;
        temp = DBL_EPSILON;
      }

      r /= temp;
      im /= temp;

      ncc.setDoubleAtIndex(i * 2, r);
      ncc.setDoubleAtIndex(i * 2 + 1, im);

    }
  }

  /**
   * Gets the max index using native libraries
   * 
   * @param c the pointer to the piece of memory
   * @param size the size of the data
   * @return the index that has the highest correlation
   */
  public static int getMaxIdx(Pointer<Double> c, int size) {
    if (hasUtilFnsStitching)
      return UtilFnsLibrary.reduce_max_abs(c, size);
    return getMaxIdxJava(c, size);
  }

  /**
   * Gets the max index in Java for float array
   * 
   * @param c the float array
   * @param width width of data
   * @param height height of data
   * @return the index that has the highest correlation
   */
  public static int getMaxIdxJava(float[][] c, int width, int height) {
    double max_norm = Double.NEGATIVE_INFINITY;
    double val;
    int max_idx = 0;
    for (int row = 0; row < height; row++) {
      for (int col = 0; col < width; col++) {
        val = c[row][col];
        if (val > max_norm) {
          max_norm = val;
          max_idx = row * width + col;
        }
      }

    }
    return max_idx;
  }

  /**
   * Gets the max index using Java
   * 
   * @param c the pointer to the piece of memory
   * @param size the size of the data
   * @return the index that has the highest correlation
   */
  public static int getMaxIdxJava(Pointer<Double> c, int size) {
    double max = Double.NEGATIVE_INFINITY;
    double val;
    int max_idx = 0;
    for (int i = 0; i < size; i++) {
      val = c.getDoubleAtIndex(i);
      if (val > max) {
        max = val;
        max_idx = i;
      }
    }

    return max_idx;
  }

  /**
   * Finds the nPeaks max values that are distance 10 from eachother using native libraries
   * 
   * @param pcm the phase correlation matrix pointer
   * @param nPeaks the number of peaks to find
   * @param width the width of the data
   * @param height the height of the data
   * @param peaks the reference to the peaks memory
   * @return a list of the highest correlation triples that are distance 10 from eachother
   */
  @SuppressWarnings("unused")
  public static List<CorrelationTriple> multiPeakCorrelationMatrix(Pointer<Double> pcm, int nPeaks,
      int width, int height, Pointer<Integer> peaks) {

    if (hasUtilFnsStitching) {
      UtilFnsLibrary.get_multi_max_no_sort(pcm, nPeaks, width, height, MAX_DISTANCE, peaks);

      List<CorrelationTriple> corrPeaks = new ArrayList<CorrelationTriple>(nPeaks);

      for (int i = 0; i < nPeaks; i++) {

        int peak = peaks.getIntAtIndex(i);
        int y = peak / width;
        int x = peak % width;
        corrPeaks.add(new CorrelationTriple(-1.0, x, y));
        Debug.msg(DebugType.VERBOSE, "Found multimax peak " + i + " ( " + x + ", " + y + ")");
      }

      return corrPeaks;
    }
    
    if(MAX_DISTANCE <= 1) {
      return multiPeakCorrelationMatrixNoDist(pcm, nPeaks, width, height);
    }
    
    return multiPeakCorrelationMatrixNoSort(pcm, nPeaks, width, height);
  }

  private static double distance(int x1, int x2, int y1, int y2) {
    return (((double) x1 - (double) x2) * ((double) x1 - (double) x2))
        + (((double) y1 - (double) y2) * ((double) y1 - (double) y2));
  }

  private static boolean checkDistance(int[] maxFilter, int nMax, int curIdx, int width) {
    int row = curIdx / width;
    int col = curIdx % width;

    double dist;
    for (int j = 0; j < nMax; j++) {
      int rowFilter = maxFilter[j] / width;
      int colFilter = maxFilter[j] % width;

      dist = distance(rowFilter, row, colFilter, col);

      if (dist < MAX_DISTANCE_SQ) {
        return false;
      }
    }

    return true;

  }

  private static int filterNextMax(Pointer<Double> m, int[] maxFilter, int nMax, int width,
      int height) {
    double maxVal = Double.NEGATIVE_INFINITY;
    int maxIdx = 0;
    for (int i = 0; i < width * height; i++) {
      double val = m.getDoubleAtIndex(i);

      if (val > maxVal) {
        if (checkDistance(maxFilter, nMax, i, width)) {
          maxVal = val;
          maxIdx = i;
        }
      }
    }

    return maxIdx;
  }

  public static List<CorrelationTriple> multiPeakCorrelationMatrixNoDist(Pointer<Double> pcm, int nPeaks, int width, int height)
  {
    List<CorrelationTriple> peaks = new ArrayList<CorrelationTriple>();
    SortedSet<IndexValuePair> maxIndices = new TreeSet<IndexValuePair>();

    for (int i = 0; i < width * height; i++)
    {
      if (maxIndices.size() < nPeaks)
      {
        maxIndices.add(new IndexValuePair(i, pcm.getDoubleAtIndex(i)));
      }
      else
      {
        IndexValuePair lastIndex = maxIndices.last();

        double val = lastIndex.getValue();
        if (val < pcm.getDoubleAtIndex(i))
        {
          // remove last
          maxIndices.remove(lastIndex);
          maxIndices.add(new IndexValuePair(i, pcm.getDoubleAtIndex(i)));
        }
      }

    }

    int count = 0;
    for (IndexValuePair pair : maxIndices)
    {
      int index = pair.getIndex();
      int row = index / width;
      int col = index % width;

      peaks.add(new CorrelationTriple(0.0, col, row));
      Debug.msg(DebugType.VERBOSE, "Found multimax peak " + count + " ( " + col + ", " + row + ")");
      count++;
    }

    return peaks;
  }


  /**
   * Finds the nPeaks max values that are distance d (10) from each other using native libraries
   * 
   * @param pcm the phase correlation matrix pointer
   * @param nPeaks the number of peaks to find
   * @param width the width of the data
   * @param height the height of the data
   * @return a list of the highest correlation triples that are distance 10 from eachother
   */
  public static List<CorrelationTriple> multiPeakCorrelationMatrixNoSort(Pointer<Double> pcm,
      int nPeaks, int width, int height) {

    List<CorrelationTriple> peaks = new ArrayList<CorrelationTriple>();

    int curMax = 0;
    int nMax = 0;
    int[] maxVals = new int[nPeaks];

    while (nMax < nPeaks) {
      int nextMax = filterNextMax(pcm, maxVals, nMax, width, height);

      maxVals[curMax] = nextMax;

      curMax++;
      nMax++;
    }

    for (int i = 0; i < nMax; i++) {
      int row = maxVals[i] / width;
      int col = maxVals[i] % width;
      peaks.add(new CorrelationTriple(0.0, col, row));
      Debug.msg(DebugType.VERBOSE, "Found multimax peak " + i + " ( " + col + ", " + row + ")");

    }

    return peaks;
  }

  private static int filterNextMax(float[][] m, int[] maxFilter, int nMax, int width, int height) {
    double maxVal = Double.NEGATIVE_INFINITY;
    int maxIdx = 0;
    for (int row = 0; row < height; row++) {
      for (int col = 0; col < width; col++) {
        float val = m[row][col];
        int index = row * width + col;
        if (val > maxVal) {
          if (checkDistance(maxFilter, nMax, index, width)) {
            maxVal = val;
            maxIdx = index;
          }
        }
      }
    }
    return maxIdx;
  }

  /**
   * Finds the nPeaks max values that are distance d (10) from each other using native libraries
   * 
   * @param pcm the phase correlation matrix pointer
   * @param nPeaks the number of peaks to find
   * @param width the width of the data
   * @param height the height of the data
   * @return a list of the highest correlation triples that are distance 10 from eachother
   */
  public static List<CorrelationTriple> multiPeakCorrelationMatrixNoSort(float[][] pcm, int nPeaks,
      int width, int height) {

    List<CorrelationTriple> peaks = new ArrayList<CorrelationTriple>();

    int curMax = 0;
    int nMax = 0;
    int[] maxVals = new int[nPeaks];

    while (nMax < nPeaks) {
      int nextMax = filterNextMax(pcm, maxVals, nMax, width, height);

      maxVals[curMax] = nextMax;

      curMax++;
      nMax++;
    }

    for (int i = 0; i < nMax; i++) {
      int row = maxVals[i] / width;
      int col = maxVals[i] % width;
      peaks.add(new CorrelationTriple(0.0, col, row));
      Debug.msg(DebugType.VERBOSE, "Found multimax peak " + i + " ( " + col + ", " + row + ")");

    }

    return peaks;
  }

}
