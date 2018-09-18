// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Oct 1, 2014 1:56:35 PM EST
//
// Time-stamp: <Oct 1, 2014 1:56:35 PM tjb3>
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
import gov.nist.isg.mist.gui.params.objects.RangeParam;
import gov.nist.isg.mist.gui.params.utils.MacroUtils;
import gov.nist.isg.mist.gui.params.utils.PreferencesUtils;
import gov.nist.isg.mist.gui.params.utils.StitchingParamUtils;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.tilegrid.loader.RowColTileGridLoader;
import gov.nist.isg.mist.lib.tilegrid.loader.SequentialTileGridLoader;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoader;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoader.GridDirection;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoader.GridOrigin;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoader.LoaderType;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoaderUtils;

/**
 * InputParameters are the input parameters for Stitching
 *
 * @author Tim Blattner
 */
public class InputParameters implements StitchingAppParamFunctions {

  private static final String GRID_WIDTH = "gridWidth";
  private static final String GRID_HEIGHT = "gridHeight";
  private static final String START_TILE = "startTile";
  private static final String START_TILE_ROW = "startTileRow";
  private static final String START_TILE_COL = "startTileCol";
  private static final String IMAGE_DIR = "imageDir";
  private static final String FILENAME_PATTERN = "filenamePattern";
  private static final String FILENAME_PATTERN_TYPE = "filenamePatternType";
  private static final String GRID_ORIGIN = "gridOrigin";
  private static final String NUMBERING_PATTERN = "numberingPattern";
  private static final String ASSEMBLE_FROM_META = "assembleFromMetadata";
  private static final String START_ROW = "startRow";
  private static final String START_COL = "startCol";
  private static final String EXTENT_WIDTH = "extentWidth";
  private static final String EXTENT_HEIGHT = "extentHeight";
  private static final String TIME_SLICES = "timeSlices";
  private static final String IS_TIME_SLICES_ENABLED = "isTimeSlicesEnabled";
  private static final String GLOBAL_POSITIONS_FILE = "globalPositionsFile";





  private int gridWidth;
  private int gridHeight;
  private int startTile;
  private int startTileRow;
  private int startTileCol;
  private String imageDir;
  private String filenamePattern;
  private LoaderType filenamePatternType;
  private GridOrigin origin;
  private GridDirection numberingPattern;
  private boolean assembleFromMetadata;

  private String globalPositionsFile;

  private int startRow;
  private int startCol;
  private int extentWidth;
  private int extentHeight;

  private List<RangeParam> timeSlices;
  private boolean isTimeSlicesEnabled;


  public InputParameters() {
    this.gridWidth = 1;
    this.gridHeight = 1;
    this.startTile = 0;
    this.startTileRow = 0;
    this.startTileCol = 0;
    this.imageDir = System.getProperty("user.home");
    this.filenamePattern = "img_r{rrr}_c{ccc}.tif";
    this.filenamePatternType = LoaderType.ROWCOL;
    this.origin = GridOrigin.UL;
    this.numberingPattern = GridDirection.HORIZONTALCOMBING;
    this.assembleFromMetadata = false;
    this.globalPositionsFile = "";

    // Processing Options
    this.startRow = 0;
    this.startCol = 0;
    this.extentWidth = 1;
    this.extentHeight = 1;

    // timing options
    this.timeSlices = new ArrayList<RangeParam>();
    this.isTimeSlicesEnabled = false;
  }


  /**
   * Parses a time slice pattern
   *
   * @param timeSlice the time slice
   * @param silent    whether to parse the time slice pattern silently or not
   * @return a String that replaces the time slice pattern with the time slice
   */
  public String parseTimeSlicePattern(int timeSlice, boolean silent) {
    return TileGridLoaderUtils.parseTimeSlicePattern(this.filenamePattern, timeSlice, silent);
  }

  /**
   * Generates the tile grid loader. Uses the timeslice to parse the file pattern
   *
   * @param timeSlice the timeslice
   * @return the tile grid loader or null if invalid file pattern loader type
   */
  public TileGridLoader getTileGridLoader(int timeSlice) {
    switch (this.filenamePatternType) {
      case ROWCOL:
        // Function will use startTile with a Sequential LoaderType and (startRow, startCol) with a ROWCOL LoaderType
        return new RowColTileGridLoader(this.getGridWidth(), this.getGridHeight(),
            this.getStartTile(), this.getStartTileRow(), this.getStartTileCol(), this.parseTimeSlicePattern(timeSlice, false), this.getOrigin());
      case SEQUENTIAL:
        // Function will use startTile with a Sequential LoaderType and (startRow, startCol) with a ROWCOL LoaderType
        return new SequentialTileGridLoader(this.getGridWidth(), this.getGridHeight(),
            this.getStartTile(), this.getStartTileRow(), this.getStartTileCol(), this.parseTimeSlicePattern(timeSlice, false), this.getOrigin(),
            this.getNumbering());
    }

    return null;
  }

  /**
   * Gets the tile grid loader.
   *
   * @return the tile grid loader or null if invalid file pattern loader type
   */
  public TileGridLoader getTileGridLoader() {

    switch (this.filenamePatternType) {
      case ROWCOL:
        // Function will use startTile with a Sequential LoaderType and (startRow, startCol) with a ROWCOL LoaderType
        return new RowColTileGridLoader(this.getGridWidth(), this.getGridHeight(),
            this.getStartTile(), this.getStartTileRow(), this.getStartTileCol(), this.getFilenamePattern(), this.getOrigin());
      case SEQUENTIAL:
        // Function will use startTile with a Sequential LoaderType and (startRow, startCol) with a ROWCOL LoaderType
        return new SequentialTileGridLoader(this.getGridWidth(), this.getGridHeight(),
            this.getStartTile(), this.getStartTileRow(), this.getStartTileCol(), this.getFilenamePattern(), this.getOrigin(), this.getNumbering());
    }

    return null;
  }


  /**
   * Gets the list of time slice range parameters, this will return a single time slice if no time
   * slices exist
   *
   * @return the list of time slice range parameters
   */
  public List<RangeParam> getTimeSlices() {
    if (this.timeSlices.size() == 0) {
      this.timeSlices.add(new RangeParam(1, 1));
    }

    return this.timeSlices;
  }


  @Override
  public boolean checkParams() {
    if (this.filenamePattern != null && this.imageDir != null && checkSubGrid()) {

      if (this.isTimeSlicesEnabled) {
        if (this.timeSlices.size() == 0)
          return false;
      }
      return true;
    }
    return false;
  }

  /**
   * Converts the timeslice list into a string
   *
   * @return the string form of the timeslice
   */
  public String getTimeSlicesStr() {
    String timeSliceStr = "";
    for (int i = 0; i < this.timeSlices.size(); i++) {
      if (i == this.timeSlices.size() - 1)
        timeSliceStr += this.timeSlices.get(i);
      else
        timeSliceStr += this.timeSlices.get(i) + ",";
    }

    return timeSliceStr;
  }

  private boolean checkSubGrid() {
    if (this.gridWidth < 0 || this.gridHeight < 0 || this.startRow < 0 || this.startCol < 0 || this.extentWidth < 0
        || this.extentHeight < 0) {
      Log.msg(LogType.MANDATORY, "Invalid grid/subgrid");
      return false;
    }

    if (this.extentWidth + this.startCol > this.gridWidth) {
      Log.msg(LogType.MANDATORY, "Extent Width + Start Col" + " <= Grid Width ("
          + this.extentWidth + "+" + this.startCol + "<=" + this.gridWidth + ")");
      return false;
    }

    if (this.extentHeight + this.startRow > this.gridHeight) {
      Log.msg(LogType.MANDATORY, "Extent Height + Start Row" + " <= Grid Height ("
          + this.extentHeight + "+" + this.startRow + "<=" + this.gridHeight + ")");
      return false;
    }

    return true;
  }

  @Override
  public boolean loadParams(File file) {
    try {
      boolean noErrors = true;

      //Log.msg(LogType.MANDATORY, "Loading input parameters");
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
            Log.msg(LogType.MANDATORY, "Error parsing input option: " + e.getMessage());
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
    if (key.equals(GRID_WIDTH))
      this.gridWidth = StitchingParamUtils.loadInteger(value, this.gridWidth);
    else if (key.equals(GRID_HEIGHT))
      this.gridHeight = StitchingParamUtils.loadInteger(value, this.gridHeight);
    else if (key.equals(START_TILE))
      this.startTile = StitchingParamUtils.loadInteger(value, this.startTile);
    else if (key.equals(START_TILE_ROW))
      this.startTileRow = StitchingParamUtils.loadInteger(value, this.startTileRow);
    else if (key.equals(START_TILE_COL))
      this.startTileCol = StitchingParamUtils.loadInteger(value, this.startTileCol);
    else if (key.equals(IMAGE_DIR))
      this.imageDir = value;
    else if (key.equals(FILENAME_PATTERN))
      this.filenamePattern = value;
    else if (key.equals(FILENAME_PATTERN_TYPE))
      this.filenamePatternType = LoaderType.valueOf(value.toUpperCase());
    else if (key.equals(GRID_ORIGIN))
      this.origin = GridOrigin.valueOf(value.toUpperCase());
    else if (key.equals(NUMBERING_PATTERN))
      this.numberingPattern = GridDirection.valueOf(value.toUpperCase());
    else if (key.equals(ASSEMBLE_FROM_META))
      this.assembleFromMetadata = StitchingParamUtils.loadBoolean(value, this.assembleFromMetadata);
    else if (key.equals(GLOBAL_POSITIONS_FILE))
      this.globalPositionsFile = value;
    else if (key.equals(START_ROW))
      this.startRow = StitchingParamUtils.loadInteger(value, this.startRow);
    else if (key.equals(START_COL))
      this.startCol = StitchingParamUtils.loadInteger(value, this.startCol);
    else if (key.equals(EXTENT_WIDTH))
      this.extentWidth = StitchingParamUtils.loadInteger(value, this.gridWidth);
    else if (key.equals(EXTENT_HEIGHT))
      this.extentHeight = StitchingParamUtils.loadInteger(value, this.gridHeight);
    else if (key.equals(TIME_SLICES))
      this.timeSlices = RangeParam.parseTimeSlices(value);
    else if (key.equals(IS_TIME_SLICES_ENABLED))
      this.isTimeSlicesEnabled = StitchingParamUtils.loadBoolean(value, this.isTimeSlicesEnabled);
  }


  @Override
  public boolean loadParams(Preferences pref) {
    this.gridWidth = pref.getInt(GRID_WIDTH, this.gridWidth);
    this.gridHeight = pref.getInt(GRID_HEIGHT, this.gridHeight);
    this.startTile = pref.getInt(START_TILE, this.startTile);
    this.startTileRow = pref.getInt(START_TILE_ROW, this.startTileRow);
    this.startTileCol = pref.getInt(START_TILE_COL, this.startTileCol);
    this.imageDir = pref.get(IMAGE_DIR, this.imageDir);
    this.filenamePattern = pref.get(FILENAME_PATTERN, this.filenamePattern);
    this.filenamePatternType =
        PreferencesUtils.loadFilePatternLoaderType(pref, FILENAME_PATTERN_TYPE, this.filenamePatternType.name());
    this.origin = PreferencesUtils.loadPrefGridOrigin(pref, GRID_ORIGIN, this.origin.name());
    this.numberingPattern = PreferencesUtils.loadPrefGridNumbering(pref, NUMBERING_PATTERN, this.numberingPattern.name());
    this.assembleFromMetadata = pref.getBoolean(ASSEMBLE_FROM_META, this.assembleFromMetadata);
    this.globalPositionsFile = pref.get(GLOBAL_POSITIONS_FILE, this.globalPositionsFile);
    this.startRow = pref.getInt(START_ROW, this.startRow);
    this.startCol = pref.getInt(START_COL, this.startCol);
    this.extentWidth = pref.getInt(EXTENT_WIDTH, this.extentWidth);
    this.extentHeight = pref.getInt(EXTENT_HEIGHT, this.extentHeight);
    this.timeSlices = PreferencesUtils.loadPrefTimeslices(pref, TIME_SLICES);
    this.isTimeSlicesEnabled = pref.getBoolean(IS_TIME_SLICES_ENABLED, this.isTimeSlicesEnabled);


    return true;
  }


  @Override
  public void printParams(LogType logLevel) {
    Log.msg(logLevel, GRID_WIDTH + ": " + this.gridWidth);
    Log.msg(logLevel, GRID_HEIGHT + ": " + this.gridHeight);
    if(this.getFilenamePatternLoaderType() == LoaderType.ROWCOL) {
      Log.msg(logLevel, START_TILE_ROW + ": " + this.startTileRow);
      Log.msg(logLevel, START_TILE_COL + ": " + this.startTileCol);
    }else{
      Log.msg(logLevel, START_TILE + ": " + this.startTile);
    }
    Log.msg(logLevel, IMAGE_DIR + ": " + this.imageDir);
    Log.msg(logLevel, FILENAME_PATTERN + ": " + this.filenamePattern);
    Log.msg(logLevel, FILENAME_PATTERN_TYPE + ": " + this.filenamePatternType);
    Log.msg(logLevel, GRID_ORIGIN + ": " + this.origin);
    Log.msg(logLevel, NUMBERING_PATTERN + ": " + this.numberingPattern);
    Log.msg(logLevel, ASSEMBLE_FROM_META + ": " + this.assembleFromMetadata);
    Log.msg(logLevel, GLOBAL_POSITIONS_FILE + ": " + this.globalPositionsFile);
    Log.msg(logLevel, START_ROW + ": " + this.startRow);
    Log.msg(logLevel, START_COL + ": " + this.startCol);
    Log.msg(logLevel, EXTENT_WIDTH + ": " + this.extentWidth);
    Log.msg(logLevel, EXTENT_HEIGHT + ": " + this.extentHeight);

    String timeSliceStr = "";
    for (int i = 0; i < this.timeSlices.size(); i++) {
      if (i == this.timeSlices.size() - 1)
        timeSliceStr += this.timeSlices.get(i);
      else
        timeSliceStr += this.timeSlices.get(i) + ",";
    }
    Log.msg(logLevel, TIME_SLICES + ": " + timeSliceStr);
    Log.msg(logLevel, IS_TIME_SLICES_ENABLED + ": " + this.isTimeSlicesEnabled);
  }


  @Override
  public void loadMacro(String macroOptions) {
    this.gridWidth = MacroUtils.loadMacroInteger(macroOptions, GRID_WIDTH, this.gridWidth);
    this.gridHeight = MacroUtils.loadMacroInteger(macroOptions, GRID_HEIGHT, this.gridHeight);
    this.startTileRow = MacroUtils.loadMacroInteger(macroOptions, START_TILE_ROW, this.startTileRow);
    this.startTileCol = MacroUtils.loadMacroInteger(macroOptions, START_TILE_COL, this.startTileCol);
    this.startTile = MacroUtils.loadMacroInteger(macroOptions, START_TILE, this.startTile);
    this.startTile = MacroUtils.loadMacroInteger(macroOptions, START_TILE, this.startTile);
    this.imageDir = MacroUtils.loadMacroString(macroOptions, IMAGE_DIR, this.imageDir);
    this.filenamePattern = MacroUtils.loadMacroString(macroOptions, FILENAME_PATTERN, this.filenamePattern);
    this.filenamePatternType =
        MacroUtils.loadMacroLoaderType(macroOptions, FILENAME_PATTERN_TYPE,
            this.filenamePatternType.name());
    this.origin = MacroUtils.loadMacroGridOrigin(macroOptions, GRID_ORIGIN, this.origin.name());
    this.numberingPattern = MacroUtils.loadMacroGridNumbering(macroOptions, "numberingPattern",
        this.numberingPattern.name());
    this.assembleFromMetadata = MacroUtils.loadMacroBoolean(macroOptions, ASSEMBLE_FROM_META,
        this.assembleFromMetadata);
    this.globalPositionsFile = MacroUtils.loadMacroString(macroOptions, GLOBAL_POSITIONS_FILE,
        this.globalPositionsFile);
    this.startRow = MacroUtils.loadMacroInteger(macroOptions, START_ROW, this.startRow);
    this.startCol = MacroUtils.loadMacroInteger(macroOptions, START_COL, this.startCol);
    this.extentWidth = MacroUtils.loadMacroInteger(macroOptions, EXTENT_WIDTH, this.extentWidth);
    this.extentHeight = MacroUtils.loadMacroInteger(macroOptions, EXTENT_HEIGHT, this.extentHeight);
    this.timeSlices = MacroUtils.loadMacroTimeslices(macroOptions, "timeSlices");
    this.isTimeSlicesEnabled = MacroUtils.loadMacroBoolean(macroOptions, IS_TIME_SLICES_ENABLED,
        this.isTimeSlicesEnabled);
  }


  @Override
  public void recordMacro() {
    MacroUtils.recordInteger(GRID_WIDTH + ": ", this.gridWidth);
    MacroUtils.recordInteger(GRID_HEIGHT + ": ", this.gridHeight);

    if(this.getFilenamePatternLoaderType() == LoaderType.ROWCOL) {
      MacroUtils.recordInteger(START_TILE_ROW + ": ", this.startTileRow);
      MacroUtils.recordInteger(START_TILE_COL + ": ", this.startTileCol);
    }else{
      MacroUtils.recordInteger(START_TILE + ": ", this.startTile);
    }

    MacroUtils.recordString(IMAGE_DIR + ": ", this.imageDir);
    MacroUtils.recordString(FILENAME_PATTERN + ": ", this.filenamePattern);
    MacroUtils.recordString(FILENAME_PATTERN_TYPE + ": ", this.filenamePatternType.name());
    MacroUtils.recordString(GRID_ORIGIN + ": ", this.origin.name());
    MacroUtils.recordBoolean(ASSEMBLE_FROM_META + ": ", this.assembleFromMetadata);
    MacroUtils.recordString(GLOBAL_POSITIONS_FILE + ": ", this.globalPositionsFile);
    MacroUtils.recordString(NUMBERING_PATTERN + ": ", this.numberingPattern.name());
    MacroUtils.recordInteger(START_ROW + ": ", this.startRow);
    MacroUtils.recordInteger(START_COL + ": ", this.startCol);
    MacroUtils.recordInteger(EXTENT_WIDTH + ": ", this.extentWidth);
    MacroUtils.recordInteger(EXTENT_HEIGHT + ": ", this.extentHeight);
    MacroUtils.recordTimeslices(this.timeSlices);
    MacroUtils.recordBoolean(IS_TIME_SLICES_ENABLED + ": ", this.isTimeSlicesEnabled);
  }


  @Override
  public void saveParams(Preferences pref) {
    pref.putInt(GRID_WIDTH, this.gridWidth);
    pref.putInt(GRID_HEIGHT, this.gridHeight);
    pref.putInt(START_TILE_ROW, this.startTileRow);
    pref.putInt(START_TILE_COL, this.startTileCol);
    pref.putInt(START_TILE, this.startTile);
    pref.put(IMAGE_DIR, this.imageDir);
    pref.put(FILENAME_PATTERN, this.filenamePattern);
    pref.put(FILENAME_PATTERN_TYPE, this.filenamePatternType.name());
    pref.put(GRID_ORIGIN, this.origin.name());
    pref.putBoolean(ASSEMBLE_FROM_META, this.assembleFromMetadata);
    pref.put(GLOBAL_POSITIONS_FILE, this.globalPositionsFile);
    pref.put(NUMBERING_PATTERN, this.numberingPattern.name());
    pref.putInt(START_ROW, this.startRow);
    pref.putInt(START_COL, this.startCol);
    pref.putInt(EXTENT_WIDTH, this.extentWidth);
    pref.putInt(EXTENT_HEIGHT, this.extentHeight);
    PreferencesUtils.recordPrefTimeslices(pref, this.timeSlices);
    pref.putBoolean(IS_TIME_SLICES_ENABLED, this.isTimeSlicesEnabled);

  }


  @Override
  public boolean saveParams(FileWriter fw) {
    String newLine = "\n";
    try {
      fw.write(GRID_WIDTH + ": " + this.gridWidth + newLine);
      fw.write(GRID_HEIGHT + ": " + this.gridHeight + newLine);

      if(this.getFilenamePatternLoaderType() == LoaderType.ROWCOL) {
        fw.write(START_TILE_ROW + ": " + this.startTileRow + newLine);
        fw.write(START_TILE_COL + ": " + this.startTileCol + newLine);
      }else{
        fw.write(START_TILE + ": " + this.startTile + newLine);
      }

      fw.write(IMAGE_DIR + ": " + this.imageDir + newLine);
      fw.write(FILENAME_PATTERN + ": " + this.filenamePattern + newLine);
      fw.write(FILENAME_PATTERN_TYPE + ": " + this.filenamePatternType.name() + newLine);
      fw.write(GRID_ORIGIN + ": " + this.origin.name() + newLine);
      fw.write(NUMBERING_PATTERN + ": " + this.numberingPattern.name() + newLine);
      fw.write(ASSEMBLE_FROM_META + ": " + this.assembleFromMetadata + newLine);
      fw.write(GLOBAL_POSITIONS_FILE + ": " + this.globalPositionsFile + newLine);
      fw.write(START_ROW + ": " + this.startRow + newLine);
      fw.write(START_COL + ": " + this.startCol + newLine);
      fw.write(EXTENT_WIDTH + ": " + this.extentWidth + newLine);
      fw.write(EXTENT_HEIGHT + ": " + this.extentHeight + newLine);

      String timeSliceStr = "";
      for (int i = 0; i < this.timeSlices.size(); i++) {
        if (i == this.timeSlices.size() - 1)
          timeSliceStr += this.timeSlices.get(i) + newLine;
        else
          timeSliceStr += this.timeSlices.get(i) + ",";
      }
      fw.write(TIME_SLICES + ": " + timeSliceStr);

      fw.write(IS_TIME_SLICES_ENABLED + ": " + this.isTimeSlicesEnabled + newLine);

      return true;

    } catch (IOException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }
    return false;
  }



  /**
   * @return the gridWidth
   */
  public int getGridWidth() {
    return this.gridWidth;
  }


  /**
   * @param gridWidth the gridWidth to set
   */
  public void setGridWidth(int gridWidth) {
    this.gridWidth = gridWidth;
  }


  /**
   * @return the gridHeight
   */
  public int getGridHeight() {
    return this.gridHeight;
  }


  /**
   * @param gridHeight the gridHeight to set
   */
  public void setGridHeight(int gridHeight) {
    this.gridHeight = gridHeight;
  }


  /**
   * @return the startTile
   */
  public int getStartTile() {
    return this.startTile;
  }

  /**
   * @param startTile the startTile to set
   */
  public void setStartTile(int startTile) {
    this.startTile = startTile;
  }

  /**
   * @return the startTileRow
   */
  public int getStartTileRow() {
    return this.startTileRow;
  }

  /**
   * @param startTileRow the startTileRow to set
   */
  public void setStartTileRow(int startTileRow) {
    this.startTileRow = startTileRow;
  }

  /**
   * @return the startTileCol
   */
  public int getStartTileCol() {
    return this.startTileCol;
  }

  /**
   * @param startTileCol the startTileCol to set
   */
  public void setStartTileCol(int startTileCol) {
    this.startTileCol = startTileCol;
  }




  /**
   * @return the imageDir
   */
  public String getImageDir() {
    return this.imageDir;
  }


  /**
   * @param imageDir the imageDir to set
   */
  public void setImageDir(String imageDir) {
    this.imageDir = imageDir;
  }


  /**
   * @return the filenamePattern
   */
  public String getFilenamePattern() {
    return this.filenamePattern;
  }


  /**
   * @param filenamePattern the filenamePattern to set
   */
  public void setFilenamePattern(String filenamePattern) {
    this.filenamePattern = filenamePattern;
  }


  /**
   * @return the filenamePatternLoaderType
   */
  public LoaderType getFilenamePatternLoaderType() {
    return this.filenamePatternType;
  }


  /**
   * @param filenamePatternLoaderType the filenamePatternLoaderType to set
   */
  public void setFilenamePatternLoaderType(LoaderType filenamePatternLoaderType) {
    this.filenamePatternType = filenamePatternLoaderType;
  }


  /**
   * @return the origin
   */
  public GridOrigin getOrigin() {
    return this.origin;
  }


  /**
   * @param origin the origin to set
   */
  public void setOrigin(GridOrigin origin) {
    this.origin = origin;
  }


  /**
   * @return the numbering
   */
  public GridDirection getNumbering() {
    return this.numberingPattern;
  }


  /**
   * @param numbering the numbering to set
   */
  public void setNumbering(GridDirection numbering) {
    this.numberingPattern = numbering;
  }


  /**
   * @return the assembleFromMetadata
   */
  public boolean isAssembleFromMetadata() {
    return this.assembleFromMetadata;
  }


  /**
   * @param assembleFromMetadata the assembleFromMetadata to set
   */
  public void setAssembleFromMetadata(boolean assembleFromMetadata) {
    this.assembleFromMetadata = assembleFromMetadata;
  }


  /**
   * @return the startRow
   */
  public int getStartRow() {
    return this.startRow;
  }


  /**
   * @param startRow the startRow to set
   */
  public void setStartRow(int startRow) {
    this.startRow = startRow;
  }


  /**
   * @return the startCol
   */
  public int getStartCol() {
    return this.startCol;
  }


  /**
   * @param startCol the startCol to set
   */
  public void setStartCol(int startCol) {
    this.startCol = startCol;
  }


  /**
   * @return the extentWidth
   */
  public int getExtentWidth() {
    return this.extentWidth;
  }


  /**
   * @param extentWidth the extentWidth to set
   */
  public void setExtentWidth(int extentWidth) {
    this.extentWidth = extentWidth;
  }


  /**
   * @return the extentHeight
   */
  public int getExtentHeight() {
    return this.extentHeight;
  }


  /**
   * @param extentHeight the extentHeight to set
   */
  public void setExtentHeight(int extentHeight) {
    this.extentHeight = extentHeight;
  }


  /**
   * @return the isTimeSlicesEnabled
   */
  public boolean isTimeSlicesEnabled() {
    return this.isTimeSlicesEnabled;
  }


  /**
   * @param isTimeSlicesEnabled the isTimeSlicesEnabled to set
   */
  public void setTimeSlicesEnabled(boolean isTimeSlicesEnabled) {
    this.isTimeSlicesEnabled = isTimeSlicesEnabled;
  }


  /**
   * @param timeSlices the timeSlices to set
   */
  public void setTimeSlices(List<RangeParam> timeSlices) {
    this.timeSlices = timeSlices;
  }

  /**
   * Gets the global position file
   *
   * @return the global position file
   */
  public String getGlobalPositionsFile() {
    return globalPositionsFile;
  }

  /**
   * @param globalPositionsFile the global position file to set
   */
  public void setGlobalPositionsFile(String globalPositionsFile) {
    this.globalPositionsFile = globalPositionsFile;
  }

  public static String getParametersCommandLineHelp() {
    String line = "\r\n";
    String str = "********* Input Parameters *********";
    str += line;
    str += GRID_WIDTH + "=" + line;
    str += GRID_HEIGHT + "=" + line;
    str += START_TILE + "=" + line;
    str += START_TILE_ROW + "=" + line;
    str += START_TILE_COL + "=" + line;
    str += IMAGE_DIR + "=" + line;
    str += FILENAME_PATTERN + "=" + line;
    str += FILENAME_PATTERN_TYPE + "=" + line;
    str += NUMBERING_PATTERN + "=" + line;
    str += ASSEMBLE_FROM_META + "=" + line;
    str += START_ROW + "=" + line;
    str += START_COL + "=" + line;
    str += EXTENT_WIDTH + "=" + line;
    str += EXTENT_HEIGHT + "=" + line;
    str += TIME_SLICES + "=" + line;
    str += IS_TIME_SLICES_ENABLED + "=" + line;
    str += GLOBAL_POSITIONS_FILE + "=" + line;
    return str;
  }

}
