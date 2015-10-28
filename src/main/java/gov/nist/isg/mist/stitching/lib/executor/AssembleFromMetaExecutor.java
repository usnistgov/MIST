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
// characteristic. We would appreciate acknowledgement if the software
// is used. This software can be redistributed and/or modified freely
// provided that any derivative works bear some notice that they are
// derived from it, and any modified versions bear some notice that
// they have been modified.
//
// ================================================================

// ================================================================
//
// Author: tjb3
// Date: Oct 1, 2014 1:41:32 PM EST
//
// Time-stamp: <Oct 1, 2014 1:41:32 PM tjb3>
//
// ================================================================
package gov.nist.isg.mist.stitching.lib.executor;

import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoaderUtils;
import jcuda.CudaException;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGridUtils;

import javax.swing.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;

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
  }

  @Override
  public boolean checkForLibs(StitchingAppParams params, boolean displayGui) {
    return true;
  }

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

  @Override
  public TileGrid<ImageTile<T>> initGrid(StitchingAppParams params, int timeSlice) {

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

    return grid;
  }

  @Override
  public void cleanup() {
  }

  @Override
  public <T> boolean checkMemory(TileGrid<ImageTile<T>> grid, int numWorkers) {
    return true;
  }

}
