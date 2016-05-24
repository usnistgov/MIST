
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
// Date: Aug 1, 2013 4:18:02 PM EST
//
// Time-stamp: <Aug 1, 2013 4:18:02 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.fftw;

import java.io.File;

import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib32.imagetile.fftw.FftwImageTile32;
import gov.nist.isg.mist.timing.TimeUtil;

/**
 * Test case for loading/saving/creating FFTW wisdom files.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TestFFTWWisdom32 {

  /**
   * Tests generating FFTW wisdoms natively
   */
  public static void runTestFFTWTestWisdom32() {
    Log.setLogLevel(LogType.INFO);
    Log.msg(LogType.MANDATORY, "Running Test Create FFTW 32 Wisdom");

    File file = new File("C:\\majurski\\image-data\\1h_Wet_10Perc\\KB_2012_04_13_1hWet_10Perc_IR_00001.tif");
    if (FftwImageTile32.initLibrary("C:\\majurski\\NISTGithub\\MIST\\lib\\fftw", "", "libfftw3f")) {

      FftwImageTile32 tile = new FftwImageTile32(file);

      Log.msg(LogType.INFO, "Loading FFTW plan");
      TimeUtil.tick();
      FftwImageTile32.initPlans(tile.getWidth(), tile.getHeight(), 0x41, false, null);

      Log.msg(LogType.INFO, "Finished creating FFTW plans in: " + TimeUtil.tock() + " ms");

      Log.msg(LogType.INFO, "Testing saving FFTW plan");
      TimeUtil.tick();
      if (FftwImageTile32.savePlan("test.dat") == 0)
        Log.msg(LogType.INFO, "Failed to save plan");
      else
        Log.msg(LogType.INFO, "Saving plan test Complete in " + TimeUtil.tock() + " ms");

      Log.msg(LogType.INFO, "Testing destroying plans");
      FftwImageTile32.destroyPlans();
      Log.msg(LogType.INFO, "Plan destruction test completed");

      Log.msg(LogType.INFO, "Testing loading FFTW plan");
      FftwImageTile32.initPlans(tile.getWidth(), tile.getHeight(), 0x41, true, "test.dat");

      FftwImageTile32.destroyPlans();

    }

    Log.msg(LogType.MANDATORY, "Test Completed.");

  }

  /**
   * Executes the test case
   *
   * @param args not used
   */
  public static void main(String[] args) {
    TestFFTWWisdom32.runTestFFTWTestWisdom32();
  }
}
