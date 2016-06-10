// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 12:25:29 PM EST
//
// Time-stamp: <Apr 18, 2014 12:25:29 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui.components.filechooser;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * FileChooserPanel is used as a wrapper to contain a file chooser Utility functions for getting the
 * selected file are available.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class FileChooserPanel extends JPanel implements FocusListener, ActionListener {

  private static final long serialVersionUID = 1L;

  private JLabel label;
  private JTextField input;
  private JButton button;
  private JButton questionButton = null;

  private JDialog helpDialog;
  private JTextArea helpTextArea;

  /**
   * Creates a file chooser with the default being the user.home
   *
   * @param label the label associated with the file chooser
   */
  public FileChooserPanel(String label) {
    this(label, null, System.getProperty("user.home"));
  }

  /**
   * Creates a file chooser
   *
   * @param label    the label associated with the file chooser
   * @param helpText the help text
   */
  public FileChooserPanel(String label, String helpText) {
    this(label, helpText, System.getProperty("user.home"));
  }

  /**
   * Creates a file chooser
   *
   * @param label       the label associated with the file chooser
   * @param helpText    the help text
   * @param defLocation the default file
   */
  public FileChooserPanel(String label, String helpText, String defLocation) {
    super(new FlowLayout(FlowLayout.CENTER));

    File f = new File(defLocation);
    f.mkdirs();

    this.label = new JLabel(label);
    this.input = new JTextField(defLocation, 20);

    this.input.addFocusListener(this);
    this.button = new JButton("Browse");
    this.input.setToolTipText(label);

    this.button.addActionListener(this);

    add(this.label);
    add(this.input);
    add(this.button);

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
      questionButton = new JButton("?");
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
   * Shows an error for this file chooser
   */
  public void showError() {
//    this.input.setBackground(Color.RED);
    this.input.setBackground(Color.PINK);
  }

  /**
   * Hides an error for this file chooser
   */
  public void hideError() {
    this.input.setBackground(Color.WHITE);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    label.setEnabled(enabled);
    input.setEnabled(enabled);
    button.setEnabled(enabled);
  }

  /**
   * Sets the value for the file chooser
   *
   * @param value the file location
   */
  public void setValue(String value) {
    this.input.setText(value);
  }

  /**
   * Gets the value for the file chooser
   *
   * @return the value of the file chooser
   */
  public String getValue() {
    return this.input.getText();
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
   * Gets the file for the file chooser
   *
   * @return the file for the file chooser
   */
  public File getFile() {
    return new File(this.input.getText());
  }

  @Override
  public void focusGained(FocusEvent arg0) {
    this.input.selectAll();
  }

  @Override
  public void focusLost(FocusEvent arg0) {
    this.input.setSelectionEnd(0);
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {

    if (arg0.getSource() == this.questionButton) {
      this.helpDialog.setVisible(true);
    }
    if (arg0.getSource() == this.button) {
      JFileChooser chooser = new JFileChooser(FileChooserPanel.this.input.getText());
      chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

      int val = chooser.showOpenDialog(FileChooserPanel.this.button);
      if (val == JFileChooser.APPROVE_OPTION) {
        this.input.setText(chooser.getSelectedFile().getAbsolutePath());
      }
    }
  }


}
