// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.


package gov.nist.isg.mist.optimization.model;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import gov.nist.isg.mist.gui.StitchingStatistics;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.lib.common.CorrelationTriple;
import gov.nist.isg.mist.lib.common.MinMaxElement;
import gov.nist.isg.mist.lib.exceptions.GlobalOptimizationException;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.statistics.StatisticUtils;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.lib.tilegrid.TileGrid.Direction;
import gov.nist.isg.mist.lib.tilegrid.TileGrid.DisplacementValue;
import gov.nist.isg.mist.optimization.model.overlap.MleOverlapParallelExecutor;
import gov.nist.isg.mist.optimization.model.overlap.MleOverlapSequentialExecutor;
import gov.nist.isg.mist.optimization.model.overlap.OverlapExecutorInterface;

/**
 * The Stage Model used to filter, refine, and fix a TileGrids translations.
 *
 * @author Michael Majurski
 */
public class StageModel<T> {


  /**
   * The largest repeatability before the system will warn the user that the computed
   * repeatability value is unusually large.
   */
  public static final double MaxRepeatability = 10;
  /**
   * The default percent overlap uncertainty
   */
  private static final double OverlapError = 3.0;

  private volatile boolean isCancelled;

  private TileGrid<ImageTile<T>> grid;
  private int userDefinedRepeatability;
  private boolean isUserDefinedRepeatability;
  private double userDefinedVerticalOverlap;
  private double userDefinedHorizontalOverlap;
  private StitchingAppParams params;
  private StitchingStatistics stitchingStatistics;
  private boolean isSequential;
  private double percOverlapError;
  private double overlapVertical;
  private double overlapHorizontal;
  private int repeatabilityNorth;
  private int repeatabilityWest;
  private int repeatability;
  private OverlapExecutorInterface<T> overlapExecutorInterface;
  private HashSet<ImageTile<T>> validTranslationsNorth;
  private HashSet<ImageTile<T>> validTranslationsWest;

  /**
   * The Stage Model used to filter, refine, and fix a TileGrids translations.
   *
   * @param grid                the TileGrid from which the stage model will be built.
   * @param params              the stitching parameters.
   * @param stitchingStatistics the stitching statistics object.
   * @param isSequential        whether or not to utilize parallel execution to build the stage
   *                            model.
   */
  public StageModel(TileGrid<ImageTile<T>> grid,
                    StitchingAppParams params, StitchingStatistics stitchingStatistics,
                    boolean isSequential) {

    this.grid = grid;
    this.params = params;

    this.userDefinedRepeatability = params.getAdvancedParams().getRepeatability();
    this.userDefinedHorizontalOverlap = params.getAdvancedParams().getHorizontalOverlap();
    this.userDefinedVerticalOverlap = params.getAdvancedParams().getVerticalOverlap();

    percOverlapError = OverlapError;
    if (!Double.isNaN(params.getAdvancedParams().getOverlapUncertainty()))
      percOverlapError = params.getAdvancedParams().getOverlapUncertainty();

    this.isSequential = isSequential;
    this.isUserDefinedRepeatability = this.userDefinedRepeatability != 0;
    this.stitchingStatistics = stitchingStatistics;
    this.isCancelled = false;

    this.repeatability = 0;
    this.repeatabilityNorth = 0;
    this.repeatabilityWest = 0;
    this.overlapHorizontal = Double.NaN;
    this.overlapVertical = Double.NaN;

  }

  /**
   * Static method to obtain the valid range of translations for a given direction.
   *
   * @param grid      the TileGrid to draw translations from.
   * @param dispValue the displacement value to obtain a range for.
   * @param <T>       the type of ImageTile.
   * @return the maximum valid translation between two images (the width or height of that image).
   */
  public static <T> int getTranslationRange(TileGrid<ImageTile<T>> grid, DisplacementValue dispValue) {
    // get valid range for translations given the direction
    ImageTile<T> tile = grid.getTileThatExists();
    tile.readTile();
    int range = 0;
    switch (dispValue) {
      case X:
        range = tile.getWidth();
        break;
      case Y:
        range = tile.getHeight();
        break;
    }
    return range;
  }

  /**
   * Get the stage model overall repeatability.
   *
   * @return the stage model overall repeatability.
   */
  public int getRepeatability() {
    return this.repeatability;
  }

  /**
   * Get the stage model repeatability for a given direction.
   *
   * @param dir the direction to get the repeatability for.
   * @return the stage model repeatability for the query direction.
   */
  public int getRepeatability(Direction dir) {
    switch (dir) {
      case North:
        return this.repeatabilityNorth;
      case West:
        return this.repeatabilityWest;
    }
    return 0;
  }

  /**
   * Get the stage model vertical image overlap.
   *
   * @return the stage model vertical image overlap.
   */
  public double getVerticalOverlap() {
    return this.overlapVertical;
  }

  /**
   * Get the stage model horizontal image overlap.
   *
   * @return the stage model horizontal image overlap.
   */
  public double getHorizontalOverlap() {
    return this.overlapHorizontal;
  }

  /**
   * Get the set of valid translations found in building the stage model given a direction.
   *
   * @param dir the direction for which to get the valid translations.
   * @return the set of valid translations.
   */
  public HashSet<ImageTile<T>> getValidTranslations(Direction dir) {
    switch (dir) {
      case North:
        return this.validTranslationsNorth;
      case West:
        return this.validTranslationsWest;
    }
    return null;
  }


  /**
   * Method to build the stage model.
   *
   * This function updates the following StageModel class variables:
   * overlapVertical,
   * overlapHorizontal,
   * repeatabilityNorth,
   * repeatabilityWest,
   * repeatability
   */
  public void buildModel() throws GlobalOptimizationException {

    if (isCancelled) return;
    // compute the vertical overlap
    overlapVertical = computeOverlap(percOverlapError, TileGrid.Direction.North);


    if (isCancelled) return;
    // compute the horizontal overlap
    overlapHorizontal = computeOverlap(percOverlapError, TileGrid.Direction.West);


    if (isCancelled) return;
    repeatabilityNorth = computeRepeatability(TileGrid.Direction.North);
    if (isCancelled) return;
    repeatabilityWest = computeRepeatability(TileGrid.Direction.West);

    repeatability = Math.max(repeatabilityNorth, repeatabilityWest);
  }


  /**
   * Method to compute the model overlap in a given direction.
   *
   * @param percOverlapError the percent overlap uncertainty allowed in the stage model.
   * @param dir              the direction in which to compute the model image overlap.
   * @return the estimated model image overlap.
   */
  private double computeOverlap(double percOverlapError, Direction dir) throws GlobalOptimizationException {
    DisplacementValue dispValue = getDisplacement(dir);
    if (dispValue == null) return Double.NaN;


    // print the grid being used to compute the overlap in Direction
    Log.msg(LogType.INFO, "Grid being used to compute tile overlap for " + dir);
    for(int i = 0; i < grid.getExtentHeight(); ++i) {
      String tmp = "";
      for(int j = 0; j < grid.getExtentWidth(); ++j) {
        if(grid.hasTile(i + grid.getStartRow(), j + grid.getStartCol())) {
          if(dir == Direction.North) {
            if( grid.getTile(i + grid.getStartRow(), j + grid.getStartCol()).getNorthTranslation() != null)
              tmp = tmp + grid.getTile(i + grid.getStartRow(), j + grid.getStartCol()).getNorthTranslation().getY() + ", ";
          }else{
            if( grid.getTile(i + grid.getStartRow(), j + grid.getStartCol()).getWestTranslation() != null)
              tmp = tmp + grid.getTile(i + grid.getStartRow(), j + grid.getStartCol()).getWestTranslation().getX() + ", ";
          }
        }
      }
      Log.msg(LogType.INFO, tmp);
    }


    // compute the overlap from the translations
    // setup the overlap computation executor
    if (isSequential) {
      overlapExecutorInterface = new MleOverlapSequentialExecutor<T>(grid, dir, dispValue);
    } else {
      overlapExecutorInterface = new MleOverlapParallelExecutor<T>(grid, dir, dispValue, params.getAdvancedParams().getNumCPUThreads());
    }
    // run the overlap executor
    overlapExecutorInterface.execute();

    // handle any exceptions thrown by the overlap workers
    if (overlapExecutorInterface.isExceptionThrown()) {
      Throwable e = overlapExecutorInterface.getWorkerThrowable();
      Log.msg(LogType.MANDATORY, "Error Occurred in overlap computation worker: " + e.toString());
      for (StackTraceElement st : e.getStackTrace())
        Log.msg(LogType.MANDATORY, st.toString());
      throw new GlobalOptimizationException("Error occurred in overlap worker");
    }


    // get result overlap
    double overlap = overlapExecutorInterface.getOverlap();

    // record the computed overlap in the statistics file
    stitchingStatistics.setComputedOverlap(dir, overlap);

    // overwrite the computed overlap with the manual one, if applicable
    switch (dir) {
      case West:
        if (!Double.isNaN(userDefinedHorizontalOverlap))
          overlap = userDefinedHorizontalOverlap;
        break;
      case North:
        if (!Double.isNaN(userDefinedVerticalOverlap))
          overlap = userDefinedVerticalOverlap;
        break;
    }

    // check that a valid overlap value has been computed
    if (Double.isNaN(overlap)) {
      Log.msg(LogType.MANDATORY, "Warning: Unable to compute overlap for " + dir
          + " direction. Please set your overlap in the advanced options");
      throw new GlobalOptimizationException("Unable to compute overlap for " + dir + " direction.");
    }

    // record the computed overlap in the statistics object
    this.stitchingStatistics.setOverlap(dir, overlap);
    Log.msg(LogType.MANDATORY, "Computed " + dir + " overlap: " + overlap + "%");

    return overlap;
  }

  /**
   * Method to compute the model repeatability in a given direction.
   *
   * @param dir the direction in which to compute the model stage repeatability.
   * @return the estimated model stage repeatability for the given direction.
   */
  private int computeRepeatability(Direction dir) {

    double overlap = Double.NaN;
    switch (dir) {
      case West:
        overlap = this.overlapHorizontal;
        break;
      case North:
        overlap = this.overlapVertical;
        break;
    }

    // filter the translations to obtain a set of valid
    HashSet<ImageTile<T>> validTranslations = filterTranslations(this.grid,
        dir, percOverlapError, overlap, this.stitchingStatistics);

    switch (dir) {
      case West:
        validTranslationsWest = validTranslations;
        break;
      case North:
        validTranslationsNorth = validTranslations;
        break;
    }

    int repeatabilityValue = 0;
    // if no valid translations have been found
    if (validTranslations.size() == 0) {
      Log.msg(LogType.MANDATORY, "Warning: no good translations found for " + dir
          + " direction. Estimated translations generated from the overlap.");

      if (this.isUserDefinedRepeatability) {
        Log.msg(LogType.MANDATORY, "Warning: no good translations found for " + dir
            + " direction. Repeatability has been set to " +
            this.userDefinedRepeatability + " (advanced options value).");
      } else {
        Log.msg(LogType.MANDATORY, "Warning: no good translations found for " + dir
            + " direction. Repeatability has been set to zero.");
      }

      Log.msg(LogType.MANDATORY, "Please check the statistics file for more details.");
      repeatabilityValue = 0;
    } else {
      // the valid translations list was not empty
      Log.msg(LogType.INFO, "Computing min/max combinations using " + validTranslations.size()
          + " valid translations");

      Log.msg(LogType.INFO, "Computing Repeatability");
      MinMaxElement minMaxVal = getMinMaxValidTiles(validTranslations,
          dir, getDisplacementOrthogonal(dir));
      int repeatability1 = getRepeatability(minMaxVal);

      List<MinMaxElement> minMaxList = null;
      switch (dir) {
        case North:
          minMaxList = getMinMaxValidPerRow(this.grid,
              validTranslations, dir, getDisplacement(dir));
          break;
        case West:
          minMaxList = getMinMaxValidPerCol(this.grid,
              validTranslations, dir, getDisplacement(dir));
          break;
      }

      int repeatability2 = getRepeatability(minMaxList);

      repeatabilityValue = Math.max(repeatability1, repeatability2);
    }

    // If the user has defined the repeatability, overwrite the computed value
    if (this.isUserDefinedRepeatability) {
      Log.msg(LogType.MANDATORY, "Computed repeatability: " + repeatabilityValue + " Overridden by user specified repeatability: " + this.userDefinedRepeatability);
      repeatabilityValue = this.userDefinedRepeatability;
    } else {
      if (repeatabilityValue > MaxRepeatability) {
        Log.msg(LogType.MANDATORY, "Warning: the computed repeatability (" + repeatabilityValue
            + ") is unusually large. Consider manually specifying the repeatability in the Advanced Parameters.");
      }
    }

    // update the statistics file with the repeatability and number of valid tiles
    this.stitchingStatistics.setRepeatability(dir, repeatabilityValue);
    Log.msg(LogType.MANDATORY, "Repeatability for " + dir.name() + ": " + repeatabilityValue + " pixels");

    return repeatabilityValue;
  }


  /**
   * Computes the min and max of the list of image tiles for a given direction and displacement
   * value
   *
   * @param tiles   the list of tiles
   * @param dir     the direction
   * @param dispVal the displacement value to analyze
   * @return the min and max (MinMaxElement)
   */
  private static <T> MinMaxElement getMinMaxValidTiles(HashSet<ImageTile<T>> tiles, Direction dir,
                                                       DisplacementValue dispVal) {

    List<Double> T = new ArrayList<Double>();

    for (ImageTile<T> t : tiles) {
      CorrelationTriple triple = t.getTranslation(dir);

      if (triple == null || Double.isNaN(triple.getCorrelation()))
        continue;

      switch (dispVal) {
        case X:
          T.add((double) triple.getX());
          break;
        case Y:
          T.add((double) triple.getY());
          break;
      }
    }

    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;
    for (double d : T) {
      int val = (int) d;
      if (val < min) min = val;
      if (val > max) max = val;
    }

    return new MinMaxElement(min, max);
  }


  /**
   * Computes a list of min max elements, one for reach row in the grid of tiles for a given
   * direction and displacement value per row.
   *
   * @param grid       the grid of tiles
   * @param validTiles the set of valid tiles
   * @param dir        the direction
   * @param dispVal    the displacement value
   * @return a list of MinMaxElements, one per row.
   */
  private static <T> List<MinMaxElement> getMinMaxValidPerRow(TileGrid<ImageTile<T>> grid,
                                                              HashSet<ImageTile<T>> validTiles, Direction dir, DisplacementValue dispVal) {

    List<MinMaxElement> minMaxPerRow = new ArrayList<MinMaxElement>(grid.getExtentHeight());

    for (int row = 0; row < grid.getExtentHeight(); row++) {
      List<Double> T = new ArrayList<Double>();

      for (int col = 0; col < grid.getExtentWidth(); col++) {
        // get tile on row
        ImageTile<T> tile = grid.getSubGridTile(row, col);

        // If the tile we look at is not considered valid, then skip it
        if (!validTiles.contains(tile)) continue;
        CorrelationTriple triple = tile.getTranslation(dir);
        if (triple == null) continue;

        switch (dispVal) {
          case Y:
            T.add((double) triple.getY());
            break;
          case X:
            T.add((double) triple.getX());
            break;
        }
      }

      int min = Integer.MAX_VALUE;
      int max = Integer.MIN_VALUE;
      for (double d : T) {
        int val = (int) d;
        if (val < min) min = val;
        if (val > max) max = val;
      }

      if (min != Integer.MAX_VALUE || max != Integer.MIN_VALUE)
        minMaxPerRow.add(new MinMaxElement(min, max));

    }
    return minMaxPerRow;
  }


  /**
   * Computes a list of min max elements, one for reach column in the grid of tiles for a given
   * direction and displacement value.
   *
   * @param grid       the grid of image tiles
   * @param validTiles the set of valid tiles
   * @param dir        the direction
   * @param dispVal    the displacement value to be operated on
   * @return the list of mins and maxes (MinMaxElement), one for each column of the grid
   */
  private static <T> List<MinMaxElement> getMinMaxValidPerCol(TileGrid<ImageTile<T>> grid,
                                                              HashSet<ImageTile<T>> validTiles, Direction dir, DisplacementValue dispVal) {

    List<MinMaxElement> minMaxPerCol = new ArrayList<MinMaxElement>(grid.getExtentWidth());

    for (int col = 0; col < grid.getExtentWidth(); col++) {
      List<Double> T = new ArrayList<Double>();

      for (int row = 0; row < grid.getExtentHeight(); row++) {
        // get tile on row
        ImageTile<T> tile = grid.getSubGridTile(row, col);

        if (!validTiles.contains(tile)) continue;
        CorrelationTriple triple = tile.getTranslation(dir);
        if (triple == null) continue;

        switch (dispVal) {
          case Y:
            T.add((double) triple.getY());
            break;
          case X:
            T.add((double) triple.getX());
            break;
        }
      }

      int min = Integer.MAX_VALUE;
      int max = Integer.MIN_VALUE;
      for (double d : T) {
        int val = (int) d;
        if (val < min) min = val;
        if (val > max) max = val;
      }

      if (min != Integer.MAX_VALUE || max != Integer.MIN_VALUE)
        minMaxPerCol.add(new MinMaxElement(min, max));

    }
    return minMaxPerCol;
  }

  /**
   * Compute the repeatability for a min and max : ceil( (max - min) / 2.0). Typically this can be
   * called in conjunction with getMinMaxValidTiles
   *
   * @param minMax the min max element
   * @return the repeatability for the min max element : ceil ( (max - min) / 2.0)
   */
  private static int getRepeatability(MinMaxElement minMax) {
    return (int) Math.ceil(((double) minMax.getMax() - (double) minMax.getMin()) / 2.0);
  }

  /**
   * Computes the repeatability for a list of mins and maxes. Typically this can be called in
   * conjunction with getMinMaxPerRow or getMinMaxPerCol
   *
   * @param minMaxes the list of min max elements.
   * @return the repeatability
   */
  private static int getRepeatability(List<MinMaxElement> minMaxes) {
    List<Integer> vals = new ArrayList<Integer>();

    // compute ceil((max-min)/2) for each row/col
    for (int i = 0; i < minMaxes.size(); i++) {
      MinMaxElement minMax = minMaxes.get(i);
      if (minMax == null)
        continue;

      vals.add((int) Math.ceil(((double) minMax.getMax() - (double) minMax.getMin()) / 2.0));
    }

    return (int) StatisticUtils.max(vals);

  }

  /**
   * Get the displacement value in line with a Direction.
   *
   * @param dir the query direction.
   * @return the primary displacement value given the direction (North:Y, West:X)
   */
  private static DisplacementValue getDisplacement(Direction dir) {
    DisplacementValue dispValue = null;
    switch (dir) {
      case North:
        dispValue = TileGrid.DisplacementValue.Y;
        break;
      case West:
        dispValue = TileGrid.DisplacementValue.X;
        break;
    }
    return dispValue;
  }

  /**
   * Get the displacement value orthogonal to a Direction
   *
   * @param dir the query direction
   * @return the orthogonal displacement value given the direction (North:X, West:Y)
   */
  private static DisplacementValue getDisplacementOrthogonal(Direction dir) {
    DisplacementValue dispValue = null;
    switch (dir) {
      case North:
        dispValue = TileGrid.DisplacementValue.X;
        break;
      case West:
        dispValue = TileGrid.DisplacementValue.Y;
        break;
    }
    return dispValue;
  }


  /**
   * Filters grid of image tiles based on calculated overlap, correlation, and standard deviation. A
   * set of valid image tiles after filtering is returned.
   *
   * @param grid                the grid of image tiles
   * @param dir                 the direction that is to be filtered
   * @param percOverlapError    the percent overlap of error
   * @param overlap             the overlap between images
   * @param stitchingStatistics the stitching statistics
   * @return the list of valid image tiles
   */
  private static <T> HashSet<ImageTile<T>> filterTranslations(TileGrid<ImageTile<T>> grid,
                                                              Direction dir, double percOverlapError,
                                                              double overlap,
                                                              StitchingStatistics stitchingStatistics) {
    Log.msg(LogType.INFO, "Filtering translations");
    DisplacementValue dispValue = null;
    switch (dir) {
      case North:
        dispValue = TileGrid.DisplacementValue.Y;
        break;
      case West:
        dispValue = TileGrid.DisplacementValue.X;
        break;
      default:
        break;
    }

    // filter the image tiles by overlap (using percent overlap uncertainty) and correlation
    HashSet<ImageTile<T>> validTiles =
        filterTilesFromOverlapAndCorrelation(dir, dispValue, overlap, percOverlapError, grid,
            stitchingStatistics);

    // filter the translations to remove outliers
    // this replaces the std filtering of the overlap region between images
    switch (dir) {
      case North:
        validTiles = filterTranslationsRemoveOutliers(validTiles, dir, TileGrid.DisplacementValue.Y);
        validTiles = filterTranslationsRemoveOutliers(validTiles, dir, TileGrid.DisplacementValue.X);
        break;
      case West:
        validTiles = filterTranslationsRemoveOutliers(validTiles, dir, TileGrid.DisplacementValue.X);
        validTiles = filterTranslationsRemoveOutliers(validTiles, dir, TileGrid.DisplacementValue.Y);
        break;
    }

    Log.msg(LogType.VERBOSE, "Finished filter - valid tiles: " + validTiles.size());

    return validTiles;
  }


  /**
   * Filter the translations of a given direction using the percent overlap uncertainty and the
   * correlation.
   *
   * @param dir                 the direction in which to filter the translations.
   * @param dispValue           the displacement value to use in filtering translations.
   * @param overlap             the estimated overlap between images.
   * @param percOverlapError    the percent overlap uncertainty allowable in the stage model.
   * @param grid                the TileGrid from which to draw the translations.
   * @param stitchingStatistics the stitching statistics file.
   * @param <T>                 the Type of ImageTile in the TileGrid.
   * @return the set of valid translations.
   */
  private static <T> HashSet<ImageTile<T>> filterTilesFromOverlapAndCorrelation(Direction dir, DisplacementValue dispValue, double overlap, double
      percOverlapError, TileGrid<ImageTile<T>> grid, StitchingStatistics stitchingStatistics) {
    double minCorrelation = TranslationFilter.CorrelationThreshold;

    HashSet<ImageTile<T>> validTiles = new HashSet<ImageTile<T>>();

    double t_min = 0;
    double t_max = 0;

    ImageTile<T> initTile = grid.getTileThatExists();
    initTile.readTile();

    double width = initTile.getWidth();
    double height = initTile.getHeight();

    switch (dispValue) {
      case Y:
        t_min = height - (overlap + percOverlapError) * height / 100.0;
        t_max = height - (overlap - percOverlapError) * height / 100.0;
        break;
      case X:
        t_min = width - (overlap + percOverlapError) * width / 100.0;
        t_max = width - (overlap - percOverlapError) * width / 100.0;
        break;
    }

    stitchingStatistics.setMinFilterThreshold(dir, t_min);
    stitchingStatistics.setMaxFilterThreshold(dir, t_max);

    Log.msg(LogType.VERBOSE, "min,max threshold: " + t_min + "," + t_max);

    // Filter based on t_min, t_max, and minCorrelation
    for (int r = 0; r < grid.getExtentHeight(); r++) {
      for (int c = 0; c < grid.getExtentWidth(); c++) {
        ImageTile<T> tile = grid.getSubGridTile(r, c);

        CorrelationTriple triple = tile.getTranslation(dir);

        if (triple == null) continue;
        if (triple.getCorrelation() < minCorrelation) continue;

        switch (dispValue) {
          case Y:
            if (triple.getY() < t_min || triple.getY() > t_max) continue;
            // limit the valid translations to within percent overlap error of 0 on the orthogonal direction
            if (triple.getX() < -percOverlapError || triple.getX() > percOverlapError) continue;
            break;
          case X:
            if (triple.getX() < t_min || triple.getX() > t_max) continue;
            // limit the valid translations to within percent overlap error of 0 on the orthogonal direction
            if (triple.getY() < -percOverlapError || triple.getY() > percOverlapError) continue;
            break;
        }
        validTiles.add(tile);
      }
    }
    return validTiles;
  }

  /**
   * Filter the translations by removing the outlier translations using the standard definition of
   * an outlier.
   *
   * @param validTiles the set of valid tiles to filter.
   * @param dir        the direction in which to filter the translations.
   * @param dispVal    the displacement value to select which translation component to filter.
   * @param <T>        the Type of the ImageTiles in the TileGrid
   * @return set of valid tiles with the outliers removed.
   */
  private static <T> HashSet<ImageTile<T>> filterTranslationsRemoveOutliers(HashSet<ImageTile<T>> validTiles,
                                                                            Direction dir, DisplacementValue dispVal) {
    // obtain a List of the relevant translations
    List<Double> T = new ArrayList<Double>();

    for (ImageTile<T> t : validTiles) {
      CorrelationTriple triple = t.getTranslation(dir);
      if (triple == null || Double.isNaN(triple.getCorrelation())) continue;

      switch (dispVal) {
        case X:
          T.add((double) triple.getX());
          break;
        case Y:
          T.add((double) triple.getY());
          break;
      }
    }

    // compute the statistics required to determine which translations are outliers

    // q1 is first quartile
    // q2 is second quartile (median)
    // q3 is third quartile
    // filter based on (>q3 + w(q3-q1)) and (<q1 - w(q3-q1))

    // only filter if there are more than 3 translations
    if (T.size() <= 3)
      return validTiles;

    double weight = 1.5; // default statistical outlier w (1.5)
    double median = getMedian(T);
    List<Double> lessThan = new ArrayList<Double>();
    List<Double> greaterThan = new ArrayList<Double>();
    for (Double d : T) {
      if (d < median)
        lessThan.add(d);
      if (d > median)
        greaterThan.add(d);
    }
    // if any sublist is empty, return
    if (lessThan.size() == 0 || greaterThan.size() == 0)
      return validTiles;

    double q1 = getMedian(lessThan);
    double q3 = getMedian(greaterThan);
    double iqd = Math.abs(q3 - q1);

    // Iterate over the translations and remove those that are outliers
    Iterator<ImageTile<T>> itr = validTiles.iterator();
    while (itr.hasNext()) {
      ImageTile<T> t = itr.next();
      CorrelationTriple triple = null;
      switch (dir) {
        case North:
          triple = t.getNorthTranslation();
          break;
        case West:
          triple = t.getWestTranslation();
          break;
      }

      if (triple != null && !Double.isNaN(triple.getCorrelation())) {
        double translation = Double.NaN;
        switch (dispVal) {
          case X:
            translation = triple.getX();
            break;
          case Y:
            translation = triple.getY();
            break;
        }
        // if the translations is out of range, remove it from the valid list
        if (translation < (q1 - weight * iqd) || translation > (q3 + weight * iqd))
          itr.remove();
      }
    }

    return validTiles;
  }


  /**
   * Method to compute the median of a List
   *
   * @param values the List of values to compute the median from.
   * @return the median of the List of values.
   */
  private static double getMedian(List<Double> values) {

    if (values.isEmpty())
      return Double.NaN;

    Collections.sort(values);
    if (values.size() % 2 == 1) {
      return values.get((values.size() + 1) / 2 - 1);
    } else {
      double upper = values.get(values.size() / 2 - 1);
      double lower = values.get(values.size() / 2);
      return (lower + upper) / 2.0;
    }
  }

  /**
   * Cancels the stage model build and any running executors
   */
  public void cancel() {
    this.isCancelled = true;
    if (overlapExecutorInterface != null)
      overlapExecutorInterface.cancel();
  }
}
