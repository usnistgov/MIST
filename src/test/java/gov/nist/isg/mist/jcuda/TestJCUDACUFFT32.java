// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.




// ================================================================
//
// Author: tjb3
// Date: Jul 2, 2014 11:54:02 AM EST
//
// Time-stamp: <Jul 2, 2014 11:54:02 AM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.jcuda;

import java.io.File;
import java.io.FileNotFoundException;

import gov.nist.isg.mist.lib.imagetile.jcuda.CudaUtils;
import gov.nist.isg.mist.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib32.imagetile.jcuda.CudaImageTile32;
import gov.nist.isg.mist.timing.TimeUtil;
import jcuda.LogLevel;
import jcuda.driver.JCudaDriver;
import jcuda.runtime.JCuda;

/**
 * Test case for computing the FFT using CUDA cuFFT
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TestJCUDACUFFT32 {

  private static void runTestJCUDACUFFT() throws FileNotFoundException {
    LibraryUtils.initalize();
    JCudaDriver.setExceptionsEnabled(true);
    JCudaDriver.setLogLevel(LogLevel.LOG_DEBUG);
    Log.setLogLevel(LogType.VERBOSE);
    Log.msg(LogType.MANDATORY, "Running JCUDA CUFFT test");

    File file = new File("C:\\majurski\\image-data\\1h_Wet_10Perc\\KB_2012_04_13_1hWet_10Perc_IR_00001.tif");

    // BridJ.addLibraryPath(System.getProperty("user.dir") + File.separator
    // + "libs" + File.separator + "jcuda");

    CudaImageTile32 tile = new CudaImageTile32(file);

    boolean enableCudaExceptions = true;
    CudaUtils.initJCUDA(1, new int[]{0}, tile, enableCudaExceptions);

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
    try {
      TestJCUDACUFFT32.runTestJCUDACUFFT();
    } catch (FileNotFoundException e) {
      Log.msg(LogType.MANDATORY, "File not found: " + e.getMessage());
    }
  }
}
