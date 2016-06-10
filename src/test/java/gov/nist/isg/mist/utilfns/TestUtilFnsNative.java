// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.




// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 4:20:49 PM EST
//
// Time-stamp: <Aug 1, 2013 4:20:49 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.utilfns;

import org.bridj.BridJ;
import org.bridj.Pointer;

import java.io.File;

import gov.nist.isg.mist.lib.imagetile.utilfns.UtilFnsLibrary;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;

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
