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


// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 12:37:30 PM EST
//
// Time-stamp: <Apr 18, 2014 12:37:30 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.gui.components.textfield.textFieldModel;

import gov.nist.isg.mist.stitching.gui.panels.outputTab.OutputPanel;

/**
 * Validator that checks values based on a regex
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class RegexResetImageSizeModel extends RegexModel {

  private OutputPanel outputPanel;

  /**
   * Creates a regex validator based on a given regex
   *
   * @param regex          the regex to check
   * @param errorText      the error text associated with this validator
   * @param outputPanel    the output panel to reset image size
   * @param invalidStrings the invalid strings for this validator
   */
  public RegexResetImageSizeModel(String regex, String errorText, OutputPanel outputPanel, String... invalidStrings) {
    super(regex, errorText, invalidStrings);
    this.outputPanel = outputPanel;
  }

  @Override
  public boolean validateText(String val) {
    if (super.validateText(val)) {
      this.outputPanel.resetImageDimensions();
      return true;
    }
    return false;

  }

}
