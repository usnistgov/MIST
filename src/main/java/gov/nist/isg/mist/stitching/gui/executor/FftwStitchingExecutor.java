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
// Date: Oct 1, 2014 1:44:34 PM EST
//
// Time-stamp: <Oct 1, 2014 1:44:34 PM tjb3>
//
// ================================================================
package gov.nist.isg.mist.stitching.gui.executor;

import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import jcuda.CudaException;
import gov.nist.isg.mist.stitching.gui.StitchingGuiUtils;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.parallel.cpu.CPUStitchingThreadExecutor;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;


/**
 * FftwStitching executes the stitching using FFTW
 * @author Tim Blattner
 *
 * @param <T>
 */
public class FftwStitchingExecutor<T> implements StitchingExecutorInterface<T> {

  private boolean librariesInitialized;
  private boolean init;
  private CPUStitchingThreadExecutor<T> fftwExecutor;
  private StitchingExecutor executor;
  
  public FftwStitchingExecutor(StitchingExecutor executor) {
    this.librariesInitialized = false;
    this.init = false;
    this.fftwExecutor = null;
    this.executor = executor;
  }

  @Override
  public void cancelExecution() {
    if (this.fftwExecutor != null)
      this.fftwExecutor.cancel();
  }

  @Override
  public void launchStitching(TileGrid<ImageTile<T>> grid, StitchingAppParams params,
      JProgressBar progressBar, int timeSlice) throws OutOfMemoryError, CudaException, FileNotFoundException {

    ImageTile<T> tile = grid.getSubGridTile(0, 0);
    tile.readTile();
    
    if (!this.init)
    {
      String defaultPlanName = tile.getWidth() + "x" + tile.getHeight() + params.getAdvancedParams().getFftwPlanType().toString()
          + "Plan.dat";
      
      String plan = params.getAdvancedParams().getPlanPath();
      File file = new File(plan);
           
      if (file.exists() && file.isDirectory()) {
        plan = file.getAbsolutePath() + File.separator + defaultPlanName;
      }
      
      StitchingGuiUtils.updateProgressBar(progressBar, true, "Loading FFTW Plan...");
      
      FftwImageTile.initPlans(tile.getWidth(), tile.getHeight(), params.getAdvancedParams().getFftwPlanType()
          .getVal(), params.getAdvancedParams().isLoadFFTWPlan(), plan);
      
      if (params.getAdvancedParams().isSaveFFTWPlan())
        FftwImageTile.savePlan(plan);

      this.init = true;

      Log.msg(LogType.MANDATORY, "Finished loading"
          + (params.getAdvancedParams().isSaveFFTWPlan() ? "/saving" : "") + " FFTW plan. Commencing stitching.");

      this.executor.initProgressBar();
      
    }
    
    this.fftwExecutor = new CPUStitchingThreadExecutor<T>(1, params.getAdvancedParams().getNumCPUThreads(), tile, grid, progressBar);

    tile.releasePixels();
    
    this.fftwExecutor.execute();
    
   
  }

  @Override
  public boolean checkForLibs(StitchingAppParams params, boolean displayGui) {
    if (this.librariesInitialized)
    {
      return true;
    }
           
    String fftwLibraryPath = params.getAdvancedParams().getFftwLibraryPath();
    String fftwLibraryName = params.getAdvancedParams().getFftwLibraryName();
    String utilFnsPath = System.getProperty("user.dir") + File.separator + "util-fns";
    
    if (FftwImageTile.initLibrary(fftwLibraryPath, utilFnsPath, fftwLibraryName))
    {
      this.librariesInitialized = true;
    }
          
    return this.librariesInitialized;
  }
  
  @Override
  public TileGrid<ImageTile<T>> initGrid(StitchingAppParams params, int timeSlice) {
        
    TileGrid<ImageTile<T>> grid = null;
    
    if (params.getInputParams().isTimeSlicesEnabled())
    {    
      try {
        grid =
            new TileGrid<ImageTile<T>>(params, timeSlice, FftwImageTile.class);
      } catch (InvalidClassException e) {
        e.printStackTrace();
      }
    }
    else
    {
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

}
