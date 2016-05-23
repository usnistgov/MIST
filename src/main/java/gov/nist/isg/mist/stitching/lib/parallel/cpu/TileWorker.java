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
// Date: Aug 1, 2013 3:59:43 PM EST
//
// Time-stamp: <Aug 1, 2013 3:59:43 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.parallel.cpu;

import gov.nist.isg.mist.stitching.gui.StitchingGuiUtils;
import gov.nist.isg.mist.stitching.lib.common.CorrelationTriple;
import gov.nist.isg.mist.stitching.lib.log.Debug;
import gov.nist.isg.mist.stitching.lib.log.Debug.DebugType;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.java.JavaImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.CudaTileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.FftwTileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.JavaTileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.memorypool.DynamicMemoryPool;
import gov.nist.isg.mist.stitching.lib.parallel.common.StitchingTask;
import gov.nist.isg.mist.stitching.lib.parallel.common.StitchingTask.TaskType;
import gov.nist.isg.mist.stitching.lib32.imagetile.Stitching32;
import gov.nist.isg.mist.stitching.lib32.imagetile.fftw.FftwImageTile32;
import gov.nist.isg.mist.stitching.lib32.imagetile.java.JavaImageTile32;
import gov.nist.isg.mist.stitching.lib32.imagetile.jcuda.CudaImageTile32;
import gov.nist.isg.mist.stitching.lib32.imagetile.memory.CudaTileWorkerMemory32;
import gov.nist.isg.mist.stitching.lib32.imagetile.memory.FftwTileWorkerMemory32;
import gov.nist.isg.mist.stitching.lib32.imagetile.memory.JavaTileWorkerMemory32;

import javax.swing.*;

import java.io.FileNotFoundException;
import java.util.Random;
import java.util.concurrent.PriorityBlockingQueue;

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
          try {
            task.getTile().computeFft(this.memoryPool, this.memory);
          } catch (FileNotFoundException e) {
            Log.msg(LogType.MANDATORY, "Unable to find file: " + e.getMessage() + ". Skipping");
            continue;
          }
          task.setTask(TaskType.BK_CHECK_NEIGHBORS);
          this.bkQueue.put(task);
        } else if (task.getTask() == TaskType.PCIAM_NORTH) {
          ImageTile<T> tile = task.getTile();
          ImageTile<T> neighbor = task.getNeighbor();

          CorrelationTriple corr;
          try {
            if (this.useDoublePrecision) {
              corr = Stitching.phaseCorrelationImageAlignment(neighbor, tile, this.memory);
            } else {
              corr = Stitching32.phaseCorrelationImageAlignment(neighbor, tile, this.memory);
            }
          } catch (FileNotFoundException e) {
            Log.msg(LogType.MANDATORY, "Unable to find file: " + e.getMessage() + ". Skipping");
            continue;
          }


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

          CorrelationTriple corr;
          try {
            if (this.useDoublePrecision) {
              corr = Stitching.phaseCorrelationImageAlignment(neighbor, tile, this.memory);
            } else {
              corr = Stitching32.phaseCorrelationImageAlignment(neighbor, tile, this.memory);
            }
          } catch (FileNotFoundException e) {
            Log.msg(LogType.MANDATORY, "Unable to find file: " + e.getMessage() + ". Skipping");
            continue;
          }


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
