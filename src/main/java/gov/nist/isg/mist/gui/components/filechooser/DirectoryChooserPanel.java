// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 12:21:22 PM EST
//
// Time-stamp: <Apr 18, 2014 12:21:22 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui.components.filechooser;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * DirectoryChooserPanel is used as a wrapper to contain a directory chooser Utility functions for
 * getting the selected directory are available.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class DirectoryChooserPanel extends JPanel implements FocusListener, ActionListener {

  private static final long serialVersionUID = 1L;

  private JLabel label;
  private JTextField input;
  private JButton button;

  /**
   * Creates a directory chooser
   *
   * @param label       the label associated with the directory chooser
   * @param defLocation the default directory
   * @param sz          the size of the text field
   */
  public DirectoryChooserPanel(String label, String defLocation, int sz) {
    super(new FlowLayout(FlowLayout.CENTER));

    File f = new File(defLocation);
    f.mkdirs();

    this.label = new JLabel(label);
    this.input = new JTextField(defLocation, sz);

    this.input.addFocusListener(this);


    this.button = new JButton("Browse");

    this.input.setToolTipText(label);
    this.button.addActionListener(this);

    add(this.label);
    add(this.input);
    add(this.button);
  }

  /**
   * Creates a directory chooser
   *
   * @param label       the label associated with the directory chooser
   * @param defLocation the default directory
   */
  public DirectoryChooserPanel(String label, String defLocation) {
    this(label, defLocation, 20);
  }

  /**
   * Creates a directory chooser with the default location being user.home
   *
   * @param label the label associated with the directory chooser
   */
  public DirectoryChooserPanel(String label) {
    this(label, System.getProperty("user.home"));
  }

  /**
   * Creates a directory chooser with the default location being user.home
   *
   * @param label the label associated with the directory chooser
   * @param sz    the size of the text field
   */
  public DirectoryChooserPanel(String label, int sz) {
    this(label, System.getProperty("user.home"), sz);
  }

  /**
   * Gets the input text field for the directory chooser
   *
   * @return the input text field
   */
  public JTextField getInputField() {
    return this.input;
  }

  /**
   * Shows an error for the text field
   */
  public void showError() {
//    this.input.setBackground(Color.RED);
    this.input.setBackground(Color.PINK);
  }

  /**
   * Hides an error for the text field
   */
  public void hideError() {
    this.input.setBackground(Color.WHITE);
  }

  /**
   * Sets the value for the text field
   *
   * @param value the value
   */
  public void setValue(String value) {
    this.input.setText(value);
  }

  /**
   * Gets the value of the text field
   *
   * @return the text field value
   */
  public String getValue() {
    return this.input.getText();
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
    JFileChooser chooser = new JFileChooser(this.input.getText());
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

    int val = chooser.showOpenDialog(this.button);
    if (val == JFileChooser.APPROVE_OPTION) {
      this.input.setText(chooser.getSelectedFile().getAbsolutePath());
    }

  }

}
