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
// Date: Jul 2, 2014 11:55:27 AM EST
//
// Time-stamp: <Jul 2, 2014 11:55:27 AM tjb3>
//
//
// ================================================================
package gov.nist.isg.mist.jcuda;

import gov.nist.isg.mist.timing.TimeUtil;
import jcuda.LogLevel;
import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.CUcontext;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.JCudaDriver;
import gov.nist.isg.mist.stitching.lib.common.CorrelationTriple;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwStitching;
import gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaStitching;
import gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaUtils;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.CudaTileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.FftwTileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.imagetile.utilfns.UtilFnsStitching;
import gov.nist.isg.mist.stitching.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.stitching.lib.log.Debug;
import gov.nist.isg.mist.stitching.lib.log.Debug.DebugType;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Test case for computing the custom CUDA multimax kernels
 * 
 * @author Tim Blattner
 * @version 1.0
 */
public class TestJCUDAMultiMax {

  static {

    // Initialize libraries
    LibraryUtils.initalize();
  }

  private static void runTestJCUDACUFFT() throws FileNotFoundException {
    JCudaDriver.setExceptionsEnabled(true);
    JCudaDriver.setLogLevel(LogLevel.LOG_DEBUG);
    Log.setLogLevel(LogType.HELPFUL);
    Debug.setDebugLevel(DebugType.VERBOSE);
    Log.msg(LogType.MANDATORY, "Running JCUDA CUFFT test");

    File file1 =
        new File(
            "F:\\StitchingData\\Stitching_test_5Percent\\KB_2010_04_20_24hDry_10Perc_IR_00292.tif");
    File file2 =
        new File(
            "F:\\StitchingData\\Stitching_test_5Percent\\KB_2010_04_20_24hDry_10Perc_IR_00312.tif");

    FftwImageTile tile1 = new FftwImageTile(file1);
    FftwImageTile tile2 = new FftwImageTile(file2);
    CudaImageTile cudaTile1 = new CudaImageTile(file1);

    FftwImageTile.initLibrary(System.getProperty("user.dir") + File.separator + "libs"
        + File.separator + "fftw", System.getProperty("user.dir") + File.separator + "util-fns",
        "libfftw3");
    FftwImageTile.initPlans(tile1.getWidth(), tile1.getHeight(), 0x21, true, "test.dat");

    CUcontext[] contexts = CudaUtils.initJCUDA(1, new int[] {0}, cudaTile1);

    CudaTileWorkerMemory cudaMem = new CudaTileWorkerMemory(cudaTile1);
    FftwTileWorkerMemory mem = new FftwTileWorkerMemory(tile1);

    tile1.computeFft();
    tile2.computeFft();

    org.bridj.Pointer<Double> pcm = FftwStitching.peakCorrelationMatrix(tile2, tile1, mem);

    int width = 1392;
    int height = 1040;

    double[] vals = new double[width * height];
    double[][] javaVals = new double[height][width];
    // org.bridj.Pointer<Double>fftwVals =
    // org.bridj.Pointer.allocateDoubles(width*height);

    for (int r = 0; r < height; r++) {
      for (int c = 0; c < width; c++) {

        // double val = Math.random();
        // vals[r*width+c] = val;
        vals[r * width + c] = pcm.getDoubleAtIndex(r * width + c);
        javaVals[r][c] = pcm.getDoubleAtIndex(r * width + c);
        // fftwVals.set(r*width+c, val);
        // vals[r*width+c] = (double)r*width+c;
        // javaVals[r][c] = (double)r*width+c;
        // fftwVals.set(r*width+c, (double)r*width+c);
      }
    }

    int size = width * height;
    CUdeviceptr in = new CUdeviceptr();

    JCudaDriver.cuCtxSetCurrent(contexts[0]);
    JCudaDriver.cuMemAlloc(in, size * Sizeof.DOUBLE);
    JCudaDriver.cuMemcpyHtoD(in, Pointer.to(vals), size * Sizeof.DOUBLE);

    TimeUtil.tick();

    // Now we have the pcm...
    List<CorrelationTriple> peaks =
        UtilFnsStitching.multiPeakCorrelationMatrix(pcm, Stitching.NUM_PEAKS, tile1.getWidth(),
            tile1.getHeight(), mem.getPeaks());

    Log.msg(LogType.HELPFUL, "Time to getmultimax FFTW with sort: " + TimeUtil.tock());

    for (CorrelationTriple t : peaks)
      System.out.println(t);

    TimeUtil.tick();

    peaks =
        UtilFnsStitching.multiPeakCorrelationMatrixNoSort(pcm, Stitching.NUM_PEAKS,
            tile1.getWidth(), tile1.getHeight());

    Log.msg(LogType.HELPFUL, "Time to getmultimax FFTW without sort: " + TimeUtil.tock());

    for (CorrelationTriple t : peaks)
      System.out.println(t);

    TimeUtil.tick();

    CudaStitching.multiPeakCorrelationMatrix(in, Stitching.NUM_PEAKS, tile1.getWidth(),
        tile1.getHeight(), cudaMem, null, 0);

    ByteBuffer values =
        CudaStitching.getMultiMaxIdx(in, Stitching.NUM_PEAKS, width, height, cudaMem, null, 0);

    Log.msg(LogType.HELPFUL, "Time to getmultimax CUDA: " + TimeUtil.tock());

    TimeUtil.tick();

    // List<CorrelationTriple> javaAnswer =
    // JavaStitching.multiPeakCorrelationMatrix(javaVals,
    // Stitching.NUM_PEAKS, width, height, null);
    // Log.msg(LogType.HELPFUL, "Time to getmultimax Java: " +
    // TimeUtil.tock());
    //
    // TimeUtil.tick();
    // List<CorrelationTriple> fftwAnswer =
    // UtilFnsStitching.multiPeakCorrelationMatrix(fftwVals, nPeaks, width,
    // height);
    // Log.msg(LogType.HELPFUL, "Time to getmultimax FFTW: " +
    // TimeUtil.tock());

    // int count = 0;
    // boolean testPassed = true;
    for (int i = 0; i < Stitching.NUM_PEAKS; i++) {
      int val = values.getInt();
      System.out.println(val);
      // CorrelationTriple javaValue = javaAnswer.get(count);
      //
      // int idx = javaValue.getY()*width+javaValue.getX();
      // int idx2 =
      // fftwAnswer.get(count).getY()*width+fftwAnswer.get(count).getX();

      // if (val != idx || val != idx2)
      // {
      // testPassed = false;
      // }
      //
      //
      // Log.msg(LogType.HELPFUL, "CUDA Value " + count + ": " + val +
      // " Java Value " + count + ": " + idx + " FFTW Value " + count +
      // ": " + idx2);
      // count++;
    }

    // Log.msg(LogType.MANDATORY, "Result of test: " + (testPassed ?
    // "PASSED" : "FAILED"));

    Log.msg(LogType.MANDATORY, "Test Completed.");
  }

  /**
   * Executes the test case
   * 
   * @param args not used
   */
  public static void main(String[] args) {
    try {
        TestJCUDAMultiMax.runTestJCUDACUFFT();
    } catch (FileNotFoundException e)
    {
        Log.msg(LogType.MANDATORY, "Unable to find file: " + e.getMessage());
    }
  }
}
