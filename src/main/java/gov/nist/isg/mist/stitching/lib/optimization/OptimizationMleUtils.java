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


  private static final int MLE_GRID_SEARCH_SIZE_PER_SIDE = 5;
  private static final double SQRT2PI = Math.sqrt(2*Math.PI);


  /**
   * Calculates the overlap for a given direction using Maximum Likelihood Estimation
   *
   * @param grid the grid of image tiles
   * @param dir the direction
   * @param dispValue the displacement value
   * @param pou the percent overlap uncertainty
   * @return the overlap
   * @throws GlobalOptimizationException thrown if no valid tiles are found
   */
  public static <T> double getOverlapMLE(TileGrid<ImageTile<T>> grid, OptimizationUtils.Direction dir, OptimizationUtils.DisplacementValue dispValue, double pou) throws GlobalOptimizationException,
                                                                                                                                                                         FileNotFoundException {
    Log.msg(Log.LogType.INFO, "Computing overlap for " + dir.name()
                              + " direction using Maximum Likelihood Estimation.");


    // TODO update this to use multipoint hill climbing (n points in a grid)
//    using n starting points, run hill climbing n times.
//    half of the n points are randomly selected starting points, the other half form a grid within the valid search space.
//    store the already computed ncc points so that if multiple hill climbs hit the same place it wont recompute.


    // get valid range for translations given the direction
    int range = OptimizationUtils.getOverlapRange(grid, dispValue);

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
              if (t > 0 && t < range
                  && tile.getNorthTranslation().getCorrelation() >= OptimizationUtils.getCorrelationThreshold())
                translations.add(t);

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
              if (t > 0 && t < range
                  && tile.getWestTranslation().getCorrelation() >= OptimizationUtils.getCorrelationThreshold())
                translations.add(t);
            }
            break;
        }
      }
    }

    double overlap;
    try{
      overlap = getOverlapFromMultipointMleHillClimb(translations, range, pou);
    }catch(GlobalOptimizationException e) {
      throw new GlobalOptimizationException("Unable to compute overlap for " + dir.name() + ", translation list is empty.");
    }
    return overlap;

  }




  /**
   * Calculates the overlap for a given direction using Maximum Likelihood Estimation
   *
   * @param translations List of the translations to use in estimating overlap
   * @param range the valid range of translations
   * @param pou the percent overlap uncertainty
   * @return the overlap
   * @throws GlobalOptimizationException thrown if no valid tiles are found

   */
  public static double getOverlapFromMultipointMleHillClimb(List<Integer> translations, int range,
                                                            double pou) throws GlobalOptimizationException {

    if (translations.size() < 1) {
      throw new GlobalOptimizationException("Unable to compute overlap, translation list is empty.");
    }

    // extract the translations into an primitive array
    double[] T = new double[translations.size()];
    for(int i = 0; i < translations.size(); i++)
      T[i] = translations.get(i);



    // setup search bounds for sigma tied to the range
    int smax;
    if(Double.isNaN(pou)) {
      smax = Math.round(range/2);
    }else {
      smax = (int) Math.round((pou / 100) * range);
    }


    // init MLE model parameters
    MLEPoint bestPoint = new MLEPoint(-1,-1,-1,Double.NEGATIVE_INFINITY);
    List<MLEPoint> hcFoundPoints = new ArrayList<MLEPoint>();


    // TODO fix the search as it is not working
    // loop over the grid of starting points
    int pSkip = Math.round(100/MLE_GRID_SEARCH_SIZE_PER_SIDE);
    int mSkip = Math.round(range/MLE_GRID_SEARCH_SIZE_PER_SIDE);
    int sSkip = Math.round(smax/MLE_GRID_SEARCH_SIZE_PER_SIDE);
    for(int p = 1; p < 100; p += pSkip) {
      for (int m = 1; m < range; m += mSkip) {
        for (int s = 1; s < smax; s += sSkip) {

          MLEPoint stop = performMleHillClimb(T, new MLEPoint(p,m,s,Double.NEGATIVE_INFINITY), range, smax);
          hcFoundPoints.add(stop);

          if(stop.likelihood > bestPoint.likelihood) {
            bestPoint = stop;
          }
        }
      }
    }


    // TODO find the number of hc iterations that found bestPoint
//    System.out.println("MLE multipoint hill climb: " + numConverged + " out of " + totalNumGridPoints);


    return Math.round(100*(range-bestPoint.mu)/range);
  }






  private static MLEPoint performMleHillClimb(double[] T, MLEPoint point, int range, int smax) {

    int pmin = Math.max(1, point.PIuni - 1);
    int pmax = Math.min(99, point.PIuni + 1);
    int mmin = Math.max(1, point.mu - 1);
    int mmax = Math.min(range, point.mu + 1);
    int smin = Math.max(1, point.sigma - 1);

    point.likelihood = Double.NEGATIVE_INFINITY;
    for(int p = pmin; p <= pmax; p++) {
      for(int m = mmin; m <= mmax; m++) {
        for(int s = smin; s <= smax; s++) {
          double l = computeMLEValue(T, p, m, s, range);
          if(l > point.likelihood) {
            point.PIuni = p;
            point.mu = m;
            point.sigma = s;
            point.likelihood = l;
          }
        }
      }
    }


    return point;
  }



  private static double computeMLEValue(double[] T, double PIuni, double mu, double sigma, double range) {

    PIuni = PIuni/100;
    // loop over the elements the x array
    double likelihood = 0; // init sum value
    for(int i = 0; i < T.length; i++) {
      double temp = (T[i] - mu) / sigma;
      temp = Math.exp(-0.5 * temp * temp);
      likelihood = likelihood + Math.log(Math.abs((PIuni/range) + (1-PIuni)*(temp / (SQRT2PI * sigma))));
    }

    return likelihood;
  }



}
