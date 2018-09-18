// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 12:53:27 PM EST
//
// Time-stamp: <Apr 18, 2014 12:53:27 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui.panels.subgrid;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import gov.nist.isg.mist.gui.components.helpDialog.HelpDocumentationViewer;
import gov.nist.isg.mist.gui.components.textfield.TextFieldInputPanel;
import gov.nist.isg.mist.gui.components.textfield.textFieldModel.CheckSubGridAndFileSizeModel;
import gov.nist.isg.mist.gui.components.textfield.textFieldModel.CheckSubGridOneBasedModel;
import gov.nist.isg.mist.gui.images.AppImageHelper;
import gov.nist.isg.mist.gui.panels.inputTab.InputPanel;
import gov.nist.isg.mist.gui.panels.outputTab.OutputPanel;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.gui.params.interfaces.GUIParamFunctions;

/**
 * Creates the advanced options panel
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class SubgridPanel extends JPanel implements GUIParamFunctions {

  private static final long serialVersionUID = 1L;

  private JCheckBox useFullGrid = new JCheckBox("Use full grid");

  private TextFieldInputPanel<Integer> startRow;
  private TextFieldInputPanel<Integer> startCol;
  private TextFieldInputPanel<Integer> extentWidth;
  private TextFieldInputPanel<Integer> extentHeight;
  private JLabel picLabel;
  private InputPanel inputPanel;

  private boolean changingValues;

  /**
   * Initializes the advanced options panel
   *
   * @param outputPanel the output panel to reference for image size
   */
  public SubgridPanel(OutputPanel outputPanel) {


    this.useFullGrid.setSelected(true);

    this.startRow =
        new TextFieldInputPanel<Integer>("Start Row", "1", new CheckSubGridOneBasedModel(1,
            Integer.MAX_VALUE, this));
    this.startCol =
        new TextFieldInputPanel<Integer>("Start Col", "1", new CheckSubGridOneBasedModel(1,
            Integer.MAX_VALUE, this));
    this.extentWidth =
        new TextFieldInputPanel<Integer>("Extent Width", "4", new CheckSubGridAndFileSizeModel(
            1, Integer.MAX_VALUE, this, outputPanel));
    this.extentHeight =
        new TextFieldInputPanel<Integer>("Extent Height", "4",
            new CheckSubGridAndFileSizeModel(1, Integer.MAX_VALUE, this, outputPanel));

    ImageIcon icon = null;
    try {
      icon = AppImageHelper.loadImage("Subgrid.png", 0.8);
    } catch (FileNotFoundException e) {
    } catch (IOException e1) {
    }

    if (icon != null)
      this.picLabel = new JLabel(icon);
    else
      this.picLabel = new JLabel();

    setFocusable(false);

    initControls();

    this.changingValues = false;

    this.useFullGrid.addItemListener(new ItemListener() {

      @Override
      public void itemStateChanged(ItemEvent e) {
        updateSubGrid();
      }
    });

  }

  /**
   * Sets the reference to the InputPanel
   *
   * @param inputPanel the input panel that we wish to reference
   */
  public void setInputPanel(InputPanel inputPanel) {
    this.inputPanel = inputPanel;
  }

  private void initControls() {
    JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    JPanel vertPanel = new JPanel(new GridBagLayout());

    GridBagConstraints c = new GridBagConstraints();

    JButton qButton = new JButton("Help?");
    qButton.addActionListener(new HelpDocumentationViewer("subgrid-parameters"));

    c.anchor = GridBagConstraints.NORTHEAST;
    c.gridy = 0;
    vertPanel.add(qButton, c);

    c.anchor = GridBagConstraints.LINE_START;
    c.gridy = 1;
    c.fill = GridBagConstraints.HORIZONTAL;

    vertPanel.add(this.useFullGrid, c);

    JPanel rowColPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    rowColPanel.add(this.startCol);
    rowColPanel.add(this.startRow);

    c.gridy = 2;
    vertPanel.add(rowColPanel, c);

    JPanel widthHeightPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    widthHeightPanel.add(this.extentWidth);
    widthHeightPanel.add(this.extentHeight);

    c.gridy = 3;
    vertPanel.add(widthHeightPanel, c);

    c.gridy = 4;
    vertPanel.add(this.picLabel, c);

    mainPanel.add(vertPanel);

    add(mainPanel);


  }

  /**
   * Updates the sub-grid, which is located in the inputPanel
   */
  public void updateSubGrid() {

    if (this.useFullGrid.isSelected()) {
      // disable the subgrid width height start row and start column
      this.startRow.setEnabled(false);
      this.startCol.setEnabled(false);
      this.extentHeight.setEnabled(false);
      this.extentWidth.setEnabled(false);


      this.changingValues = true;

      int plateWidth = this.inputPanel.getPlateWidth();
      int plateHeight = this.inputPanel.getPlateHeight();

      this.startRow.setValue(1);
      this.startCol.setValue(1);

      this.extentWidth.setValue(plateWidth);
      this.extentHeight.setValue(plateHeight);

      this.changingValues = false;
    } else {
      // enable the subgrid width height start row and start column
      this.startRow.setEnabled(true);
      this.startCol.setEnabled(true);
      this.extentHeight.setEnabled(true);
      this.extentWidth.setEnabled(true);

      if (isValidSubGrid()) {
        this.hideSubGridError();
      } else {
        this.showSubGridError();
      }
    }
  }

  /**
   * Sets the check box to use the full grid
   *
   * @param val true to check the full grid checkbox
   */
  public void setUseFullGrid(boolean val) {
    this.useFullGrid.setSelected(val);
  }

  /**
   * Gets the error text for the sub-grid
   *
   * @return the error text associated with the subgrid panel
   */
  public String getErrorText() {
    String txt = "<html>Sub Grid is out of bounds:<br>";

    txt +=
        "Extent Width + Start Col &lt= Plate Width (" + getExtentWidth() + "+" + getStartCol()
            + "&lt=" + this.inputPanel.getPlateWidth() + ") <br>";
    txt +=
        "Extent Height + Start Row &lt= Plate Height (" + getExtentHeight() + "+" + getStartRow()
            + "&lt=" + this.inputPanel.getPlateHeight() + ") <br>";
    txt += "</html>";

    return txt;
  }

  /**
   * Checks if the sub-grid is valid or not
   *
   * @return true if the grid is valid otherwise false
   */
  public boolean isValidSubGrid() {
    if (this.changingValues)
      return true;

    int plateWidth = this.inputPanel.getPlateWidth();
    int plateHeight = this.inputPanel.getPlateHeight();
    int startRow = this.getStartRow();
    int startCol = this.getStartCol();
    int extentWidth = this.getExtentWidth();
    int extentHeight = this.getExtentHeight();

    if (plateWidth < 1 || plateHeight < 1 || startRow < 0 || startCol < 0 || extentWidth < 1
        || extentHeight < 1) {
      showSubGridError();
      return false;
    }

    if (extentWidth + startCol > plateWidth) {
      this.extentWidth.showError();
      this.startCol.showError();
      return false;
    }

    if (extentHeight + startRow > plateHeight) {
      this.extentHeight.showError();
      this.startRow.showError();
      return false;
    }

    hideSubGridError();
    return true;
  }

//  /**
//   * Checks to see if the user parameters are using the full grid options or not. If they are not,
//   * then it will uncheck use full grid
//   */
//  public void checkUseFullGrid() {
//    if (this.startRow.getValue() != 0 || this.startCol.getValue() != 0
//        || this.extentWidth.getValue() != this.inputPanel.getPlateWidth()
//        || this.extentHeight.getValue() != this.inputPanel.getPlateHeight()) {
//      setUseFullGrid(false);
//    } else {
//      setUseFullGrid(true);
//    }
//  }

  /**
   * Displays errors for the sub-grid options
   */
  public void showSubGridError() {
    this.startCol.showError();
    this.startRow.showError();
    this.extentWidth.showError();
    this.extentHeight.showError();
  }

  /**
   * Hides errors for the sub-grid options
   */
  public void hideSubGridError() {
    this.startCol.hideError();
    this.startRow.hideError();
    this.extentWidth.hideError();
    this.extentHeight.hideError();
  }


  /**
   * Gets the start row for the sub-grid
   *
   * @return the start row
   */
  public int getStartRow() {
    return this.startRow.getValue();
  }

  /**
   * Gets the start column for the sub-grid
   *
   * @return the start column
   */
  public int getStartCol() {
    return this.startCol.getValue();
  }

  /**
   * Gets the extend width for the sub-grid
   *
   * @return the width of the sub-grid
   */
  public int getExtentWidth() {
    return this.extentWidth.getValue();
  }

  /**
   * Gets the extent height for the sub-grid
   *
   * @return the height of the sub-grid
   */
  public int getExtentHeight() {
    return this.extentHeight.getValue();
  }

  /**
   * Gets the input panel tab
   *
   * @return the input panel tab
   */
  public InputPanel getInputPanel() {
    return this.inputPanel;
  }

  @Override
  public void loadParamsIntoGUI(StitchingAppParams params) {
    this.changingValues = true;
    this.startRow.setValue(params.getInputParams().getStartRow() + 1);
    this.startCol.setValue(params.getInputParams().getStartCol() + 1);
    this.extentWidth.setValue(params.getInputParams().getExtentWidth());
    this.extentHeight.setValue(params.getInputParams().getExtentHeight());

    if (this.startRow.getValue() != 0 || this.startCol.getValue() != 0
        || this.extentWidth.getValue() != this.inputPanel.getPlateWidth()
        || this.extentHeight.getValue() != this.inputPanel.getPlateHeight()) {
      setUseFullGrid(false);
    }

    updateSubGrid();

    this.changingValues = false;

  }

  @Override
  public boolean checkAndParseGUI(StitchingAppParams params) {
    if (checkGUIArgs()) {
      saveParamsFromGUI(params, false);
      return true;
    }
    return false;
  }

  @Override
  public boolean checkGUIArgs() {
    return isValidSubGrid();
  }

  private boolean loadingParams = false;

  @Override
  public void enableLoadingParams() {
    this.loadingParams = true;
    this.startRow.enableIgnoreErrors();
    this.startCol.enableIgnoreErrors();
    this.extentWidth.enableIgnoreErrors();
    this.extentHeight.enableIgnoreErrors();
  }

  @Override
  public void disableLoadingParams() {
    this.loadingParams = false;
    this.startRow.disableIgnoreErrors();
    this.startCol.disableIgnoreErrors();
    this.extentWidth.disableIgnoreErrors();
    this.extentHeight.disableIgnoreErrors();
  }

  @Override
  public boolean isLoadingParams() {
    return this.loadingParams;
  }

  @Override
  public void saveParamsFromGUI(StitchingAppParams params, boolean isClosing) {
    params.getInputParams().setStartRow(this.startRow.getValue());
    params.getInputParams().setStartCol(this.startCol.getValue());
    params.getInputParams().setExtentWidth(this.extentWidth.getValue());
    params.getInputParams().setExtentHeight(this.extentHeight.getValue());
  }
}
