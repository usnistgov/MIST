// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



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

import javax.swing.JProgressBar;

import gov.nist.isg.mist.gui.StitchingGuiUtils;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.lib.exceptions.EmptyGridException;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.java.JavaImageTile;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.parallel.cpu.CPUStitchingThreadExecutor;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.lib32.imagetile.java.JavaImageTile32;


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
