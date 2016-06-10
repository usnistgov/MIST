// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 12:18:03 PM EST
//
// Time-stamp: <Apr 18, 2014 12:18:03 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui.components.buttongroup;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;

/**
 * ButtonGroupPanel is used as a wrapper to contain a button group of radio buttons into a single
 * panel. Utility functions for getting the selected value are available.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class ButtonGroupPanel extends JPanel {

  private static final long serialVersionUID = 1L;

  String title;
  private JRadioButton[] radio_btns;
  private ButtonGroup radio_btn_group;

  /**
   * Constructs a button group panel
   *
   * @param labels the labels for the radio buttons
   * @param title  the title of the panel
   */
  public ButtonGroupPanel(Object[] labels, String title) {
    int cols = 3;
    int rows = (int) Math.ceil((double) labels.length / (double) cols);

    super.setLayout(new GridLayout(rows, cols, 0, 0));
    this.radio_btns = new JRadioButton[labels.length];
    this.radio_btn_group = new ButtonGroup();
    this.title = title;

    for (int i = 0; i < this.radio_btns.length; i++) {
      this.radio_btns[i] = new JRadioButton(labels[i].toString());
      this.radio_btns[i].setActionCommand(labels[i].toString());
      this.radio_btn_group.add(this.radio_btns[i]);
      this.add(this.radio_btns[i]);
    }

    this.radio_btns[0].setSelected(true);

    Border gray_line = BorderFactory.createLineBorder(Color.GRAY);

    this.setBorder(BorderFactory.createTitledBorder(gray_line, title));

  }

  /**
   * Gets the radio buttons
   *
   * @return the radio buttons
   */
  public JRadioButton[] getRadioButtons() {
    return this.radio_btns;
  }

  /**
   * Gets the button group
   *
   * @return the button group
   */
  public ButtonGroup getButtonGroup() {
    return this.radio_btn_group;
  }

  /**
   * Sets the selected value for the button group
   *
   * @param value the value that you wish the button group to select
   */
  public void setValue(String value) {
    Enumeration<AbstractButton> abs = this.radio_btn_group.getElements();

    while (abs.hasMoreElements()) {
      AbstractButton btn = abs.nextElement();
      if (btn.getActionCommand().equals(value)) {
        btn.doClick();
        break;
      }
    }

  }

  /**
   * Enables all buttons
   */
  public void enableAllButtons() {
    for (JRadioButton btn : this.radio_btns) {
      btn.setEnabled(true);
    }
  }

  /**
   * Disables all buttons except one
   *
   * @param buttonToEnable the button that is to be enabled
   */
  public void disableAllButtonsExcept(String buttonToEnable) {
    for (JRadioButton btn : this.radio_btns) {
      if (btn.getText().equalsIgnoreCase(buttonToEnable)) {
        btn.setEnabled(true);
        btn.setSelected(true);
      } else {
        btn.setEnabled(false);
        btn.doClick();
      }
    }
  }

  /**
   * Disables a button
   *
   * @param button the button to disable
   */
  public void disableButton(String button) {
    for (JRadioButton btn : this.radio_btns) {
      if (btn.getText().equalsIgnoreCase(button)) {
        btn.setEnabled(false);
      }
    }
  }

  /**
   * Enables a button
   *
   * @param button the button to enable
   */
  public void enableButton(String button) {
    for (JRadioButton btn : this.radio_btns) {
      if (btn.getText().equalsIgnoreCase(button)) {
        btn.setEnabled(true);
      }
    }
  }

  /**
   * Gets the selected button
   *
   * @return the selected button
   */
  public String getValue() {
    return this.radio_btn_group.getSelection().getActionCommand();
  }
}
