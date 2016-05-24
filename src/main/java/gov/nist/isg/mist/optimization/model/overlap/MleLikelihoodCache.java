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
   * Thread safe method to set a cached MLE likelihood valie.
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
