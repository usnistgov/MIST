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
// Date: May 16, 2014 3:51:00 PM EST
//
// Time-stamp: <May 16, 2014 3:51:00 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.gui.panels.advancedTab.parallelPanels;

import gov.nist.isg.mist.stitching.gui.components.textfield.TextFieldInputPanel;
import gov.nist.isg.mist.stitching.gui.components.textfield.textFieldModel.IntModel;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.gui.params.interfaces.GUIParamFunctions;

import javax.swing.*;

/**
 * Cretes a panel to select Java options
 * 
 * @author Tim Blattner
 * @version 1.0
 * 
 */
public class JavaPanel extends JPanel implements GUIParamFunctions {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private TextFieldInputPanel<Integer> numThreadsCPU;


  /**
   * Initializes the Java panel
   */
  public JavaPanel() {

    int numProc = Runtime.getRuntime().availableProcessors();
    this.numThreadsCPU =
        new TextFieldInputPanel<Integer>("CPU worker threads", Integer.toString(numProc),
            new IntModel(1, 2 * numProc));
    initControls();
  }

  private void initControls() {
    add(this.numThreadsCPU);
  }

  /**
   * Get the number of CPU threads
   * 
   * @return the number of CPU threads
   */
  public int getNumCPUThreads() {
    return this.numThreadsCPU.getValue();
  }


  @Override
  public void loadParamsIntoGUI(StitchingAppParams params) {
    this.numThreadsCPU.setValue(params.getAdvancedParams().getNumCPUThreads());
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
    int val = getNumCPUThreads();

    if (val < 1)
      return false;

    return true;
  }

  private boolean loadingParams = false;

  @Override
  public void enableLoadingParams() {
    this.loadingParams = true;
    this.numThreadsCPU.enableIgnoreErrors();
  }

  @Override
  public void disableLoadingParams() {
    this.loadingParams = false;
    this.numThreadsCPU.disableIgnoreErrors();
  }

  @Override
  public boolean isLoadingParams() {
    return this.loadingParams;
  }

  @Override
  public void saveParamsFromGUI(StitchingAppParams params, boolean isClosing) {
    int val = getNumCPUThreads();
    params.getAdvancedParams().setNumCPUThreads(val);
  }
}
