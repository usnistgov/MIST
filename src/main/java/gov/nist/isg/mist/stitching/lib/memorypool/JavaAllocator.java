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

package gov.nist.isg.mist.stitching.lib.memorypool;

/**
 * Represents an allocator for allocating Java memory in the form float[][]
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class JavaAllocator implements Allocator<float[][]> {

  @Override
  public float[][] allocate(int... n) {
    int n1 = 0;
    int n2 = 0;

    if (n.length == 2) {
      n1 = n[0];
      n2 = n[1];
    } else {
      throw new IllegalArgumentException("Java Allocator requires 2-dimensions");
    }

    return new float[n1][n2];
  }

  @Override
  public float[][] deallocate(float[][] memory) {
    memory = null;
    return null;
  }

}
