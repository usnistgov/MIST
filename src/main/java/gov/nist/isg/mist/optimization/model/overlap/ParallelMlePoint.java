// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.


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
    if (p.getLikelihood() > point.getLikelihood()) {
      numStableIterations = 0;
      point.setLikelihood(p.getLikelihood());
      point.setMu(p.getMu());
      point.setSigma(p.getSigma());
      point.setPIuni(p.getPIuni());
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
