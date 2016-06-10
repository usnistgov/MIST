// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.


package gov.nist.isg.mist.optimization.model.overlap;

import java.util.Random;

/**
 * Class to hold MLE model.
 *
 * @author Michael Majurski
 */
public class MLEPoint implements Cloneable {

  private int PIuni;
  private int mu;
  private int sigma;
  private double likelihood;

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

  public int getPIuni() { return this.PIuni; }
  public void setPIuni(int val) { this.PIuni = val; }

  public int getMu() { return this.mu; }
  public void setMu(int val) { this.mu = val; }

  public int getSigma() { return this.sigma; }
  public void setSigma(int val) { this.sigma = val; }

  public double getLikelihood() { return this.likelihood; }
  public void setLikelihood(double val) { this.likelihood = val; }


  /**
   * Create a new deep copy of the given MLE model
   */
  @Override
  public MLEPoint clone() {
    return new MLEPoint(PIuni, mu, sigma, likelihood);
  }


}
