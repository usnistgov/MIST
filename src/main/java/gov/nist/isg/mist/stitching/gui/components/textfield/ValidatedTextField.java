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
// Date: Apr 18, 2014 12:42:42 PM EST
//
// Time-stamp: <Apr 18, 2014 12:42:42 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.gui.components.textfield;

import gov.nist.isg.mist.stitching.gui.components.textfield.textFieldModel.TextFieldModel;

import javax.swing.*;
import javax.swing.text.*;

import java.awt.*;

/**
 * Creates a text field that is validated by a validator.
 *
 * @param <T> the type for the text field
 * @author Tim Blattner
 * @version 1.0
 */
public class ValidatedTextField<T> extends JTextField {

  private static final long serialVersionUID = 1L;
  private boolean ignoreErrors = false;
  private TextFieldModel<T> validator;

  private TextFieldFilter<T> textFieldFilter;

  /**
   * Creates a text field validated by a validator
   *
   * @param size      the size of the text field
   * @param text      the default text for the text field
   * @param validator the validator for the text field
   */
  public ValidatedTextField(int size, String text, TextFieldModel<T> validator) {
    super(size);
    this.setText(text);
    this.validator = validator;

    PlainDocument doc = (PlainDocument) super.getDocument();

    this.textFieldFilter = new TextFieldFilter<T>(this, validator);
    doc.setDocumentFilter(this.textFieldFilter);
    this.setToolTipText(validator.getErrorMessage());
  }

  /**
   * Sets the validator associated with this text field filter
   *
   * @param validator the validator
   */
  public void setValidator(TextFieldModel<T> validator) {
    this.validator = validator;
    this.setToolTipText(validator.getErrorMessage());

    this.textFieldFilter.setValidator(validator);

    hasError();
  }


  /**
   * Shows an error for the text field
   */
  public void showError() {
//    this.setBackground(Color.RED);
    this.setBackground(Color.PINK);
  }

  /**
   * Hides an error for the text field
   */
  public void hideError() {
    this.setBackground(Color.WHITE);
  }

  /**
   * Checks if there is an error in the text field
   *
   * @return true if an error exists
   */
  public boolean hasError() {
    if (this.validator.validateText(this.getText())) {
      hideError();
      return false;
    }

    showError();
    return true;
  }

  /**
   * Enables ignore errors
   */
  public void enableIgnoreErrors() {
    this.ignoreErrors = true;
  }

  /**
   * Disables ignore errors
   */
  public void disableIgnoreErrors() {
    this.ignoreErrors = false;
  }

  /**
   * Gets if ignore errors or not
   *
   * @return true if ignore errors otherwise false
   */
  public boolean isIgnoreErrors() {
    return this.ignoreErrors;
  }

  /**
   * Creates a text field filter that handles input into the text field.
   *
   * @param <V> the type of the text field
   * @author Tim Blattner
   * @version 1.0
   */
  class TextFieldFilter<V> extends DocumentFilter {

    private TextFieldModel<V> validator;
    private ValidatedTextField<V> txtArea;

    /**
     * Creates a text field filter
     *
     * @param txtArea   the text area associated with the filter
     * @param validator the validator to validate text
     */
    public TextFieldFilter(ValidatedTextField<V> txtArea, TextFieldModel<V> validator) {
      this.txtArea = txtArea;
      this.validator = validator;
    }

    public void setValidator(TextFieldModel<V> validator) {
      this.validator = validator;
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
        throws BadLocationException {
      if (!this.txtArea.isIgnoreErrors()) {
        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder();
        sb.append(doc.getText(0, doc.getLength()));
        sb.insert(offset, string);
        super.insertString(fb, offset, string, attr);

        if (this.validator.validateText(sb.toString())) {
          this.txtArea.setBackground(Color.WHITE);
        } else {
//          this.txtArea.setBackground(Color.RED);
          this.txtArea.setBackground(Color.PINK);
        }

      } else
        super.insertString(fb, offset, string, attr);

    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
        throws BadLocationException {
      if (!this.txtArea.isIgnoreErrors()) {
        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder();
        sb.append(doc.getText(0, doc.getLength()));
        sb.replace(offset, offset + length, text);
        super.replace(fb, offset, length, text, attrs);

        if (this.validator.validateText(sb.toString())) {
          this.txtArea.setBackground(Color.WHITE);
        } else {
//          this.txtArea.setBackground(Color.RED);
          this.txtArea.setBackground(Color.PINK);
        }
      } else
        super.replace(fb, offset, length, text, attrs);

    }

    @Override
    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
      if (!this.txtArea.isIgnoreErrors()) {
        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder();
        sb.append(doc.getText(0, doc.getLength()));
        sb.delete(offset, offset + length);
        super.remove(fb, offset, length);

        if (this.validator.validateText(sb.toString())) {
          this.txtArea.setBackground(Color.WHITE);
        } else {
//          this.txtArea.setBackground(Color.RED);
          this.txtArea.setBackground(Color.PINK);

        }
      } else
        super.remove(fb, offset, length);

    }
  }
}
