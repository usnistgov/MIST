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


// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 3:54:04 PM EST
//
// Time-stamp: <Aug 1, 2013 3:54:04 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib32.parallel.gpu;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import javax.swing.*;

import gov.nist.isg.mist.gui.StitchingGuiUtils;
import gov.nist.isg.mist.lib.executor.StitchingExecutor;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.memorypool.CudaAllocator;
import gov.nist.isg.mist.lib.memorypool.DynamicMemoryPool;
import gov.nist.isg.mist.lib.parallel.common.StitchingTask;
import gov.nist.isg.mist.lib.parallel.gpu.BookKeeper;
import gov.nist.isg.mist.lib.parallel.gpu.TileProducer;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.lib.tilegrid.TileGrid.GridDecomposition;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverser;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverser.Traversals;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverserFactory;
import gov.nist.isg.mist.lib32.imagetile.jcuda.CudaImageTile32;
import gov.nist.isg.mist.lib32.imagetile.memory.CudaTileWorkerMemory32;
import jcuda.CudaException;
import jcuda.Sizeof;
import jcuda.driver.CUcontext;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.JCudaDriver;

/**
 * Image stitching multi-threaded entry point that sets up multithreaded execution. Initializes task
 * queues, threads, and the memory pool. Execute to initiate execution.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class GPUStitchingThreadExecutor32<T> implements Thread.UncaughtExceptionHandler {

  private static final int BlockingQueueSize = 300;

  private List<Thread> threads;

  private List<BookKeeper<T>> bookKeepers;
  private List<TileCpuCcfWorker32<T>> ccfWorkers;
  private List<TileGPUFftWorker32<T>> fftWorkers;
  private List<TileGPUPciamWorker32<T>> pciamWorkers;
  private List<TileProducer<T>> producers;

  private PriorityBlockingQueue<StitchingTask<T>> bkQueue;
  private PriorityBlockingQueue<StitchingTask<T>> ccfQueue;

  private PriorityBlockingQueue<StitchingTask<T>>[] fftQueues;
  private PriorityBlockingQueue<StitchingTask<T>>[] pciamQueues;

  private DynamicMemoryPool<T>[] memoryPools;
  private TileWorkerMemory[] memories;
  private CUcontext[] contexts;

  private JProgressBar progressBar;

  private int numGPUs;


  private boolean exceptionThrown;
  private Throwable workerThrowable;

  /**
   * Initializes stitching thread executor
   *
   * @param numGPUs     number of GPUs
   * @param numWorkers  number of CPU CCF workers
   * @param initTile    initial tile
   * @param grid        grid of tiles
   * @param contexts    GPU contexts
   * @param devIDs      the GPU device ids
   * @param progressBar optional progress bar (null if none)
   * @param executor    the stitching executor
   */
  @SuppressWarnings("unchecked")
  public GPUStitchingThreadExecutor32(int numGPUs, int numWorkers, ImageTile<T> initTile,
                                      TileGrid<ImageTile<T>> grid, CUcontext[] contexts, int[] devIDs, JProgressBar progressBar,
                                      StitchingExecutor executor, boolean enableCudaExceptions) throws OutOfMemoryError, CudaException {

    JCudaDriver.setExceptionsEnabled(true);

    this.numGPUs = numGPUs;
    this.contexts = contexts;
    this.threads = new ArrayList<Thread>();
    this.bookKeepers = new ArrayList<BookKeeper<T>>();
    this.ccfWorkers = new ArrayList<TileCpuCcfWorker32<T>>();
    this.fftWorkers = new ArrayList<TileGPUFftWorker32<T>>();
    this.pciamWorkers = new ArrayList<TileGPUPciamWorker32<T>>();
    this.producers = new ArrayList<TileProducer<T>>();
    this.bkQueue = new PriorityBlockingQueue<StitchingTask<T>>(BlockingQueueSize);
    this.ccfQueue = new PriorityBlockingQueue<StitchingTask<T>>(BlockingQueueSize);

    this.exceptionThrown = false;
    this.workerThrowable = null;


    // 20 is added to reduce the amount of throttling
    int gWidth = grid.getExtentWidth();
    int gHeight = grid.getExtentHeight();

    int memoryPoolSize = Math.min(gWidth, gHeight) + 2 + numWorkers;

    Log.msg(LogType.MANDATORY, "Memory pool size: " + memoryPoolSize);

    this.progressBar = progressBar;
    int numNeighbors =
        ((grid.getExtentHeight() - 1) * grid.getExtentWidth())
            + ((grid.getExtentWidth() - 1) * grid.getExtentHeight());

    // Decompose grid across numGPUs
    List<TileGrid<ImageTile<T>>> grids = grid.partitionGrid(this.numGPUs, GridDecomposition.HORIZONTAL);

    if (grids.size() != this.numGPUs) {
      Log.msg(LogType.MANDATORY, "A better decomposition for your grid"
          + " has been found. Reducing numGPUs from " + this.numGPUs + " to " + grids.size());
      this.numGPUs = grids.size();
    }

    this.contexts = contexts;
    this.memoryPools = new DynamicMemoryPool[this.numGPUs];

    this.pciamQueues = new PriorityBlockingQueue[this.numGPUs];
    this.fftQueues = new PriorityBlockingQueue[this.numGPUs];
    this.memories = new TileWorkerMemory[this.numGPUs];

    for (int i = 0; i < this.numGPUs; i++) {
      this.fftQueues[i] = new PriorityBlockingQueue<StitchingTask<T>>(BlockingQueueSize);
      this.pciamQueues[i] = new PriorityBlockingQueue<StitchingTask<T>>(BlockingQueueSize);

      JCudaDriver.cuCtxSetCurrent(contexts[i]);

      this.memories[i] = new CudaTileWorkerMemory32(initTile);
      int[] sz = {CudaImageTile32.fftSize * Sizeof.FLOAT * 2};

      this.memoryPools[i] =
          (DynamicMemoryPool<T>) new DynamicMemoryPool<CUdeviceptr>(memoryPoolSize, false,
              new CudaAllocator(), sz);
    }


    Thread tmp;

    for (int i = 0; i < this.numGPUs; i++) {
      TileGridTraverser<ImageTile<T>> gridTraverser;
      gridTraverser = TileGridTraverserFactory.makeTraverser(Traversals.DIAGONAL, grids.get(i));

      TileProducer<T> producer;
      producer = new TileProducer<T>(gridTraverser, this.fftQueues[i], this.memoryPools[i]);

      this.producers.add(producer);

      tmp = new Thread(producer);
      tmp.setUncaughtExceptionHandler(this);
      this.threads.add(tmp);
    }

    for (int i = 0; i < this.numGPUs; i++) {
      TileGPUFftWorker32<T> fftWorker;
      fftWorker =
          new TileGPUFftWorker32<T>(this.fftQueues[i], this.bkQueue, this.memoryPools[i], this.memories[i],
              devIDs[i], i, this.contexts[i]);

      TileGPUPciamWorker32<T> pciamWorker;
      pciamWorker =
          new TileGPUPciamWorker32<T>(this.pciamQueues[i], this.bkQueue, this.ccfQueue,
              this.memories[i], initTile.getWidth(), initTile.getHeight(), devIDs[i], i, this.contexts[i], contexts, devIDs);


      this.fftWorkers.add(fftWorker);
      this.pciamWorkers.add(pciamWorker);

      tmp = new Thread(fftWorker);
      tmp.setUncaughtExceptionHandler(this);
      this.threads.add(tmp);

      tmp = new Thread(pciamWorker);
      tmp.setUncaughtExceptionHandler(this);
      this.threads.add(tmp);

    }

    for (int i = 0; i < numWorkers; i++) {
      TileCpuCcfWorker32<T> ccfWorker;
      ccfWorker = new TileCpuCcfWorker32<T>(this.ccfQueue, numNeighbors, progressBar);

      this.ccfWorkers.add(ccfWorker);

      tmp = new Thread(ccfWorker);
      tmp.setUncaughtExceptionHandler(this);
      this.threads.add(tmp);
    }

    BookKeeper<T> bookKeeper;
    bookKeeper = new BookKeeper<T>(this.bkQueue, this.pciamQueues, this.memoryPools, grid);

    this.bookKeepers.add(bookKeeper);

    tmp = new Thread(bookKeeper);
    tmp.setUncaughtExceptionHandler(this);
    this.threads.add(tmp);

    JCudaDriver.setExceptionsEnabled(enableCudaExceptions);

  }

  /**
   * Initializes a stitching thread executor
   *
   * @param numGPUs    the number of GPUS
   * @param numWorkers the number of CPU workers
   * @param initTile   the initial tile
   * @param grid       the grid of tiles
   * @param contexts   the GPU contexts
   * @param devIDs     the GPU device IDs used
   */
  public GPUStitchingThreadExecutor32(int numGPUs, int numWorkers, ImageTile<T> initTile,
                                      TileGrid<ImageTile<T>> grid, CUcontext[] contexts, int[] devIDs) {
    this(numGPUs, numWorkers, initTile, grid, contexts, devIDs, null, null, false);
  }

  /**
   * Executes the threads to initiate stitching
   */
  public void execute() {

    for (Thread thread : this.threads)
      thread.start();

    for (Thread thread : this.threads)
      try {
        thread.join();
      } catch (InterruptedException e) {
        Log.msg(LogType.MANDATORY, "Interrupted stitching.");
      }

    StitchingGuiUtils.updateProgressBar(this.progressBar, true, "Garbage Collecting");

    for (int i = 0; i < this.numGPUs; i++) {
      this.memoryPools[i].releaseAll();
      this.memories[i].releaseMemory();
    }

    System.gc();
  }

  /**
   * Cancels the executor
   */
  public void cancel() {
    for (TileProducer<T> producer : this.producers)
      producer.cancel();

    for (TileGPUFftWorker32<T> fftWorker : this.fftWorkers)
      fftWorker.cancel();

    for (TileGPUPciamWorker32<T> pciamWorker : this.pciamWorkers)
      pciamWorker.cancel();

    for (TileCpuCcfWorker32<T> ccfWorker : this.ccfWorkers)
      ccfWorker.cancel();

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
