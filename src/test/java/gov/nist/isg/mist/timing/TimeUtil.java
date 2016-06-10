// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.




// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 4:05:38 PM EST
//
// Time-stamp: <Aug 1, 2013 4:05:38 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.timing;

/**
 * Timing utlity functions for obtaining timing information in milliseconds.
 *
 * <pre>
 * <code>
 * TimeUtil.tick();
 * Run.Computation();
 * System.out.println("The compute time is: " + TimeUtil.tock() + " ms");
 * </code>
 * </pre>
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TimeUtil {

  private static long tickTime = 0L;

  /**
   * Utility function to start a timer (must not call tock before calling tick)
   */
  public static void tick() {
    if (tickTime == 0L) {
      tickTime = System.currentTimeMillis();
    } else {
      System.err.println("Must call tock before tick");
      System.exit(1);
    }
  }

  /**
   * Utility function to get the time from calling tick. Must call tick before calling tock.
   *
   * @return the difference between calling tick and tock
   */
  public static long tock() {
    if (tickTime == 0L) {
      System.err.println("Must call tick before tock");
      System.exit(1);
    }

    long ret = System.currentTimeMillis() - tickTime;
    tickTime = 0L;
    return ret;

  }

}
