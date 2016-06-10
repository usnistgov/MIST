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

package gov.nist.isg.mist.optimization.translation.refinement;

import javax.swing.*;

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
