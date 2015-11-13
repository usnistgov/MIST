package gov.nist.isg.mist.stitching.lib.optimization;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import gov.nist.isg.mist.stitching.lib.exceptions.GlobalOptimizationException;
import gov.nist.isg.mist.stitching.lib.executor.MleExecutor;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;

/**
 * Created by mmajursk on 7/16/2015.
 */
public class OptimizationMleUtils {


  private static final int MLE_GRID_SEARCH_SIZE_PER_SIDE = 4;

  /**
   * Calculates the overlap for a given direction using Maximum Likelihood Estimation
   *
   * @param grid      the grid of image tiles
   * @param dir       the direction
   * @param dispValue the displacement value
   * @param numWorkers the number of CPU workers to use
   * @return the overlap
   * @throws GlobalOptimizationException thrown if no valid tiles are found
   */
  public static <T> double getOverlapMle(TileGrid<ImageTile<T>> grid,
                                         OptimizationUtils.Direction dir,
                                         OptimizationUtils.DisplacementValue dispValue,
                                         int numWorkers) throws FileNotFoundException {

    Log.msg(Log.LogType.INFO, "Computing overlap for " + dir.name()
        + " direction using Maximum Likelihood Estimation.");

    // get valid range for translations given the direction
    int range = OptimizationUtils.getOverlapRange(grid, dispValue);

    List<Integer> translations = getTranslationsFromGrid(grid, dir, dispValue, range, false);

    MuSigmaTuple mleModel = new MuSigmaTuple(Double.NaN, Double.NaN);
    try {
      Log.msg(Log.LogType.INFO, "Translation used for MLE " + dir + ":");
      Log.msg(Log.LogType.INFO, translations.toString());

      MleExecutor executor = new MleExecutor(translations, range, numWorkers, MLE_GRID_SEARCH_SIZE_PER_SIDE);
      executor.execute();
      mleModel = executor.getMuSigmaTuple();

      Log.msg(Log.LogType.HELPFUL, "MLE " + dir + " model parameters: mu=" + mleModel.mu + " sigma=" + mleModel.sigma);
    } catch (GlobalOptimizationException e) {
      Log.msg(Log.LogType.MANDATORY, e.getMessage());
    }
    return mleModel.mu;
  }


  /**
   * Extracts a List of translations from the image grid
   * @param grid the grid to extract tracnslations from
   * @param dir the direction
   * @param dispValue the displacement value
   * @param range the range (maximum dimension of the source image along the dir and dispValue)
   * @param filterLowCorrelationTranslations whether to filter out low correlation translations
   * @param <T>
   * @return List of Integers containing the relevant translations
   */
  private static <T> List<Integer> getTranslationsFromGrid(TileGrid<ImageTile<T>> grid, OptimizationUtils.Direction dir, OptimizationUtils.DisplacementValue dispValue, int range, boolean filterLowCorrelationTranslations) {
    // allocate list to hold the translations
    List<Integer> translations = new ArrayList<Integer>();

    // gather all relevant translations into an array
    for (int row = 0; row < grid.getExtentHeight(); row++) {
      for (int col = 0; col < grid.getExtentWidth(); col++) {
        ImageTile<T> tile = grid.getSubGridTile(row, col);
        switch (dir) {
          case North:
            if (tile.getNorthTranslation() != null) {
              int t = 0;
              switch (dispValue) {
                case X:
                  t = tile.getNorthTranslation().getX();
                  break;
                case Y:
                  t = tile.getNorthTranslation().getY();
                  break;
              }

              if (filterLowCorrelationTranslations) {
                if (t > 0 && t < range
                    && tile.getNorthTranslation().getCorrelation() >= OptimizationUtils.getCorrelationThreshold())
                  translations.add(t);
              } else {
                if (t > 0 && t < range)
                  translations.add(t);
              }

            }
            break;
          case West:
            if (tile.getWestTranslation() != null) {
              int t = 0;
              switch (dispValue) {
                case X:
                  t = tile.getWestTranslation().getX();
                  break;
                case Y:
                  t = tile.getWestTranslation().getY();
                  break;
              }
              if (filterLowCorrelationTranslations) {
                if (t > 0 && t < range
                    && tile.getWestTranslation().getCorrelation() >= OptimizationUtils.getCorrelationThreshold())
                  translations.add(t);
              } else {
                if (t > 0 && t < range)
                  translations.add(t);
              }
            }
            break;
        }
      }
    }

    return translations;
  }


}
