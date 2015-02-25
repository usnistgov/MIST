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

package main.gov.nist.isg.mist.stitching.lib.imagetile.java;

import java.io.File;
import java.io.FileNotFoundException;

import jcuda.driver.CUstream;
import main.gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import main.gov.nist.isg.mist.stitching.lib.imagetile.memory.TileWorkerMemory;
import main.gov.nist.isg.mist.stitching.lib.log.Log;
import main.gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import main.gov.nist.isg.mist.stitching.lib.memorypool.DynamicMemoryPool;

/**
 * Represents an image tile that uses only java (no native bindings). Must initialize an FFT plan
 * before using.
 * 
 * <pre>
 * <code>
 * JavaImageTile.initJavaPlan(initialTile);
 * </pre>
 * 
 * </code>
 * 
 * @author Tim Blattner
 * @version 2013.08.7
 * 
 */
public class JavaImageTile extends ImageTile<float[][]> {

  /**
   * The Java FFT plan
   */
  public static Fft fftPlan = null;

  /**
   * Creates an image tile in a grid
   * 
   * @param file the image tile file
   * @param row the row location in the grid
   * @param col the column location in the grid
   * @param gridWidth the width of the tile grid (subgrid)
   * @param gridHeight the height of the tile grid (subgrid)
   * @param startRow the start row of the tile grid (subgrid)
   * @param startCol the start column of the tile grid (subgrid)
   */
  public JavaImageTile(File file, int row, int col, int gridWidth, int gridHeight, int startRow,
      int startCol) {

    this(file, row, col, gridWidth, gridHeight, startRow, startCol, true);
  }


  /**
   * Creates an image tile in a grid
   * 
   * @param file the image tile file
   * @param row the row location in the grid
   * @param col the column location in the grid
   * @param gridWidth the width of the tile grid (subgrid)
   * @param gridHeight the height of the tile grid (subgrid)
   * @param startRow the start row of the tile grid (subgrid)
   * @param startCol the start column of the tile grid (subgrid)
   * @param read whether or not to read the tile here
   */
  public JavaImageTile(File file, int row, int col, int gridWidth, int gridHeight, int startRow,
      int startCol, boolean read) {
    super(file, row, col, gridWidth, gridHeight, startRow, startCol, read);
  }

  /**
   * Creates an image tile from a file
   * 
   * @param file the image tile file
   */
  public JavaImageTile(File file) {
    this(file, 0, 0, 1, 1, 0, 0, true);
  }


  /**
   * Initializes image tile and optionally does not read
   * 
   * @param file the file assosiated with this tile
   * @param read whether or not to read the tile here
   */
  public JavaImageTile(File file, boolean read) {
    this(file, 0, 0, 1, 1, 0, 0, read);
  }

  @Override
  public void releaseFftMemory() {
    if (this.fft != null)
      this.fft = null;
  }


  /**
   * Computes this image's FFT
   */
  @Override
  public void computeFft()throws FileNotFoundException{

    if (fftPlan == null)
      initJavaPlan(this);

    if (!isTileRead()) {
      readTile();
    }

    this.fft =
        new float[fftPlan.getFrequencySampling2().getCount()][fftPlan.getFrequencySampling1()
            .getCount() * 2];

    copyAndPadFFT();

    fftPlan.applyForwardPadded(this.fft);

  }

  /**
   * Computes this image's FFT
   * 
   * @param pool pool of memory that we might allocate memory from if it is not loaded
   * @param memory extra memory for input if needed
   */
  @Override
  public void computeFft(DynamicMemoryPool<float[][]> pool, TileWorkerMemory memory) throws FileNotFoundException {
    if (!isTileRead()) {
      readTile();
    }

    if (!super.isMemoryLoaded()) {
      this.fft = pool.getMemory();
      super.setMemoryLoaded(true);
    }


    copyAndPadFFT();

    fftPlan.applyForwardPadded(this.fft);
  }

  @Override
  public void computeFft(DynamicMemoryPool<float[][]> pool, TileWorkerMemory memory, CUstream stream) throws FileNotFoundException {
    computeFft(pool, memory);
  }

  /**
   * Copies pixel data into fft array for in place transform.
   */
  private void copyAndPadFFT() {
    // n1 = width
    // n2 = height
    int n1 = fftPlan.getFrequencySampling1().getCount() * 2;
    int n2 = fftPlan.getFrequencySampling2().getCount();

    for (int r = 0; r < n2; r++) {
      for (int c = 0; c < n1; c++) {

        if (r < this.getHeight() && c < this.getWidth()) {
          
          short val = super.getPixels()[r * this.getWidth() + c];
          int temp;
          
          if (val < 0)
          {
            temp = val & 0x7fff;
            temp |= 0x8000;
          }
          else
          {
            temp = val;
          }
          
          this.fft[r][c] = temp;
        } else {
          this.fft[r][c] = 0.0f;
        }
      }
    }


  }

  /**
   * Initializes FFT java plan. This plan might apply padding based on paddedHeight/2
   * 
   * @param tile the initial tile to get the width and height
   */
  public static void initJavaPlan(ImageTile<?> tile) throws FileNotFoundException {
    if (!tile.isTileRead())
      tile.readTile();

    Log.msg(LogType.VERBOSE, "Initializing Java FFT Plans.");

    fftPlan = new Fft(tile.getWidth(), tile.getHeight());
    fftPlan.setComplex(false);
    fftPlan.setOverwrite(true);
  }

}
