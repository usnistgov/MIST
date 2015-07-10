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
// Date: Apr 18, 2014 12:37:30 PM EST
//
// Time-stamp: <Apr 18, 2014 12:37:30 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.gui.components.textfield.textFieldModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validator that checks values based on a two regex
 * 
 * @author Tim Blattner
 * @version 1.0
 * 
 */
public class InvalidTextModel implements TextFieldModel<String> {

  private List<String> invalidText;
  private String errorText;

  /**
   * Creates a regex validator based on a given regex
   *
   * @param invalidStrings the array of invalid strings
   * @param errorText the error text associated with this validator
   */
  public InvalidTextModel(String errorText, String ... invalidStrings) {
    this.invalidText = Arrays.asList(invalidStrings);
    this.errorText = errorText;

    // update the error text to reflect that the invalid strings are invalid
    String extraErrorText = "<br><br>Invalid Character Sequences:";
    for(String str : invalidStrings)
      extraErrorText = extraErrorText + "<br>" + "\"" + str + "\"";

    extraErrorText = extraErrorText + "</html>";
    this.errorText = this.errorText.replace("</html>", extraErrorText);
  }

  @Override
  public boolean validateText(String val) {
    for (String s : this.invalidText)
      if (val.contains(s))
        return false;

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
