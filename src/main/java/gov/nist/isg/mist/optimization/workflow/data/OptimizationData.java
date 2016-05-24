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
// Date: Aug 1, 2013 3:59:00 PM EST
//
// Time-stamp: <Aug 1, 2013 3:59:00 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.optimization.workflow.data;

import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;

/**
 * Created by tjb3 on 4/1/2015.
 */
public class OptimizationData<T> {


  public enum TaskType {
    READ,
    BK_CHECK_NEIGHBORS,
    BK_CHECK_MEMORY,
    OPTIMIZE_NORTH,
    OPTIMIZE_WEST,
    BK_DONE,
    CANCELLED

  }


  private ImageTile<T> tile;
  private ImageTile<T> neighbor;

  private TaskType type;

  public OptimizationData(ImageTile<T> tile, TaskType type) {
    this.tile = tile;
    this.type = type;
  }


  public OptimizationData(ImageTile<T> tile, ImageTile<T> neighbor, TaskType type) {
    this.tile = tile;
    this.neighbor = neighbor;

    this.type = type;
  }

  public ImageTile<T> getTile() {
    return tile;
  }

  public ImageTile<T> getNeighbor() {
    return neighbor;
  }

  public TaskType getType() {
    return type;
  }

  public void setType(TaskType type) {
    this.type = type;
  }
}
