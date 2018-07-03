// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.




// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 4:18:09 PM EST
//
// Time-stamp: <Aug 1, 2013 4:18:09 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.jcuda;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;

import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.jcuda.CudaUtils;
import gov.nist.isg.mist.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.lib.log.Debug;
import gov.nist.isg.mist.lib.log.Debug.DebugType;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.lib.tilegrid.loader.SequentialTileGridLoader;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoader;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoader.GridDirection;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoader.GridOrigin;
import gov.nist.isg.mist.lib32.imagetile.jcuda.CudaImageTile32;
import gov.nist.isg.mist.lib32.parallel.gpu.GPUStitchingThreadExecutor32;
import gov.nist.isg.mist.timing.TimeUtil;
import jcuda.driver.CUcontext;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.JCudaDriver;

/**
 * Test case for stitching a grid of tiles with multithreading using FFTW.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TestJCUDAGridPhaseCorrelationMultiThreaded32 {

  static {

    // Initialize libraries
    LibraryUtils.initalize();
  }

  /**
   * Computes the phase correlation using a multiple thread on a grid of tiles using FFTW
   */
  public static void runTestGridPhaseCorrelation() throws FileNotFoundException {
    int startRow = 0;
    int startCol = 0;
    int extentWidth = 23;
    int extentHeight = 30;

    File tileDir = new File("C:\\majurski\\image-data\\1h_Wet_10Perc\\");

    JCudaDriver.setExceptionsEnabled(false);
    Debug.setDebugLevel(DebugType.NONE);
    Log.setLogLevel(LogType.MANDATORY);
    // Debug.setDebugLevel(LogType.VERBOSE);
    Log.msg(LogType.MANDATORY, "Running Test Grid Phase Correlation Multithreaded JCUDA");


    Log.msg(LogType.INFO, "Generating tile grid");
    TileGrid<ImageTile<CUdeviceptr>> grid = null;
    try {
      TileGridLoader loader =
          new SequentialTileGridLoader(extentWidth, extentHeight, 1, 0,0,"KB_2012_04_13_1hWet_10Perc_IR_0{pppp}.tif", GridOrigin.UR,
              GridDirection.VERTICALCOMBING);

      grid =
          new TileGrid<ImageTile<CUdeviceptr>>(startRow, startCol, extentWidth, extentHeight,
              loader, tileDir, CudaImageTile32.class);
    } catch (InvalidClassException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }

    if (grid == null)
      return;

    ImageTile<CUdeviceptr> tile = grid.getSubGridTile(0, 0);
    tile.readTile();

    int[] devIDs = new int[]{0, 1, 2};
    boolean enableCudaExceptions = true;
    CUcontext[] contexts = CudaUtils.initJCUDA(1, devIDs, tile, enableCudaExceptions);

    GPUStitchingThreadExecutor32<CUdeviceptr> executor =
        new GPUStitchingThreadExecutor32<CUdeviceptr>(contexts.length, 12, tile, grid, contexts,
            devIDs);

    tile.releasePixels();

    Log.msg(LogType.INFO, "Computing translations");
    TimeUtil.tick();
    executor.execute();

    Log.msg(LogType.INFO, "Computing global optimization");

    Log.msg(LogType.INFO, "Computing absolute positions");

    Log.msg(LogType.MANDATORY, "Completed Test in " + TimeUtil.tock() + " ms");

  }

  /**
   * Executes the test case
   *
   * @param args not used
   */
  public static void main(String args[]) {
    try {
      TestJCUDAGridPhaseCorrelationMultiThreaded32.runTestGridPhaseCorrelation();
    } catch (FileNotFoundException e) {
      Log.msg(LogType.MANDATORY, "Unable to find file: " + e.getMessage());
    }
  }
}
