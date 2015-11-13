package gov.nist.isg.mist.stitching.lib.executor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import gov.nist.isg.mist.stitching.lib.exceptions.GlobalOptimizationException;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.optimization.MLEPoint;
import gov.nist.isg.mist.stitching.lib.optimization.MuSigmaTuple;
import gov.nist.isg.mist.stitching.lib.parallel.cpu.MleWorker;

/**
 * Created by mmajursk on 11/13/2015.
 */
public class MleExecutor implements Thread.UncaughtExceptionHandler {

  public static final int MAX_SIGMA_PERCENT = 25;

  private boolean exceptionThrown;
  private Throwable workerThrowable;

  private ConcurrentLinkedQueue<MLEPoint> workQueue = new ConcurrentLinkedQueue<MLEPoint>();
  private ConcurrentLinkedQueue<MLEPoint> resultsQueue = new ConcurrentLinkedQueue<MLEPoint>();

  private List<MleWorker> workers = new ArrayList<MleWorker>();
  private List<Thread> threads = new ArrayList<Thread>();

  private boolean completedMleSearch;

  public MleExecutor(List<Integer> translations, int range, int numWorkers, int numGridPointsPerSize) throws GlobalOptimizationException {

    this.completedMleSearch = false;


    // extract the translations into an primitive array
    double[] T = new double[translations.size()];
    for (int i = 0; i < translations.size(); i++)
      T[i] = translations.get(i);

    if (translations.size() < 1) {
      throw new GlobalOptimizationException("Unable to compute overlap, translation list is empty.");
    }


    this.workQueue = new ConcurrentLinkedQueue<MLEPoint>();
    this.resultsQueue = new ConcurrentLinkedQueue<MLEPoint>();

    this.workers = new ArrayList<MleWorker>();
    this.threads = new ArrayList<Thread>();

    // setup the worker pool
    for (int i = 0; i < numWorkers; i++) {
      MleWorker worker = new MleWorker(this.workQueue, this.resultsQueue, T, range);
      workers.add(worker);
      Thread thread = new Thread(worker);
      thread.setUncaughtExceptionHandler(this);
      threads.add(thread);
    }

    // setup the work queue
    // setup the grid of hill climb starting points
    int pSkip = Math.round(100 / numGridPointsPerSize);
    int mSkip = Math.round(100 / numGridPointsPerSize);
    int sSkip = Math.round(MAX_SIGMA_PERCENT / numGridPointsPerSize);
    // to start the grid search in the middle of the box
    int pStart = pSkip / 2;
    int mStart = mSkip / 2;
    int sStart = sSkip / 2;

    // loop over the grid of starting points
    for (int p = pStart; p < 100; p += pSkip) {
      for (int m = mStart; m < 100; m += mSkip) {
        for (int s = sStart; s < MAX_SIGMA_PERCENT; s += sSkip) {
          this.workQueue.add(new MLEPoint(p, m, s, Double.NaN));
        }
      }
    }
  }

  /**
   * Get the best MuSigma tuple found during the multipoint hill climbing optimization
   * @return
   */
  public MuSigmaTuple getMuSigmaTuple() {
    if (resultsQueue == null || resultsQueue.isEmpty() || !completedMleSearch)
      return null;

    MLEPoint bestPoint = null;

    Iterator<MLEPoint> itr = resultsQueue.iterator();
    while (itr.hasNext()) {
      MLEPoint point = itr.next();
      if (bestPoint == null || bestPoint.likelihood < point.likelihood) {
        bestPoint = point;
      }
    }
    return new MuSigmaTuple(bestPoint.mu, bestPoint.sigma);
  }


  /**
   * Executes the threads to initiate MLE estimation
   */
  public void execute() {
    for (Thread thread : this.threads)
      thread.start();

    for (Thread thread : this.threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        Log.msg(Log.LogType.MANDATORY, "Interrupted MLE Overlap estimation.");
      }
    }
    this.completedMleSearch = true;
  }

  /**
   * Cancels the MLE estimation
   */
  public void cancel() {
    for (MleWorker worker : workers)
      worker.cancel();

  }

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    this.exceptionThrown = true;
    this.workerThrowable = e;
    this.cancel();
  }

  public boolean isExceptionThrown() {
    return this.exceptionThrown;
  }

  public Throwable getWorkerThrowable() {
    return this.workerThrowable;
  }

}
