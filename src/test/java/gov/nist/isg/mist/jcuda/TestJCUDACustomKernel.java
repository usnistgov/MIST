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
// Date: Jul 2, 2014 11:54:17 AM EST
//
// Time-stamp: <Jul 2, 2014 11:54:17 AM tjb3>
//
//
// ================================================================
package gov.nist.isg.mist.jcuda;

import jcuda.LogLevel;
import jcuda.Pointer;
import jcuda.driver.*;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;

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
      checkError(JCudaDriver.cuModuleLoad(module, "util-cuda-bin.cubin"));

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
              Pointer.to(new int[] {64}));

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
