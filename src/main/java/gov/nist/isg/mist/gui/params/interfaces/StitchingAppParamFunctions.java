// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Oct 1, 2014 1:46:11 PM EST
//
// Time-stamp: <Oct 1, 2014 1:46:11 PM tjb3>
//
// ================================================================
package gov.nist.isg.mist.gui.params.interfaces;

import java.io.File;
import java.io.FileWriter;
import java.util.prefs.Preferences;

import gov.nist.isg.mist.lib.log.Log.LogType;

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
