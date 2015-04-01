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
// Date: Oct 1, 2014 1:44:51 PM EST
//
// Time-stamp: <Oct 1, 2014 1:44:51 PM tjb3>
//
// ================================================================

package gov.nist.isg.mist.stitching.gui.executor;

import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import jcuda.CudaException;
import gov.nist.isg.mist.stitching.gui.StitchingGuiUtils;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.java.JavaImageTile;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.parallel.cpu.CPUStitchingThreadExecutor;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;


/**
 * JavaStitchingExecutor executes the stitching using CUDA
 * @author Tim Blattner
 *
 * @param <T>
 */
public class JavaStitchingExecutor<T> implements StitchingExecutorInterface<T> {

  private boolean init;  
  private CPUStitchingThreadExecutor<T>executor;

  public JavaStitchingExecutor() {
    this.init = false;
    this.executor = null;
  }

  @Override
  public void cancelExecution() {
    if (this.executor != null)
      this.executor.cancel();
  }


  @Override
  public boolean checkForLibs(StitchingAppParams params, boolean displayGui) {

    if (displayGui)
    {
      int res =
          JOptionPane.showConfirmDialog(null,
              "Warning: Using the Java stitching library uses single precision. \n"
                  + "If your results are not accurate enough please install FFTW or CUDA",
                  "Java Usage Warning", JOptionPane.WARNING_MESSAGE, JOptionPane.OK_CANCEL_OPTION,
                  null);
      if (res == JOptionPane.CANCEL_OPTION) {
        Log.msg(LogType.MANDATORY, "Java Execution cancelled.");
        return false;
      }
    }
    return true;
  }

  @Override
  public void launchStitching(TileGrid<ImageTile<T>> grid, StitchingAppParams params, JProgressBar progressBar, int timeSlice) throws OutOfMemoryError,
  CudaException, FileNotFoundException {

    ImageTile<T> tile = grid.getSubGridTile(0, 0);
    tile.readTile();

    if (!this.init) {
      JavaImageTile.initJavaPlan(tile);
      this.init = true;
    }

    this.executor =
        new CPUStitchingThreadExecutor<T>(1, params.getAdvancedParams().getNumCPUThreads(), tile, grid,
            progressBar);

    tile.releasePixels();

    StitchingGuiUtils.updateProgressBar(progressBar, false, null);



    this.executor.execute();        
  }

  @Override
  public TileGrid<ImageTile<T>> initGrid(StitchingAppParams params, int timeSlice) {

    TileGrid<ImageTile<T>> grid = null;

    if (params.getInputParams().isTimeSlicesEnabled())
    {    
      try {
        grid =
            new TileGrid<ImageTile<T>>(params, timeSlice, JavaImageTile.class);
      } catch (InvalidClassException e) {
        e.printStackTrace();
      }
    }
    else
    {
      try {
        grid = new TileGrid<ImageTile<T>>(params, JavaImageTile.class);
      } catch (InvalidClassException e) {
        e.printStackTrace();
      }
    }    

    return grid;
  }

  @Override
  public void cleanup() {    
  }

}
