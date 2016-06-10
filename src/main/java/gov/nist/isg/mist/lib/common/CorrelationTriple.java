// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: May 10, 2013 2:58:51 PM EST
//
// Time-stamp: <May 10, 2013 2:58:51 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.common;

/**
 * A correlation triple with double-precision correlation and x and y locations.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class CorrelationTriple implements Comparable<CorrelationTriple> {

  private double correlation;
  private int x;
  private int y;

  /**
   * Creates a correlation triple that has a correlation x and y position.
   *
   * @param corr the correlation
   * @param x    the x position
   * @param y    the y position
   */
  public CorrelationTriple(double corr, int x, int y) {
    this.correlation = corr;
    this.x = x;
    this.y = y;
  }

  @Override
  public CorrelationTriple clone() {
    return new CorrelationTriple(this.correlation, this.x, this.y);
  }

  /**
   * @return the correlation
   */
  public double getCorrelation() {
    return this.correlation;
  }

  /**
   * @param correlation the correlation to set
   */
  public void setCorrelation(double correlation) {
    this.correlation = correlation;
  }

  /**
   * Adds value to correlation
   *
   * @param val the value you want to add to the correlation
   */
  public void incrementCorrelation(double val) {
    this.correlation += val;
  }

  /**
   * @return the x
   */
  public int getX() {
    return this.x;
  }

  /**
   * Gets X with string formatting
   *
   * @return the x value with only 6 decimal places
   */
  public String getMatlabFormatStrX() {
    return String.format("%.10f", (double) this.x);
  }

  /**
   * Gets Y with string formatting
   *
   * @return the y value with only 6 decimal places
   */
  public String getMatlabFormatStrY() {
    return String.format("%.10f", (double) this.y);
  }

  /**
   * Gets correlation with string formatting
   *
   * @return the corrleation with only 6 decimal places
   */
  public String getMatlatFormatStrCorr() {
    return String.format("%.10f", this.correlation);
  }

  /**
   * @param x the x to set
   */
  public void setX(int x) {
    this.x = x;
  }

  /**
   * @return the y
   */
  public int getY() {
    return this.y;
  }

  /**
   * @param y the y to set
   */
  public void setY(int y) {
    this.y = y;
  }

  /**
   * Gets the string representation of the correlation triple
   */
  @Override
  public String toString() {
    return "( " + getMatlatFormatStrCorr() + ", " + this.x + ", " + this.y + " )";
  }

  /**
   * Converts the correlation triple into a CSV style format
   *
   * @return the correlation triple string
   */
  public String toCSVString() {
    return getMatlatFormatStrCorr() + ", " + this.x + ", " + this.y;
  }

  /**
   * Converts the correlation triple into ( x, y) format
   *
   * @return the string format of the correlation triple
   */
  public String toXYString() {
    return "(" + this.x + ", " + this.y + ")";
  }

  /**
   * Compares one correlation with the other. Compares each correlation value.
   */
  @Override
  public int compareTo(CorrelationTriple o) {
    return Double.compare(this.getCorrelation(), o.getCorrelation());
  }

  /**
   * Gets a default string representation of a correlation triple
   *
   * @return a correlation triple that represents a null correlation triple
   */
  public static String toStringStatic() {
    return "( -1.0 , 0.0, 0.0 )";
  }
}
