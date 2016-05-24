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

import java.util.Random;

/**
 * Class to hold MLE model.
 *
 * @author Michael Majurski
 */
public class MLEPoint implements Cloneable {

  public int PIuni;
  public int mu;
  public int sigma;
  public double likelihood;

  public static Random randMaker = new Random(System.currentTimeMillis());

  /**
   * Create a new MLE model from the model parameters.
   *
   * @param PIuni (PIuniform) the probability of translation belonging in the uniform distribution.
   * @param mu    the mean of the models normal distribution
   * @param sigma the standard deviation of the models normal distributions.
   * @param l     the likelihood of the current model given some set of translations.
   */
  public MLEPoint(int PIuni, int mu, int sigma, double l) {
    this.PIuni = PIuni;
    this.mu = mu;
    this.sigma = sigma;
    this.likelihood = l;
  }

  /**
   * Get a new random MLEPoint.
   *
   * @return MLEPoint with random PIuni, mu, sigma, and Double.NaN likelihood.
   */
  public static synchronized MLEPoint getRandomPoint() {
    int p = (int) Math.round(100 * randMaker.nextDouble());
    int m = (int) Math.round(100 * randMaker.nextDouble());
    int s = (int) Math.round(100 * randMaker.nextDouble());
    return new MLEPoint(p, m, s, Double.NaN);
  }

  /**
   * Create a new deep copy of the given MLE model
   */
  @Override
  public MLEPoint clone() {
    return new MLEPoint(PIuni, mu, sigma, likelihood);
  }


}
