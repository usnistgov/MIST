// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 12:53:27 PM EST
//
// Time-stamp: <Apr 18, 2014 12:53:27 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui.panels.advancedTab;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import gov.nist.isg.mist.gui.components.helpDialog.HelpDocumentationViewer;
import gov.nist.isg.mist.gui.components.textfield.TextFieldInputPanel;
import gov.nist.isg.mist.gui.components.textfield.textFieldModel.DblModel;
import gov.nist.isg.mist.gui.components.textfield.textFieldModel.IntModel;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.gui.params.interfaces.GUIParamFunctions;
import gov.nist.isg.mist.lib.imagetile.Stitching;
import gov.nist.isg.mist.lib.log.Debug;
import gov.nist.isg.mist.lib.log.Log;

/**
 * Creates the advanced options panel
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class AdvancedPanel extends JPanel implements GUIParamFunctions, ActionListener {

  private static final String repeatabilityHelp = "During translation optimization, uses the "
      + "user-specified repeatability of the stage. Leave this field blank to use the default. "
      + "This value represents the uncertainty that the microscope stage has "
      + "related to the mechanics that move the stage. Setting this value may "
      + "increase the search space that is used to find the correct translation "
      + "between neighboring images. This value is specified in pixels.";


  private static final String horizontalOverlapHelp = "During translation optimization, uses the "
      + "user-specified horizontal overlap for computing the repeatability of "
      + "the stage. Leave this field blank to use the default. Modifying this "
      + "field will aid in correcting translations that have low correlation in "
      + "the horizontal direction. By default we compute the horizontal overlap "
      + "based on the translations. This value is specified in percent.";

  private static final String verticalOverlapHelp = "During translation optimization, uses the "
      + "user-specified vertical overlap for computing the repeatability of the "
      + "stage. Leave this field blank to use the default. Modifying this field "
      + "will aid in correcting translations that have low correlation in the "
      + "vertical direction. By default we compute the vertical overlap based on "
      + "the translations. This value is specified in percent.";

  private static final String overlapUncertaintyHelp = "During translation optimization, uses the "
      + "user-specified overlap uncertainty for computing the repeatability of the "
      + "stage. Leave this field blank to use the default. Modifying this field "
      + "will aid in correcting translations where the overlap uncertainty should be increased. "
      + "This value is specified in percent."
      + "\n\nTIP: Value should not exceed 20.0, default is 5.0";

  private static final String numFftPeaksHelp = "Specifies the number of peaks to check when"
      + " computing the phase correlation image alignment method. Modifying this value can yield "
      + "more accurate pre-optimization displacements. \n\nTIP: Value should not exceed 10.0, default is 2.0";

  private static final String numHCHelp = "Specifies the type of translation refinement " +
      "optimization. A single hill climb starting at the computed/estimated translation. " +
      "Multiple hill climbs starting at the computed/estimated translation and <i>n</i> " +
      "additional points. Exhaustive search of all valid translations within the stage " +
      "repeatability bounds.";


  private static final long serialVersionUID = 1L;

  private TextFieldInputPanel<Integer> numFFTPeaks;
  private TextFieldInputPanel<Integer> maxRepeatability;
  private TextFieldInputPanel<Double> horizontalOverlap;
  private TextFieldInputPanel<Double> verticalOverlap;
  private TextFieldInputPanel<Double> overlapUncertainty;


  private ParallelOptPane parallelOptions;
  private JComboBox loggingLevel;
  private JComboBox debugLevel;
  private JCheckBox useDoublePrecision = new JCheckBox("Use Double Precision Math?");
  private JCheckBox useBioFormats = new JCheckBox("Use BioFormats Image Reader?");
  private JCheckBox suppressModalWarningDialog = new JCheckBox("Suppress Any Modal Warning Dialogs?");

  private JComboBox translationRefinementType;
  private TextFieldInputPanel<Integer> numTransRefineHillClimbs;

  /**
   * Initializes the advanced options panel
   */
  public AdvancedPanel() {

    this.loggingLevel = new JComboBox(Log.LogType.values());
    this.debugLevel = new JComboBox(Debug.DebugType.values());
    this.translationRefinementType = new JComboBox(Stitching.TranslationRefinementType.values());

    this.loggingLevel.setSelectedItem(Log.getLogLevel());
    this.debugLevel.setSelectedItem(Debug.getDebugLevel());
    this.translationRefinementType.setSelectedItem(Stitching.TranslationRefinementType.SINGLE_HILL_CLIMB);

    this.numFFTPeaks =
        new TextFieldInputPanel<Integer>("Number of FFT Peaks", "",
            new IntModel(1, 100, true), numFftPeaksHelp);

    this.maxRepeatability =
        new TextFieldInputPanel<Integer>("Stage Repeatability", "", new IntModel(1,
            Integer.MAX_VALUE, true), repeatabilityHelp);

    this.horizontalOverlap =
        new TextFieldInputPanel<Double>("Horizontal overlap", "",
            new DblModel(0.0, 100.0, true), horizontalOverlapHelp);
    this.verticalOverlap =
        new TextFieldInputPanel<Double>("Vertical overlap", "", new DblModel(0.0, 100.0, true),
            verticalOverlapHelp);

    this.overlapUncertainty =
        new TextFieldInputPanel<Double>("Overlap uncertainty", "", new DblModel(0.0, 100.0, true),
            overlapUncertaintyHelp);


    this.numTransRefineHillClimbs = new TextFieldInputPanel<Integer>("Number", "", new IntModel(1,
        Integer.MAX_VALUE, true), numHCHelp);
    this.numTransRefineHillClimbs.setEnabled(false);

    this.parallelOptions = new ParallelOptPane();


    setFocusable(false);

    initControls();
    initListeners();

  }

  private void initListeners() {
    this.loggingLevel.addActionListener(this);
    this.debugLevel.addActionListener(this);
    this.translationRefinementType.addActionListener(this);
  }

  private void initControls() {

    JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    JPanel vertPanel = new JPanel(new GridBagLayout());

    GridBagConstraints c = new GridBagConstraints();

    JButton qButton = new JButton("Help?");

    qButton.addActionListener(new HelpDocumentationViewer("advanced-parameters-optional-"));


    // setup the stage model params Panel
    JPanel stageModelPanel = new JPanel(new GridBagLayout());
    stageModelPanel.setBorder(
        new TitledBorder(new LineBorder(Color.BLACK), "Stage Model Parameters"));
    c.insets = new Insets(0, 0, 0, 0);
    c.gridy = 0;
    c.gridx = 0;
    c.gridwidth = 1;
    c.anchor = GridBagConstraints.LINE_START;
    stageModelPanel.add(this.maxRepeatability, c);
    c.gridy = 1;
    stageModelPanel.add(this.horizontalOverlap, c);
    c.gridy = 2;
    stageModelPanel.add(this.verticalOverlap, c);
    c.gridy = 3;
    stageModelPanel.add(this.overlapUncertainty, c);


    // setup the logging panel
    JPanel logPanel = new JPanel();
    c.gridy = 0;
    logPanel.add(new JLabel("Log Level"), c);
    c.gridy = 1;
    logPanel.add(this.loggingLevel, c);

    c.gridy = 0;

    c.insets = new Insets(0, 10, 0, 0);
    logPanel.add(new JLabel("Debug Level"), c);
    c.gridy = 1;
    logPanel.add(this.debugLevel, c);
    c.insets = new Insets(0, 0, 0, 0);

    // setup translation refinement options panel
    JPanel transRefinePanel = new JPanel(new GridBagLayout());
    c.gridy = 0;
    c.gridx = 0;
    transRefinePanel.add(new JLabel("Translation Refinement Method"), c);
    c.gridy = 1;
    transRefinePanel.add(this.translationRefinementType, c);
    c.gridy = 1;
    c.gridx = 1;
    transRefinePanel.add(this.numTransRefineHillClimbs, c);

    // setup the other advanced params
    JPanel otherAdvancedPanel = new JPanel(new GridBagLayout());
    otherAdvancedPanel.setBorder(
        new TitledBorder(new LineBorder(Color.BLACK), "Other Advanced Parameters"));
    c.gridy = 0;
    c.gridx = 0;
    c.gridwidth = 1;
    c.anchor = GridBagConstraints.LINE_START;
    otherAdvancedPanel.add(this.suppressModalWarningDialog, c);
    c.gridy++;
    otherAdvancedPanel.add(this.useBioFormats, c);
    c.gridy++;
    otherAdvancedPanel.add(this.useDoublePrecision, c);
    c.gridy++;
    otherAdvancedPanel.add(transRefinePanel, c);
    c.gridy++;
    otherAdvancedPanel.add(this.numFFTPeaks, c);
    c.gridy++;
    otherAdvancedPanel.add(logPanel, c);


    c.anchor = GridBagConstraints.NORTHEAST;
    c.gridy = 0;
    vertPanel.add(qButton, c);
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.LINE_START;

    c.gridy = 1;
    c.anchor = GridBagConstraints.CENTER;
    c.fill = GridBagConstraints.HORIZONTAL;
    vertPanel.add(stageModelPanel, c);
    c.insets = new Insets(10, 0, 0, 0);
    c.gridy = 2;
    vertPanel.add(otherAdvancedPanel, c);
    c.gridy = 3;
    vertPanel.add(this.parallelOptions.getStitchingTypePanel(), c);
    c.gridy = 4;
    vertPanel.add(this.parallelOptions.getProgramPanel(), c);

    mainPanel.add(vertPanel);
    add(mainPanel);
  }

  @Override
  public void loadParamsIntoGUI(StitchingAppParams params) {
    this.numFFTPeaks.setValue(params.getAdvancedParams().getNumFFTPeaks());
    this.maxRepeatability.setValue(params.getAdvancedParams().getRepeatability());
    this.horizontalOverlap.setValue(params.getAdvancedParams().getHorizontalOverlap());
    this.verticalOverlap.setValue(params.getAdvancedParams().getVerticalOverlap());
    this.overlapUncertainty.setValue(params.getAdvancedParams().getOverlapUncertainty());
    this.parallelOptions.loadParamsIntoGUI(params);
    this.useDoublePrecision.setSelected(params.getAdvancedParams().isUseDoublePrecision());
    this.useBioFormats.setSelected(params.getAdvancedParams().isUseBioFormats());
    this.suppressModalWarningDialog.setSelected(params.getAdvancedParams().isSuppressModelWarningDialog());
    this.translationRefinementType.setSelectedItem(params.getAdvancedParams()
        .getTranslationRefinementType());
    this.numTransRefineHillClimbs.setValue(params.getAdvancedParams().getNumTranslationRefinementStartPoints());


    Log.LogType logType = params.getLogParams().getLogLevel();

    if (logType == null)
      logType = Log.LogType.MANDATORY;

    Debug.DebugType debugType = params.getLogParams().getDebugLevel();

    if (debugType == null)
      debugType = Debug.DebugType.NONE;

    this.loggingLevel.setSelectedItem(logType);
    this.debugLevel.setSelectedItem(debugType);

  }


  @Override
  public boolean checkAndParseGUI(StitchingAppParams params) {
    if (checkGUIArgs()) {
      saveParamsFromGUI(params, false);
      return true;
    }
    return false;
  }

  @Override
  public boolean checkGUIArgs() {

    return this.parallelOptions.checkGUIArgs();
  }

  private boolean loadingParams = false;

  @Override
  public void enableLoadingParams() {
    this.loadingParams = true;
    this.numFFTPeaks.enableIgnoreErrors();
    this.maxRepeatability.enableIgnoreErrors();
    this.horizontalOverlap.enableIgnoreErrors();
    this.verticalOverlap.enableIgnoreErrors();
    this.overlapUncertainty.enableIgnoreErrors();
    this.parallelOptions.enableLoadingParams();

  }

  @Override
  public void disableLoadingParams() {
    this.loadingParams = false;
    this.numFFTPeaks.disableIgnoreErrors();
    this.maxRepeatability.disableIgnoreErrors();
    this.horizontalOverlap.disableIgnoreErrors();
    this.verticalOverlap.disableIgnoreErrors();
    this.overlapUncertainty.disableIgnoreErrors();
    this.parallelOptions.disableLoadingParams();
  }

  @Override
  public boolean isLoadingParams() {
    return this.loadingParams;
  }

  @Override
  public void saveParamsFromGUI(StitchingAppParams params, boolean isClosing) {
    params.getAdvancedParams().setRepeatability(this.maxRepeatability.getValue());
    params.getAdvancedParams().setHorizontalOverlap(this.horizontalOverlap.getValue());
    params.getAdvancedParams().setVerticalOverlap(this.verticalOverlap.getValue());
    params.getAdvancedParams().setNumFFTPeaks(this.numFFTPeaks.getValue());
    params.getAdvancedParams().setOverlapUncertainty(this.overlapUncertainty.getValue());
    params.getAdvancedParams().setUseDoublePrecision(this.useDoublePrecision.isSelected());
    params.getAdvancedParams().setUseBioFormats(this.useBioFormats.isSelected());
    params.getAdvancedParams().setSuppressModalWarningDialog(this.suppressModalWarningDialog.isSelected());
    params.getAdvancedParams().setTranslationRefinementType(this.getTransRefinementType());

    int val = this.numTransRefineHillClimbs.getValue();
    if (val > 0) // if empty, leave as the default value
      params.getAdvancedParams().setNumTranslationRefinementStartPoints(val);


    Log.LogType logLevel = this.getLogLevel();
    Debug.DebugType debugLevel = this.getDebugLevel();

    params.getLogParams().setLogLevel(logLevel);
    params.getLogParams().setDebugLevel(debugLevel);

    this.parallelOptions.saveParamsFromGUI(params, isClosing);
  }

  /**
   * Gets the log level
   *
   * @return the log level
   */
  public Log.LogType getLogLevel() {
    return (Log.LogType) this.loggingLevel.getSelectedItem();
  }

  /**
   * Gets the debug level
   *
   * @return the debug level
   */
  public Debug.DebugType getDebugLevel() {
    return (Debug.DebugType) this.debugLevel.getSelectedItem();
  }

  public Stitching.TranslationRefinementType getTransRefinementType() {
    return (Stitching.TranslationRefinementType) this.translationRefinementType.getSelectedItem();
  }


  @Override
  public void actionPerformed(ActionEvent arg0) {
    Object src = arg0.getSource();
    if (src instanceof JComboBox) {
      JComboBox action = (JComboBox) src;

      if (action.equals(this.loggingLevel)) {
        Log.setLogLevel((Log.LogType) action.getSelectedItem());
      } else if (action.equals(this.debugLevel)) {
        Debug.setDebugLevel((Debug.DebugType) action.getSelectedItem());
      } else if (action.equals(this.translationRefinementType)) {

        if (action.getSelectedItem().equals(Stitching.TranslationRefinementType
            .MULTI_POINT_HILL_CLIMB)) {
          this.numTransRefineHillClimbs.setEnabled(true);
        } else {
          this.numTransRefineHillClimbs.setEnabled(false);
        }
      }
    }
  }


}
