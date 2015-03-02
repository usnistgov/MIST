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
// Date: Oct 1, 2014 1:56:35 PM EST
//
// Time-stamp: <Oct 1, 2014 1:56:35 PM tjb3>
//
// ================================================================
package gov.nist.isg.mist.stitching.gui.params;

import gov.nist.isg.mist.stitching.gui.params.interfaces.StitchingAppParamFunctions;
import gov.nist.isg.mist.stitching.gui.params.objects.RangeParam;
import gov.nist.isg.mist.stitching.gui.params.utils.MacroUtils;
import gov.nist.isg.mist.stitching.gui.params.utils.PreferencesUtils;
import gov.nist.isg.mist.stitching.gui.params.utils.StitchingParamUtils;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.RowColTileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.SequentialTileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridDirection;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridOrigin;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.LoaderType;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoaderUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * InputParameters are the input parameters for Stitching
 * @author Tim Blattner
 *
 */
public class InputParameters implements StitchingAppParamFunctions {
  
  private static final String GRID_WIDTH = "gridWidth";
  private static final String GRID_HEIGHT = "gridHeight";
  private static final String START_TILE = "startTile";
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
  
  
  private int gridWidth;
  private int gridHeight;
  private int startTile;
  private String imageDir;
  private String filenamePattern;
  private LoaderType filenamePatternType;
  private GridOrigin origin;
  private GridDirection numberingPattern;
  private boolean assembleFromMetadata;

  private int startRow;
  private int startCol;
  private int extentWidth;
  private int extentHeight;

  private List<RangeParam> timeSlices;
  private boolean isTimeSlicesEnabled;


  public InputParameters()
  {
    this.gridWidth = 1;
    this.gridHeight = 1;
    this.startTile = 0;
    this.imageDir = System.getProperty("user.home");
    this.filenamePattern = "tilename_{pppp}.tif";
    this.filenamePatternType = LoaderType.SEQUENTIAL;
    this.origin = GridOrigin.UR;
    this.numberingPattern = GridDirection.VERTICALCOMBING;
    this.assembleFromMetadata = false;

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
   * @param silent whether to parse the time slice pattern silently or not
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
        return new RowColTileGridLoader(this.gridWidth, this.gridHeight,
            this.startTile, this.parseTimeSlicePattern(timeSlice, false), this.origin);
      case SEQUENTIAL:
        return new SequentialTileGridLoader(this.gridWidth, this.gridHeight,
            this.startTile, this.parseTimeSlicePattern(timeSlice, false), this.origin,
            this.numberingPattern);
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
        return new RowColTileGridLoader(this.getGridWidth(), this.getGridHeight(),
            this.getStartTile(), this.getFilenamePattern(), this.getOrigin());
      case SEQUENTIAL:
        return new SequentialTileGridLoader(this.getGridWidth(), this.getGridHeight(),
            this.getStartTile(), this.getFilenamePattern(), this.getOrigin(), this.getNumbering());
    }

    return null;
  }

  /**
   * Gets the list of time slice range parameters, this will return
   * a single time slice if no time slices exist
   * 
   * @return the list of time slice range parameters
   */
  public List<RangeParam> getTimeSlices() {
    if (this.timeSlices.size() == 0)
    {
      this.timeSlices.add(new RangeParam(1, 1));
    }

    return this.timeSlices;
  }


  @Override
  public boolean checkParams() {
    if (this.filenamePattern != null && this.imageDir != null && checkSubGrid())
    {
      if (this.isTimeSlicesEnabled)
      {
        if (this.timeSlices.size() == 0)
          return false;

        int startTimeSlice = this.timeSlices.get(0).getMin();

        if (TileGridLoaderUtils.checkStartTile(this.imageDir, this.filenamePattern, this.startTile, startTimeSlice,
            this.filenamePatternType, false))
          return true;

      }
      else if (TileGridLoaderUtils.checkStartTile(this.imageDir, this.filenamePattern, this.startTile,
          this.filenamePatternType, false))
        return true;
      else 
        return false;



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
    try
    {
      boolean noErrors = true;

      Log.msg(LogType.MANDATORY, "Loading input parameters");
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
            if (contents[0].equals(GRID_WIDTH))
              this.gridWidth = StitchingParamUtils.loadInteger(contents[1], this.gridWidth);
            else if (contents[0].equals(GRID_HEIGHT))
              this.gridHeight = StitchingParamUtils.loadInteger(contents[1], this.gridHeight);
            else if (contents[0].equals(START_TILE))
              this.startTile = StitchingParamUtils.loadInteger(contents[1], this.startTile);
            else if (contents[0].equals(IMAGE_DIR))
              this.imageDir = contents[1];
            else if (contents[0].equals(FILENAME_PATTERN))
              this.filenamePattern = contents[1];
            else if (contents[0].equals(FILENAME_PATTERN_TYPE))
              this.filenamePatternType = LoaderType.valueOf(contents[1].toUpperCase());
            else if (contents[0].equals(GRID_ORIGIN))
              this.origin = GridOrigin.valueOf(contents[1].toUpperCase());
            else if (contents[0].equals(NUMBERING_PATTERN))              
              this.numberingPattern = GridDirection.valueOf(contents[1].toUpperCase());
            else if (contents[0].equals(ASSEMBLE_FROM_META))
              this.assembleFromMetadata = StitchingParamUtils.loadBoolean(contents[1], this.assembleFromMetadata);
            else if (contents[0].equals(START_ROW))
              this.startRow = StitchingParamUtils.loadInteger(contents[1], this.startRow);
            else if (contents[0].equals(START_COL))
              this.startCol = StitchingParamUtils.loadInteger(contents[1], this.startCol);
            else if (contents[0].equals(EXTENT_WIDTH))
              this.extentWidth = StitchingParamUtils.loadInteger(contents[1], this.gridWidth);
            else if (contents[0].equals(EXTENT_HEIGHT))
              this.extentHeight = StitchingParamUtils.loadInteger(contents[1], this.gridHeight);         
            else if (contents[0].equals(TIME_SLICES))
              this.timeSlices = RangeParam.parseTimeSlices(contents[1]);
            else if (contents[0].equals(IS_TIME_SLICES_ENABLED))
              this.isTimeSlicesEnabled = StitchingParamUtils.loadBoolean(contents[1], this.isTimeSlicesEnabled);
          } catch (IllegalArgumentException e)
          {
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


  @Override
  public boolean loadParams(Preferences pref) {
    this.gridWidth = pref.getInt(GRID_WIDTH, this.gridWidth);
    this.gridHeight = pref.getInt(GRID_HEIGHT, this.gridHeight);
    this.startTile = pref.getInt(START_TILE, this.startTile);
    this.imageDir = pref.get(IMAGE_DIR, this.imageDir);
    this.filenamePattern = pref.get(FILENAME_PATTERN, this.filenamePattern);
    this.filenamePatternType =
        PreferencesUtils.loadFilePatternLoaderType(pref, FILENAME_PATTERN_TYPE, this.filenamePatternType.name());
    this.origin = PreferencesUtils.loadPrefGridOrigin(pref, GRID_ORIGIN, this.origin.name());
    this.numberingPattern = PreferencesUtils.loadPrefGridNumbering(pref, NUMBERING_PATTERN, this.numberingPattern.name());
    this.assembleFromMetadata = pref.getBoolean(ASSEMBLE_FROM_META, this.assembleFromMetadata);
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
    Log.msg(logLevel, START_TILE + ": " + this.startTile);
    Log.msg(logLevel, IMAGE_DIR + ": " + this.imageDir);
    Log.msg(logLevel, FILENAME_PATTERN + ": " + this.filenamePattern);
    Log.msg(logLevel, FILENAME_PATTERN_TYPE + ": " + this.filenamePatternType);
    Log.msg(logLevel, GRID_ORIGIN + ": " + this.origin);
    Log.msg(logLevel, NUMBERING_PATTERN + ": " + this.numberingPattern);
    Log.msg(logLevel, ASSEMBLE_FROM_META + ": " + this.assembleFromMetadata);
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
    this.startTile = MacroUtils.loadMacroInteger(macroOptions, START_TILE, this.startTile);
    this.imageDir = MacroUtils.loadMacroString(macroOptions, IMAGE_DIR, this.imageDir);
    this.filenamePattern = MacroUtils.loadMacroString(macroOptions, FILENAME_PATTERN, this.filenamePattern);
    this.filenamePatternType =
        MacroUtils.loadMacroLoaderType(macroOptions, FILENAME_PATTERN_TYPE, this.filenamePatternType.name());
    this.origin = MacroUtils.loadMacroGridOrigin(macroOptions, GRID_ORIGIN, this.origin.name());
    this.numberingPattern = MacroUtils.loadMacroGridNumbering(macroOptions, "numberingPattern", this.numberingPattern.name());
    this.assembleFromMetadata = MacroUtils.loadMacroBoolean(macroOptions, ASSEMBLE_FROM_META, this.assembleFromMetadata);
    this.startRow = MacroUtils.loadMacroInteger(macroOptions, START_ROW, this.startRow);
    this.startCol = MacroUtils.loadMacroInteger(macroOptions, START_COL, this.startCol);
    this.extentWidth = MacroUtils.loadMacroInteger(macroOptions, EXTENT_WIDTH, this.extentWidth);
    this.extentHeight = MacroUtils.loadMacroInteger(macroOptions, EXTENT_HEIGHT, this.extentHeight);   
    this.timeSlices = MacroUtils.loadMacroTimeslices(macroOptions, "timeSlices");
    this.isTimeSlicesEnabled = MacroUtils.loadMacroBoolean(macroOptions, IS_TIME_SLICES_ENABLED, this.isTimeSlicesEnabled);    
  }


  @Override
  public void recordMacro() {
    MacroUtils.recordInteger(GRID_WIDTH + ": ", this.gridWidth);
    MacroUtils.recordInteger(GRID_HEIGHT + ": ", this.gridHeight);
    MacroUtils.recordInteger(START_TILE + ": ", this.startTile);
    MacroUtils.recordString(IMAGE_DIR + ": ", this.imageDir);
    MacroUtils.recordString(FILENAME_PATTERN + ": ", this.filenamePattern);
    MacroUtils.recordString(FILENAME_PATTERN_TYPE + ": ", this.filenamePatternType.name());
    MacroUtils.recordString(GRID_ORIGIN + ": ", this.origin.name());
    MacroUtils.recordBoolean(ASSEMBLE_FROM_META + ": ", this.assembleFromMetadata);
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
    pref.putInt(START_TILE, this.startTile);
    pref.put(IMAGE_DIR, this.imageDir);
    pref.put(FILENAME_PATTERN, this.filenamePattern);
    pref.put(FILENAME_PATTERN_TYPE, this.filenamePatternType.name());
    pref.put(GRID_ORIGIN, this.origin.name());
    pref.putBoolean(ASSEMBLE_FROM_META, this.assembleFromMetadata);
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
      fw.write(START_TILE + ": " + this.startTile + newLine);
      fw.write(IMAGE_DIR + ": " + this.imageDir + newLine);
      fw.write(FILENAME_PATTERN + ": " + this.filenamePattern + newLine);
      fw.write(FILENAME_PATTERN_TYPE + ": " + this.filenamePatternType.name() + newLine);
      fw.write(GRID_ORIGIN + ": " + this.origin.name() + newLine);
      fw.write(NUMBERING_PATTERN + ": " + this.numberingPattern.name() + newLine);
      fw.write(ASSEMBLE_FROM_META + ": " + this.assembleFromMetadata + newLine);
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


}
