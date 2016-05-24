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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;

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
        if (mleModel.mu == point.mu && mleModel.sigma == point.sigma)
          numberConverged++;
      }
    }

    // set the overlap
    this.overlap = 100 - mleModel.mu;

    Log.msg(Log.LogType.HELPFUL, "MLE " + getDirection() + " model parameters: mu=" + mleModel.mu
        + "% sigma=" + mleModel.sigma + "%");
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
