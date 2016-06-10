// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.




// ================================================================
//
// Author: tjb3
// Date: May 10, 2013 2:59:15 PM EST
//
// Time-stamp: <May 10, 2013 2:59:15 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.jcuda;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import gov.nist.isg.mist.lib.common.CorrelationTriple;
import gov.nist.isg.mist.lib.imagetile.Stitching;
import gov.nist.isg.mist.lib.imagetile.jcuda.CudaImageTile;
import gov.nist.isg.mist.lib.imagetile.jcuda.CudaUtils;
import gov.nist.isg.mist.lib.imagetile.memory.CudaTileWorkerMemory;
import gov.nist.isg.mist.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.timing.TimeUtil;
import jcuda.driver.CUcontext;
import jcuda.driver.CUstream;
import jcuda.driver.CUstream_flags;
import jcuda.driver.JCudaDriver;
import jcuda.jcufft.JCufft;

/**
 * Test case for computing the phase correlation between two images using FFTW.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TestJCUDAPhaseCorrelationImageAlignment {

  static {
    LibraryUtils.initalize();
  }

  /**
   * Computes the phase correlation between two tiles using FFTW
   */
  public static void runTestPhaseCorrelationImageAlignment() throws FileNotFoundException {
    JCudaDriver.setExceptionsEnabled(true);
    JCufft.setExceptionsEnabled(true);
    Log.setLogLevel(LogType.HELPFUL);
    // Debug.setDebugLevel(LogType.VERBOSE);
    Log.msg(LogType.MANDATORY, "Running Test Phase Correlation Image Alignment JCUDA");

    // Read two images.
    // Read two images.
    File file1 = new File("C:\\majurski\\image-data\\1h_Wet_10Perc\\KB_2012_04_13_1hWet_10Perc_IR_00002.tif");
    File file2 = new File("C:\\majurski\\image-data\\1h_Wet_10Perc\\KB_2012_04_13_1hWet_10Perc_IR_00003.tif");

    CudaImageTile neighbor = new CudaImageTile(file1, 0, 0, 2, 2, 0, 0);
    CudaImageTile origin = new CudaImageTile(file2, 1, 0, 2, 2, 0, 0);

    Log.msg(LogType.INFO, neighbor.toString());
    Log.msg(LogType.INFO, origin.toString());

    Log.msg(LogType.INFO, "Initializing JCUDA");
    boolean enableCudaExceptions = true;
    CUcontext[] contexts = CudaUtils.initJCUDA(1, new int[]{0}, neighbor, enableCudaExceptions);
    CudaImageTile.initFunc(1);
    try {
      CudaImageTile.initPlans(neighbor.getWidth(), neighbor.getHeight(), contexts[0], 0, enableCudaExceptions);
    } catch (IOException e) {
      e.printStackTrace();
    }
    TimeUtil.tick();
    Log.msg(LogType.INFO, "Initialized JCUDA in " + TimeUtil.tock() + " ms");

    TimeUtil.tick();

    JCudaDriver.cuCtxSetCurrent(contexts[0]);
    CUstream stream = new CUstream();
    JCudaDriver.cuStreamCreate(stream, CUstream_flags.CU_STREAM_DEFAULT);
    CudaTileWorkerMemory memory = new CudaTileWorkerMemory(neighbor);
    neighbor.setDev(0);
    origin.setDev(0);
    neighbor.computeFft();
    origin.computeFft();
    CorrelationTriple result =
        Stitching.phaseCorrelationImageAlignmentCuda(neighbor,
            origin, memory, stream);

    Log.msg(LogType.MANDATORY, "Completed image alignment between " + neighbor.getFileName()
        + " and " + origin.getFileName() + " with " + result.toString() + " in " + TimeUtil.tock()
        + "ms");

    Log.msg(LogType.MANDATORY, "Test Completed.");

  }

  /**
   * Executes the test case
   *
   * @param args not used
   */
  public static void main(String[] args) {
    try {
      TestJCUDAPhaseCorrelationImageAlignment.runTestPhaseCorrelationImageAlignment();
    } catch (FileNotFoundException e) {
      Log.msg(LogType.MANDATORY, "Unable to find file: " + e.getMessage());
    }
  }

}
