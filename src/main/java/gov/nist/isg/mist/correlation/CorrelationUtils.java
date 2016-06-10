// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.


package gov.nist.isg.mist.correlation;

import gov.nist.isg.mist.lib.common.Array2DView;
import gov.nist.isg.mist.lib.imagetile.ImageTile;

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
