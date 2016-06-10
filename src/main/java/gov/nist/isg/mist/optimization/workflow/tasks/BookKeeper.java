// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 3:59:00 PM EST
//
// Time-stamp: <Aug 1, 2013 3:59:00 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.optimization.workflow.tasks;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.log.Debug;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.optimization.workflow.data.OptimizationData;

/**
 * A thread dedicated to managing the state, dependencies, and freeing memory of image tiles.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class BookKeeper<T> implements Runnable {

  private BlockingQueue<OptimizationData<T>> bkQueue;
  private BlockingQueue<OptimizationData<T>> workQueue;
  private Semaphore sem;

  private int tile_count;

  private TileGrid<ImageTile<T>> grid;

  private volatile boolean isCancelled;

  /**
   * @param bkQueue   the book keeping queue
   * @param workQueue the work queue
   * @param sem       semaphore
   * @param grid      the image grid
   */
  public BookKeeper(BlockingQueue<OptimizationData<T>> bkQueue,
                    BlockingQueue<OptimizationData<T>> workQueue, Semaphore sem,
                    TileGrid<ImageTile<T>> grid) {
    this.bkQueue = bkQueue;
    this.workQueue = workQueue;
    this.sem = sem;
    this.tile_count = 0;
    this.grid = grid;
    this.isCancelled = false;
  }

  @Override
  public void run() {
    int maxTileCount = this.grid.getExtentWidth() * this.grid.getExtentHeight();

    try {
      while (!this.isCancelled && (this.tile_count != maxTileCount)) {
        OptimizationData<T> task = this.bkQueue.take();

        Debug.msg(Debug.DebugType.VERBOSE, "BK Task received: " + task.getType());

        if (task.getType() == OptimizationData.TaskType.BK_CHECK_NEIGHBORS) {
          this.tile_count++;
          ImageTile<T> tile = task.getTile();

          int row = tile.getRow();
          int col = tile.getCol();
          // west
          if (col > this.grid.getStartCol()) {
            ImageTile<T> west = this.grid.getTile(row, col - 1);
            if (!west.fileExists() || west.isTileRead()) {
              Debug.msg(Debug.DebugType.VERBOSE,
                  "sending west: " + tile.getFileName() + " with " + west.getFileName());

              this.workQueue.put(new OptimizationData<T>(tile, west, OptimizationData.TaskType.OPTIMIZE_WEST));
            }
          }

          // north
          if (row > this.grid.getStartRow()) {
            ImageTile<T> north = this.grid.getTile(row - 1, col);
            Debug.msg(Debug.DebugType.VERBOSE, "north state: " + north.getFftState());
            if (!north.fileExists() || north.isTileRead()) {
              Debug.msg(Debug.DebugType.VERBOSE, "sending north: " + tile.getFileName() + " with "
                  + north.getFileName());

              this.workQueue.put(new OptimizationData<T>(tile, north, OptimizationData.TaskType.OPTIMIZE_NORTH));
            }
          }

        } else if (task.getType() == OptimizationData.TaskType.BK_CHECK_MEMORY) {

          ImageTile<T> tile = task.getTile();
          ImageTile<T> neighbor = task.getNeighbor();

          tile.decrementPixelDataReleaseCount();
          neighbor.decrementPixelDataReleaseCount();

          if (tile.getPixelDataReleaseCount() == 0) {
            if (sem == null)
              tile.releasePixels();
            else
              tile.releasePixels(sem);
          }

          if (neighbor.getPixelDataReleaseCount() == 0) {
            if (sem == null)
              neighbor.releasePixels();
            else
              neighbor.releasePixels(sem);
          }
        }

        Debug.msg(Debug.DebugType.INFO, "tiles: " + this.tile_count);

      }

      Debug.msg(Debug.DebugType.INFO, "BK DONE");
      this.workQueue.put(new OptimizationData<T>(null, null, OptimizationData.TaskType.BK_DONE));

    } catch (InterruptedException e) {
      Log.msg(Log.LogType.MANDATORY, "Interrupted bookkeeping thread");
    }
  }

  /**
   * Sets that this thread is cancelled
   */
  public void cancel() {
    this.isCancelled = true;
    this.bkQueue.add(new OptimizationData<T>(null, null, OptimizationData.TaskType.CANCELLED));
  }

}
