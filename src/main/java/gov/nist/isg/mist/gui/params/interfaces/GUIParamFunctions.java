// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 1:05:00 PM EST
//
// Time-stamp: <Apr 18, 2014 1:05:00 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui.params.interfaces;

import gov.nist.isg.mist.gui.params.StitchingAppParams;

/**
 * An interface that represents parameter functions
 *
 * @author Tim Blattner
 * @version 1.0
 */
public interface GUIParamFunctions {

  /**
   * Loads parameters into the GUI
   *
   * @param params the params to load
   */
  public void loadParamsIntoGUI(StitchingAppParams params);

  /**
   * Checks and parses the arguments in the GUI, setting the StitchingAppParams
   *
   * @param params the parameters that the GUI should set
   * @return true if the parameters were set
   */
  public boolean checkAndParseGUI(StitchingAppParams params);

  /**
   * Checks the GUI arguments
   *
   * @return true if the arguments passed
   */
  public boolean checkGUIArgs();

  /**
   * Enables flag to indicate that we are loading parameters
   */
  public void enableLoadingParams();

  /**
   * Disables flag to indicate that we are loading parameters
   */
  public void disableLoadingParams();

  /**
   * Checks if we are loading parameters
   *
   * @return true if we are laoding parameters
   */
  public boolean isLoadingParams();

  /**
   * Loads parameters from GUI into parameter object that is used to save the parameters to a file
   *
   * @param params    the params that are to be loaded from the GUI and saved
   * @param isClosing true if the application is being closed, otherwise false
   */
  public void saveParamsFromGUI(StitchingAppParams params, boolean isClosing);

}
