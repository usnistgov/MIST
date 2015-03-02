package gov.nist.isg.mist.stitching.lib.imagetile.fftw;

/**
 * Type of FFTW plan
 * 
 * @author Tim Blattner
 * @version 1.0
 */
public enum FftwPlanType {

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
  EXHAUSTIVE("Exhaustive", FFTW3Library.FFTW_EXHAUSTIVE)
  ;
  

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


