// ================================================================
//
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
//
// ================================================================

// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 3:52:24 PM EST
//
// Time-stamp: <Aug 1, 2013 3:52:24 PM tjb3>
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.memorypool;

import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import jcuda.CudaException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A memory pool that can optionally grow dynamically if all memory in the pool is taken. If the
 * pool does not grow dynamically, then the thread trying to obtain memory will wait until more
 * memory is added into the pool.
 * 
 * @author Tim Blattner
 * @version 1.0
 * @param <T>
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
   * @param dynamic whether the pool is dynamic or not
   * @param allocator the allocator
   * @param sz the size of the pool
   * @throws OutOfMemoryError
   * @throws CudaException
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

}
