
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
// Date: May 10, 2013 2:59:20 PM EST
//
// Time-stamp: <May 10, 2013 2:59:20 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.basic;

import java.io.File;

import javax.swing.*;

import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.timing.TimeUtil;

/**
 * Test case for reading an image.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TestReadingImage {

  /**
   * Tests reading an image
   */
  public static void testReadingImage() {
    JFrame frame = new JFrame();

    Log.msg(LogType.MANDATORY, "Running Test Reading Image");

    JFileChooser chooser =
        new JFileChooser("F:\\StitchingData\\StitchingTestSlowFileLoad\\SSEA4_5x(1)");
    chooser.setDialogTitle("Select File to test reading image.");

    int result = chooser.showOpenDialog(frame);

    // File file = new
    // File("C:\\Users\\tjb3\\Desktop\\input_images\\F_0001.tif");

    if (result == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();

      TimeUtil.tick();
      ImageTile<?> tile = new FftwImageTile(file);
      Log.msg(LogType.INFO, "Read Complete: " + tile.toString());
    } else {
      Log.msg(LogType.MANDATORY, "Canceled selecting image.");
    }

    Log.msg(LogType.MANDATORY, "Test Completed in " + TimeUtil.tock() + "ms");
  }

  /**
   * Executes the test case
   *
   * @param args not used
   */
  public static void main(String[] args) {
    TestReadingImage.testReadingImage();
  }
}
