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
// Date: Jul 2, 2014 11:48:52 AM EST
//
// Time-stamp: <Jul 2, 2014 11:48:52 AM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching;

import gov.nist.isg.mist.stitching.MIST.ExecutionType;
import gov.nist.isg.mist.stitching.gui.StitchingSwingWorker;
import gov.nist.isg.mist.stitching.gui.images.AppImageHelper;
import gov.nist.isg.mist.stitching.gui.panels.advancedTab.AdvancedPanel;
import gov.nist.isg.mist.stitching.gui.panels.helpTab.HelpPanel;
import gov.nist.isg.mist.stitching.gui.panels.inputTab.InputPanel;
import gov.nist.isg.mist.stitching.gui.panels.outputTab.OutputPanel;
import gov.nist.isg.mist.stitching.gui.panels.subgrid.SubgridPanel;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.gui.params.interfaces.GUIParamFunctions;
import gov.nist.isg.mist.stitching.lib.log.Debug;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import ij.IJ;
import ij.gui.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileNotFoundException;
import java.util.prefs.Preferences;

/**
 * Creates the main NIST image sitching gui.
 * 
 * @author Tim Blattner
 * @version 1.0
 * 
 */
public class StitchingGUIFrame extends JFrame implements ActionListener, GUIParamFunctions {

  private static final long serialVersionUID = 1L;

  private static final String PreferenceName = "nist_stitching";

  private JPanel mainFrame;

  private JButton beginStitchingButton;
  private JButton loadParamsButton;
  private JButton saveParamsButton;
  private InputPanel inputPanel;
  private OutputPanel outputPanel;
  private AdvancedPanel advancedPanel;
  private SubgridPanel subgridPanel;
  private HelpPanel helpPanel;

  /**
   * Initializes the stitching for headless mode
   */
  public StitchingGUIFrame() {
    this("MIST", IJ.getInstance() != null ? IJ.getInstance() : new Frame());
  }

  /**
   * Initializes the stitching GUI
   * 
   * @param title the tile of the dialog
   * @param parent the parent frame
   */
  public StitchingGUIFrame(String title, Frame parent) {
    super(title);

    this.setSize(new Dimension(570, 675));
    this.setMinimumSize(new Dimension(570, 675));

    this.mainFrame = new JPanel(new GridBagLayout());

    init();

    add(this.mainFrame);
  }

  /**
   * Initializes the stitching gui (only if not in headless)
   * 
   * @param headless whether the gui is run in headless mode or not
   */
  public void init(boolean headless) {
    if (!headless)
      init();
  }

  private void init() {
    initContent();
    initListeners();
    initTooltips();
  }

  private void initContent() {
    JTabbedPane tabbedPane = new JTabbedPane();

    this.advancedPanel = new AdvancedPanel();
    this.outputPanel = new OutputPanel(this);
    this.subgridPanel = new SubgridPanel(this.outputPanel);
    this.inputPanel = new InputPanel(this.subgridPanel, this.outputPanel);
    this.helpPanel = new HelpPanel();

    this.outputPanel.setInputPanel(this.inputPanel);
    this.outputPanel.setSubGridPanel(this.subgridPanel);
    this.subgridPanel.setInputPanel(this.inputPanel);

    JScrollPane sp = new JScrollPane(this.advancedPanel);
    sp.getVerticalScrollBar().setUnitIncrement(8);

    tabbedPane.addTab("Input", this.inputPanel);
    tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

    tabbedPane.addTab("Output", this.outputPanel);
    tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);

    tabbedPane.addTab("Subgrid", this.subgridPanel);
    tabbedPane.setMnemonicAt(2, KeyEvent.VK_3);

    tabbedPane.addTab("Advanced", sp);
    tabbedPane.setMnemonicAt(3, KeyEvent.VK_4);

    tabbedPane.addTab("Help", this.helpPanel);
    tabbedPane.setMnemonicAt(4, KeyEvent.VK_5);


    GridBagConstraints c = new GridBagConstraints();

    c.gridy = 0;
    this.mainFrame.add(tabbedPane, c);

    JPanel bottomPanel = new JPanel();
    this.beginStitchingButton = new JButton("Begin Stitching");
    this.beginStitchingButton.setPreferredSize(new Dimension(150, 40));
    this.beginStitchingButton.setMinimumSize(new Dimension(150, 40));

    this.saveParamsButton = new JButton("Save Params");
    this.loadParamsButton = new JButton("Load Params");

//    ImageIcon icon = null;
//    JLabel picLabel = null;
//    try {
//      icon = AppImageHelper.loadImage("NIST-Logo_5.png");
//    } catch (FileNotFoundException e) {
//      // Log.msg(LogType.MANDATORY, "ERROR: NIST Logo file not found.");
//    }
//
//    if (icon != null) {
//      picLabel = new JLabel(icon);
//    } else
//      picLabel = new JLabel();
//
//    bottomPanel.add(picLabel);
    bottomPanel.add(this.saveParamsButton);
    bottomPanel.add(this.beginStitchingButton);
    bottomPanel.add(this.loadParamsButton);

    c.gridy = 1;
    this.mainFrame.add(bottomPanel, c);
  }

  private static void initTooltips() {
    ToolTipManager manager = ToolTipManager.sharedInstance();
    manager.setDismissDelay(60000);
    manager.setInitialDelay(500);
    manager.setReshowDelay(500);
    manager.setLightWeightPopupEnabled(true);
  }

  /**
   * Gets the input panel in this gui
   * 
   * @return the input panel
   */
  public InputPanel getInputPanel() {
    return this.inputPanel;
  }

  /**
   * @return the beginStitchingButton
   */
  public JButton getBeginStitchingButton() {
    return this.beginStitchingButton;
  }

  /**
   * @return the loadParamsButton
   */
  public JButton getLoadParamsButton() {
    return this.loadParamsButton;
  }

  /**
   * @return the saveParamsButton
   */
  public JButton getSaveParamsButton() {
    return this.saveParamsButton;
  }

  private void initListeners() {
    this.beginStitchingButton.addActionListener(this);
    this.saveParamsButton.addActionListener(this);
    this.loadParamsButton.addActionListener(this);

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        performExit();
      }

    });

  }

  /**
   * Performs exit functions to save params.
   */
  public void performExit() {
    if (!MIST.isMacro()) {
      StitchingAppParams params = new StitchingAppParams();
      this.saveParamsFromGUI(params, true);

      params.saveParams(Preferences.userRoot().node(PreferenceName));
    }
    this.dispose();
  }

  /**
   * Displays the GUI
   */
  public void display() {
    loadPreviousRunParams();

    try {
      setIconImage(AppImageHelper.loadImage("stitching_icon.png").getImage());
    } catch (FileNotFoundException e) {
      Log.msg(LogType.MANDATORY, "ERROR: Loading nist_stitching.app icon image failed.");
    }

    GUI.center(this);
    setVisible(true);
  }

  private void loadPreviousRunParams() {
    StitchingAppParams oldParams = new StitchingAppParams();
    oldParams.loadParams(Preferences.userRoot().node(PreferenceName));

    Log.setLogLevel(oldParams.getLogParams().getLogLevel());
    Debug.setDebugLevel(oldParams.getLogParams().getDebugLevel());
    
    this.loadParamsIntoGUI(oldParams);
  }

  @Override
  public void actionPerformed(ActionEvent e) {

    if (!MIST.isStitching()) {
      ExecutionType type = null;
      if (e.getSource().equals(this.beginStitchingButton)) {
        if (this.inputPanel.isAssembleWithMetadata())
          type = ExecutionType.RunStitchingFromMeta;
        else
          type = ExecutionType.RunStitching;
      } else if (e.getSource().equals(this.loadParamsButton))
        type = ExecutionType.LoadParams;
      else if (e.getSource().equals(this.saveParamsButton))
        type = ExecutionType.SaveParams;

      StitchingSwingWorker executor = new StitchingSwingWorker(this, type);
      executor.execute();
    } else {
      Log.msg(LogType.MANDATORY, "Stitching is already executing.");
    }
  }

  /**
   * Executes display no overlap
   */
  public void displayNoOverlap() {
    ExecutionType type = ExecutionType.PreviewNoOverlap;
    StitchingSwingWorker executor = new StitchingSwingWorker(this, type);
    executor.execute();
  }

  @Override
  public void loadParamsIntoGUI(StitchingAppParams params) {
    enableLoadingParams();
    this.outputPanel.loadParamsIntoGUI(params);
    this.advancedPanel.loadParamsIntoGUI(params);
    this.subgridPanel.loadParamsIntoGUI(params);
    this.inputPanel.loadParamsIntoGUI(params);
    this.helpPanel.loadParamsIntoGUI(params);
    disableLoadingParams();
  }

  @Override
  public boolean checkAndParseGUI(StitchingAppParams params) {
    if (this.inputPanel.checkAndParseGUI(params) && this.outputPanel.checkAndParseGUI(params)
        && this.advancedPanel.checkAndParseGUI(params) && this.subgridPanel.checkAndParseGUI(params)
        && this.helpPanel.checkAndParseGUI(params))
      return true;
    
    return false;
  }

  @Override
  public boolean checkGUIArgs() {
    if (this.inputPanel.checkGUIArgs() && this.outputPanel.checkGUIArgs() && this.advancedPanel.checkGUIArgs()
        && this.subgridPanel.checkGUIArgs() && this.helpPanel.checkGUIArgs())
      return true;
    
    return false;
  }

  private boolean loadingParams = false;

  @Override
  public void enableLoadingParams() {
    this.loadingParams = true;
    this.inputPanel.enableLoadingParams();
    this.outputPanel.enableLoadingParams();
    this.advancedPanel.enableLoadingParams();
    this.subgridPanel.enableLoadingParams();
    this.helpPanel.enableLoadingParams();
  }

  @Override
  public void disableLoadingParams() {
    this.loadingParams = false;
    this.inputPanel.disableLoadingParams();
    this.outputPanel.disableLoadingParams();
    this.advancedPanel.disableLoadingParams();
    this.subgridPanel.disableLoadingParams();
    this.helpPanel.disableLoadingParams();
  }

  @Override
  public boolean isLoadingParams() {
    return this.loadingParams;
  }

  @Override
  public void saveParamsFromGUI(StitchingAppParams params, boolean isClosing) {
    this.inputPanel.saveParamsFromGUI(params, isClosing);
    this.outputPanel.saveParamsFromGUI(params, isClosing);
    this.advancedPanel.saveParamsFromGUI(params, isClosing);
    this.subgridPanel.saveParamsFromGUI(params, isClosing);
    this.helpPanel.saveParamsFromGUI(params, isClosing);
  }

}
