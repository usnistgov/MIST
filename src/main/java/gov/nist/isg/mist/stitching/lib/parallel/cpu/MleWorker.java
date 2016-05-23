package gov.nist.isg.mist.stitching.lib.parallel.cpu;

import java.util.concurrent.ConcurrentLinkedQueue;

import gov.nist.isg.mist.stitching.lib.executor.MleExecutor;
import gov.nist.isg.mist.stitching.lib.optimization.MLEPoint;

/**
 * Created by mmajursk on 11/13/2015.
 */
public class MleWorker implements Runnable {


  private static final double SQRT2PI = Math.sqrt(2 * Math.PI);

  private volatile boolean isCancelled;

  private double[] T;
  private int range;
  private double factor;

  private ConcurrentLinkedQueue<MLEPoint> workQueue;
  private ConcurrentLinkedQueue<MLEPoint> resultsQueue;

  /**
   * Worker to perform maximum likelihood estimatino via hill climbing given a starting point (in percent) within the valid range.
   * @param workQueue The queue of starting points to perform hill climbing from
   * @param resultsQueue The output results queue to hold the location of hte local maxima found
   * @param translations The translations to be used in performing MLE estimation
   * @param range The valid range for the current direction and dispValue. This is the original input image dimension for the current direction.
   */
  public MleWorker(ConcurrentLinkedQueue<MLEPoint> workQueue, ConcurrentLinkedQueue<MLEPoint> resultsQueue,
                   double[] translations, int range) {

    this.workQueue = workQueue;
    this.resultsQueue = resultsQueue;
    this.T = translations;

    this.range = range;

    // perform hill climbing as percentage of range
    factor = ((double) range) / 100;

    this.isCancelled = false;
  }


  @Override
  public void run() {

    boolean workerDone = false;
    while (!workerDone && !this.isCancelled) {
      MLEPoint point = workQueue.poll();

      if (point == null) {
        // Shut this worker down because it is done
        workerDone = true;

      } else {
        // the poll was successful at getting an element

        // Perform hill climbing at percent of range resolution
        MLEPoint bestPoint = performPercResolutionHillClimb(point);

        // convert from percent of range to translation values
        bestPoint.mu = (int) Math.round(factor * bestPoint.mu);
        bestPoint.sigma = (int) Math.round(factor * bestPoint.sigma);

        // perform hill climbing at pixel resolution
        bestPoint = performPixelResolutionHillClimb(bestPoint);

        // add the found point to the output queue
        resultsQueue.add(bestPoint);
      }
    }

  }

  /**
   * Cancels this task
   */
  public void cancel() {
    this.isCancelled = true;
  }


  private MLEPoint performPercResolutionHillClimb(MLEPoint startingPoint) {

    // allocate and init the matrix to hold cached likelihood values
    double[][][] likelihoodValues = new double[100][100][MleExecutor.MAX_SIGMA_PERCENT];
    for (int p = 0; p < 100; p++) {
      for (int m = 0; m < 100; m++) {
        for (int s = 0; s < 25; s++) {
          likelihoodValues[p][m][s] = Double.NaN;
        }
      }
    }


    // init the MLE points
    MLEPoint bestPoint = new MLEPoint(startingPoint.PIuni, startingPoint.mu, startingPoint.sigma, Double.NEGATIVE_INFINITY);
    MLEPoint temp = new MLEPoint(startingPoint.PIuni, startingPoint.mu, startingPoint.sigma, Double.NEGATIVE_INFINITY);
    boolean done = false;

    while (!done) {
      for (int pDelta = -1; pDelta <= 1; pDelta++) {
        for (int mDelta = -1; mDelta <= 1; mDelta++) {
          for (int sDelta = -1; sDelta <= 1; sDelta++) {

            int curP = bestPoint.PIuni + pDelta;
            int curM = bestPoint.mu + mDelta;
            int curS = bestPoint.sigma + sDelta;

            // Check if this point is within the search bounds
            if (curP > 1 && curP < 100 && curM > 1 && curM < 100 && curS > 1 && curS < MleExecutor.MAX_SIGMA_PERCENT) {
              // look up the stored value
              double l = likelihoodValues[curP][curM][curS];
              if (Double.isNaN(l)) {
                l = computeMLELikelihood(T, curP, factor * curM,
                    factor * curS, range);
                likelihoodValues[curP][curM][curS] = l;
              }
              if (l > temp.likelihood) {
                temp.PIuni = curP;
                temp.mu = curM;
                temp.sigma = curS;
                temp.likelihood = l;
              }
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

    return bestPoint;
  }


  private MLEPoint performPixelResolutionHillClimb(MLEPoint bestPoint) {

    // perform refinement hill climb using pixel level mu and sigma resolution
    boolean done = false;
    MLEPoint temp = new MLEPoint(bestPoint.PIuni, bestPoint.mu, bestPoint.sigma, bestPoint.likelihood);
    double maxSigma = factor * MleExecutor.MAX_SIGMA_PERCENT;

    while (!done) {
      for (int pDelta = -1; pDelta <= 1; pDelta++) {
        for (int mDelta = -1; mDelta <= 1; mDelta++) {
          for (int sDelta = -1; sDelta <= 1; sDelta++) {

            int hcP = bestPoint.PIuni + pDelta;
            int hcM = bestPoint.mu + mDelta;
            int hcS = bestPoint.sigma + sDelta;
            // Check if this point is within the search bounds
            if (hcP > 1 && hcP < 100 && hcM > 1 && hcM < range && hcS > 1 && hcS < maxSigma) {
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

    return bestPoint;
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

}
