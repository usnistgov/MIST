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

package gov.nist.isg.mist.lib.executor;

import java.io.InvalidClassException;

import javax.swing.*;

import gov.nist.isg.mist.gui.StitchingGuiUtils;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.lib.exceptions.EmptyGridException;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.Stitching;
import gov.nist.isg.mist.lib.imagetile.java.JavaImageTile;
import gov.nist.isg.mist.lib.imagetile.memory.JavaTileWorkerMemory;
import gov.nist.isg.mist.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverser;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverserFactory;

/**
 * Sequential Java Stitching executor.
 *
 * @author Michael Majurski
 */
public class SequentialJavaStitchingExecutor<T> implements StitchingExecutorInterface<T> {

  private boolean isCanceled = false;

  /**
   * Initialize the sequential Java stitching executor tile grid.
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
        grid =
            new TileGrid<ImageTile<T>>(params, timeSlice, JavaImageTile.class);
      } catch (InvalidClassException e) {
        e.printStackTrace();
      }
    } else {
      try {
        grid = new TileGrid<ImageTile<T>>(params, JavaImageTile.class);
      } catch (InvalidClassException e) {
        e.printStackTrace();
      }
    }

    ImageTile<T> tile = grid.getTileThatExists();
    if (tile == null)
      throw new EmptyGridException("Image Tile Grid contains no valid tiles. Check " +
          "Stitching Parameters");
    tile.readTile();

    JavaImageTile.initJavaPlan(tile);

    return grid;
  }


  @Override
  public void cancelExecution() {
    Log.msg(Log.LogType.MANDATORY, "Canceling Stitching Java Sequential Executor");
    this.isCanceled = true;
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
  public void launchStitching(TileGrid<ImageTile<T>> grid, StitchingAppParams params,
                              JProgressBar progressBar, int timeSlice) throws Throwable {

    Log.msg(Log.LogType.MANDATORY, "Running Sequential Java Stitching");
    TileGridTraverser<ImageTile<T>> traverser = TileGridTraverserFactory.makeTraverser(
        TileGridTraverser.Traversals.DIAGONAL_CHAINED, grid);

    TileWorkerMemory memory = null;
    for (ImageTile<T> t : traverser) {
      if (this.isCanceled) return;

      t.setThreadID(0);


      t.readTile();

      if (memory == null) {
        memory = new JavaTileWorkerMemory(t);
      }
      int row = t.getRow();
      int col = t.getCol();

      t.computeFft();

      if (col > grid.getStartCol()) {

        ImageTile<T> west = grid.getTile(row, col - 1);
        t.setWestTranslation(Stitching.phaseCorrelationImageAlignment(west, t, memory));

        Log.msgNoTime(Log.LogType.HELPFUL, " pciam_W(\"" + t.getFileName() + "\",\"" + west
            .getFileName() + "\"): " + t.getWestTranslation());


        west.releaseFftMemory();
        west.releasePixels();

        if (progressBar != null)
          StitchingGuiUtils.incrementProgressBar(progressBar);

      }

      if (row > grid.getStartRow()) {
        ImageTile<T> north = grid.getTile(row - 1, col);

        t.setNorthTranslation(Stitching.phaseCorrelationImageAlignment(north, t, memory));

        Log.msgNoTime(
            Log.LogType.HELPFUL,
            " pciam_N(\"" + north.getFileName() + "\",\"" + t.getFileName() + "\"): "
                + t.getNorthTranslation());


        north.releaseFftMemory();
        north.releasePixels();

        if (progressBar != null)
          StitchingGuiUtils.incrementProgressBar(progressBar);
      }

      t.releaseFftMemory();
      t.releasePixels();

    }

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
    long memoryPoolCount = 2;
    ImageTile<T> tile = grid.getTileThatExists();
    tile.readTile();

    // Account for image pixel data
    requiredMemoryBytes += (long) tile.getHeight() * (long) tile.getWidth() * memoryPoolCount * 2L; // 16 bit pixel data

    // Account for image pixel data up conversion
    long byteDepth = tile.getBitDepth() / 8;
    if (byteDepth != 2) {
      // if up-converting at worst case there will be numWorkers copies of the old precision pixel data
      requiredMemoryBytes += (long) numWorkers * (long) tile.getHeight() * (long) tile.getWidth() * byteDepth;
    }

    // Account for Java FFT data
    int[] n = {JavaImageTile.fftPlan.getFrequencySampling2().getCount(),
        JavaImageTile.fftPlan.getFrequencySampling1().getCount() * 2};
    long size = 1;
    for (int val : n)
      size *= val;
    requiredMemoryBytes += memoryPoolCount * size * 4L; // float[n1][n2]

    requiredMemoryBytes += size * 4L; // new float[fftHeight][fftWidth];

    // pad with 100MB
    requiredMemoryBytes += 100L * 1024L * 1024L;

    return requiredMemoryBytes < Runtime.getRuntime().maxMemory();
  }
}
