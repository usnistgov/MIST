// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.


package gov.nist.isg.mist.optimization.translation.refinement;

import javax.swing.JProgressBar;

import gov.nist.isg.mist.gui.StitchingGuiUtils;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.lib.common.CorrelationTriple;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.Stitching;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverser;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverserFactory;
import gov.nist.isg.mist.optimization.model.TranslationFilter;

/**
 * Translation refinement sequential executor.
 *
 * @author Michael Majurski
 */
public class TransRefinementSequentialExecutor<T> extends TransRefinementExecutorInterface {

  private volatile boolean isCancelled = false;

  /**
   * Translation refinement parallel executor.
   *
   * @param grid               the TileGrid to refine the translations of.
   * @param modelRepeatability the stage model repeatability.
   * @param progressBar        the GUI progress bar.
   * @param params             the stitching parameters.
   */
  public TransRefinementSequentialExecutor(TileGrid grid, int modelRepeatability,
                                           JProgressBar progressBar,
                                           StitchingAppParams params) {
    super(grid, modelRepeatability, progressBar, params);
  }


  @Override
  public void cancel() {
    Log.msg(Log.LogType.MANDATORY, "Canceling Translation Refinement Sequential Executor");
    isCancelled = true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void execute() {
    StitchingGuiUtils.updateProgressBar(progressBar, false, null, "Optimization...", 0,
        grid.getExtentHeight() * grid.getExtentWidth(), 0, false);

    TileGridTraverser<ImageTile<T>> traverser =
        TileGridTraverserFactory.makeTraverser(TileGridTraverser.Traversals.DIAGONAL, this.grid);

    // loop over the image tiles
    for (ImageTile<T> t : traverser) {
      if (this.isCancelled) return;

      t.readTile();

      int row = t.getRow();
      int col = t.getCol();

      // optimize with west neighbor
      if (col > grid.getStartCol()) {
        ImageTile<T> west = grid.getTile(row, col - 1);
        CorrelationTriple westTrans = t.getWestTranslation();
        if (t.fileExists() && west.fileExists()) {
          west.readTile();

          int xMin = westTrans.getX() - modelRepeatability;
          int xMax = westTrans.getX() + modelRepeatability;
          int yMin = westTrans.getY() - modelRepeatability;
          int yMax = westTrans.getY() + modelRepeatability;

          double oldCorr = westTrans.getCorrelation();
          CorrelationTriple bestWest = null;

          switch (params.getAdvancedParams().getTranslationRefinementType()) {
            case SINGLE_HILL_CLIMB:
              bestWest = Stitching.computeCCF_HillClimbing(xMin, xMax, yMin, yMax, westTrans.getX(),
                  westTrans.getY(), west, t);
              break;
            case MULTI_POINT_HILL_CLIMB:
              bestWest = Stitching.computeCCF_MultiPoint_HillClimbing(xMin, xMax, yMin, yMax,
                  westTrans.getX(), westTrans.getY(), params.getAdvancedParams()
                      .getNumTranslationRefinementStartPoints(), west, t);
              break;
            case EXHAUSTIVE:
              bestWest = Stitching.computeCCF_Exhaustive(xMin, xMax, yMin, yMax, westTrans.getX(),
                  westTrans.getY(), west, t);
              break;
          }

          t.setWestTranslation(bestWest);

          if (!Double.isNaN(oldCorr)) {
            // If the old correlation was a number, then it was a good translation.
            // Increment the new translation by the value of the old correlation to increase beyond 1
            // This will enable these tiles to have higher priority in minimum spanning tree search
            t.getWestTranslation().incrementCorrelation(TranslationFilter.CorrelationWeight);
          }

          if (t.getTileCorrelation() < bestWest.getCorrelation())
            t.setTileCorrelation(bestWest.getCorrelation());


          if (progressBar != null)
            StitchingGuiUtils.incrementProgressBar(progressBar);

          west.releasePixels();
        } else {
          // this translation connects at least one non-existent image tile
          t.getWestTranslation().setCorrelation(-1.0);
        }
      }


      // optimize with north neighbor
      if (row > grid.getStartRow()) {
        ImageTile<T> north = grid.getTile(row - 1, col);
        CorrelationTriple northTrans = t.getNorthTranslation();
        if (t.fileExists() && north.fileExists()) {
          north.readTile();

          int xMin = northTrans.getX() - modelRepeatability;
          int xMax = northTrans.getX() + modelRepeatability;
          int yMin = northTrans.getY() - modelRepeatability;
          int yMax = northTrans.getY() + modelRepeatability;

          double oldCorr = northTrans.getCorrelation();
          CorrelationTriple bestNorth = null;

          switch (params.getAdvancedParams().getTranslationRefinementType()) {
            case SINGLE_HILL_CLIMB:
              bestNorth = Stitching.computeCCF_HillClimbing(xMin, xMax, yMin, yMax, northTrans
                  .getX(), northTrans.getY(), north, t);
              break;
            case MULTI_POINT_HILL_CLIMB:
              bestNorth = Stitching.computeCCF_MultiPoint_HillClimbing(xMin, xMax, yMin, yMax,
                  northTrans.getX(), northTrans.getY(), params.getAdvancedParams()
                      .getNumTranslationRefinementStartPoints(), north, t);
              break;
            case EXHAUSTIVE:
              bestNorth = Stitching.computeCCF_Exhaustive(xMin, xMax, yMin, yMax, northTrans.getX(),
                  northTrans.getY(), north, t);
              break;
          }

          t.setNorthTranslation(bestNorth);
          if (!Double.isNaN(oldCorr)) {
            // If the old correlation was a number, then it was a good translation.
            // Increment the new translation by the value of the old correlation to increase beyond 1
            // This will enable these tiles to have higher priority in minimum spanning tree search
            t.getNorthTranslation().incrementCorrelation(TranslationFilter.CorrelationWeight);
          }

          if (t.getTileCorrelation() < bestNorth.getCorrelation())
            t.setTileCorrelation(bestNorth.getCorrelation());

          if (progressBar != null)
            StitchingGuiUtils.incrementProgressBar(progressBar);

          north.releasePixels();
        } else {
          // this translation connects at least one non-existent image tile
          t.getNorthTranslation().setCorrelation(-1.0);
        }
      }

      t.releasePixels();
    }
  }
}
