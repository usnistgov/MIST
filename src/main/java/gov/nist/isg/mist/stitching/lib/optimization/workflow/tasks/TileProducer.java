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
// Date: Aug 1, 2013 3:59:00 PM EST
//
// Time-stamp: <Aug 1, 2013 3:59:00 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.optimization.workflow.tasks;

import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.memorypool.DynamicMemoryPool;
import gov.nist.isg.mist.stitching.lib.optimization.workflow.data.OptimizationData;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverser;

import java.io.FileNotFoundException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * A thread dedicated to reading and allocating memory for image tiles.
 * 
 * @author Tim Blattner
 * @version 1.0
 * @param <T>
 */
public class TileProducer<T> implements Runnable {

  private TileGridTraverser<ImageTile<T>> traverser;
  private BlockingQueue<OptimizationData<T>> workQueue;
  private Semaphore sem;

  private volatile boolean isCancelled;

  /**
   * Initializes a producer thread
   * 
   * @param traverser the traverser
   * @param workQueue the work queu
   * @param pool the pool of memory
   */
  public TileProducer(TileGridTraverser<ImageTile<T>> traverser,
                      BlockingQueue<OptimizationData<T>> workQueue, Semaphore sem) {
    this.traverser = traverser;
    this.workQueue = workQueue;
    this.sem = sem;
    this.isCancelled = false;
  }

  @Override
  public void run() {
    for (ImageTile<T> tile : this.traverser) {
      if (this.isCancelled)
        break;

      try {
        if (sem != null) {
          sem.acquire();
        }

        tile.readTile();

      } catch (FileNotFoundException e) {
        Log.msg(Log.LogType.MANDATORY, "Unable to find file: " + e.getMessage() + ". Skipping tile");
        continue;
      } catch (InterruptedException e) {
        Log.msg(Log.LogType.MANDATORY, "Tile producer interupted");
        return;
      }

      this.workQueue.add(new OptimizationData<T>(tile, OptimizationData.TaskType.BK_CHECK_NEIGHBORS));

    }

  }

  /**
   * Cancels the task
   */
  public void cancel() {
    this.isCancelled = true;
    if (this.sem != null)
      this.sem.release();
  }
}
