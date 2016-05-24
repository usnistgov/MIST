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

package gov.nist.isg.mist.optimization.model.overlap;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;


/**
 * Sequential MLE overlap executor.
 *
 * @author Michael Majurski
 */
public class MleOverlapSequentialExecutor<T> extends OverlapExecutorInterface<T> {


  private boolean isCancelled = false;
  private double overlap = Double.NaN;

  /**
   * Sequential MLE overlap executor.
   *
   * @param grid              the TileGrid from which to compute the overlap.
   * @param direction         the direction in which to compute the overlap.
   * @param displacementValue which displacement component of the specified direction to use in
   *                          computing the overlap.
   */
  public MleOverlapSequentialExecutor(TileGrid<ImageTile<T>> grid,
                                      TileGrid.Direction direction,
                                      TileGrid.DisplacementValue displacementValue) {
    super(grid, direction, displacementValue);
  }


  /**
   * Executes the MLE overlap computation.
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

    List<MLEPoint> hcResults = new ArrayList<MLEPoint>();

    // set the cache to null to will simply not use the cache
    MleLikelihoodCache mleCache = null;
    MLEPoint mleModel = new MLEPoint(0, 0, 0, Double.NEGATIVE_INFINITY);
    int numStableIterations = 0;

//    while (numStableIterations < MleUtils.NUMBER_STABLE_MLE_ITERATIONS) {
    while (numStableIterations < 100) {
      if (this.isCancelled)
        break;

      // create the current starting point
      MLEPoint point = MLEPoint.getRandomPoint();

      // Perform hill climbing at percent of range resolution
      point = MleUtils.hillClimbSearch(point, mleCache, trans);

      // add hill climbing resulting point to the output list
      hcResults.add(point);

      // if this point is better, keep it
      if (point.likelihood > mleModel.likelihood) {
        mleModel = point;
        numStableIterations = 0;
      } else {
        numStableIterations++;
      }
    }


    int numberConverged = 0; // to count the number of workers that converged
    // find the number of starting points which converged to the best answer
    Iterator<MLEPoint> itr = hcResults.iterator();
    while (itr.hasNext()) {
      MLEPoint point = itr.next();
      // if the current point is the same as the best
      if (mleModel.mu == point.mu && mleModel.sigma == point.sigma)
        numberConverged++;
    }

    // set the overlap
    this.overlap = 100 - mleModel.mu;

    Log.msg(Log.LogType.HELPFUL, "MLE " + getDirection() + " model parameters: mu=" + mleModel.mu
        + "% sigma=" + mleModel.sigma + "%");
    Log.msg(Log.LogType.HELPFUL, "MLE " + getDirection() + " had " + numberConverged + "/" +
        hcResults.size() + " hill climbs converge");
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

  /**
   * Cancels the MLE estimation
   */
  @Override
  public void cancel() {
    Log.msg(Log.LogType.MANDATORY, "Canceling Stage Model Build");
    this.isCancelled = true;
  }

}
