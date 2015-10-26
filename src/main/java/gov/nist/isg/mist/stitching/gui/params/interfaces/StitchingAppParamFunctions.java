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
// Date: Oct 1, 2014 1:46:11 PM EST
//
// Time-stamp: <Oct 1, 2014 1:46:11 PM tjb3>
//
// ================================================================
package gov.nist.isg.mist.stitching.gui.params.interfaces;

import gov.nist.isg.mist.stitching.lib.log.Log.LogType;

import java.io.File;
import java.io.FileWriter;
import java.util.prefs.Preferences;

/**
 * StitchingAppParamFunctions describes the stitching app param functions
 *
 * @author Tim Blattner
 */
public interface StitchingAppParamFunctions {

  /**
   * Checks parameters
   *
   * @return true if parameters check is successful, otherwise false
   */
  public boolean checkParams();

  /**
   * Loads parameters from a fileReader
   *
   * @param file the file to load params from
   * @return true if loading was successful, otherwise false
   */
  public boolean loadParams(File file);

  /**
   * Loads parameters from preferences
   *
   * @param preferences the preferences you wish to load
   * @return true if loading was successful, otherwise false
   */
  public boolean loadParams(Preferences preferences);

  /**
   * Prints parameters specifying a log level
   *
   * @param logLevel the log level to use for printing parameters
   */
  public void printParams(LogType logLevel);

  /**
   * Loads parameters from macro
   *
   * @param macroOptions the macro options to load
   */
  public void loadMacro(String macroOptions);

  /**
   * Records parameters into macro
   */
  public void recordMacro();

  /**
   * Saves parameters into preferences
   *
   * @param preferences the preferences to save
   */
  public void saveParams(Preferences preferences);

  /**
   * Saves parameters into file
   *
   * @param writer the file writer to save
   * @return true if the save was successful, otherwise false
   */
  public boolean saveParams(FileWriter writer);


}
