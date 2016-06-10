// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.


package gov.nist.isg.mist.optimization.model.overlap;

/**
 * Class to hold thread safe cache of Maximum Likelihood values to prevent recomputing between
 * threads.
 *
 * @author Michael Majurski
 */
public class MleLikelihoodCache {

  double likelihood[][][];

  /**
   * Allocates the MLE likelihood cache and initializes each element to Double.NaN
   */
  public MleLikelihoodCache() {
    likelihood = new double[100][100][100];

    // init all values to Double.Nan
    for (int p = 0; p < 100; p++)
      for (int m = 0; m < 100; m++)
        for (int s = 0; s < 100; s++)
          likelihood[p][m][s] = Double.NaN;


  }


  /**
   * Thread safe method to get a cached MLE likelihood value.
   *
   * Per the Java documentation, synchronized is per instance. Therefore, since this cache
   * instance is shared among the workers, there is no potential conflicts.
   *
   * @param p the PIuniform query percent value. Valid range: [0,100)
   * @param m the mu query percent value. Valid range: [0,100)
   * @param s the sigma query percent value. Valid range: [0,100)
   * @return the cached likelihood value, or Double.NaN if not yet computed.
   */
  public synchronized double getLikelihood(int p, int m, int s) {
    double val = Double.NEGATIVE_INFINITY;
    if (p >= 0 && p < 100 && m >= 0 && m < 100 && s >= 0 && s < 100)
      val = likelihood[p][m][s];

    return val;
  }

  /**
   * Thread safe method to set a cached MLE likelihood value.
   *
   * Per the Java documentation, synchronized is per instance. Therefore, since this cache
   * instance is shared among the workers, there is no potential conflicts.
   *
   * @param p the PIuniform query percent value. Valid range: [0,100)
   * @param m the mu query percent value. Valid range: [0,100)
   * @param s the sigma query percent value. Valid range: [0,100)
   * @param l the likelihood value.
   */
  public synchronized void setLikelihood(int p, int m, int s, double l) {
    if (p >= 0 && p < 100 && m >= 0 && m < 100 && s >= 0 && s < 100)
      likelihood[p][m][s] = l;

  }

}
