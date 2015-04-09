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
import gov.nist.isg.mist.stitching.lib.log.Debug;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.memorypool.DynamicMemoryPool;
import gov.nist.isg.mist.stitching.lib.optimization.workflow.data.OptimizationData;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;

import java.util.concurrent.BlockingQueue;

/**
 * A thread dedicated to managing the state, dependencies, and freeing memory of image tiles.
 *
 * @author Tim Blattner
 * @version 1.0
 * @param <T>
 */
public class BookKeeper<T> implements Runnable {

    private BlockingQueue<OptimizationData<T>> bkQueue;
    private BlockingQueue<OptimizationData<T>> workQueue;
    private DynamicMemoryPool<short[]> memoryPool;

    private int tile_count;

    private TileGrid<ImageTile<T>> grid;

    private volatile boolean isCancelled;

    /**
     * @param bkQueue
     * @param workQueue
     * @param memoryPool
     * @param grid
     */
    public BookKeeper(BlockingQueue<OptimizationData<T>> bkQueue,
                      BlockingQueue<OptimizationData<T>> workQueue, DynamicMemoryPool<short[]> memoryPool,
                      TileGrid<ImageTile<T>> grid) {
        this.bkQueue = bkQueue;
        this.workQueue = workQueue;
        this.memoryPool = memoryPool;
        this.tile_count = 0;
        this.grid = grid;
        this.isCancelled = false;
    }

    @Override
    public void run() {
        int maxTileCount = this.grid.getExtentWidth() * this.grid.getExtentHeight();

        try {
            while (!this.isCancelled
                    && (this.tile_count != maxTileCount)) {
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
                        if (west.isTileRead()) {
                            Debug.msg(Debug.DebugType.VERBOSE,
                                    "sending west: " + tile.getFileName() + " with " + west.getFileName());

                            this.workQueue.put(new OptimizationData<T>(tile, west, OptimizationData.TaskType.OPTIMIZE_WEST));
                        }
                    }

                    // north
                    if (row > this.grid.getStartRow()) {
                        ImageTile<T> north = this.grid.getTile(row - 1, col);
                        Debug.msg(Debug.DebugType.VERBOSE, "north state: " + north.getFftState());
                        if (north.isTileRead()) {
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
                        if (memoryPool == null)
                            tile.releasePixels();
                        else
                            tile.releasePixels(memoryPool);
                    }

                    if (neighbor.getPixelDataReleaseCount() == 0) {
                        if (memoryPool == null)
                            neighbor.releasePixels();
                        else
                            neighbor.releasePixels(memoryPool);
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
