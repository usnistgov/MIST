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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validator that checks values based on a two regex
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class DualRegexModel extends InvalidTextModel {

  private Pattern pattern1;
  private Pattern pattern2;

  /**
   * Creates a regex validator based on a given regex
   *
   * @param regex1         the first regex to check
   * @param regex2         the second regex to check
   * @param errorText      the error text associated with this validator
   * @param invalidStrings the invalid strings for this validator
   */
  public DualRegexModel(String regex1, String regex2, String errorText, String... invalidStrings) {
    super(errorText, invalidStrings);
    this.pattern1 = Pattern.compile(regex1);
    this.pattern2 = Pattern.compile(regex2);
  }

  @Override
  public boolean validateText(String val) {
    if (!super.validateText(val))
      return false;

    Matcher matcher1 = this.pattern1.matcher(val);
    Matcher matcher2 = this.pattern2.matcher(val);
    if (!matcher1.find() || matcher1.groupCount() != 3 || !matcher2.find()
        || matcher2.groupCount() != 3) {
      return false;
    }
    return true;
  }

}
