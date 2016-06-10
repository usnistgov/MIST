// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Oct 1, 2014 1:56:58 PM EST
//
// Time-stamp: <Oct 1, 2014 1:56:58 PM tjb3>
//
// ================================================================
package gov.nist.isg.mist.gui.params;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.prefs.Preferences;

import gov.nist.isg.mist.gui.params.interfaces.StitchingAppParamFunctions;
import gov.nist.isg.mist.gui.params.utils.MacroUtils;
import gov.nist.isg.mist.gui.params.utils.PreferencesUtils;
import gov.nist.isg.mist.lib.log.Debug;
import gov.nist.isg.mist.lib.log.Debug.DebugType;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;

/**
 * OutputParameters are the output parameters for Stitching
 *
 * @author Tim Blattner
 */
public class LoggingParameters implements StitchingAppParamFunctions {

  private static final String LOG_LEVEL = "logLevel";
  private static final String DEBUG_LEVEL = "debugLevel";



  private LogType logLevel;
  private DebugType debugLevel;

  public LoggingParameters() {
    this.logLevel = LogType.MANDATORY;
    this.debugLevel = DebugType.NONE;
  }

  @Override
  public boolean checkParams() {
    return true;
  }


  @Override
  public boolean loadParams(File file) throws IllegalArgumentException {
    try {
      Log.msg(LogType.MANDATORY, "Loading output parameters");
      boolean noErrors = true;

      FileReader fr = new FileReader(file.getAbsolutePath());

      BufferedReader br = new BufferedReader(fr);


      String line = null;
      while ((line = br.readLine()) != null) {
        String[] contents = line.split(":", 2);

        if (contents.length > 1) {
          try {
            loadParameter(contents[0],contents[1]);

          } catch (IllegalArgumentException e) {
            Log.msg(LogType.MANDATORY, "Unable to parse line: " + line);
            Log.msg(LogType.MANDATORY, "Error parsing output option: " + e.getMessage());
            noErrors = false;
          }
        }
      }

      br.close();

      fr.close();

      return noErrors;

    } catch (IOException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }
    return false;
  }


  /**
   * Load the value into the parameter defined by key.
   * @param key the parameter name to overwrite with value
   * @param value the value to save into the parameter defined by key
   */
  public void loadParameter(String key, String value) {
    key = key.trim();
    value = value.trim();
    if (key.equals(LOG_LEVEL))
      this.logLevel = LogType.valueOf(value.toUpperCase());
    else if (key.equals(DEBUG_LEVEL))
      this.debugLevel = DebugType.valueOf(value.toUpperCase());
  }

  @Override
  public boolean loadParams(Preferences pref) {
    this.logLevel = PreferencesUtils.loadPrefLogType(pref, LOG_LEVEL, this.logLevel.name());
    Log.setLogLevel(this.logLevel);
    this.debugLevel = PreferencesUtils.loadPrefDebugType(pref, DEBUG_LEVEL, this.debugLevel.name());
    Debug.setDebugLevel(this.debugLevel);

    return true;
  }


  @Override
  public void printParams(LogType logLevel) {
    Log.msg(logLevel, LOG_LEVEL + ": " + this.logLevel);
    Log.msg(logLevel, DEBUG_LEVEL + ": " + this.debugLevel);
  }


  @Override
  public void loadMacro(String macroOptions) {
    this.logLevel = MacroUtils.loadMacroLogType(macroOptions, LOG_LEVEL, this.logLevel.name());
    Log.setLogLevel(this.logLevel);
    this.debugLevel = MacroUtils.loadMacroDebugType(macroOptions, DEBUG_LEVEL, this.debugLevel.name());
    Debug.setDebugLevel(this.debugLevel);
  }


  @Override
  public void recordMacro() {
    MacroUtils.recordString(LOG_LEVEL + ": ", this.logLevel.name());
    MacroUtils.recordString(DEBUG_LEVEL + ": ", this.debugLevel.name());
  }


  @Override
  public void saveParams(Preferences pref) {
    pref.put(LOG_LEVEL, this.logLevel.name());
    pref.put(DEBUG_LEVEL, this.debugLevel.name());
  }


  @Override
  public boolean saveParams(FileWriter fw) {
    String newLine = "\n";
    try {
      fw.write(LOG_LEVEL + ": " + this.logLevel.name() + newLine);
      fw.write(DEBUG_LEVEL + ": " + this.debugLevel.name() + newLine);

      return true;

    } catch (IOException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }
    return false;
  }


  /**
   * @return the logLevel
   */
  public LogType getLogLevel() {
    return this.logLevel;
  }


  /**
   * @param logLevel the logLevel to set
   */
  public void setLogLevel(LogType logLevel) {
    this.logLevel = logLevel;
  }


  /**
   * @return the debugLevel
   */
  public DebugType getDebugLevel() {
    return this.debugLevel;
  }


  /**
   * @param debugLevel the debugLevel to set
   */
  public void setDebugLevel(DebugType debugLevel) {
    this.debugLevel = debugLevel;
  }


  public static String getParametersCommandLineHelp() {
    String line = "\r\n";
    String str = "********* Logging Parameters *********";
    str += line;
    str += LOG_LEVEL + "=" + line;
    str += DEBUG_LEVEL + "=" + line;
    return str;
  }


}
