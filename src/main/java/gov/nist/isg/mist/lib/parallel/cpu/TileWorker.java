// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 3:59:43 PM EST
//
// Time-stamp: <Aug 1, 2013 3:59:43 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.parallel.cpu;

import java.util.concurrent.PriorityBlockingQueue;

import javax.swing.JProgressBar;

import gov.nist.isg.mist.gui.StitchingGuiUtils;
import gov.nist.isg.mist.lib.common.CorrelationTriple;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.Stitching;
import gov.nist.isg.mist.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.lib.imagetile.java.JavaImageTile;
import gov.nist.isg.mist.lib.imagetile.jcuda.CudaImageTile;
import gov.nist.isg.mist.lib.imagetile.memory.CudaTileWorkerMemory;
import gov.nist.isg.mist.lib.imagetile.memory.FftwTileWorkerMemory;
import gov.nist.isg.mist.lib.imagetile.memory.JavaTileWorkerMemory;
import gov.nist.isg.mist.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.lib.log.Debug;
import gov.nist.isg.mist.lib.log.Debug.DebugType;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.memorypool.DynamicMemoryPool;
import gov.nist.isg.mist.lib.parallel.common.StitchingTask;
import gov.nist.isg.mist.lib.parallel.common.StitchingTask.TaskType;
import gov.nist.isg.mist.lib32.imagetile.fftw.FftwImageTile32;
import gov.nist.isg.mist.lib32.imagetile.java.JavaImageTile32;
import gov.nist.isg.mist.lib32.imagetile.jcuda.CudaImageTile32;
import gov.nist.isg.mist.lib32.imagetile.memory.CudaTileWorkerMemory32;
import gov.nist.isg.mist.lib32.imagetile.memory.FftwTileWorkerMemory32;
import gov.nist.isg.mist.lib32.imagetile.memory.JavaTileWorkerMemory32;

/**
 * A thread dedicated to computing FFTs and phase correlations for image tiles.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TileWorker<T> implements Runnable {

  private PriorityBlockingQueue<StitchingTask<T>> workQueue;
  private PriorityBlockingQueue<StitchingTask<T>> bkQueue;
  private DynamicMemoryPool<T> memoryPool;


  private static boolean readDone = false;
  private static boolean bkDone = false;

  private TileWorkerMemory memory;

  private JProgressBar progressBar;

  private volatile boolean isCancelled;
  private boolean useDoublePrecision;

  /**
   * Initializes a tile worker pool for computing PCIAM and FFT computations
   *
   * @param workQueue   the work queue to pull data from
   * @param bkQueue     the bookkeeper queue to pass data to
   * @param memoryPool  the pool of memory
   * @param initTile    the initial image tile
   * @param progressBar the progress bar
   */
  public TileWorker(PriorityBlockingQueue<StitchingTask<T>> workQueue,
                    PriorityBlockingQueue<StitchingTask<T>> bkQueue, DynamicMemoryPool<T> memoryPool,
                    ImageTile<T> initTile, JProgressBar progressBar) throws OutOfMemoryError {
    readDone = false;
    bkDone = false;
    if (initTile instanceof FftwImageTile) {
      this.memory = new FftwTileWorkerMemory(initTile);
      this.useDoublePrecision = true;
    } else if (initTile instanceof FftwImageTile32) {
      this.memory = new FftwTileWorkerMemory32(initTile);
      this.useDoublePrecision = false;
    } else if (initTile instanceof CudaImageTile) {
      this.memory = new CudaTileWorkerMemory(initTile);
      this.useDoublePrecision = true;
    } else if (initTile instanceof CudaImageTile32) {
      this.memory = new CudaTileWorkerMemory32(initTile);
      this.useDoublePrecision = false;
    } else if (initTile instanceof JavaImageTile) {
      this.memory = new JavaTileWorkerMemory(initTile);
      this.useDoublePrecision = true;
    } else if (initTile instanceof JavaImageTile32) {
      this.memory = new JavaTileWorkerMemory32(initTile);
      this.useDoublePrecision = false;
    }

    this.workQueue = workQueue;
    this.bkQueue = bkQueue;
    this.memoryPool = memoryPool;
    this.progressBar = progressBar;
    this.isCancelled = false;

  }

  @Override
  public void run() {

    try {
      while (!this.isCancelled && (!readDone || !bkDone)) {
        StitchingTask<T> task = this.workQueue.take();

        Debug.msg(DebugType.VERBOSE,
            "WP Task acquired: " + task.getTask() + "  size: " + this.workQueue.size());


        if (task.getTask() == TaskType.FFT) {
          task.getTile().computeFft(this.memoryPool, this.memory);
          task.setTask(TaskType.BK_CHECK_NEIGHBORS);
          this.bkQueue.put(task);
        } else if (task.getTask() == TaskType.PCIAM_NORTH) {
          ImageTile<T> tile = task.getTile();
          ImageTile<T> neighbor = task.getNeighbor();

          CorrelationTriple corr = Stitching.phaseCorrelationImageAlignment(neighbor, tile, this.memory);

          tile.setNorthTranslation(corr);
          task.setTask(TaskType.BK_CHECK_MEM);
          this.bkQueue.put(task);

          Log.msg(LogType.HELPFUL, "N: " + tile.getFileName() + " -> " + neighbor.getFileName()
              + " x: " + tile.getNorthTranslation().getMatlabFormatStrX() + " y: "
              + tile.getNorthTranslation().getMatlabFormatStrY() + " ccf: "
              + tile.getNorthTranslation().getMatlatFormatStrCorr());

          StitchingGuiUtils.incrementProgressBar(this.progressBar);


        } else if (task.getTask() == TaskType.PCIAM_WEST) {
          ImageTile<T> tile = task.getTile();
          ImageTile<T> neighbor = task.getNeighbor();

          CorrelationTriple corr = Stitching.phaseCorrelationImageAlignment(neighbor, tile, this.memory);

          tile.setWestTranslation(corr);
          task.setTask(TaskType.BK_CHECK_MEM);
          this.bkQueue.put(task);

          Log.msg(LogType.HELPFUL, "W: " + tile.getFileName() + " -> " + neighbor.getFileName()
              + " x: " + tile.getWestTranslation().getMatlabFormatStrX() + " y: "
              + tile.getWestTranslation().getMatlabFormatStrY() + " ccf: "
              + tile.getWestTranslation().getMatlatFormatStrCorr());


          StitchingGuiUtils.incrementProgressBar(this.progressBar);


        } else if (task.getTask() == TaskType.READ_DONE) {
          synchronized (TileWorker.class) {
            readDone = true;
          }
        } else if (task.getTask() == TaskType.BK_DONE) {
          synchronized (TileWorker.class) {
            bkDone = true;
          }
        }

      }

      // Signal other workers that may be waiting to finish
      this.workQueue.put(new StitchingTask<T>(null, null, TaskType.SENTINEL));

    } catch (InterruptedException e) {
      Log.msg(LogType.MANDATORY, "Interrupted worker thread");

    }

    this.memory.releaseMemory();
  }

  /**
   * Cancels this task
   */
  public void cancel() {
    this.isCancelled = true;
    this.workQueue.put(new StitchingTask<T>(null, null, TaskType.CANCELLED));
  }

}
