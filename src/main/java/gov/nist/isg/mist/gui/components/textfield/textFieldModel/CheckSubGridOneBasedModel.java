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
 * plate. Also converts the values to enforce 1-based indexing. When getting the value, it will
 * convert the value from 1-based to 0-based, and when setting the value it converts from 0-based to
 * 1-based.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class CheckSubGridOneBasedModel extends CheckSubGridModel {

  /**
   * Creates a sub grid validator.
   *
   * @param advPanel the panel where the subgrid exists
   */
  public CheckSubGridOneBasedModel(SubgridPanel advPanel) {
    super(advPanel);
  }

  /**
   * Checks a sub grid validator, also keeping the value in bounds by min (inclusive) and max
   * (inclusive)
   *
   * @param min      the minimum value that is valid (inclusive)
   * @param max      the maximum value that is valid (inclusive)
   * @param advPanel the panel where the subgrid exists
   */
  public CheckSubGridOneBasedModel(int min, int max, SubgridPanel advPanel) {
    super(min, max, advPanel);
  }

  @Override
  public Integer getModelValue(String val) {
    int value = super.getModelValue(val);

    return value - 1;

  }


  @Override
  public String setModelValue(String val) {
    String valueStr = super.setModelValue(val);

    int zeroBasedVal = Integer.parseInt(valueStr);

    return Integer.toString(zeroBasedVal + 1);
  }

}
