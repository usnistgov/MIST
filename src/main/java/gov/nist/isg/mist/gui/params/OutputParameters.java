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
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import gov.nist.isg.mist.gui.params.interfaces.StitchingAppParamFunctions;
import gov.nist.isg.mist.gui.params.utils.MacroUtils;
import gov.nist.isg.mist.gui.params.utils.PreferencesUtils;
import gov.nist.isg.mist.gui.params.utils.StitchingParamUtils;
import gov.nist.isg.mist.lib.export.LargeImageExporter.BlendingMode;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;

/**
 * OutputParameters are the output parameters for Stitching
 *
 * @author Tim Blattner
 */
public class OutputParameters implements StitchingAppParamFunctions {

  private static final String OUTPUT_PATH = "outputPath";
  private static final String DISPLAY_STITCHING = "displayStitching";
  private static final String OUTPUT_FULL_IMAGE = "outputFullImage";
  private static final String OUTPUT_META = "outputMeta";
  private static final String OUTPUT_IMG_PYRAMID = "outputImgPyramid";
  private static final String OUT_FILE_PREFIX = "outFilePrefix";
  private static final String BLENDING_MODE = "blendingMode";
  private static final String BLENDING_ALPHA = "blendingAlpha";



  public static final String metadataSuffix = ".txt";
  public static final String absPosFilename = "global-positions";
  private static final String relPosFilename = "relative-positions";
  private static final String relPosNoOptFilename = "relative-positions-no-optimization";
  private static final String fullImgFilename = "stitched";
  private static final String statisticsFilename = "statistics";
  private static final String logFilename = "log";
  private static final String hillClimbPosFilename = "hillclimb-starting-positions";

  private String outputPath;
  private boolean displayStitching;
  private boolean outputFullImage;
  private boolean outputMeta;
  private boolean outputImgPyramid;
  private String outFilePrefix;
  private BlendingMode blendingMode;
  private double blendingAlpha;


  public OutputParameters() {
    this.outputPath = System.getProperty("user.home");
    this.displayStitching = false;
    this.outputFullImage = false;
    this.outputImgPyramid = false;
    this.outputMeta = true;
    this.outFilePrefix = "img-";
    this.blendingMode = BlendingMode.OVERLAY;
    this.blendingAlpha = Double.NaN;
  }


  /**
   * Gets the output image name
   *
   * @param timeSlice the timeslice for the output image
   * @return the output image name
   */
  public String getOutputImageName(int timeSlice) {
    String fmt = "%d";
    return this.outFilePrefix + fullImgFilename + "-" + String.format(fmt, timeSlice) + ".tif";
  }

  /**
   * Gets the hill climbing starting position filename
   *
   * @param timeslice the timeslice
   * @return the hill climbing starting position name
   */
  public String getHillClimbStartingPositionsName(int timeslice) {
    String fmt = "%d";
    return this.outFilePrefix + hillClimbPosFilename + "-" + String.format(fmt, timeslice) + metadataSuffix;
  }

  /**
   * Gets the statistics filename
   */
  public String getStatFileName() {
    return this.outFilePrefix + statisticsFilename + metadataSuffix;
  }

  public String getLogFileName() {
    return this.outFilePrefix + logFilename + metadataSuffix;
  }

  /**
   * Gets the absolute positions file name at a given timeslice
   *
   * @param timeSlice the timeslice
   * @return the absolute positions filename
   */
  public String getAbsPosFileName(int timeSlice) {
    String fmt = "%d";
    return this.outFilePrefix + absPosFilename + "-" + String.format(fmt, timeSlice) + metadataSuffix;
  }

  /**
   * Gets the relative positions file name at a given timeslice
   *
   * @param timeSlice the timeslice
   * @return the relative positions filename
   */
  public String getRelPosFileName(int timeSlice) {
    String fmt = "%d";
    return this.outFilePrefix + relPosFilename + "-" + String.format(fmt, timeSlice) + metadataSuffix;
  }

  /**
   * Gets the relative positions before optimization file name at a given timeslice
   *
   * @param timeSlice the timeslice
   * @return the relative positions before optimization filename
   */
  public String getRelPosNoOptFileName(int timeSlice) {
    String fmt = "%d";
    return this.outFilePrefix + relPosNoOptFilename + "-" + String.format(fmt, timeSlice) + metadataSuffix;
  }

  /**
   * Gets the statistics file
   *
   * @return the static file
   */
  public File getStatsFile() {
    return new File(this.outputPath, this.getStatFileName());
  }

  public File getLogFile() {
    return new File(this.outputPath, this.getLogFileName());
  }


  /**
   * Gets the hill climb position file
   *
   * @param timeSlice the timeslice
   * @return the hill climb position file
   */
  public File getHillClimbPositionFile(int timeSlice) {
    return new File(this.outputPath, getHillClimbStartingPositionsName(timeSlice));
  }

  /**
   * Gets the output image file at a given time slice
   *
   * @param timeSlice the time slice
   * @return the output image file
   */
  public File getOutputImageFile(int timeSlice) {
    return new File(this.outputPath, this.getOutputImageName(timeSlice));
  }

  /**
   * Gets the absolute position file at a given time slice
   *
   * @param timeSlice the time slice
   * @return the absolute position file
   */
  public File getAbsPosFile(int timeSlice) {
    return new File(this.outputPath, getAbsPosFileName(timeSlice));
  }

  /**
   * Gets the relative position file at a given time slice
   *
   * @param timeSlice the time slice
   * @return the relative position file
   */
  public File getRelPosFile(int timeSlice) {
    return new File(this.outputPath, getRelPosFileName(timeSlice));
  }

  /**
   * Gets the relative position without optimization file at a given time slice
   *
   * @param timeSlice the time slice
   * @return the relative position without optimization file
   */
  public File getRelPosNoOptFile(int timeSlice) {
    return new File(this.outputPath, getRelPosNoOptFileName(timeSlice));
  }


  @Override
  public boolean checkParams() {
    return true;
  }


  @Override
  public boolean loadParams(File file) throws IllegalArgumentException {
    try {
      //Log.msg(LogType.MANDATORY, "Loading output parameters");
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
    if (key.equals(OUTPUT_PATH))
      this.outputPath = value;
    else if (key.equals(DISPLAY_STITCHING))
      this.displayStitching = StitchingParamUtils.loadBoolean(value, this.displayStitching);
    else if (key.equals(OUTPUT_FULL_IMAGE))
      this.outputFullImage = StitchingParamUtils.loadBoolean(value, this.outputFullImage);
    else if (key.equals(OUTPUT_META))
      this.outputMeta = StitchingParamUtils.loadBoolean(value, this.outputMeta);
    else if (key.equals(OUTPUT_IMG_PYRAMID))
      this.outputImgPyramid = StitchingParamUtils.loadBoolean(value, this.outputImgPyramid);
    else if (key.equals(BLENDING_MODE))
      this.blendingMode = BlendingMode.valueOf(value.toUpperCase());
    else if (key.equals(BLENDING_ALPHA))
      this.blendingAlpha = StitchingParamUtils.loadDouble(value, this.blendingAlpha);
    else if (key.equals(OUT_FILE_PREFIX))
      this.outFilePrefix = value;
  }


  @Override
  public boolean loadParams(Preferences pref) {

    this.outputPath = pref.get(OUTPUT_PATH, this.outputPath);
    this.displayStitching = pref.getBoolean(DISPLAY_STITCHING, this.displayStitching);
    this.outputFullImage = pref.getBoolean(OUTPUT_FULL_IMAGE, this.outputFullImage);
    this.outputMeta = pref.getBoolean(OUTPUT_META, this.outputMeta);
    this.outputImgPyramid = pref.getBoolean(OUTPUT_IMG_PYRAMID, this.outputImgPyramid);
    this.blendingMode = PreferencesUtils.loadPrefBlendingModeType(pref, BLENDING_MODE, this.blendingMode.name());
    this.blendingAlpha = pref.getDouble(BLENDING_ALPHA, this.blendingAlpha);
    this.outFilePrefix = pref.get(OUT_FILE_PREFIX, this.outFilePrefix);


    return true;
  }


  @Override
  public void printParams(LogType logLevel) {
    Log.msg(logLevel, OUTPUT_PATH + ": " + this.outputPath);
    Log.msg(logLevel, DISPLAY_STITCHING + ": " + this.displayStitching);
    Log.msg(logLevel, OUTPUT_FULL_IMAGE + ": " + this.outputFullImage);
    Log.msg(logLevel, OUTPUT_META + ": " + this.outputMeta);
    Log.msg(logLevel, OUTPUT_IMG_PYRAMID + ": " + this.outputImgPyramid);
    Log.msg(logLevel, BLENDING_MODE + ": " + this.blendingMode);
    Log.msg(logLevel, BLENDING_ALPHA + ": " + this.blendingAlpha);
    Log.msg(logLevel, OUT_FILE_PREFIX + ": " + this.outFilePrefix);
  }


  @Override
  public void loadMacro(String macroOptions) {
    this.outputPath = MacroUtils.loadMacroString(macroOptions, OUTPUT_PATH, this.outputPath);
    this.displayStitching = MacroUtils.loadMacroBoolean(macroOptions, DISPLAY_STITCHING, this.displayStitching);
    this.outputFullImage = MacroUtils.loadMacroBoolean(macroOptions, OUTPUT_FULL_IMAGE, this.outputFullImage);
    this.outputMeta = MacroUtils.loadMacroBoolean(macroOptions, OUTPUT_META, this.outputMeta);
    this.outputImgPyramid = MacroUtils.loadMacroBoolean(macroOptions, OUTPUT_IMG_PYRAMID, this.outputImgPyramid);
    this.blendingMode = MacroUtils.loadMacroBlendingModeType(macroOptions, BLENDING_MODE, this.blendingMode.name());
    this.blendingAlpha = MacroUtils.loadMacroDouble(macroOptions, BLENDING_ALPHA, this.blendingAlpha);
    this.outFilePrefix = MacroUtils.loadMacroString(macroOptions, OUT_FILE_PREFIX, this.outFilePrefix);
  }


  @Override
  public void recordMacro() {
    MacroUtils.recordString(OUTPUT_PATH + ": ", this.outputPath);
    MacroUtils.recordBoolean(DISPLAY_STITCHING + ": ", this.displayStitching);
    MacroUtils.recordBoolean(OUTPUT_FULL_IMAGE + ": ", this.outputFullImage);
    MacroUtils.recordBoolean(OUTPUT_META + ": ", this.outputMeta);
    MacroUtils.recordBoolean(OUTPUT_IMG_PYRAMID + ": ", this.outputImgPyramid);
    MacroUtils.recordString(BLENDING_MODE + ": ", this.blendingMode.name());
    MacroUtils.recordDouble(BLENDING_ALPHA + ": ", this.blendingAlpha);
    MacroUtils.recordString(OUT_FILE_PREFIX + ": ", this.outFilePrefix);
  }


  @Override
  public void saveParams(Preferences pref) {
    pref.put(OUTPUT_PATH, this.outputPath);
    pref.putBoolean(DISPLAY_STITCHING, this.displayStitching);
    pref.putBoolean(OUTPUT_FULL_IMAGE, this.outputFullImage);
    pref.putBoolean(OUTPUT_META, this.outputMeta);
    pref.putBoolean(OUTPUT_IMG_PYRAMID, this.outputImgPyramid);
    pref.put(BLENDING_MODE, this.blendingMode.name());
    pref.putDouble(BLENDING_ALPHA, this.blendingAlpha);
    pref.put(OUT_FILE_PREFIX, this.outFilePrefix);
  }


  @Override
  public boolean saveParams(FileWriter fw) {
    String newLine = "\n";
    try {

      fw.write(OUTPUT_PATH + ": " + this.outputPath + newLine);
      fw.write(DISPLAY_STITCHING + ": " + this.displayStitching + newLine);
      fw.write(OUTPUT_FULL_IMAGE + ": " + this.outputFullImage + newLine);
      fw.write(OUTPUT_META + ": " + this.outputMeta + newLine);
      fw.write(OUTPUT_IMG_PYRAMID + ": " + this.outputImgPyramid + newLine);
      fw.write(BLENDING_MODE + ": " + this.blendingMode.name() + newLine);
      fw.write(BLENDING_ALPHA + ": " + this.blendingAlpha + newLine);
      fw.write(OUT_FILE_PREFIX + ": " + this.outFilePrefix + newLine);

      return true;

    } catch (IOException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }
    return false;
  }


  /**
   * @return the outputPath
   */
  public String getOutputPath() {
    return this.outputPath;
  }


  /**
   * @param outputPath the outputPath to set
   */
  public void setOutputPath(String outputPath) {
    this.outputPath = outputPath;
  }


  /**
   * @return the displayStitching
   */
  public boolean isDisplayStitching() {
    return this.displayStitching;
  }


  /**
   * @param displayStitching the displayStitching to set
   */
  public void setDisplayStitching(boolean displayStitching) {
    this.displayStitching = displayStitching;
  }


  /**
   * @return the outputFullImage
   */
  public boolean isOutputFullImage() {
    return this.outputFullImage;
  }


  /**
   * @param outputFullImage the outputFullImage to set
   */
  public void setOutputFullImage(boolean outputFullImage) {
    this.outputFullImage = outputFullImage;
  }


  /**
   * @return the outputMeta
   */
  public boolean isOutputMeta() {
    return this.outputMeta;
  }


  /**
   * @param outputMeta the outputMeta to set
   */
  public void setOutputMeta(boolean outputMeta) {
    this.outputMeta = outputMeta;
  }


  /**
   * @return the outputImgPyramid
   */
  public boolean isOutputImgPyramid() {
    return this.outputImgPyramid;
  }


  /**
   * @param outputImgPyramid the outputImgPyramid to set
   */
  public void setOutputImgPyramid(boolean outputImgPyramid) {
    this.outputImgPyramid = outputImgPyramid;
  }


  /**
   * @return the outFilePrefix
   */
  public String getOutFilePrefix() {
    return this.outFilePrefix;
  }


  /**
   * @param outFilePrefix the outFilePrefix to set
   */
  public void setOutFilePrefix(String outFilePrefix) {
    this.outFilePrefix = outFilePrefix;
  }


  /**
   * @return the blendingMode
   */
  public BlendingMode getBlendingMode() {
    return this.blendingMode;
  }


  /**
   * @param blendingMode the blendingMode to set
   */
  public void setBlendingMode(BlendingMode blendingMode) {
    this.blendingMode = blendingMode;
  }


  /**
   * @return the blendingAlpha
   */
  public double getBlendingAlpha() {
    return this.blendingAlpha;
  }


  /**
   * @param blendingAlpha the blendingAlpha to set
   */
  public void setBlendingAlpha(double blendingAlpha) {
    this.blendingAlpha = blendingAlpha;
  }


  /**
   * Builds the list of output parameter names
   * 
   * @return the list of output parameter names
   */
  public static List<String> getParameterNamesList() {
  	List<String> parameterNames = new ArrayList<String>();
  	parameterNames.add(OUTPUT_PATH);
  	parameterNames.add(DISPLAY_STITCHING);
  	parameterNames.add(OUTPUT_FULL_IMAGE);
  	parameterNames.add(OUTPUT_META);
  	parameterNames.add(OUTPUT_IMG_PYRAMID);
  	parameterNames.add(OUT_FILE_PREFIX);
  	parameterNames.add(BLENDING_MODE);
  	parameterNames.add(BLENDING_ALPHA);
    return parameterNames;
  }

}
