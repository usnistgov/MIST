// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: May 10, 2013 2:58:58 PM EST
//
// Time-stamp: <May 10, 2013 2:58:58 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.imagetile;

import java.io.File;
import java.util.concurrent.Semaphore;

import gov.nist.isg.mist.lib.common.CorrelationTriple;
import gov.nist.isg.mist.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.memorypool.DynamicMemoryPool;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import jcuda.driver.CUstream;

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

  private CorrelationTriple preOptimizationWestTranslation;
  private CorrelationTriple preOptimizationNorthTranslation;

  private double tileCorrelation;

  private int fftReleaseCount;
  private int pixelDataReleaseCount;
  private int threadID;
  private int devID;

  private int mstReleaseCount = 0;

  private boolean fileExists = false;
  private boolean hasCheckedFileExists = false;

  /**
   * Creates an image tile from a file
   *
   * @param file the image tile file
   */
  public ImageTile(File file) {
    this(file, 0, 0, 1, 1, 0, 0);
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
   */
  public ImageTile(File file, int row, int col, int gridWidth, int gridHeight, int startRow,
                   int startCol) {
    this.pixelsLoaded = false;
    this.fpath = file.getAbsolutePath();
    this.fname = file.getName();

    this.pixels = null;
    this.width = 0;
    this.height = 0;

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

  public CorrelationTriple getTranslation(TileGrid.Direction dir) {
    switch (dir) {
      case North:
        return this.getNorthTranslation();
      case West:
        return this.getWestTranslation();
    }
    return null;
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
   * Checks if a tile has been read from disk or not
   *
   * @return true if the tile has been read, otherwise false
   */
  public boolean isTileRead() {
    return this.pixelsLoaded;
  }

  /**
   * Reads image tile from file
   */
  public void readTile() {
    if (this.isTileRead()) return;
    Log.msg(LogType.INFO, "Loading image: " + this.fpath);

    ImagePlus image = this.getImagePlus();

//    String[] commands = {"Despeckle","Gaussian Blur..."};
//    String[] params = {"", "sigma=1"};
//    for(int i = 0; i < commands.length; i++) {
//      IJ.run(image, commands[i], params[i]);
//    }


    if (image == null || image.getWidth() == 0 || image.getHeight() == 0)
      Log.msg(LogType.INFO, "Unable to read file: " + this.fpath);


    if (image != null) {
      this.width = image.getWidth();
      this.height = image.getHeight();
      this.bitDepth = image.getBitDepth();
      this.pixels = image.getProcessor();
      this.pixels.setCalibrationTable(null);
      this.pixelsLoaded = true;
    }
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

    // if this file does not exist, return null
    if (!this.fileExists())
      return null;

    ImagePlus image;
    if (Stitching.USE_BIOFORMATS) {
      image = BioFormatsReader.readImage(this.fpath);
    } else {
      image = new ImagePlus(this.fpath);
      // if the image is empty the read failed, so try bioformats
      if (image.getWidth() == 0 || image.getHeight() == 0) {
        Stitching.USE_BIOFORMATS = true;
        image = BioFormatsReader.readImage(this.fpath);
      }
    }

    return image;
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

  public String getFilePath() {
    return this.fpath;
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
   * Determine if this image tile exists on disk.
   *
   * @return true if the image exists on disk, false otherwise
   */
  public boolean fileExists() {
    // only check the disk for the file one, after that cache the result
    if (!this.hasCheckedFileExists) {
      this.fileExists = (new File(this.fpath)).exists();
      this.hasCheckedFileExists = true;
    }
    return this.fileExists;
  }

  /**
   * Set whether to this file exists, will override the Filesystem check. Warning, setting a file to
   * exist, that actually doesn't will cause a null pointer exception when its translations are
   * accessed.
   *
   * @param val whether the file exists on disk.
   */
  public void setFileExists(boolean val) {
    this.fileExists = val;
    this.hasCheckedFileExists = true;
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
   * Count to denote the number of neighboring tiles which have been added to the MST
   * @return
   */
  public int getMstReleaseCount() { return this.mstReleaseCount; }

  /**
   * Count to denote the number of neighboring tiles which have been added to the MST
   * @return
   */
  public void setMstReleaseCount(int val) { this.mstReleaseCount = val; }

  /**
   * Count to denote the number of neighboring tiles which have been added to the MST
   * @return
   */
  public void decrementMstConnectedNeighborCount() { this.mstReleaseCount--; }

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
  public abstract void computeFft();

  /**
   * Computes image's FFT without memory allocation asynchronously on GPU
   *
   * @param pool   dynamic memory pool
   * @param memory pre-allocated memory
   * @param stream CUDA CUstream
   */
  public abstract void computeFft(DynamicMemoryPool<T> pool, TileWorkerMemory memory,
                                  CUstream stream);

  /**
   * Computes the FFT for this tile using a pool, if the memory has not been allocated for the FFT
   *
   * @param pool   the pool of memory
   * @param memory the tile worker memory
   */
  public abstract void computeFft(DynamicMemoryPool<T> pool, TileWorkerMemory memory);

}
