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
// Date: Apr 25, 2014 4:23:52 PM EST
//
// Time-stamp: <Apr 25, 2014 4:23:52 PM tjb3>
//
//
// ================================================================
package gov.nist.isg.mist.lib.export.blend;

import gov.nist.isg.mist.lib.common.Array2DView;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * Blending interface
 *
 * @author Tim Blattner
 * @version 1.0
 */
public interface Blender {

  /**

   */
  /**
   * Initializes the blending funciton
   *
   * @param width   the width of the stitched image
   * @param height  the height of the stitched image
   * @param initImg the initial image to help initialization for export
   * @throws OutOfMemoryError           out of memory error
   * @throws NegativeArraySizeException negative array size error, thrown when image dimensions
   *                                    exceed maximum size
   */
  public void init(int width, int height, ImagePlus initImg) throws OutOfMemoryError,
      NegativeArraySizeException;

  /**
   * Blends a pixel array into the final image
   *
   * @param x      the current x position in the final image
   * @param y      the current y position in the final image
   * @param pixels the 2D view of pixels that are being added
   * @param tile   the image tile to blend
   */
  public void blend(int x, int y, Array2DView pixels, ImageTile<?> tile);

  /**
   * Applies post-processing functions
   */
  public void postProcess();

  /**
   * Gets the result of the blending
   *
   * @return the resulting image
   */
  public ImageProcessor getResult();
}
