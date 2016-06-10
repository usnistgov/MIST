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
// Date: May 16, 2014 3:53:06 PM EST
//
// Time-stamp: <May 16, 2014 3:53:06 PM tjb3>
//
// ================================================================

package gov.nist.isg.mist.lib.memorypool;

/**
 * Represents an allocator for allocating Java memory in the form float[][]
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class ImageAllocator implements Allocator<short[]> {

  @Override
  public short[] allocate(int... n) {
    int size = 1;

    for (int s : n)
      size *= s;

    return new short[size];
  }

  @Override
  public short[] deallocate(short[] memory) {
    memory = null;
    return null;
  }

}
