// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 25, 2014 4:27:16 PM EST
//
// Time-stamp: <Apr 25, 2014 4:27:16 PM tjb3>
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
 * Creates an overlay blending function
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class OverlayBlend implements Blender {

  private ImageProcessor ip;
  private int numChannels;

  @Override
  public void init(int width, int height, ImagePlus initImg) throws OutOfMemoryError,
      NegativeArraySizeException {

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
        this.ip = new ShortProcessor(width, height);
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
  }

  @Override
  public void blend(int x, int y, Array2DView pixels, ImageTile<?> tile) {

    ImagePlus imgPlus = tile.getImagePlus();


    for (int row = 0; row < pixels.getViewHeight(); row++) {
      for (int col = 0; col < pixels.getViewWidth(); col++) {

        int[] pixelChannels = imgPlus.getPixel(col, row);


        int val = 0;
        for (int channel = 0; channel < this.numChannels; channel++) {
          if (this.numChannels > 1)
            val = val | ((pixelChannels[channel] & 0xFF) << ((this.numChannels - 1 - channel) * 8));
          else
            val = pixelChannels[channel];
        }
        this.ip.set(col + x, row + y, val);
      }
    }
  }

  @Override
  public ImageProcessor getResult() {
    return this.ip;
  }

  @Override
  public void postProcess() {
  }

}
