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

package gov.nist.isg.mist.optimization.translationrefinement;

import java.util.concurrent.BlockingQueue;

import javax.swing.*;

import gov.nist.isg.mist.optimization.model.TranslationFilter;
import gov.nist.isg.mist.optimization.workflow.data.OptimizationData;
import gov.nist.isg.mist.stitching.gui.StitchingGuiUtils;
import gov.nist.isg.mist.stitching.lib.common.CorrelationTriple;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching.TranslationRefinementType;
import gov.nist.isg.mist.stitching.lib.log.Log;

/**
 * Translation refinement worker.
 *
 * @author Michael Majurski
 */
public class TransRefinementWorker<T> implements Runnable {

  private BlockingQueue<OptimizationData<T>> tiles;
  private BlockingQueue<OptimizationData<T>> bkQueue;
  private JProgressBar progressBar;

  private int repeatability;
  private volatile boolean isCancelled;
  private static boolean bkDone = false;
  private TranslationRefinementType translationRefinementType;
  private int numHillClimbStartPoints;

  /**
   * Creates an optimization repeatability worker for executing a cross correlation search.
   *
   * @param queue         the queue of tiles to be processed
   * @param bkQueue       the bookkeeper queue
   * @param repeatability the repeatability of the microscope
   * @param progressBar   the progress bar
   */
  public TransRefinementWorker(BlockingQueue<OptimizationData<T>> queue, BlockingQueue<OptimizationData<T>> bkQueue,
                               int repeatability, TranslationRefinementType type, int
                                   numHillClimbStartPoints,
                               JProgressBar progressBar) {
    this.tiles = queue;
    this.bkQueue = bkQueue;
    this.progressBar = progressBar;
    this.repeatability = repeatability;
    this.isCancelled = false;
    bkDone = false;
    this.translationRefinementType = type;
    this.numHillClimbStartPoints = numHillClimbStartPoints;
  }

  @Override
  public void run() {

    while (!this.isCancelled && !bkDone) {
      OptimizationData<T> task;

      try {
        task = this.tiles.take();

        if (task.getType() == OptimizationData.TaskType.CANCELLED) {
          this.tiles.add(new OptimizationData<T>(null, null, OptimizationData.TaskType.CANCELLED));
          this.isCancelled = true;
        }


        ImageTile<T> tile = task.getTile();

        if (task.getType() == OptimizationData.TaskType.OPTIMIZE_NORTH) {
          ImageTile<T> neighbor = task.getNeighbor();
          CorrelationTriple northTrans = tile.getNorthTranslation();
          if (tile.fileExists() && neighbor.fileExists()) {
            int xMin = northTrans.getX() - this.repeatability;
            int xMax = northTrans.getX() + this.repeatability;
            int yMin = northTrans.getY() - this.repeatability;
            int yMax = northTrans.getY() + this.repeatability;

            double oldCorr = northTrans.getCorrelation();
            CorrelationTriple bestNorth = null;

            switch (translationRefinementType) {
              case SINGLE_HILL_CLIMB:
                bestNorth = Stitching.computeCCF_HillClimbing(xMin, xMax, yMin, yMax, northTrans
                    .getX(), northTrans.getY(), neighbor, tile);
                break;
              case MULTI_POINT_HILL_CLIMB:
                bestNorth = Stitching.computeCCF_MultiPoint_HillClimbing(xMin, xMax, yMin, yMax,
                    northTrans.getX(), northTrans.getY(), numHillClimbStartPoints, neighbor, tile);
                break;
              case EXHAUSTIVE:
                bestNorth = Stitching.computeCCF_Exhaustive(xMin, xMax, yMin, yMax, northTrans.getX(),
                    northTrans.getY(), neighbor, tile);
                break;
            }

            tile.setNorthTranslation(bestNorth);

            if (!Double.isNaN(oldCorr)) {
              // If the old correlation was a number, then it was a good translation.
              // Increment the new translation by the value of the old correlation to increase beyond 1
              // This will enable these tiles to have higher priority in minimum spanning tree search
              tile.getNorthTranslation().incrementCorrelation(TranslationFilter.CorrelationWeight);
            }

            if (tile.getTileCorrelation() < bestNorth.getCorrelation()) {
              tile.setTileCorrelation(bestNorth.getCorrelation());
            }

          } else {
            // this translation connects at least one non-existent image tile
            tile.getNorthTranslation().setCorrelation(-1.0);
          }

          task.setType(OptimizationData.TaskType.BK_CHECK_MEMORY);
          bkQueue.add(task);

        } else if (task.getType() == OptimizationData.TaskType.OPTIMIZE_WEST) {
          ImageTile<T> neighbor = task.getNeighbor();
          CorrelationTriple westTrans = tile.getWestTranslation();
          if (tile.fileExists() && neighbor.fileExists()) {
            int xMin = westTrans.getX() - this.repeatability;
            int xMax = westTrans.getX() + this.repeatability;
            int yMin = westTrans.getY() - this.repeatability;
            int yMax = westTrans.getY() + this.repeatability;

            double oldCorr = westTrans.getCorrelation();
            CorrelationTriple bestWest = null;

            switch (translationRefinementType) {
              case SINGLE_HILL_CLIMB:
                bestWest = Stitching.computeCCF_HillClimbing(xMin, xMax, yMin, yMax, westTrans.getX(),
                    westTrans.getY(), neighbor, tile);
                break;
              case MULTI_POINT_HILL_CLIMB:
                bestWest = Stitching.computeCCF_MultiPoint_HillClimbing(xMin, xMax, yMin, yMax,
                    westTrans.getX(), westTrans.getY(), numHillClimbStartPoints, neighbor, tile);
                break;
              case EXHAUSTIVE:
                bestWest = Stitching.computeCCF_Exhaustive(xMin, xMax, yMin, yMax, westTrans.getX(),
                    westTrans.getY(), neighbor, tile);
                break;
            }

            tile.setWestTranslation(bestWest);

            if (!Double.isNaN(oldCorr)) {
              // If the old correlation was a number, then it was a good translation.
              // Increment the new translation by the value of the old correlation to increase beyond 1
              // This will enable these tiles to have higher priority in minimum spanning tree search
              tile.getWestTranslation().incrementCorrelation(TranslationFilter.CorrelationWeight);
            }

            if (tile.getTileCorrelation() < bestWest.getCorrelation()) {
              tile.setTileCorrelation(bestWest.getCorrelation());
            }

          } else {
            // this translation connects at least one non-existent image tile
            tile.getWestTranslation().setCorrelation(-1.0);
          }

          task.setType(OptimizationData.TaskType.BK_CHECK_MEMORY);
          bkQueue.add(task);

        } else if (task.getType() == OptimizationData.TaskType.BK_DONE) {
          bkDone = true;
          tiles.add(task);
        }

        StitchingGuiUtils.incrementProgressBar(this.progressBar);
      } catch (InterruptedException e1) {
        Log.msg(Log.LogType.MANDATORY, "Optimization repeatability worker interrupted.");
      }
    }
  }


  public void cancelExecution() {
    this.isCancelled = true;
    this.tiles.add(new OptimizationData<T>(null, null, OptimizationData.TaskType.CANCELLED));
  }

}
