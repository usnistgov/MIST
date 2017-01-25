// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.

package gov.nist.isg.mist.lib.executor;

import java.io.InvalidClassException;

import javax.swing.JProgressBar;

import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.lib.common.CorrelationTriple;
import gov.nist.isg.mist.lib.exceptions.EmptyGridException;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.java.JavaImageTile;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.lib.tilegrid.TileGridUtils;
import gov.nist.isg.mist.lib32.imagetile.java.JavaImageTile32;


/**
   * NoOverlapStitchingExecutor executes the stitching with no overlap between image tiles
   *
   * @author Michael Majurski
   */
  public class NoOverlapStitchingExecutor<T> implements StitchingExecutorInterface<T> {

  private boolean init;


  @Override
  public void cancelExecution() {
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


  /**
   * Launches the sequential Java stitching.
   *
   * @param grid        the image tile grid
   * @param params      the stitching application parameters
   * @param progressBar the GUI progress bar
   * @param timeSlice   the timeslice to stitch
   */
  @Override
  public void launchStitching(TileGrid<ImageTile<T>> grid, StitchingAppParams params, JProgressBar progressBar, int timeSlice) throws Throwable {

    // treat as if assembling from metadata to prevent the optimization stage for doing anything
    params.getInputParams().setAssembleFromMetadata(true);



    for (int row = 0; row < grid.getExtentHeight(); row++) {
      for (int col = 0; col < grid.getExtentWidth(); col++) {

        ImageTile<T> tile = grid.getSubGridTile(row, col);
        tile.readTile();

        int absX = col * tile.getWidth();
        int absY = row * tile.getHeight();

        // set the relative positions
        if(col > 0)
          tile.setWestTranslation(new CorrelationTriple(0, tile.getWidth(), 0));
        if(row > 0)
          tile.setNorthTranslation(new CorrelationTriple(0, 0, tile.getHeight()));

        // set the absolute positions
        tile.setAbsXPos(absX);
        tile.setAbsYPos(absY);
      }
    }

    // create the no-optimization relative translations
    TileGridUtils.backupTranslations(grid);

  }

  /**
   * Initialize the Java stitching executor tile grid.
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
        if (params.getAdvancedParams().isUseDoublePrecision()) {
          grid = new TileGrid<ImageTile<T>>(params, timeSlice, JavaImageTile.class);
        } else {
          grid = new TileGrid<ImageTile<T>>(params, timeSlice, JavaImageTile32.class);
        }
      } catch (InvalidClassException e) {
        e.printStackTrace();
      }
    } else {
      try {
        if (params.getAdvancedParams().isUseDoublePrecision()) {
          grid = new TileGrid<ImageTile<T>>(params, JavaImageTile.class);
        } else {
          grid = new TileGrid<ImageTile<T>>(params, JavaImageTile32.class);
        }
      } catch (InvalidClassException e) {
        e.printStackTrace();
      }
    }

    ImageTile<T> tile = grid.getTileThatExists();
    if (tile == null)
      throw new EmptyGridException("Image Tile Grid contains no valid tiles. Check " +
          "Stitching Parameters");
    tile.readTile();

    if (params.getAdvancedParams().isUseDoublePrecision()) {
      JavaImageTile.initJavaPlan(tile);
    } else {
      JavaImageTile32.initJavaPlan(tile);
    }

    this.init = true;

    return grid;
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
      ImageTile<T> tile = grid.getTileThatExists();
      tile.readTile();

      // Account for image pixel data
      // must hold whole image grid in memory
      requiredMemoryBytes += (long) tile.getHeight() * (long) tile.getWidth() * (long) grid.getSubGridSize() * 2L; // 16 bit pixel data

      // pad with 10MB
      requiredMemoryBytes += 10L * 1024L * 1024L;

      return requiredMemoryBytes < Runtime.getRuntime().maxMemory();
    }

  }
