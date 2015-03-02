// ================================================================
//
// Disclaimer: IMPORTANT: This software was developed at the National
// Institute of Standards and Technology by employees of the Federal
// Government in the course of their official duties. Pursuant to
// title 17 Section 105 of the United States Code this software is not
// subject to copyright protection and is in the public domain. This
// is an experimental system. NIST assumes no responsibility
// whatsoever for its use by other parties, and makes no guarantees,
// expressed or implied, about its quality, reliability, or any other
// characteristic. We would appreciate acknowledgment if the software
// is used. This software can be redistributed and/or modified freely
// provided that any derivative works bear some notice that they are
// derived from it, and any modified versions bear some notice that
// they have been modified.
//
// ================================================================

// ================================================================
//
// Author: tjb3
// Date: May 10, 2013 2:58:51 PM EST
//
// Time-stamp: <May 10, 2013 2:58:51 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.common;

/**
 * A correlation triple with double-precision correlation and x and y locations.
 * 
 * @author Tim Blattner
 * @version 1.0
 * 
 */
public class CorrelationTriple implements Comparable<CorrelationTriple> {

  private double correlation;
  private int x;
  private int y;

  /**
   * Creates a correlation triple that has a correlation x and y position.
   * 
   * @param corr the correlation
   * @param x the x position
   * @param y the y position
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
