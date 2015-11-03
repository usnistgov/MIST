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
// characteristic. We would appreciate acknowledgment if the software
// is used. This software can be redistributed and/or modified freely
// provided that any derivative works bear some notice that they are
// derived from it, and any modified versions bear some notice that
// they have been modified.
//
// ================================================================

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

import gov.nist.isg.mist.stitching.lib.common.CorrelationTriple;
import gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaUtils;
import gov.nist.isg.mist.stitching.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib32.imagetile.Stitching32;
import gov.nist.isg.mist.stitching.lib32.imagetile.jcuda.CudaImageTile32;
import gov.nist.isg.mist.stitching.lib32.imagetile.memory.CudaTileWorkerMemory32;
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
public class TestJCUDAPhaseCorrelationImageAlignment32 {

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

    CudaImageTile32 neighbor = new CudaImageTile32(file1, 0, 0, 2, 2, 0, 0);
    CudaImageTile32 origin = new CudaImageTile32(file2, 1, 0, 2, 2, 0, 0);

    Log.msg(LogType.INFO, neighbor.toString());
    Log.msg(LogType.INFO, origin.toString());

    Log.msg(LogType.INFO, "Initializing JCUDA");
    boolean enableCudaExceptions = true;
    CUcontext[] contexts = CudaUtils.initJCUDA(1, new int[]{0}, neighbor, enableCudaExceptions);
    CudaImageTile32.initFunc(1);
    try {
      CudaImageTile32.initPlans(neighbor.getWidth(), neighbor.getHeight(), contexts[0], 0, enableCudaExceptions);
    } catch (IOException e) {
      e.printStackTrace();
    }
    TimeUtil.tick();
    Log.msg(LogType.INFO, "Initialized JCUDA in " + TimeUtil.tock() + " ms");

    TimeUtil.tick();

    JCudaDriver.cuCtxSetCurrent(contexts[0]);
    CUstream stream = new CUstream();
    JCudaDriver.cuStreamCreate(stream, CUstream_flags.CU_STREAM_DEFAULT);
    CudaTileWorkerMemory32 memory = new CudaTileWorkerMemory32(neighbor);
    neighbor.setDev(0);
    origin.setDev(0);
    neighbor.computeFft();
    origin.computeFft();
    CorrelationTriple result =
        Stitching32.phaseCorrelationImageAlignmentCuda(neighbor,
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
      TestJCUDAPhaseCorrelationImageAlignment32.runTestPhaseCorrelationImageAlignment();
    } catch (FileNotFoundException e) {
      Log.msg(LogType.MANDATORY, "Unable to find file: " + e.getMessage());
    }
  }

}
