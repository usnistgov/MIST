// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.


package gov.nist.isg.mist.optimization.translation.refinement;

import javax.swing.JProgressBar;

import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;

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
