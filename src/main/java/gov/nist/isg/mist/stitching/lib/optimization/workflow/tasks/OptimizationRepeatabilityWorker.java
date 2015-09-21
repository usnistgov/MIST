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

package gov.nist.isg.mist.stitching.lib.optimization.workflow.tasks;

import gov.nist.isg.mist.stitching.gui.StitchingGuiUtils;
import gov.nist.isg.mist.stitching.lib.common.CorrelationTriple;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.optimization.OptimizationUtils;
import gov.nist.isg.mist.stitching.lib.optimization.workflow.data.OptimizationData;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;

import javax.swing.*;

import java.util.concurrent.BlockingQueue;

/**
 * Class that represents a thread that computes a cross correlation search.
 * 
 * @author Tim Blattner
 * @version 1.0
 * @param <T>
 */
public class OptimizationRepeatabilityWorker<T> implements Runnable {

  private static final long TIMEOUT = 1000L;


  private BlockingQueue<OptimizationData<T>> tiles;
  private BlockingQueue<OptimizationData<T>> bkQueue;
  private TileGrid<ImageTile<T>> grid;
  private JProgressBar progressBar;

  private int repeatabilty;
  private volatile boolean isCancelled;
  private static boolean bkDone = false;


  /**
   * Creates an optimization repeatability worker for executing a cross correlation search.
   * 
   * @param queue the queue of tiles to be processed
   * @param bkQueue the bookkeeper queue
   * @param grid the grid of tiles
   * @param repeatabilty the repeatability of the microscope
   * @param progressBar the progress bar
   */
  public OptimizationRepeatabilityWorker(BlockingQueue<OptimizationData<T>> queue, BlockingQueue<OptimizationData<T>> bkQueue,
                                         TileGrid<ImageTile<T>> grid, int repeatabilty, JProgressBar progressBar) {
    this.tiles = queue;
    this.bkQueue = bkQueue;
    this.grid = grid;
    this.progressBar = progressBar;
    this.repeatabilty = repeatabilty;
    this.isCancelled = false;
    bkDone = false;
  }

  @Override
  public void run() {

    while (!this.isCancelled && !bkDone) {
      OptimizationData<T> task;

      try {
        task = this.tiles.take();

        if (task.getType() == OptimizationData.TaskType.CANCELLED) {
          this.isCancelled = true;
        }


        ImageTile<T> tile = task.getTile();

        if (task.getType() == OptimizationData.TaskType.OPTIMIZE_NORTH) {
          ImageTile<T> neighbor = task.getNeighbor();

          CorrelationTriple northTrans = tile.getNorthTranslation();

          int xMin = northTrans.getX() - this.repeatabilty;
          int xMax = northTrans.getX() + this.repeatabilty;
          int yMin = northTrans.getY() - this.repeatabilty;
          int yMax = northTrans.getY() + this.repeatabilty;

          double oldCorr = northTrans.getCorrelation();
          CorrelationTriple bestNorth;
          try {
            if (Stitching.USE_HILLCLIMBING) {

              if(Stitching.USE_EXHAUSTIVE_INSTEAD_OF_HILLCLIMB_SEARCH) {
                bestNorth =
                    Stitching.computeCCF_Exhaustive_UD(xMin, xMax, yMin, yMax, northTrans.getX(),
                                                       northTrans.getY(), neighbor, tile);
              }else{
                bestNorth =
                    Stitching.computeCCF_HillClimbing_UD(xMin, xMax, yMin, yMax, northTrans.getX(),
                                                         northTrans.getY(), neighbor, tile);
              }


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
            tile.getNorthTranslation().incrementCorrelation(OptimizationUtils.CorrelationWeight);
          }

          if (tile.getTileCorrelation() < bestNorth.getCorrelation()) {
            tile.setTileCorrelation(bestNorth.getCorrelation());
          }

          task.setType(OptimizationData.TaskType.BK_CHECK_MEMORY);
          bkQueue.add(task);

        } else if (task.getType() == OptimizationData.TaskType.OPTIMIZE_WEST) {
          ImageTile<T> neighbor = task.getNeighbor();

          CorrelationTriple westTrans = tile.getWestTranslation();

          int xMin = westTrans.getX() - this.repeatabilty;
          int xMax = westTrans.getX() + this.repeatabilty;
          int yMin = westTrans.getY() - this.repeatabilty;
          int yMax = westTrans.getY() + this.repeatabilty;

          double oldCorr = westTrans.getCorrelation();
          CorrelationTriple bestWest;

          try {
            if (Stitching.USE_HILLCLIMBING) {
              if(Stitching.USE_EXHAUSTIVE_INSTEAD_OF_HILLCLIMB_SEARCH) {
                bestWest =
                    Stitching.computeCCF_Exhaustive_LR(xMin, xMax, yMin, yMax, westTrans.getX(),
                                                       westTrans.getY(), neighbor, tile);
              }else {
                bestWest =
                    Stitching.computeCCF_HillClimbing_LR(xMin, xMax, yMin, yMax, westTrans.getX(),
                                                         westTrans.getY(), neighbor, tile);
              }
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
            tile.getWestTranslation().incrementCorrelation(OptimizationUtils.CorrelationWeight);
          }

          if (tile.getTileCorrelation() < bestWest.getCorrelation()) {
            tile.setTileCorrelation(bestWest.getCorrelation());
          }

          task.setType(OptimizationData.TaskType.BK_CHECK_MEMORY);
          bkQueue.add(task);

        } else if (task.getType() == OptimizationData.TaskType.BK_DONE) {
          bkDone = true;
          tiles.add(task);

        }

        StitchingGuiUtils.incrementProgressBar(this.progressBar);
      } catch (InterruptedException e1) {
        Log.msg(LogType.MANDATORY, "Optimization repeatability worker interrupted.");
      }
    }
  }


  /**
   * Sets that this task has been cancelled
   */
  public void cancelExecution() {
    this.isCancelled = true;
    this.tiles.add(new OptimizationData<T>(null, null, OptimizationData.TaskType.CANCELLED));
  }

}
