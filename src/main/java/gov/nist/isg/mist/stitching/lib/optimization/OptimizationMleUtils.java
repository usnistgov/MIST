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
   * @return the overlap
   * @throws GlobalOptimizationException thrown if no valid tiles are found
   */
  public static <T> MuSigmaTuple getOverlapMLE(TileGrid<ImageTile<T>> grid, OptimizationUtils.Direction dir, OptimizationUtils.DisplacementValue dispValue) throws GlobalOptimizationException,
                                                                                                                                                                         FileNotFoundException {
    Log.msg(Log.LogType.INFO, "Computing overlap for " + dir.name()
                              + " direction using Maximum Likelihood Estimation.");


    // TODO update this to use multipoint hill climbing (nxnxn grid)
//    run hill climbing nxnxn times with different starting points in a grid
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

    MuSigmaTuple mleModel;
    try{
      mleModel = getOverlapFromMultipointMleHillClimb(translations, range);
    }catch(GlobalOptimizationException e) {
      throw new GlobalOptimizationException("Unable to compute overlap for " + dir.name() + ", translation list is empty.");
    }
    return mleModel;

  }




  /**
   * Calculates the overlap for a given direction using Maximum Likelihood Estimation
   *
   * @param translations List of the translations to use in estimating overlap
   * @param range the valid range of translations
   * @return the overlap
   * @throws GlobalOptimizationException thrown if no valid tiles are found

   */
  public static MuSigmaTuple getOverlapFromMultipointMleHillClimb(List<Integer> translations, int range) throws GlobalOptimizationException {

    if (translations.size() < 1) {
      throw new GlobalOptimizationException("Unable to compute overlap, translation list is empty.");
    }

    long startTime = System.currentTimeMillis();

    // extract the translations into an primitive array
    double[] T = new double[translations.size()];
    for(int i = 0; i < translations.size(); i++)
      T[i] = translations.get(i);


    // init MLE model parameters
    MLEPoint bestPoint = new MLEPoint(-1,-1,-1,Double.NEGATIVE_INFINITY);
    List<MLEPoint> hcFoundPoints = new ArrayList<MLEPoint>();


    // the hill climbing is done on half resolution, where only even values of PIuni, mu, and sigma are checked for computational speed
    // setup arrays of PIuni,mu,sigma values
    int[] pVals = new int[100/2];
    int[] mVals = new int[range/2];
    int[] sVals = new int[(range/4)/2];
    for(int i = 0; i < 100; i=i+2) pVals[i/2] = i;
    for(int i = 0; i < range; i=i+2) mVals[i/2] = i;
    for(int i = 0; i < (range/4); i=i+2) sVals[i/2] = i;


    // allocate and init the matrix to hold cached likelihood values
    double[][][] likelihoodValues = new double[pVals.length][mVals.length][sVals.length];
    for(int p = 0; p < pVals.length; p++) {
      for (int m = 0; m < mVals.length; m++) {
        for (int s = 0; s < sVals.length; s++) {
          likelihoodValues[p][m][s] = Double.NaN;
        }
      }
    }

    // loop over the grid of starting points
    int pSkip = Math.round(pVals.length/MLE_GRID_SEARCH_SIZE_PER_SIDE);
    int mSkip = Math.round(mVals.length/MLE_GRID_SEARCH_SIZE_PER_SIDE);
    int sSkip = Math.round(sVals.length/MLE_GRID_SEARCH_SIZE_PER_SIDE);
    for(int p = 1; p < pVals.length; p += pSkip) {
      for (int m = 1; m < mVals.length; m += mSkip) {
        for (int s = 1; s < sVals.length; s += sSkip) {
          // *********************************
          // Perform hill climbing

          MLEPoint point = new MLEPoint(p,m,s,Double.NEGATIVE_INFINITY);
          MLEPoint temp = new MLEPoint(p,m,s, Double.NEGATIVE_INFINITY);
          boolean done = false;

          while(!done) {
            int pmin = Math.max(1, temp.PIuni - 1);
            int pmax = Math.min(pVals.length-1, temp.PIuni + 1);
            int mmin = Math.max(1, temp.mu - 1);
            int mmax = Math.min(mVals.length-1, temp.mu + 1);
            int smin = Math.max(1, temp.sigma - 1);
            int smax = Math.min(sVals.length-1, temp.sigma + 1);

            for (int hcP = pmin; hcP <= pmax; hcP++) {
              for (int hcM = mmin; hcM <= mmax; hcM++) {
                for (int hcS = smin; hcS <= smax; hcS++) {
                  // check for a cached value first
                  double l = likelihoodValues[hcP][hcM][hcS];
                  if(Double.isNaN(l)) {
                    l = computeMLEValue(T, pVals[hcP], mVals[hcM], sVals[hcS], range);
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


    // convert from half resolution to full using the lookup vectors
    bestPoint.PIuni = pVals[bestPoint.PIuni];
    bestPoint.mu = mVals[bestPoint.mu];
    bestPoint.sigma = sVals[bestPoint.sigma];


    // refine half resolution hill climbing estimate which is within 1 of the correct answer
    int pmin = Math.max(1, bestPoint.PIuni - 1);
    int pmax = Math.min(100, bestPoint.PIuni + 1);
    int mmin = Math.max(1, bestPoint.mu - 1);
    int mmax = Math.min(range, bestPoint.mu + 1);
    int smin = Math.max(1, bestPoint.sigma - 1);
    int smax = Math.min((range/4), bestPoint.sigma + 1);

    for (int hcP = pmin; hcP <= pmax; hcP++) {
      for (int hcM = mmin; hcM <= mmax; hcM++) {
        for (int hcS = smin; hcS <= smax; hcS++) {
          double l = computeMLEValue(T, hcP, hcM, hcS, range);

          if (l > bestPoint.likelihood) {
            bestPoint.PIuni = hcP;
            bestPoint.mu = hcM;
            bestPoint.sigma = hcS;
            bestPoint.likelihood = l;
          }
        }
      }
    }


    long endTime = System.currentTimeMillis();
    System.out.println("MLE multipoint hill climb took: " + (endTime-startTime) + "ms for grid of "
                       + MLE_GRID_SEARCH_SIZE_PER_SIDE + "x"
                       + MLE_GRID_SEARCH_SIZE_PER_SIDE + "x"
                       + MLE_GRID_SEARCH_SIZE_PER_SIDE + ", "
                       + (100*numCorrectlyFoundPoints/numHillClimbs) + "% converged");


    System.out.println("PIuni = " + bestPoint.PIuni + " mu = " + bestPoint.mu + " sigma = " + bestPoint.sigma + " likelihood = " + bestPoint.likelihood);

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
