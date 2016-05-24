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
// Date: Apr 11, 2014 11:37:36 AM EST
//
// Time-stamp: <Apr 11, 2014 11:37:36 AM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib32.imagetile.memory;

import org.bridj.Pointer;

import java.nio.ByteBuffer;

import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.stitching.lib32.imagetile.fftw.FFTW3Library32;
import gov.nist.isg.mist.stitching.lib32.imagetile.fftw.FftwImageTile32;
import jcuda.driver.CUdeviceptr;

/**
 * Class that represents all the memory required for image stitching using FFTW. Memory is reused
 * for independent translation computation for image tiles.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class FftwTileWorkerMemory32 extends TileWorkerMemory<Float> {

  private Pointer<Float> pcmP;
  private Pointer<Float> pcmInP;
  private Pointer<Float> fftInP;
  private Pointer<Integer> peaks;

  /**
   * Initializes the FFTW tile worker memory
   *
   * @param initTile the initial tile
   */
  public FftwTileWorkerMemory32(ImageTile<?> initTile) {
    super(initTile.getWidth(), initTile.getHeight());

    this.pcmP = FFTW3Library32.fftwf_alloc_real(super.getWidth() * super.getHeight());
    this.fftInP = FFTW3Library32.fftwf_alloc_real(super.getWidth() * super.getHeight());
    this.pcmInP = FFTW3Library32.fftwf_alloc_complex(FftwImageTile32.fftSize);
    this.peaks = Pointer.allocateInts(Stitching.NUM_PEAKS);
  }

  @Override
  public void releaseMemory() {

    if (this.peaks != null) {
      Pointer.release(this.peaks);
      this.peaks = null;
    }

    if (this.pcmP != null) {
      FFTW3Library32.fftwf_free(this.pcmP);
      this.pcmP = null;
    }

    if (this.pcmInP != null) {
      FFTW3Library32.fftwf_free(this.pcmInP);
      this.pcmInP = null;
    }

    if (this.fftInP != null) {
      FFTW3Library32.fftwf_free(this.fftInP);
      this.fftInP = null;
    }

    System.gc();

  }

  @Override
  public Pointer<Float> getPCMPMemory() {
    return this.pcmP;
  }

  @Override
  public Pointer<Float> getPCMInMemory() {
    return this.pcmInP;
  }

  @Override
  public Pointer<Float> getFFTInP() {
    return this.fftInP;
  }

  @Override
  public Pointer<Integer> getPeaks() {
    return this.peaks;
  }

  @Override
  public ByteBuffer getImageBuffer() {
    throw new IllegalStateException("getImageBuffer is only used for JCUDA Tile workers");
  }

  @Override
  public ByteBuffer getIndexBuffer() {
    throw new IllegalStateException("getIndexBuffer is only used for JCUDA Tile workers");
  }

  @Override
  public ByteBuffer getFilterBuffer() {
    throw new IllegalStateException("getFilterBuffer is only used for JCUDA Tile workers");
  }

  @Override
  public CUdeviceptr getFftIn() {
    throw new IllegalStateException("getFftIn is only used for JCUDA Tile workers");
  }

  @Override
  public CUdeviceptr getPcmIn() {
    throw new IllegalStateException("getPcmIn is only used for JCUDA Tile workers");
  }

  @Override
  public CUdeviceptr getPcm() {
    throw new IllegalStateException("getPcm is only used for JCUDA Tile workers");
  }

  @Override
  public CUdeviceptr getMaxOut() {
    throw new IllegalStateException("getMaxOut is only used for JCUDA Tile workers");
  }

  @Override
  public CUdeviceptr getMultiMaxOut() {
    throw new IllegalStateException("getMultiMaxOut is only used for JCUDA Tile workers");
  }

  @Override
  public CUdeviceptr getMultiIdxOut() {
    throw new IllegalStateException("getMultiIdxOut is only used for JCUDA Tile workers");
  }

  @Override
  public CUdeviceptr getIdxOut() {
    throw new IllegalStateException("getIdxOut is only used for JCUDA Tile workers");
  }

  @Override
  public CUdeviceptr getIdxFilter() {
    throw new IllegalStateException("getIdxFilter is only used for JCUDA Tile workers");
  }

  @Override
  public Integer[] getIndices() {
    throw new IllegalStateException("getIndices is only used for JCUDA Tile workers");
  }

  @Override
  public float[][] getArrayMemory() {
    throw new IllegalStateException("getArrayMemory is only used for Java Tile workers");
  }

}
