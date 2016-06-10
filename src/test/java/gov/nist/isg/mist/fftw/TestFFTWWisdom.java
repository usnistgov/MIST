// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.




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

import gov.nist.isg.mist.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.timing.TimeUtil;

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
