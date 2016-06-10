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

package gov.nist.isg.mist.lib32.imagetile.java;

import java.io.File;

import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.java.Fft;
import gov.nist.isg.mist.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.memorypool.DynamicMemoryPool;
import jcuda.driver.CUstream;

/**
 * Represents an image tile that uses only java (no native bindings). Must initialize an FFT plan
 * before using.
 *
 * <pre>
 * <code>
 * JavaImageTile32.initJavaPlan(initialTile);
 * </pre>
 *
 * </code>
 *
 * @author Tim Blattner
 * @version 2013.08.7
 */
public class JavaImageTile32 extends ImageTile<float[][]> {

  /**
   * The Java FFT plan
   */
  public static Fft fftPlan = null;


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
  public JavaImageTile32(File file, int row, int col, int gridWidth, int gridHeight, int startRow,
                         int startCol) {
    super(file, row, col, gridWidth, gridHeight, startRow, startCol);
  }

  /**
   * Creates an image tile from a file
   *
   * @param file the image tile file
   */
  public JavaImageTile32(File file) {
    this(file, 0, 0, 1, 1, 0, 0);
  }


  @Override
  public void releaseFftMemory() {
    if (this.fft != null)
      this.fft = null;
  }


  /**
   * Computes this image's FFT
   */
  @Override
  public void computeFft() {

    // if the file does not exists on disk, skip computing the fft
    if (!this.fileExists())
      return;

    if (fftPlan == null)
      initJavaPlan(this);

    readTile();

    this.fft =
        new float[fftPlan.getFrequencySampling2().getCount()][fftPlan.getFrequencySampling1()
            .getCount() * 2];

    copyAndPadFFT();

    fftPlan.applyForwardPadded(this.fft);

  }

  /**
   * Computes this image's FFT
   *
   * @param pool   pool of memory that we might allocate memory from if it is not loaded
   * @param memory extra memory for input if needed
   */
  @Override
  public void computeFft(DynamicMemoryPool<float[][]> pool, TileWorkerMemory memory) {

    // if the file does not exists on disk, skip computing the fft
    if (!this.fileExists())
      return;

    readTile();

    if (!super.isMemoryLoaded()) {
      this.fft = pool.getMemory();
      super.setMemoryLoaded(true);
    }


    copyAndPadFFT();

    fftPlan.applyForwardPadded(this.fft);
  }

  @Override
  public void computeFft(DynamicMemoryPool<float[][]> pool, TileWorkerMemory memory, CUstream stream) {
    computeFft(pool, memory);
  }

  /**
   * Copies pixel data into fft array for in place transform.
   */
  private void copyAndPadFFT() {
    // n1 = width
    // n2 = height
    int n1 = fftPlan.getFrequencySampling1().getCount() * 2;
    int n2 = fftPlan.getFrequencySampling2().getCount();

    for (int r = 0; r < n2; r++) {
      for (int c = 0; c < n1; c++) {

        if (r < this.getHeight() && c < this.getWidth()) {
          this.fft[r][c] = super.getPixels().getPixelValue(c, r);
        } else {
          this.fft[r][c] = 0.0f;
        }
      }
    }


  }

  /**
   * Initializes FFT java plan. This plan might apply padding based on paddedHeight/2
   *
   * @param tile the initial tile to get the width and height
   */
  public static void initJavaPlan(ImageTile<?> tile) {
    tile.readTile();

    Log.msg(LogType.VERBOSE, "Initializing Java FFT Plans.");

    fftPlan = new Fft(tile.getWidth(), tile.getHeight());
    fftPlan.setComplex(false);
    fftPlan.setOverwrite(true);
  }

}
