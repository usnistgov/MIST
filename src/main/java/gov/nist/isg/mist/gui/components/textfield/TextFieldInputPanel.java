// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 12:39:52 PM EST
//
// Time-stamp: <Apr 18, 2014 12:39:52 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui.components.textfield;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import gov.nist.isg.mist.gui.components.textfield.textFieldModel.TextFieldModel;

/**
 * Cretes a text field input panel
 *
 * @param <T> the type of text field
 * @author Tim Blattner
 * @version 1.0
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
   * @param label     the label for the text field
   * @param text      the text inside the text field
   * @param validator the validator associated with the text field
   */
  public TextFieldInputPanel(String label, String text, TextFieldModel<T> validator) {
    this(label, text, defaultSize, validator);
  }


  /**
   * Creates a text field input panel
   *
   * @param label     the label for the text field
   * @param text      the text inside the text field
   * @param validator the validator associated with the text field
   * @param helpText  the help text that describes this text field
   */
  public TextFieldInputPanel(String label, String text, TextFieldModel<T> validator, String helpText) {
    this(label, text, defaultSize, validator, helpText);
  }

  /**
   * Creates a text field input panel
   *
   * @param label     the label for the text field
   * @param text      the text inside the text field
   * @param sz        the size of the text field
   * @param validator the validator associated with the text field
   */
  public TextFieldInputPanel(String label, String text, int sz, TextFieldModel<T> validator) {
    this(label, text, sz, validator, null);


  }

  /**
   * Creates a text field input panel with help text
   *
   * @param label     the label for the text field
   * @param text      the text inside the text field
   * @param sz        the size of the text field
   * @param validator the validator associated with the text field
   * @param helpText  the help text that describes this text field
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
      questionButton.setPreferredSize(new Dimension(15, 20));
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
    if (text != null)
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

  public String getInputText() {
    return this.input.getText();
  }

}
