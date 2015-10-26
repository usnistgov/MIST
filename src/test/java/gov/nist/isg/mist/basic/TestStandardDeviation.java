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
// Date: May 10, 2013 2:59:20 PM EST
//
// Time-stamp: <May 10, 2013 2:59:20 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.basic;

import gov.nist.isg.mist.timing.TimeUtil;
import gov.nist.isg.mist.stitching.lib.common.Array2DView;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;

import javax.swing.*;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Test case for reading an image.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TestStandardDeviation {

  /**
   * Tests reading an image
   */
  public static void testStdDev() throws FileNotFoundException {
    // JFrame frame = new JFrame();

    Log.msg(LogType.MANDATORY, "Running Test StdDev Image");

    // JFileChooser chooser = new
    // JFileChooser("F:\\StitchingData\\1h_wet\\10P");
    // chooser.setDialogTitle("Select File to test reading image.");

    int result = JFileChooser.APPROVE_OPTION;// =
    // chooser.showOpenDialog(frame);

    File file = new File("F:\\StitchingData\\1h_wet\\10P\\KB_2012_04_13_1hWet_10Perc_IR_00001.tif");

    if (result == JFileChooser.APPROVE_OPTION) {
      // File file = chooser.getSelectedFile();

      ImageTile<?> tile = new FftwImageTile(file);
      tile.readTile();
      Log.msg(LogType.INFO, "Read Complete: " + tile.toString());

      Array2DView view = new Array2DView(tile, 0, tile.getHeight(), 0, tile.getWidth());

      TimeUtil.tick();
      double stdDevTwoPass = view.getStdDevTwoPass();
      Log.msg(LogType.MANDATORY, "Two-Pass StdDev: " + stdDevTwoPass + "  took: " + TimeUtil.tock());


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
    try {
      TestStandardDeviation.testStdDev();
    } catch (FileNotFoundException e) {
      Log.msg(LogType.MANDATORY, "Unable to load file: " + e.getMessage());
    }
  }
}
