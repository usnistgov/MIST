// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 3:54:04 PM EST
//
// Time-stamp: <Aug 1, 2013 3:54:04 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.parallel.cpu;

import org.bridj.Pointer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import javax.swing.JProgressBar;

import gov.nist.isg.mist.gui.StitchingGuiUtils;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.lib.imagetile.java.JavaImageTile;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.memorypool.DynamicMemoryPool;
import gov.nist.isg.mist.lib.memorypool.JavaAllocator;
import gov.nist.isg.mist.lib.memorypool.PointerAllocator;
import gov.nist.isg.mist.lib.parallel.common.StitchingTask;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverser;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverser.Traversals;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverserFactory;
import gov.nist.isg.mist.lib32.imagetile.fftw.FftwImageTile32;
import gov.nist.isg.mist.lib32.imagetile.java.JavaImageTile32;
import gov.nist.isg.mist.lib32.memorypool.PointerAllocator32;

/**
 * Image stitching multi-threaded entry point that sets up multithreaded execution. Initializes task
 * queues, threads, and the memory pool. Execute to initiate execution.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class CPUStitchingThreadExecutor<T> implements Thread.UncaughtExceptionHandler {

  private static final int BlockingQueueSize = 300;

  private List<Thread> threads;

  private List<BookKeeper<T>> bookKeepers;
  private List<TileProducer<T>> producers;
  private List<TileWorker<T>> workers;

  private PriorityBlockingQueue<StitchingTask<T>> bkQueue;
  private PriorityBlockingQueue<StitchingTask<T>> workQueue;

  private JProgressBar progressBar;

  private DynamicMemoryPool<T> memoryPool;

  private boolean exceptionThrown;
  private Throwable workerThrowable;

  /**
   * Creates a CPU stitching thread executor
   *
   * @param numProducers the number of producers
   * @param numWorkers   the number of workers
   * @param initTile     the initial tile
   * @param grid         the grid of images
   * @param progressBar  the progress bar
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

    this.exceptionThrown = false;
    this.workerThrowable = null;

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
    } else if (initTile instanceof FftwImageTile32) {
      int[] size = {FftwImageTile32.fftSize};

      this.memoryPool =
          (DynamicMemoryPool<T>) new DynamicMemoryPool<Pointer<Float>>(memoryPoolSize, false,
              new PointerAllocator32(), size);
    } else if (initTile instanceof JavaImageTile) {

      int[] size =
          {JavaImageTile.fftPlan.getFrequencySampling2().getCount(),
              JavaImageTile.fftPlan.getFrequencySampling1().getCount() * 2};

      this.memoryPool =
          (DynamicMemoryPool<T>) new DynamicMemoryPool<float[][]>(memoryPoolSize, false,
              new JavaAllocator(), size);
    } else if (initTile instanceof JavaImageTile32) {
      int[] size =
          {JavaImageTile32.fftPlan.getFrequencySampling2().getCount(),
              JavaImageTile32.fftPlan.getFrequencySampling1().getCount() * 2};

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

    Thread tmp;

    for (int i = 0; i < numProducers; i++) {
      TileProducer<T> producer;
      producer = new TileProducer<T>(gridTraverser, this.workQueue, this.memoryPool, numProducers);

      this.producers.add(producer);

      tmp = new Thread(producer);
      tmp.setUncaughtExceptionHandler(this);
      this.threads.add(tmp);
    }

    for (int i = 0; i < numWorkers; i++) {
      TileWorker<T> worker;

      worker = new TileWorker<T>(this.workQueue, this.bkQueue, this.memoryPool, initTile, progressBar);

      this.workers.add(worker);

      tmp = new Thread(worker);
      tmp.setUncaughtExceptionHandler(this);
      this.threads.add(tmp);
    }

    BookKeeper<T> bookKeeper;
    bookKeeper = new BookKeeper<T>(this.bkQueue, this.workQueue, this.memoryPool, grid);

    this.bookKeepers.add(bookKeeper);

    tmp = new Thread(bookKeeper);
    tmp.setUncaughtExceptionHandler(this);
    this.threads.add(tmp);

  }

  /**
   * @param numProducers the number of producer threads
   * @param numWorkers   the number of worker threads
   * @param initTile     the initialization tile
   * @param grid         the image grid
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
        Log.msg(LogType.MANDATORY, "Interrupted stitching.");

      }

    StitchingGuiUtils.updateProgressBar(this.progressBar, true, "Garbage Collecting");
    this.memoryPool.releaseAll();
    System.gc();
  }

  /**
   * Cancels the stitching executor threads
   */
  public void cancel() {
    for (TileProducer<T> producer : this.producers)
      producer.cancel();

    for (TileWorker<T> worker : this.workers)
      worker.cancel();

    for (BookKeeper<T> bookKeeper : this.bookKeepers)
      bookKeeper.cancel();

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
