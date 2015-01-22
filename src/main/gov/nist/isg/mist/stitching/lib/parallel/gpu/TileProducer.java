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

package main.gov.nist.isg.mist.stitching.lib.parallel.gpu;

import java.util.concurrent.PriorityBlockingQueue;

import main.gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import main.gov.nist.isg.mist.stitching.lib.imagetile.ImageTile.State;
import main.gov.nist.isg.mist.stitching.lib.memorypool.DynamicMemoryPool;
import main.gov.nist.isg.mist.stitching.lib.parallel.common.StitchingTask;
import main.gov.nist.isg.mist.stitching.lib.parallel.common.StitchingTask.TaskType;
import main.gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverser;

/**
 * A thread dedicated to reading and allocating memory for image tiles.
 * 
 * @author Tim Blattner
 * @version 1.0
 * @param <T>
 */
public class TileProducer<T> implements Runnable {

  private TileGridTraverser<ImageTile<T>> traverser;
  private PriorityBlockingQueue<StitchingTask<T>> workQueue;
  private DynamicMemoryPool<T> pool;

  private volatile boolean isCancelled;

  /**
   * Initializes a producer thread
   * 
   * @param traverser the traverser
   * @param workQueue the work queu
   * @param pool the pool of memory
   */
  public TileProducer(TileGridTraverser<ImageTile<T>> traverser,
      PriorityBlockingQueue<StitchingTask<T>> workQueue, DynamicMemoryPool<T> pool) {
    this.traverser = traverser;
    this.workQueue = workQueue;
    this.pool = pool;
    this.isCancelled = false;
  }

  @Override
  public void run() {
    for (ImageTile<T> tile : this.traverser) {
      if (this.isCancelled)
        break;

      tile.readTile();
      tile.setFftState(State.IN_FLIGHT);
      tile.allocateFftMemory(this.pool);
      this.workQueue.put(new StitchingTask<T>(tile, null, TaskType.FFT));
    }

    this.workQueue.put(new StitchingTask<T>(null, null, StitchingTask.TaskType.READ_DONE));
  }

  /**
   * Cancels the task
   */
  public void cancel() {
    this.isCancelled = true;
  }
}
