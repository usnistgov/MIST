// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 1:11:28 PM EST
//
// Time-stamp: <Apr 18, 2014 1:11:28 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui.params;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import gov.nist.isg.mist.MISTMain;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import ij.plugin.frame.Recorder;

/**
 * StitchingAppParams is an object that holds parameters for the stitching application. This object
 * contains methods for loading/saving and checking arguments
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class StitchingAppParams {

  private InputParameters inputParams;
  private OutputParameters outputParams;
  private AdvancedParameters advancedParams;
  private LoggingParameters logParams;

  /**
   * Initialize default parameters
   */
  public StitchingAppParams() {
    this.inputParams = new InputParameters();
    this.outputParams = new OutputParameters();
    this.advancedParams = new AdvancedParameters();
    this.logParams = new LoggingParameters();
  }

  /**
   * Saves the parameters to a file
   *
   * @param file the file
   * @return true if the save was successful, otherwise false
   */
  public boolean saveParams(File file) {
    boolean ret = false;
    try {
      if (!file.exists())
        file.createNewFile();

      FileWriter fw = new FileWriter(file.getAbsolutePath());
      ret = saveParams(fw);
      fw.close();
      Log.msg(LogType.MANDATORY, "Saved Parameters to " + file.getAbsolutePath());

      return ret;

    } catch (IOException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }
    return ret;
  }

  /**
   * Saves the parameters to a filewriter
   *
   * @param fw the file writer
   * @return true if the save was successful, otherwise false
   */
  public boolean saveParams(FileWriter fw) {
    boolean ret = true;

    ret &= this.inputParams.saveParams(fw);
    ret &= this.outputParams.saveParams(fw);
    ret &= this.advancedParams.saveParams(fw);
    ret &= this.logParams.saveParams(fw);

    return ret;
  }

  /**
   * Loads parameters from preferences
   *
   * @param pref the preferencews
   * @return true, if the load was successful, otherwise false
   */
  public boolean loadParams(Preferences pref) {
    try {
      pref.sync();
    } catch (BackingStoreException e) {
      Log.msg(LogType.MANDATORY, "Error synchronizing preferences: " + e.getMessage());
    }

    boolean ret = true;

    ret &= this.inputParams.loadParams(pref);
    ret &= this.outputParams.loadParams(pref);
    ret &= this.advancedParams.loadParams(pref);
    ret &= this.logParams.loadParams(pref);

    return ret;
  }

  /**
   * Loads parameters from a file
   *
   * @param file the file to load parameters from
   * @return true if the load was successful, otherwise false
   */
  public boolean loadParams(File file) {
    boolean ret = true;

    if (!file.exists()) {
      Log.msg(LogType.MANDATORY,
          "Failed to load file (does not exist): " + file.getAbsolutePath());
      return false;
    }

    ret &= this.inputParams.loadParams(file);
    ret &= this.outputParams.loadParams(file);
    ret &= this.advancedParams.loadParams(file);
    ret &= this.logParams.loadParams(file);
    return ret;
  }

  /**
   * Prints parameters to the logger
   */
  public void printParams() {
    LogType logLevel = LogType.INFO;
    this.inputParams.printParams(logLevel);
    this.outputParams.printParams(logLevel);
    this.advancedParams.printParams(logLevel);
    this.logParams.printParams(logLevel);
  }

  /**
   * Checks if the parameters are valid
   *
   * @return true if the parameters are valid, otherwise false
   */
  public boolean checkParams() {
    return this.inputParams.checkParams() && this.outputParams.checkParams()
        && this.advancedParams.checkParams() && this.logParams.checkParams();
  }

  /**
   * @return the inputParams
   */
  public InputParameters getInputParams() {
    return this.inputParams;
  }

  /**
   * @return the outputParams
   */
  public OutputParameters getOutputParams() {
    return this.outputParams;
  }

  /**
   * @return the advancedParams
   */
  public AdvancedParameters getAdvancedParams() {
    return this.advancedParams;
  }

  /**
   * @return the logParams
   */
  public LoggingParameters getLogParams() {
    return this.logParams;
  }

  /**
   * Loads parameters from an ImageJ/Fiji Macro
   */
  public void loadMacro() {

    Log.msg(LogType.MANDATORY, "Loading macro parameters");

    String options = MISTMain.getMacroOptions();

    if (options == null)
      Log.msg(LogType.MANDATORY, "Error: no options present for macro");

    this.inputParams.loadMacro(options);
    this.outputParams.loadMacro(options);
    this.advancedParams.loadMacro(options);
    this.logParams.loadMacro(options);

    Log.msg(LogType.MANDATORY, "Finished loading macro");

  }

  /**
   * Records parameters to an ImageJ/Fiji macro
   */
  public void recordMacro() {
    Recorder.setCommand(MISTMain.recorderCommand);
    Log.msg(LogType.MANDATORY, "Recording macro");

    this.inputParams.recordMacro();
    this.outputParams.recordMacro();
    this.advancedParams.recordMacro();
    this.logParams.recordMacro();

    Recorder.saveCommand();
  }


  /**
   * Records the parameters into the preferences.
   *
   * @param pref the preferences to store the parameters.
   */
  public void saveParams(Preferences pref) {
    try {
      pref.clear();
    } catch (BackingStoreException e1) {
      Log.msg(LogType.MANDATORY, "Error unable clear preferences: " + e1.getMessage());
    }
    Log.msg(LogType.VERBOSE, "Recording user preferneces");

    this.inputParams.saveParams(pref);
    this.outputParams.saveParams(pref);
    this.advancedParams.saveParams(pref);
    this.logParams.saveParams(pref);

    try {
      pref.flush();
    } catch (BackingStoreException e) {
      Log.msg(LogType.MANDATORY, "Error unable to record preferences: " + e.getMessage());
    }
  }


}
