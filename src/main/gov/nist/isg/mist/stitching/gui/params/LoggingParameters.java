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
// Date: Oct 1, 2014 1:56:58 PM EST
//
// Time-stamp: <Oct 1, 2014 1:56:58 PM tjb3>
//
// ================================================================
package main.gov.nist.isg.mist.stitching.gui.params;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.prefs.Preferences;

import main.gov.nist.isg.mist.stitching.gui.params.interfaces.StitchingAppParamFunctions;
import main.gov.nist.isg.mist.stitching.gui.params.utils.MacroUtils;
import main.gov.nist.isg.mist.stitching.gui.params.utils.PreferencesUtils;
import main.gov.nist.isg.mist.stitching.lib.log.Log;
import main.gov.nist.isg.mist.stitching.lib.log.Debug.DebugType;
import main.gov.nist.isg.mist.stitching.lib.log.Log.LogType;

/**
 * OutputParameters are the output parameters for Stitching
 * @author Tim Blattner
 *
 */
public class LoggingParameters implements StitchingAppParamFunctions {
  
  private static final String LOG_LEVEL = "logLevel";
  private static final String DEBUG_LEVEL = "debugLevel";
  
  private LogType logLevel;
  private DebugType debugLevel;
  
  public LoggingParameters()
  {
    this.logLevel = LogType.MANDATORY;
    this.debugLevel = DebugType.NONE;
  }

  @Override
  public boolean checkParams() {          
    return true;
  }


  @Override
  public boolean loadParams(File file) throws IllegalArgumentException{
    try
    {
      Log.msg(LogType.MANDATORY, "Loading output parameters");
      boolean noErrors = true;

      FileReader fr = new FileReader(file.getAbsolutePath());

      BufferedReader br = new BufferedReader(fr);


      String line = null;
      while ((line = br.readLine()) != null) {
        String[] contents = line.split(":", 2);

        if (contents.length > 1) {
          contents[0] = contents[0].trim();
          contents[1] = contents[1].trim();

          try
          {
            if (contents[0].equals(LOG_LEVEL))
              this.logLevel = LogType.valueOf(contents[1].toUpperCase());
            else if (contents[0].equals(DEBUG_LEVEL))
              this.debugLevel = DebugType.valueOf(contents[1].toUpperCase());
            
          } catch (IllegalArgumentException e)
          {
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


  @Override
  public boolean loadParams(Preferences pref) {
    this.logLevel = PreferencesUtils.loadPrefLogType(pref, LOG_LEVEL, this.logLevel.name());
    this.debugLevel = PreferencesUtils.loadPrefDebugType(pref, DEBUG_LEVEL, this.debugLevel.name());    

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
    this.debugLevel = MacroUtils.loadMacroDebugType(macroOptions, DEBUG_LEVEL, this.debugLevel.name());      
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
      fw.write(DEBUG_LEVEL + ": " + this.debugLevel.name()+ newLine);
      
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

  


}
