// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: May 16, 2014 3:52:20 PM EST
//
// Time-stamp: <May 16, 2014 3:52:20 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.imagetile.memory;

import org.bridj.Pointer;

import java.nio.ByteBuffer;

import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.java.JavaImageTile;
import jcuda.driver.CUdeviceptr;

/**
 * Represents memory that a Java Tile will be working with
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class JavaTileWorkerMemory extends TileWorkerMemory {

  private float[][] arrayMemory;

  /**
   * Initializes the Java tile worker memory
   *
   * @param initTile the initial tile
   */
  public JavaTileWorkerMemory(ImageTile<?> initTile) {
    super(initTile.getWidth(), initTile.getHeight());

    int fftWidth = JavaImageTile.fftPlan.getFrequencySampling1().getCount() * 2;
    int fftHeight = JavaImageTile.fftPlan.getFrequencySampling2().getCount();
    this.arrayMemory = new float[fftHeight][fftWidth];
  }

  @Override
  public float[][] getArrayMemory() {
    return this.arrayMemory;
  }

  @Override
  public void releaseMemory() {
    this.arrayMemory = null;
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
  public Pointer<Integer> getPeaks() {
    throw new IllegalStateException("getPeaks is only used for FFTW Tile workers");
  }


}
