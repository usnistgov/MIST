// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.


package gov.nist.isg.mist.optimization.translation.refinement;

import javax.swing.JProgressBar;

import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;

/**
 * Translation refinement abstract interface class for all translation refinement executors.
 *
 * @author Michael Majurski
 */
public abstract class TransRefinementExecutorInterface<T> implements Thread.UncaughtExceptionHandler {

  protected volatile boolean exceptionThrown;
  protected Throwable workerThrowable;

  protected TileGrid<ImageTile<T>> grid;
  protected int modelRepeatability;
  protected JProgressBar progressBar;
  protected StitchingAppParams params;

  /**
   * Translation refinement abstract interface class for all translation refinement executors.
   *
   * @param grid               the TileGrid to refine the translations of.
   * @param modelRepeatability the stage model repeatability.
   * @param progressBar        the GUI progress bar.
   * @param params             the stitching parameters.
   */
  public TransRefinementExecutorInterface(TileGrid<ImageTile<T>> grid, int modelRepeatability,
                                          JProgressBar progressBar,
                                          StitchingAppParams params) {
    this.grid = grid;
    this.modelRepeatability = modelRepeatability;
    this.progressBar = progressBar;
    this.params = params;

    exceptionThrown = false;
    workerThrowable = null;
  }


  public abstract void cancel();

  public abstract void execute();

  /**
   * If an uncaught exception happens in a worker this notifies the parent.
   *
   * @param t worker thread
   * @param e throwable thrown
   */
  @Override
  public void uncaughtException(Thread t, Throwable e) {
    this.exceptionThrown = true;
    this.workerThrowable = e;

    this.cancel();
  }

  /**
   * Determine if an uncaught worker exception was thrown.
   *
   * @return whether or not any of the workers have thrown an uncaught exception.
   */
  public boolean isExceptionThrown() {
    return this.exceptionThrown;
  }

  /**
   * Get a reference to the worker exception thrown, if any.
   *
   * @return the exception thrown by a worker, or null.
   */
  public Throwable getWorkerThrowable() {
    return this.workerThrowable;
  }

}
