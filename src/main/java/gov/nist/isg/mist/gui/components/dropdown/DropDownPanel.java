// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



package gov.nist.isg.mist.gui.components.dropdown;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Drop down panel.
 *
 * @author Michael Majurski
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

  public void addComboBoxActionListener(ActionListener a) {
    this.comboBox.addActionListener(a);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    label.setEnabled(enabled);
    comboBox.setEnabled(enabled);
  }

  public void setHelpText(String helpText) {
    this.helpTextArea.setText(helpText);
  }

  public JComboBox getComboBox() {
    return this.comboBox;
  }

  @Override
  public void focusGained(FocusEvent arg0) {
  }

  @Override
  public void focusLost(FocusEvent arg0) {
  }

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
