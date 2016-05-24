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
// Date: Aug 1, 2013 3:58:49 PM EST
//
// Time-stamp: <Aug 1, 2013 3:58:49 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.parallel.gpu;

import java.util.concurrent.PriorityBlockingQueue;

import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile.State;
import gov.nist.isg.mist.stitching.lib.log.Debug;
import gov.nist.isg.mist.stitching.lib.log.Debug.DebugType;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.memorypool.DynamicMemoryPool;
import gov.nist.isg.mist.stitching.lib.parallel.common.StitchingTask;
import gov.nist.isg.mist.stitching.lib.parallel.common.StitchingTask.TaskType;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;

/**
 * A thread dedicated to managing the state, dependencies, and freeing memory of image tiles.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class BookKeeper<T> implements Runnable {

  private PriorityBlockingQueue<StitchingTask<T>> bkQueue;
  private PriorityBlockingQueue<StitchingTask<T>>[] workQueues;

  private DynamicMemoryPool<T>[] memoryPools;

  private int fftCount;
  private int pciamCount;

  private TileGrid<ImageTile<T>> grid;

  private volatile boolean isCancelled;

  /**
   * @param bkQueue
   * @param workQueue
   * @param memoryPool
   * @param grid
   */
  public BookKeeper(PriorityBlockingQueue<StitchingTask<T>> bkQueue,
                    PriorityBlockingQueue<StitchingTask<T>>[] workQueue, DynamicMemoryPool<T>[] memoryPool,
                    TileGrid<ImageTile<T>> grid) {
    this.bkQueue = bkQueue;
    this.workQueues = workQueue;
    this.memoryPools = memoryPool;
    this.fftCount = 0;
    this.pciamCount = 0;
    this.grid = grid;
    this.isCancelled = false;
  }

  @Override
  public void run() {
    int maxFftCount = this.grid.getExtentWidth() * this.grid.getExtentHeight();
    int maxPciamCount = ((this.grid.getExtentWidth() - 1) * this.grid.getExtentHeight())
        + ((this.grid.getExtentHeight() - 1) * this.grid.getExtentWidth());
    try {
      while (!this.isCancelled
          && (this.fftCount != maxFftCount || this.pciamCount != maxPciamCount)) {
        StitchingTask<T> task = this.bkQueue.take();

        Debug.msg(DebugType.VERBOSE, "BK Task received: " + task.getTask());

        if (task.getTask() == TaskType.BK_CHECK_NEIGHBORS) {
          this.fftCount++;
          ImageTile<T> tile = task.getTile();

          int dev = tile.getThreadID();

          tile.setFftState(State.COMPLETE);
          int row = tile.getRow();
          int col = tile.getCol();

          // west
          if (col > this.grid.getStartCol()) {
            ImageTile<T> west = this.grid.getTile(row, col - 1);
            if (west.getFftState() == State.COMPLETE && tile.getPciamWestState() == State.NONE) {
              Debug.msg(DebugType.VERBOSE,
                  "sending west: " + tile.getFileName() + " with " + west.getFileName());
              tile.setPciamWestState(State.IN_FLIGHT);

              this.workQueues[dev].put(new StitchingTask<T>(tile, west, TaskType.PCIAM_WEST));
            }
          }

          // north
          if (row > this.grid.getStartRow()) {
            ImageTile<T> north = this.grid.getTile(row - 1, col);
            Debug.msg(DebugType.VERBOSE, "north state: " + north.getFftState());

            if (north.getFftState() == State.COMPLETE && tile.getPciamNorthState() == State.NONE)

            {
              Debug.msg(DebugType.VERBOSE, "sending north: " + tile.getFileName() + " with "
                  + north.getFileName());

              tile.setPciamNorthState(State.IN_FLIGHT);
              this.workQueues[dev].put(new StitchingTask<T>(tile, north, TaskType.PCIAM_NORTH));
            }
          }

          // south
          if (row < this.grid.getExtentHeight() - 1 + this.grid.getStartRow()) {
            ImageTile<T> south = this.grid.getTile(row + 1, col);
            if (south.getFftState() == State.COMPLETE
                && south.getPciamNorthState() == State.NONE) {
              Debug.msg(DebugType.VERBOSE, "sending south: " + south.getFileName() + " with "
                  + tile.getFileName());

              south.setPciamNorthState(State.IN_FLIGHT);
              this.workQueues[south.getThreadID()].put(new StitchingTask<T>(south, tile, TaskType.PCIAM_NORTH));
            }

          }

          // east
          if (col < this.grid.getExtentWidth() - 1 + this.grid.getStartCol()) {
            ImageTile<T> east = this.grid.getTile(row, col + 1);

            if (east.getFftState() == State.COMPLETE && east.getPciamWestState() == State.NONE) {
              Debug.msg(DebugType.VERBOSE,
                  "sending east: " + east.getFileName() + " with " + tile.getFileName());

              east.setPciamWestState(State.IN_FLIGHT);
              this.workQueues[east.getThreadID()].put(new StitchingTask<T>(east, tile, TaskType.PCIAM_WEST));
            }

          }

        } else if (task.getTask() == TaskType.BK_CHECK_MEM) {
          this.pciamCount++;

          ImageTile<T> tile = task.getTile();
          ImageTile<T> neighbor = task.getNeighbor();

          tile.decrementFftReleaseCount();
          neighbor.decrementFftReleaseCount();

          if (tile.getFftReleaseCount() == 0)
            tile.releaseFftMemory(this.memoryPools[tile.getThreadID()]);

          if (neighbor.getFftReleaseCount() == 0)
            neighbor.releaseFftMemory(this.memoryPools[neighbor.getThreadID()]);
        }

        Debug.msg(DebugType.VERBOSE, "ffts: " + this.fftCount + " pciamCount: " + this.pciamCount);

      }

      Debug.msg(DebugType.HELPFUL, "BK DONE");

      for (PriorityBlockingQueue<StitchingTask<T>> q : this.workQueues)
        q.put(new StitchingTask<T>(null, null, TaskType.BK_DONE));

    } catch (InterruptedException e) {
      Log.msg(LogType.MANDATORY, "Interrupted bookkeeper");
    }
  }

  /**
   * Cancels this task
   */
  public void cancel() {
    this.isCancelled = true;
    this.bkQueue.put(new StitchingTask<T>(null, null, TaskType.CANCELLED));
  }

}
