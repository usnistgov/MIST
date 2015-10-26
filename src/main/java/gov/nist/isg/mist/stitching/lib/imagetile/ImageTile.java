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
// characteristic. We would appreciate acknowledgment if the software
// is used. This software can be redistributed and/or modified freely
// provided that any derivative works bear some notice that they are
// derived from it, and any modified versions bear some notice that
// they have been modified.
//
// ================================================================

// ================================================================
//
// Author: tjb3
// Date: May 10, 2013 2:58:58 PM EST
//
// Time-stamp: <May 10, 2013 2:58:58 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.imagetile;

import gov.nist.isg.mist.stitching.lib.common.Array2DView;
import gov.nist.isg.mist.stitching.lib.common.CorrelationTriple;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import jcuda.driver.CUstream;
import gov.nist.isg.mist.stitching.lib.memorypool.DynamicMemoryPool;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.Semaphore;

/**
 * Main image tile class that represents an image tile.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public abstract class ImageTile<T> implements Comparable<ImageTile<?>> {

  private static boolean freePixelData = true;

  /**
   * States of an image tile
   */
  public static enum State {
    /**
     * Nothing has been done
     */
    NONE,

    /**
     * The tile is currently in flight for processing
     */
    IN_FLIGHT,

    /**
     * The tile has completed processing
     */
    COMPLETE
  }

  protected T fft;

  private boolean memoryLoaded;

  private State fftState;
  private State pciamWestState;
  private State pciamNorthState;

  private int width;
  private int height;
  private String fpath;
  private String fname;

  private boolean pixelsLoaded;
  private ImageProcessor pixels;
  private int bitDepth;

  private int rowIdx;
  private int colIdx;

  private int absXPos;
  private int absYPos;

  private CorrelationTriple westTranslation;
  private CorrelationTriple northTranslation;

  private double stdDevNorthOverlapOrigin;
  private double stdDevNorthOverlapNeighbor;
  private double stdDevWestOverlapOrigin;
  private double stdDevWestOverlapNeighbor;

  private CorrelationTriple preOptimizationWestTranslation;
  private CorrelationTriple preOptimizationNorthTranslation;

  private double tileCorrelation;

  private int fftReleaseCount;
  private int pixelDataReleaseCount;
  private int threadID;
  private int devID;

  /**
   * Creates an image tile in a grid
   *
   * @param file       the image tile file
   * @param row        the row location in the grid
   * @param col        the column location in the grid
   * @param gridWidth  the width of the tile grid (subgrid)
   * @param gridHeight the height of the tile grid (subgrid)
   * @param startRow   the start row
   * @param startCol   the start column
   */
  public ImageTile(File file, int row, int col, int gridWidth, int gridHeight, int startRow,
                   int startCol) {
    this(file, row, col, gridWidth, gridHeight, startRow, startCol, true);
  }

  /**
   * Creates an image tile from a file
   *
   * @param file the image tile file
   */
  public ImageTile(File file) {
    this(file, 0, 0, 1, 1, 0, 0, true);
  }

  /**
   * Initializes image tile and optionally does not read
   *
   * @param file the file assosiated with this tile
   * @param read whether or not to read the tile here
   */
  public ImageTile(File file, boolean read) {
    this(file, 0, 0, 1, 1, 0, 0, read);
  }

  /**
   * Creates an image tile in a grid
   *
   * @param file       the image tile file
   * @param row        the row location in the grid
   * @param col        the column location in the grid
   * @param gridWidth  the width of the tile grid (subgrid)
   * @param gridHeight the height of the tile grid (subgrid)
   * @param startRow   the start row
   * @param startCol   the start column
   * @param read       whether or not to read the tile here
   */
  public ImageTile(File file, int row, int col, int gridWidth, int gridHeight, int startRow,
                   int startCol, boolean read) {
    this.pixelsLoaded = false;
    this.fpath = file.getAbsolutePath();
    this.fname = file.getName();

    if (read) {
      try {
        this.readTile();
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    } else {
      this.pixels = null;
      this.width = 0;
      this.height = 0;
    }

    this.rowIdx = row;
    this.colIdx = col;

    this.westTranslation = null;
    this.northTranslation = null;

    this.preOptimizationWestTranslation = null;
    this.preOptimizationNorthTranslation = null;

    this.absXPos = Integer.MAX_VALUE;
    this.absYPos = Integer.MAX_VALUE;

    this.tileCorrelation = -1.0;

    this.fftReleaseCount = getReleaseCount(gridWidth, gridHeight, startRow, startCol);
    this.pixelDataReleaseCount = getReleaseCount(gridWidth, gridHeight, startRow, startCol);

    this.fftState = State.NONE;
    this.pciamWestState = State.NONE;
    this.pciamNorthState = State.NONE;

    this.threadID = -1;
    this.devID = -1;

    this.stdDevNorthOverlapOrigin = Double.NaN;
    this.stdDevNorthOverlapNeighbor = Double.NaN;
    this.stdDevWestOverlapOrigin = Double.NaN;
    this.stdDevWestOverlapNeighbor = Double.NaN;

    this.memoryLoaded = false;
    this.fft = null;

  }

  /**
   * Constructs an image tile using only absolute position data
   *
   * @param file the image tile file
   * @param absX the absolute x position in the final image
   * @param absY the absolute y position in the final image
   * @param corr the tile correlation
   */
  public ImageTile(File file, int absX, int absY, double corr) {
    this.fpath = file.getAbsolutePath();
    this.fname = file.getName();
    this.absXPos = absX;
    this.absYPos = absY;
    this.tileCorrelation = corr;

    this.pixels = null;
  }


  /**
   * Computes the release count that is based on how many neighbors this tile has assuming that
   * there are tiles on the 4 cardinal directions (north, south, east, west).
   *
   * If a tile is on the edge of the grid, then its release count is 3, if the tile is on a corner
   * then the release count is 2, if the tile is in the center then the release count is 4.
   *
   * @return the release count based on how many neighbors the tile has
   */
  public int getReleaseCount(int gridWidth, int gridHeight, int startRow, int startCol) {
    return 2 + (this.colIdx - startCol < gridWidth - 1 && this.colIdx > startCol ? 1 : 0)
        + (this.rowIdx - startRow < gridHeight - 1 && this.rowIdx > startRow ? 1 : 0);
  }

  /**
   * Resets the pixel release count
   *
   * @param gridWidth  the grid width
   * @param gridHeight the grid height
   * @param startRow   the startRow
   * @param startCol   the startCol
   */
  public void resetPixelReleaseCount(int gridWidth, int gridHeight, int startRow, int startCol) {
    this.pixelDataReleaseCount = getReleaseCount(gridWidth, gridHeight, startRow, startCol);

  }

  /**
   * @param corr the correlation value to be set
   */
  public void setTileCorrelation(double corr) {
    this.tileCorrelation = corr;
  }

  /**
   * @return the tile's correlation
   */
  public double getTileCorrelation() {
    return this.tileCorrelation;
  }

  /**
   * @return a formatted tile correlation string
   */
  public String getTileCorrelationStr() {
    return String.format("%.10f", this.tileCorrelation);

  }

  /**
   * @return the abs_x_pos
   */
  public int getAbsXPos() {
    return this.absXPos;
  }

  /**
   * @param absXPos the abs_x_pos to set
   */
  public void setAbsXPos(int absXPos) {
    this.absXPos = absXPos;
  }

  /**
   * @return the abs_y_pos
   */
  public int getAbsYPos() {
    return this.absYPos;
  }

  /**
   * @param absYPos the abs_y_pos to set
   */
  public void setAbsYPos(int absYPos) {
    this.absYPos = absYPos;
  }

  /**
   * Subtracts the absolute position
   *
   * @param x the x position
   * @param y the y position
   */
  public void subractAbsPos(int x, int y) {
    this.absXPos -= x;
    this.absYPos -= y;
  }

  /**
   * Prints both west and north translations if available. If not translation then 0, 0 is printed
   */
  public void printTranslations() {
    if (this.northTranslation != null)
      Log.msg(LogType.MANDATORY, " pciam_N(" + this.getFileName() + ") " + this.northTranslation);
    else
      Log.msg(LogType.MANDATORY,
          " pciam_N(" + this.getFileName() + ") " + CorrelationTriple.toStringStatic());
    if (this.westTranslation != null)
      Log.msg(LogType.MANDATORY, " pciam_W(" + this.getFileName() + ") " + this.westTranslation);
    else
      Log.msg(LogType.MANDATORY,
          " pciam_W(" + this.getFileName() + ") " + CorrelationTriple.toStringStatic());
  }

  /**
   * @return the fft_state
   */
  public State getFftState() {
    return this.fftState;
  }

  /**
   * @param fftState the fft_state to set
   */
  public void setFftState(State fftState) {
    this.fftState = fftState;
  }

  /**
   * @return the pciam_west_state
   */
  public State getPciamWestState() {
    return this.pciamWestState;
  }

  /**
   * @param pciamWestState the pciam_west_state to set
   */
  public void setPciamWestState(State pciamWestState) {
    this.pciamWestState = pciamWestState;
  }

  /**
   * @return the pciam_north_state
   */
  public State getPciamNorthState() {
    return this.pciamNorthState;
  }

  /**
   * @param pciamNorthState the pciam_north_state to set
   */
  public void setPciamNorthState(State pciamNorthState) {
    this.pciamNorthState = pciamNorthState;
  }

  /**
   * @return the pixel array
   */
  public ImageProcessor getPixels() {
    return this.pixels;
  }

  /**
   * Gets the release count for image tile
   *
   * @return the release count
   */
  public int getFftReleaseCount() {
    return this.fftReleaseCount;
  }

  /**
   * Decrements release count
   */
  public void decrementFftReleaseCount() {
    this.fftReleaseCount--;
  }

  /**
   * @return the pixel data release count
   */
  public int getPixelDataReleaseCount() {
    return this.pixelDataReleaseCount;
  }

  /**
   * Sets the pixel data release count
   *
   * @param val the pixel data release count
   */
  public void setPixelDataReleaseCount(int val) {
    this.pixelDataReleaseCount = val;
  }

  /**
   * Decrements pixel data release count
   */
  public void decrementPixelDataReleaseCount() {
    this.pixelDataReleaseCount--;
  }

  /**
   * Increments the pixel data release count
   */
  public void incrementPixelDataReleaseCount() {
    this.pixelDataReleaseCount++;
  }

  /**
   * Gets the thread id associated with this image tile
   *
   * @return threadID the thread id associated with the thread
   */
  public int getThreadID() {
    return this.threadID;
  }

  /**
   * Sets the thread id associated with this image tile
   *
   * @param threadID the thread id associated with the thread
   */
  public void setThreadID(int threadID) {
    this.threadID = threadID;
  }

  /**
   * Gets west translation
   *
   * @return the west translation
   */
  public CorrelationTriple getWestTranslation() {
    return this.westTranslation;
  }

  /**
   * Gets north translation
   *
   * @return the north translation
   */
  public CorrelationTriple getNorthTranslation() {
    return this.northTranslation;
  }

  /**
   * Sets west translation for image tile
   *
   * @param corrTriple the correlation
   */
  public void setWestTranslation(CorrelationTriple corrTriple) {
    this.westTranslation = corrTriple;
  }

  /**
   * Sets north translation for image tile
   *
   * @param corrTriple the correlation
   */
  public void setNorthTranslation(CorrelationTriple corrTriple) {
    this.northTranslation = corrTriple;
  }

  /**
   * @return the originalWestTranslation
   */
  public CorrelationTriple getPreOptimizationWestTranslation() {
    return this.preOptimizationWestTranslation;
  }

  /**
   * @param preOptimizationWestTranslation the originalWestTranslation to set
   */
  public void setPreOptimizationWestTranslation(CorrelationTriple preOptimizationWestTranslation) {
    this.preOptimizationWestTranslation = preOptimizationWestTranslation;
  }

  /**
   * @return the originalNorthTranslation
   */
  public CorrelationTriple getPreOptimizationNorthTranslation() {
    return this.preOptimizationNorthTranslation;
  }

  /**
   * @param preOptimizationNorthTranslation the originalNorthTranslation to set
   */
  public void setPreOptimizationNorthTranslation(CorrelationTriple preOptimizationNorthTranslation) {
    this.preOptimizationNorthTranslation = preOptimizationNorthTranslation;
  }

  /**
   * Gets the lowest standard deviation north between the origin tile and its neighbor
   *
   * @return the lowest standard deviation for the north overlap
   */
  public double getLowestStdDevNorth() {
    return getStdDevNorthOverlapOrigin() < getStdDevNorthOverlapNeighbor() ? getStdDevNorthOverlapOrigin()
        : getStdDevNorthOverlapNeighbor();

  }

  /**
   * Gets the lowest standard deviation west between the origin tile and its neighbor
   *
   * @return the lowest standard deviation for the west overlap
   */
  public double getLowestStdDevWest() {
    return getStdDevWestOverlapOrigin() < getStdDevWestOverlapNeighbor() ? getStdDevWestOverlapOrigin()
        : getStdDevWestOverlapNeighbor();
  }

  /**
   * Computes the standard deviation for the northern overlap. The overlap is based on the north
   * translation for this tile
   *
   * @param neighbor this tile's northern neighbor
   */
  public void computeStdDevNorth(ImageTile<T> neighbor) throws FileNotFoundException {
    CorrelationTriple translation = this.getNorthTranslation();

    if (translation == null)
      return;

    int x = translation.getX();
    int y = translation.getY();

    if (Double.isNaN(getStdDevNorthOverlapOrigin())) {
      this.readTile();

      Array2DView originView = null;
      if (x >= 0) {
        originView = new Array2DView(this, 0, this.height - y, 0, this.width - x);
      } else {
        originView = new Array2DView(this, 0, this.height - y, -x, this.width + x);

      }

      this.setStdNorthOverlapOrigin(originView.getStdDevTwoPass());

    }

    if (Double.isNaN(getStdDevNorthOverlapNeighbor())) {
      neighbor.readTile();

      Array2DView neighborView = null;

      if (x >= 0) {
        neighborView = new Array2DView(neighbor, y, this.height - y, x, this.width - x);
      } else {
        neighborView = new Array2DView(neighbor, y, this.height - y, 0, this.width + x);
      }

      this.setStdNorthOverlapNeighbor(neighborView.getStdDevTwoPass());
    }

  }

  /**
   * Computes the standard deviation for the western overlap. The overlap is based on the west
   * translation for this tile
   *
   * @param neighbor this tile's western neighbor
   */
  public void computeStdDevWest(ImageTile<T> neighbor) throws FileNotFoundException {
    CorrelationTriple translation = this.getWestTranslation();

    if (translation == null)
      return;

    int x = translation.getX();
    int y = translation.getY();

    if (Double.isNaN(getStdDevWestOverlapOrigin())) {
      this.readTile();

      Array2DView originView = null;
      if (y >= 0) {
        originView = new Array2DView(this, y, this.height - y, 0, this.width - x);
      } else {
        originView = new Array2DView(this, 0, this.height + y, 0, this.width - x);

      }

      this.setStdWestOverlapOrigin(originView.getStdDevTwoPass());

    }

    if (Double.isNaN(getStdDevWestOverlapNeighbor())) {
      neighbor.readTile();

      Array2DView neighborView = null;

      if (y >= 0) {
        neighborView = new Array2DView(neighbor, 0, this.height - y, x, this.width - x);
      } else {
        neighborView = new Array2DView(neighbor, -y, this.height + y, x, this.width - x);
      }

      this.setStdWestOverlapNeighbor(neighborView.getStdDevTwoPass());
    }

  }

  /**
   * Computes the standard deviation for the northern overlap. The overlap is based on the overlap
   * +- percentOverlapError
   *
   * @param neighbor         this tile's northern neighbor
   * @param overlap          the percent overlap
   * @param percOverlapError the percent overlap error
   */
  public void computeStdDevNorth(ImageTile<T> neighbor, double overlap, double percOverlapError) throws FileNotFoundException {
    if (Double.isNaN(getStdDevNorthOverlapOrigin())) {
      this.readTile();

      Array2DView originView =
          new Array2DView(this, 0, (int) Math.round((overlap + percOverlapError) * this.getHeight()
              / 100.0), 0, this.getWidth());

      this.setStdNorthOverlapOrigin(originView.getStdDevTwoPass());
    }

    if (Double.isNaN(getStdDevNorthOverlapNeighbor())) {
      neighbor.readTile();

      Array2DView neighborView =
          new Array2DView(neighbor, neighbor.getHeight()
              - (int) Math.round((overlap + percOverlapError) * neighbor.getHeight() / 100.0),
              (int) Math.round((overlap + percOverlapError) * neighbor.getHeight() / 100.0), 0,
              this.getWidth());

      this.setStdNorthOverlapNeighbor(neighborView.getStdDevTwoPass());
    }

  }

  /**
   * Resets the north standard deviations to NaN
   */
  public void resetStdDevNorth() {
    this.setStdNorthOverlapOrigin(Double.NaN);
    this.setStdNorthOverlapNeighbor(Double.NaN);
  }

  /**
   * Computes the standard deviation for the western overlap. The overlap is based on the overlap +-
   * percentOverlapError
   *
   * @param neighbor         this tile's western neighbor
   * @param overlap          the percent overlap
   * @param percOverlapError the percent overlap error
   */
  public void computeStdDevWest(ImageTile<T> neighbor, double overlap, double percOverlapError) throws FileNotFoundException {
    if (Double.isNaN(getStdDevWestOverlapOrigin())) {

      this.readTile();

      Array2DView originView =
          new Array2DView(this, 0, this.getHeight(), 0,
              (int) Math.round((overlap + percOverlapError) * this.getWidth() / 100.0));

      this.setStdWestOverlapOrigin(originView.getStdDevTwoPass());
    }

    if (Double.isNaN(getStdDevWestOverlapNeighbor())) {
      neighbor.readTile();

      Array2DView neighborView =
          new Array2DView(neighbor, 0, this.getHeight(), neighbor.getWidth()
              - (int) Math.round((overlap + percOverlapError) * neighbor.getWidth() / 100.0),
              (int) Math.round((overlap + percOverlapError) * neighbor.getWidth() / 100.0));

      this.setStdWestOverlapNeighbor(neighborView.getStdDevTwoPass());
    }
  }

  /**
   * Resets the west standard deviations to NaN
   */
  public void resetStdDevWest() {
    this.setStdWestOverlapOrigin(Double.NaN);
    this.setStdWestOverlapNeighbor(Double.NaN);
  }

  /**
   * Checks if the north standard deviation is below a threshold
   *
   * @param threshold the standard deviation threshold
   * @return true if either of the origin or neighbor's standard deviation is below the threshold
   */
  public boolean hasLowStdDevNorth(double threshold) {
    if (getStdDevNorthOverlapOrigin() < threshold || getStdDevNorthOverlapNeighbor() < threshold)
      return true;
    return false;
  }

  /**
   * Checks if the west standard deviation is below a threshold
   *
   * @param threshold the standard deviation threshold
   * @return true if either of the origin or neighbor's standard deviation is below the threshold
   */
  public boolean hasLowStdDevWest(double threshold) {
    if (getStdDevWestOverlapOrigin() < threshold || getStdDevWestOverlapNeighbor() < threshold)
      return true;
    return false;
  }

  /**
   * @return the stdNorthOverlapOrigin
   */
  public double getStdDevNorthOverlapOrigin() {
    return this.stdDevNorthOverlapOrigin;
  }

  /**
   * @param stdDevNorthOverlapOrigin the stdNorthOverlapOrigin to set
   */
  public void setStdNorthOverlapOrigin(double stdDevNorthOverlapOrigin) {
    this.stdDevNorthOverlapOrigin = stdDevNorthOverlapOrigin;
  }

  /**
   * @return the stdNorthOverlapNeighbor
   */
  public double getStdDevNorthOverlapNeighbor() {
    return this.stdDevNorthOverlapNeighbor;
  }

  /**
   * @param stdDevNorthOverlapNeighbor the stdNorthOverlapNeighbor to set
   */
  public void setStdNorthOverlapNeighbor(double stdDevNorthOverlapNeighbor) {
    this.stdDevNorthOverlapNeighbor = stdDevNorthOverlapNeighbor;
  }

  /**
   * @return the stdWestOverlapOrigin
   */
  public double getStdDevWestOverlapOrigin() {
    return this.stdDevWestOverlapOrigin;
  }

  /**
   * @param stdDevWestOverlapOrigin the stdWestOverlapOrigin to set
   */
  public void setStdWestOverlapOrigin(double stdDevWestOverlapOrigin) {
    this.stdDevWestOverlapOrigin = stdDevWestOverlapOrigin;
  }

  /**
   * @return the stdWestOverlapNeighbor
   */
  public double getStdDevWestOverlapNeighbor() {
    return this.stdDevWestOverlapNeighbor;
  }

  /**
   * @param stdDevWestOverlapNeighbor the stdWestOverlapNeighbor to set
   */
  public void setStdWestOverlapNeighbor(double stdDevWestOverlapNeighbor) {
    this.stdDevWestOverlapNeighbor = stdDevWestOverlapNeighbor;
  }

  /**
   * Checks if a tile has been read from disk or not
   *
   * @return true if the tile has been read, otherwise false
   */
  public boolean isTileRead() {
    return this.pixelsLoaded;
  }

  /**
   * Reads image tile from file
   *
   * @throws java.io.FileNotFoundException if the file does not exist
   */
  public void readTile() throws FileNotFoundException {
    if (this.isTileRead()) return;
    Log.msg(LogType.INFO, "Loading image: " + this.fpath);

    ImagePlus image = new ImagePlus(this.fpath);

    this.width = image.getWidth();
    this.height = image.getHeight();

    if (this.width == 0 || this.height == 0) {
      Log.msg(LogType.MANDATORY, "Error: Unable to read file: " + this.fpath);
      Log.msg(LogType.MANDATORY,
          "Please ensure your grid parameters are correctly setup (origin, direction, width, height)");

      throw new FileNotFoundException(this.fpath);
    }

    this.bitDepth = image.getBitDepth();
    this.pixels = image.getProcessor();
    this.pixels.setCalibrationTable(null);
    this.pixelsLoaded = true;
  }

  /**
   * Gets the raw image processor for this tile. Used to get the pixel values from the original
   * image.
   *
   * @return the image processor for the tile
   */
  public ImageProcessor getImageProcessor() {
    return this.pixels;
  }

  /**
   * Gets an ImagePlus object of this tile
   *
   * @return the image plus object for this tile
   */
  public ImagePlus getImagePlus() {
    return new ImagePlus(this.fpath);
  }

  /**
   * Gets the bit depth for the image: e.g. 16-bit; 32-bit...
   *
   * @return the bit depth
   */
  public int getBitDepth() {
    return this.bitDepth;
  }

  /**
   * Releases pixels only if the freePixelData flag is enabled
   */
  public void releasePixels() {
    if (freePixelData) {
      this.pixels = null;
      this.pixelsLoaded = false;
    }
  }

  /**
   * Forces freeing pixels immediately
   */
  public void releasePixelsNow() {
    this.pixels = null;
    this.pixelsLoaded = false;
  }


  public void releasePixels(Semaphore sem) {
    this.pixels = null;
    this.pixelsLoaded = false;
    sem.release();
  }

  public void releasePixelsNow(Semaphore sem) {
    this.pixels = null;
    this.pixelsLoaded = false;
    sem.release();
  }


  /**
   * Gets the filename for this image tile.
   *
   * @return the filename string
   */
  public String getFileName() {
    return this.fname;
  }

  /**
   * Converts image tile into human readable string.
   */
  @Override
  public String toString() {
    return "Image: " + this.fname + " path: " + this.fpath + " width: " + this.width + " height: " + this.height
        + " contains pixels: " + (this.pixels == null ? "-1 (not loaded)" : this.pixels.getWidth() * this.pixels.getHeight()) + " abs x: "
        + this.absXPos + " abs y: " + this.absYPos;
  }

  /**
   * Gets the row index
   *
   * @return the row index
   */
  public int getRow() {
    return this.rowIdx;
  }

  /**
   * Gets column index
   *
   * @return the column index
   */
  public int getCol() {
    return this.colIdx;
  }

  /**
   * @return the width
   */
  public int getWidth() {
    return this.width;
  }

  /**
   * @return the height
   */
  public int getHeight() {
    return this.height;
  }

  /**
   * Determines if this image tile and another image tile are neighbors along a row
   *
   * @param o the neighboring image tile
   * @return true if the two tiles are row neighbors
   */
  public boolean isSameRowAs(ImageTile<T> o) {
    return this.getRow() == o.getRow();
  }

  /**
   * Determines if this image tile and another image tile are neighbors along a column
   *
   * @param o the neighboring image tile
   * @return true if the two tiles are column neighbors
   */
  public boolean isSameColAs(ImageTile<T> o) {
    return this.getCol() == o.getCol();
  }

  /**
   * Checks if this tile is north of another tile
   *
   * @param other the other tile
   * @return true if this tile is north of another tile, otherwise false
   */
  public boolean isNorthOf(ImageTile<T> other) {
    return isSameColAs(other) && this.getRow() == other.getRow() - 1;
  }

  /**
   * Checks if this tile is west of another tile
   *
   * @param other the other tile
   * @return true if this tile is west of another tile, otherwise false
   */
  public boolean isWestOf(ImageTile<T> other) {
    return isSameRowAs(other) && this.getCol() == other.getCol() - 1;
  }

  /**
   * Checks if this tile is south of another tile
   *
   * @param other the other tile
   * @return true if this tile is south of another tile, otherwise false
   */
  public boolean isSouthOf(ImageTile<T> other) {
    return isSameColAs(other) && this.getRow() == other.getRow() + 1;
  }

  /**
   * Checks if this tile is east of another tile
   *
   * @param other the other tile
   * @return true if this tile is east of another tile, otherwise false
   */
  public boolean isEastOf(ImageTile<T> other) {
    return isSameRowAs(other) && this.getCol() == other.getCol() + 1;
  }


  /**
   * Gets the correlation associated with the neighbor image tile
   *
   * @param neighbor the neighbor image tile to reference
   * @return the correlation associated with the neighbor
   */
  public double getCorr(ImageTile<T> neighbor) {
    if (this.isNorthOf(neighbor))
      return neighbor.getNorthTranslation().getCorrelation();
    else if (this.isWestOf(neighbor))
      return neighbor.getWestTranslation().getCorrelation();
    else if (this.isSouthOf(neighbor))
      return this.getNorthTranslation().getCorrelation();
    else if (this.isEastOf(neighbor))
      return this.getWestTranslation().getCorrelation();
    else
      throw new IllegalAccessError(neighbor.toString() + " is not a neighbor of " + this.toString());
  }

  /**
   * Updates the absolute position of this tile relative to another tile
   *
   * @param other the other tile that we are updating based on
   */
  public void updateAbsolutePosition(ImageTile<T> other) {
    int x = other.getAbsXPos();
    int y = other.getAbsYPos();
    int newX, newY;

    CorrelationTriple corr;
    if (this.isNorthOf(other)) {
      corr = other.getNorthTranslation();
      newX = x - corr.getX();
      newY = y - corr.getY();

      this.setAbsXPos(newX);
      this.setAbsYPos(newY);

    } else if (this.isWestOf(other)) {
      corr = other.getWestTranslation();
      newX = x - corr.getX();
      newY = y - corr.getY();

      this.setAbsXPos(newX);
      this.setAbsYPos(newY);
    } else if (this.isSouthOf(other)) {
      corr = this.getNorthTranslation();
      newX = x + corr.getX();
      newY = y + corr.getY();

      this.setAbsXPos(newX);
      this.setAbsYPos(newY);

    } else if (this.isEastOf(other)) {
      corr = this.getWestTranslation();
      newX = x + corr.getX();
      newY = y + corr.getY();

      this.setAbsXPos(newX);
      this.setAbsYPos(newY);
    }
  }

  @Override
  public int compareTo(ImageTile<?> o) {

    return this.getFileName().compareTo(o.getFileName());
  }

  /**
   * Get whether the system should free pixel data or not
   *
   * @return true if the system should free pixel data, otherwise false
   */
  public static boolean freePixelData() {
    return freePixelData;
  }

  /**
   * Enable freeing pixel data
   */
  public static void enableFreePixelData() {
    freePixelData = true;
  }

  /**
   * Disable freeing pixel data
   */
  public static void disableFreePixelData() {
    freePixelData = false;
  }

  /**
   * Releases memory into a pool
   *
   * @param pool the pool to put memory into
   */
  public void releaseFftMemory(DynamicMemoryPool<T> pool) {
    pool.addMemory(this.fft);
    this.memoryLoaded = false;
  }

  /**
   * Allocated Memory
   *
   * @param pool the memory pool
   */
  public void allocateFftMemory(DynamicMemoryPool<T> pool) {
    if (!this.memoryLoaded) {
      this.fft = pool.getMemory();
      this.memoryLoaded = true;
    }
  }

  /**
   * Gets the device number for the GPU
   *
   * @return the device number associated with the GPU
   */
  public int getDev() {
    return this.devID;
  }

  /**
   * Sets the GPU device number
   *
   * @param dev the device number associated with the GPU
   */
  public void setDev(int dev) {
    this.devID = dev;
  }

  /**
   * Determines if an fft exists or not
   *
   * @return if the fft is null, then false, otherwise true
   */
  public boolean hasFft() {
    return this.fft != null;
  }

  /**
   * Gets this image tile's fft
   *
   * @return the fft
   * @throws NullPointerException if the FFT has not been computed
   */
  public T getFft() throws NullPointerException {
    if (!hasFft())
      throw new NullPointerException("FFT has not been computed for " + this.getFileName());

    return this.fft;
  }

  /**
   * @return if the memory is loaded or not
   */
  public boolean isMemoryLoaded() {
    return this.memoryLoaded;
  }


  /**
   * Sets if the memory is loaded or not
   */
  public void setMemoryLoaded(boolean val) {
    this.memoryLoaded = val;
  }

  /**
   * Releases memory for this tile
   */
  public abstract void releaseFftMemory();

  /**
   * Computes this image's FFT
   */
  public abstract void computeFft() throws FileNotFoundException;

  /**
   * Computes image's FFT without memory allocation asynchronously on GPU
   *
   * @param pool   dynamic memory pool
   * @param memory pre-allocated memory
   * @param stream CUDA CUstream
   */
  public abstract void computeFft(DynamicMemoryPool<T> pool, TileWorkerMemory memory,
                                  CUstream stream) throws FileNotFoundException;

  /**
   * Computes the FFT for this tile using a pool, if the memory has not been allocated for the FFT
   *
   * @param pool   the pool of memory
   * @param memory the tile worker memory
   */
  public abstract void computeFft(DynamicMemoryPool<T> pool, TileWorkerMemory memory) throws FileNotFoundException;

}
