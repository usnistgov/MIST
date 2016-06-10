// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 3:54:22 PM EST
//
// Time-stamp: <Aug 1, 2013 3:54:22 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.parallel.common;

import gov.nist.isg.mist.lib.imagetile.ImageTile;

/**
 * A task descriptor passed between threads.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class StitchingTask<T> implements Comparable<StitchingTask<T>> {

  /**
   * Types of tasks
   */
  public static enum TaskType {
    /**
     * The task is reading
     */
    READ,

    /**
     * The task is computing FFT
     */
    FFT,

    /**
     * The task is a bookkeeping, checking neighbors
     */
    BK_CHECK_NEIGHBORS,

    /**
     * The task is computing phase correlation north
     */
    PCIAM_NORTH,

    /**
     * The task is computing phase correlation west
     */
    PCIAM_WEST,

    /**
     * The task is bookkeeping, checking to free memory
     */
    BK_CHECK_MEM,

    /**
     * The task has completed all reading
     */
    READ_DONE,

    /**
     * The task has completed all bookkeeping
     */
    BK_DONE,

    /**
     * The task is compute cross correlation factors
     */
    CCF,

    /**
     * The task is finding multiple peaks that are distance d from eachother
     */
    MULTI_MAX,

    /**
     * The task is notifying that the CCF has completed
     */
    CCF_DONE,

    /**
     * The task is notifying that it is being cancelled
     */
    CANCELLED,

    SENTINEL,

  }

  private int[] indices;

  private ImageTile<T> tile;
  private ImageTile<T> neighbor;
  private TaskType task;
  private int devID;
  private int threadID;

  /**
   * Initializes a task with a tile, its neighbor, and its type
   *
   * @param tile     the origin tile
   * @param neighbor the neighbor tile
   * @param task     the task type
   */
  public StitchingTask(ImageTile<T> tile, ImageTile<T> neighbor, TaskType task) {
    this(tile, neighbor, null, 0, 0, task);

  }

  /**
   * Initializes a task for use with CCF. Also used by other constructors
   *
   * @param tile     the origin tile
   * @param neighbor the neighbor tile
   * @param indices  the indices for CCF computation (could be 1 or more)
   * @param devID    the GPU device id
   * @param threadID the thread id
   * @param task     the task type
   */
  public StitchingTask(ImageTile<T> tile, ImageTile<T> neighbor, int[] indices, int devID,
                       int threadID, TaskType task) {
    this.tile = tile;
    this.neighbor = neighbor;
    this.task = task;
    this.indices = indices;
    this.devID = devID;
    this.threadID = threadID;
  }

  /**
   * @return the thread ID
   */
  public int getThreadID() {
    return this.threadID;
  }

  /**
   * @return the GPU device ID
   */
  public int getDevID() {
    return this.devID;
  }

  /**
   * Sets indices for this task
   */
  public void setIndices(int[] indices) {
    this.indices = indices;
  }

  /**
   * @return the indices
   */
  public int[] getIndices() {
    return this.indices;
  }

  /**
   * Sets the task type
   */
  public void setTask(TaskType task) {
    this.task = task;
  }

  /**
   * @return the tile
   */
  public ImageTile<T> getTile() {
    return this.tile;
  }

  /**
   * @return the neighbor
   */
  public ImageTile<T> getNeighbor() {
    return this.neighbor;
  }

  /**
   * @return the task
   */
  public TaskType getTask() {
    return this.task;
  }

  /**
   * Clones the task to make a new instance of this task
   */
  @Override
  public StitchingTask<T> clone() {
    return new StitchingTask<T>(this.tile, this.neighbor, this.task);
  }

  /**
   * Compares a task with another task determining the priority of tasks. Priority from lowest to
   * highest: CANCELLED, BK_CHECK_MEM, MULTI_MAX, PCIAM_NORTH/PCIAM_WEST, FFT
   *
   * @param o the other task
   */
  @Override
  public int compareTo(StitchingTask<T> o) {
    if (this.getTask() == o.getTask())
      return 0;
    else if (this.getTask() == TaskType.CANCELLED)
      return -1;
    else if (o.getTask() == TaskType.CANCELLED)
      return 1;
    else if (this.getTask() == TaskType.BK_CHECK_MEM)
      return -1;
    else if (o.getTask() == TaskType.BK_CHECK_MEM)
      return 1;
    else if (this.getTask() == TaskType.MULTI_MAX)
      return -1;
    else if (o.getTask() == TaskType.MULTI_MAX)
      return 1;
    else if (this.getTask() == TaskType.PCIAM_NORTH || this.getTask() == TaskType.PCIAM_WEST)
      return -1;
    else if (o.getTask() == TaskType.PCIAM_NORTH || o.getTask() == TaskType.PCIAM_WEST)
      return 1;
    else if (this.getTask() == TaskType.FFT)
      return -1;
    else if (o.getTask() == TaskType.FFT)
      return 1;
    else if (this.getTask() == TaskType.SENTINEL)
      return -1;
    else if (o.getTask() == TaskType.SENTINEL)
      return 1;
    else
      return 0;
  }

}
