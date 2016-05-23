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
// Date: Aug 1, 2013 3:52:13 PM EST
//
// Time-stamp: <Aug 1, 2013 3:52:13 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.log;

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
