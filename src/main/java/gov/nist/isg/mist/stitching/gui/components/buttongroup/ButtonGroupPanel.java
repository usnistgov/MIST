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
// Date: Apr 18, 2014 12:18:03 PM EST
//
// Time-stamp: <Apr 18, 2014 12:18:03 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.gui.components.buttongroup;

import javax.swing.*;
import javax.swing.border.Border;

import java.awt.*;
import java.util.Enumeration;

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
