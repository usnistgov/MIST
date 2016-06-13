// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Jul 2, 2014 11:48:52 AM EST
//
// Time-stamp: <Jul 2, 2014 11:48:52 AM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ToolTipManager;

import gov.nist.isg.mist.MISTMain.ExecutionType;
import gov.nist.isg.mist.gui.StitchingSwingWorker;
import gov.nist.isg.mist.gui.images.AppImageHelper;
import gov.nist.isg.mist.gui.panels.advancedTab.AdvancedPanel;
import gov.nist.isg.mist.gui.panels.helpTab.HelpPanel;
import gov.nist.isg.mist.gui.panels.inputTab.InputPanel;
import gov.nist.isg.mist.gui.panels.outputTab.OutputPanel;
import gov.nist.isg.mist.gui.panels.subgrid.SubgridPanel;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.gui.params.interfaces.GUIParamFunctions;
import gov.nist.isg.mist.lib.log.Debug;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import ij.IJ;
import ij.gui.GUI;

/**
 * Creates the main NIST image sitching gui.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class StitchingGUIFrame extends JFrame implements ActionListener, GUIParamFunctions {

  private static final long serialVersionUID = 1L;

  private static final String PreferenceName = "nist_mist";

  private JPanel mainFrame;

  private JButton beginStitchingButton;
  private JButton loadParamsButton;
  private JButton saveParamsButton;
  private JButton previewNoOverlapButton;
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
   * @param title  the tile of the dialog
   * @param parent the parent frame
   */
  public StitchingGUIFrame(String title, Frame parent) {
    super(title);

    this.mainFrame = new JPanel(new GridBagLayout());

    init();

    add(this.mainFrame);
    Dimension guiSize = new Dimension(600, 620);
    this.setPreferredSize(guiSize);
    this.setSize(guiSize);
    this.setMinimumSize(guiSize);


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
    this.outputPanel = new OutputPanel();
    this.subgridPanel = new SubgridPanel(this.outputPanel);
    this.inputPanel = new InputPanel(this.subgridPanel, this.outputPanel);
    this.helpPanel = new HelpPanel();

    this.outputPanel.setInputPanel(this.inputPanel);
    this.outputPanel.setSubGridPanel(this.subgridPanel);
    this.subgridPanel.setInputPanel(this.inputPanel);

    JScrollPane sp = new JScrollPane(this.advancedPanel);
    sp.getVerticalScrollBar().setUnitIncrement(8);

    // TODO debug why these keybindings don't work
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
    c.fill = GridBagConstraints.BOTH;
    c.weightx = 1.0;
    c.weighty = 1.0;
    this.mainFrame.add(tabbedPane, c);

    JPanel bottomPanel = new JPanel();
    this.beginStitchingButton = new JButton("<html><center>Begin<br>Stitching</html>");
    this.beginStitchingButton.setPreferredSize(new Dimension(130, 40));
    this.beginStitchingButton.setMinimumSize(new Dimension(130, 40));
    this.beginStitchingButton.setToolTipText("Launch Stitching");

    this.previewNoOverlapButton = new JButton("<html><center>Preview<br>(0% overlap)</html>");
    this.previewNoOverlapButton.setPreferredSize(new Dimension(130, 40));
    this.previewNoOverlapButton.setMinimumSize(new Dimension(130, 40));
    this.previewNoOverlapButton.addActionListener(this);
    this.previewNoOverlapButton.setToolTipText("Preview the mosaic image assuming 0% overlap.");

    this.saveParamsButton = new JButton("Save Params");
    this.saveParamsButton.setToolTipText("Save parameters to disk");
    this.loadParamsButton = new JButton("Load Params");
    this.loadParamsButton.setToolTipText("load parameters from disk");

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
    bottomPanel.add(this.previewNoOverlapButton);
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
    if (!MISTMain.isMacro()) {
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

    if (!MISTMain.isStitching()) {
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
      else if (e.getSource().equals(this.previewNoOverlapButton))
        type = ExecutionType.PreviewNoOverlap;

      if (type != null) {
        StitchingSwingWorker executor = new StitchingSwingWorker(this, type);
        executor.execute();
      }
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
    this.inputPanel.loadParamsIntoGUI(params);
    this.subgridPanel.loadParamsIntoGUI(params);
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
