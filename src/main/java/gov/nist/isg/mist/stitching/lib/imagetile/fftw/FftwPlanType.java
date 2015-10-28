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
// Date: May 10, 2013 2:58:58 PM EST
//
// Time-stamp: <May 10, 2013 2:58:58 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.imagetile.fftw;

/**
 * Type of FFTW plan
 *
 * @author Tim Blattner
 * @version 1.0
 */
public enum FftwPlanType {

  /**
   * Selects Estimate FFTW planning mode
   */
  ESTIMATE("Measure", FFTW3Library.FFTW_ESTIMATE),

  /**
   * Selects measure FFTW planning mode
   */
  MEASURE("Measure", FFTW3Library.FFTW_MEASURE),

  /**
   * Selects patient FFTW planning mode
   */
  PATIENT("Patient", FFTW3Library.FFTW_PATIENT),

  /**
   * Selects exhaustive FFTW planning mode
   */
  EXHAUSTIVE("Exhaustive", FFTW3Library.FFTW_EXHAUSTIVE);


  private FftwPlanType(final String text, int val) {
    this.text = text;
    this.val = val;
  }

  private final String text;
  private final int val;

  @Override
  public String toString() {
    return this.text;
  }

  /**
   * Gets the FFTW planning mode value
   *
   * @return the FFTW planning mode value
   */
  public int getVal() {
    return this.val;
  }
} 


