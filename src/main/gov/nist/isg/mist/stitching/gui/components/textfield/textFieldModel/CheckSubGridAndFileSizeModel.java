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
// Date: Apr 18, 2014 12:38:18 PM EST
//
// Time-stamp: <Apr 18, 2014 12:38:18 PM tjb3>
//
//
// ================================================================

package main.gov.nist.isg.mist.stitching.gui.components.textfield.textFieldModel;

import main.gov.nist.isg.mist.stitching.gui.panels.outputTab.OutputPanel;
import main.gov.nist.isg.mist.stitching.gui.panels.subgrid.SubgridPanel;

/**
 * Validator that checks integers and if valid updates a referenced sub-grid
 * 
 * @author Tim Blattner
 * @version 1.0
 * 
 */
public class CheckSubGridAndFileSizeModel extends CheckSubGridModel {

  private OutputPanel outputPanel;
  private SubgridPanel subgridPanel;

  /**
   * Creates a validator that updates the sub-grid when the check passes
   * 
   * @param subgridPanel the panel where the sub-grid exists
   * @param outputPanel the output panel to reference the file size
   */
  public CheckSubGridAndFileSizeModel(SubgridPanel subgridPanel, OutputPanel outputPanel) {
    super(subgridPanel);
    this.outputPanel = outputPanel;
    this.subgridPanel = subgridPanel;
  }

  /**
   * Creates a validator that updates the sub-grid when the check passes
   * 
   * @param min the minimum value that is valid (inclusive)
   * @param max the maximum value that is valid (inclusive)
   * @param subgridPanel the panel where the subgrid exists
   * @param outputPanel the output panel to reference the file size
   */
  public CheckSubGridAndFileSizeModel(int min, int max, SubgridPanel subgridPanel,
      OutputPanel outputPanel) {
    super(min, max, subgridPanel);
    this.outputPanel = outputPanel;
    this.subgridPanel = subgridPanel;

  }

  @Override
  public boolean validateText(String val) {
    if (super.validateText(val)) {
      this.outputPanel.updateFileSize(this.subgridPanel.getExtentHeight() * this.subgridPanel.getExtentWidth());
      return true;
    }
    this.outputPanel.updateFileSize(0);
    return false;
  }

}
