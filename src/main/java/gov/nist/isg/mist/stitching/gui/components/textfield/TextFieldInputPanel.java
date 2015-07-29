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
// Date: Apr 18, 2014 12:39:52 PM EST
//
// Time-stamp: <Apr 18, 2014 12:39:52 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.gui.components.textfield;

import gov.nist.isg.mist.stitching.gui.components.textfield.textFieldModel.TextFieldModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Cretes a text field input panel
 * 
 * @author Tim Blattner
 * @version 1.0
 * 
 * @param <T> the type of text field
 */
public class TextFieldInputPanel<T> extends JPanel implements ActionListener, FocusListener {

  private static final long serialVersionUID = 1L;

  private JDialog helpDialog;

  private JLabel label;
  private ValidatedTextField<T> input;
  private TextFieldModel<T> validator;
  private JTextArea helpTextArea;

  private static final int defaultSize = 6;

  /**
   * Creates a text field input panel
   * 
   * @param label the label for the text field
   * @param text the text inside the text field
   * @param validator the validator associated with the text field
   */
  public TextFieldInputPanel(String label, String text, TextFieldModel<T> validator) {
    this(label, text, defaultSize, validator);
  }


  /**
   * Creates a text field input panel
   * 
   * @param label the label for the text field
   * @param text the text inside the text field
   * @param validator the validator associated with the text field
   * @param helpText the help text that describes this text field
   */
  public TextFieldInputPanel(String label, String text, TextFieldModel<T> validator, String helpText) {
    this(label, text, defaultSize, validator, helpText);
  }

  /**
   * Creates a text field input panel
   * 
   * @param label the label for the text field
   * @param text the text inside the text field
   * @param sz the size of the text field
   * @param validator the validator associated with the text field
   */
  public TextFieldInputPanel(String label, String text, int sz, TextFieldModel<T> validator) {
    this(label, text, sz, validator, null);


  }

  /**
   * Creates a text field input panel with help text
   * 
   * @param label the label for the text field
   * @param text the text inside the text field
   * @param sz the size of the text field
   * @param validator the validator associated with the text field
   * @param helpText the help text that describes this text field
   */
  public TextFieldInputPanel(String label, String text, int sz, TextFieldModel<T> validator,
      String helpText) {
    super(new FlowLayout(FlowLayout.LEFT));

    this.validator = validator;
    this.label = new JLabel(label);
    this.input = new ValidatedTextField<T>(sz, text, validator);

    this.input.addFocusListener(this);
    
    add(this.label);
    add(this.input);

    if (helpText != null) {

      this.helpTextArea = new JTextArea(helpText, 10, 40);
      this.helpTextArea.setLineWrap(true);
      this.helpTextArea.setEditable(false);
      this.helpTextArea.setWrapStyleWord(true);

      this.helpDialog = new JDialog();

      this.helpDialog.setSize(new Dimension(300, 300));
      this.helpDialog.setLocationRelativeTo(this);
      this.helpDialog.setTitle(this.label.getText() + " Help");

      JScrollPane scroll = new JScrollPane(this.helpTextArea);
      this.helpDialog.add(scroll);

      // Add question mark
      JButton questionButton = new JButton("?");
      questionButton.setFocusable(false);

      Insets insets = questionButton.getInsets();
      insets.top = 0;
      insets.bottom = 0;
      insets.left = 0;
      insets.right = 0;
      questionButton.setMargin(insets);

      questionButton.addActionListener(this);
      add(questionButton);

    }


  }

  /**
   * Sets the help text for the help text area
   * 
   * @param text the text to set the help text to
   */
  public void setHelpText(String text) {
    if(text != null)
      this.helpTextArea.setText(text);
  }

  /**
   * Sets the validator associated with this text field
   * 
   * @param validator the new validator
   */
  public void setValidator(TextFieldModel<T> validator) {
    this.validator = validator;
    this.input.setValidator(validator);

  }

  /**
   * Sets the value for the text field (String)
   * 
   * @param value the String value
   */
  public void setValue(String value) {
    this.input.setText(this.validator.setModelValue(value));
  }



  /**
   * Sets the value for the text field (integer)
   * 
   * @param value the integer value
   */
  public void setValue(int value) {
    this.setValue(Integer.toString(value));
  }

  /**
   * Sets the value for the text field (double)
   * 
   * @param value the double value
   */
  public void setValue(double value) {
    this.setValue(Double.toString(value));
  }


  /**
   * Checks if an error exists in the input
   * 
   * @return true if an error exists
   */
  public boolean hasError() {
    return this.input.hasError();
  }

  /**
   * Shows the error for this text field
   */
  public void showError() {
    this.input.showError();
  }

  /**
   * Hides the error for this text field
   */
  public void hideError() {
    this.input.hideError();
  }

  /**
   * Gets the value for the text field parsed by the validator
   * 
   * @return the value parsed by the validator
   */
  public T getValue() {
    return this.validator.getModelValue(this.input.getText());
  }

  /**
   * Enables ignoring errors
   */
  public void enableIgnoreErrors() {
    this.input.enableIgnoreErrors();
  }

  /**
   * Disables ignoring errors
   */
  public void disableIgnoreErrors() {
    this.input.disableIgnoreErrors();
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);

    this.label.setEnabled(enabled);
    this.input.setEnabled(enabled);
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() instanceof JButton) {
      JButton btn = (JButton) e.getSource();
      if (btn.getText().equals("?")) {
        // helpDialog.setLocationRelativeTo(this);
        this.helpDialog.setVisible(true);
      }
    }
  }


  @Override
  public void focusGained(FocusEvent e) {
    this.input.selectAll();    
  }


  @Override
  public void focusLost(FocusEvent e) {
    this.input.setSelectionEnd(0);
  }

}
