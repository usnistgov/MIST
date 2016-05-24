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

package gov.nist.isg.mist.optimization.model.overlap;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Maximum likelihood estimation worker.
 *
 * @author Michael Majurski
 */
public class MleWorker implements Runnable {


  private volatile boolean isCancelled;

  private double[] T;

  private ParallelMlePoint bestPoint;
  private ConcurrentLinkedQueue<MLEPoint> resultsQueue;
  private MleLikelihoodCache mleCache;

  /**
   * Worker to perform maximum likelihood estimation via hill climbing given a starting point (in
   * percent) within the valid range.
   *
   * @param bestPoint    The thread safe MLEPoint denoting the current best among all the workers
   * @param resultsQueue The output results queue to hold the location of hte local maxima found
   * @param translations The translations to be used in performing MLE estimation. Must be within
   *                     [0,100].
   */
  public MleWorker(ParallelMlePoint bestPoint, ConcurrentLinkedQueue<MLEPoint> resultsQueue,
                   double[] translations, MleLikelihoodCache mleCache) {

    this.bestPoint = bestPoint;
    this.resultsQueue = resultsQueue;
    this.T = translations;
    this.mleCache = mleCache;
    this.isCancelled = false;
  }


  /**
   * Run the MleWorker task
   */
  @Override
  public void run() {

    boolean workerDone = false;
    MLEPoint point;
    while (!workerDone && !this.isCancelled) {

      // get a new random point
      point = MLEPoint.getRandomPoint();

      // Perform hill climbing at percent of range resolution
      point = MleUtils.hillClimbSearch(point, mleCache, T);

      // add the found point to the output queue
      resultsQueue.add(point);

      // add the point to the best point if its better than the global best
      bestPoint.setIfBetter(point);

      // if the best point has been stable for the required number of iterations, stop
      if (bestPoint.getNumStableIterations() > MleUtils.NUMBER_STABLE_MLE_ITERATIONS)
        workerDone = true;
    }
  }

  /**
   * Cancels this task
   */
  public void cancel() {
    this.isCancelled = true;
  }


}
