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
// Date: Oct 1, 2014 1:44:51 PM EST
//
// Time-stamp: <Oct 1, 2014 1:44:51 PM tjb3>
//
// ================================================================

package gov.nist.isg.mist.lib.executor;

import java.io.InvalidClassException;

import javax.swing.*;

import gov.nist.isg.mist.gui.StitchingGuiUtils;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.lib.exceptions.EmptyGridException;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.java.JavaImageTile;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.parallel.cpu.CPUStitchingThreadExecutor;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.stitching.lib32.imagetile.java.JavaImageTile32;


/**
 * JavaStitchingExecutor executes the stitching using CUDA
 *
 * @author Tim Blattner
 */
public class JavaStitchingExecutor<T> implements StitchingExecutorInterface<T> {

  private boolean init;
  private CPUStitchingThreadExecutor<T> executor;

  public JavaStitchingExecutor() {
    this.init = false;
    this.executor = null;
  }


  @Override
  public void cancelExecution() {
    Log.msg(Log.LogType.MANDATORY, "Canceling Stitching Java Executor");
    if (this.executor != null)
      this.executor.cancel();
  }

  /**
   * Checks for the required libraries.
   *
   * @param params     the stitching application params
   * @param displayGui whether to display gui or not
   * @return flag denoting whether the libraries required for this executor were found.
   */
  @Override
  public boolean checkForLibs(StitchingAppParams params, boolean displayGui) {
    return true;
  }


  /**
   * Launches the sequential Java stitching.
   *
   * @param grid        the image tile grid
   * @param params      the stitching application parameters
   * @param progressBar the GUI progress bar
   * @param timeSlice   the timeslice to stitch
   */
  @Override
  public void launchStitching(TileGrid<ImageTile<T>> grid, StitchingAppParams params, JProgressBar progressBar, int timeSlice) throws Throwable {

    ImageTile<T> tile = grid.getTileThatExists();
    tile.readTile();

    this.executor =
        new CPUStitchingThreadExecutor<T>(1, params.getAdvancedParams().getNumCPUThreads(), tile, grid,
            progressBar);

    tile.releasePixels();

    StitchingGuiUtils.updateProgressBar(progressBar, false, null);


    this.executor.execute();
    if (this.executor.isExceptionThrown())
      throw this.executor.getWorkerThrowable();

  }

  /**
   * Initialize the Java stitching executor tile grid.
   *
   * @param params    the stitching params.
   * @param timeSlice the timeslice to stitch.
   * @return the TileGrid to be stitched when launchStitching is called.
   */
  @Override
  public TileGrid<ImageTile<T>> initGrid(StitchingAppParams params, int timeSlice) throws EmptyGridException {

    TileGrid<ImageTile<T>> grid = null;

    if (params.getInputParams().isTimeSlicesEnabled()) {
      try {
        if (params.getAdvancedParams().isUseDoublePrecision()) {
          grid = new TileGrid<ImageTile<T>>(params, timeSlice, JavaImageTile.class);
        } else {
          grid = new TileGrid<ImageTile<T>>(params, timeSlice, JavaImageTile32.class);
        }
      } catch (InvalidClassException e) {
        e.printStackTrace();
      }
    } else {
      try {
        if (params.getAdvancedParams().isUseDoublePrecision()) {
          grid = new TileGrid<ImageTile<T>>(params, JavaImageTile.class);
        } else {
          grid = new TileGrid<ImageTile<T>>(params, JavaImageTile32.class);
        }
      } catch (InvalidClassException e) {
        e.printStackTrace();
      }
    }

    ImageTile<T> tile = grid.getTileThatExists();
    if (tile == null)
      throw new EmptyGridException("Image Tile Grid contains no valid tiles. Check " +
          "Stitching Parameters");
    tile.readTile();

    if (params.getAdvancedParams().isUseDoublePrecision()) {
      JavaImageTile.initJavaPlan(tile);
    } else {
      JavaImageTile32.initJavaPlan(tile);
    }

    this.init = true;

    return grid;
  }

  @Override
  public void cleanup() {
  }


  /**
   * Determines if the system has the required memory to perform this stitching experiment as
   * configured.
   *
   * @param grid       the image tile grid
   * @param numWorkers the number of worker threads
   * @param <T>        the Type of ImageTile in the TileGrid
   * @return flag denoting whether the system has enough memory to stitch this experiment as is.
   */
  @Override
  public <T> boolean checkMemory(TileGrid<ImageTile<T>> grid, int numWorkers) {

    long requiredMemoryBytes = 0;
    long memoryPoolCount = Math.min(grid.getExtentHeight(), grid.getExtentWidth()) + 2 + numWorkers;
    ImageTile<T> tile = grid.getTileThatExists();
    tile.readTile();

    // Account for image pixel data
    if (ImageTile.freePixelData()) {
      // If freeing image pixel data
      requiredMemoryBytes += (long) tile.getHeight() * (long) tile.getWidth() * memoryPoolCount * 2L; // 16 bit pixel data
    } else {
      // If not freeing image pixel data
      // must hold whole image grid in memory
      requiredMemoryBytes += (long) tile.getHeight() * (long) tile.getWidth() * (long) grid.getSubGridSize() * 2L; // 16 bit pixel data
    }

    // Account for image pixel data up conversion
    long byteDepth = tile.getBitDepth() / 8;
    if (byteDepth != 2) {
      // if up-converting at worst case there will be numWorkers copies of the old precision pixel data
      requiredMemoryBytes += (long) numWorkers * (long) tile.getHeight() * (long) tile.getWidth() * byteDepth;
    }

    // Account for Java FFT data
    long size = 1;
    if (tile instanceof JavaImageTile) {
      int n[] = {JavaImageTile.fftPlan.getFrequencySampling2().getCount(),
          JavaImageTile.fftPlan.getFrequencySampling1().getCount() * 2};

      for (int val : n)
        size *= val;
    } else {
      int n[] = {JavaImageTile32.fftPlan.getFrequencySampling2().getCount(),
          JavaImageTile32.fftPlan.getFrequencySampling1().getCount() * 2};

      for (int val : n)
        size *= val;
    }


    requiredMemoryBytes += memoryPoolCount * size * 4L; // float[n1][n2]

    requiredMemoryBytes += size * 4L; // new float[fftHeight][fftWidth];

    // pad with 10MB
    requiredMemoryBytes += 10L * 1024L * 1024L;

    return requiredMemoryBytes < Runtime.getRuntime().maxMemory();
  }

}
