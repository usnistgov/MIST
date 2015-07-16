package gov.nist.isg.mist.stitching.lib.optimization;

/**
 * Created by mmajursk on 7/16/2015.
 */
public class MLEPoint {

  public int PIuni;
  public int mu;
  public int sigma;
  public double likelihood;

  public MLEPoint(int PIuni, int mu, int sigma, double l) {
    this.PIuni = PIuni;
    this.mu = mu;
    this.sigma = sigma;
    this.likelihood = l;
  }

}
