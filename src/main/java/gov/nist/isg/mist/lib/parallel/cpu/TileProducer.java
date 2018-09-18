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

package gov.nist.isg.mist.lib.parallel.cpu;

import java.util.concurrent.PriorityBlockingQueue;

import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.ImageTile.State;
import gov.nist.isg.mist.lib.memorypool.DynamicMemoryPool;
import gov.nist.isg.mist.lib.parallel.common.StitchingTask;
import gov.nist.isg.mist.lib.parallel.common.StitchingTask.TaskType;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverser;

/**
 * A thread dedicated to reading and allocating memory for image tiles.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TileProducer<T> implements Runnable {

  private TileGridTraverser<ImageTile<T>> traverser;
  private PriorityBlockingQueue<StitchingTask<T>> workQueue;
  private DynamicMemoryPool<T> pool;
  private static int threadCount;

  private volatile boolean isCancelled;

  private int imageTileHeight = 0;
  private int imageTileWidth = 0;

  /**
   * Initializes a producer thread
   *
   * @param traverser   the traverser for traversing hte grid
   * @param workQueue   the work queue to pass to the next stage
   * @param pool        the pool of memory to allocate from
   * @param threadCount the total thread number producer threads
   */
  public TileProducer(TileGridTraverser<ImageTile<T>> traverser,
                      PriorityBlockingQueue<StitchingTask<T>> workQueue, DynamicMemoryPool<T> pool, int threadCount) {
    this.traverser = traverser;
    this.workQueue = workQueue;
    TileProducer.threadCount = threadCount;
    this.pool = pool;
    this.isCancelled = false;
  }

  @Override
  public void run() {
    for (ImageTile<T> tile : this.traverser) {
      if (this.isCancelled)
        break;

      tile.readTile();

      if(imageTileHeight == 0) {
        imageTileHeight = tile.getHeight();
      }
      if(imageTileWidth == 0) {
        imageTileWidth = tile.getWidth();
      }

      if(imageTileHeight != tile.getHeight() || imageTileWidth != tile.getWidth()) {
        throw new RuntimeException("All image tiles must be the same width and height. Expected image size: (" +
                imageTileWidth + "," + imageTileHeight + ") but " + tile.getFileName() + " is of size: (" +
                tile.getWidth() + ", " + tile.getHeight() + ").");
      }

      tile.setFftState(State.IN_FLIGHT);
      tile.allocateFftMemory(this.pool);
      this.workQueue.put(new StitchingTask<T>(tile, null, TaskType.FFT));
    }

    synchronized (TileProducer.class) {
      threadCount--;
    }

    if (threadCount == 0) {
      this.workQueue.put(new StitchingTask<T>(null, null, StitchingTask.TaskType.READ_DONE));
    }

  }

  /**
   * Cancels this task
   */
  public void cancel() {
    this.isCancelled = true;
  }
}
