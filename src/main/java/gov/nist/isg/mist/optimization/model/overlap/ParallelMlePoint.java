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

/**
 * Class to hold thread safe best point to enable the stopping condition of the MLE search.
 *
 * @author Michael Majurski
 */
public class ParallelMlePoint {

  private MLEPoint point;
  private int numStableIterations;

  public ParallelMlePoint() {
    numStableIterations = 0;
    point = new MLEPoint(0, 0, 0, Double.NEGATIVE_INFINITY);
  }


  /**
   * Set this local MLEPoint if the new likelihood is better than the current on.
   *
   * @param p the new point to check against the one held by this class.
   */
  public synchronized void setIfBetter(MLEPoint p) {
    if (p.likelihood > point.likelihood) {
      numStableIterations = 0;
      point.likelihood = p.likelihood;
      point.mu = p.mu;
      point.sigma = p.sigma;
      point.PIuni = p.PIuni;
    } else {
      numStableIterations++;
    }
  }

  /**
   * get a deep copy of the MLEPoint held within this class.
   *
   * @return deep copy of the local MLEPoint1
   */
  public synchronized MLEPoint getPoint() {
    // return a new deep copy of this point
    return point.clone();
  }


  /**
   * Get the number of times setIfBetter has been called since the last time the new point was
   * accepted.
   *
   * @return the number of times setIfBetter has been called without accepting the new point. Aka
   * the new likelihood was worse than the likelihood of the point held in the class.
   */
  public synchronized int getNumStableIterations() {
    return this.numStableIterations;
  }
}
