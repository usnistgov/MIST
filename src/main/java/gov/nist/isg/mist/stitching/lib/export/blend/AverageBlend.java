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
// Date: Apr 25, 2014 4:23:18 PM EST
//
// Time-stamp: <Apr 25, 2014 4:23:18 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.export.blend;

import gov.nist.isg.mist.stitching.lib.common.Array2DView;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 * Creates an average blending function
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class AverageBlend implements Blender {

  private ImageProcessor ip;
  private float[][][] sums;
  private int[][][] counts;
  private int numChannels;

  private int width;
  private int height;

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

    try {
      this.sums = new float[this.numChannels][height][width];
      this.counts = new int[this.numChannels][height][width];
    } catch (Exception e) {
      e.printStackTrace();
    }
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

          if (pixelChannels != null) {
            this.sums[channel][row + y][col + x] += pixelChannels[channel];
          } else {
            this.sums[channel][row + y][col + x] += pixels.get(row, col);
          }

          this.counts[channel][row + y][col + x] = this.counts[channel][row + y][col + x] + 1;
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
            val |=
                (((int) (this.sums[channel][row][col] / this.counts[channel][row][col]) & 0xFF) << ((this.numChannels - 1 - channel) * 8));
          else
            val = (int) (this.sums[channel][row][col] / this.counts[channel][row][col]);
        }
        this.ip.set(col, row, val);
      }
    }
  }

}
