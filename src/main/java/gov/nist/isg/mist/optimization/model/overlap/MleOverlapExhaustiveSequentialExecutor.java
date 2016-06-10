// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.


package gov.nist.isg.mist.optimization.model.overlap;

import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;

/**
 * Sequential exhaustive MLE overlap computation executor.
 *
 * @author Michael Majurski
 */
public class MleOverlapExhaustiveSequentialExecutor<T> extends OverlapExecutorInterface<T> {

  private boolean isCancelled = false;
  private double overlap = Double.NaN;


  /**
   * Sequential exhaustive MLE overlap computation executor.
   *
   * @param grid              the TileGrid from which to compute the overlap.
   * @param direction         the direction in which to compute the overlap.
   * @param displacementValue which displacement component of the given direction to use in
   *                          computing the overlap.
   */
  public MleOverlapExhaustiveSequentialExecutor(TileGrid<ImageTile<T>> grid,
                                                TileGrid.Direction direction,
                                                TileGrid.DisplacementValue displacementValue) {
    super(grid, direction, displacementValue);
  }


  /**
   * Cancels the MLE estimation
   */
  @Override
  public void cancel() {
    Log.msg(Log.LogType.MANDATORY, "Canceling Stage Model Build");
    this.isCancelled = true;
  }


  /**
   * Executes the MLE overlap computation
   */
  @Override
  public void execute() {
    double[] trans = getTranslations();
    if (trans.length < 1) {
      Log.msg(Log.LogType.MANDATORY, "Unable to compute overlap, translation list is empty.");
      return;
    }

    // convert translations into a percentage of the valid range (so all translations are [0,100])
    double range = getRange();
    for (int i = 0; i < trans.length; i++)
      trans[i] = 100 * trans[i] / range;

    Log.msg(Log.LogType.INFO, "Computing overlap for " + getDirection().name()
        + " direction using Maximum Likelihood Estimation.");

    MLEPoint mleModel = MleUtils.exhaustiveSearch(trans);

    // set the overlap
    this.overlap = 100 - mleModel.getMu();

    Log.msg(Log.LogType.HELPFUL, "MLE " + getDirection() + " model parameters: mu=" + mleModel.getMu()
        + "% sigma=" + mleModel.getSigma() + "%");
  }


  /**
   * Method to get the resulting overlap.
   *
   * @return the overlap value computed. Rounded to 2 decimal places.
   */
  @Override
  public double getOverlap() {
    return ((double) Math.round(100 * this.overlap)) / 100;
  }


}
