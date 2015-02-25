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
// Date: Apr 11, 2014 11:04:37 AM EST
//
// Time-stamp: <Apr 11, 2014 11:04:37 AM tjb3>
//
//
// ================================================================

package main.gov.nist.isg.mist.stitching.lib.optimization;

import javax.swing.JProgressBar;

import main.gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import main.gov.nist.isg.mist.stitching.lib.exceptions.GlobalOptimizationException;
import main.gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import main.gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import main.gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import main.gov.nist.isg.mist.stitching.lib.log.Log;
import main.gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;

import java.io.FileNotFoundException;

/**
 * Class for computing the global optimization of a TileGrid.
 * 
 * @author Tim Blattner
 * @version 1.0
 * @param <T> the underlying data type of the TileGrid
 */
public class GlobalOptimization<T> implements Runnable {

  /**
   * Global Optimization type enum
   * 
   * @author Tim Blattner
   * @version 1.0
   */
  public enum GlobalOptimizationType {
    /**
     * Run default optimization
     */
    DEFAULT("Default"),

    /**
     * Compute Repeatability
     */
    COMPUTEREPEATABILITY("Compute Repeatability"),

    /**
     * No optimization is run
     */
    NONE("None");

    private GlobalOptimizationType(final String text) {
      this.text = text;
    }

    private final String text;

    @Override
    public String toString() {
      return this.text;
    }


  }

  private TileGrid<ImageTile<T>> grid;
  private JProgressBar progressBar;
  private StitchingAppParams params;
  private OptimizationRepeatability<T> optimizationRepeatability;
  private boolean exceptionThrown;

  /**
   * Constructs a global optimization execution for a grid of tiles
   * 
   * @param grid the grid of tiles
   * @param progressBar the progress bar (null if none)
   * @param params the stitching parameters
   */
  public GlobalOptimization(TileGrid<ImageTile<T>> grid, final JProgressBar progressBar,
      StitchingAppParams params) {
    this.grid = grid;
    this.progressBar = progressBar;
    this.params = params;
    this.optimizationRepeatability = null;
    this.exceptionThrown = false;
  }

  @Override
  public void run() {
    GlobalOptimizationType type = this.params.getAdvancedParams().getGlobalOpt();
    Stitching.USE_HILLCLIMBING = this.params.getAdvancedParams().isUseHillClimbing();

    OptimizationUtils.backupTranslations(this.grid);

    switch (type) {
      case COMPUTEREPEATABILITY:
      case DEFAULT:
        this.optimizationRepeatability =
            new OptimizationRepeatability<T>(this.grid, this.progressBar, this.params);
        try {
          this.optimizationRepeatability.computeGlobalOptimizationRepeatablity();
        } catch (GlobalOptimizationException e) {
          this.exceptionThrown = true;
        }
           catch (FileNotFoundException ex)
           {
               Log.msg(LogType.MANDATORY, "Unable to find file: " + ex.getMessage() + ". Cancelling global optimization.");
           }
        break;        
      case NONE:
        break;
      default:
        break;

    }
  }

  /**
   * Forces global optimization to cancel execution
   */
  public void cancelOptimization() {
    if (this.optimizationRepeatability != null) {
      this.optimizationRepeatability.cancelOptimization();
    }
  }
  
  /**
   * Gets whether an exception was thrown or not
   * @return true if an exception was thrown, otherwise false
   */
  public boolean isExceptionThrown()
  {
    return this.exceptionThrown;
  }


}
