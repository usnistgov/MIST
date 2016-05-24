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

package gov.nist.isg.mist.optimization.translationrefinement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

import javax.swing.*;

import gov.nist.isg.mist.optimization.workflow.data.OptimizationData;
import gov.nist.isg.mist.optimization.workflow.tasks.BookKeeper;
import gov.nist.isg.mist.optimization.workflow.tasks.TileProducer;
import gov.nist.isg.mist.stitching.gui.StitchingGuiUtils;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverser;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverserFactory;

/**
 * Translation refinement parallel executor.
 *
 * @author Michael Majurski
 */
public class TransRefinementParallelExecutor<T> extends TransRefinementExecutorInterface {


  private List<TransRefinementWorker<T>> workers;
  private TileProducer<T> producer;
  private BookKeeper<T> bk;
  private List<Thread> executionThreads;
  private volatile boolean isCancelled = false;

  /**
   * Translation refinement parallel executor.
   *
   * @param grid               the TileGrid to refine the translations of.
   * @param modelRepeatability the stage model repeatability.
   * @param progressBar        the GUI progress bar.
   * @param params             the stitching parameters.
   */
  public TransRefinementParallelExecutor(TileGrid grid, int modelRepeatability,
                                         JProgressBar progressBar,
                                         StitchingAppParams params) {
    super(grid, modelRepeatability, progressBar, params);
  }


  @Override
  public void cancel() {
    Log.msg(Log.LogType.MANDATORY, "Canceling Translation Refinement Parallel Executor");
    this.isCancelled = true;
    if (this.workers != null) {
      for (TransRefinementWorker<T> worker : this.workers)
        worker.cancelExecution();
    }

    if (this.bk != null)
      this.bk.cancel();

    if (this.producer != null)
      this.producer.cancel();
  }


  @Override
  public void execute() {

    StitchingGuiUtils.updateProgressBar(progressBar, false, null, "Optimization...", 0,
        grid.getExtentHeight() * grid.getExtentWidth(), 0, false);

    // Reset pixel release counts if we must manage pixel data memory
    if (ImageTile.freePixelData()) {
      for (int r = 0; r < grid.getExtentHeight(); r++) {
        for (int c = 0; c < grid.getExtentWidth(); c++) {
          grid.getSubGridTile(r, c).resetPixelReleaseCount(grid.getExtentWidth(),
              grid.getExtentHeight(), grid.getStartRow(), grid.getStartCol());
          grid.getSubGridTile(r, c).releasePixels();
        }
      }
    }

    int numThreads = params.getAdvancedParams().getNumCPUThreads();

    executionThreads = new ArrayList<Thread>();
    workers = new ArrayList<TransRefinementWorker<T>>();

    // setup the grid traverser
    TileGridTraverser<ImageTile<T>> traverser =
        TileGridTraverserFactory.makeTraverser(TileGridTraverser.Traversals.DIAGONAL, grid);

    Semaphore sem = null;

    if (ImageTile.freePixelData()) {
      int numPermits = Math.min(grid.getExtentWidth(), grid.getExtentHeight()) + 2 + numThreads;
      sem = new Semaphore(numPermits, true);
    }

    BlockingQueue<OptimizationData<T>> tileQueue = new ArrayBlockingQueue<OptimizationData<T>>(grid.getSubGridSize() * 2);
    BlockingQueue<OptimizationData<T>> bkQueue = new ArrayBlockingQueue<OptimizationData<T>>(grid.getSubGridSize() * 2);

    producer = new TileProducer<T>(traverser, bkQueue, sem);
    bk = new BookKeeper<T>(bkQueue, tileQueue, sem, grid);


    Thread tmp;
    for (int i = 0; i < numThreads; i++) {
      TransRefinementWorker<T> worker = new TransRefinementWorker<T>(tileQueue,
          bkQueue, modelRepeatability, params.getAdvancedParams().getTranslationRefinementType(),
          params.getAdvancedParams().getNumTranslationRefinementStartPoints(),
          progressBar);
      workers.add(worker);
      tmp = new Thread(worker);
      // set the uncaught exception handler to this class to workers throwing exceptions can be
      // handled
      tmp.setUncaughtExceptionHandler(this);
      tmp.setName("OptimizationWorker");
      executionThreads.add(tmp);
    }

    Thread producerThread = new Thread(producer);
    producerThread.setName("OptimizationProducer");
    Thread bkThread = new Thread(bk);
    bkThread.setName("OptimizationBk");

    producerThread.start();
    bkThread.start();

    for (Thread thread : executionThreads)
      thread.start();

    try {
      producerThread.join();
      bkThread.join();

      for (Thread thread : executionThreads)
        thread.join();

    } catch (InterruptedException e) {
      cancel();
      Log.msg(Log.LogType.MANDATORY, e.getMessage());
    }
  }


}
