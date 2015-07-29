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

package gov.nist.isg.mist.stitching.gui.components.dropdown;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.*;

/**
 * Created by mmajursk on 7/29/2015.
 */
public class DropDownPanel extends JPanel implements FocusListener, ActionListener {

  private static final long serialVersionUID = 1L;


  private JLabel label;
  private JComboBox comboBox;

  private JTextArea helpTextArea;
  private JDialog helpDialog;


  public DropDownPanel(String label, Object[] values, String helpText) {
    super(new FlowLayout(FlowLayout.CENTER));

    this.label = new JLabel(label);
    this.comboBox = new JComboBox(values);

    add(this.label);
    add(this.comboBox);

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

  public void addComboBoxActionListener(ActionListener a) {
    this.comboBox.addActionListener(a);
  }

  @Override
  public void setEnabled(boolean enabled)
  {
    super.setEnabled(enabled);
    label.setEnabled(enabled);
    comboBox.setEnabled(enabled);
  }

  public void setHelpText(String helpText) {
    this.helpTextArea.setText(helpText);
  }

  public JComboBox getComboBox() { return this.comboBox; }

  @Override
  public void focusGained(FocusEvent arg0) { }

  @Override
  public void focusLost(FocusEvent arg0) {}

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() instanceof JButton) {
      JButton btn = (JButton) e.getSource();
      if (btn.getText().equals("?")) {
        this.helpDialog.setVisible(true);
      }
    }
  }
}
