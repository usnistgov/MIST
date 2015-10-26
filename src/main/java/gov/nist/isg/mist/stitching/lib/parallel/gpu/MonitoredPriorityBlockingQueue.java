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
// Date: Apr 11, 2014 12:12:37 PM EST
//
// Time-stamp: <Apr 11, 2014 12:12:37 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.parallel.gpu;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * Class that adds a monitor on the size of the priority blocking queue. The class monitors the
 * maximum size that the queue achieves.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class MonitoredPriorityBlockingQueue<E> extends PriorityBlockingQueue<E> {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  private int monitorSize;

  /**
   * Creates a monitor implementation of a blocking queue. This queue monitors the maximum size of
   * the queue
   *
   * @param size the size of the queue
   */
  public MonitoredPriorityBlockingQueue(int size) {
    super(size);
    this.monitorSize = 0;
  }

  @Override
  public E take() throws InterruptedException {

    if (this.monitorSize < super.size()) {
      this.monitorSize = super.size();
    }

    E ret = super.take();

    return ret;
  }

  /**
   * Gets the monitored size of the queue
   *
   * @return the maximum size the queue achieved.
   */
  public int getMonitorSize() {
    return this.monitorSize;
  }

}
