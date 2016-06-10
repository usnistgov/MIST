// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 11, 2014 11:40:10 AM EST
//
// Time-stamp: <Apr 11, 2014 11:40:10 AM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.imagetile.memory;

import org.bridj.Pointer;

import java.nio.ByteBuffer;

import jcuda.driver.CUdeviceptr;

/**
 * Class that represents the memory required for stitching a pair of tiles. This class is used as a
 * super class for other TileWorkerMemories.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public abstract class TileWorkerMemory<T> {

  private int width;
  private int height;

  /**
   * Initializes tile worker memory
   *
   * @param tileWidth  the width of the image tile
   * @param tileHeight the height of the image tile
   */
  public TileWorkerMemory(int tileWidth, int tileHeight) {
    this.width = tileWidth;
    this.height = tileHeight;
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
  public abstract Pointer<T> getPCMPMemory();

  /**
   * Gets the input phase correlation matrix memory
   *
   * @return the pcm input memory
   */
  public abstract Pointer<T> getPCMInMemory();

  /**
   * @return the FFT In pointer
   */
  public abstract Pointer<T> getFFTInP();

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
