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
public class TileLinearBlend implements TileBlender {

  private static final double DEFAULT_ALPHA = 1.5;

  private ImageProcessor ip;
  private int bytesPerPixel;
  private int imageType;
  private ByteBuffer buffer;

  private double[][][] pixelSums;
  private double[][][] weightSums;
  private double[][] lookupTable;

  private int numChannels;

  /**
   * Initializes the linear blend
   *
   * @param initImgWidth  the width of a single image
   * @param initImgHeight the height of a single image
   * @param alpha         the alpha component of the linear blend
   */
  public TileLinearBlend(int bytesPerPixel, int imageType, int initImgWidth, int initImgHeight, double alpha) {
    this.bytesPerPixel = bytesPerPixel;
    this.imageType = imageType;

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
  public void init(int tileSizeX, int tileSizeY) {
    // Reset buffer to zero
    this.buffer = ByteBuffer.allocate(tileSizeY * tileSizeX * this.bytesPerPixel);
    this.buffer.order(ByteOrder.BIG_ENDIAN);

    switch(imageType){
      case ImagePlus.GRAY8:
        this.ip = new ByteProcessor(tileSizeX, tileSizeY);
        this.numChannels = 1;
        break;
      case ImagePlus.GRAY16:
        this.ip = new ShortProcessor(tileSizeX, tileSizeY);
        this.numChannels = 1;
        break;
      case ImagePlus.GRAY32:
        this.ip = new FloatProcessor(tileSizeX, tileSizeY);
        this.numChannels = 1;
        break;
      case ImagePlus.COLOR_RGB:
        this.ip = new ColorProcessor(tileSizeX, tileSizeY);
        this.numChannels = 4;
        break;
      default:
        // TODO: Error or set a default?
    }

    this.pixelSums = new double[tileSizeY][tileSizeX][this.numChannels];
    this.weightSums = new double[tileSizeY][tileSizeX][this.numChannels];
  }

  @Override
  public void blend(int x, int y, Array2DView pixels, ImageTile<?> tile) {
    ImagePlus imgPlus = tile.getImagePlus();

    int tileY = 0;
    for (int row = pixels.getStartRow(); row < pixels.getStartRow() + pixels.getViewHeight(); row++) {
      int tileX = 0;
      for (int col = pixels.getStartCol(); col < pixels.getStartCol() + pixels.getViewWidth(); col++) {

        int[] pixelChannels = imgPlus.getPixel(col, row);
        double weight = this.lookupTable[row][col];

        for (int channel = 0; channel < this.numChannels; channel++) {
            this.pixelSums[y + tileY][x + tileX][channel] += (weight * pixelChannels[channel]);
            this.weightSums[y + tileY][x + tileX][channel] += weight;
        }
        tileX++;
      }
      tileY++;
    }
  }

  @Override
  public void postProcess(int tileX, int tileY, int tileXSize, int tileYSize, OMETiffWriter omeTiffWriter) throws IOException, FormatException
  {
    for (int row = 0; row < this.ip.getHeight(); row++) {
      for (int col = 0; col < this.ip.getWidth(); col++) {
        int val = 0;
        for (int channel = 0; channel < this.numChannels; channel++) {
          double weightedVal = 0.0;
          if (this.weightSums[row][col][channel] != 0) {
            weightedVal = this.pixelSums[row][col][channel] / this.weightSums[row][col][channel];
          }


          if (this.numChannels > 1) {
            val = val | ((int) weightedVal & 0xFF) << ((this.numChannels - 1 - channel) * 8);
          } else {
            val = (int) weightedVal;
          }
        }
        this.ip.set(col, row, val);
      }
    }


    Object pixels = this.ip.getPixels();

    switch (imageType) {
      case ImagePlus.GRAY8:
        this.buffer.put((byte[]) pixels);
        break;
      case ImagePlus.GRAY16:
        ShortBuffer shortBuffer = this.buffer.asShortBuffer();
        shortBuffer.put((short[]) pixels);
        break;
      case ImagePlus.GRAY32:
        FloatBuffer floatBuffer = this.buffer.asFloatBuffer();
        floatBuffer.put((float[]) pixels);
        break;
      case ImagePlus.COLOR_RGB:
        IntBuffer intBuffer = this.buffer.asIntBuffer();
        intBuffer.put((int[]) pixels);
        break;
      default:
        // TODO: Error or set a default?
    }

    omeTiffWriter.saveBytes(0, this.buffer.array(), tileX, tileY, tileXSize, tileYSize);

  }

}
