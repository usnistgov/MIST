// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 12:31:32 PM EST
//
// Time-stamp: <Apr 18, 2014 12:31:32 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui.components.textfield.textFieldModel;

import gov.nist.isg.mist.gui.panels.subgrid.SubgridPanel;

/**
 * Validator that checks if the subgrid parameters are correct based on the width and height of a
 * plate.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class CheckSubGridModel extends IntModel {

  private SubgridPanel subgridPanel;

  /**
   * Creates a sub grid validator.
   *
   * @param advPanel the panel where the subgrid exists
   */
  public CheckSubGridModel(SubgridPanel advPanel) {
    this.subgridPanel = advPanel;
  }

  /**
   * Checks a sub grid validator, also keeping the value in bounds by min (inclusive) and max
   * (inclusive)
   *
   * @param min      the minimum value that is valid (inclusive)
   * @param max      the maximum value that is valid (inclusive)
   * @param advPanel the panel where the subgrid exists
   */
  public CheckSubGridModel(int min, int max, SubgridPanel advPanel) {
    super(min, max);
    this.subgridPanel = advPanel;
  }

  @Override
  public boolean validateText(String val) {
    if (super.validateText(val)) {
      if (this.subgridPanel.isValidSubGrid()) {
//        this.subgridPanel.checkUseFullGrid();
        return true;
      }
    }
    return false;

  }

}
