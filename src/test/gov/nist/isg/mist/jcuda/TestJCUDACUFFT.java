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
// Date: Jul 2, 2014 11:54:02 AM EST
//
// Time-stamp: <Jul 2, 2014 11:54:02 AM tjb3>
//
//
// ================================================================

package test.gov.nist.isg.mist.jcuda;

import java.io.File;

import test.timing.TimeUtil;
import main.gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaImageTile;
import main.gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaUtils;
import main.gov.nist.isg.mist.stitching.lib.libraryloader.LibraryUtils;
import main.gov.nist.isg.mist.stitching.lib.log.Log;
import main.gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import jcuda.LogLevel;
import jcuda.driver.JCudaDriver;
import jcuda.runtime.JCuda;

/**
 * Test case for computing the FFT using CUDA cuFFT
 * 
 * @author Tim Blattner
 * @version 1.0
 */
public class TestJCUDACUFFT {

  private static void runTestJCUDACUFFT() {
    LibraryUtils.initalize();
    JCudaDriver.setExceptionsEnabled(true);
    JCudaDriver.setLogLevel(LogLevel.LOG_DEBUG);
    Log.setLogLevel(LogType.VERBOSE);
    Log.msg(LogType.MANDATORY, "Running JCUDA CUFFT test");

    File file = new File("F:\\StitchingData\\70perc_input_images\\F_0001.tif");
    // File file = new File("/home/img-stitching/input_images/F_0001.tif");

    // BridJ.addLibraryPath(System.getProperty("user.dir") + File.separator
    // + "libs" + File.separator + "jcuda");

    CudaImageTile tile = new CudaImageTile(file);

    CudaUtils.initJCUDA(1, new int[] {0}, tile);

    TimeUtil.tick();
    tile.setDev(0);
    tile.computeFft();

    JCuda.cudaDeviceSynchronize();

    Log.msg(LogType.MANDATORY, "Completed FFT in : " + TimeUtil.tock() + "ms");

    Log.msg(LogType.MANDATORY, "Test Completed.");
  }

  /**
   * Executes the test case
   * 
   * @param args not used
   */
  public static void main(String[] args) {
    TestJCUDACUFFT.runTestJCUDACUFFT();
  }
}
