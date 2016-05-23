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
// Date: Aug 1, 2013 3:54:22 PM EST
//
// Time-stamp: <Aug 1, 2013 3:54:22 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.parallel.common;

import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;

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
