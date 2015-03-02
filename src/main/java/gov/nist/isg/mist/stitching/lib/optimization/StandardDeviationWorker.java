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
// Date: Apr 11, 2014 11:37:58 AM EST
//
// Time-stamp: <Apr 11, 2014 11:37:58 AM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.optimization;

import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.optimization.OptimizationUtils.Direction;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;

import java.io.FileNotFoundException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Class that represents a thread that computes the standard deviation of the overlap region for a
 * tile.
 * 
 * @author Tim Blattner
 * @version 1.0
 * @param <T>
 */
public class StandardDeviationWorker<T> implements Runnable {
  private static final long TIMEOUT = 1000L;

  private BlockingQueue<ImageTile<T>> tiles;
  private TileGrid<ImageTile<T>> grid;
  private Direction dir;
  private double overlap;
  private double percOverlapError;

  /**
   * Constructs a standard deviation worker to compute the standard deviations of a list of tiles
   * using multi-threading.
   * 
   * @param queue the queue of tiles to be processed
   * @param grid the grid of tiles
   * @param dir the direction
   * @param overlap the percent overlap
   * @param percOverlapError the percent overlap error
   */
  public StandardDeviationWorker(BlockingQueue<ImageTile<T>> queue, TileGrid<ImageTile<T>> grid,
      Direction dir, double overlap, double percOverlapError) {
    this.tiles = queue;
    this.grid = grid;
    this.dir = dir;
    this.overlap = overlap;
    this.percOverlapError = percOverlapError;
  }

  @Override
  public void run() {
    while (this.tiles.size() > 0) {
      ImageTile<T> tile;
      try {
        tile = this.tiles.poll(TIMEOUT, TimeUnit.NANOSECONDS);

        if (tile == null)
          continue;

        int row = tile.getRow();
        int col = tile.getCol();

        ImageTile<T> neighbor = null;
        switch (this.dir) {
          case North:
            neighbor = this.grid.getTile(row - 1, col);
            break;
          case West:
            neighbor = this.grid.getTile(row, col - 1);
            break;
        }

        if (neighbor == null)
          continue;


        if (!this.grid.hasTile(neighbor)) {
          continue;
        }
        try {
            readTile(tile);
            readTile(neighbor);

            switch (this.dir) {
                case North:
                    tile.computeStdDevNorth(neighbor, this.overlap, this.percOverlapError);
                    break;
                case West:
                    tile.computeStdDevWest(neighbor, this.overlap, this.percOverlapError);
                    break;
            }
        } catch (FileNotFoundException e)
        {
            Log.msg(LogType.MANDATORY, "Unable to load file: " + e.getMessage() + ". Skipping");
            continue;
        }

        releaseTile(tile);
        releaseTile(neighbor);

      } catch (InterruptedException e) {
        Log.msg(LogType.MANDATORY, e.getMessage());
        break;
      }

    }

  }

  /**
   * Synchronously reads a tile
   * 
   * @param tile the tile to be read
   */
  private static synchronized <T> void readTile(ImageTile<T> tile) throws FileNotFoundException {
    if (!tile.isTileRead())
      tile.readTile();
  }

  /**
   * Synchronously releases a tile
   * 
   * @param tile the tile to be released
   */
  private static synchronized <T> void releaseTile(ImageTile<T> tile) {
    tile.releasePixels();
  }

}
