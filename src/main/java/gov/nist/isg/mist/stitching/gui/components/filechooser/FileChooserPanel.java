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
// Date: Apr 18, 2014 12:25:29 PM EST
//
// Time-stamp: <Apr 18, 2014 12:25:29 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.gui.components.filechooser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;

/**
 * FileChooserPanel is used as a wrapper to contain a file chooser Utility functions for getting the
 * selected file are available.
 * 
 * @author Tim Blattner
 * @version 1.0
 * 
 */
public class FileChooserPanel extends JPanel implements FocusListener, ActionListener {

  private static final long serialVersionUID = 1L;

  private JLabel label;
  private JTextField input;
  private JButton button;

  /**
   * Creates a file chooser
   * 
   * @param label the label associated with the file chooser
   * @param defLocation the default file
   */
  public FileChooserPanel(String label, String defLocation) {
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
  }

  /**
   * Creates a file chooser with the default being the user.home
   * 
   * @param label the label associated with the file chooser
   */
  public FileChooserPanel(String label) {
    this(label, System.getProperty("user.home"));
  }

  /**
   * Shows an error for this file chooser
   */
  public void showError() {
    this.input.setBackground(Color.RED);
  }

  /**
   * Hides an error for this file chooser
   */
  public void hideError() {
    this.input.setBackground(Color.WHITE);
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
    JFileChooser chooser = new JFileChooser(FileChooserPanel.this.input.getText());
    chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

    int val = chooser.showOpenDialog(FileChooserPanel.this.button);
    if (val == JFileChooser.APPROVE_OPTION) {
      this.input.setText(chooser.getSelectedFile().getAbsolutePath());
    }

  }    
  

}
