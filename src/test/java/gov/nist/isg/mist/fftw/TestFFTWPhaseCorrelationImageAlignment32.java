// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.




// ================================================================
//
// Author: tjb3
// Date: May 10, 2013 2:59:15 PM EST
//
// Time-stamp: <May 10, 2013 2:59:15 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.fftw;

import java.io.File;
import java.io.FileNotFoundException;

import gov.nist.isg.mist.lib.common.CorrelationTriple;
import gov.nist.isg.mist.lib.imagetile.Stitching;
import gov.nist.isg.mist.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.lib.imagetile.utilfns.UtilFnsStitching;
import gov.nist.isg.mist.lib.log.Debug;
import gov.nist.isg.mist.lib.log.Debug.DebugType;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib32.imagetile.fftw.FFTW3Library32;
import gov.nist.isg.mist.lib32.imagetile.fftw.FftwImageTile32;
import gov.nist.isg.mist.lib32.imagetile.memory.FftwTileWorkerMemory32;
import gov.nist.isg.mist.timing.TimeUtil;

/**
 * Test case for computing the phase correlation between two images using FFTW.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TestFFTWPhaseCorrelationImageAlignment32 {

  /**
   * Computes the phase correlation between two tiles using FFTW
   */
  public static void runTestPhaseCorrelationImageAlignment() throws FileNotFoundException {
    Log.setLogLevel(LogType.VERBOSE);
    Debug.setDebugLevel(DebugType.VERBOSE);
    UtilFnsStitching.disableUtilFnsNativeLibrary();
    Log.msg(LogType.MANDATORY, "Running Test Phase Correlation Image Alignment FFTW");

    // Read two images.
    File file1 = new File("C:\\majurski\\image-data\\1h_Wet_10Perc\\KB_2012_04_13_1hWet_10Perc_IR_00002.tif");
    File file2 = new File("C:\\majurski\\image-data\\1h_Wet_10Perc\\KB_2012_04_13_1hWet_10Perc_IR_00003.tif");

    FftwImageTile32 neighbor = new FftwImageTile32(file1, 0, 0, 2, 2, 0, 0);
    FftwImageTile32 origin = new FftwImageTile32(file2, 1, 0, 2, 2, 0, 0);

    Log.msg(LogType.INFO, neighbor.toString());
    Log.msg(LogType.INFO, origin.toString());

    if (FftwImageTile32.initLibrary("C:\\majurski\\NISTGithub\\MISTMain\\lib\\fftw", "", "libfftw3f")) {
      FftwImageTile tile = new FftwImageTile(file1);

      Log.msg(LogType.INFO, "Loading FFTW plan");
      TimeUtil.tick();
      FftwImageTile32.initPlans(tile.getWidth(), tile.getHeight(), FFTW3Library32.FFTW_MEASURE, true, "test.dat");
      Log.msg(LogType.INFO, "Loaded plan in " + TimeUtil.tock() + " ms");

      FftwImageTile32.savePlan("test.dat");

      TimeUtil.tick();
      TileWorkerMemory memory = new FftwTileWorkerMemory32(tile);
      CorrelationTriple result =
          Stitching.phaseCorrelationImageAlignmentFftw(neighbor, origin, memory);

      Log.msg(
          LogType.MANDATORY,
          "Completed image alignment between " + neighbor.getFileName() + " and "
              + origin.getFileName() + " with " + result.toString() + " in " + TimeUtil.tock()
              + "ms");
    }
    Log.msg(LogType.MANDATORY, "Test Completed.");

  }

  /**
   * Executes the test case
   *
   * @param args not used
   */
  public static void main(String[] args) {
    try {
      TestFFTWPhaseCorrelationImageAlignment32.runTestPhaseCorrelationImageAlignment();
    } catch (FileNotFoundException e) {
      Log.msg(LogType.MANDATORY, "Unable to find file: " + e.getMessage());
    }
  }

}
