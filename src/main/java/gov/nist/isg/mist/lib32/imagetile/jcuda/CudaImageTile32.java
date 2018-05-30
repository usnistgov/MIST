// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: May 10, 2013 2:58:58 PM EST
//
// Time-stamp: <May 10, 2013 2:58:58 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib32.imagetile.jcuda;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.memorypool.DynamicMemoryPool;
import ij.IJ;
import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.CUcontext;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.CUfunction;
import jcuda.driver.CUmodule;
import jcuda.driver.CUstream;
import jcuda.driver.JCudaDriver;
import jcuda.jcufft.JCufft;
import jcuda.jcufft.cufftHandle;
import jcuda.jcufft.cufftType;
import jcuda.runtime.cudaStream_t;

/**
 * Represents an image tile that uses native library bindings with CUDA. Must initialize the
 * libraries before using native bindings and initialize CUDA library.
 *
 * <pre>
 * <code>
 * JCUDAImageTile.initLibrary(fftwPath, "");
 * </pre>
 *
 * </code>
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class CudaImageTile32 extends ImageTile<CUdeviceptr> {

  private static final String CUDA_MODULE_NAME = "lib/jcuda/stitching-util-cuda-bin.ptx";
  private static final String FUNC_ELT_PROD = "elt_prod_conj_v2f";
  private static final String FUNC_MAX = "reduce_max_mainf";
  private static final String FUNC_MAX_FIN = "reduce_max_finalf";
  private static final String FUNC_MAX_FILTER = "reduce_max_filter_mainf";
  private static final String FUNC_MAX_FILTER_FIN = "reduce_max_filter_finalf";

  /**
   * The reference to the element-wise product CUDA function for each GPU
   */
  public static CUfunction[] elt_prod_function;

  /**
   * The reference to the max main CUDA function for each GPU
   */
  public static CUfunction[] reduce_max_main;

  /**
   * The reference to the max final CUDA function for each GPU
   */
  public static CUfunction[] reduce_max_final;

  /**
   * The reference to the max filter main CUDA function for each GPU
   */
  public static CUfunction[] reduce_max_filter_main;

  /**
   * The reference to the max filter final CUDA function for each GPU
   */
  public static CUfunction[] reduce_max_filter_final;

  /**
   * The size of the FFT
   */
  public static int fftSize;

  /**
   * Reference to the forward FFT plan for each GPU
   */
  public static cufftHandle[] plan_fwd;

  /**
   * Reference to the backward (inverse) FFT plan for each GPU
   */
  public static cufftHandle[] plan_bwd;


  /**
   * Creates an image tile in a grid
   *
   * @param file       the image tile file
   * @param row        the row location in the grid
   * @param col        the column location in the grid
   * @param gridWidth  the width of the tile grid (subgrid)
   * @param gridHeight the height of the tile grid (subgrid)
   * @param startRow   the start row of the tile grid (subgrid)
   * @param startCol   the start column of the tile grid (subgrid)
   */
  public CudaImageTile32(File file, int row, int col, int gridWidth, int gridHeight, int startRow,
                         int startCol) {
    super(file, row, col, gridWidth, gridHeight, startRow, startCol);
  }

  /**
   * Creates an image tile from a file
   *
   * @param file the image tile file
   */
  public CudaImageTile32(File file) {
    this(file, 0, 0, 1, 1, 0, 0);
  }


  @Override
  public void releaseFftMemory() {
    if (super.isMemoryLoaded()) {
      JCudaDriver.cuMemFree(this.fft);
      this.fft = null;
    }
  }

  /**
   * Computes this image's FFT
   */
  @Override
  public void computeFft() {

    // if the file does not exists on disk, skip computing the fft
    if (!this.fileExists())
      return;

    if (hasFft())
      return;

    readTile();

    float tempJ[] = new float[super.getWidth() * super.getHeight()];


    for (int r = 0; r < super.getHeight(); r++)
      for (int c = 0; c < super.getWidth(); c++) {
        tempJ[r * super.getWidth() + c] = super.getPixels().getPixelValue(c, r);
      }

    this.fft = new CUdeviceptr();
    CUdeviceptr ptr = new CUdeviceptr();

    JCudaDriver.cuMemAlloc(ptr, super.getWidth() * super.getHeight() * Sizeof.FLOAT);

    JCudaDriver.cuMemAlloc(this.fft, fftSize * Sizeof.FLOAT * 2);

    // Copy data from the CPU to the GPU
    JCudaDriver.cuMemcpyHtoD(ptr, Pointer.to(tempJ), super.getWidth() * super.getHeight()
        * Sizeof.FLOAT);

    // Apply real to complex transform
    JCufft.cufftExecR2C(plan_fwd[super.getDev()], ptr, this.fft);

    JCudaDriver.cuMemFree(ptr);

    // Synchronize to ensure fft computation is complete
    JCudaDriver.cuCtxSynchronize();
    super.setMemoryLoaded(true);
  }

  /**
   * Computes this image's FFT
   */
  @Override
  public void computeFft(DynamicMemoryPool<CUdeviceptr> pool, TileWorkerMemory memory,
                         CUstream stream) {

    // if the file does not exists on disk, skip computing the fft
    if (!this.fileExists())
      return;

    readTile();

    ByteBuffer dBuffer = memory.getImageBuffer();

    for (int r = 0; r < super.getHeight(); r++)
      for (int c = 0; c < super.getWidth(); c++) {
        dBuffer.putFloat((r * super.getWidth() + c) * Sizeof.FLOAT, super.getPixels().getPixelValue(c, r));
      }

    CUdeviceptr fftIn = memory.getFftIn();

    JCudaDriver.cuMemcpyHtoDAsync(fftIn, Pointer.to(dBuffer), super.getWidth() * super.getHeight()
        * Sizeof.FLOAT, stream);

    JCufft.cufftExecR2C(plan_fwd[this.getThreadID()], fftIn, this.fft);
    JCudaDriver.cuStreamSynchronize(stream);

    dBuffer.rewind();
  }

  @Override
  public void computeFft(DynamicMemoryPool<CUdeviceptr> pool, TileWorkerMemory memory) {
    this.computeFft(pool, memory, null);
  }

  /**
   * Initializes GPU functions
   *
   * @param nGPUs the number of GPUs being used
   */
  public static void initFunc(int nGPUs) {
    elt_prod_function = new CUfunction[nGPUs];
    reduce_max_main = new CUfunction[nGPUs];
    reduce_max_final = new CUfunction[nGPUs];
    reduce_max_filter_main = new CUfunction[nGPUs];
    reduce_max_filter_final = new CUfunction[nGPUs];

    plan_fwd = new cufftHandle[nGPUs];
    plan_bwd = new cufftHandle[nGPUs];

  }

  /**
   * Initializes JCUDA plans and functions
   *
   * @param width   the width for the tile
   * @param height  the height for the tile
   * @param context the GPU context that is to be initialized
   * @param id      the thread ID associated with the GPU context
   * @return true if the plan was initialized successfully, otherwise false
   * @throws IOException fails to read PTX CUDA file
   */
  public static boolean initPlans(int width, int height, CUcontext context, int id, boolean enableCudaExceptions)
      throws IOException {

    String fijiDir = IJ.getDirectory("imagej");
    if(fijiDir == null)
      fijiDir = "";

    File testFile = new File(fijiDir + CUDA_MODULE_NAME);

    if (!testFile.canRead()) {
      Log.msg(LogType.MANDATORY,
          "Error: unable to read CUDA library file: " + testFile.getAbsolutePath());
      throw new IOException("Unable to read CUDA library file: " + testFile.getAbsolutePath());
    }

    Log.msg(LogType.INFO, "Loading CUDA library: " + testFile.getAbsolutePath());

    JCudaDriver.cuCtxSetCurrent(context);

    CUmodule module = new CUmodule();
    JCudaDriver.cuModuleLoad(module, fijiDir + CUDA_MODULE_NAME);

    Log.msg(LogType.INFO, "Successfully loaded CUDA library... Obtaining functions");


    elt_prod_function[id] = new CUfunction();
    JCudaDriver.cuModuleGetFunction(elt_prod_function[id], module, FUNC_ELT_PROD);

    reduce_max_main[id] = new CUfunction();
    JCudaDriver.cuModuleGetFunction(reduce_max_main[id], module, FUNC_MAX);

    reduce_max_final[id] = new CUfunction();
    JCudaDriver.cuModuleGetFunction(reduce_max_final[id], module, FUNC_MAX_FIN);

    reduce_max_filter_main[id] = new CUfunction();
    JCudaDriver.cuModuleGetFunction(reduce_max_filter_main[id], module, FUNC_MAX_FILTER);

    reduce_max_filter_final[id] = new CUfunction();
    JCudaDriver.cuModuleGetFunction(reduce_max_filter_final[id], module, FUNC_MAX_FILTER_FIN);

    Log.msg(LogType.INFO, "Initializing CUFFT");


    try {
      JCufft.setExceptionsEnabled(true);

      fftSize = (width / 2 + 1) * height;
      plan_fwd[id] = new cufftHandle();
      plan_bwd[id] = new cufftHandle();
      JCufft.cufftPlan2d(plan_fwd[id], height, width, cufftType.CUFFT_R2C);
      JCufft.cufftPlan2d(plan_bwd[id], height, width, cufftType.CUFFT_C2R);

      JCufft.setExceptionsEnabled(enableCudaExceptions);

    } catch (UnsatisfiedLinkError ex) {
      Log.msg(LogType.MANDATORY, "Unable to load CUFFT library. Currently it is "
          + "mandatory to install the CUDA toolkit (v" + LibraryUtils.JCUDA_VERSION + "). " +
          "If you recently installed the toolkit, please restart your computer.");
      Log.msg(LogType.MANDATORY, "http://nvidia.com/getcuda");
      return false;
    }

    return true;
  }

  /**
   * Destroys forward and backward plan's memory
   *
   * @param dev the GPU device
   */
  public static void destroyPlans(int dev) {
    if (plan_fwd != null)
      JCufft.cufftDestroy(plan_fwd[dev]);
    if (plan_bwd != null)
      JCufft.cufftDestroy(plan_bwd[dev]);
  }

  /**
   * Binds the forward plan to a particular CUDA stream
   *
   * @param stream the CUDA stream
   * @param dev    the GPU device
   */
  public static void bindFwdPlanToStream(CUstream stream, int dev) {
    cudaStream_t stream_t = new cudaStream_t(stream);
    JCufft.cufftSetStream(plan_fwd[dev], stream_t);
  }

  /**
   * Binds the backward (inverse) plan to a particular CUDA stream
   *
   * @param stream the CUDA stream
   * @param dev    the GPU device
   */
  public static void bindBwdPlanToStream(CUstream stream, int dev) {
    cudaStream_t stream_t = new cudaStream_t(stream);
    JCufft.cufftSetStream(plan_bwd[dev], stream_t);
  }

}
