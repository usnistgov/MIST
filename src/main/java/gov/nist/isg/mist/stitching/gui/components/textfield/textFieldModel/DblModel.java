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
// characteristic. We would appreciate acknowledgement if the software
// is used. This software can be redistributed and/or modified freely
// provided that any derivative works bear some notice that they are
// derived from it, and any modified versions bear some notice that
// they have been modified.
//
// ================================================================

// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 12:34:15 PM EST
//
// Time-stamp: <Apr 18, 2014 12:34:15 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.gui.components.textfield.textFieldModel;

/**
 * Validator that checks double values
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class DblModel implements TextFieldModel<Double> {

  private double min;
  private double max;

  private String errorText;
  private boolean allowEmpty;

  /**
   * Creates a double validator that allows any double
   */
  public DblModel() {
    this(false);
  }

  /**
   * Creates a double validator that checks in bounds by min (inclusive) and max (inclusive)
   *
   * @param min the minimum value that is valid (inclusive)
   * @param max the maximum value that is valid (inclusive)
   */
  public DblModel(double min, double max) {
    this(min, max, false);
  }

  /**
   * Creates an integer validator that checks in bounds by min (inclusive) and max (inclusive)
   *
   * @param allowEmpty enables this validator to accept empty text. Empty text represents a value of
   *                   NaN
   */
  public DblModel(boolean allowEmpty) {
    this.min = Double.NEGATIVE_INFINITY;
    this.max = Double.POSITIVE_INFINITY;
    this.errorText =
        "<html>Please only enter numbers in the text field.<br>" + "Must be any double.</html?";
    this.allowEmpty = allowEmpty;
  }


  /**
   * Creates an integer validator that checks in bounds by min (inclusive) and max (inclusive)
   *
   * @param min        the minimum value that is valid (inclusive)
   * @param max        the maximum value that is valid (inclusive)
   * @param allowEmpty enables this validator to accept empty text. Empty text represents a value of
   *                   NaN
   */
  public DblModel(double min, double max, boolean allowEmpty) {
    this.allowEmpty = allowEmpty;
    this.min = min;
    this.max = max;
    if (allowEmpty) {
      this.errorText =
          "<html>Please only enter numbers in the text field.<br>"
              + "Must be greater than or equal to " + min + " and less than or equal to " + max
              + "<br>empty values enforces use of default parameters.</html>";
    } else {
      this.errorText =
          "<html>Please only enter numbers in the text field.<br>"
              + "Must be greater than or equal to " + min + " and less than or equal to " + max
              + "</html>";
    }
  }


  @Override
  public boolean validateText(String val) {
    if (val.equals("")) {
      if (this.allowEmpty)
        return true;
      return false;
    }
    try {
      double test = Double.parseDouble(val);

      if (test < this.min || test > this.max)
        return false;
      return true;
    } catch (NumberFormatException e) {
      return false;
    }

  }

  @Override
  public String getErrorMessage() {
    return this.errorText;
  }

  @Override
  public Double getModelValue(String val) {
    if (val.equals(""))
      return Double.NaN;
    try {
      return Double.parseDouble(val);
    } catch (NumberFormatException ex) {
      return this.min;
    }
  }

  @Override
  public String setModelValue(String val) {
    double value = getModelValue(val);

    if (Double.isNaN(value)) {
      if (this.allowEmpty)
        return "";
    }

    return Double.toString(value);
  }

  @Override
  public void updateTextFields() {
  }

}
