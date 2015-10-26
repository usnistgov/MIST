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
// Date: Apr 18, 2014 1:36:52 PM EST
//
// Time-stamp: <Apr 18, 2014 1:36:52 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.gui;

import gov.nist.isg.mist.stitching.gui.executor.StitchingExecutor;
import gov.nist.isg.mist.stitching.gui.images.AppImageHelper;
import ij.gui.GUI;
import gov.nist.isg.mist.stitching.lib.log.Debug;
import gov.nist.isg.mist.stitching.lib.log.Debug.DebugType;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileNotFoundException;

/**
 * Creates a windows to display the status of the stitching application
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class StitchingStatusFrame extends JFrame implements ActionListener, PropertyChangeListener, WindowListener {

  private static final long serialVersionUID = 1L;

  private JPanel mainFrame;

  private StitchingExecutor executor;

  private JProgressBar progressBar;
  private JLabel progressLabel;

  private JComboBox loggingLevel;
  private JComboBox debugLevel;

  private JButton cancelButton;

  /**
   * Constructs the stitching status frame
   *
   * @param executor the stitching executor to reference
   * @param headless whether we are in headless mode or not
   */
  public StitchingStatusFrame(StitchingExecutor executor, boolean headless) {
    this.executor = executor;
  }

  /**
   * Initializes the stitching status frame with a stitching executor
   *
   * @param executor the stitching executor
   */
  public StitchingStatusFrame(StitchingExecutor executor) {
    this("MIST Status", executor);
  }

  /**
   * Constructs the stitching status frame
   *
   * @param title    the title of the frame
   * @param executor the stitching executor to reference
   */
  public StitchingStatusFrame(String title, StitchingExecutor executor) {
    super(title);

    super.setLayout(new GridBagLayout());

    this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    this.setSize(new Dimension(525, 150));

    this.executor = executor;

    init();
  }

  private void init() {
    initContent();
    initListeners();
  }

  private void initContent() {
    this.mainFrame = new JPanel(new GridBagLayout());

    GridBagConstraints c = new GridBagConstraints();

    JPanel progressPanel = new JPanel(new FlowLayout());
    this.progressBar = new JProgressBar();
    this.progressLabel = new JLabel("Progress: ");
    progressPanel.add(this.progressLabel);
    progressPanel.add(this.progressBar);

    this.progressBar.setString("Waiting to start");
    this.progressBar.setStringPainted(true);
    this.progressBar.addPropertyChangeListener(this);

    JPanel logPanel = new JPanel(new GridBagLayout());

    this.loggingLevel = new JComboBox(LogType.values());
    this.debugLevel = new JComboBox(DebugType.values());
    this.loggingLevel.setSelectedItem(Log.getLogLevel());
    this.debugLevel.setSelectedItem(Debug.getDebugLevel());

    c.gridy = 0;
    c.anchor = GridBagConstraints.LINE_END;
    logPanel.add(new JLabel("Log Level"), c);
    c.gridy = 1;
    logPanel.add(this.loggingLevel, c);


    c.gridy = 0;

    logPanel.add(new JLabel("Debug Level"), c);
    c.gridy = 1;
    c.insets = new Insets(0, 10, 0, 0);
    logPanel.add(this.debugLevel, c);


    c.insets = new Insets(0, 0, 0, 0);
    c.gridy = 1;
    c.gridx = 0;
    c.anchor = GridBagConstraints.LINE_START;
    this.mainFrame.add(progressPanel, c);

    c.anchor = GridBagConstraints.LINE_END;
    c.gridx = 1;
    this.mainFrame.add(logPanel, c);


    c.gridy = 0;
    c.gridx = 0;
    c.anchor = GridBagConstraints.CENTER;

    add(this.mainFrame, c);

    c.gridy = 1;
    c.gridx = 0;
    c.insets = new Insets(10, 0, 0, 0);
    this.cancelButton = new JButton("Cancel Execution");
    add(this.cancelButton, c);
  }

  /**
   * Gets the progress bar associated with this frame
   *
   * @return the progress bar
   */
  public JProgressBar getProgressBar() {
    return this.progressBar;
  }

  /**
   * Gets the progress bar label associated with this fram
   *
   * @return the progress bar label
   */
  public JLabel getProgressLabel() {
    return this.progressLabel;
  }

  private void initListeners() {
    this.loggingLevel.addActionListener(this);
    this.debugLevel.addActionListener(this);
    this.cancelButton.addActionListener(this);

    addWindowListener(this);

  }

  private void performExit() {
    if (this.executor != null)
      this.executor.cancelExecution();
  }

  /**
   * Displays the stitching status executor frame
   */
  public void display() {


    try {
      setIconImage(AppImageHelper.loadImage("stitching_icon.png").getImage());
    } catch (FileNotFoundException e) {
      Log.msg(LogType.MANDATORY, "ERROR: Loading nist_stitching.app" + " icon image failed.");
    }

    if (!GraphicsEnvironment.isHeadless()) {
      GUI.center(this);
      setVisible(true);
    }
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {

    Object src = arg0.getSource();

    if (src instanceof JComboBox) {
      JComboBox action = (JComboBox) src;

      if (action.equals(this.loggingLevel)) {
        Log.setLogLevel((LogType) action.getSelectedItem());
      } else if (action.equals(this.debugLevel)) {
        Debug.setDebugLevel((DebugType) action.getSelectedItem());
      }
    } else if (src.equals(this.cancelButton)) {
      performExit();
      this.dispose();
    }
  }

  @Override
  public void propertyChange(PropertyChangeEvent arg0) {
    int progress = StitchingStatusFrame.this.progressBar.getValue();
    progress++;
    this.progressBar.setValue(progress);
  }

  @Override
  public void windowActivated(WindowEvent arg0) {
  }

  @Override
  public void windowClosed(WindowEvent arg0) {
  }

  @Override
  public void windowClosing(WindowEvent arg0) {
    performExit();
  }

  @Override
  public void windowDeactivated(WindowEvent arg0) {
  }

  @Override
  public void windowDeiconified(WindowEvent arg0) {
  }

  @Override
  public void windowIconified(WindowEvent arg0) {
  }

  @Override
  public void windowOpened(WindowEvent arg0) {
  }

}
