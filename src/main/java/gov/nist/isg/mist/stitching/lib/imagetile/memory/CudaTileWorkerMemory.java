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


// ================================================================
//
// Author: tjb3
// Date: Apr 11, 2014 11:37:47 AM EST
//
// Time-stamp: <Apr 11, 2014 11:37:47 AM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.imagetile.memory;

import org.bridj.Pointer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaImageTile;
import jcuda.Sizeof;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.JCudaDriver;

/**
 * Class that represents all the memory required for image stitching using CUDA. Memory is reused
 * for independent translation computation for image tiles.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class CudaTileWorkerMemory extends TileWorkerMemory {

  private CUdeviceptr fftIn;
  private CUdeviceptr pcmIn;
  private CUdeviceptr pcm;

  private CUdeviceptr maxOut;
  private CUdeviceptr idxOut;
  private CUdeviceptr idxFilter;
  private ByteBuffer imageBuffer;
  private ByteBuffer indexBuffer;
  private ByteBuffer filterBuffer;

  private CUdeviceptr multiMaxOut;
  private CUdeviceptr multiIdxOut;

  /**
   * Initializes the CUDA tile worker memory
   *
   * @param initTile the initial tile
   */
  public CudaTileWorkerMemory(ImageTile<?> initTile) {
    super(initTile.getWidth(), initTile.getHeight());

    this.fftIn = new CUdeviceptr();
    this.pcmIn = new CUdeviceptr();
    this.pcm = new CUdeviceptr();
    this.maxOut = new CUdeviceptr();
    this.idxOut = new CUdeviceptr();
    this.idxFilter = new CUdeviceptr();
    this.multiMaxOut = new CUdeviceptr();
    this.multiIdxOut = new CUdeviceptr();

    JCudaDriver.cuMemAllocHost(this.fftIn, super.getWidth() * super.getHeight() * Sizeof.DOUBLE);
    JCudaDriver.cuMemAlloc(this.pcmIn, CudaImageTile.fftSize * Sizeof.DOUBLE * 2);
    JCudaDriver.cuMemAlloc(this.pcm, super.getWidth() * super.getHeight() * Sizeof.DOUBLE);
    JCudaDriver.cuMemAlloc(this.maxOut, super.getWidth() * super.getHeight() * Sizeof.DOUBLE);
    JCudaDriver.cuMemAlloc(this.multiMaxOut, super.getWidth() * super.getHeight() * Sizeof.DOUBLE);
    JCudaDriver.cuMemAllocHost(this.idxOut, super.getWidth() * super.getHeight() * Sizeof.INT);
    JCudaDriver.cuMemAllocHost(this.multiIdxOut, super.getWidth() * super.getHeight() * Sizeof.INT);
    JCudaDriver.cuMemAllocHost(this.idxFilter, Stitching.NUM_PEAKS * Sizeof.INT);

    this.imageBuffer = ByteBuffer.allocateDirect(super.getWidth() * super.getHeight() * Sizeof.DOUBLE);
    this.indexBuffer = ByteBuffer.allocateDirect(super.getWidth() * super.getHeight() * Sizeof.INT);
    this.filterBuffer = ByteBuffer.allocateDirect(Stitching.NUM_PEAKS * Sizeof.INT);

    this.imageBuffer.order(ByteOrder.LITTLE_ENDIAN);
    this.indexBuffer.order(ByteOrder.LITTLE_ENDIAN);
    this.filterBuffer.order(ByteOrder.LITTLE_ENDIAN);

    this.imageBuffer.rewind();
    this.indexBuffer.rewind();
    this.filterBuffer.rewind();

  }


  @Override
  public void releaseMemory() {

    if (this.fftIn != null) {
      JCudaDriver.cuMemFreeHost(this.fftIn);
      this.fftIn = null;
    }

    if (this.pcmIn != null) {
      JCudaDriver.cuMemFree(this.pcmIn);
      this.pcmIn = null;
    }

    if (this.pcm != null) {
      JCudaDriver.cuMemFree(this.pcm);
      this.pcm = null;
    }

    if (this.maxOut != null) {
      JCudaDriver.cuMemFree(this.maxOut);
      this.maxOut = null;
    }

    if (this.idxOut != null) {
      JCudaDriver.cuMemFreeHost(this.idxOut);
      this.idxOut = null;
    }

    if (this.idxFilter != null) {
      JCudaDriver.cuMemFreeHost(this.idxFilter);
      this.idxFilter = null;
    }

    if (this.multiMaxOut != null) {
      JCudaDriver.cuMemFree(this.multiMaxOut);
      this.multiMaxOut = null;
    }

    if (this.multiIdxOut != null) {
      JCudaDriver.cuMemFreeHost(this.multiIdxOut);
      this.multiIdxOut = null;
    }

    if (this.imageBuffer != null) {
      this.imageBuffer = null;
    }

    if (this.indexBuffer != null) {
      this.indexBuffer = null;
    }

    if (this.filterBuffer != null)
      this.filterBuffer = null;

    System.gc();

  }

  /**
   * @return the imageBuffer
   */
  @Override
  public ByteBuffer getImageBuffer() {
    return this.imageBuffer;
  }

  /**
   * @return the indexBuffer
   */
  @Override
  public ByteBuffer getIndexBuffer() {
    return this.indexBuffer;
  }

  /**
   * @return the filterBuffer
   */
  @Override
  public ByteBuffer getFilterBuffer() {
    return this.filterBuffer;
  }

  /**
   * @return the fftIn
   */
  @Override
  public CUdeviceptr getFftIn() {
    return this.fftIn;
  }

  /**
   * @return the pcmIn
   */
  @Override
  public CUdeviceptr getPcmIn() {
    return this.pcmIn;
  }

  /**
   * @return the pcm
   */
  @Override
  public CUdeviceptr getPcm() {
    return this.pcm;
  }

  /**
   * @return the maxOut
   */
  @Override
  public CUdeviceptr getMaxOut() {
    return this.maxOut;
  }

  /**
   * @return the idxOut
   */
  @Override
  public CUdeviceptr getIdxOut() {
    return this.idxOut;
  }

  @Override
  public CUdeviceptr getMultiMaxOut() {
    return this.multiMaxOut;
  }

  @Override
  public CUdeviceptr getMultiIdxOut() {
    return this.multiIdxOut;
  }

  /**
   * @return the idxFilter
   */
  @Override
  public CUdeviceptr getIdxFilter() {
    return this.idxFilter;
  }

  @Override
  public Pointer<Double> getPCMPMemory() {
    throw new IllegalStateException("getPCMPMemory is only used for FFTW Tile workers");
  }

  @Override
  public Pointer<Double> getPCMInMemory() {
    throw new IllegalStateException("getPCMInMemory is only used for FFTW Tile workers");
  }

  @Override
  public Pointer<Double> getFFTInP() {
    throw new IllegalStateException("getFFTInP is only used for FFTW Tile workers");
  }

  @Override
  public Integer[] getIndices() {
    throw new IllegalStateException("getIndices is only used for FFTW Tile workers");

  }

  @Override
  public Pointer<Integer> getPeaks() {
    throw new IllegalStateException("getPeaks is only used for FFTW Tile workers");
  }

  @Override
  public float[][] getArrayMemory() {
    throw new IllegalStateException("getArrayMemory is only used for Java Tile workers");
  }
}
