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
// Date: Apr 11, 2014 11:40:10 AM EST
//
// Time-stamp: <Apr 11, 2014 11:40:10 AM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.imagetile.memory;

import jcuda.driver.CUdeviceptr;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import org.bridj.Pointer;

import java.nio.ByteBuffer;

/**
 * Class that represents the memory required for stitching a pair of tiles. This class is used as a
 * super class for other TileWorkerMemories.
 * 
 * @author Tim Blattner
 * @version 1.0
 */
public abstract class TileWorkerMemory {

  private int width;
  private int height;

  /**
   * Initializes tile worker memory
   * 
   * @param initTile a tile to initialize with
   */
  public TileWorkerMemory(ImageTile<?> initTile) {
    this.width = initTile.getWidth();
    this.height = initTile.getHeight();
  }

  /**
   * @return the width
   */
  public int getWidth() {
    return this.width;
  }

  /**
   * @return the height
   */
  public int getHeight() {
    return this.height;
  }


  /**
   * Releases tile memory
   */
  public abstract void releaseMemory();

  /**
   * @return the imageBuffer
   */
  public abstract ByteBuffer getImageBuffer();

  /**
   * @return the indexBuffer
   */
  public abstract ByteBuffer getIndexBuffer();

  /**
   * @return the filterBuffer
   */
  public abstract ByteBuffer getFilterBuffer();


  /**
   * @return the fftIn
   */
  public abstract CUdeviceptr getFftIn();

  /**
   * @return the pcmIn
   */
  public abstract CUdeviceptr getPcmIn();

  /**
   * @return the pcm
   */
  public abstract CUdeviceptr getPcm();

  /**
   * @return the maxOut
   */
  public abstract CUdeviceptr getMaxOut();

  /**
   * @return the multiMaxOut
   */
  public abstract CUdeviceptr getMultiMaxOut();

  /**
   * @return the multimax index output
   */
  public abstract CUdeviceptr getMultiIdxOut();

  /**
   * @return the idxOut
   */
  public abstract CUdeviceptr getIdxOut();

  /**
   * @return the idxFilter
   */
  public abstract CUdeviceptr getIdxFilter();

  /**
   * Returns a reference to the phase correlation matrix memory
   * 
   * @return the pcm memory
   */
  public abstract Pointer<Double> getPCMPMemory();

  /**
   * Gets the input phase correlation matrix memory
   * 
   * @return the pcm input memory
   */
  public abstract Pointer<Double> getPCMInMemory();

  /**
   * @return the FFT In pointer
   */
  public abstract Pointer<Double> getFFTInP();

  /**
   * @return the array of indices memory
   */
  public abstract Integer[] getIndices();

  /**
   * @return the peaks memory
   */
  public abstract Pointer<Integer> getPeaks();

  /**
   * @return the array memory
   */
  public abstract float[][] getArrayMemory();

}
