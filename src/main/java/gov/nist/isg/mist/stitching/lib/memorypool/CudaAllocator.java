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
// Date: Apr 11, 2014 11:04:56 AM EST
//
// Time-stamp: <Apr 11, 2014 11:04:56 AM tjb3>
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.memorypool;

import jcuda.driver.CUdeviceptr;
import jcuda.driver.JCudaDriver;

/**
 * Allocator type that allocates GPU memory (JCUDA)
 * 
 * @author Tim Blattner
 * @version 1.0
 */
public class CudaAllocator implements Allocator<CUdeviceptr> {

  @Override
  public CUdeviceptr allocate(int... n) {
    int size = 1;
    for (int val : n) {
      size *= val;
    }

    CUdeviceptr ptr = new CUdeviceptr();
    JCudaDriver.cuMemAlloc(ptr, size);

    return ptr;
  }

  @Override
  public CUdeviceptr deallocate(CUdeviceptr memory) {
    JCudaDriver.cuMemFree(memory);
    return memory;
  }
}
