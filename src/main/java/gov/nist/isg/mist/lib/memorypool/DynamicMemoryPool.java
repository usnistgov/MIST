// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 3:52:24 PM EST
//
// Time-stamp: <Aug 1, 2013 3:52:24 PM tjb3>
//
// ================================================================

package gov.nist.isg.mist.lib.memorypool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import jcuda.CudaException;

/**
 * A memory pool that can optionally grow dynamically if all memory in the pool is taken. If the
 * pool does not grow dynamically, then the thread trying to obtain memory will wait until more
 * memory is added into the pool.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class DynamicMemoryPool<T> {

  private BlockingQueue<T> memoryQueue;
  private boolean dynamic;
  private Allocator<T> allocator;
  private int[] sz;

  /**
   * Allocates a dynamic memory pool given a size, a type of tile and whether the pool is
   * dynamically growing or not
   *
   * @param queueSize the size of the queue
   * @param dynamic   whether the pool is dynamic or not
   * @param allocator the allocator
   * @param sz        the size of the pool
   */
  public DynamicMemoryPool(int queueSize, boolean dynamic, Allocator<T> allocator, int... sz)
      throws OutOfMemoryError, CudaException {
    this.allocator = allocator;
    this.sz = sz;
    this.dynamic = dynamic;
    Collection<T> values = new ArrayList<T>(queueSize);

    for (int i = 0; i < queueSize; i++) {
      values.add(allocator.allocate(sz));
    }

    if (dynamic)
      queueSize *= 2;

    this.memoryQueue = new ArrayBlockingQueue<T>(queueSize, false, values);

  }

  /**
   * Releases all memory from this pool
   */
  public void releaseAll() {
    for (T p : this.memoryQueue) {
      p = this.allocator.deallocate(p);
    }

    System.gc();
  }

  /**
   * Gets pointer memory from the pool
   *
   * @return the memory
   */
  public T getMemory() {
    try {
      if (this.memoryQueue.peek() == null && this.dynamic) {
        // Add a piece of memory
        this.memoryQueue.offer(this.allocator.allocate(this.sz));
      }

      return this.memoryQueue.take();
    } catch (InterruptedException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
      return null;
    }
  }

  /**
   * Adds java memory to the pool
   *
   * @param o the java memory
   */
  public void addMemory(T o) {
    try {
      this.memoryQueue.put(o);
    } catch (InterruptedException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }
  }


  public int getSize() {
    return this.memoryQueue.size();
  }
}
