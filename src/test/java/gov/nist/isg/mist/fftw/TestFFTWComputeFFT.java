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
// Date: May 10, 2013 2:59:10 PM EST
//
// Time-stamp: <May 10, 2013 2:59:10 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.fftw;

import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FFTW3Library;
import gov.nist.isg.mist.timing.TimeUtil;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Test case for computing the FFT of an image using FFTW
 * 
 * @author Tim Blattner
 * @version 1.0
 */
public class TestFFTWComputeFFT {

  /**
   * Computes the FFT for an image using FFTW
   */
  public static void runTestFFTImage() throws FileNotFoundException {
    Log.setLogLevel(LogType.INFO);

    Log.msg(LogType.MANDATORY, "Running Test Compute FFT Image using FFTW");


    File file = new File("C:\\majurski\\image-data\\1h_Wet_10Perc\\KB_2012_04_13_1hWet_10Perc_IR_00001.tif");
    FftwImageTile.initLibrary("C:\\majurski\\NISTGithub\\MIST\\lib\\fftw", "", "libfftw3");

    FftwImageTile tile = new FftwImageTile(file);

    Log.msg(LogType.INFO, "Loading FFTW plan");

    FftwImageTile.initPlans(tile.getWidth(), tile.getHeight(), FFTW3Library.FFTW_MEASURE, true, "test.dat");
    FftwImageTile.savePlan("test.dat");
    Log.msg(LogType.INFO, "Computing FFT");
    TimeUtil.tick();
    tile.computeFft();
    Log.msg(LogType.HELPFUL, "Finished Computing FFT in " + TimeUtil.tock() + " ms");

    tile.releaseFftMemory();

    FftwImageTile.destroyPlans();

    Log.msg(LogType.MANDATORY, "Test Completed.");
  }

  /**
   * Executes the test case
   * 
   * @param args not used
   */
  public static void main(String[] args) {
    try {
        TestFFTWComputeFFT.runTestFFTImage();
    } catch (FileNotFoundException e)
    {
        Log.msg(LogType.MANDATORY, "Unable to find file: " + e.getMessage());
    }
  }
}
