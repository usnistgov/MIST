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
// Date: Aug 1, 2013 1:07:06 PM EST
//
// Time-stamp: <Aug 1, 2013 1:07:06 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.common;

import java.awt.image.WritableRaster;

import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import ij.process.ImageProcessor;

/**
 * Utility for viewing an image tile two dimensionally, given a starting row, col, height, and
 * width. <p> The tile's data is accessed as:
 *
 * <pre>
 * <code> (row + startRow) * dataWidth + (col + startCol)
 * </code>
 * </pre>
 * <p> The class is used mainly to compute regions for CCF computation, but has been adapted intothe
 * visualizer as well for writing data to a writeable raster.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class Array2DView {

  private int startCol;
  private int startRow;
  private int viewWidth;
  private int viewHeight;
  private int dataWidth;
  private int dataHeight;

  private ImageProcessor data;

  /**
   * @param tile       the image tile backing the view
   * @param startCol   the views starting pixel column
   * @param startRow   the views starting pixel row
   * @param viewWidth  the view width
   * @param viewHeight the view height
   */
  public Array2DView(ImageTile<?> tile, int startRow, int viewHeight, int startCol, int viewWidth) {
    this.data = tile.getPixels();
    this.startCol = startCol;
    this.startRow = startRow;
    this.viewWidth = viewWidth;
    this.viewHeight = viewHeight;
    this.dataWidth = tile.getWidth();
    this.dataHeight = tile.getHeight();
  }

  /**
   * Gets pixel value inside of array2dView at index row and column
   *
   * @param row the row of the pixel
   * @param col the column of the pixel
   * @return the value at row and column
   */
  public double get(int row, int col) {
    return this.data.getPixelValue(col + this.startCol, row + this.startRow);
  }


  /**
   * Gets pixel value inside of array2dView at index row and column
   *
   * @param row the row of the pixel
   * @param col the column of the pixel
   * @return the value at row and column
   */
  public float getf(int row, int col) {
    return this.data.getPixelValue(col + this.startCol, row + this.startRow);
  }


  /**
   * Gets the index at the specified row and column
   *
   * @param row the row
   * @param col the column
   * @return the index
   */
  public int getIdx(int row, int col) {
    return (row + this.startRow) * this.dataWidth + (col + this.startCol);
  }

  /**
   * @return the startCol
   */
  public int getStartCol() {
    return this.startCol;
  }

  /**
   * @return the startRow
   */
  public int getStartRow() {
    return this.startRow;
  }

  /**
   * @return the viewWidth
   */
  public int getViewWidth() {
    return this.viewWidth;
  }

  /**
   * @return the viewHeight
   */
  public int getViewHeight() {
    return this.viewHeight;
  }

  /**
   * @return the dataWidth
   */
  public int getDataWidth() {
    return this.dataWidth;
  }

  /**
   * @return the dataHeight
   */
  public int getDataHeight() {
    return this.dataHeight;
  }

  /**
   * @return the data
   */
  public ImageProcessor getData() {
    return this.data;
  }

  /**
   * Computes the average in the array 2d view
   *
   * @return the average
   */
  public double getAvg() {
    double total = 0.0;
    double size = this.getViewWidth() * this.getViewHeight();

    for (int r = 0; r < this.getViewHeight(); r++) {
      for (int c = 0; c < this.getViewWidth(); c++) {
        total += get(r, c);
      }
    }

    return total / size;

  }

  /**
   * Computes the standard deviation using a two-pass method
   *
   * @return the standard deviation
   */
  public double getStdDevTwoPass() {

    double mean = getAvg();
    double size = this.getViewHeight() * this.getViewWidth();

    double v = 0.0;

    for (int r = 0; r < this.getViewHeight(); r++) {
      for (int c = 0; c < this.getViewWidth(); c++) {
        v = v + (get(r, c) - mean) * (get(r, c) - mean);
      }
    }

    return Math.sqrt(v / (size - 1.0));
  }

  /**
   * Writes the entire view into a raster from an x and y location
   *
   * @param raster the raster you are writing to
   * @param x      the x index inside of the raster
   * @param y      the y index inside of the rasters
   */
  public void writeViewData(WritableRaster raster, int x, int y) {
    Log.msg(LogType.VERBOSE, "x : " + x + " y: " + y + " viewHeight: " + this.viewHeight
        + " viewWidth: " + this.viewWidth);

    for (int row = 0; row < this.viewHeight; row++) {
      for (int col = 0; col < this.viewWidth; col++) {
        raster.setSample(col + x, row + y, 0, get(row, col));
      }
    }
  }

}
