// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.




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

import java.io.File;
import java.io.FileNotFoundException;

import gov.nist.isg.mist.lib.imagetile.fftw.FFTW3Library;
import gov.nist.isg.mist.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.timing.TimeUtil;

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
    FftwImageTile.initLibrary("C:\\majurski\\NISTGithub\\MISTMain\\lib\\fftw", "", "libfftw3");

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
    } catch (FileNotFoundException e) {
      Log.msg(LogType.MANDATORY, "Unable to find file: " + e.getMessage());
    }
  }
}
