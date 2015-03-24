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
// Date: Oct 1, 2014 1:42:44 PM EST
//
// Time-stamp: <Oct 1, 2014 1:42:44 PM tjb3>
//
// ================================================================

package gov.nist.isg.mist.stitching.gui.executor;

import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.gui.params.objects.CudaDeviceParam;
import jcuda.CudaException;
import jcuda.driver.CUcontext;
import jcuda.driver.JCudaDriver;
import jcuda.jcufft.JCufft;
import jcuda.runtime.cudaError;
import gov.nist.isg.mist.stitching.gui.StitchingGuiUtils;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaUtils;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.parallel.gpu.GPUStitchingThreadExecutor;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;
import java.util.List;

/**
 * CudaStitchingExecutor executes the stitching using CUDA
 * @author Tim Blattner
 *
 * @param <T>
 */
public class CudaStitchingExecutor<T> implements StitchingExecutorInterface<T>{

  private boolean librariesInitialized;
  private boolean init;
  private CUcontext[] contexts;  
  private int[] devIDs;
  private StitchingExecutor executor;
  
  private GPUStitchingThreadExecutor<T>gpuExecutor;
  
  public CudaStitchingExecutor(StitchingExecutor executor) {   
    this.librariesInitialized = false;
    this.init = false;
    this.contexts = null;
    this.devIDs = null;
    this.executor = executor;
  }

  @Override
  public void cancelExecution() {
    if (this.gpuExecutor != null)
      this.gpuExecutor.cancel();
  }

  @Override
  public void launchStitching(TileGrid<ImageTile<T>> grid, StitchingAppParams params,
      JProgressBar progressBar, int timeSlice) throws OutOfMemoryError, CudaException, FileNotFoundException {
    
    List<CudaDeviceParam> devices = params.getAdvancedParams().getCudaDevices();
    ImageTile<T> tile = grid.getSubGridTile(0, 0);
    tile.readTile();

    
    
    if (!this.init)
    {
      StitchingGuiUtils.updateProgressBar(progressBar, true, "Initializing GPU(s)");
      
      if (devices.size() == 0) {
        this.devIDs = new int[] {0};
        Log.msg(LogType.MANDATORY, "No device selected from " + "table. Using default (0)");
        this.contexts = CudaUtils.initJCUDA(1, this.devIDs, tile);
      } else {
        Log.msg(LogType.MANDATORY, devices.size() + " device(s) selected from table.");
        this.contexts = CudaUtils.initJCUDA(devices, tile);
        this.devIDs = new int[devices.size()];
        for (int j = 0; j < devices.size(); j++)
          this.devIDs[j] = devices.get(j).getId();
      }

      this.executor.initProgressBar();
      
      this.init = true;
    }
    
    if (this.contexts == null)
    {
      Log.msg(LogType.MANDATORY, "Error initializing CUDA");
      throw new CudaException("Error initializing CUDA");
    }
    
    
    this.gpuExecutor = new GPUStitchingThreadExecutor<T>(this.contexts.length, params.getAdvancedParams().getNumCPUThreads(), tile, grid, this.contexts, this.devIDs, progressBar, this.executor);
    
    this.gpuExecutor.execute();

  }

  @Override
  public boolean checkForLibs(StitchingAppParams params, boolean displayGui) {
    
    if (this.librariesInitialized)
    {
      return true;
    }
    
    try {
      int[] count = new int[1];
      int ret = JCudaDriver.cuDeviceGetCount(count);
      if (ret == cudaError.cudaSuccess && count[0] > 0) {        
        this.librariesInitialized = true;
        return true;
      }

      JCufft.setExceptionsEnabled(true);
      JCufft.initialize();
      JCufft.setExceptionsEnabled(false);

      return true;
    } catch (UnsatisfiedLinkError err) {
      Log.msg(LogType.MANDATORY, "Error loading CUDA. Unable to find " + "required libraries: "
          + err.getMessage());
      Log.msg(LogType.MANDATORY, "You must install the cuda toolkit to "
          + "make this version work: nvidia.com/getcuda");
      return false;
    } catch (NullPointerException ex) {
      Log.msg(LogType.MANDATORY, "Error loading CUDA. Unable to find " + "required libraries: "
          + ex.getMessage());
      Log.msg(LogType.MANDATORY, "You must install the cuda toolkit to "
          + "make this version work: nvidia.com/getcuda");
      return false;
    }
  }
  
  @Override
  public TileGrid<ImageTile<T>> initGrid(StitchingAppParams params, int timeSlice) {
        
    TileGrid<ImageTile<T>> grid = null;
    
    if (params.getInputParams().isTimeSlicesEnabled())
    {    
      try {
        grid =
            new TileGrid<ImageTile<T>>(params, timeSlice, CudaImageTile.class);
      } catch (InvalidClassException e) {
        e.printStackTrace();
      }
    }
    else
    {
      try {
        grid = new TileGrid<ImageTile<T>>(params, CudaImageTile.class);
      } catch (InvalidClassException e) {
        e.printStackTrace();
      }
    }    
    
    return grid;
  }

  @Override
  public void cleanup() {
    CudaUtils.destroyJCUDA(this.contexts.length);    
  }


}
