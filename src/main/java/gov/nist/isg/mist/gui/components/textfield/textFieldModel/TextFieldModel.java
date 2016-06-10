// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 12:28:13 PM EST
//
// Time-stamp: <Apr 18, 2014 12:28:13 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui.components.textfield.textFieldModel;

/**
 * Validator is an interface that handles text validation for text fields. Functions for testing the
 * text, getting the values, and getting the error text associated with the validation are
 * provided.
 *
 * @param <T> the type of Object for validation
 * @author Tim Blattner
 * @version 1.0
 */
public interface TextFieldModel<T> {
  /**
   * Tests the text using a specific validator
   *
   * @param val the text you wish to test
   * @return true if the the text is valid
   */
  public boolean validateText(String val);

  /**
   * Gets the error message associated with this validator
   *
   * @return the error message
   */
  public String getErrorMessage();

  /**
   * Gets the value of the text based on the validation type
   *
   * @param val the value you wish to parse
   * @return the value parsed by the validator
   */
  public T getModelValue(String val);

  /**
   * Gets the value of the text based on the validation type. The value that is returned represents
   * the value that is to be used in the text
   *
   * @param val the value you wish to parse
   * @return the value that is to be placed inside the text field
   */
  public String setModelValue(String val);

  /**
   * Updates the view based on the model
   */
  public void updateTextFields();
}
