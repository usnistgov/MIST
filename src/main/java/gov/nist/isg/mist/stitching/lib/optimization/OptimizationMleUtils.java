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
  private static final double SQRT2PI = Math.sqrt(2*Math.PI);


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
              switch(dispValue) {
                case X:
                  t = tile.getNorthTranslation().getX();
                  break;
                case Y:
                  t = tile.getNorthTranslation().getY();
                  break;
              }

              if(filterLowCorrelationTranslations) {
                if (t > 0 && t < range
                    && tile.getNorthTranslation().getCorrelation() >= OptimizationUtils.getCorrelationThreshold())
                  translations.add(t);
              }else {
                if (t > 0 && t < range)
                  translations.add(t);
              }

            }
            break;
          case West:
            if (tile.getWestTranslation() != null) {
              int t = 0;
              switch(dispValue) {
                case X:
                  t = tile.getWestTranslation().getX();
                  break;
                case Y:
                  t = tile.getWestTranslation().getY();
                  break;
              }
              if(filterLowCorrelationTranslations) {
                if (t > 0 && t < range
                    && tile.getWestTranslation().getCorrelation() >= OptimizationUtils.getCorrelationThreshold())
                  translations.add(t);
              }else {
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


  // TODO update the javadoc for the modified functions
  /**
   * Calculates the overlap for a given direction using Maximum Likelihood Estimation
   *
   * @param grid the grid of image tiles
   * @param dir the direction
   * @param dispValue the displacement value
   * @return the overlap
   * @throws GlobalOptimizationException thrown if no valid tiles are found
   */
  public static <T> double getMleModelMu(TileGrid<ImageTile<T>> grid,
                                         OptimizationUtils.Direction dir,
                                         OptimizationUtils.DisplacementValue dispValue) throws FileNotFoundException {
    Log.msg(Log.LogType.INFO, "Computing overlap for " + dir.name()
                              + " direction using Maximum Likelihood Estimation.");

    // get valid range for translations given the direction
    int range = OptimizationUtils.getOverlapRange(grid, dispValue);

    List<Integer> translations = getTranslationsFromGrid(grid, dir, dispValue, range, false);
//    if(translations.size() < 50) {
//      System.out.println("To few translations above correlation threshold; fitting MLE model to all translations.");
//      translations = getTranslationsFromGrid(grid, dir, dispValue, range, false);
//    }


    MuSigmaTuple mleModel = new MuSigmaTuple(Double.NaN, Double.NaN);
    try{
      mleModel = getMleModelFromMultipointHillClimb(translations, range);
    }catch(GlobalOptimizationException e) {
      Log.msg(Log.LogType.MANDATORY, e.getMessage());
    }
    return mleModel.mu;
  }




  /**
   * Calculates the overlap for a given direction using Maximum Likelihood Estimation
   *
   * @param translations List of the translations to use in estimating overlap
   * @param range the valid range of translations
   * @return the overlap
   * @throws GlobalOptimizationException thrown if no valid tiles are found

   */
  public static MuSigmaTuple getMleModelFromMultipointHillClimb(List<Integer> translations,
                                                                int range) throws GlobalOptimizationException {

    if (translations.size() < 1) {
      throw new GlobalOptimizationException("Unable to compute overlap, translation list is empty.");
    }


    // TODO clean up this function to make is more readable
    // TODO remove all of the print statements, convert  the relevant ones to logger
    // TODO add in the percent MLE convergence to the stats file as an indicator of the quality of the translations
    long startTime = System.currentTimeMillis();

    // extract the translations into an primitive array
    double[] T = new double[translations.size()];
    for(int i = 0; i < translations.size(); i++)
      T[i] = translations.get(i);

    // init MLE model parameters
    MLEPoint bestPoint = new MLEPoint(-1,-1,-1,Double.NEGATIVE_INFINITY);
    List<MLEPoint> hcFoundPoints = new ArrayList<MLEPoint>();

    // perform hill climbing as percentage of range and sigma in range/4
    double factor = ((double)range)/100;


    // allocate and init the matrix to hold cached likelihood values
    double[][][] likelihoodValues = new double[100][100][25];
    for(int p = 0; p < 100; p++) {
      for (int m = 0; m < 100; m++) {
        for (int s = 0; s < 25; s++) {
          likelihoodValues[p][m][s] = Double.NaN;
        }
      }
    }

    // loop over the grid of starting points
    int pSkip = Math.round(100/MLE_GRID_SEARCH_SIZE_PER_SIDE);
    int mSkip = Math.round(100/MLE_GRID_SEARCH_SIZE_PER_SIDE);
    int sSkip = Math.round(25/MLE_GRID_SEARCH_SIZE_PER_SIDE);
    for(int p = 1; p < 100; p += pSkip) {
      for (int m = 0; m < 100; m += mSkip) {
        for (int s = 1; s < 25; s += sSkip) {
          // *********************************
          // Perform hill climbing

          MLEPoint point = new MLEPoint(p,m,s,Double.NEGATIVE_INFINITY);
          MLEPoint temp = new MLEPoint(p,m,s, Double.NEGATIVE_INFINITY);
          boolean done = false;

          while(!done) {
            int pmin = Math.max(1, temp.PIuni - 1);
            int pmax = Math.min(100-1, temp.PIuni + 1);
            int mmin = Math.max(0, temp.mu - 1);
            int mmax = Math.min(100-1, temp.mu + 1);
            int smin = Math.max(0, temp.sigma - 1);
            int smax = Math.min(25-1, temp.sigma + 1);

            for (int hcP = pmin; hcP <= pmax; hcP++) {
              for (int hcM = mmin; hcM <= mmax; hcM++) {
                for (int hcS = smin; hcS <= smax; hcS++) {
                  // check for a cached value first
                  double l = likelihoodValues[hcP][hcM][hcS];
                  if(Double.isNaN(l)) {
                    l = computeMLEValue(T, hcP, factor*hcM, factor*hcS, range);
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

            if(temp.likelihood > point.likelihood) {
              // record current best
              point.PIuni = temp.PIuni;
              point.mu = temp.mu;
              point.sigma = temp.sigma;
              point.likelihood = temp.likelihood;
            }else{
              done = true;
            }
          }
          // end hill climbing
          // *********************************

          // add the point hill climbing found to the list
          hcFoundPoints.add(point);
          if(point.likelihood > bestPoint.likelihood) {
            bestPoint = point;
          }
        }
      }
    }


    int numHillClimbs = hcFoundPoints.size();
    int numCorrectlyFoundPoints = 0;
    for(MLEPoint p : hcFoundPoints) {
      if(p.PIuni == bestPoint.PIuni && p.mu == bestPoint.mu && p.sigma == bestPoint.sigma)
        numCorrectlyFoundPoints++;
    }


    // convert from percent of range to translation values
    bestPoint.mu = (int)Math.round(factor*bestPoint.mu);
    bestPoint.sigma = (int)Math.round(factor*bestPoint.sigma);

    // perform refinement hill climb
    boolean done = false;
    MLEPoint temp = new MLEPoint(bestPoint.PIuni, bestPoint.mu, bestPoint.sigma, bestPoint.likelihood);

    while(!done) {
      int pmin = Math.max(1, temp.PIuni - 1);
      int pmax = Math.min(100 - 1, temp.PIuni + 1);
      int mmin = Math.max(0, temp.mu - 1);
      int mmax = Math.min(range-1, temp.mu + 1);
      int smin = Math.max(0, temp.sigma - 1);
      int smax = Math.min(range/4, temp.sigma + 1);

      for (int hcP = pmin; hcP <= pmax; hcP++) {
        for (int hcM = mmin; hcM <= mmax; hcM++) {
          for (int hcS = smin; hcS <= smax; hcS++) {
            // check for a cached value first
            double l = computeMLEValue(T, hcP, hcM, hcS, range);

            if (l > temp.likelihood) {
              temp.PIuni = hcP;
              temp.mu = hcM;
              temp.sigma = hcS;
              temp.likelihood = l;
            }
          }
        }
      }

      if(temp.likelihood > bestPoint.likelihood) {
        // record current best
        bestPoint.PIuni = temp.PIuni;
        bestPoint.mu = temp.mu;
        bestPoint.sigma = temp.sigma;
        bestPoint.likelihood = temp.likelihood;
      }else{
        done = true;
      }

    }


    long endTime = System.currentTimeMillis();
    System.out.println("MLE multipoint hill climb took: " + (endTime-startTime) + "ms for grid of "
                       + MLE_GRID_SEARCH_SIZE_PER_SIDE + "x"
                       + MLE_GRID_SEARCH_SIZE_PER_SIDE + "x"
                       + MLE_GRID_SEARCH_SIZE_PER_SIDE + ", "
                       + (100*numCorrectlyFoundPoints/numHillClimbs) + "% converged");


    System.out.println("mu = " + bestPoint.mu + " sigma = " + bestPoint.sigma + " PIuni = " + bestPoint.PIuni);

    return new MuSigmaTuple(bestPoint.mu, bestPoint.sigma);
  }



  private static double computeMLEValue(double[] T, double PIuni, double mu, double sigma, double range) {

    PIuni = PIuni/100;
    // loop over the elements the x array
    double likelihood = 0; // init sum value
    for(int i = 0; i < T.length; i++) {
      double temp = (T[i] - mu) / sigma;
      temp = Math.exp(-0.5 * temp * temp);
      temp = temp / (SQRT2PI * sigma);
      temp = (PIuni/range) + (1 - PIuni) * temp;
      temp = Math.abs(temp);
      temp = Math.log(temp);
      likelihood = likelihood + temp;
    }

    return likelihood;
  }



}
