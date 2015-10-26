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
// Date: Aug 1, 2013 4:18:02 PM EST
//
// Time-stamp: <Aug 1, 2013 4:18:02 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.fftw;

import gov.nist.isg.mist.timing.TimeUtil;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;

import java.io.File;

/**
 * Test case for loading/saving/creating FFTW wisdom files.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TestFFTWWisdom {

  /**
   * Tests generating FFTW wisdoms natively
   */
  public static void runTestFFTWTestWisdom() {
    Log.msg(LogType.MANDATORY, "Running Test Create FFTW Wisdom");

    File file = new File("/home/img-stitching/input_images/F_0001.tif");
    if (FftwImageTile.initLibrary("/usr/nist_stitching.lib",
        "/home/tjb3/workspace/image-stitching/util-fns", "libfftw3")) {
      FftwImageTile tile = new FftwImageTile(file);

      Log.msg(LogType.INFO, "Loading FFTW plan");
      TimeUtil.tick();
      FftwImageTile.initPlans(tile.getWidth(), tile.getHeight(), 0x41, false, null);

      Log.msg(LogType.INFO, "Finished creating FFTW plans in: " + TimeUtil.tock() + " ms");

      Log.msg(LogType.INFO, "Testing saving FFTW plan");
      TimeUtil.tick();
      if (FftwImageTile.savePlan("test.dat") == 0)
        Log.msg(LogType.INFO, "Failed to save plan");
      else
        Log.msg(LogType.INFO, "Saving plan test Complete in " + TimeUtil.tock() + " ms");

      Log.msg(LogType.INFO, "Testing destroying plans");
      FftwImageTile.destroyPlans();
      Log.msg(LogType.INFO, "Plan destruction test completed");

      Log.msg(LogType.INFO, "Testing loading FFTW plan");
      FftwImageTile.initPlans(tile.getWidth(), tile.getHeight(), 0x41, true, "test.dat");

      FftwImageTile.destroyPlans();

    }

    Log.msg(LogType.MANDATORY, "Test Completed.");

  }

  /**
   * Executes the test case
   *
   * @param args not used
   */
  public static void main(String[] args) {
    TestFFTWWisdom.runTestFFTWTestWisdom();
  }
}
