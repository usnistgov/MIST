// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.


package gov.nist.isg.mist.optimization.model.overlap;

/**
 * Collection of static methods related to MLE overlap estimation.
 *
 * @author Michael Majurski
 */
public class MleUtils {

  private static final double SQRT2PI = Math.sqrt(2 * Math.PI);
  public static final int NUMBER_STABLE_MLE_ITERATIONS = 20;

  /**
   * Defines 3D hill climbing direction in the MLE model parameters (PIuni,mu,sigma).
   *
   * @author Michael Majurski
   * @version 1.0
   */
  enum HillClimbDirection {
    PosP(1, 0, 0),
    NegP(-1, 0, 0),
    PosM(0, 1, 0),
    NegM(0, -1, 0),
    PosS(0, 0, 1),
    NegS(0, 0, -1),
    NoMove(0, 0, 0);

    private int pDir;
    private int mDir;
    private int sDir;

    HillClimbDirection(int p, int m, int s) {
      this.pDir = p;
      this.mDir = m;
      this.sDir = s;
    }

    public int getPDir() {
      return this.pDir;
    }

    public int getMDir() {
      return this.mDir;
    }

    public int getSDir() {
      return this.sDir;
    }
  }

  /**
   * Computes the MLE model using a percentile resolution exhaustive search of the parameter space.
   *
   * @param T vector of translations to fit the model to. Must be within [0,100]
   * @return the MLE model which has the highest likelihood.
   */
  public static MLEPoint exhaustiveSearch(double T[]) {
    MLEPoint bestPoint = new MLEPoint(0, 0, 0, Double.NEGATIVE_INFINITY);

    double range = 100;

    // loop over the valid PIuniform values
    for (int p = 0; p < range; p++) {
      // loop over the valid mu values
      for (int m = 0; m < range; m++) {
        // loop over the valid sigma values
        for (int s = 0; s < range; s++) {
          // compute the likelihood given these model parameters
          double l = computeMleLikelihood(T, p, m, s);
          // if this model is better than the current best, save it
          if (l > bestPoint.getLikelihood()) {
            bestPoint.setMu(m);
            bestPoint.setSigma(s);
            bestPoint.setPIuni(p);
            bestPoint.setLikelihood(l);
          }
        }
      }
    }

    return bestPoint;
  }


  /**
   * Computes the MLE model using a percentile resolution hill climbing in the parameter space.
   *
   * @param point    the hill climbing starting point.
   * @param mleCache cache object for storing computed likelihood values.
   * @param T        vector of translations to fit the model to. Must be within [0,100].
   * @return the MLE model with the highest likelihood found by the hill climbing.
   */
  public static MLEPoint hillClimbSearch(MLEPoint point, MleLikelihoodCache mleCache, double[] T) {

    // init the MLE points
    MLEPoint temp = new MLEPoint(point.getPIuni(), point.getMu(), point.getSigma(), Double
        .NEGATIVE_INFINITY);

    boolean done = false;
    while (!done) {
      // Check each direction and move based on highest correlation
      for (HillClimbDirection dir : HillClimbDirection.values()) {
        // Skip NoMove direction
        if (dir == HillClimbDirection.NoMove)
          continue;

        // get the new MLE Point given the 3D hill climb direction
        int p = point.getPIuni() + dir.getPDir();
        int m = point.getMu() + dir.getMDir();
        int s = point.getSigma() + dir.getSDir();

        // Check if this point is within the search bounds
        if (p > 0 && p < 100 && m > 0 && m < 100 && s > 0 && s < 100) {
          // check cache to see if this likelihood has previously been computed
          double l = Double.NaN;
          if (mleCache != null)
            l = mleCache.getLikelihood(p, m, s);

          // if this value has not been computed
          if (Double.isNaN(l)) {
            // compute the likelihood
            l = computeMleLikelihood(T, p, m, s);
            // add it to the shared cache
            if (mleCache != null)
              mleCache.setLikelihood(p, m, s, l);
          }

          // if the new likelihood is better than the best local one so far record it
          if (l > temp.getLikelihood()) {
            temp.setPIuni(p);
            temp.setMu(m);
            temp.setSigma(s);
            temp.setLikelihood(l);
          }
        }
      }

      // if the best local neighborhood point is better that the current global best
      if (Double.isNaN(point.getLikelihood()) || temp.getLikelihood() > point.getLikelihood()) {
        // record current best
        point.setPIuni(temp.getPIuni());
        point.setMu(temp.getMu());
        point.setSigma(temp.getSigma());
        point.setLikelihood(temp.getLikelihood());
      } else {
        done = true;
      }
    }

    return point;
  }


  /**
   * Method to compute the the MLE model likelihood given the model parameters and a set of
   * translations. The model parameters are specified as percentage value of the translations
   * range.
   *
   * @param T     vector of translations to fit the model to. Must be within [0,100].
   * @param PIuni the model parameter PIuniform.
   * @param mu    the model parameter mu as a percentage of range.
   * @param sigma the model parameter sigma as a percentage of range.
   * @return the likelihood of the model, given the translations.
   */
  public static double computeMleLikelihood(double[] T, double PIuni, double mu, double sigma) {

    double range = 100;
    double likelihood = Double.NEGATIVE_INFINITY;
    if (PIuni >= 0 && PIuni < 100) {
      PIuni = PIuni / 100;
      // loop over the elements the x array
      likelihood = 0; // init sum value
      for (int i = 0; i < T.length; i++) {
        double temp = (T[i] - mu) / sigma;
        temp = Math.exp(-0.5 * temp * temp);
        temp = temp / (SQRT2PI * sigma);
        temp = (PIuni / range) + (1 - PIuni) * temp;
        temp = Math.abs(temp);
        temp = Math.log(temp);
        likelihood = likelihood + temp;
      }
    }

    return likelihood;
  }

}
