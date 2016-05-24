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

package gov.nist.isg.mist.optimization;

import java.io.File;

import javax.swing.*;

import gov.nist.isg.mist.optimization.model.StageModel;
import gov.nist.isg.mist.optimization.model.TranslationFilter;
import gov.nist.isg.mist.optimization.translationrefinement.TranslationRefinement;
import gov.nist.isg.mist.stitching.gui.StitchingGuiUtils;
import gov.nist.isg.mist.stitching.gui.StitchingStatistics;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGridUtils;

/**
 * Stitching global optimization.
 *
 * @author Michael Majurski
 */
public class GlobalOptimization<T> {

  private TileGrid<ImageTile<T>> grid;
  private JProgressBar progressBar;
  private StitchingAppParams params;
  private StitchingStatistics stitchingStatistics;
  private TranslationRefinement<T> transRefinement;
  private StageModel<T> stageModel;

  private volatile boolean isCancelled;

  private boolean isSequential;
  private boolean assembleFromMeta = false;


  /**
   * Initializes the optimization technique using the stage model.
   *
   * @param grid                the grid of image tiles
   * @param progressBar         a progress bar (or null if no progress bar is needed)
   * @param params              the stitching app parameters
   * @param stitchingStatistics the statistics file
   * @param isSequential        whether to sequential or parallel execution to perform the global
   *                            optimization.
   * @param assembleFromMeta    whether to assemble from metadata.
   */
  public GlobalOptimization(TileGrid<ImageTile<T>> grid, JProgressBar progressBar,
                            StitchingAppParams params, StitchingStatistics stitchingStatistics,
                            boolean isSequential, boolean assembleFromMeta) {
    this.grid = grid;
    this.progressBar = progressBar;
    this.params = params;
    this.isSequential = isSequential;

    this.stitchingStatistics = stitchingStatistics;
    this.isCancelled = false;

    this.assembleFromMeta = assembleFromMeta;
  }


  public boolean optimize() throws Throwable {
    if (isCancelled)
      return false;
    if (assembleFromMeta)
      return true;

    StitchingGuiUtils.updateProgressBar(progressBar, true, "Computing optimization");

    this.stitchingStatistics.startTimer(StitchingStatistics.RunTimers.GlobalOptimizationTime);


    // backup the original translations
    TileGridUtils.backupTranslations(grid);

    // update the progressbar to show that the optimization has started
    StitchingGuiUtils.updateProgressBar(progressBar, true, "Computing Repeatability",
        "Optimization...", 0, 0, 0, false);

    // write the non-optimized relative translations to file
    File directory = new File(params.getOutputParams().getOutputPath());
    if (params.getOutputParams().isOutputMeta())
      directory.mkdirs();

    File preOptPosFile = params.getOutputParams().getRelPosNoOptFile(stitchingStatistics.getCurrentTimeSlice(),
        params.getInputParams().getNumberTimeSliceDigits());
    if (params.getOutputParams().isOutputMeta())
      Stitching.outputRelativeDisplacementsNoOptimization(grid, preOptPosFile);


    this.stitchingStatistics.startTimer(StitchingStatistics.RunTimers.StageModel);
    // compute the stage model
    stageModel = new StageModel<T>(grid, params, stitchingStatistics, isSequential);
    stageModel.buildModel(); // build the model from the translations
    int modelRepeatability = stageModel.getRepeatability();
    if (isCancelled)
      return false;

    // filter the translations using the stage model
    TranslationFilter<T> translationFilter = new TranslationFilter<T>(grid, params, stitchingStatistics);
    translationFilter.applyStageModel(stageModel);

    // update the repeatability to reflect the search range (to encompass +- r)
    modelRepeatability = 2 * Math.max(modelRepeatability, 1) + 1;
    Log.msg(Log.LogType.MANDATORY, "Calculated Repeatability: " + modelRepeatability + " pixels");

    this.stitchingStatistics.stopTimer(StitchingStatistics.RunTimers.StageModel);
    Log.msg(Log.LogType.HELPFUL, "Completed Stage Model Construction in "
        + stitchingStatistics.getDuration(StitchingStatistics.RunTimers.StageModel) + "ms");

    if (isCancelled)
      return false;
    // perform the translation refinement
    transRefinement = new TranslationRefinement<T>(grid, modelRepeatability,
        progressBar, params, isSequential);
    transRefinement.refine();

    stitchingStatistics.stopTimer(StitchingStatistics.RunTimers.GlobalOptimizationTime);

    Log.msg(Log.LogType.HELPFUL, "Completed Global Optimization in "
        + stitchingStatistics.getDuration(StitchingStatistics.RunTimers.GlobalOptimizationTime) + "ms");

    StitchingGuiUtils.updateProgressBar(progressBar, true, "Composing tiles");

    this.stitchingStatistics.startTimer(StitchingStatistics.RunTimers.GlobalPositionTime);
    TileGridUtils.traverseMaximumSpanningTree(grid);
    if (isCancelled)
      return false;

    stitchingStatistics.stopTimer(StitchingStatistics.RunTimers.GlobalPositionTime);

    Log.msg(Log.LogType.HELPFUL, "Completed MST in "
        + stitchingStatistics.getDuration(StitchingStatistics.RunTimers.GlobalPositionTime) + "ms");

    StitchingGuiUtils.updateProgressBarCompleted(progressBar);

    return true;
  }

  public void cancel() {
    Log.msg(Log.LogType.MANDATORY, "Canceling Global Optimization");
    isCancelled = true;
    if (stageModel != null)
      stageModel.cancel();
    if (transRefinement != null)
      transRefinement.cancel();
  }


}
