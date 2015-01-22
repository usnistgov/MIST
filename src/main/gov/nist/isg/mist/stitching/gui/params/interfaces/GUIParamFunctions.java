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
// Date: Apr 18, 2014 1:05:00 PM EST
//
// Time-stamp: <Apr 18, 2014 1:05:00 PM tjb3>
//
//
// ================================================================

package main.gov.nist.isg.mist.stitching.gui.params.interfaces;

import main.gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;

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
   * @param params the params that are to be loaded from the GUI and saved
   * @param isClosing true if the application is being closed, otherwise false
   */
  public void saveParamsFromGUI(StitchingAppParams params, boolean isClosing);

}
