// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.


package gov.nist.isg.mist.optimization.translation.refinement;

import java.util.concurrent.BlockingQueue;

import javax.swing.JProgressBar;

import gov.nist.isg.mist.gui.StitchingGuiUtils;
import gov.nist.isg.mist.lib.common.CorrelationTriple;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.Stitching;
import gov.nist.isg.mist.lib.imagetile.Stitching.TranslationRefinementType;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.optimization.model.TranslationFilter;
import gov.nist.isg.mist.optimization.workflow.data.OptimizationData;

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
