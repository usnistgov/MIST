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

package test.gov.nist.isg.mist.java;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import test.gov.nist.isg.mist.timing.TimeUtil;
import main.gov.nist.isg.mist.stitching.lib.imagetile.java.JavaImageTile;
import main.gov.nist.isg.mist.stitching.lib.log.Log;
import main.gov.nist.isg.mist.stitching.lib.log.Log.LogType;

/**
 * Test case for computing the FFT of an image using Java.
 * 
 * @author Tim Blattner
 * @version 1.0
 */
public class TestJavaComputeFFT {

  /**
   * Tests computing the FFT of an image using java.
   */
  public static void runTestFFTImage() {
    JFrame frame = new JFrame();

    Log.setLogLevel(LogType.INFO);
    Log.msg(LogType.MANDATORY, "Running Test Compute FFT Image Java");

    JFileChooser chooser = new JFileChooser("F:\\StitchingData\\joe_bad_data");
    chooser.setDialogTitle("Select File to test computing FFT.");

    int result = chooser.showOpenDialog(frame);

    if (result == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();

      JavaImageTile tile = new JavaImageTile(file);
      JavaImageTile.initJavaPlan(tile);
      Log.msg(LogType.INFO, "Read complete: " + tile.toString());

      TimeUtil.tick();
      tile.computeFft();


      Log.msg(LogType.MANDATORY, "FFT complete for: " + tile.getFileName() + " computed in "
          + TimeUtil.tock() + "ms");


    } else {
      Log.msg(LogType.MANDATORY, "Canceled selecting image.");
    }

    Log.msg(LogType.MANDATORY, "Test Completed.");
  }

  /**
   * Executes the test case
   * 
   * @param args not used
   */
  public static void main(String[] args) {
    TestJavaComputeFFT.runTestFFTImage();
  }
}
