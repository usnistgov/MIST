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

package gov.nist.isg.mist.correlation;

import gov.nist.isg.mist.stitching.lib.common.Array2DView;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;

/**
 * Set of static methods related to computing cross correlation between overlapping images.
 *
 * @author Michael Majurski
 */
public class CorrelationUtils {


  /**
   * Extracts the sub-region visible if the image view window is translated the given (x,y)
   * distance).
   *
   * @param tile The image tile a sub-region is being extracted from. The translation (x,y) is
   *             relative to the upper left corner of this image.
   * @param x    the x component of the translation.
   * @param y    the y component of the translation.
   * @return the portion of tile shown if the view is translated (x,y) pixels.
   */
  public static Array2DView extractSubRegion(ImageTile<?> tile, int x, int y) {

    int height = tile.getHeight();
    int width = tile.getWidth();

    // get starting and ending coordinates
    int xStart = x;
    int xEnd = xStart + width - 1;
    int yStart = y;
    int yEnd = yStart + height - 1;

    // constrain to valid coordinates
    xStart = Math.max(0, Math.min(xStart, width - 1));
    xEnd = Math.max(0, Math.min(xEnd, width - 1));
    yStart = Math.max(0, Math.min(yStart, height - 1));
    yEnd = Math.max(0, Math.min(yEnd, height - 1));

    // determine the view size
    int viewWidth = xEnd - xStart + 1;
    int viewHeight = yEnd - yStart + 1;

    // if the translations (x,y) would leave no overlap between the images, return an empty
    // Array2DView
    if (Math.abs(x) >= width || Math.abs(y) >= height)
      return new Array2DView(tile, yStart, 0, xStart, 0);

    return new Array2DView(tile, yStart, viewHeight, xStart, viewWidth);
  }


  /**
   * Computes the cross correlation between two ImageTiles given the offset (x,y) from the first to
   * the second.
   *
   * @param i1 the first ImageTile.
   * @param i2 the second ImageTile.
   * @param x  the x component of the translation from i1 to i2.
   * @param y  the y component of the translation from i1 to i2.
   * @return the normalized cross correlation between the overlapping pixels given the translation
   * between i1 and i2 (x,y).
   */
  public static double computeCrossCorrelation(ImageTile<?> i1, ImageTile<?> i2, int x, int y) {

    // translation x,y is from i1, to i2.
    Array2DView a1 = extractSubRegion(i1, x, y);
    Array2DView a2 = extractSubRegion(i2, -x, -y);

    return crossCorrelation(a1, a2);
  }


  /**
   * Computes the cross correlation between two arrays
   *
   * @param a1 pixel data
   * @param a2 pixel data
   * @return the cross correlation
   */
  public static double crossCorrelation(Array2DView a1, Array2DView a2) {
    double sum_prod = 0.0;
    double sum1 = 0.0;
    double sum2 = 0.0;
    double norm1 = 0.0;
    double norm2 = 0.0;
    double a1_ij;
    double a2_ij;

    int n_rows = a1.getViewHeight();
    int n_cols = a1.getViewWidth();

    // ensure that both images are the same size
    if (a2.getViewHeight() != n_rows || a2.getViewWidth() != n_cols)
      return -1.0;

    int sz = n_rows * n_cols;

    for (int i = 0; i < n_rows; i++)
      for (int j = 0; j < n_cols; j++) {
        a1_ij = a1.get(i, j);
        a2_ij = a2.get(i, j);
        sum_prod += a1_ij * a2_ij;
        sum1 += a1_ij;
        sum2 += a2_ij;
        norm1 += a1_ij * a1_ij;
        norm2 += a2_ij * a2_ij;
      }

    double numer = sum_prod - sum1 * sum2 / sz;
    double denom = Math.sqrt((norm1 - sum1 * sum1 / sz) * (norm2 - sum2 * sum2 / sz));

    double val = numer / denom;

    if (Double.isNaN(val) || Double.isInfinite(val)) {
      val = -1.0;
    }

    return val;
  }


}
