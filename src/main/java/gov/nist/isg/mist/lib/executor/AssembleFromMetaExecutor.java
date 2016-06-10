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
// Date: Oct 1, 2014 1:41:32 PM EST
//
// Time-stamp: <Oct 1, 2014 1:41:32 PM tjb3>
//
// ================================================================
package gov.nist.isg.mist.lib.executor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;

import javax.swing.*;

import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.lib.exceptions.EmptyGridException;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.Stitching;
import gov.nist.isg.mist.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.lib.tilegrid.TileGridUtils;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoaderUtils;
import jcuda.CudaException;

/**
 * Assemble from meta data executor updates a grid of tiles from a file
 *
 * @author Tim Blattner
 */
public class AssembleFromMetaExecutor<T> implements StitchingExecutorInterface<T> {

  public AssembleFromMetaExecutor() {

  }

  @Override
  public void cancelExecution() {
    Log.msg(Log.LogType.MANDATORY, "Canceling Stitching Assemble Metadata Executor");
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
   * Launches the stitching.
   *
   * @param grid        the image tile grid
   * @param params      the stitching application parameters
   * @param progressBar the GUI progress bar
   * @param timeSlice   the timeslice to stitch
   */
  @Override
  public void launchStitching(TileGrid<ImageTile<T>> grid, StitchingAppParams params, JProgressBar progressBar, int timeSlice) throws OutOfMemoryError,
      CudaException, FileNotFoundException {

    String absPosFilename = params.getInputParams().getGlobalPositionsFile();
    String parsedFilename;

    if (!TileGridLoaderUtils.hasTimeFilePattern(absPosFilename) && params.getInputParams().isTimeSlicesEnabled()) {
      throw new IllegalArgumentException("Timeslices are being used. The global positions filename should contain '{t}' to represent the timeslice.");
    }

    if (TileGridLoaderUtils.hasTimeFilePattern(absPosFilename)) {
      parsedFilename = TileGridLoaderUtils.parseTimeSlicePattern(absPosFilename, timeSlice, true);
    } else {
      parsedFilename = absPosFilename;
    }


    File absPosFile = new File(parsedFilename); //params.getOutputParams().getAbsPosFile(timeSlice);

    if (!absPosFile.exists()) {
      Log.msg(LogType.MANDATORY, "Error: Global position file does not exist for timeslice "
          + timeSlice + ": " + absPosFile.getAbsolutePath());

      throw new FileNotFoundException("Global position file not found: " + absPosFile.getAbsolutePath());
    }

    if (grid == null) {
      Log.msg(LogType.MANDATORY, "Error creating tile grid.");
      throw new NullPointerException("Grid is null");
    }

    if (Stitching.parseAbsolutePositions(grid, absPosFile)) {
      TileGridUtils.translateTranslations(grid);
    } else {
      throw new FileNotFoundException("Error parsing: " + absPosFile.getAbsolutePath());
    }

  }

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
            new TileGrid<ImageTile<T>>(params, timeSlice, FftwImageTile.class);
      } catch (InvalidClassException e) {
        e.printStackTrace();
      }
    } else {
      try {
        grid = new TileGrid<ImageTile<T>>(params, FftwImageTile.class);
      } catch (InvalidClassException e) {
        e.printStackTrace();
      }
    }

    if (grid.getTileThatExists() == null)
      throw new EmptyGridException("Image Tile Grid contains no valid tiles. Check " +
          "Stitching Parameters");

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
    return true;
  }

}
