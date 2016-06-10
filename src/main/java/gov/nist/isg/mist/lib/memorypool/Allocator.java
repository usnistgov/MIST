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


// ================================================================
//
// Author: tjb3
// Date: Apr 11, 2014 11:05:10 AM EST
//
// Time-stamp: <Apr 11, 2014 11:05:10 AM tjb3>
//
// ================================================================

package gov.nist.isg.mist.lib.memorypool;

import jcuda.CudaException;

/**
 * Interface to create different types of memory allocators.
 *
 * @param <T> the underlying memory type.
 * @author Tim Blattner
 * @version 1.0
 */
public interface Allocator<T> {
  /**
   * Allocates memory
   *
   * @param n an array of dimensions
   * @return the memory reference
   */
  public T allocate(int... n) throws OutOfMemoryError, CudaException;

  /**
   * Deallocates memory
   *
   * @param memory the memory reference
   * @return the reference after deallocation (normally null)
   */
  public T deallocate(T memory);
}
