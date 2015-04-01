// ================================================================
//
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
//
// ================================================================

// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 3:54:04 PM EST
//
// Time-stamp: <Aug 1, 2013 3:54:04 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.parallel.cpu;

import gov.nist.isg.mist.stitching.gui.StitchingGuiUtils;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.java.JavaImageTile;
import gov.nist.isg.mist.stitching.lib.memorypool.DynamicMemoryPool;
import gov.nist.isg.mist.stitching.lib.memorypool.JavaAllocator;
import gov.nist.isg.mist.stitching.lib.memorypool.PointerAllocator;
import gov.nist.isg.mist.stitching.lib.parallel.common.StitchingTask;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverser;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverser.Traversals;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverserFactory;
import org.bridj.Pointer;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Image stitching multi-threaded entry point that sets up multithreaded execution. Initializes task
 * queues, threads, and the memory pool. Execute to initiate execution.
 * 
 * @author Tim Blattner
 * @version 1.0
 * @param <T>
 */
public class CPUStitchingThreadExecutor<T> {

  private static final int BlockingQueueSize = 300;

  private List<Thread> threads;

  private List<BookKeeper<T>> bookKeepers;
  private List<TileProducer<T>> producers;
  private List<TileWorker<T>> workers;

  private PriorityBlockingQueue<StitchingTask<T>> bkQueue;
  private PriorityBlockingQueue<StitchingTask<T>> workQueue;

  private JProgressBar progressBar;

  private DynamicMemoryPool<T> memoryPool;

  /**
   * Creates a CPU stitching thread executor
   * 
   * @param numProducers the number of producers
   * @param numWorkers the number of workers
   * @param initTile the initial tile
   * @param grid the grid of images
   * @param progressBar the progress bar
   * @throws OutOfMemoryError
   */
  @SuppressWarnings("unchecked")
  public CPUStitchingThreadExecutor(int numProducers, int numWorkers, ImageTile<T> initTile,
      TileGrid<ImageTile<T>> grid, JProgressBar progressBar) throws OutOfMemoryError {
    this.threads = new ArrayList<Thread>(numProducers + numWorkers + 1);
    this.bookKeepers = new ArrayList<BookKeeper<T>>();
    this.producers = new ArrayList<TileProducer<T>>();
    this.workers = new ArrayList<TileWorker<T>>();

    this.bkQueue = new PriorityBlockingQueue<StitchingTask<T>>(BlockingQueueSize);
    this.workQueue = new PriorityBlockingQueue<StitchingTask<T>>(BlockingQueueSize);
    
    int gWidth = grid.getExtentWidth();
    int gHeight = grid.getExtentHeight();

    int memoryPoolSize = Math.min(gWidth, gHeight) + 2 + numWorkers;

    Log.msg(LogType.MANDATORY, "memory pool size: " + memoryPoolSize);

    Log.msg(LogType.VERBOSE, "Initializing Stitching Thread Executor.");

    this.progressBar = progressBar;

    Log.msg(LogType.VERBOSE, "Grid initialized ... checking tile type: "
        + initTile.getClass().toString());

    if (initTile instanceof FftwImageTile) {
      int[] size = {FftwImageTile.fftSize};
      this.memoryPool =
          (DynamicMemoryPool<T>) new DynamicMemoryPool<Pointer<Double>>(memoryPoolSize, false,
              new PointerAllocator(), size);
    } else if (initTile instanceof JavaImageTile) {

      int[] size =
          {JavaImageTile.fftPlan.getFrequencySampling2().getCount(),
              JavaImageTile.fftPlan.getFrequencySampling1().getCount() * 2};
      this.memoryPool =
          (DynamicMemoryPool<T>) new DynamicMemoryPool<float[][]>(memoryPoolSize, false,
              new JavaAllocator(), size);
    } else {
      Log.msg(LogType.VERBOSE, "Initial tile is of type: " + initTile.getClass().toString());
    }

    Log.msg(LogType.VERBOSE, "Setting up grid traverser");
    TileGridTraverser<ImageTile<T>> gridTraverser;
    gridTraverser = TileGridTraverserFactory.makeTraverser(Traversals.DIAGONAL, grid);

    Log.msg(LogType.VERBOSE, "Initializing threads");

    for (int i = 0; i < numProducers; i++) {
      TileProducer<T> producer;
      producer = new TileProducer<T>(gridTraverser, this.workQueue, this.memoryPool, numProducers);

      this.producers.add(producer);

      this.threads.add(new Thread(producer));
    }

    for (int i = 0; i < numWorkers; i++) {
      TileWorker<T> worker;

      worker = new TileWorker<T>(this.workQueue, this.bkQueue, this.memoryPool, initTile, progressBar);

      this.workers.add(worker);

      this.threads.add(new Thread(worker));
    }

    BookKeeper<T> bookKeeper;
    bookKeeper = new BookKeeper<T>(this.bkQueue, this.workQueue, this.memoryPool, grid);

    this.bookKeepers.add(bookKeeper);

    this.threads.add(new Thread(bookKeeper));

  }

  /**
   * @param numProducers
   * @param numWorkers
   * @param initTile
   * @param grid
   */
  public CPUStitchingThreadExecutor(int numProducers, int numWorkers, ImageTile<T> initTile,
      TileGrid<ImageTile<T>> grid) {
    this(numProducers, numWorkers, initTile, grid, null);

  }

  /**
   * Executes the threads to initiate stitching
   */
  public void execute() {

    Log.msg(LogType.VERBOSE, "Executing threads");
    for (Thread thread : this.threads) {
      thread.start();
    }

    for (Thread thread : this.threads)
      try {
        thread.join();
      } catch (InterruptedException e) {
        Log.msg(LogType.MANDATORY, "Interupted stitching.");

      }

    StitchingGuiUtils.updateProgressBar(this.progressBar, true, "Garbage Collecting");
    this.memoryPool.releaseAll();
    System.gc();
  }

  /**
   * Cancels the stitching executor threads
   */
  public void cancel() {
    Log.msg(LogType.MANDATORY, "Cancelling stitching thread executor");

    for (TileProducer<T> producer : this.producers)
      producer.cancel();

    for (TileWorker<T> worker : this.workers)
      worker.cancel();

    for (BookKeeper<T> bookKeeper : this.bookKeepers)
      bookKeeper.cancel();

  }

}
