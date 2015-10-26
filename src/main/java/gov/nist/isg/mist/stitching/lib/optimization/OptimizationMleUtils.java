package gov.nist.isg.mist.stitching.lib.optimization;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import gov.nist.isg.mist.stitching.lib.exceptions.GlobalOptimizationException;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;

/**
 * Created by mmajursk on 7/16/2015.
 */
public class OptimizationMleUtils {


  private static final int MLE_GRID_SEARCH_SIZE_PER_SIDE = 4;
  private static final double SQRT2PI = Math.sqrt(2 * Math.PI);

  /**
   * Calculates the overlap for a given direction using Maximum Likelihood Estimation
   *
   * @param grid      the grid of image tiles
   * @param dir       the direction
   * @param dispValue the displacement value
   * @return the overlap
   * @throws GlobalOptimizationException thrown if no valid tiles are found
   */
  public static <T> double getOverlapMle(TileGrid<ImageTile<T>> grid,
                                         OptimizationUtils.Direction dir,
                                         OptimizationUtils.DisplacementValue dispValue) throws FileNotFoundException {
    Log.msg(Log.LogType.INFO, "Computing overlap for " + dir.name()
        + " direction using Maximum Likelihood Estimation.");

    // get valid range for translations given the direction
    int range = OptimizationUtils.getOverlapRange(grid, dispValue);

    List<Integer> translations = getTranslationsFromGrid(grid, dir, dispValue, range, false);


    MuSigmaTuple mleModel = new MuSigmaTuple(Double.NaN, Double.NaN);
    try {
      Log.msg(Log.LogType.INFO, "Translation used for MLE " + dir + ":");
      Log.msg(Log.LogType.INFO, translations.toString());
      mleModel = getMleModelFromMultipointHillClimb(translations, range);
      Log.msg(Log.LogType.HELPFUL, "MLE " + dir + " model parameters: mu=" + mleModel.mu + " sigma=" + mleModel.sigma);
    } catch (GlobalOptimizationException e) {
      Log.msg(Log.LogType.MANDATORY, e.getMessage());
    }
    return mleModel.mu;
  }


  /**
   * Calculates the overlap for a given direction using Maximum Likelihood Estimation
   *
   * @param translations List of the translations to use in estimating overlap
   * @param range        the valid range of translations
   * @return the MLE model parameters mu and sigma
   * @throws GlobalOptimizationException thrown if no valid tiles are found
   */
  public static MuSigmaTuple getMleModelFromMultipointHillClimb(List<Integer> translations,
                                                                int range) throws GlobalOptimizationException {

    if (translations.size() < 1) {
      throw new GlobalOptimizationException("Unable to compute overlap, translation list is empty.");
    }

    // extract the translations into an primitive array
    double[] T = new double[translations.size()];
    for (int i = 0; i < translations.size(); i++)
      T[i] = translations.get(i);


    // init MLE model parameters
    MLEPoint bestPoint = new MLEPoint(-1, -1, -1, Double.NEGATIVE_INFINITY);

    // perform hill climbing as percentage of range and sigma
    double factor = ((double) range) / 100;

    // allocate and init the matrix to hold cached likelihood values
    double[][][] likelihoodValues = new double[100][100][25];
    for (int p = 0; p < 100; p++) {
      for (int m = 0; m < 100; m++) {
        for (int s = 0; s < 25; s++) {
          likelihoodValues[p][m][s] = Double.NaN;
        }
      }
    }


    // limit the PIuni values 0:100 at resolution of 1%
    int[] pVals = new int[100];
    for (int i = 0; i < pVals.length; i++)
      pVals[i] = i;
    // limit the mu values 0:100 at resolution of 1%
    int[] mVals = new int[100];
    for (int i = 0; i < mVals.length; i++)
      mVals[i] = i;
    // limit sigma possible values to 0:25 at resolution of 1%
    int[] sVals = new int[25];
    for (int i = 0; i < sVals.length; i++)
      sVals[i] = i;


    // setup the grid of hill climb starting points
    int pSkip = Math.round(pVals.length / MLE_GRID_SEARCH_SIZE_PER_SIDE);
    int mSkip = Math.round(mVals.length / MLE_GRID_SEARCH_SIZE_PER_SIDE);
    int sSkip = Math.round(sVals.length / MLE_GRID_SEARCH_SIZE_PER_SIDE);
    // to start the grid search in the middle of the box
    int deltaP = pSkip / 2;
    int deltaM = mSkip / 2;
    int deltaS = sSkip / 2;

    // loop over the grid of starting points
    for (int p = deltaP; p < pVals.length; p += pSkip) {
      for (int m = deltaM; m < mVals.length; m += mSkip) {
        for (int s = deltaS; s < sVals.length; s += sSkip) {
          // *********************************
          // Perform hill climbing
          MLEPoint point = new MLEPoint(pVals[p], mVals[m], sVals[s], Double.NEGATIVE_INFINITY);
          MLEPoint temp = new MLEPoint(pVals[p], mVals[m], sVals[s], Double.NEGATIVE_INFINITY);
          boolean done = false;

          while (!done) {
            // setup search bounds
            int pmin = Math.max(1, temp.PIuni - 1);
            int pmax = Math.min(pVals.length - 1, temp.PIuni + 1);
            int mmin = Math.max(0, temp.mu - 1);
            int mmax = Math.min(mVals.length - 1, temp.mu + 1);
            int smin = Math.max(0, temp.sigma - 1);
            int smax = Math.min(sVals.length - 1, temp.sigma + 1);

            // loop over the local neighborhood
            for (int hcP = pmin; hcP <= pmax; hcP++) {
              for (int hcM = mmin; hcM <= mmax; hcM++) {
                for (int hcS = smin; hcS <= smax; hcS++) {
                  // check for a cached value first
                  double l = likelihoodValues[hcP][hcM][hcS];
                  if (Double.isNaN(l)) {
                    l = computeMLELikelihood(T, pVals[hcP], factor * mVals[hcM],
                        factor * sVals[hcS], range);
                    likelihoodValues[hcP][hcM][hcS] = l;
                  }

                  if (l > temp.likelihood) {
                    temp.PIuni = hcP;
                    temp.mu = hcM;
                    temp.sigma = hcS;
                    temp.likelihood = l;
                  }
                }
              }
            }

            // if the best local neighborhood point is better
            if (temp.likelihood > point.likelihood) {
              // record current best
              point.PIuni = temp.PIuni;
              point.mu = temp.mu;
              point.sigma = temp.sigma;
              point.likelihood = temp.likelihood;
            } else {
              done = true;
            }
          }
          // end hill climbing
          // *********************************

          // keep track of the most likely point across all hill climbings
          if (point.likelihood > bestPoint.likelihood) {
            bestPoint = point;
          }
        }
      }
    }


    // convert from percent of range to translation values
    bestPoint.PIuni = pVals[bestPoint.PIuni];
    bestPoint.mu = (int) Math.round(factor * mVals[bestPoint.mu]);
    bestPoint.sigma = (int) Math.round(factor * sVals[bestPoint.sigma]);


    // perform refinement hill climb using pixel level mu and sigma resolution
    boolean done = false;
    MLEPoint temp =
        new MLEPoint(bestPoint.PIuni, bestPoint.mu, bestPoint.sigma, bestPoint.likelihood);

    while (!done) {
      // setup search bounds
      int pmin = Math.max(1, temp.PIuni - 1);
      int pmax = Math.min(100 - 1, temp.PIuni + 1);
      int mmin = Math.max(0, temp.mu - 1);
      int mmax = Math.min(range - 1, temp.mu + 1);
      int smin = Math.max(0, temp.sigma - 1);
      int smax = Math.min(range / 4, temp.sigma + 1);

      // loop over the local neighborhood
      for (int hcP = pmin; hcP <= pmax; hcP++) {
        for (int hcM = mmin; hcM <= mmax; hcM++) {
          for (int hcS = smin; hcS <= smax; hcS++) {
            // check for a cached value first
            double l = computeMLELikelihood(T, hcP, hcM, hcS, range);

            if (l > temp.likelihood) {
              temp.PIuni = hcP;
              temp.mu = hcM;
              temp.sigma = hcS;
              temp.likelihood = l;
            }
          }
        }
      }

      // if the best local neighborhood point is better
      if (temp.likelihood > bestPoint.likelihood) {
        // record current best
        bestPoint.PIuni = temp.PIuni;
        bestPoint.mu = temp.mu;
        bestPoint.sigma = temp.sigma;
        bestPoint.likelihood = temp.likelihood;
      } else {
        done = true;
      }
    }

    Log.msg(Log.LogType.INFO,
        "MLE model parameters: " + "mu = " + bestPoint.mu + " sigma = " + bestPoint.sigma
            + " PIuni = " + bestPoint.PIuni);

    return new MuSigmaTuple(bestPoint.mu, bestPoint.sigma);
  }


  private static double computeMLELikelihood(double[] T, double PIuni, double mu, double sigma,
                                             double range) {

    PIuni = PIuni / 100;
    // loop over the elements the x array
    double likelihood = 0; // init sum value
    for (int i = 0; i < T.length; i++) {
      double temp = (T[i] - mu) / sigma;
      temp = Math.exp(-0.5 * temp * temp);
      temp = temp / (SQRT2PI * sigma);
      temp = (PIuni / range) + (1 - PIuni) * temp;
      temp = Math.abs(temp);
      temp = Math.log(temp);
      likelihood = likelihood + temp;
    }

    return likelihood;
  }


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
