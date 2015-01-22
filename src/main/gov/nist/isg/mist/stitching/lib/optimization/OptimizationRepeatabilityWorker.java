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
// Date: Apr 11, 2014 11:04:11 AM EST
//
// Time-stamp: <Apr 11, 2014 11:04:11 AM tjb3>
//
//
// ================================================================

package main.gov.nist.isg.mist.stitching.lib.optimization;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.swing.JProgressBar;

import main.gov.nist.isg.mist.stitching.gui.StitchingGuiUtils;
import main.gov.nist.isg.mist.stitching.lib.common.CorrelationTriple;
import main.gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import main.gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import main.gov.nist.isg.mist.stitching.lib.log.Log;
import main.gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import main.gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;

/**
 * Class that represents a thread that computes a cross correlation search.
 * 
 * @author Tim Blattner
 * @version 1.0
 * @param <T>
 */
public class OptimizationRepeatabilityWorker<T> implements Runnable {

  private static final long TIMEOUT = 1000L;

  private BlockingQueue<ImageTile<T>> tiles;
  private TileGrid<ImageTile<T>> grid;
  private JProgressBar progressBar;

  private int repeatabilty;
  private volatile boolean isCancelled;

  /**
   * Creates an optimization repeatability worker for executing a cross correlation search.
   * 
   * @param queue the queue of tiles to be processed
   * @param grid the grid of tiles
   * @param repeatabilty the repeatability of the microscope
   * @param progressBar the progress bar
   */
  public OptimizationRepeatabilityWorker(BlockingQueue<ImageTile<T>> queue,
      TileGrid<ImageTile<T>> grid, int repeatabilty, JProgressBar progressBar) {
    this.tiles = queue;
    this.grid = grid;
    this.progressBar = progressBar;
    this.repeatabilty = repeatabilty;
    this.isCancelled = false;
  }

  @Override
  public void run() {

    while (!this.isCancelled && this.tiles.size() > 0) {
      ImageTile<T> tile;
      try {
        tile = this.tiles.poll(TIMEOUT, TimeUnit.NANOSECONDS);


        if (tile == null)
          continue;

        int row = tile.getRow();
        int col = tile.getCol();

        if (tile.getNorthTranslation() != null) {
          ImageTile<T> neighbor = this.grid.getTile(row - 1, col);

          if (this.grid.hasTile(neighbor)) {            

            CorrelationTriple northTrans = tile.getNorthTranslation();

            readTile(tile);
            readTile(neighbor);

            int xMin = northTrans.getX() - this.repeatabilty;
            int xMax = northTrans.getX() + this.repeatabilty;
            int yMin = northTrans.getY() - this.repeatabilty;
            int yMax = northTrans.getY() + this.repeatabilty;

            double oldCorr = northTrans.getCorrelation();
            CorrelationTriple bestNorth;
            try {
              if (Stitching.USE_HILLCLIMBING) {

                bestNorth =
                    Stitching.computeCCF_HillClimbing_UD(xMin, xMax, yMin, yMax, northTrans.getX(),
                        northTrans.getY(), neighbor, tile);


              } else {
                bestNorth = Stitching.computeCCF_UD(xMin, xMax, yMin, yMax, neighbor, tile);
              }
            } catch (NullPointerException e) {
              continue;
            }
            tile.setNorthTranslation(bestNorth);

            if (!Double.isNaN(oldCorr)) {
              // If the old correlation was a number, then it was a good translation.
              // Increment the new translation by the value of the old correlation to increase beyond 1
              // This will enable these tiles to have higher priority in minimum spanning tree search
              tile.getNorthTranslation().incrementCorrelation(Math.floor(oldCorr));
            }

            if (tile.getTileCorrelation() < bestNorth.getCorrelation()) {
              tile.setTileCorrelation(bestNorth.getCorrelation());
            }

            releaseTile(tile);
            releaseTile(neighbor);
          }
        }

        if (tile.getWestTranslation() != null) {
          ImageTile<T> neighbor = this.grid.getTile(row, col - 1);

          if (this.grid.hasTile(neighbor)) {

            CorrelationTriple westTrans = tile.getWestTranslation();

            readTile(tile);
            readTile(neighbor);

            int xMin = westTrans.getX() - this.repeatabilty;
            int xMax = westTrans.getX() + this.repeatabilty;
            int yMin = westTrans.getY() - this.repeatabilty;
            int yMax = westTrans.getY() + this.repeatabilty;

            double oldCorr = westTrans.getCorrelation();
            CorrelationTriple bestWest;

            try {
              if (Stitching.USE_HILLCLIMBING) {
                bestWest =
                    Stitching.computeCCF_HillClimbing_LR(xMin, xMax, yMin, yMax, westTrans.getX(),
                        westTrans.getY(), neighbor, tile);
              } else {
                bestWest = Stitching.computeCCF_LR(xMin, xMax, yMin, yMax, neighbor, tile);
              }
            } catch (NullPointerException e) {
              continue;
            }

            tile.setWestTranslation(bestWest);

            if (!Double.isNaN(oldCorr)) {
              // If the old correlation was a number, then it was a good translation.
              // Increment the new translation by the value of the old correlation to increase beyond 1
              // This will enable these tiles to have higher priority in minimum spanning tree search
              tile.getWestTranslation().incrementCorrelation(Math.floor(oldCorr));
            }

            if (tile.getTileCorrelation() < bestWest.getCorrelation()) {
              tile.setTileCorrelation(bestWest.getCorrelation());
            }

            releaseTile(tile);
            releaseTile(neighbor);
          }
        }


        StitchingGuiUtils.incrementProgressBar(this.progressBar);
      } catch (InterruptedException e1) {
        Log.msg(LogType.MANDATORY, "Optimization repeatability worker interrupted.");
      }
    }
  }

  /**
   * Synchronously reads a tile
   * 
   * @param tile the tile that is to be read
   */
  private static synchronized <T> void readTile(ImageTile<T> tile) {
    if (!tile.isTileRead())
      tile.readTile();
  }

  /**
   * Synchronously releases a tile
   * 
   * @param tile the tile that is to be released
   */
  private static synchronized <T> void releaseTile(ImageTile<T> tile) {
    tile.decrementPixelDataReleaseCount();
    if (tile.getPixelDataReleaseCount() == 0)
      tile.releasePixels();
  }

  /**
   * Sets that this task has been cancelled
   */
  public void cancelExecution() {
    this.isCancelled = true;
  }

}
