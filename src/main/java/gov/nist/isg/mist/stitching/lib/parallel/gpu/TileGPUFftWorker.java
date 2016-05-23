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
// Date: Apr 11, 2014 12:26:33 PM EST
//
// Time-stamp: <Apr 11, 2014 12:26:33 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.parallel.gpu;

import gov.nist.isg.mist.stitching.lib.log.Debug;
import gov.nist.isg.mist.stitching.lib.log.Debug.DebugType;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import jcuda.driver.CUcontext;
import jcuda.driver.CUstream;
import jcuda.driver.CUstream_flags;
import jcuda.driver.JCudaDriver;
import gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.memorypool.DynamicMemoryPool;
import gov.nist.isg.mist.stitching.lib.parallel.common.StitchingTask;
import gov.nist.isg.mist.stitching.lib.parallel.common.StitchingTask.TaskType;

import java.io.FileNotFoundException;
import java.util.Random;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Class that computes the FFT of a tile on the GPU. One thread per GPU is used.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TileGPUFftWorker<T> implements Runnable {

  private PriorityBlockingQueue<StitchingTask<T>> workQueue;
  private PriorityBlockingQueue<StitchingTask<T>> bkQueue;
  private DynamicMemoryPool<T> memoryPool;

  private boolean readDone;

  private TileWorkerMemory memory;

  private CUstream stream;

  private CUcontext context;
  private int devID;
  private int threadID;

  private volatile boolean isCancelled;

  /**
   * Initializes a tile worker pool for computing PCIAM and FFT computations
   *
   * @param workQueue  the work queue to pull data from
   * @param bkQueue    the bookkeeper queue to pass data to
   * @param memoryPool the pool of memory
   * @param memory     the tile worker memory
   * @param devID      the GPU device ID
   * @param threadID   the thread ID
   * @param context    the GPU context
   */
  public TileGPUFftWorker(PriorityBlockingQueue<StitchingTask<T>> workQueue,
                          PriorityBlockingQueue<StitchingTask<T>> bkQueue, DynamicMemoryPool<T> memoryPool,
                          TileWorkerMemory memory, int devID, int threadID, CUcontext context) {
    this.readDone = false;
    this.context = context;
    this.memory = memory;
    this.workQueue = workQueue;
    this.bkQueue = bkQueue;
    this.memoryPool = memoryPool;
    this.devID = devID;
    this.threadID = threadID;
    this.isCancelled = false;

  }

  @Override
  public void run() {

    JCudaDriver.cuCtxSetCurrent(this.context);

    this.stream = new CUstream();
    JCudaDriver.cuStreamCreate(this.stream, CUstream_flags.CU_STREAM_DEFAULT);
    CudaImageTile.bindFwdPlanToStream(this.stream, this.threadID);
    try {
      while (!this.isCancelled && (!this.readDone || this.workQueue.size() > 0)) {
        StitchingTask<T> task = this.workQueue.take();

        Debug.msg(DebugType.VERBOSE,
            "WP Task acquired: " + task.getTask() + "  size: " + this.workQueue.size());
        if (task.getTask() == TaskType.FFT) {
          task.getTile().setDev(this.devID);
          task.getTile().setThreadID(this.threadID);

          try {
            task.getTile().computeFft(this.memoryPool, this.memory, this.stream);
          } catch (FileNotFoundException e) {
            Log.msg(LogType.MANDATORY, "Unable to find file: " + e.getMessage() + ". Skipping file");
            continue;
          }

          task.setTask(TaskType.BK_CHECK_NEIGHBORS);

          this.bkQueue.put(task);

        } else if (task.getTask() == TaskType.READ_DONE) {
          synchronized (TileGPUFftWorker.class) {
            this.readDone = true;
          }
        } else if (task.getTask() == TaskType.CANCELLED) {
          this.isCancelled = true;
        }


      }

      Debug.msg(DebugType.HELPFUL, "FFT Done");

      // Signal other workers that may be waiting to finish
      this.workQueue.put(new StitchingTask<T>(null, null, TaskType.SENTINEL));

    } catch (InterruptedException e) {
      Log.msg(LogType.MANDATORY, "Interrupted FFT worker");
    }

    JCudaDriver.cuStreamDestroy(this.stream);
  }

  /**
   * Cancels the task
   */
  public void cancel() {
    this.workQueue.put(new StitchingTask<T>(null, null, TaskType.CANCELLED));
  }
}
