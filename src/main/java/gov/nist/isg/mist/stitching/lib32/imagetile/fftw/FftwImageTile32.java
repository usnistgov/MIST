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

package gov.nist.isg.mist.stitching.lib32.imagetile.fftw;

import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib32.imagetile.fftw.FFTW3Library32.fftwf_plan;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.memorypool.DynamicMemoryPool;
import jcuda.driver.CUstream;

import org.bridj.BridJ;
import org.bridj.Pointer;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Represents an image tile that uses native library bindings with FFTW. Must initialize the
 * libraries before using native bindings and initialize FFTW plans.
 *
 * <pre>
 * <code>
 * FFTWImageTile.initLibrary(fftwPath, "");
 * \\/*
 *  * Or if you want to use native bindings for other routines...
 *  * FFTWImageTile.initLibrary(fftwPath, utilFnsPath);
 *  *\\/
 *    FFTWImageTile.initPlans(width, height, tryLoadExistingBoolean,
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
  public FftwImageTile32(File file, int row, int col, int gridWidth, int gridHeight, int startRow,
                         int startCol, boolean read) {
    super(file, row, col, gridWidth, gridHeight, startRow, startCol, read);
  }

  /**
   * Creates an image tile from a file
   *
   * @param file the image tile file
   */
  public FftwImageTile32(File file) {
    this(file, 0, 0, 1, 1, 0, 0, true);
  }

  /**
   * Initializes image tile and optionally does not read
   *
   * @param file the file assosiated with this tile
   * @param read whether or not to read the tile here
   */
  public FftwImageTile32(File file, boolean read) {
    this(file, 0, 0, 1, 1, 0, 0, read);
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
  public void computeFft() throws FileNotFoundException {

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
  public void computeFft(DynamicMemoryPool<Pointer<Float>> pool, TileWorkerMemory memory) throws FileNotFoundException {
    readTile();

    if (super.isMemoryLoaded()) {
      this.fftIn = memory.getFFTInP();
      int count = 0;
      for (long r = 0; r < super.getHeight(); r++)
        for (long c = 0; c < super.getWidth(); c++) {
          fftIn.setFloatAtIndex(r * super.getWidth() + c, super.getPixels().getPixelValue((int) c, (int) r));
        }

      FFTW3Library32.fftwf_execute_dft_r2c(plan_fwd, this.fftIn, this.fft);
    }
  }

  @Override
  public void computeFft(DynamicMemoryPool<Pointer<Float>> pool, TileWorkerMemory memory,
                         CUstream stream) throws FileNotFoundException {
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
      Log.msg(LogType.MANDATORY, "Unabled to load FFTW: " + ex.toString());
      return false;
    } catch (Exception e) {
      Log.msg(LogType.MANDATORY, "Unabled to load FFTW: " + e.toString());
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
