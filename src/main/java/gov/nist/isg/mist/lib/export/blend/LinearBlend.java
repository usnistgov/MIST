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
// Date: Apr 25, 2014 4:26:31 PM EST
//
// Time-stamp: <Apr 25, 2014 4:26:31 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.export.blend;

import gov.nist.isg.mist.lib.common.Array2DView;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 * Creates a linear blending function
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class LinearBlend implements Blender {

  private static final double DEFAULT_ALPHA = 1.5;

  private ImageProcessor ip;

  private double[][][] pixelSums;
  private double[][][] weightSums;
  private double[][] lookupTable;

  private int numChannels;

  private int width;
  private int height;

  /**
   * Initializes the linear blend
   *
   * @param initImgWidth  the width of a single image
   * @param initImgHeight the height of a single image
   * @param alpha         the alpha component of the linear blend
   */
  public LinearBlend(int initImgWidth, int initImgHeight, double alpha) {

    if (Double.isNaN(alpha))
      alpha = DEFAULT_ALPHA;

    this.lookupTable = new double[initImgHeight][initImgWidth];
    for (int i = 0; i < initImgHeight; i++) {
      for (int j = 0; j < initImgWidth; j++) {
        this.lookupTable[i][j] = getWeight(i, j, initImgWidth, initImgHeight, alpha);
      }
    }

  }

  private static double getWeight(int row, int col, int imWidth, int imHeight, double alpha) {
    double distWest = col + 1.0;
    double distNorth = row + 1.0;
    double distEast = imWidth - col;
    double distSouth = imHeight - row;
    double minEastWest = Math.min(distEast, distWest);
    double minNorthSouth = Math.min(distNorth, distSouth);
    double weight = minEastWest * minNorthSouth;

    return Math.pow(weight, alpha);

  }

  @Override
  public void init(int width, int height, ImagePlus initImg) throws OutOfMemoryError,
      NegativeArraySizeException {

    this.width = width;
    this.height = height;

    if (initImg.getBitDepth() == 24) {
      this.numChannels = 3;
    } else {
      this.numChannels = 1;
    }

    switch (initImg.getBitDepth()) {
      case 8:
        this.ip = new ByteProcessor(width, height);
        break;
      case 16:
        this.ip = new ShortProcessor(width, height, true);
        break;
      case 24:
        this.ip = new ColorProcessor(width, height);
        break;
      case 32:
        this.ip = new FloatProcessor(width, height);
        break;
      default:
        this.ip = new FloatProcessor(width, height);
        break;
    }

    this.pixelSums = new double[this.numChannels][height][width];
    this.weightSums = new double[this.numChannels][height][width];
  }

  @Override
  public void blend(int x, int y, Array2DView pixels, ImageTile<?> tile) {
    ImagePlus imgPlus = null;
    if (tile.getBitDepth() > 16) {
      imgPlus = tile.getImagePlus();
    }

    for (int row = 0; row < pixels.getViewHeight(); row++) {
      for (int col = 0; col < pixels.getViewWidth(); col++) {
        int[] pixelChannels = null;
        if (imgPlus != null) {
          pixelChannels = imgPlus.getPixel(col, row);
        }
        for (int channel = 0; channel < this.numChannels; channel++) {
          double weight = this.lookupTable[row][col];
          if (pixelChannels != null) {
            this.pixelSums[channel][row + y][col + x] += (weight * pixelChannels[channel]);

          } else {
            this.pixelSums[channel][row + y][col + x] += (weight * pixels.get(row, col));
          }

          this.weightSums[channel][row + y][col + x] += weight;
        }

      }
    }
  }

  @Override
  public ImageProcessor getResult() {
    return this.ip;
  }

  @Override
  public void postProcess() {
    for (int row = 0; row < this.height; row++) {
      for (int col = 0; col < this.width; col++) {
        int val = 0;
        for (int channel = 0; channel < this.numChannels; channel++) {
          if (this.numChannels > 1)
            val = val |
                (((int) (this.pixelSums[channel][row][col] / this.weightSums[channel][row][col]) & 0xFF) << ((this.numChannels - 1 - channel) * 8));
          else
            val = (int) (this.pixelSums[channel][row][col] / this.weightSums[channel][row][col]);
        }
        this.ip.set(col, row, val);
      }
    }
  }

}
