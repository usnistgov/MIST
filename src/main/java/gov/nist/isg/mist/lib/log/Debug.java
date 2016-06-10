// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 3:52:13 PM EST
//
// Time-stamp: <Aug 1, 2013 3:52:13 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.log;

import ij.IJ;

/**
 * Utility functions for printing debug information. <p> A debug message is only printed if the
 * debug level is set to something greater than or equal to the static debug level.
 *
 * <pre>
 * <code>
 * Debug.setDebugLevel(debugLevel); // to set debug level for printing
 * Debug.msg(level, "Debug message with newline");
 * Debug.msgnonl(level, "Debug message without newline");
 * </code>
 * </pre>
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class Debug {

  /**
   * Different types of logging
   */
  public static enum DebugType {

    /**
     * All debugging is turned completely off and no log output is printed
     */
    NONE("None"),

    /**
     * Must print debug messages that always are printed
     */
    MANDATORY("Mandatory"),

    /**
     * Helpfuul debug messages for the programmer
     */
    HELPFUL("Helpful"),

    /**
     * debug messages that are informative
     */
    INFO("Info"),

    /**
     * Verbose debug messages prints a lot of information including class, method, and line
     */
    VERBOSE("Verbose");

    private DebugType(final String text) {
      this.text = text;
    }

    private final String text;


    @Override
    public String toString() {
      return this.text;
    }


  }

  private static DebugType debugLevel = DebugType.MANDATORY;

  /**
   * Sets the debugger level
   *
   * @param level the new debug level
   */
  public static void setDebugLevel(String level) {
    DebugType type = DebugType.valueOf(level.toUpperCase());
    if (type == null)
      Debug.debugLevel = DebugType.NONE;
    else
      Debug.debugLevel = type;
  }

  /**
   * Set debugger level.
   *
   * @param level the new debug level
   */
  public static void setDebugLevel(DebugType level) {
    Debug.debugLevel = level;
  }

  /**
   * Prints debug message at level with newline
   *
   * @param level   the debug level
   * @param message the message
   */
  public static void msg(DebugType level, String message) {
    if (level.ordinal() <= Debug.debugLevel.ordinal()) {
      if (Debug.debugLevel == DebugType.VERBOSE) {
        // Get the class and line number information from the stack
        // 3 because we want to omit this method and the calling method
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        if (stackTrace != null) {
          String fullClassName = stackTrace[3].getClassName();
          String methodName = stackTrace[3].getMethodName();
          int lineNumber = stackTrace[3].getLineNumber();
          message = fullClassName + ":" + methodName + ":" + lineNumber + " - " + message;
        }
      }

      IJ.log(message);
      // System.out.println(message);
    }
  }

  /**
   * Prints debug message at level without newline
   *
   * @param level   the debug level
   * @param message the message
   */
  public static void msgnonl(DebugType level, String message) {
    if (level.ordinal() <= Debug.debugLevel.ordinal()) {
      if (Debug.debugLevel == DebugType.VERBOSE) {
        // Get the class and line number information from the stck
        // 3 because we want to omit this method and the calling method
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        String fullClassName = stackTrace[3].getClassName();
        String methodName = stackTrace[3].getMethodName();
        int lineNumber = stackTrace[3].getLineNumber();
        message = fullClassName + ":" + methodName + ":" + lineNumber + " - " + message;
      }

      IJ.log(message);
      // System.out.print(message);
    }
  }

  /**
   * Gets the debug level
   *
   * @return the debug level
   */
  public static DebugType getDebugLevel() {
    return debugLevel;
  }

}
