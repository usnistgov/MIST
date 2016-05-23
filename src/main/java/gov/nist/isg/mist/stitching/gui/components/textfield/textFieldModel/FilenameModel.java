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
// Date: Jul 2, 2014 11:20:29 AM EST
//
// Time-stamp: <Jul 2, 2014 11:20:29 AM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.gui.components.textfield.textFieldModel;

import java.io.File;
import java.io.IOException;

/**
 * Validator that checks for valid file name
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class FilenameModel implements TextFieldModel<String> {

  private String errorText;

  /**
   * Constructs the file name validator
   *
   * @param errorText the errtext associated with this validator
   */
  public FilenameModel(String errorText) {
    this.errorText = errorText;

  }

  @Override
  public boolean validateText(String val) {
    File testFile = new File(val);
    try {
      testFile.getCanonicalPath();
      testFile = null;
    } catch (IOException e) {
      return false;
    }

    return true;
  }

  @Override
  public String getErrorMessage() {
    return this.errorText;
  }

  @Override
  public String getModelValue(String val) {
    return val;
  }

  @Override
  public String setModelValue(String val) {
    return val;
  }

  @Override
  public void updateTextFields() {
  }

}
