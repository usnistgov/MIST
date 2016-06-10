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
// Date: Aug 1, 2013 3:52:18 PM EST
//
// Time-stamp: <Aug 1, 2013 3:52:18 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.log;

import ij.IJ;

/**
 * Utility functions for printing log information. <p> A log message is only printed if the log
 * level is set to something greater than or equal to the static log level.
 *
 * <pre>
 * <code>
 * Log.setDebugLevel(logLevel); // to set log level for printing
 * Log.msg(level, "Log message with newline");
 * Log.msgnonl(level, "Log message without newline");
 * </code>
 * </pre>
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class Log {

  /**
   * Different types of logging
   */
  public static enum LogType {

    /**
     * All logging is turned completely off and no log output is printed
     */
    NONE("None"),

    /**
     * Must print log messages that always are printed
     */
    MANDATORY("Mandatory"),

    /**
     * Helpfuul log messages for the programmer
     */
    HELPFUL("Helpful"),

    /**
     * log messages that are informative
     */
    INFO("Info"),

    /**
     * Verbose log messages prints a lot of information including class, method, and line
     */
    VERBOSE("Verbose");

    private LogType(final String text) {
      this.text = text;
    }

    private final String text;

    @Override
    public String toString() {
      return this.text;
    }
  }

  private static long startTime = 0;
  private static LogType logLevel = LogType.MANDATORY;
  private static boolean timeEnabled = false;

  /**
   * Enables timing in print statements
   */
  public static void enableTiming() {
    timeEnabled = true;
  }

  /**
   * Disables timing in print statements
   */
  public static void disableTiming() {
    timeEnabled = false;
  }

  /**
   * Sets the logger level
   *
   * @param level the new log level
   */
  public static void setLogLevel(String level) {
    LogType type = LogType.valueOf(level.toUpperCase());
    if (type == null)
      Log.logLevel = LogType.NONE;
    else
      Log.logLevel = type;
  }

  /**
   * Set logger level.
   *
   * @param level the new log level
   */
  public static void setLogLevel(LogType level) {
    Log.logLevel = level;
  }

  /**
   * Prints debug message at level with newline
   *
   * @param level   the log level
   * @param message the message
   */
  public static void msg(LogType level, String message) {

    if (level.ordinal() <= Log.logLevel.ordinal()) {
      if (Log.logLevel == LogType.VERBOSE) {
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

      if (timeEnabled) {
        if (startTime == 0)
          startTime = System.currentTimeMillis();

        long elapsed = (System.currentTimeMillis() - startTime);

        // System.out.println(elapsed + ": " + message);
        IJ.log(elapsed + ": " + message);
      } else {
        IJ.log(message);
      }
    }

  }

  /**
   * Prints debug message at level without newline
   *
   * @param level   the log level
   * @param message the message
   */
  public static void msgnonl(LogType level, String message) {

    if (level.ordinal() <= Log.logLevel.ordinal()) {
      if (Log.logLevel == LogType.VERBOSE) {
        // Get the class and line number information from the stck
        // 3 because we want to omit this method and the calling method
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        String fullClassName = stackTrace[3].getClassName();
        String methodName = stackTrace[3].getMethodName();
        int lineNumber = stackTrace[3].getLineNumber();
        message = fullClassName + ":" + methodName + ":" + lineNumber + " - " + message;
      }

      if (timeEnabled) {
        if (startTime == 0)
          startTime = System.currentTimeMillis();

        long elapsed = (System.currentTimeMillis() - startTime);

        IJ.log(elapsed + ": " + message);
      } else {
        IJ.log(message);
      }
    }
  }

  /**
   * Prints debug message at level with newline
   *
   * @param level   the log level
   * @param message the message
   */
  public static void msgNoTime(LogType level, String message) {

    if (level.ordinal() <= Log.logLevel.ordinal()) {
      if (Log.logLevel == LogType.VERBOSE) {
        // Get the class and line number information from the stck
        // 3 because we want to omit this method and the calling method
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        String fullClassName = stackTrace[3].getClassName();
        String methodName = stackTrace[3].getMethodName();
        int lineNumber = stackTrace[3].getLineNumber();
        message = fullClassName + ":" + methodName + ":" + lineNumber + " - " + message;
      }

      // IJ.log(message);
      System.out.println(message);
    }

  }

  /**
   * Prints debug message at level without newline
   *
   * @param level   the log level
   * @param message the message
   */
  public static void msgnonlNoTime(LogType level, String message) {

    if (level.ordinal() <= Log.logLevel.ordinal()) {
      if (Log.logLevel == LogType.VERBOSE) {
        // Get the class and line number information from the stck
        // 3 because we want to omit this method and the calling method
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        String fullClassName = stackTrace[3].getClassName();
        String methodName = stackTrace[3].getMethodName();
        int lineNumber = stackTrace[3].getLineNumber();
        message = fullClassName + ":" + methodName + ":" + lineNumber + " - " + message;
      }

      // IJ.log(message);
      System.out.print(message);
    }
  }

  /**
   * Gets the log level
   *
   * @return the log level
   */
  public static LogType getLogLevel() {
    return logLevel;
  }

}
