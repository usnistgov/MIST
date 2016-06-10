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
// Date: Apr 18, 2014 1:03:11 PM EST
//
// Time-stamp: <Apr 18, 2014 1:03:11 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui.panels.outputTab;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import gov.nist.isg.mist.gui.components.filechooser.DirectoryChooserPanel;
import gov.nist.isg.mist.gui.components.helpDialog.HelpDocumentationViewer;
import gov.nist.isg.mist.gui.components.textfield.TextFieldInputPanel;
import gov.nist.isg.mist.gui.components.textfield.textFieldModel.DblModel;
import gov.nist.isg.mist.gui.components.textfield.textFieldModel.FilenameModel;
import gov.nist.isg.mist.gui.panels.inputTab.InputPanel;
import gov.nist.isg.mist.gui.panels.subgrid.SubgridPanel;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.gui.params.interfaces.GUIParamFunctions;
import gov.nist.isg.mist.stitching.lib.export.LargeImageExporter.BlendingMode;

/**
 * Creates the output panel
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class OutputPanel extends JPanel implements GUIParamFunctions, DocumentListener, ActionListener {

  private static final String fileSizeLabel = "Est. individual stitched image size (0% overlap): ";

  private static final String filenamePrefixHelpText = "The prefix prepended to each file saved in "
      + "the output directory. \n\nMIST will query for confirmation before overwriting any files.";


  private static final long serialVersionUID = 1L;

  private boolean makingChanges;
  private JCheckBox useImageDirectory;
  private DirectoryChooserPanel outputPath;
  private JCheckBox displayStitching;
  private JCheckBox outputFullImage;


  private TextFieldInputPanel<String> filePrefixName;
  private JComboBox blendingType = new JComboBox(BlendingMode.values());
  private TextFieldInputPanel<Double> blendingAlpha;

  private JTextField imageDirectory;

  private JLabel estimatedFileSizeLabel;
  private JButton updateBtn;

  private InputPanel inputPanel;
  private SubgridPanel subGridPanel;

  private int imageSize;

  /**
   * Creates the output panel
   */
  public OutputPanel() {
    this.makingChanges = false;
    setFocusable(false);

    initControls();
    this.imageSize = 0;
  }

  /**
   * Sets the reference to the input panel
   *
   * @param inputPanel the input panel to reference
   */
  public void setInputPanel(InputPanel inputPanel) {
    this.inputPanel = inputPanel;
    this.imageDirectory = inputPanel.getDirectoryChooserPanel().getInputField();
    this.imageDirectory.getDocument().addDocumentListener(this);
  }

  /**
   * Sets the reference to the subgrid panel
   *
   * @param subGridPanel the subgrid panel to reference
   */
  public void setSubGridPanel(SubgridPanel subGridPanel) {
    this.subGridPanel = subGridPanel;
  }

  public DirectoryChooserPanel getOutputPath() {
    return this.outputPath;
  }

  private void initControls() {

    this.useImageDirectory = new JCheckBox("Use Image Directory as Output Directory");
    this.outputPath = new DirectoryChooserPanel("Output Directory");
    this.displayStitching = new JCheckBox("Display Stitched Image");
    this.outputFullImage = new JCheckBox("Save Full Stitched Image");

    this.displayStitching.setToolTipText("<html>Displays stitched image in Fiji window"
        + "<br>Warning: Image will load into RAM to display.</html>");
    this.outputFullImage.setToolTipText("<html>Saves stitched image to output directory"
        + "<br>Warning: Virtual image will load into RAM to save.</html>");
    this.blendingAlpha =
        new TextFieldInputPanel<Double>("Alpha", "", 4, new DblModel(0.0, 10.0, true));


    this.estimatedFileSizeLabel = new JLabel(fileSizeLabel + "??");
    this.useImageDirectory.setSelected(true);
    this.displayStitching.setSelected(true);
    this.outputFullImage.setSelected(false);


    JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    JPanel vertPanel = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();

    JPanel checkBoxPanel = new JPanel(new GridLayout(1, 2));
    checkBoxPanel.add(this.displayStitching);
    checkBoxPanel.add(this.outputFullImage);

    this.filePrefixName =
        new TextFieldInputPanel<String>("Filename Prefix", "out-file-", 20, new FilenameModel(
            "Prefix for output filenames. Must be valid file name"), filenamePrefixHelpText);


    JPanel blendingPanel = new JPanel();
    blendingPanel.add(new JLabel("Blending mode: "));
    blendingPanel.add(this.blendingType);
    this.blendingType.addActionListener(this);
    blendingPanel.add(this.blendingAlpha);

    String blendingTooltipText = "<html>Blending mode:";

    for (BlendingMode b : BlendingMode.values()) {
      blendingTooltipText += "<br>" + b.name() + ": " + b.getToolTipText();
    }

    blendingTooltipText += "</html>";
    this.blendingType.setToolTipText(blendingTooltipText);


    // setup the Output Folder panel
    JPanel outputFolderPanel = new JPanel(new GridBagLayout());
    outputFolderPanel.setBorder(
        new TitledBorder(new LineBorder(Color.BLACK), "Output Folder"));
    c.gridy = 0;
    c.gridx = 0;
    c.gridwidth = 1;
    c.anchor = GridBagConstraints.LINE_START;
    outputFolderPanel.add(this.useImageDirectory, c);
    c.gridy = 1;
    outputFolderPanel.add(this.outputPath, c);
    c.gridy = 2;
    outputFolderPanel.add(this.filePrefixName, c);


    // setup the estimated stitched image size panel
    JPanel estimatedFileSizePanel = new JPanel();
    this.updateBtn = new JButton("Update");
    this.updateBtn.addActionListener(this);

    estimatedFileSizePanel.add(this.estimatedFileSizeLabel);
    estimatedFileSizePanel.add(this.updateBtn);


    // setup the Stitched Image Panel
    JPanel stitchedImagePanel = new JPanel(new GridBagLayout());
    stitchedImagePanel.setBorder(
        new TitledBorder(new LineBorder(Color.BLACK), "Stitched Image"));
    c.gridy = 0;
    c.gridx = 0;
    c.gridwidth = 1;
    c.anchor = GridBagConstraints.LINE_START;
    stitchedImagePanel.add(blendingPanel, c);
    c.gridy = 1;
    stitchedImagePanel.add(checkBoxPanel, c);
    c.gridy = 2;
    stitchedImagePanel.add(estimatedFileSizePanel, c);


    JButton qButton = new JButton("Help?");
    qButton.addActionListener(new HelpDocumentationViewer("output-parameters"));
    c.anchor = GridBagConstraints.NORTHEAST;
    c.gridy = 0;
    vertPanel.add(qButton, c);


    c.insets = new Insets(20, 0, 0, 0);
    c.anchor = GridBagConstraints.CENTER;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridy = 1;
    vertPanel.add(outputFolderPanel, c);
    c.insets = new Insets(30, 0, 0, 0);
    c.gridy = 2;
    vertPanel.add(stitchedImagePanel, c);


    mainPanel.add(vertPanel);

    add(mainPanel);

    this.useImageDirectory.addActionListener(this);
    this.outputPath.getInputField().getDocument().addDocumentListener(this);
  }


  public String getPrefix() {
    return this.filePrefixName.getValue();
  }


  /**
   * Resets the image dimensions that is shown in the output panel
   */
  public void resetImageDimensions() {
    this.imageSize = 0;
    updateFileSize(this.subGridPanel.getExtentHeight() * this.subGridPanel.getExtentWidth());
  }

  /**
   * Updates the image dimensions in the output panel
   */
  public void updateImageDimensions() {
    this.imageSize = this.inputPanel.getInitialImageFileSize();
    updateFileSize(this.subGridPanel.getExtentHeight() * this.subGridPanel.getExtentWidth());
  }

  private void setAlphaEnabled(boolean val) {
    this.blendingAlpha.setEnabled(val);
  }

  /**
   * Updates the file size to show nice formatting (GB, MB, ... , etc.)
   *
   * @param size the file size to format
   */
  public void updateFileSize(int size) {
    double imgSize = this.imageSize;
    double fileSize = imgSize * size * 2.0;

    String unit = "KB";
    fileSize = fileSize / 1024.0;

    if (fileSize > 1024) {
      unit = "MB";
      fileSize = fileSize / 1024.0;
    }

    if (fileSize > 1024) {
      unit = "GB";
      fileSize = fileSize / 1024.0;
    }

    if (fileSize > 1024) {
      unit = "TB";
      fileSize = fileSize / 1024.0;
    }

    if (fileSize < 0) {
      unit = "Err";
      fileSize = 0;
    }

    DecimalFormat df = new DecimalFormat("#.##");

    this.estimatedFileSizeLabel.setText(fileSizeLabel + df.format(fileSize) + " " + unit);
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == this.useImageDirectory) {

      if (!this.loadingParams) {
        if (this.useImageDirectory.isSelected()) {
          // Reference image directory
          this.outputPath.setValue(this.imageDirectory.getText());
        }
      }
    } else if (e.getSource() == this.updateBtn) {
      updateImageDimensions();
    } else if (e.getSource() == this.blendingType) {
      BlendingMode blendType = (BlendingMode) this.blendingType.getSelectedItem();
      setAlphaEnabled(blendType.isRequiresAlpha());
    }
  }

  private void processDocumentEvent(DocumentEvent e) {
    if (!this.loadingParams) {
      if (!this.makingChanges) {
        // Image directory changed
        if (e.getDocument() == this.imageDirectory.getDocument()) {
          if (this.useImageDirectory.isSelected()) {
            this.outputPath.setValue(this.imageDirectory.getText());
          }
        }
        // Output path
        else if (e.getDocument() == this.outputPath.getInputField().getDocument()) {
          if (this.outputPath.getValue().equals(this.imageDirectory.getText())) {
            this.useImageDirectory.setSelected(true);
          } else {
            this.useImageDirectory.setSelected(false);
          }
        }
      }
    }
  }

  @Override
  public void changedUpdate(DocumentEvent e) {
    processDocumentEvent(e);
  }

  @Override
  public void insertUpdate(DocumentEvent e) {
    processDocumentEvent(e);
  }

  @Override
  public void removeUpdate(DocumentEvent e) {
    processDocumentEvent(e);
  }

  @Override
  public void loadParamsIntoGUI(StitchingAppParams params) {
    this.makingChanges = true;
    this.outputPath.setValue(params.getOutputParams().getOutputPath());
    this.displayStitching.setSelected(params.getOutputParams().isDisplayStitching());
    this.outputFullImage.setSelected(params.getOutputParams().isOutputFullImage());

    this.filePrefixName.setValue(params.getOutputParams().getOutFilePrefix());

    if (this.outputPath.getValue().equals(this.imageDirectory.getText())) {
      this.useImageDirectory.setSelected(true);
    } else {
      this.useImageDirectory.setSelected(false);
    }

    this.blendingType.setSelectedItem(params.getOutputParams().getBlendingMode());
    this.blendingAlpha.setValue(params.getOutputParams().getBlendingAlpha());

    this.makingChanges = false;
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
    return true;
  }

  private boolean loadingParams = false;

  @Override
  public void enableLoadingParams() {
    this.loadingParams = true;
  }

  @Override
  public void disableLoadingParams() {
    this.loadingParams = false;
  }

  @Override
  public boolean isLoadingParams() {
    return this.loadingParams;
  }

  @Override
  public void saveParamsFromGUI(StitchingAppParams params, boolean isClosing) {
    params.getOutputParams().setOutputPath(this.outputPath.getValue());
    params.getOutputParams().setDisplayStitching(this.displayStitching.isSelected());
    params.getOutputParams().setOutputFullImage(this.outputFullImage.isSelected());
    params.getOutputParams().setOutputMeta(true);
    params.getOutputParams().setOutFilePrefix(this.filePrefixName.getValue());
    params.getOutputParams().setBlendingMode((BlendingMode) this.blendingType.getSelectedItem());
    params.getOutputParams().setBlendingAlpha(this.blendingAlpha.getValue());
  }
}
