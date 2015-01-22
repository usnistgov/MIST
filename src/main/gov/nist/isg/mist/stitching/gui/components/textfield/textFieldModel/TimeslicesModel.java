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
// Date: Jul 2, 2014 11:22:21 AM EST
//
// Time-stamp: <Jul 2, 2014 11:22:21 AM tjb3>
//
//
// ================================================================
package main.gov.nist.isg.mist.stitching.gui.components.textfield.textFieldModel;

import java.util.List;

import main.gov.nist.isg.mist.stitching.gui.params.objects.RangeParam;

/**
 * Validator that checks timeslice format
 * 
 * @author Tim Blattner
 * @version 1.0
 * 
 */
public class TimeslicesModel implements TextFieldModel<List<RangeParam>> {

  @Override
  public boolean validateText(String val) {
    if (val.equals(""))
      return true;

    String[] valSplit = val.split(",");

    for (String v : valSplit) {
      RangeParam param = RangeParam.parseParam(v);
      if (param == null)
        return false;
    }
    return true;
  }

  @Override
  public String getErrorMessage() {
    return "<html>Enter time slices specifying range: e.g. 1-5,8,11-13"
        + "<br>To specify all time slices leave field blank</html>";
  }

  @Override
  public List<RangeParam> getModelValue(String val) {
    return RangeParam.parseTimeSlices(val);
  }

  @Override
  public String setModelValue(String val) {
    return val;
  }

  @Override
  public void updateTextFields() {    
  }

}
