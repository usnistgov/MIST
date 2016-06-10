// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 11, 2014 12:26:33 PM EST
//
// Time-stamp: <Apr 11, 2014 12:26:33 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib32.parallel.gpu;

import java.util.concurrent.PriorityBlockingQueue;

import gov.nist.isg.mist.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.lib.log.Debug;
import gov.nist.isg.mist.lib.log.Debug.DebugType;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.memorypool.DynamicMemoryPool;
import gov.nist.isg.mist.lib.parallel.common.StitchingTask;
import gov.nist.isg.mist.lib.parallel.common.StitchingTask.TaskType;
import gov.nist.isg.mist.lib32.imagetile.jcuda.CudaImageTile32;
import jcuda.driver.CUcontext;
import jcuda.driver.CUstream;
import jcuda.driver.CUstream_flags;
import jcuda.driver.JCudaDriver;

/**
 * Class that computes the FFT of a tile on the GPU. One thread per GPU is used.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TileGPUFftWorker32<T> implements Runnable {

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
  public TileGPUFftWorker32(PriorityBlockingQueue<StitchingTask<T>> workQueue,
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
    CudaImageTile32.bindFwdPlanToStream(this.stream, this.threadID);
    try {
      while (!this.isCancelled && (!this.readDone || this.workQueue.size() > 0)) {
        StitchingTask<T> task = this.workQueue.take();

        Debug.msg(DebugType.VERBOSE,
            "WP Task acquired: " + task.getTask() + "  size: " + this.workQueue.size());
        if (task.getTask() == TaskType.FFT) {
          task.getTile().setDev(this.devID);
          task.getTile().setThreadID(this.threadID);
          task.getTile().computeFft(this.memoryPool, this.memory, this.stream);
          task.setTask(TaskType.BK_CHECK_NEIGHBORS);
          this.bkQueue.put(task);

        } else if (task.getTask() == TaskType.READ_DONE) {
          synchronized (TileGPUFftWorker32.class) {
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
