// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: May 16, 2014 3:51:00 PM EST
//
// Time-stamp: <May 16, 2014 3:51:00 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui.panels.advancedTab.parallelPanels;

import javax.swing.JPanel;

import gov.nist.isg.mist.gui.components.textfield.TextFieldInputPanel;
import gov.nist.isg.mist.gui.components.textfield.textFieldModel.IntModel;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.gui.params.interfaces.GUIParamFunctions;

/**
 * Cretes a panel to select Java options
 *
 * @author Tim Blattner
 * @version 1.0
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
            new IntModel(1, numProc));
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
    return getNumCPUThreads() >= 1;
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
