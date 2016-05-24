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
// Date: Oct 1, 2014 1:54:40 PM EST
//
// Time-stamp: <Oct 1, 2014 1:54:40 PM tjb3>
//
// ================================================================
package gov.nist.isg.mist.stitching.gui.params.utils;

import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;

/**
 * StitchingParamUtils are utility functions for stitching parameters
 *
 * @author Tim Blattner
 */
public class StitchingParamUtils {

  /**
   * Loads a double value from a string
   *
   * @param val the double value in string format
   * @param def the default value
   * @return the double value
   */
  public static double loadDouble(String val, double def) {
    try {
      return Double.parseDouble(val.trim());
    } catch (NumberFormatException e) {
      Log.msg(LogType.MANDATORY, "Error in loading double value: " + val + " using default: " + def);
      return def;
    }
  }

  /**
   * Loads a integer value from a string
   *
   * @param val the integer value in string format
   * @param def the default value
   * @return the integer value
   */
  public static int loadInteger(String val, int def) {
    try {
      return Integer.parseInt(val.trim());
    } catch (NumberFormatException e) {
      Log.msg(LogType.MANDATORY, "Error in loading integer value: " + val + " using default: "
          + def);
      return def;
    }
  }

  /**
   * Loads a boolean value from a string
   *
   * @param val the boolean value in string format
   * @param def the default value
   * @return the boolean value
   */
  public static boolean loadBoolean(String val, boolean def) {
    try {
      return Boolean.parseBoolean(val.trim());
    } catch (NumberFormatException e) {
      Log.msg(LogType.MANDATORY, "Error in loading boolean value: " + val + " using default: "
          + def);
      return def;
    }

  }
}
