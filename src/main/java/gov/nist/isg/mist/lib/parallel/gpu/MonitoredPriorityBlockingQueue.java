// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 11, 2014 12:12:37 PM EST
//
// Time-stamp: <Apr 11, 2014 12:12:37 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.parallel.gpu;

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
