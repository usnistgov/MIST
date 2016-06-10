// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Oct 1, 2014 1:45:09 PM EST
//
// Time-stamp: <Oct 1, 2014 1:45:09 PM tjb3>
//
// ================================================================
package gov.nist.isg.mist.lib.executor;

import java.io.FileNotFoundException;

import javax.swing.JProgressBar;

import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.lib.exceptions.EmptyGridException;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;


/**
 * StitchingExecutorInterface interface for various stitching executors
 *
 * @author Tim Blattner
 */
public interface StitchingExecutorInterface<T> {

  /**
   * Initializes a grid of tiles
   *
   * @param params    the stitching application params
   * @param timeSlice the timeslice
   * @return the grid initialized using the stitching app params
   */
  TileGrid<ImageTile<T>> initGrid(StitchingAppParams params, int timeSlice)
      throws FileNotFoundException, EmptyGridException;

  /**
   * Cancels the execution
   */
  void cancelExecution();

  /**
   * Launches stitching
   *
   * @param grid        the image tile grid
   * @param params      the stitching application parameters
   * @param progressBar the progress bar
   * @param timeSlice   the timeslice
   */
  void launchStitching(TileGrid<ImageTile<T>> grid, StitchingAppParams params, JProgressBar progressBar, int timeSlice)
      throws Throwable;

  /**
   * Checks for required libraries.
   *
   * @param params     the stitching application params
   * @param displayGui whether to display gui or not
   * @return true if the libraries are available, otherwise false
   */
  boolean checkForLibs(StitchingAppParams params, boolean displayGui);


  /**
   * Cleans-up / releases any resources used by the executor
   */
  void cleanup();


  /**
   * Checks to see if the JVM has enough memory to launch this grid
   *
   * @param grid       the image tile grid
   * @param numWorkers the number of worker threads
   */
  <T> boolean checkMemory(TileGrid<ImageTile<T>> grid, int numWorkers)
      throws FileNotFoundException;


}
