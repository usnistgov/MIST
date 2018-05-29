// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.




// ================================================================
//
// Author: tjb3
// Date: Jul 2, 2014 11:54:17 AM EST
//
// Time-stamp: <Jul 2, 2014 11:54:17 AM tjb3>
//
//
// ================================================================
package gov.nist.isg.mist.jcuda;

import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import jcuda.LogLevel;
import jcuda.Pointer;
import jcuda.driver.CUcontext;
import jcuda.driver.CUdevice;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.CUfunction;
import jcuda.driver.CUmodule;
import jcuda.driver.JCudaDriver;

/**
 * Test case for computing the custom CUDA kernels
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TestJCUDACustomKernel {

  private static void runTestJCUDACUFFT() {
    JCudaDriver.setExceptionsEnabled(true);
    JCudaDriver.setLogLevel(LogLevel.LOG_DEBUG);
    Log.setLogLevel(LogType.VERBOSE);
    Log.msg(LogType.MANDATORY, "Running JCUDA Custom Kernel test");
    try {
      checkError(JCudaDriver.cuInit(0));
      CUdevice device = new CUdevice();
      checkError(JCudaDriver.cuDeviceGet(device, 0));
      CUcontext context = new CUcontext();
      checkError(JCudaDriver.cuCtxCreate(context, 0, device));

      CUmodule module = new CUmodule();
      checkError(JCudaDriver.cuModuleLoad(module, "/home/tjb3/work/MIST/lib/jcuda-6.5/stitching-util-cuda-bin.ptx"));

      CUfunction function = new CUfunction();
      checkError(JCudaDriver.cuModuleGetFunction(function, module, "elt_prod_conj"));

      CUdeviceptr deviceData1 = new CUdeviceptr();
      checkError(JCudaDriver.cuMemAlloc(deviceData1, 64));
      CUdeviceptr deviceData2 = new CUdeviceptr();
      checkError(JCudaDriver.cuMemAlloc(deviceData2, 64));
      CUdeviceptr deviceData3 = new CUdeviceptr();
      checkError(JCudaDriver.cuMemAlloc(deviceData3, 64));

      int numBlocks = 2;
      int numThreads = 32;

      Pointer kernelParams =
          Pointer.to(Pointer.to(deviceData1), Pointer.to(deviceData2), Pointer.to(deviceData3),
              Pointer.to(new int[]{64}));

      checkError(JCudaDriver.cuLaunchKernel(function, numBlocks, 1, 1, numThreads, 1, 1, 32, null,
          kernelParams, null));

      checkError(JCudaDriver.cuCtxSynchronize());
    } catch (Exception e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }

    Log.msg(LogType.MANDATORY, "Test completed.");
  }

  private static void checkError(int val) {
    if (val != 0) {
      Log.msg(LogType.MANDATORY, "Error: " + val);
    }
  }

  /**
   * Executes the test case
   *
   * @param args not used
   */
  public static void main(String[] args) {
    TestJCUDACustomKernel.runTestJCUDACUFFT();
  }
}
