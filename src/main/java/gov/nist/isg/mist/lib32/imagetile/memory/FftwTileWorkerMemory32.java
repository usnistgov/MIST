// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 11, 2014 11:37:36 AM EST
//
// Time-stamp: <Apr 11, 2014 11:37:36 AM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib32.imagetile.memory;

import org.bridj.Pointer;

import java.nio.ByteBuffer;

import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.Stitching;
import gov.nist.isg.mist.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.lib32.imagetile.fftw.FFTW3Library32;
import gov.nist.isg.mist.lib32.imagetile.fftw.FftwImageTile32;
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
