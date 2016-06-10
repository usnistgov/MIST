// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 3:59:00 PM EST
//
// Time-stamp: <Aug 1, 2013 3:59:00 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.optimization.workflow.tasks;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverser;
import gov.nist.isg.mist.optimization.workflow.data.OptimizationData;

/**
 * A thread dedicated to reading and allocating memory for image tiles.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TileProducer<T> implements Runnable {

  private TileGridTraverser<ImageTile<T>> traverser;
  private BlockingQueue<OptimizationData<T>> workQueue;
  private Semaphore sem;

  private volatile boolean isCancelled;

  /**
   * Initializes a producer thread
   *
   * @param traverser the traverser
   * @param workQueue the work queue
   */
  public TileProducer(TileGridTraverser<ImageTile<T>> traverser,
                      BlockingQueue<OptimizationData<T>> workQueue, Semaphore sem) {
    this.traverser = traverser;
    this.workQueue = workQueue;
    this.sem = sem;
    this.isCancelled = false;
  }

  @Override
  public void run() {
    for (ImageTile<T> tile : this.traverser) {
      if (this.isCancelled)
        break;

      try {
        if (sem != null) {
          sem.acquire();
        }

        tile.readTile();

      } catch (InterruptedException e) {
        Log.msg(Log.LogType.MANDATORY, "Tile producer Interrupted");
        return;
      }

      this.workQueue.add(new OptimizationData<T>(tile, OptimizationData.TaskType.BK_CHECK_NEIGHBORS));

    }

  }

  /**
   * Cancels the task
   */
  public void cancel() {
    this.isCancelled = true;
    if (this.sem != null)
      this.sem.release();
  }
}
