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


// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 12:28:13 PM EST
//
// Time-stamp: <Apr 18, 2014 12:28:13 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui.components.textfield.textFieldModel;

/**
 * Validator is an interface that handles text validation for text fields. Functions for testing the
 * text, getting the values, and getting the error text associated with the validation are
 * provided.
 *
 * @param <T> the type of Object for validation
 * @author Tim Blattner
 * @version 1.0
 */
public interface TextFieldModel<T> {
  /**
   * Tests the text using a specific validator
   *
   * @param val the text you wish to test
   * @return true if the the text is valid
   */
  public boolean validateText(String val);

  /**
   * Gets the error message associated with this validator
   *
   * @return the error message
   */
  public String getErrorMessage();

  /**
   * Gets the value of the text based on the validation type
   *
   * @param val the value you wish to parse
   * @return the value parsed by the validator
   */
  public T getModelValue(String val);

  /**
   * Gets the value of the text based on the validation type. The value that is returned represents
   * the value that is to be used in the text
   *
   * @param val the value you wish to parse
   * @return the value that is to be placed inside the text field
   */
  public String setModelValue(String val);

  /**
   * Updates the view based on the model
   */
  public void updateTextFields();
}
