// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Oct 1, 2014 1:44:34 PM EST
//
// Time-stamp: <Oct 1, 2014 1:44:34 PM tjb3>
//
// ================================================================
package gov.nist.isg.mist.lib.executor;

import java.io.File;
import java.io.InvalidClassException;

import javax.swing.JProgressBar;

import gov.nist.isg.mist.gui.StitchingGuiUtils;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.lib.exceptions.EmptyGridException;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.Stitching;
import gov.nist.isg.mist.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.parallel.cpu.CPUStitchingThreadExecutor;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.lib32.imagetile.fftw.FftwImageTile32;


/**
 * FftwStitching executes the stitching using FFTW
 *
 * @author Tim Blattner
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
    Log.msg(Log.LogType.MANDATORY, "Canceling Stitching FFTW Executor");
    if (this.fftwExecutor != null)
      this.fftwExecutor.cancel();
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
  public void launchStitching(TileGrid<ImageTile<T>> grid, StitchingAppParams params,
                              JProgressBar progressBar, int timeSlice) throws Throwable {

    ImageTile<T> tile = grid.getTileThatExists();
    tile.readTile();

    if (!this.init) {
      String defaultPlanName = tile.getWidth() + "x" + tile.getHeight() + params.getAdvancedParams().getFftwPlanType().toString();
      if (params.getAdvancedParams().isUseDoublePrecision()) {
        defaultPlanName = defaultPlanName + "Plan.dat";
      } else {
        defaultPlanName = defaultPlanName + "Plan_f.dat";
      }


      String plan = params.getAdvancedParams().getPlanPath();
      File file = new File(plan);

      if (file.exists() && file.isDirectory()) {
        plan = file.getAbsolutePath() + File.separator + defaultPlanName;
      }

      StitchingGuiUtils.updateProgressBar(progressBar, true, "Loading FFTW Plan...");

      if (params.getAdvancedParams().isUseDoublePrecision()) {
        FftwImageTile.initPlans(tile.getWidth(), tile.getHeight(), params.getAdvancedParams().getFftwPlanType()
            .getVal(), params.getAdvancedParams().isLoadFFTWPlan(), plan);

        if (params.getAdvancedParams().isSaveFFTWPlan())
          FftwImageTile.savePlan(plan);
      } else {
        FftwImageTile32.initPlans(tile.getWidth(), tile.getHeight(), params.getAdvancedParams().getFftwPlanType()
            .getVal(), params.getAdvancedParams().isLoadFFTWPlan(), plan);

        if (params.getAdvancedParams().isSaveFFTWPlan())
          FftwImageTile32.savePlan(plan);
      }

      this.init = true;

      Log.msg(LogType.MANDATORY, "Finished loading"
          + (params.getAdvancedParams().isSaveFFTWPlan() ? "/saving" : "") + " FFTW plan. Commencing stitching.");

      this.executor.initProgressBar();

    }

    this.fftwExecutor = new CPUStitchingThreadExecutor<T>(1, params.getAdvancedParams().getNumCPUThreads(), tile, grid, progressBar);

    tile.releasePixels();

    this.fftwExecutor.execute();

    if (this.fftwExecutor.isExceptionThrown())
      throw this.fftwExecutor.getWorkerThrowable();


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
    if (this.librariesInitialized) {
      return true;
    }

    String fftwLibraryPath = params.getAdvancedParams().getFftwLibraryPath();
    String fftwLibraryName = params.getAdvancedParams().getFftwLibraryName();
    String utilFnsPath = System.getProperty("user.dir") + File.separator + "util-fns";

    if (params.getAdvancedParams().isUseDoublePrecision()) {
      if (FftwImageTile.initLibrary(fftwLibraryPath, utilFnsPath, fftwLibraryName))
        this.librariesInitialized = true;
    } else {
      if (FftwImageTile32.initLibrary(fftwLibraryPath, utilFnsPath, fftwLibraryName))
        this.librariesInitialized = true;
    }

    return this.librariesInitialized;
  }

  /**
   * Initialize the FFTW stitching executor tile grid.
   *
   * @param params    the stitching params.
   * @param timeSlice the timeslice to stitch.
   * @return the TileGrid to be stitched when launchStitching is called.
   */
  @Override
  public TileGrid<ImageTile<T>> initGrid(StitchingAppParams params, int timeSlice) throws EmptyGridException {

    TileGrid<ImageTile<T>> grid = null;

    if (params.getAdvancedParams().isUseDoublePrecision()) {
      if (params.getInputParams().isTimeSlicesEnabled()) {
        try {
          grid = new TileGrid<ImageTile<T>>(params, timeSlice, FftwImageTile.class);
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
    } else {
      if (params.getInputParams().isTimeSlicesEnabled()) {
        try {
          grid = new TileGrid<ImageTile<T>>(params, timeSlice, FftwImageTile32.class);
        } catch (InvalidClassException e) {
          e.printStackTrace();
        }
      } else {
        try {
          grid = new TileGrid<ImageTile<T>>(params, FftwImageTile32.class);
        } catch (InvalidClassException e) {
          e.printStackTrace();
        }
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

    long size = (tile.getWidth() / 2 + 1) * tile.getHeight();
    requiredMemoryBytes += memoryPoolCount * size * 2 * 8; // fftw_alloc_real(size*2)

    // Account for FFTW fft data
    long perWorkerMemory = 0;
    if (tile instanceof FftwImageTile) {
      perWorkerMemory += (long) tile.getHeight() * (long) tile.getWidth() * 8L; // pcmP fftw_alloc_real
      perWorkerMemory += (long) tile.getHeight() * (long) tile.getWidth() * 8L; // fftInP fftw_alloc_real
      perWorkerMemory += size * 16L; // pcmInP fftw_alloc_complex
    } else {
      perWorkerMemory += (long) tile.getHeight() * (long) tile.getWidth() * 4L; // pcmP fftwf_alloc_real
      perWorkerMemory += (long) tile.getHeight() * (long) tile.getWidth() * 4L; // fftInP fftwf_alloc_real
      perWorkerMemory += size * 8L; // pcmInP fftwf_alloc_complex
    }
    perWorkerMemory += (long) Stitching.NUM_PEAKS * 4L; // peaks Pointer.allocateInts

    requiredMemoryBytes += perWorkerMemory * (long) numWorkers;

    // pad with 10MB
    requiredMemoryBytes += 10L * 1024L * 1024L;

    return requiredMemoryBytes < Runtime.getRuntime().maxMemory();

  }
}
