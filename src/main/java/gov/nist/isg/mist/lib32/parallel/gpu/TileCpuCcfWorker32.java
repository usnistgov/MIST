// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 11, 2014 12:26:43 PM EST
//
// Time-stamp: <Apr 11, 2014 12:26:43 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib32.parallel.gpu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import javax.swing.JProgressBar;

import gov.nist.isg.mist.gui.StitchingGuiUtils;
import gov.nist.isg.mist.lib.common.CorrelationTriple;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.Stitching;
import gov.nist.isg.mist.lib.log.Debug;
import gov.nist.isg.mist.lib.log.Debug.DebugType;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.parallel.common.StitchingTask;
import gov.nist.isg.mist.lib.parallel.common.StitchingTask.TaskType;

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
