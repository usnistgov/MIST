// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 25, 2014 4:26:31 PM EST
//
// Time-stamp: <Apr 25, 2014 4:26:31 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.export.tileblender;

import gov.nist.isg.mist.lib.common.Array2DView;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import ij.ImagePlus;
import ij.plugin.filter.ImageProperties;
import ij.process.*;
import loci.formats.FormatException;
import loci.formats.out.OMETiffWriter;

import java.io.IOException;
import java.nio.*;

/**
 * Creates a linear blending function
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TileLinearBlend extends TileBlender {

  private static final double DEFAULT_ALPHA = 1.5;
  private double[][][] pixelSums;
  private double[][][] weightSums;
  private double[][] lookupTable;

  /**
   * Initializes the linear blend
   *
   * @param initImgWidth  the width of a single image
   * @param initImgHeight the height of a single image
   * @param alpha         the alpha component of the linear blend
   */
  public TileLinearBlend(int bytesPerPixel, int imageType, int initImgWidth, int initImgHeight, double alpha) {
    super(bytesPerPixel, imageType);

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
  public void initBlender(int tileSizeX, int tileSizeY) {
    this.pixelSums = new double[tileSizeY][tileSizeX][this.getNumChannels()];
    this.weightSums = new double[tileSizeY][tileSizeX][this.getNumChannels()];
  }

  @Override
  public void blend(int x, int y, Array2DView pixels, ImageTile<?> tile) {
    ImageProcessor ip = tile.getImageProcessor();

    int tileY = 0;
    for (int row = pixels.getStartRow(); row < pixels.getStartRow() + pixels.getViewHeight(); row++) {
      int tileX = 0;
      for (int col = pixels.getStartCol(); col < pixels.getStartCol() + pixels.getViewWidth(); col++) {

        int value = ip.getPixel(col, row);
//        int[] pixelChannels = imgPlus.getPixel(col, row);
        double weight = this.lookupTable[row][col];

        if (this.getNumChannels() == 1) {
          this.pixelSums[y + tileY][x + tileX][0] += (weight * value);
          this.weightSums[y + tileY][x + tileX][0] += weight;
        } else {
          int r = (value & 16711680) >> 16;
          int g = (value & '\uff00') >> 8;
          int b = value & 255;
          this.pixelSums[y + tileY][x + tileX][0] += (weight * r);
          this.weightSums[y + tileY][x + tileX][0] += weight;
          this.pixelSums[y + tileY][x + tileX][1] += (weight * g);
          this.weightSums[y + tileY][x + tileX][1] += weight;
          this.pixelSums[y + tileY][x + tileX][2] += (weight * b);
          this.weightSums[y + tileY][x + tileX][2] += weight;
        }

        tileX++;
      }
      tileY++;
    }
  }

  @Override
  public void finalizeBlend()
  {
    for (int row = 0; row < this.getTileHeight(); row++) {
      for (int col = 0; col < this.getTileWidth(); col++) {
        for (int channel = 0; channel < this.getNumChannels(); channel++) {
          double weightedVal = 0.0;
          if (this.weightSums[row][col][channel] != 0) {
            weightedVal = this.pixelSums[row][col][channel] / this.weightSums[row][col][channel];
          }
          this.setPixelValueChannel(col, row, channel, (int) weightedVal);
        }
      }
    }
  }

}
