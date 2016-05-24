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
// Date: Apr 11, 2014 12:26:43 PM EST
//
// Time-stamp: <Apr 11, 2014 12:26:43 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib32.parallel.gpu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import javax.swing.*;

import gov.nist.isg.mist.stitching.gui.StitchingGuiUtils;
import gov.nist.isg.mist.stitching.lib.common.CorrelationTriple;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.log.Debug;
import gov.nist.isg.mist.stitching.lib.log.Debug.DebugType;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.parallel.common.StitchingTask;
import gov.nist.isg.mist.stitching.lib.parallel.common.StitchingTask.TaskType;

/**
 * Class that represents a CCF (cross correlation function) worker, which are CPU threads that
 * compute the cross correlation of two neighbors based on the index computed from phase
 * correlation.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TileCpuCcfWorker32<T> implements Runnable {

  private PriorityBlockingQueue<StitchingTask<T>> ccfQueue;

  private static int count = 0;
  private int numNeighbors;
  private JProgressBar progressBar;

  private volatile boolean isCancelled;


  /**
   * Creates a CCF worker that executes cross correlations on the CPU
   *
   * @param ccfQueue     the work queue for the CCF workers
   * @param numNeighbors the total number of neighbors in the computation
   * @param progressBar  the progress bar
   */
  public TileCpuCcfWorker32(PriorityBlockingQueue<StitchingTask<T>> ccfQueue,
                            int numNeighbors, JProgressBar progressBar) {
    TileCpuCcfWorker32.count = 0;
    this.ccfQueue = ccfQueue;
    this.numNeighbors = numNeighbors;
    this.progressBar = progressBar;
    this.isCancelled = false;
  }

  @Override
  public void run() {
    try {

      List<CorrelationTriple> multi_ccfs = new ArrayList<CorrelationTriple>();

      while (!this.isCancelled && (count < this.numNeighbors)) {
        multi_ccfs.clear();
        StitchingTask<T> task = this.ccfQueue.take();

        Debug.msg(DebugType.VERBOSE,
            "WP Task acquired: " + task.getTask() + "  size: " + this.ccfQueue.size() + " count: "
                + count);

        if (task.getTask() == TaskType.CCF_DONE) {
          break;
        } else if (task.getTask() == TaskType.CCF) {
          int[] indices = task.getIndices();
          ImageTile<T> tile = task.getTile();
          ImageTile<T> neighbor = task.getNeighbor();

          boolean north = false;
          boolean west = false;
          if (tile.isSameRowAs(neighbor))
            west = true;
          else
            north = true;

          CorrelationTriple corr = new CorrelationTriple(-1.0, 0, 0);
          // If both image tiles exist on disk
          if (tile.fileExists() && neighbor.fileExists()) {

            for (int index : indices) {
              int x = index % tile.getWidth();
              int y = index / tile.getWidth();

              if (west)
                multi_ccfs.add(Stitching.peakCrossCorrelationLR(neighbor, tile, x, y));
              else if (north)
                multi_ccfs.add(Stitching.peakCrossCorrelationUD(neighbor, tile, x, y));
            }

            corr = Collections.max(multi_ccfs);
          }

          if (north) {
            tile.setNorthTranslation(corr);
            Log.msg(LogType.HELPFUL, "N: " + tile.getFileName() + " -> " + neighbor.getFileName()
                + " x: " + tile.getNorthTranslation().getMatlabFormatStrX() + " y: "
                + tile.getNorthTranslation().getMatlabFormatStrY() + " ccf: "
                + tile.getNorthTranslation().getMatlatFormatStrCorr());

            incProgressBar();
            incCount();
            decrementAndReleasePixels(tile);
            decrementAndReleasePixels(neighbor);
          } else if (west) {
            tile.setWestTranslation(corr);
            Log.msg(LogType.HELPFUL, "W: " + tile.getFileName() + " -> " + neighbor.getFileName()
                + " x: " + tile.getWestTranslation().getMatlabFormatStrX() + " y: "
                + tile.getWestTranslation().getMatlabFormatStrY() + " ccf: "
                + tile.getWestTranslation().getMatlatFormatStrCorr());


            incProgressBar();
            incCount();
            decrementAndReleasePixels(tile);
            decrementAndReleasePixels(neighbor);
          }

        } else if (task.getTask() == TaskType.CANCELLED) {
          this.isCancelled = true;
        }
      }

      Debug.msg(DebugType.HELPFUL, "CCF Done");

      // Signal other workers that may be waiting to finish
      this.ccfQueue.put(new StitchingTask<T>(null, null, TaskType.SENTINEL));

    } catch (InterruptedException e) {
      Log.msg(LogType.MANDATORY, "Interrupted CCF worker");
    }

  }

  private void incProgressBar() {
    StitchingGuiUtils.incrementProgressBar(this.progressBar);

  }

  /**
   * Cancels the task
   */
  public void cancel() {
    this.ccfQueue.put(new StitchingTask<T>(null, null, TaskType.CANCELLED));
  }

  private static synchronized void incCount() {
    count++;
  }

  private static synchronized void decrementAndReleasePixels(ImageTile<?> tile) {
    tile.decrementPixelDataReleaseCount();
    if (tile.getPixelDataReleaseCount() == 0)
      tile.releasePixels();
  }
}
