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
 *
 * @author Tim Blattner
 */
public interface StitchingExecutorInterface<T> {

  /**
   * Initializes a grid of tiles
   *
   * @param params    the stitching application params
   * @param timeSlice the timeslice
   * @return the grid initialized using the stitching app params
   */
  TileGrid<ImageTile<T>> initGrid(StitchingAppParams params, int timeSlice)
      throws FileNotFoundException;

  /**
   * Cancels the execution
   */
  void cancelExecution();

  /**
   * Launches stitching
   *
   * @param grid        the image tile grid
   * @param params      the stitching application parameters
   * @param progressBar the progress bar
   * @param timeSlice   the timeslice
   */
  void launchStitching(TileGrid<ImageTile<T>> grid, StitchingAppParams params, JProgressBar progressBar, int timeSlice)
      throws Throwable;

  /**
   * Checks for required libraries.
   *
   * @param params     the stitching application params
   * @param displayGui whether to display gui or not
   * @return true if the libraries are available, otherwise false
   */
  boolean checkForLibs(StitchingAppParams params, boolean displayGui);


  /**
   * Cleans-up / releases any resources used by the executor
   */
  void cleanup();


  /**
   * Checks to see if the JVM has enough memory to launch this grid
   *
   * @param grid       the image tile grid
   * @param numWorkers the number of worker threads
   */
  <T> boolean checkMemory(TileGrid<ImageTile<T>> grid, int numWorkers)
      throws FileNotFoundException;


}
