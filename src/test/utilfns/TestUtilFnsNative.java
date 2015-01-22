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
// Date: Aug 1, 2013 4:20:49 PM EST
//
// Time-stamp: <Aug 1, 2013 4:20:49 PM tjb3>
//
//
// ================================================================

package test.utilfns;

import java.io.File;

import main.gov.nist.isg.mist.stitching.lib.imagetile.utilfns.UtilFnsLibrary;
import main.gov.nist.isg.mist.stitching.lib.log.Log;
import main.gov.nist.isg.mist.stitching.lib.log.Log.LogType;

import org.bridj.BridJ;
import org.bridj.Pointer;

/**
 * Test case for loading the util functions native library and running an example.
 * 
 * @author Tim Blattner
 * @version 1.0
 */
public class TestUtilFnsNative {

  /**
   * Tests loading UtilFns native library
   */
  public static void runTestUtilFnsNative() {
    try {
      Log.msg(LogType.MANDATORY, "Running testing loading util functions natively");
      BridJ.setNativeLibraryActualName("utilfns", "util-fns-windows");
      BridJ.addLibraryPath(System.getProperty("user.dir") + File.separator + "util-fns");

      Pointer<Double> test = Pointer.allocateDoubles(1000);
      UtilFnsLibrary.reduce_max_abs(test, 1000);

      Log.msg(LogType.MANDATORY, "Util FNS library loaded successfully");
    } catch (UnsatisfiedLinkError ex) {
      Log.msg(LogType.MANDATORY, "Unabled to load UtilFns library: " + ex.toString());
      Log.msg(LogType.MANDATORY, ex.getMessage());
    } catch (Exception e) {
      Log.msg(LogType.MANDATORY, "Unabled to load UtilFns library: " + e.toString());
      Log.msg(LogType.MANDATORY, e.getMessage());
    }

  }

  /**
   * Executes the test case
   * 
   * @param args not used
   */
  public static void main(String[] args) {
    TestUtilFnsNative.runTestUtilFnsNative();
  
  }
  
}
