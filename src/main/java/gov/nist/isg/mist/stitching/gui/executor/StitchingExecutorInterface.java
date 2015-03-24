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
// Date: Oct 1, 2014 1:45:09 PM EST
//
// Time-stamp: <Oct 1, 2014 1:45:09 PM tjb3>
//
// ================================================================
package gov.nist.isg.mist.stitching.gui.executor;

import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import jcuda.CudaException;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;

import javax.swing.*;
import java.io.FileNotFoundException;


/**
 * StitchingExecutorInterface interface for various stitching executors
 * @author Tim Blattner
 *
 * @param <T>
 */
public interface StitchingExecutorInterface<T> {

  /**
   * Initializes a grid of tiles
   * @param params the stitching application params
   * @param timeSlice the timeslice
   * @return the grid initialized using the stitching app params
   */
  public abstract TileGrid<ImageTile<T>> initGrid(StitchingAppParams params, int timeSlice);
  
  /**
   * Cancels the execution
   */
  public abstract void cancelExecution();

  /**
   * Launches stitching
   * @param grid the image tile grid
   * @param params the stitching application parameters
   * @param progressBar the progress bar
   * @param timeSlice the timeslice

   * @throws OutOfMemoryError
   * @throws CudaException
   * @throws FileNotFoundException
   */
  public abstract void launchStitching(TileGrid<ImageTile<T>> grid, StitchingAppParams params, JProgressBar progressBar, int timeSlice) throws OutOfMemoryError, CudaException, FileNotFoundException;

  /**
   * Checks for required libraries.
   * @param params the stitching application params
   * @param displayGui whether to display gui or not
   * @return true if the libraries are available, otherwise false
   */
  public abstract boolean checkForLibs(StitchingAppParams params, boolean displayGui);

  
  /**
   * Cleans-up / releases any resources used by the executor
   */
  public abstract void cleanup();
  
}
