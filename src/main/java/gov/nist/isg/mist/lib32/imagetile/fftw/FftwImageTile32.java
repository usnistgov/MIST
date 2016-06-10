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

package gov.nist.isg.mist.lib32.imagetile.fftw;

import org.bridj.BridJ;
import org.bridj.Pointer;

import java.io.File;

import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.memorypool.DynamicMemoryPool;
import gov.nist.isg.mist.lib32.imagetile.fftw.FFTW3Library32.fftwf_plan;
import jcuda.driver.CUstream;

/**
 * Represents an image tile that uses native library bindings with FFTW. Must initialize the
 * libraries before using native bindings and initialize FFTW plans.
 *
 * <pre>
 * <code>
 * FFTWImageTile32.initLibrary(fftwPath, "");
 * \\/*
 *  * Or if you want to use native bindings for other routines...
 *  * FFTWImageTile32.initLibrary(fftwPath, utilFnsPath);
 *  *\\/
 *    FFTWImageTile32.initPlans(width, height, tryLoadExistingBoolean,
 *    pathToOldPlan);
 * </pre>
 *
 * </code>
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class FftwImageTile32 extends ImageTile<Pointer<Float>> {


  /**
   * The size of the FFT for FFTW
   */
  public static int fftSize;

  /**
   * The forward FFTW plan
   */
  public static fftwf_plan plan_fwd;

  /**
   * The backward FFTW plan (inverse FFT)
   */
  public static fftwf_plan plan_bwd;

  private Pointer<Float> fftIn;


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
  public FftwImageTile32(File file, int row, int col, int gridWidth, int gridHeight, int startRow,
                         int startCol) {
    super(file, row, col, gridWidth, gridHeight, startRow, startCol);
  }

  /**
   * Creates an image tile from a file
   *
   * @param file the image tile file
   */
  public FftwImageTile32(File file) {
    this(file, 0, 0, 1, 1, 0, 0);
  }


  @Override
  public void releaseFftMemory() {
    if (super.isMemoryLoaded()) {
      FFTW3Library32.fftwf_free(this.fft);
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

    Pointer<Float> fftIn = FFTW3Library32.fftwf_alloc_real(super.getWidth() * super.getHeight());
    this.fft = FFTW3Library32.fftwf_alloc_complex(fftSize);


    for (long r = 0; r < super.getHeight(); r++)
      for (long c = 0; c < super.getWidth(); c++) {
        fftIn.setFloatAtIndex(r * super.getWidth() + c, super.getPixels().getPixelValue((int) c, (int) r));
      }

    FFTW3Library32.fftwf_execute_dft_r2c(plan_fwd, fftIn, this.fft);
    FFTW3Library32.fftwf_free(fftIn);
    super.setMemoryLoaded(true);
  }

  /**
   * Computes this image's FFT
   */
  @Override
  public void computeFft(DynamicMemoryPool<Pointer<Float>> pool, TileWorkerMemory memory) {

    // if the file does not exists on disk, skip computing the fft
    if (!this.fileExists())
      return;

    readTile();

    if (super.isMemoryLoaded()) {
      this.fftIn = memory.getFFTInP();
      for (long r = 0; r < super.getHeight(); r++)
        for (long c = 0; c < super.getWidth(); c++) {
          fftIn.setFloatAtIndex(r * super.getWidth() + c, super.getPixels().getPixelValue((int) c, (int) r));
        }

      FFTW3Library32.fftwf_execute_dft_r2c(plan_fwd, this.fftIn, this.fft);
    }
  }

  @Override
  public void computeFft(DynamicMemoryPool<Pointer<Float>> pool, TileWorkerMemory memory,
                         CUstream stream) {
    this.computeFft(pool, memory);
  }

  /**
   * Saves the FFTW plan to the path
   *
   * @param path the path we want to save
   * @return FFTW value indicating if the save was successful
   */
  public static int savePlan(String path) {
    Log.msg(LogType.MANDATORY, "Saving plan to file: " + path);
    Pointer<Byte> fileName = Pointer.pointerToCString(path);
    return FFTW3Library32.fftwf_export_wisdom_to_filename(fileName);
  }

  /**
   * Loads the FFTW plan from the path
   *
   * @param path the path we want to load
   * @return FFTW value indicating if the save was successful
   */
  public static int loadPlan(String path) {
    Pointer<Byte> fileName = Pointer.pointerToCString(path);
    return FFTW3Library32.fftwf_import_wisdom_from_filename(fileName);
  }

  /**
   * Initializes native libraries
   *
   * @param libFFTWPath     the path to the FFTW library
   * @param libUtilFnsPath  the path to the UtilFns library
   * @param fftwLibraryName the name of the FFTW library
   * @return true if all libraries are loaded, otherwise false
   */
  public static boolean initLibrary(String libFFTWPath, String libUtilFnsPath,
                                    String fftwLibraryName) {
    boolean loaded = false;
    try {
      BridJ.addNativeLibraryAlias("fftwf", fftwLibraryName);
      BridJ.addNativeLibraryAlias("fftwf", "fftwf3");
      BridJ.addNativeLibraryAlias("fftwf", "fftwf3-3");
      BridJ.addLibraryPath(libFFTWPath);
      FFTW3Library32.fftwf_cleanup();
      Log.msg(LogType.INFO, "FFTW library loaded successfully");
      loaded = true;

    } catch (UnsatisfiedLinkError ex) {
      Log.msg(LogType.MANDATORY, "Unable to load FFTW: " + ex.toString());
      return false;
    } catch (Exception e) {
      Log.msg(LogType.MANDATORY, "Unable to load FFTW: " + e.toString());
      return false;
    }

//    try {
//      BridJ.addLibraryPath(libUtilFnsPath);
//
//      OSType osType = LibUtils.calculateOS();
//      switch (osType) {
//        case WINDOWS:
//          BridJ.setNativeLibraryActualName("utilfns", "util-fns");
//          break;
//        case APPLE:
//        case LINUX:
//        case SUN:
//        case UNKNOWN:
//        default:
//          break;
//
//      }
//
//      UtilFnsLibrary.reduce_max_abs(Pointer.allocateDouble(), 0);
//      Log.msg(LogType.INFO, "UtilFns library loaded successfully");
//      loaded &= true;
//      UtilFnsStitching.enableUtilFnsNativeLibrary();
//
//    } catch (UnsatisfiedLinkError ex) {
//      Log.msg(LogType.MANDATORY, "Unabled to load UtilFns: " + ex.toString());
//      UtilFnsStitching.disableUtilFnsNativeLibrary();
//      Log.msg(LogType.MANDATORY, "Falling back to non-native routines");
//
//    } catch (Exception e) {
//      Log.msg(LogType.MANDATORY, "Unabled to load UtilFns: " + e.toString());
//      UtilFnsStitching.disableUtilFnsNativeLibrary();
//      Log.msg(LogType.MANDATORY, "Falling back to non-native routines");
//    }
    return loaded;
  }

  /**
   * Initializes a FFTW plan using the width, height, and flags
   *
   * @param width    the width for the tile
   * @param height   the height for the tile
   * @param flags    the FFTW flag(s) for the plan
   * @param loadPlan true if you want to try to load a plan from disk
   * @param plan     the plan's path you are trying to load
   */
  public static void initPlans(int width, int height, int flags, boolean loadPlan, String plan) {
    if (loadPlan) {
      int result = FftwImageTile32.loadPlan(plan);

      if (result == 0) {
        Log.msg(LogType.MANDATORY, "Error loading plan from file: " + plan + "... generating plan.");
      } else {
        Log.msg(LogType.MANDATORY, "Successfully loaded plan from file");
      }
    }

    Log.msg(LogType.MANDATORY, "Loading FFTW Plan...");

    fftSize = (width / 2 + 1) * height;

    Pointer<Float> ptrIn = FFTW3Library32.fftwf_alloc_real(width * height);
    Pointer<Float> ptrOut = FFTW3Library32.fftwf_alloc_complex(fftSize);

    Pointer<Float> ptrC2RIn = FFTW3Library32.fftwf_alloc_complex(fftSize);
    Pointer<Float> ptrC2ROut = FFTW3Library32.fftwf_alloc_real(width * height);

    plan_fwd = FFTW3Library32.fftwf_plan_dft_r2c_2d(height, width, ptrIn, ptrOut, flags);
    plan_bwd = FFTW3Library32.fftwf_plan_dft_c2r_2d(height, width, ptrC2RIn, ptrC2ROut, flags);

    FFTW3Library32.fftwf_free(ptrIn);
    FFTW3Library32.fftwf_free(ptrOut);
    FFTW3Library32.fftwf_free(ptrC2RIn);
    FFTW3Library32.fftwf_free(ptrC2ROut);

  }

  /**
   * Destroys forward and backward plan's memory
   */
  public static void destroyPlans() {
    FFTW3Library32.fftwf_destroy_plan(plan_fwd);
    FFTW3Library32.fftwf_destroy_plan(plan_bwd);
  }

}
