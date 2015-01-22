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
// Date: Aug 1, 2013 4:05:38 PM EST
//
// Time-stamp: <Aug 1, 2013 4:05:38 PM tjb3>
//
//
// ================================================================

package test.timing;

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
