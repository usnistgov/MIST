// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.


package gov.nist.isg.mist.optimization.model.overlap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;

/**
 * Parallel MLE overlap executor.
 *
 * @author Michael Majurski
 */
public class MleOverlapParallelExecutor<T> extends OverlapExecutorInterface<T> {


  private ParallelMlePoint bestPoint;
  private ConcurrentLinkedQueue<MLEPoint> resultsQueue = new ConcurrentLinkedQueue<MLEPoint>();
  private List<MleWorker> workers = new ArrayList<MleWorker>();
  private List<Thread> threads = new ArrayList<Thread>();

  /**
   * Parallel MLE overlap executor.
   *
   * @param grid              the TileGrid from which to compute the overlap.
   * @param direction         the direction in which to compute the overlap.
   * @param displacementValue which displacement component of the specified direction to use in
   *                          computing the overlap.
   * @param numWorkerThreads  the number of worker threads to launch.
   */
  public MleOverlapParallelExecutor(TileGrid<ImageTile<T>> grid,
                                    TileGrid.Direction direction,
                                    TileGrid.DisplacementValue displacementValue,
                                    int numWorkerThreads) {
    super(grid, direction, displacementValue);

    this.resultsQueue = new ConcurrentLinkedQueue<MLEPoint>();
    this.workers = new ArrayList<MleWorker>();
    this.threads = new ArrayList<Thread>();

    // allocate the cache object
    MleLikelihoodCache mleCache = new MleLikelihoodCache();
    this.bestPoint = new ParallelMlePoint();

    double[] trans = getTranslations();
    // convert translations into a percentage of the valid range (so all translations are [0,100])
    double range = getRange();
    for (int i = 0; i < trans.length; i++)
      trans[i] = 100 * trans[i] / range;

    // setup the worker pool
    for (int i = 0; i < numWorkerThreads; i++) {
      MleWorker worker = new MleWorker(bestPoint, this.resultsQueue, trans, mleCache);
      workers.add(worker);
      Thread thread = new Thread(worker);
      // set the workers uncaught exception handler to this class
      thread.setUncaughtExceptionHandler(this);
      threads.add(thread);
    }
  }

  /**
   * Executes the MLE overlap computation.
   */
  @Override
  public void execute() {
    Log.msg(Log.LogType.INFO, "Computing overlap for " + getDirection().name()
        + " direction using Maximum Likelihood Estimation.");

    for (Thread thread : this.threads)
      thread.start();

    for (Thread thread : this.threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        Log.msg(Log.LogType.MANDATORY, "Interrupted MLE Overlap estimation.");
      }
    }

    // get the best result from the workers (this is percentile resolution)
    MLEPoint mleModel = bestPoint.getPoint();

    // get results from workers
    int numberConverged = 0; // to count the number of workers that converged

    if (resultsQueue != null) { // if the workers completed
      // get the results iterator
      Iterator<MLEPoint> itr = resultsQueue.iterator();
      // find the number of starting points which converged to the best answer
      while (itr.hasNext()) {
        MLEPoint point = itr.next();
        // if the current point is the same as the best
        if (mleModel.getMu() == point.getMu() && mleModel.getSigma() == point.getSigma())
          numberConverged++;
      }
    }

    // set the overlap
    this.overlap = 100 - mleModel.getMu();

    Log.msg(Log.LogType.HELPFUL, "MLE " + getDirection() + " model parameters: mu=" + mleModel.getMu()
        + "% sigma=" + mleModel.getSigma() + "%");
    Log.msg(Log.LogType.HELPFUL, "MLE " + getDirection() + " had " + numberConverged + "/" +
        resultsQueue.size() + " hill climbs converge");
  }


  /**
   * Method to get the resulting overlap.
   *
   * @return the overlap value computed. Rounded to 2 decimal places.
   */
  @Override
  public double getOverlap() {
    return ((double) Math.round(100 * this.overlap)) / 100;
  }


  /**
   * Cancels the MLE estimation
   */
  @Override
  public void cancel() {
    Log.msg(Log.LogType.MANDATORY, "Canceling Stage Model Build");
    for (MleWorker worker : workers)
      worker.cancel();

  }


}
