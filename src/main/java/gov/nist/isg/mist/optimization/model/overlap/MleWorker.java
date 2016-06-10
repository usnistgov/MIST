// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.


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
