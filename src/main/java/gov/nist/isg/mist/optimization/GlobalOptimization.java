// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.


package gov.nist.isg.mist.optimization;

import java.io.File;

import javax.swing.JProgressBar;

import gov.nist.isg.mist.gui.StitchingGuiUtils;
import gov.nist.isg.mist.gui.StitchingStatistics;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.Stitching;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.lib.tilegrid.TileGridUtils;
import gov.nist.isg.mist.optimization.model.StageModel;
import gov.nist.isg.mist.optimization.model.TranslationFilter;
import gov.nist.isg.mist.optimization.translation.refinement.TranslationRefinement;

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
   * Creates a GlobalOptimization object which can be used to optimize the pairwise translations
   * using the stage model.
   *
   * @param grid                the grid of image tiles
   * @param progressBar         a progress bar (or null if no progress bar is needed)
   * @param params              the stitching app parameters
   * @param stitchingStatistics the statistics file
   * @param isSequential        whether to use sequential or parallel execution when performing
   *                            the global  optimization.
   */
  public GlobalOptimization(TileGrid<ImageTile<T>> grid, JProgressBar progressBar,
                            StitchingAppParams params, StitchingStatistics stitchingStatistics,
                            boolean isSequential) {
    this.grid = grid;
    this.progressBar = progressBar;
    this.params = params;
    this.isSequential = isSequential;

    this.stitchingStatistics = stitchingStatistics;
    this.isCancelled = false;

    this.assembleFromMeta = params.getInputParams().isAssembleFromMetadata();
  }


  /**
   * Method to: (1) build the stage model, (2) use the stage model to optimize the pairwise image
   * translations, and (3) use the Maximum Spanning Tree to convert the optimized pairwise image
   * translations into a globally consistent image positions.
   *
   * This function modifies the image grid translations, the progress bar, and the image statistics
   * objects.
   *
   * @return true if the optimization completed, false otherwise.
   * @throws Throwable
   */
  public boolean optimize() throws Throwable {
    if (isCancelled)
      return false;
    if (assembleFromMeta)
      return true;

    // update the progressbar to show that the optimization has started
    StitchingGuiUtils.updateProgressBar(progressBar, true, "Computing Repeatability",
        "Optimization...", 0, 0, 0, false);

    // perform the pairwise translation optimization
    performGlobalTranslationOptimization();

    if (isCancelled) // check if the optimization was canceled
      return false;

    // update the progress bar to let the user know the global pairwise optimization is complete
    // and the tile composition phase has started
    StitchingGuiUtils.updateProgressBar(progressBar, true, "Composing tiles");

    // start the Global Position computation phase
    composeGlobalPositions();

    if (isCancelled) // check if the optimization was canceled
      return false;

    // set the progress bar to complete
    StitchingGuiUtils.updateProgressBarCompleted(progressBar);

    return true;
  }



  private int buildStageModel() throws Throwable {
    // start the StageModel build timer
    this.stitchingStatistics.startTimer(StitchingStatistics.RunTimers.StageModel);

    // compute the stage model
    stageModel = new StageModel<T>(grid, params, stitchingStatistics, isSequential);
    stageModel.buildModel(); // build the model from the translations
    int modelRepeatability = stageModel.getRepeatability();
    if (isCancelled)
      return modelRepeatability;

    // filter the translations using the stage model
    TranslationFilter<T> translationFilter = new TranslationFilter<T>(grid, stitchingStatistics);
    translationFilter.applyStageModel(stageModel);

    // update the repeatability to reflect the search range (to encompass +- r)
    modelRepeatability = 2 * Math.max(modelRepeatability, 1) + 1;
    Log.msg(Log.LogType.MANDATORY, "Calculated Repeatability: " + modelRepeatability + " pixels");

    // Stop the StageModel build timer
    this.stitchingStatistics.stopTimer(StitchingStatistics.RunTimers.StageModel);
    // log the stage model construction time
    Log.msg(Log.LogType.HELPFUL, "Completed Stage Model Construction in "
        + stitchingStatistics.getDuration(StitchingStatistics.RunTimers.StageModel) + "ms");

    return modelRepeatability;
  }

  private void performTranslationRefinement(int modelRepeatability) throws Throwable {
    // perform the translation refinement
    transRefinement = new TranslationRefinement<T>(grid, modelRepeatability,
        progressBar, params, isSequential);
    transRefinement.refine();
  }


  private void performGlobalTranslationOptimization() throws Throwable {
    // start the global optimization timer
    this.stitchingStatistics.startTimer(StitchingStatistics.RunTimers.GlobalOptimizationTime);

    // perform start of global optimization bookkeeping
    // backup the original translations
    TileGridUtils.backupTranslations(grid);

    // write the non-optimized relative translations to file
    File directory = new File(params.getOutputParams().getOutputPath());
    if (params.getOutputParams().isOutputMeta())
      directory.mkdirs();

    File preOptPosFile = params.getOutputParams().getRelPosNoOptFile(stitchingStatistics.getCurrentTimeSlice(),
        params.getInputParams().getNumberTimeSliceDigits());
    if (params.getOutputParams().isOutputMeta())
      Stitching.outputRelativeDisplacementsNoOptimization(grid, preOptPosFile);


    // build the stage model
    int modelRepeatability = buildStageModel();

    // check if the optimization was canceled
    if (isCancelled)
      return;

    performTranslationRefinement(modelRepeatability);

    // stop the global optimization timer
    stitchingStatistics.stopTimer(StitchingStatistics.RunTimers.GlobalOptimizationTime);
    // log the global optimization time
    Log.msg(Log.LogType.HELPFUL, "Completed Global Optimization in "
        + stitchingStatistics.getDuration(StitchingStatistics.RunTimers.GlobalOptimizationTime) + "ms");
  }

  private void composeGlobalPositions() {
    // start the Global Position computation phase
    this.stitchingStatistics.startTimer(StitchingStatistics.RunTimers.GlobalPositionTime);

    // Perform the maximum spanning tree walk of the translations to create the globally
    // consistent image tile positions
    TileGridUtils.traverseMaximumSpanningTree(grid);

    // stop the MST walk timer
    stitchingStatistics.stopTimer(StitchingStatistics.RunTimers.GlobalPositionTime);

    // log the MST global position generation time
    Log.msg(Log.LogType.HELPFUL, "Completed MST in "
        + stitchingStatistics.getDuration(StitchingStatistics.RunTimers.GlobalPositionTime) + "ms");
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
