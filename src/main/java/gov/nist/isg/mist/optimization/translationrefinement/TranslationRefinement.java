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

import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;

/**
 * Class to setup and perform translation refinement.
 *
 * @author Michael Majurski
 */
public class TranslationRefinement<T> {

  private TileGrid<ImageTile<T>> grid;
  private int modelRepeatability;
  private JProgressBar progressBar;

  TransRefinementExecutorInterface<T> executor;
  private StitchingAppParams params;
  private volatile boolean isCancelled;

  private boolean isSequential;

  /**
   * Translation refinement to find the best translations between images that are consistent with
   * the stitching stage model.
   *
   * @param grid               the TileGrid upon which to refine translations.
   * @param modelRepeatability the stage model repeatability.
   * @param progressBar        the GUI progress bar.
   * @param params             the stitching parameters.
   * @param isSequential       whether to perform the refinement sequentially, or use parallel
   *                           workers.
   */
  public TranslationRefinement(TileGrid<ImageTile<T>> grid, int modelRepeatability,
                               JProgressBar progressBar,
                               StitchingAppParams params, boolean isSequential) {

    this.grid = grid;
    this.modelRepeatability = modelRepeatability;
    this.progressBar = progressBar;
    this.params = params;
    this.isSequential = isSequential;
    this.isCancelled = false;
  }

  /**
   * Cancel the translation refinement.
   */
  public void cancel() {
    Log.msg(Log.LogType.MANDATORY, "Canceling Translation Refinement");
    this.isCancelled = true;
    if (executor != null)
      executor.cancel();
  }

  /**
   * Refine the translations.
   *
   * @return whether translation refinement was successful or not.
   */
  @SuppressWarnings("unchecked")
  public boolean refine() throws Throwable {

    if (!isSequential) {
      executor = new TransRefinementParallelExecutor(grid, modelRepeatability, progressBar, params);
    } else {
      executor = new TransRefinementSequentialExecutor(grid, modelRepeatability, progressBar, params);
    }
    executor.execute();

    // check that the executor did no throw and error
    if (executor.isExceptionThrown())
      throw executor.getWorkerThrowable();

    return !this.isCancelled;
  }

}
