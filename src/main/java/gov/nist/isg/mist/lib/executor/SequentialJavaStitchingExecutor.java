// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.


package gov.nist.isg.mist.lib.executor;

import java.io.InvalidClassException;

import javax.swing.JProgressBar;

import gov.nist.isg.mist.gui.StitchingGuiUtils;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.lib.exceptions.EmptyGridException;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.Stitching;
import gov.nist.isg.mist.lib.imagetile.java.JavaImageTile;
import gov.nist.isg.mist.lib.imagetile.memory.JavaTileWorkerMemory;
import gov.nist.isg.mist.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverser;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverserFactory;

/**
 * Sequential Java Stitching executor.
 *
 * @author Michael Majurski
 */
public class SequentialJavaStitchingExecutor<T> implements StitchingExecutorInterface<T> {

  private boolean isCanceled = false;

  /**
   * Initialize the sequential Java stitching executor tile grid.
   *
   * @param params    the stitching params.
   * @param timeSlice the timeslice to stitch.
   * @return the TileGrid to be stitched when launchStitching is called.
   */
  @Override
  public TileGrid<ImageTile<T>> initGrid(StitchingAppParams params, int timeSlice) throws EmptyGridException {

    TileGrid<ImageTile<T>> grid = null;

    if (params.getInputParams().isTimeSlicesEnabled()) {
      try {
        grid =
            new TileGrid<ImageTile<T>>(params, timeSlice, JavaImageTile.class);
      } catch (InvalidClassException e) {
        e.printStackTrace();
      }
    } else {
      try {
        grid = new TileGrid<ImageTile<T>>(params, JavaImageTile.class);
      } catch (InvalidClassException e) {
        e.printStackTrace();
      }
    }

    ImageTile<T> tile = grid.getTileThatExists();
    if (tile == null)
      throw new EmptyGridException("Image Tile Grid contains no valid tiles. Check " +
          "Stitching Parameters");
    tile.readTile();

    JavaImageTile.initJavaPlan(tile);

    return grid;
  }


  @Override
  public void cancelExecution() {
    Log.msg(Log.LogType.MANDATORY, "Canceling Stitching Java Sequential Executor");
    this.isCanceled = true;
  }

  /**
   * Launches the sequential Java stitching.
   *
   * @param grid        the image tile grid
   * @param params      the stitching application parameters
   * @param progressBar the GUI progress bar
   * @param timeSlice   the timeslice to stitch
   */
  @Override
  public void launchStitching(TileGrid<ImageTile<T>> grid, StitchingAppParams params,
                              JProgressBar progressBar, int timeSlice) throws Throwable {

    Log.msg(Log.LogType.MANDATORY, "Running Sequential Java Stitching");
    TileGridTraverser<ImageTile<T>> traverser = TileGridTraverserFactory.makeTraverser(
        TileGridTraverser.Traversals.DIAGONAL_CHAINED, grid);

    TileWorkerMemory memory = null;
    for (ImageTile<T> t : traverser) {
      if (this.isCanceled) return;

      t.setThreadID(0);


      t.readTile();

      if (memory == null) {
        memory = new JavaTileWorkerMemory(t);
      }
      int row = t.getRow();
      int col = t.getCol();

      t.computeFft();

      if (col > grid.getStartCol()) {

        ImageTile<T> west = grid.getTile(row, col - 1);
        t.setWestTranslation(Stitching.phaseCorrelationImageAlignment(west, t, memory));

        Log.msgNoTime(Log.LogType.HELPFUL, " pciam_W(\"" + t.getFileName() + "\",\"" + west
            .getFileName() + "\"): " + t.getWestTranslation());


        west.releaseFftMemory();
        west.releasePixels();

        if (progressBar != null)
          StitchingGuiUtils.incrementProgressBar(progressBar);

      }

      if (row > grid.getStartRow()) {
        ImageTile<T> north = grid.getTile(row - 1, col);

        t.setNorthTranslation(Stitching.phaseCorrelationImageAlignment(north, t, memory));

        Log.msgNoTime(
            Log.LogType.HELPFUL,
            " pciam_N(\"" + north.getFileName() + "\",\"" + t.getFileName() + "\"): "
                + t.getNorthTranslation());


        north.releaseFftMemory();
        north.releasePixels();

        if (progressBar != null)
          StitchingGuiUtils.incrementProgressBar(progressBar);
      }

      t.releaseFftMemory();
      t.releasePixels();

    }

  }

  /**
   * Checks for the required libraries.
   *
   * @param params     the stitching application params
   * @param displayGui whether to display gui or not
   * @return flag denoting whether the libraries required for this executor were found.
   */
  @Override
  public boolean checkForLibs(StitchingAppParams params, boolean displayGui) {
    return true;
  }

  @Override
  public void cleanup() {
  }

  /**
   * Determines if the system has the required memory to perform this stitching experiment as
   * configured.
   *
   * @param grid       the image tile grid
   * @param numWorkers the number of worker threads
   * @param <T>        the Type of ImageTile in the TileGrid
   * @return flag denoting whether the system has enough memory to stitch this experiment as is.
   */
  @Override
  public <T> boolean checkMemory(TileGrid<ImageTile<T>> grid, int numWorkers) {
    long requiredMemoryBytes = 0;
    long memoryPoolCount = 2;
    ImageTile<T> tile = grid.getTileThatExists();
    tile.readTile();

    // Account for image pixel data
    requiredMemoryBytes += (long) tile.getHeight() * (long) tile.getWidth() * memoryPoolCount * 2L; // 16 bit pixel data

    // Account for image pixel data up conversion
    long byteDepth = tile.getBitDepth() / 8;
    if (byteDepth != 2) {
      // if up-converting at worst case there will be numWorkers copies of the old precision pixel data
      requiredMemoryBytes += (long) numWorkers * (long) tile.getHeight() * (long) tile.getWidth() * byteDepth;
    }

    // Account for Java FFT data
    int[] n = {JavaImageTile.fftPlan.getFrequencySampling2().getCount(),
        JavaImageTile.fftPlan.getFrequencySampling1().getCount() * 2};
    long size = 1;
    for (int val : n)
      size *= val;
    requiredMemoryBytes += memoryPoolCount * size * 4L; // float[n1][n2]

    requiredMemoryBytes += size * 4L; // new float[fftHeight][fftWidth];

    // pad with 100MB
    requiredMemoryBytes += 100L * 1024L * 1024L;

    return requiredMemoryBytes < Runtime.getRuntime().maxMemory();
  }
}
