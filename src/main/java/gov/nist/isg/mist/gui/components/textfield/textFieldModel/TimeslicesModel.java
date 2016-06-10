// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Jul 2, 2014 11:22:21 AM EST
//
// Time-stamp: <Jul 2, 2014 11:22:21 AM tjb3>
//
//
// ================================================================
package gov.nist.isg.mist.gui.components.textfield.textFieldModel;

import java.util.List;

import gov.nist.isg.mist.gui.params.objects.RangeParam;

/**
 * Validator that checks timeslice format
 *
 * @author Tim Blattner
 * @version 1.0
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
