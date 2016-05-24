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

import javax.swing.*;

import gov.nist.isg.mist.optimization.model.TranslationFilter;
import gov.nist.isg.mist.stitching.gui.StitchingGuiUtils;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.lib.common.CorrelationTriple;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverser;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverserFactory;

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
