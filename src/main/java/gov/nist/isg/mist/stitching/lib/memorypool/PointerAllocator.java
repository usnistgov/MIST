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
// Date: Apr 11, 2014 11:04:44 AM EST
//
// Time-stamp: <Apr 11, 2014 11:04:44 AM tjb3>
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.memorypool;

import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FFTW3Library;

import org.bridj.Pointer;

/**
 * Allocator type that allocates Pointer memory for use with FFTW
 *
 * The memory that is allocated is double complex, so size = size*sizeof(double)*2
 *
 * element i*2 = real element i*2+1 = imaginary
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class PointerAllocator implements Allocator<Pointer<Double>> {

  @Override
  public Pointer<Double> allocate(int... n) {
    long size = 1;
    for (int val : n) {
      size *= val;
    }

    // Allocate complex numbers
    return FFTW3Library.fftw_alloc_real(size * 2);
  }

  @Override
  public Pointer<Double> deallocate(Pointer<Double> memory) {
    FFTW3Library.fftw_free(memory);
    return memory;
  }
}
