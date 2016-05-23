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
// Date: May 10, 2013 2:59:15 PM EST
//
// Time-stamp: <May 10, 2013 2:59:15 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.java;

import gov.nist.isg.mist.timing.TimeUtil;
import gov.nist.isg.mist.stitching.lib.common.CorrelationTriple;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.imagetile.java.JavaImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.JavaTileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.log.Debug;
import gov.nist.isg.mist.stitching.lib.log.Debug.DebugType;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Test case for computing the phase correlation between two images using Java.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TestJavaPhaseCorrelationImageAlignment {

  /**
   * Computes the phase correlation between two images.
   */
  public static void runTestPhaseCorrelationImageAlignment() throws FileNotFoundException {
    Log.setLogLevel(LogType.VERBOSE);
    Debug.setDebugLevel(DebugType.VERBOSE);

    Log.msg(LogType.MANDATORY, "Running Test Phase Correlation Image Alignment Java");

    // Read two images.
    File file1 = new File("F:\\StitchingData\\70perc_input_images\\F_2420.tif");
    File file2 = new File("F:\\StitchingData\\70perc_input_images\\F_2421.tif");

    // File file1 = new File("F:\\StitchingData\\70perc_input_images\\F_0001.tif");
    // File file2 = new File("F:\\StitchingData\\70perc_input_images\\F_0002.tif");

    JavaImageTile t1 = new JavaImageTile(file1, 0, 0, 2, 2, 0, 0);
    JavaImageTile t2 = new JavaImageTile(file2, 1, 0, 2, 2, 0, 0);

    JavaImageTile.initJavaPlan(t1);

    TimeUtil.tick();
    TileWorkerMemory memory = new JavaTileWorkerMemory(t1);

    CorrelationTriple result = Stitching.phaseCorrelationImageAlignmentJava(t1, t2, memory);

    Log.msg(LogType.MANDATORY, "Completed image alignment between " + t1.getFileName() + " and "
        + t2.getFileName() + " with " + result.toString() + " in " + TimeUtil.tock() + "ms");

    Log.msg(LogType.MANDATORY, "Test Completed.");

  }

  /**
   * Executes the test case
   *
   * @param args not used
   */
  public static void main(String[] args) {
    try {
      TestJavaPhaseCorrelationImageAlignment.runTestPhaseCorrelationImageAlignment();
    } catch (FileNotFoundException e) {
      Log.msg(LogType.MANDATORY, "Unable to find file: " + e.getMessage());
    }
  }


}
