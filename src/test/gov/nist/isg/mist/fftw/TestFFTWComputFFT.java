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

package test.gov.nist.isg.mist.fftw;

import java.io.File;
import java.io.FileNotFoundException;

import test.gov.nist.isg.mist.timing.TimeUtil;
import main.gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwImageTile;
import main.gov.nist.isg.mist.stitching.lib.log.Log;
import main.gov.nist.isg.mist.stitching.lib.log.Log.LogType;

/**
 * Test case for computing the FFT of an image using FFTW
 * 
 * @author Tim Blattner
 * @version 1.0
 */
public class TestFFTWComputFFT {

  /**
   * Computes the FFT for an image using FFTW
   */
  public static void runTestFFTImage() throws FileNotFoundException {
    Log.setLogLevel(LogType.INFO);
    // JFrame frame = new JFrame();
    Log.msg(LogType.MANDATORY, "Running Test Compute FFT Image using FFTW");
    //
    // JFileChooser chooser = new
    // JFileChooser("C:\\Users\\tjb3\\Desktop\\input_images");
    // chooser.setDialogTitle("Select File to test computing FFT.");

    // int result = chooser.showOpenDialog(frame);

    File file = new File("F:\\input_images\\F_0001.tif");

    // if (result == JFileChooser.APPROVE_OPTION)
    // {
    // File file = chooser.getSelectedFile();
    FftwImageTile.initLibrary(
        "C:\\Users\\tjb3\\Documents\\Visual Studio 2012\\Projects\\UtilFnsDll\\UtilFnsDll\\fftw",
        "C:\\Users\\tjb3\\Documents\\Visual Studio 2012\\Projects\\UtilFnsDll\\Debug", "libfftw3");
    // {
    FftwImageTile tile = new FftwImageTile(file);

    Log.msg(LogType.INFO, "Loading FFTW plan");

    FftwImageTile.initPlans(tile.getWidth(), tile.getHeight(), 0x21, true, "test.dat");
    FftwImageTile.savePlan("test.dat");
    Log.msg(LogType.INFO, "Computing FFT");
    TimeUtil.tick();
    tile.computeFft();
    Log.msg(LogType.HELPFUL, "Finished Computing FFT in " + TimeUtil.tock() + " ms");

    tile.releaseFftMemory();

    FftwImageTile.destroyPlans();

    // }

    Log.msg(LogType.MANDATORY, "Test Completed.");
  }

  /**
   * Executes the test case
   * 
   * @param args not used
   */
  public static void main(String[] args) {
    try {
        TestFFTWComputFFT.runTestFFTImage();
    } catch (FileNotFoundException e)
    {
        Log.msg(LogType.MANDATORY, "Unable to find file: " + e.getMessage());
    }
  }
}
