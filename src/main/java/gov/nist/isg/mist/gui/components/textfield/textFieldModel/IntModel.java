// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 12:35:55 PM EST
//
// Time-stamp: <Apr 18, 2014 12:35:55 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui.components.textfield.textFieldModel;

/**
 * Validator that checks integer values
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class IntModel implements TextFieldModel<Integer> {

  private boolean allowEmpty;

  private int min;
  private int max;

  private String errorText;

  /**
   * Creates an integer validator that allows any integer
   */
  public IntModel() {
    this.allowEmpty = false;
    this.min = Integer.MIN_VALUE;
    this.max = Integer.MAX_VALUE;
    this.errorText =
        "<html>Please only enter numbers in the text field.<br>" + "Must be any integer.</html?";
  }

  /**
   * Creates an integer validator that checks in bounds by min (inclusive) and max (inclusive)
   *
   * @param min the minimum value that is valid (inclusive)
   * @param max the maximum value that is valid (inclusive)
   */
  public IntModel(int min, int max) {
    this.allowEmpty = false;
    this.min = min;
    this.max = max;
    this.errorText =
        "<html>Please only enter numbers in the text field.<br>"
            + "Must be greater than or equal to " + min + " and less than or equal to " + max
            + "</html?";
  }

  /**
   * Creates an integer validator that checks in bounds by min (inclusive) and max (inclusive)
   *
   * @param min        the minimum value that is valid (inclusive)
   * @param max        the maximum value that is valid (inclusive)
   * @param allowEmpty enables this validator to accept empty text. Empty text represents a value of
   *                   0
   */
  public IntModel(int min, int max, boolean allowEmpty) {
    this.allowEmpty = allowEmpty;
    this.min = min;
    this.max = max;
    this.errorText =
        "<html>Please only enter numbers in the text field.<br>"
            + "Must be greater than or equal to " + min + " and less than or equal to " + max
            + "<br>empty values enforces use of default parameters.</html>";
  }


  @Override
  public boolean validateText(String val) {
    if (val.equals(""))
      return this.allowEmpty;

    try {
      int test = Integer.parseInt(val);
      return !(test < this.min || test > this.max);
    } catch (NumberFormatException e) {
      return false;
    }

  }

  @Override
  public String getErrorMessage() {
    return this.errorText;
  }

  @Override
  public Integer getModelValue(String val) {
    if (val.equals(""))
      return 0;
    try {
      return Integer.parseInt(val);
    } catch (NumberFormatException ex) {
      return this.min;
    }
  }

  @Override
  public String setModelValue(String val) {
    int value = getModelValue(val);

    if (value == 0) {
      if (this.allowEmpty)
        return "";
    }

    return Integer.toString(value);
  }

  @Override
  public void updateTextFields() {
  }

}
