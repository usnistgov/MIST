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
// Date: Apr 18, 2014 12:35:55 PM EST
//
// Time-stamp: <Apr 18, 2014 12:35:55 PM tjb3>
//
//
// ================================================================

package main.gov.nist.isg.mist.stitching.gui.components.textfield.textFieldModel;

/**
 * Validator that checks integer values
 * 
 * @author Tim Blattner
 * @version 1.0
 * 
 */
public class IntModel implements TextFieldModel<Integer> {

  private boolean allowEmpty;

  private int min;
  private int max;

  private String errorText;

  /**
   * Creates an integer validator that allows any integer
   */
  public IntModel() {
    this.allowEmpty = false;
    this.min = Integer.MIN_VALUE;
    this.max = Integer.MAX_VALUE;
    this.errorText =
        "<html>Please only enter numbers in the text field.<br>" + "Must be any integer.</html?";
  }

  /**
   * Creates an integer validator that checks in bounds by min (inclusive) and max (inclusive)
   * 
   * @param min the minimum value that is valid (inclusive)
   * @param max the maximum value that is valid (inclusive)
   */
  public IntModel(int min, int max) {
    this.allowEmpty = false;
    this.min = min;
    this.max = max;
    this.errorText =
        "<html>Please only enter numbers in the text field.<br>"
            + "Must be greater than or equal to " + min + " and less than or equal to " + max
            + "</html?";
  }

  /**
   * Creates an integer validator that checks in bounds by min (inclusive) and max (inclusive)
   * 
   * @param min the minimum value that is valid (inclusive)
   * @param max the maximum value that is valid (inclusive)
   * @param allowEmpty enables this validator to accept empty text. Empty text represents a value of
   *        0
   */
  public IntModel(int min, int max, boolean allowEmpty) {
    this.allowEmpty = allowEmpty;
    this.min = min;
    this.max = max;
    this.errorText =
        "<html>Please only enter numbers in the text field.<br>"
            + "Must be greater than or equal to " + min + " and less than or equal to " + max
            + "<br>empty values enforces use of default parameters.</html>";
  }



  @Override
  public boolean validateText(String val) {
    if (val.equals("")) {
      if (this.allowEmpty) {
        return true;
      }
      return false;
    }

    try {
      int test = Integer.parseInt(val);

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
  public Integer getModelValue(String val) {
    if (val.equals(""))
      return 0;
    try {
      return Integer.parseInt(val);
    } catch (NumberFormatException ex) {
      return this.min;
    }
  }

  @Override
  public String setModelValue(String val) {
    int value = getModelValue(val);

    if (value == 0) {
      if (this.allowEmpty)
        return "";
    }

    return Integer.toString(value);
  }

  @Override
  public void updateTextFields() {    
  }

}
