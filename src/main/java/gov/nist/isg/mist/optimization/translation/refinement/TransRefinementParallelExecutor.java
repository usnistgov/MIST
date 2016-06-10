// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.


package gov.nist.isg.mist.optimization.translation.refinement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

import javax.swing.JProgressBar;

import gov.nist.isg.mist.gui.StitchingGuiUtils;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverser;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverserFactory;
import gov.nist.isg.mist.optimization.workflow.data.OptimizationData;
import gov.nist.isg.mist.optimization.workflow.tasks.BookKeeper;
import gov.nist.isg.mist.optimization.workflow.tasks.TileProducer;

/**
 * Translation refinement parallel executor.
 *
 * @author Michael Majurski
 */
public class TransRefinementParallelExecutor<T> extends TransRefinementExecutorInterface {


  private List<TransRefinementWorker<T>> workers;
  private TileProducer<T> producer;
  private BookKeeper<T> bk;
  private List<Thread> executionThreads;
  private volatile boolean isCancelled = false;

  /**
   * Translation refinement parallel executor.
   *
   * @param grid               the TileGrid to refine the translations of.
   * @param modelRepeatability the stage model repeatability.
   * @param progressBar        the GUI progress bar.
   * @param params             the stitching parameters.
   */
  public TransRefinementParallelExecutor(TileGrid grid, int modelRepeatability,
                                         JProgressBar progressBar,
                                         StitchingAppParams params) {
    super(grid, modelRepeatability, progressBar, params);
  }


  @Override
  public void cancel() {
    Log.msg(Log.LogType.MANDATORY, "Canceling Translation Refinement Parallel Executor");
    this.isCancelled = true;
    if (this.workers != null) {
      for (TransRefinementWorker<T> worker : this.workers)
        worker.cancelExecution();
    }

    if (this.bk != null)
      this.bk.cancel();

    if (this.producer != null)
      this.producer.cancel();
  }


  @Override
  public void execute() {

    StitchingGuiUtils.updateProgressBar(progressBar, false, null, "Optimization...", 0,
        grid.getExtentHeight() * grid.getExtentWidth(), 0, false);

    // Reset pixel release counts if we must manage pixel data memory
    if (ImageTile.freePixelData()) {
      for (int r = 0; r < grid.getExtentHeight(); r++) {
        for (int c = 0; c < grid.getExtentWidth(); c++) {
          grid.getSubGridTile(r, c).resetPixelReleaseCount(grid.getExtentWidth(),
              grid.getExtentHeight(), grid.getStartRow(), grid.getStartCol());
          grid.getSubGridTile(r, c).releasePixels();
        }
      }
    }

    int numThreads = params.getAdvancedParams().getNumCPUThreads();

    executionThreads = new ArrayList<Thread>();
    workers = new ArrayList<TransRefinementWorker<T>>();

    // setup the grid traverser
    TileGridTraverser<ImageTile<T>> traverser =
        TileGridTraverserFactory.makeTraverser(TileGridTraverser.Traversals.DIAGONAL, grid);

    Semaphore sem = null;

    if (ImageTile.freePixelData()) {
      int numPermits = Math.min(grid.getExtentWidth(), grid.getExtentHeight()) + 2 + numThreads;
      sem = new Semaphore(numPermits, true);
    }

    BlockingQueue<OptimizationData<T>> tileQueue = new ArrayBlockingQueue<OptimizationData<T>>(grid.getSubGridSize() * 2);
    BlockingQueue<OptimizationData<T>> bkQueue = new ArrayBlockingQueue<OptimizationData<T>>(grid.getSubGridSize() * 2);

    producer = new TileProducer<T>(traverser, bkQueue, sem);
    bk = new BookKeeper<T>(bkQueue, tileQueue, sem, grid);


    Thread tmp;
    for (int i = 0; i < numThreads; i++) {
      TransRefinementWorker<T> worker = new TransRefinementWorker<T>(tileQueue,
          bkQueue, modelRepeatability, params.getAdvancedParams().getTranslationRefinementType(),
          params.getAdvancedParams().getNumTranslationRefinementStartPoints(),
          progressBar);
      workers.add(worker);
      tmp = new Thread(worker);
      // set the uncaught exception handler to this class to workers throwing exceptions can be
      // handled
      tmp.setUncaughtExceptionHandler(this);
      tmp.setName("OptimizationWorker");
      executionThreads.add(tmp);
    }

    Thread producerThread = new Thread(producer);
    producerThread.setName("OptimizationProducer");
    Thread bkThread = new Thread(bk);
    bkThread.setName("OptimizationBk");

    producerThread.start();
    bkThread.start();

    for (Thread thread : executionThreads)
      thread.start();

    try {
      producerThread.join();
      bkThread.join();

      for (Thread thread : executionThreads)
        thread.join();

    } catch (InterruptedException e) {
      cancel();
      Log.msg(Log.LogType.MANDATORY, e.getMessage());
    }
  }


}
