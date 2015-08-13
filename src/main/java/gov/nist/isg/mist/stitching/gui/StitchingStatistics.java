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
// Date: Apr 18, 2014 1:49:51 PM EST
//
// Time-stamp: <Apr 18, 2014 1:49:51 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.gui;

import gov.nist.isg.mist.stitching.gui.executor.StitchingExecutor.StitchingType;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.gui.params.objects.CudaDeviceParam;
import gov.nist.isg.mist.stitching.gui.params.objects.RangeParam;
import gov.nist.isg.mist.stitching.lib.imagetile.utilfns.UtilFnsStitching;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.optimization.OptimizationRepeatability;
import gov.nist.isg.mist.stitching.lib.optimization.OptimizationUtils.Direction;
import ij.IJ;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Creates an object that manages statistics for a stitching execution. Execution times, parameters
 * used, and observed results are stored (such as computed repeatability and overlap)
 * 
 * @author Tim Blattner
 * @version 1.0
 */
public class StitchingStatistics {

  private static final double MISSING_ROW_COL_WARNING_THRESHOLD = 75.0; // the percentage missing rows or columns required to generate Error Report warning

  /**
   * The version number for the stitching statistics
   */
  public static final double VERSION = 1.0;
  private int currentTimeslice;

  public int getCurrentTimeslice() {
    return currentTimeslice;
  }

  /**
   * Enum representing what error report status
   */
  public enum ErrorReportStatus {
    PASSED,
    WARNING,
    FAILED
  }

  /**
   * Enum represented the different runtimes observed
   * 
   * @author Tim Blattner
   * @version 1.0
   */
  public enum RunTimers {

    /**
     * The total stitching time timer
     */
    TotalStitchingTime("Total Stitching Time"),

    /**
     * The relative displacement timer
     */
    RelativeDisplacementTime("Relative Displacement Time"),

    /**
     * The global optimization timer
     */
    GlobalOptimizationTime("Global Optimization Time"),

    /**
     * The global position timer
     */
    GlobalPositionTime("Global Position Time"),

    /**
     * The output full image tile timer
     */
    OutputFullImageTileTime("Output Full Image Time"),

    /**
     * The output image pyramid timer
     */
    OutputImgPyramidTime("Output Image Pyramid Time");
    

    private String key;

    private RunTimers(String key) {
      this.key = key;
    }

    @Override
    public String toString() {
      return this.key;
    }

  }

  // Global variables across all time-slices
  private String name;
  private StitchingAppParams runParams;
  private int currentTimeSlice;

  // Used to distinguish between what actual mode was used when AUTO was
  // selected
  private StitchingType executionType;

  private long startTimeForEverything;
  private long endTimeforEverything;

  // Variables across each time-slice
  private List<Boolean> isRunSequential;
  private List<HashMap<String, Long>> startTimers;
  private List<HashMap<String, Long>> endTimers;
  private List<HashMap<Direction, Integer>> repeatabilities;
  private List<HashMap<Direction, Double>> overlaps;
  private List<HashMap<Direction, Integer>> numValidTilesAfterFilters;
  private List<HashMap<Direction, Double>> minFilterThresholds;
  private List<HashMap<Direction, Double>> maxFilterThresholds;
  private List<HashMap<Direction, Double>> stdDevThresholds;
  private List<HashMap<Direction, List<Integer>>> emptyRowCols;
  private List<HashMap<Direction, Integer>> numRowCols;
  private List<Integer> timeSlicesRun;
  private int maxTimeSlice;

  private List<ErrorReportStatus> errorReportStatus;
  private List<HashMap<Direction, Boolean>> hasNoValidTranslations;
  private List<HashMap<Direction, Double>> computedOverlaps;
  private List<HashMap<Direction, Boolean>> hasHighRepeatability;
  private List<HashMap<Direction, Boolean>> hasHighPercentMissingRowCol;




  /**
   * Initializes the stitching statistics
   * 
   * @param runParams the run-time parameters
   */
  public StitchingStatistics(StitchingAppParams runParams) {
    this("Stitching Statistics for " + runParams.getOutputParams().getOutFilePrefix(), runParams);
  }

  /**
   * Initializes the stitching statistics with a name
   * 
   * @param name the name given for these statistics
   * @param runParams the run-time parameters
   */
  public StitchingStatistics(String name, StitchingAppParams runParams) {
    this.name = name;
    this.runParams = runParams;

    this.isRunSequential = new ArrayList<Boolean>();
    this.startTimers = new ArrayList<HashMap<String, Long>>();
    this.endTimers = new ArrayList<HashMap<String, Long>>();
    this.repeatabilities = new ArrayList<HashMap<Direction, Integer>>();
    this.overlaps = new ArrayList<HashMap<Direction, Double>>();
    this.numValidTilesAfterFilters = new ArrayList<HashMap<Direction, Integer>>();
    this.minFilterThresholds = new ArrayList<HashMap<Direction, Double>>();
    this.maxFilterThresholds = new ArrayList<HashMap<Direction, Double>>();
    this.stdDevThresholds = new ArrayList<HashMap<Direction, Double>>();
    this.emptyRowCols = new ArrayList<HashMap<Direction, List<Integer>>>();
    this.numRowCols = new ArrayList<HashMap<Direction, Integer>>();
    this.timeSlicesRun = new ArrayList<Integer>();

    this.errorReportStatus = new ArrayList<ErrorReportStatus>();
    this.hasNoValidTranslations = new ArrayList<HashMap<Direction, Boolean>>();
    this.computedOverlaps = new ArrayList<HashMap<Direction, Double>>();
    this.hasHighRepeatability = new ArrayList<HashMap<Direction, Boolean>>();
    this.hasHighPercentMissingRowCol = new ArrayList<HashMap<Direction, Boolean>>();
    this.currentTimeSlice = 0;

    // Initialize HashMaps for all time slices
    int numTimeSlices = 0;

    if (this.runParams.getInputParams().isTimeSlicesEnabled()) {
      // Find largest time slice
      this.maxTimeSlice = Integer.MIN_VALUE;
      List<RangeParam> timeSlices = this.runParams.getInputParams().getTimeSlices();
      for (RangeParam timeSlice : timeSlices) {
        if (this.maxTimeSlice < timeSlice.getMax())
          this.maxTimeSlice = timeSlice.getMax();
      }
    } else {
      this.maxTimeSlice = 1;
    }

    numTimeSlices = this.maxTimeSlice;

    for (int timeslice = 0; timeslice <= numTimeSlices; timeslice++) {
      HashMap<String, Long> startTimer = new HashMap<String, Long>();
      HashMap<String, Long> endTimer = new HashMap<String, Long>();
      HashMap<Direction, Integer> repeatability = new HashMap<Direction, Integer>();
      HashMap<Direction, Double> overlap = new HashMap<Direction, Double>();
      HashMap<Direction, Integer> numValidTilesAfterFilter = new HashMap<Direction, Integer>();
      HashMap<Direction, Double> minFilterThreshold = new HashMap<Direction, Double>();
      HashMap<Direction, Double> maxFilterThreshold = new HashMap<Direction, Double>();
      HashMap<Direction, Double> stdDevThreshold = new HashMap<Direction, Double>();
      HashMap<Direction, List<Integer>> emptyRowCol = new HashMap<Direction, List<Integer>>();
      HashMap<Direction, Integer> numRowCol = new HashMap<Direction, Integer>();
      HashMap<Direction, Boolean> noValidTranslations = new HashMap<Direction, Boolean>();
      HashMap<Direction, Double> computedOverlap  = new HashMap<Direction, Double>();
      HashMap<Direction, Boolean> highRepeatability = new HashMap<Direction, Boolean>();
      HashMap<Direction, Boolean> highPercMissRowCol = new HashMap<Direction, Boolean>();

      this.isRunSequential.add(false);
      this.errorReportStatus.add(ErrorReportStatus.PASSED);
      this.startTimers.add(startTimer);
      this.endTimers.add(endTimer);
      this.repeatabilities.add(repeatability);
      this.overlaps.add(overlap);
      this.numValidTilesAfterFilters.add(numValidTilesAfterFilter);
      this.minFilterThresholds.add(minFilterThreshold);
      this.maxFilterThresholds.add(maxFilterThreshold);
      this.stdDevThresholds.add(stdDevThreshold);
      this.emptyRowCols.add(emptyRowCol);
      this.numRowCols.add(numRowCol);
      this.hasNoValidTranslations.add(noValidTranslations);
      this.computedOverlaps.add(computedOverlap);
      this.hasHighRepeatability.add(highRepeatability);
      this.hasHighPercentMissingRowCol.add(highPercMissRowCol);
    }
  }

  /**
   * Sets the current time slice during execution
   * 
   * @param timeslice the current time slice
   */
  public void setCurrentTimeSlice(int timeslice) {
    currentTimeSlice = timeslice;
  }


  /**
   * Adds timeslice to these statistics
   * 
   * @param timeSlice
   */
  public void addTimeSlice(int timeSlice) {
    this.timeSlicesRun.add(timeSlice);
  }

  /**
   * Sets the execution type for the execution
   * 
   * @param type the type of execution
   */
  public void setExecutionType(StitchingType type) {
    this.executionType = type;
  }

  /**
   * Starts the timer that measures end-to-end time
   */
  public void startEndToEndTimer() {
    this.startTimeForEverything = System.currentTimeMillis();
  }

  /**
   * Stops the timer that measures end-to-end time
   */
  public void stopEndToEndTimer() {
    this.endTimeforEverything = System.currentTimeMillis();
  }

  /**
   * Gets the end-to-end timer duration
   * 
   * @return the end-to-end timer duration
   */
  public long getEndToEndDuration() {
    return this.endTimeforEverything - this.startTimeForEverything;
  }

  /**
   * Starts a given timer
   * 
   * @param timer the timer to start
   */
  public void startTimer(RunTimers timer) {
    HashMap<String, Long> startTimerMap = this.startTimers.get(currentTimeSlice);
    startTimerMap.put(timer.name(), System.currentTimeMillis());
  }

  /**
   * Stops a given timer
   * 
   * @param timer the timer to stop
   */
  public void stopTimer(RunTimers timer) {
    HashMap<String, Long> endTimerMap = this.endTimers.get(currentTimeSlice);
    endTimerMap.put(timer.name(), System.currentTimeMillis());
  }

  /**
   * Sets the repeatability for a direction
   * 
   * @param dir the direction
   * @param repeatability the repeatability
   */
  public void setRepeatability(Direction dir, int repeatability) {
    HashMap<Direction, Integer> repeatabilityMap = this.repeatabilities.get(currentTimeSlice);
    repeatabilityMap.put(dir, repeatability);
  }

  /**
   * Sets the overlap for a direction
   * 
   * @param dir the direction
   * @param overlap the overlap
   */
  public void setOverlap(Direction dir, double overlap) {
    HashMap<Direction, Double> overlapMap = this.overlaps.get(currentTimeSlice);
    overlapMap.put(dir, overlap);
  }

  /**
   * Sets whether the stitching experiment is running the sequential version or not
   * for the current timeslice
   * @param val true if running sequential, otherwise false
   */
  public void setIsRunSequential(boolean val) {
    this.isRunSequential.set(currentTimeSlice, val);
  }

  /**
   * Sets the computed overlap for a direction
   *
   * @param dir the direction
   * @param overlap the overlap
   */
  public void setComputedOverlap(Direction dir, double overlap) {
    HashMap<Direction, Double> computedOverlapMap = this.computedOverlaps.get(currentTimeSlice);
    computedOverlapMap.put(dir, overlap);
  }

  /**
   * Sets the number of valid tiles after filtering for a direction
   * 
   * @param dir the direction
   * @param numTiles the number of valid tiles
   */
  public void setNumValidTilesAfterFilter(Direction dir, int numTiles) {
    HashMap<Direction, Integer> numTilesAfterFilterMap =
        this.numValidTilesAfterFilters.get(currentTimeSlice);
    numTilesAfterFilterMap.put(dir, numTiles);
  }

  /**
   * Sets the minimum filter threshold for a direction
   * 
   * @param dir the direction
   * @param threshold the threshold
   */
  public void setMinFilterThreshold(Direction dir, double threshold) {
    HashMap<Direction, Double> thresholdMap = this.minFilterThresholds.get(currentTimeSlice);
    thresholdMap.put(dir, threshold);
  }

  /**
   * Sets the maximum filter threshold for a direction
   * 
   * @param dir the direction
   * @param threshold the threshold
   */
  public void setMaxFilterThreshold(Direction dir, double threshold) {
    HashMap<Direction, Double> thresholdMap = this.maxFilterThresholds.get(currentTimeSlice);
    thresholdMap.put(dir, threshold);
  }

  /**
   * Sets the standard deviation filter threshold for a direction
   * 
   * @param dir the direction
   * @param threshold the threshold
   */
  public void setStdDevThreshold(Direction dir, double threshold) {
    HashMap<Direction, Double> thresholdMap = this.stdDevThresholds.get(currentTimeSlice);
    thresholdMap.put(dir, threshold);
  }

  /**
   * Sets the list of empty rows/cols for a direction
   * 
   * @param dir the direction
   * @param emptyRowCol the list of empty rows/cols
   */
  public void setEmptyRowsCols(Direction dir, List<Integer> emptyRowCol) {
    HashMap<Direction, List<Integer>> thresholdMap = this.emptyRowCols.get(currentTimeSlice);
    thresholdMap.put(dir, emptyRowCol);
  }

  /**
   * Sets the number of rows/cols for a direction
   *
   * @param dir the direction
   * @param numRowCol the number of rows/cols
   */
  public void setNumRowsCols(Direction dir, int numRowCol) {
    HashMap<Direction, Integer> numRowCols = this.numRowCols.get(currentTimeSlice);
    numRowCols.put(dir, numRowCol);
  }

  /**
   * Checks to see if a timer has computed its duration
   * 
   * @param timer the time to check
   * @return true if the timer has a duration, otherwise false
   */
  public boolean hasDuration(RunTimers timer) {
    return hasDuration(timer, currentTimeSlice);
  }

  /**
   * Gets the duration for a timer
   * 
   * @param timer the timer to get the duration for
   * @return the duration
   */
  public long getDuration(RunTimers timer) {
    return getDuration(timer, currentTimeSlice);
  }

  /**
   * Checks if a timer has a duration for a time slice
   * 
   * @param timer the timer to check
   * @param timeSlice the time slice to check
   * @return the duration for a timer for a given time slice
   */
  public boolean hasDuration(RunTimers timer, int timeSlice) {
    HashMap<String, Long> startMap = this.startTimers.get(timeSlice);
    HashMap<String, Long> endMap = this.endTimers.get(timeSlice);

    if (startMap.containsKey(timer.name()) && endMap.containsKey(timer.name()))
      return true;
    return false;
  }

  /**
   * Gets the duration for a timer at a given time slice
   * 
   * @param timer the timer to get the duration for
   * @param timeSlice the time slice
   * @return the duration for the timer
   */
  public long getDuration(RunTimers timer, int timeSlice) {
    HashMap<String, Long> startMap = this.startTimers.get(timeSlice);
    HashMap<String, Long> endMap = this.endTimers.get(timeSlice);

    if (startMap.containsKey(timer.name()) && endMap.containsKey(timer.name()))
      return endMap.get(timer.name()) - startMap.get(timer.name());
    return 0L;
  }

  /**
   * Gets the repeatability for a direction
   * 
   * @param dir the direction
   * @return the repeatability
   */
  public int getRepeatability(Direction dir) {
    return getRepeatability(dir, currentTimeSlice);
  }

  /**
   * Checks if a direction has repeatability
   * 
   * @param dir the direction
   * @return true if the direction has a repeatability
   */
  public boolean hasRepeatability(Direction dir) {
    return hasRepeatability(dir, currentTimeSlice);
  }

  /**
   * Gets the repeatability for a direction at a time slice
   * 
   * @param dir the direction
   * @param timeSlice the time slice
   * @return the repeatability
   */
  public int getRepeatability(Direction dir, int timeSlice) {
    HashMap<Direction, Integer> repeatabilityMap = this.repeatabilities.get(timeSlice);

    if (repeatabilityMap.containsKey(dir))
      return repeatabilityMap.get(dir);
    return -1;
  }

  /**
   * Checks if a direction has a repeatability at a given direction
   * 
   * @param dir the direction
   * @param timeSlice the time slice
   * @return the repeatability
   */
  public boolean hasRepeatability(Direction dir, int timeSlice) {
    HashMap<Direction, Integer> repeatabilityMap = this.repeatabilities.get(timeSlice);
    return repeatabilityMap.containsKey(dir);
  }

  /**
   * Gets the overlap for a direction
   * 
   * @param dir the direction
   * @return the overlap
   */
  public double getOverlap(Direction dir) {
    return getOverlap(dir, currentTimeSlice);
  }


  /**
   * Gets whether a timeslice is running sequential or not
   * @param timeslice the timeslice
   * @return true if running sequential, otherwise false
   */
  public boolean isRunSequential(int timeslice)
  {
    return this.isRunSequential.get(timeslice);
  }

  /**
   * Checks if a direction has an overlap
   * 
   * @param dir the direction
   * @return true if it has an overlap
   */
  public boolean hasOverlap(Direction dir) {
    return hasOverlap(dir, currentTimeSlice);
  }

  /**
   * Gets the overlap for a direction and timeslice
   * 
   * @param dir the direction
   * @param timeSlice the time slice
   * @return the overlap or -1 if it does not exist
   */
  public double getOverlap(Direction dir, int timeSlice) {
    HashMap<Direction, Double> overlapMap = this.overlaps.get(timeSlice);

    if (overlapMap.containsKey(dir))
      return overlapMap.get(dir);
    return -1;
  }

  /**
   * Gets the computed overlap for a direction and timeslice
   *
   * @param dir the direction
   * @param timeSlice the time slice
   * @return the overlap or -1 if it does not exist
   */
  public double getComputedOverlap(Direction dir, int timeSlice) {
    HashMap<Direction, Double> overlapMap = this.computedOverlaps.get(timeSlice);

    if (overlapMap.containsKey(dir))
      return overlapMap.get(dir);
    return -1;
  }

  /**
   * Checks if overlap exists for a direction at a particular time slice
   * 
   * @param dir the direction
   * @param timeSlice the time slice
   * @return true if the overlap exists
   */
  public boolean hasOverlap(Direction dir, int timeSlice) {
    HashMap<Direction, Double> overlapMap = this.overlaps.get(timeSlice);
    return overlapMap.containsKey(dir);
  }

  /**
   * Gets the number of valid tiles after filtering for a direction
   * 
   * @param dir the direction
   * @return the number of valid tiles after filtering
   */
  public int getNumValidTilesAfterFilter(Direction dir) {
    return getNumValidTilesAfterFilter(dir, currentTimeSlice);
  }

  /**
   * Checks if a direction has valid tiles after filtering
   * 
   * @param dir the direction
   * @return true if valid tiles exists
   */
  public boolean hasNumValidTilesAfterFilter(Direction dir) {
    return hasNumValidTilesAfterFilter(dir, currentTimeSlice);
  }

  /**
   * Gets the number of valid tiles after filter for a direction and time slice
   * 
   * @param dir the direction
   * @param timeSlice the time slice
   * @return the number of valid tiles after filtering or -1 if none
   */
  public int getNumValidTilesAfterFilter(Direction dir, int timeSlice) {
    HashMap<Direction, Integer> numValidTilesAfterFilter = this.numValidTilesAfterFilters.get(timeSlice);

    if (numValidTilesAfterFilter.containsKey(dir))
      return numValidTilesAfterFilter.get(dir);
    return -1;
  }

  /**
   * Checks if the number of valid tiles after filtering exists for a direction and time slice
   * 
   * @param dir the direction
   * @param timeSlice the time slice
   * @return true if a direction has valid tiles after filter, otherwise false
   */
  public boolean hasNumValidTilesAfterFilter(Direction dir, int timeSlice) {
    HashMap<Direction, Integer> numValidTilesAfterFilter = this.numValidTilesAfterFilters.get(timeSlice);
    return numValidTilesAfterFilter.containsKey(dir);
  }

  /**
   * Gets the minimum filter threshold for a direction
   * 
   * @param dir the direction
   * @return the minimum filter threshold
   */
  public double getMinFilterThreshold(Direction dir) {
    return getMinFilterThreshold(dir, currentTimeSlice);
  }

  /**
   * Checks if the minimum filter theshold exists for a direction
   * 
   * @param dir the direction
   * @return true if the minimum filter threshold exists
   */
  public boolean hasMinFilterThreshold(Direction dir) {
    return hasMinFilterThreshold(dir, currentTimeSlice);
  }

  /**
   * Gets the minimum filter threshold for a direction at a time slice
   * 
   * @param dir the direction
   * @param timeSlice the time slice
   * @return the minimum filter threshold or -1 if it does not exist
   */
  public double getMinFilterThreshold(Direction dir, int timeSlice) {
    HashMap<Direction, Double> minFilterThresholdMap = this.minFilterThresholds.get(timeSlice);

    if (minFilterThresholdMap.containsKey(dir))
      return minFilterThresholdMap.get(dir);
    return -1;
  }

  /**
   * Checks if the minimum filter theshold exists for a direction at a time slice
   * 
   * @param dir the direction
   * @param timeSlice the time slice
   * @return true if the minimum filter threshold exists
   */
  public boolean hasMinFilterThreshold(Direction dir, int timeSlice) {
    HashMap<Direction, Double> minFilterThresholdMap = this.minFilterThresholds.get(timeSlice);
    return minFilterThresholdMap.containsKey(dir);
  }

  /**
   * Gets the maximum filter threshold for a direction
   * 
   * @param dir the direction
   * @return the maximum filter threshold
   */
  public double getMaxFilterThreshold(Direction dir) {
    return getMaxFilterThreshold(dir, currentTimeSlice);
  }

  /**
   * Checks if the maximum filter threshold exists for a direction
   * 
   * @param dir the direction
   * @return true if the maximum filter threshold exists
   */
  public boolean hasMaxFilterThreshold(Direction dir) {
    return hasMaxFilterThreshold(dir, currentTimeSlice);
  }

  /**
   * Gets the maximum filter threshold for a direction at a timeslice
   * 
   * @param dir the direction
   * @param timeSlice the time slice
   * @return the maximum filter threshold or -1 if it does not exist
   */
  public double getMaxFilterThreshold(Direction dir, int timeSlice) {
    HashMap<Direction, Double> maxFilterThresholdMap = this.maxFilterThresholds.get(timeSlice);

    if (maxFilterThresholdMap.containsKey(dir))
      return maxFilterThresholdMap.get(dir);
    return -1;
  }

  /**
   * Checks if the maximum filter threshold exists for a direction at a time slice
   * 
   * @param dir the direction
   * @param timeSlice the time slice
   * @return true if the maximum filter threshold exists
   */
  public boolean hasMaxFilterThreshold(Direction dir, int timeSlice) {
    HashMap<Direction, Double> maxFilterThresholdMap = this.maxFilterThresholds.get(timeSlice);
    return maxFilterThresholdMap.containsKey(dir);
  }

  /**
   * Gets the standard deviation threshold for a direction
   * 
   * @param dir the direction
   * @return the standard deviation threshold
   */
  public double getStdDevThreshold(Direction dir) {
    return getStdDevThreshold(dir, currentTimeSlice);
  }

  /**
   * Checks if the standard deviation threshold for a direction exists
   * 
   * @param dir the direction
   * @return true if the standard deviation threshold exists
   */
  public boolean hasStdDevThreshold(Direction dir) {
    return hasStdDevThreshold(dir, currentTimeSlice);
  }

  /**
   * Gets the standard deviation threshold for a direction at a time slice
   * 
   * @param dir the direction
   * @param timeSlice the time slice
   * @return the standard deviation threshold or -1 if it does not exist
   */
  public double getStdDevThreshold(Direction dir, int timeSlice) {
    HashMap<Direction, Double> stdDevThresholdMap = this.stdDevThresholds.get(timeSlice);

    if (stdDevThresholdMap.containsKey(dir))
      return stdDevThresholdMap.get(dir);
    return -1;
  }

  /**
   * Checks if the standard deviation threshold for a direction exists at a time slice
   * 
   * @param dir the direction
   * @param timeSlice the time slice
   * @return true if the standard deviation threshold exists
   */
  public boolean hasStdDevThreshold(Direction dir, int timeSlice) {
    HashMap<Direction, Double> stdDevThresholdMap = this.stdDevThresholds.get(timeSlice);
    return stdDevThresholdMap.containsKey(dir);
  }

  /**
   * Gets the list of empty row/cols for a direction
   * 
   * @param dir the direction
   * @return the list of empty rows/cols
   */
  public List<Integer> getEmptyRowCols(Direction dir) {
    return getEmptyRowCols(dir, currentTimeSlice);
  }

  /**
   * Checks if the list of empty row/cols for a direction exist
   * 
   * @param dir the direction
   * @return true the list of empty rows/cols exists
   */
  public boolean hasEmptyRowCols(Direction dir) {
    return hasEmptyRowCols(dir, currentTimeSlice);
  }

  /**
   * Gets the list of empty row/cols for a direction at a time slice
   * 
   * @param dir the direction
   * @param timeSlice
   * @return the list of empty rows/cols or null
   */
  public List<Integer> getEmptyRowCols(Direction dir, int timeSlice) {
    HashMap<Direction, List<Integer>> EmptyRowColMap = this.emptyRowCols.get(timeSlice);

    if (EmptyRowColMap.containsKey(dir))
      return EmptyRowColMap.get(dir);
    return null;
  }

  public Integer getNumRowCols(Direction dir, int timeSlice) {
    HashMap<Direction, Integer> numRowCol = this.numRowCols.get(timeSlice);

    if (numRowCol.containsKey(dir))
      return numRowCol.get(dir);
    return 0;
  }

  /**
   * Checks if the list of empty row/cols for a direction exist for a time slice
   * 
   * @param dir the direction
   * @param timeSlice the time slice
   * @return true the list of empty rows/cols exists
   */
  public boolean hasEmptyRowCols(Direction dir, int timeSlice) {
    HashMap<Direction, List<Integer>> EmptyRowColMap = this.emptyRowCols.get(timeSlice);
    return EmptyRowColMap.containsKey(dir);
  }



  @Override
  public String toString() {
    return this.name;
  }

  /**
   * Writes the statitics to a file
   * 
   * @param fileName the file path
   */
  public void writeStatistics(String fileName)
  {
    this.writeStatistics(new File(fileName));
  }

  private void updateErrorStatus(int timeSlice, ErrorReportStatus status)
  {
    ErrorReportStatus curStatus = this.errorReportStatus.get(timeSlice);

    if (curStatus == ErrorReportStatus.PASSED || curStatus == ErrorReportStatus.WARNING)
      this.errorReportStatus.set(timeSlice, status);

  }

  private String getErrorReportStatus(int timeSlice)
  {
    return this.errorReportStatus.get(timeSlice).name();
  }


  /**
   * Runs error checks on the statistics file
   * @param timeSlice the timeslice you want to check
   * @return the string of errors
   */
  public String runErrorChecks(int timeSlice)
  {
    String newLine = "\n";

    String errorMessage = "";

    // Check no valid translations
    for (Direction dir : Direction.values()) {

      int numValid = getNumValidTilesAfterFilter(dir, timeSlice);
      if (numValid == 0) {
        updateErrorStatus(timeSlice, ErrorReportStatus.FAILED);
        errorMessage += "- No reliable " + dir + " translations found" + newLine;
        errorMessage += "Due to the content of the image data, MIST is not able to compute any "
                        + dir + " translations with high confidence." + newLine;
      }

      double overlap = getOverlap(dir, timeSlice);
      double computedOverlap = getComputedOverlap(dir, timeSlice);

      if (overlap != computedOverlap) {
        updateErrorStatus(timeSlice, ErrorReportStatus.WARNING);
        errorMessage += "- Computed " + dir + " overlap = " + computedOverlap + ". Value was clipped "
                        + "to " + overlap + newLine + "Clipped value can be adjusted using the "
                        + "percent overlap uncertainty or you can specify the overlap." + newLine;
      }

      double repeatability = getRepeatability(dir, timeSlice);

      if (repeatability > OptimizationRepeatability.MaxRepeatability) {
        updateErrorStatus(timeSlice, ErrorReportStatus.WARNING);
        errorMessage += "- Computed " + dir + " repeatability is high." + newLine;
      }

      List<Integer> emptyRowColsLst = getEmptyRowCols(dir, timeSlice);

      if (hasEmptyRowCols(dir, timeSlice)) {
        double percMissingRowCol = 100.0 * emptyRowColsLst.size() / getNumRowCols(dir, timeSlice);

        if (percMissingRowCol > MISSING_ROW_COL_WARNING_THRESHOLD) {
          updateErrorStatus(timeSlice, ErrorReportStatus.WARNING);
          errorMessage += "- Percentage missing " + dir + " rows/cols is high." + newLine;
        }
      }
    }

    String output = "Error report: " + getErrorReportStatus(timeSlice) + newLine;
    output += errorMessage;

    if (this.errorReportStatus.get(timeSlice) == ErrorReportStatus.FAILED) {
      output += "- We suggest that the user performs some combination of pre-processing " + newLine
                + "steps to to increase the confidence in the computed translations. " + newLine
                + "Pre-processing steps can include: (1) filtering the images, (2) segment " + newLine
                + "regions of interest and setting the background to zero to perform " + newLine
                + "feature-based translation computation. " + newLine
                + "- Please stitch the pre-processed images as the registration channel. " + newLine
                + "- Using \"Assemble From Metadata\" you can stitch the pre-processed images "
                + "and assemble the original images. " + newLine;

      output += "- For now MIST can only display a naively stitched image. " + newLine;
    }

    if (this.errorReportStatus.get(timeSlice) != ErrorReportStatus.PASSED) {
      output += "- The developers are interested in problematic data sets. " + newLine
                + "Issues with stitching can be submitted to: " + newLine
                + "nist-mist@nist.gov" + newLine + "or" + newLine + "http://github.com/NIST-ISG/MIST/issues";
      output += newLine;
    }

    return output;
  }


  public void writeLog(File file) {
    // write the contents of the log file to disk
    Log.msg(LogType.MANDATORY, "Saving Log to \"" + file.getAbsolutePath() + "\"");

    try{
      // ensure the directory to write the statistics file to exists
      File parent = file.getParentFile();
      if(!parent.exists()) parent.mkdir();

      FileWriter writer = new FileWriter(file);
      String logContents = IJ.getLog();
      if(logContents != null) {
        writer.write(logContents);
      }else{
        writer.write("IJ Log string was null. \n\nThis is likely due to the stitching being run in Headless mode.");
      }

      writer.close();

    }catch(IOException e) {
      Log.msg(LogType.MANDATORY, "Saving Log contents to disk failed");
      Log.msg(LogType.MANDATORY, e.getMessage());
    }
  }


  /**
   * Writes the statistics to a file
   * 
   * @param file the file to write the statistics
   */
  public void writeStatistics(File file) {
    Log.msg(LogType.MANDATORY, "Saving Statistics to \"" + file.getAbsolutePath() + "\"");
    String newLine = "\n";
    DecimalFormat df = new DecimalFormat("#.#");

    try {
      // ensure the directory to write the statistics file to exists
      File parent = file.getParentFile();
      if(!parent.exists()) parent.mkdir();

      FileWriter writer = new FileWriter(file);

      writer.write("System information:" + newLine);
      writer.write("Java Version: " + System.getProperty("java.version") + newLine);
      writer.write("Operating System: " + System.getProperty("os.name") + " "
          + System.getProperty("os.arch") + " v" + System.getProperty("os.version") + newLine);
      writer.write("CPU threads used for relative displacements: "
          + this.runParams.getAdvancedParams().getNumCPUThreads() + newLine);
      writer.write("CPU threads used for global optimization: "
          + Runtime.getRuntime().availableProcessors() + newLine);
      writer.write("Free memory available to JVM (GB): "
          + (Runtime.getRuntime().freeMemory() / 1024 / 1024 / 1024) + newLine);
      writer.write("Total memory available to JVM (GB): "
          + (Runtime.getRuntime().totalMemory() / 1024 / 1024 / 1024) + newLine);
      long maxMem = Runtime.getRuntime().maxMemory();
      writer.write("Max memory for JVM (GB): "
          + (maxMem == Long.MAX_VALUE ? "no limit" : (maxMem / 1024 / 1024 / 1024)) + newLine);

      writer.write("Execution type: " + this.executionType + newLine);
      if (this.executionType != null) {
        switch (this.executionType) {
          case AUTO:
            //writer.write("Error in selecting execution type" + newLine);
            break;
          case CUDA:
            List<CudaDeviceParam> cudaDevices = this.runParams.getAdvancedParams().getCudaDevices();
            writer.write("Number of GPUs used: " + cudaDevices.size() + newLine);
            for (CudaDeviceParam dev : cudaDevices) {
              writer.write(dev + newLine);
            }

            break;
          case FFTW:
            writer.write("FFTW Planning mode: " + this.runParams.getAdvancedParams().getFftwPlanType() + newLine);
            writer.write("Native utility functions used?: "
                + UtilFnsStitching.hasUtilFnsStitchingNativeLibrary() + newLine);
            break;
          case JAVA:
            break;
          default:
            break;
        }
      }

      writer.write(newLine + "Execution timing and general information:" + newLine);
      writer.write("Statistics Output Version: " + VERSION + newLine);
      writer.write(newLine);
      writer.write("Total time for experiment (ms): " + this.getEndToEndDuration() + newLine);

      for (int timeSlice : this.timeSlicesRun) {

        if (this.runParams.getInputParams().isTimeSlicesEnabled())
          writer.write("Time slice: " + timeSlice + newLine);

        if (this.isRunSequential(timeSlice))
          writer.write("Running sequential version (LOW MEMORY)" + newLine);

        for (RunTimers timer : RunTimers.values()) {
          if (this.hasDuration(timer, timeSlice)) {
            writer.write(timer.toString() + " (ms): " + this.getDuration(timer, timeSlice) + newLine);
          }
        }

        writer.write(newLine);
        for (Direction dir : Direction.values()) {
          if (hasRepeatability(dir, timeSlice))
            writer.write(dir + " repeatability: " + getRepeatability(dir, timeSlice) + newLine);

          if (hasOverlap(dir, timeSlice))
            writer.write(dir + " overlap: " + getOverlap(dir, timeSlice) + newLine);

          if (hasNumValidTilesAfterFilter(dir, timeSlice)) {
            int totalTiles = (this.runParams.getInputParams().getExtentHeight() * this.runParams.getInputParams().getExtentWidth());

            switch (dir) {
              case North:
                totalTiles -= this.runParams.getInputParams().getExtentWidth();
                break;
              case West:
                totalTiles -= this.runParams.getInputParams().getExtentHeight();
                break;
            }

            writer.write(dir + " valid tiles after filter: "
                + getNumValidTilesAfterFilter(dir, timeSlice) + " out of " + totalTiles + " (" + df.format(100.0*getNumValidTilesAfterFilter(dir, timeSlice)/totalTiles) + "%)" + newLine);
          }

          if (hasMinFilterThreshold(dir, timeSlice))
            writer.write(dir + " min filter threshold: " + getMinFilterThreshold(dir, timeSlice)
                + newLine);

          if (hasMaxFilterThreshold(dir, timeSlice))
            writer.write(dir + " max filter threshold: " + getMaxFilterThreshold(dir, timeSlice)
                + newLine);

          if (hasStdDevThreshold(dir, timeSlice))
            writer.write(dir + " standard deviation threshold: "
                + getStdDevThreshold(dir, timeSlice) + newLine);

          if (hasEmptyRowCols(dir, timeSlice)) {
            List<Integer> emptyRowColsLst = getEmptyRowCols(dir, timeSlice);
            writer.write(dir + " missing row/col: "
                    + Arrays.toString(emptyRowColsLst.toArray()) + newLine);

            writer.write(dir + " percentage missing row/col: " + df.format(
                100.0 * emptyRowColsLst.size() / getNumRowCols(dir, timeSlice)) + "%" + newLine);
          }
          writer.write(newLine);
        }

        writer.write(runErrorChecks(timeSlice));

        writer.write(newLine);
      }

      writer.write("Stitching Run-time parameters:" + newLine);
      this.runParams.saveParams(writer);

      writer.close();

    } catch (IOException e) {
//      e.printStackTrace();
      Log.msg(LogType.MANDATORY, "Saving Log contents to disk failed");
      Log.msg(LogType.MANDATORY, e.getMessage());
    }
  }

}
