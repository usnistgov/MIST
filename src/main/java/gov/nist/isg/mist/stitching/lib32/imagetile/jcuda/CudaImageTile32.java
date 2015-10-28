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
// Date: May 10, 2013 2:58:58 PM EST
//
// Time-stamp: <May 10, 2013 2:58:58 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib32.imagetile.jcuda;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.memorypool.DynamicMemoryPool;
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

  private static final String CUDA_MODULE_NAME = "lib/jcuda-" + LibraryUtils.JCUDA_VERSION + "/stitching-util-cuda-bin.ptx";
  private static final String FUNC_ELT_PROD = "elt_prod_conj_v2";
  private static final String FUNC_MAX = "reduce_max_main";
  private static final String FUNC_MAX_FIN = "reduce_max_final";
  private static final String FUNC_MAX_FILTER = "reduce_max_filter_main";
  private static final String FUNC_MAX_FILTER_FIN = "reduce_max_filter_final";

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
    this(file, row, col, gridWidth, gridHeight, startRow, startCol, true);
  }

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
   * @param read       whether or not to read the tile here
   */
  public CudaImageTile32(File file, int row, int col, int gridWidth, int gridHeight, int startRow,
                         int startCol, boolean read) {
    super(file, row, col, gridWidth, gridHeight, startRow, startCol, read);
  }

  /**
   * Creates an image tile from a file
   *
   * @param file the image tile file
   */
  public CudaImageTile32(File file) {
    this(file, 0, 0, 1, 1, 0, 0, true);
  }

  /**
   * Initializes image tile and optionally does not read
   *
   * @param file the file assosiated with this tile
   * @param read whether or not to read the tile here
   */
  public CudaImageTile32(File file, boolean read) {
    this(file, 0, 0, 1, 1, 0, 0, read);
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
  public void computeFft() throws FileNotFoundException {

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
                         CUstream stream) throws FileNotFoundException {

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
  public void computeFft(DynamicMemoryPool<CUdeviceptr> pool, TileWorkerMemory memory) throws FileNotFoundException {
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
  public static boolean initPlans(int width, int height, CUcontext context, int id)
      throws IOException {

    File testFile = new File(CUDA_MODULE_NAME);

    if (!testFile.canRead()) {
      Log.msg(LogType.MANDATORY,
          "Error: unable to read CUDA library file: " + testFile.getAbsolutePath());
      throw new IOException("Unable to read CUDA library file: " + testFile.getAbsolutePath());
    }

    Log.msg(LogType.INFO, "Loading CUDA library: " + testFile.getAbsolutePath());

    JCudaDriver.cuCtxSetCurrent(context);

    CUmodule module = new CUmodule();
    JCudaDriver.cuModuleLoad(module, CUDA_MODULE_NAME);

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

      JCufft.setExceptionsEnabled(false);

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
    JCufft.cufftDestroy(plan_fwd[dev]);
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
