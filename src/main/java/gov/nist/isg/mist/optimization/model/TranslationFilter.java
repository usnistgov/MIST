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

package gov.nist.isg.mist.optimization.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gov.nist.isg.mist.stitching.gui.StitchingStatistics;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.lib.common.CorrelationTriple;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.statistics.StatisticUtils;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid.Direction;

/**
 * Translation filtering based on the stage model.
 *
 * @author Michael Majurski
 */
public class TranslationFilter<T> {


  public static final double CorrelationThreshold = 0.5;
  public static final double CorrelationWeight = 3.0;

  private TileGrid<ImageTile<T>> grid;
  private StitchingAppParams params;
  private StitchingStatistics stitchingStatistics;

  /**
   * Translation filtering based on the stage model.
   *
   * @param grid                the TileGrid to filter translations from.
   * @param params              the stitching parameters.
   * @param stitchingStatistics the stitching statistics.
   */
  public TranslationFilter(TileGrid<ImageTile<T>> grid,
                           StitchingAppParams params, StitchingStatistics stitchingStatistics) {

    this.grid = grid;
    this.params = params;
    this.stitchingStatistics = stitchingStatistics;
  }

  /**
   * Apply the stage model on the TileGrid to filter the translations.
   *
   * @param stageModel the model to apply.
   */
  public void applyStageModel(StageModel stageModel) {

    applyModelPerDirection(stageModel, TileGrid.Direction.North);
    applyModelPerDirection(stageModel, TileGrid.Direction.West);
  }


  /**
   * Apply the stage model on the TileGrid given a direction.
   *
   * @param stageModel the model to apply.
   * @param dir        the direction of translations upon which to apply the model.
   */
  @SuppressWarnings("unchecked")
  private void applyModelPerDirection(StageModel stageModel, Direction dir) {
    HashSet<ImageTile<T>> validTranslations = stageModel.getValidTranslations(dir);

    if (validTranslations.size() == 0) {
      // replace with translation estimated from overlap
      switch (dir) {
        case North:
          replaceTranslationFromOverlap(this.grid, dir,
              TileGrid.DisplacementValue.Y, stageModel.getVerticalOverlap());
          break;
        case West:
          replaceTranslationFromOverlap(this.grid, dir,
              TileGrid.DisplacementValue.X, stageModel.getHorizontalOverlap());
          break;
      }
      // If the valid tiles list is empty, the repeatability has already been set to 0 in StageModel.buildModel
      return;
    }


    Log.msg(Log.LogType.VERBOSE, "Fixing translations");
    List<Integer> missingRowOrCol = null;
    switch (dir) {
      case North:
        removeInvalidTranslationsPerRow(validTranslations, stageModel.getRepeatability(dir), dir);
        this.stitchingStatistics.setNumValidTilesAfterFilter(dir, validTranslations.size());
        this.stitchingStatistics.setNumRowsCols(dir, grid.getExtentHeight());
        missingRowOrCol = fixInvalidTranslationsPerRow(this.grid, dir, StatisticUtils.OP_TYPE.MEDIAN);
        break;
      case West:
        removeInvalidTranslationsPerCol(validTranslations, stageModel.getRepeatability(dir), dir);
        this.stitchingStatistics.setNumValidTilesAfterFilter(dir, validTranslations.size());
        this.stitchingStatistics.setNumRowsCols(dir, grid.getExtentWidth());
        missingRowOrCol = fixInvalidTranslationsPerCol(this.grid, dir, StatisticUtils.OP_TYPE.MEDIAN);
        break;
    }

    this.stitchingStatistics.setEmptyRowsCols(dir, missingRowOrCol);

    if (missingRowOrCol != null && missingRowOrCol.size() > 0) {
      CorrelationTriple median = computeOp(validTranslations, dir, StatisticUtils.OP_TYPE.MEDIAN);

      switch (dir) {
        case North:
          replaceTranslationsRow(this.grid, median, missingRowOrCol);
          break;
        case West:
          replaceTranslationsCol(this.grid, median, missingRowOrCol);
          break;
      }
    }
  }


  /**
   * Remove invalid translations that are less than 0.5 and not within the median per row for X and
   * Y. All translations that are not in range or have a correlation less than 0.5 have their
   * correlations set to NaN. All translations that are valid have their correlations incremented by
   * 3.0
   *
   * @param validTiles    a set of valid tiles
   * @param repeatability the computed repeatability
   * @param dir           the direction we are working with
   */
  private void removeInvalidTranslationsPerRow(Set<ImageTile<T>> validTiles, int repeatability, Direction dir) {

    if (validTiles.isEmpty()) return;

    // compute median X and Y values
    List<Double> medianXVals = new ArrayList<Double>();
    List<Double> medianYVals = new ArrayList<Double>();

    List<CorrelationTriple> validTriples = new ArrayList<CorrelationTriple>();
    for (int row = 0; row < grid.getExtentHeight(); row++) {
      validTriples.clear();

      for (int col = 0; col < grid.getExtentWidth(); col++) {
        ImageTile<T> tile = grid.getSubGridTile(row, col);

        if (!validTiles.contains(tile)) continue;

        switch (dir) {
          case North:
            validTriples.add(tile.getNorthTranslation());
            break;
          case West:
            validTriples.add(tile.getWestTranslation());
            break;
        }
      }

      double medianRowX = Double.NaN;
      double medianRowY = Double.NaN;

      if (validTriples.size() > 0) {
        medianRowX = computeOp(validTriples, StatisticUtils.OP_TYPE.MEDIAN, TileGrid.DisplacementValue.X);
        medianRowY = computeOp(validTriples, StatisticUtils.OP_TYPE.MEDIAN, TileGrid.DisplacementValue.Y);
      }
      medianXVals.add(medianRowX);
      medianYVals.add(medianRowY);
    }

    // Filter based on repeatability and correlation
    for (int row = 0; row < grid.getExtentHeight(); row++) {
      for (int col = 0; col < grid.getExtentWidth(); col++) {
        ImageTile<T> tile = grid.getSubGridTile(row, col);

        CorrelationTriple triple = null;

        switch (dir) {
          case North:
            triple = tile.getNorthTranslation();
            break;
          case West:
            triple = tile.getWestTranslation();
            break;
        }

        if (triple == null) continue;

        if (Double.isNaN(medianXVals.get(row)) || Double.isNaN(medianYVals.get(row))) {
          triple.setCorrelation(Double.NaN);
          continue;
        }

        double xMin = medianXVals.get(row) - repeatability;
        double xMax = medianXVals.get(row) + repeatability;
        double yMin = medianYVals.get(row) - repeatability;
        double yMax = medianYVals.get(row) + repeatability;

        // If correlation is less than CorrelationThreshold
        // or outside x range or outside y range, then throw away
        if (triple.getCorrelation() < CorrelationThreshold || triple.getX() < xMin
            || triple.getX() > xMax || triple.getY() < yMin || triple.getY() > yMax) {
          if (validTiles.contains(tile))
            validTiles.remove(tile);

          triple.setCorrelation(Double.NaN);
        } else {
//          triple.incrementCorrelation(CorrelationWeight);
          validTiles.add(tile);
        }
      }
    }
  }

  /**
   * Remove invalid translations that are less than 0.5 and not within the median per column for X
   * and Y. All translations that are not in range or have a correlation less than 0.5 have their
   * correlations set to NaN. All translations that are valid have their correlations incremented by
   * 3.0
   *
   * @param validTiles    a set of valid tiles
   * @param repeatability the computed repeatability
   * @param dir           the direction we are working with
   */
  private void removeInvalidTranslationsPerCol(Set<ImageTile<T>> validTiles, int repeatability, Direction dir) {

    if (validTiles.isEmpty()) return;

    // compute median X and Y values
    List<Double> medianXVals = new ArrayList<Double>();
    List<Double> medianYVals = new ArrayList<Double>();

    List<CorrelationTriple> validTriples = new ArrayList<CorrelationTriple>();
    for (int col = 0; col < grid.getExtentWidth(); col++) {
      validTriples.clear();

      for (int row = 0; row < grid.getExtentHeight(); row++) {
        ImageTile<T> tile = grid.getSubGridTile(row, col);

        if (!validTiles.contains(tile)) continue;

        switch (dir) {
          case North:
            validTriples.add(tile.getNorthTranslation());
            break;
          case West:
            validTriples.add(tile.getWestTranslation());
            break;
        }
      }

      double medianColX = Double.NaN;
      double medianColY = Double.NaN;

      if (validTriples.size() > 0) {
        medianColX = computeOp(validTriples, StatisticUtils.OP_TYPE.MEDIAN, TileGrid.DisplacementValue.X);
        medianColY = computeOp(validTriples, StatisticUtils.OP_TYPE.MEDIAN, TileGrid.DisplacementValue.Y);
      }
      medianXVals.add(medianColX);
      medianYVals.add(medianColY);
    }

    // Filter based on repeatability and correlation
    for (int col = 0; col < grid.getExtentWidth(); col++) {
      for (int row = 0; row < grid.getExtentHeight(); row++) {
        ImageTile<T> tile = grid.getSubGridTile(row, col);

        CorrelationTriple triple = null;

        switch (dir) {
          case North:
            triple = tile.getNorthTranslation();
            break;
          case West:
            triple = tile.getWestTranslation();
            break;
        }

        if (triple == null) continue;

        if (Double.isNaN(medianXVals.get(col)) || Double.isNaN(medianYVals.get(col))) {
          triple.setCorrelation(Double.NaN);
          continue;
        }

        double xMin = medianXVals.get(col) - repeatability;
        double xMax = medianXVals.get(col) + repeatability;
        double yMin = medianYVals.get(col) - repeatability;
        double yMax = medianYVals.get(col) + repeatability;

        // If correlation is less than CorrelationThreshold
        // or outside x range or outside y range, then throw away
        if (triple.getCorrelation() < CorrelationThreshold || triple.getX() < xMin
            || triple.getX() > xMax || triple.getY() < yMin || triple.getY() > yMax) {
          if (validTiles.contains(tile))
            validTiles.remove(tile);

          triple.setCorrelation(Double.NaN);
        } else {
//          triple.incrementCorrelation(CorrelationWeight);
          validTiles.add(tile);
        }
      }
    }
  }


  /**
   * Fixes invalid translations in each row (translations that have NaN in their correlation).
   * op(validTranslationsPerRow) is used as the corrected translations. If an entire row is empty,
   * then it is added to the list of empty rows, which is returned.
   *
   * @param grid the grid of image tiles
   * @param dir  the direction
   * @param op   the operation type (median, mode, max, ... etc.)
   * @return the list of rows that have no good translations
   */
  public static <T> List<Integer> fixInvalidTranslationsPerRow(TileGrid<ImageTile<T>> grid,
                                                               Direction dir, StatisticUtils.OP_TYPE op) {
    List<Integer> emptyRows = new ArrayList<Integer>();
    List<CorrelationTriple> validCorrTriplesInRow = new ArrayList<CorrelationTriple>();
    List<CorrelationTriple> invalidCorrTriplesInRow = new ArrayList<CorrelationTriple>();

    // handle per row
    for (int row = 0; row < grid.getExtentHeight(); row++) {
      validCorrTriplesInRow.clear();
      invalidCorrTriplesInRow.clear();

      // First get all correlations that are not NaN
      for (int col = 0; col < grid.getExtentWidth(); col++) {

        ImageTile<T> tile = grid.getSubGridTile(row, col);
        CorrelationTriple triple = null;

        switch (dir) {
          case North:
            triple = tile.getNorthTranslation();
            break;
          case West:
            triple = tile.getWestTranslation();
            break;
        }

        if (triple == null) continue;

        if (Double.isNaN(triple.getCorrelation()))
          invalidCorrTriplesInRow.add(triple);
        else
          validCorrTriplesInRow.add(triple);
      }

      // Check for empty row
      if (validCorrTriplesInRow.size() == 0) {
        if (invalidCorrTriplesInRow.size() > 0)
          emptyRows.add(row);
      } else {
        CorrelationTriple optimizedTriple = computeOp(validCorrTriplesInRow, op);

        for (CorrelationTriple t : invalidCorrTriplesInRow) {
          t.setX(optimizedTriple.getX());
          t.setY(optimizedTriple.getY());
        }
      }
    }

    return emptyRows;
  }

  /**
   * Fixes invalid translations in each column (translations that have NaN in their correlation).
   * op(validTranslationsPerCol) is used as the corrected translations. If an entire column is
   * empty, then it is added to the list of empty columns, which is returned.
   *
   * @param grid the grid of image tiles
   * @param dir  the direction
   * @param op   the operation type (median, mode, max, ... etc.)
   * @return the list of columns that have no good translations
   */
  public static <T> List<Integer> fixInvalidTranslationsPerCol(TileGrid<ImageTile<T>> grid,
                                                               Direction dir, StatisticUtils.OP_TYPE op) {
    List<Integer> emptyCols = new ArrayList<Integer>();
    List<CorrelationTriple> validCorrTriplesInCol = new ArrayList<CorrelationTriple>();
    List<CorrelationTriple> invalidCorrTriplesInCol = new ArrayList<CorrelationTriple>();

    // handle per row
    for (int col = 0; col < grid.getExtentWidth(); col++) {
      validCorrTriplesInCol.clear();
      invalidCorrTriplesInCol.clear();

      // First get all correlations that are not NaN
      for (int row = 0; row < grid.getExtentHeight(); row++) {

        ImageTile<T> tile = grid.getSubGridTile(row, col);
        CorrelationTriple triple = null;

        switch (dir) {
          case North:
            triple = tile.getNorthTranslation();
            break;
          case West:
            triple = tile.getWestTranslation();
            break;
        }

        if (triple == null) continue;

        if (Double.isNaN(triple.getCorrelation()))
          invalidCorrTriplesInCol.add(triple);
        else
          validCorrTriplesInCol.add(triple);
      }

      // Check for empty column
      if (validCorrTriplesInCol.size() == 0) {
        if (invalidCorrTriplesInCol.size() > 0)
          emptyCols.add(col);
      } else {
        CorrelationTriple optimizedTriple = computeOp(validCorrTriplesInCol, op);

        for (CorrelationTriple t : invalidCorrTriplesInCol) {
          t.setX(optimizedTriple.getX());
          t.setY(optimizedTriple.getY());
        }
      }
    }
    return emptyCols;
  }

  /**
   * Replace any invalid translations with an estimate derived from the stage model image overlap.
   *
   * @param grid      the TileGrid to replace invalid translations within.
   * @param dir       the direction of translations being replaced.
   * @param dispValue the displacement value of the translation.
   * @param overlap   the stage model overlap to use in replacing the translations.
   * @param <T>       the Type of ImageTile in the TileGrid.
   */
  private static <T> void replaceTranslationFromOverlap(TileGrid<ImageTile<T>> grid, Direction dir, TileGrid.DisplacementValue dispValue, double overlap) {

    int estTranslation;
    switch (dir) {
      case West:
        estTranslation = (int) Math.round(StageModel.getTranslationRange(grid, dispValue) * (1 - overlap / 100));
        for (int col = 0; col < grid.getExtentWidth(); col++) {
          for (int row = 0; row < grid.getExtentHeight(); row++) {
            CorrelationTriple triple = grid.getSubGridTile(row, col).getWestTranslation();
            if (triple != null) {
              triple.setX(estTranslation);
              triple.setY(0);
              triple.setCorrelation(Double.NaN);
            }
          }
        }
        break;

      case North:
        estTranslation = (int) Math.round(StageModel.getTranslationRange(grid, dispValue) * (1 - overlap / 100));
        for (int col = 0; col < grid.getExtentWidth(); col++) {
          for (int row = 0; row < grid.getExtentHeight(); row++) {
            CorrelationTriple triple = grid.getSubGridTile(row, col).getNorthTranslation();
            if (triple != null) {
              triple.setX(0);
              triple.setY(estTranslation);
              triple.setCorrelation(Double.NaN);
            }
          }
        }
        break;
    }
  }

  /**
   * Replace the invalid translations within a row given the replacement correlation triple.
   *
   * @param grid        the TileGrid in which to replace translations.
   * @param median      the correlation triple containing the replacement values.
   * @param missingRows the rows which have values needing replacement.
   * @param <T>         the Type of ImageTile within the TileGrid.
   */
  private static <T> void replaceTranslationsRow(TileGrid<ImageTile<T>> grid, CorrelationTriple median, List<Integer> missingRows) {
    for (int row : missingRows) {
      for (int col = 0; col < grid.getExtentWidth(); col++) {
        ImageTile<T> tile = grid.getSubGridTile(row, col);

        CorrelationTriple triple = tile.getNorthTranslation();
        triple.setX(median.getX());
        triple.setY(median.getY());
      }
    }
  }

  /**
   * Replace the invalid translations within a column given the replacement correlation triple.
   *
   * @param grid        the TileGrid in which to replace translations.
   * @param median      the correlation triple containing the replacement values.
   * @param missingCols the columns which hvae values needing replacement.
   * @param <T>         the Type of ImageTile within the TileGrid.
   */
  private static <T> void replaceTranslationsCol(TileGrid<ImageTile<T>> grid, CorrelationTriple median, List<Integer> missingCols) {
    for (int col : missingCols) {
      for (int row = 0; row < grid.getExtentHeight(); row++) {
        ImageTile<T> tile = grid.getSubGridTile(row, col);

        CorrelationTriple triple = tile.getWestTranslation();
        triple.setX(median.getX());
        triple.setY(median.getY());
      }
    }
  }


  /**
   * Computes an operation on a list of correlation triples (one for both X and Y)
   *
   * @param vals the list of correlation triples
   * @param op   the operation type
   * @return the correlation values s.t. corrTriple.x = op(vals.x), corrTriple.y = op(vals.y), and
   * corrTriple.corr is irrelevant
   */
  private static CorrelationTriple computeOp(List<CorrelationTriple> vals, StatisticUtils.OP_TYPE op) {
    List<Double> xVals = new ArrayList<Double>();
    List<Double> yVals = new ArrayList<Double>();

    for (CorrelationTriple t : vals) {
      xVals.add((double) t.getX());
      yVals.add((double) t.getY());
    }

    double xOptimized = 0.0;
    double yOptimized = 0.0;

    switch (op) {
      case MAX:
        xOptimized = StatisticUtils.max(xVals);
        yOptimized = StatisticUtils.max(yVals);
        break;
      case MEAN:
        xOptimized = StatisticUtils.mean(xVals);
        yOptimized = StatisticUtils.mean(yVals);
        break;
      case MEDIAN:
        xOptimized = StatisticUtils.median(xVals);
        yOptimized = StatisticUtils.median(yVals);
        break;
      case MIN:
        xOptimized = StatisticUtils.min(xVals);
        yOptimized = StatisticUtils.min(yVals);
        break;
      case MODE:
        xOptimized = StatisticUtils.mode(xVals);
        yOptimized = StatisticUtils.mode(yVals);
        break;
    }

    xOptimized = Math.round(xOptimized);
    yOptimized = Math.round(yOptimized);

    return new CorrelationTriple(Double.NaN, (int) xOptimized, (int) yOptimized);
  }

  /**
   * Computes an operation on a list of correlation triples for the given displacement value.
   *
   * @param vals    the list of correlation triples
   * @param op      the operation type
   * @param dispVal the displacement value that is operated on
   * @return op(vals.dispVal)
   */
  private static double computeOp(List<CorrelationTriple> vals, StatisticUtils.OP_TYPE op, TileGrid.DisplacementValue dispVal) {
    List<Double> tVals = new ArrayList<Double>();

    for (CorrelationTriple t : vals) {
      switch (dispVal) {
        case X:
          tVals.add((double) t.getX());
          break;
        case Y:
          tVals.add((double) t.getY());
          break;
      }
    }

    double opResult = 0.0;

    switch (op) {
      case MAX:
        opResult = StatisticUtils.max(tVals);
        break;
      case MEAN:
        opResult = StatisticUtils.mean(tVals);
        break;
      case MEDIAN:
        opResult = StatisticUtils.median(tVals);
        break;
      case MIN:
        opResult = StatisticUtils.min(tVals);
        break;
      case MODE:
        opResult = StatisticUtils.mode(tVals);
        break;
    }
    return opResult;
  }

  /**
   * Computes an operation on a list of ImageTiles (one for both X and Y) for a given direction.
   *
   * @param tiles the list of image tiles
   * @param dir   the direction
   * @param op    the operation type
   * @return the correlation values s.t. corrTriple.x = op(vals.x), corrTriple.y = op(vals.y), and
   * corrTriple.corr is irrelevant
   */
  private static <T> CorrelationTriple computeOp(List<ImageTile<T>> tiles, Direction dir, StatisticUtils.OP_TYPE op) {
    List<CorrelationTriple> corrList = new ArrayList<CorrelationTriple>();

    for (ImageTile<T> t : tiles)
      corrList.add(t.getTranslation(dir));

    return computeOp(corrList, op);

  }

  /**
   * Computes an operation on a set of ImageTiles (one for both X and Y) for a given direction.
   *
   * @param tiles the set of image tiles
   * @param dir   the direction
   * @param op    the operation type
   * @return the correlation values s.t. corrTriple.x = op(vals.x), corrTriple.y = op(vals.y), and
   * corrTriple.corr is irrelevant
   */
  private static <T> CorrelationTriple computeOp(HashSet<ImageTile<T>> tiles, Direction dir,
                                                 StatisticUtils.OP_TYPE op) {
    List<CorrelationTriple> corrList = new ArrayList<CorrelationTriple>();

    for (ImageTile<T> t : tiles)
      corrList.add(t.getTranslation(dir));

    return computeOp(corrList, op);
  }


}
