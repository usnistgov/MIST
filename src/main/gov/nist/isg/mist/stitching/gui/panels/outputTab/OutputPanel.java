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
// Date: Apr 18, 2014 1:03:11 PM EST
//
// Time-stamp: <Apr 18, 2014 1:03:11 PM tjb3>
//
//
// ================================================================

package main.gov.nist.isg.mist.stitching.gui.panels.outputTab;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import main.gov.nist.isg.mist.stitching.StitchingGUIFrame;
import main.gov.nist.isg.mist.stitching.gui.components.filechooser.DirectoryChooserPanel;
import main.gov.nist.isg.mist.stitching.gui.components.helpDialog.HelpDocumentationViewer;
import main.gov.nist.isg.mist.stitching.gui.components.textfield.TextFieldInputPanel;
import main.gov.nist.isg.mist.stitching.gui.components.textfield.textFieldModel.DblModel;
import main.gov.nist.isg.mist.stitching.gui.components.textfield.textFieldModel.FilenameModel;
import main.gov.nist.isg.mist.stitching.gui.panels.inputTab.InputPanel;
import main.gov.nist.isg.mist.stitching.gui.panels.subgrid.SubgridPanel;
import main.gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import main.gov.nist.isg.mist.stitching.gui.params.interfaces.GUIParamFunctions;
import main.gov.nist.isg.mist.stitching.lib.export.LargeImageExporter.BlendingMode;

/**
 * Creates the output panel
 * 
 * @author Tim Blattner
 * @version 1.0
 * 
 */
public class OutputPanel extends JPanel implements GUIParamFunctions, DocumentListener, ActionListener {

  private static final String fileSizeLabel = "Estimated stitched image file size (0% overlap): ";

  private static final long serialVersionUID = 1L;

  private boolean makingChanges;
  private DirectoryChooserPanel metadataPath;
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

  private JButton previewNoOverlapBtn;

  private InputPanel inputPanel;
  private SubgridPanel subGridPanel;

  private StitchingGUIFrame mainGui;

  private int imageSize;

  /**
   * Creates the output panel
   * 
   * @param mainGui the reference to the main gui window
   */
  public OutputPanel(StitchingGUIFrame mainGui) {
    this.makingChanges = false;
    setFocusable(false);

    this.mainGui = mainGui;

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


  private void initControls() {

    this.metadataPath = new DirectoryChooserPanel("Metadata Directory");

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
            "Prefix for output filenames. Must be valid file name"));

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

    JButton qButton = new JButton("?");
    qButton.addActionListener(new HelpDocumentationViewer("OutputParameters"));  
    c.anchor = GridBagConstraints.NORTHEAST;
    c.gridy = 0;
    vertPanel.add(qButton, c);

    c.insets = new Insets(0, 0, 0, 0);
    c.gridy = 1;
    c.anchor = GridBagConstraints.LINE_START;   
    vertPanel.add(this.useImageDirectory, c);
            
    c.gridy = 2;
    vertPanel.add(this.outputPath, c);
    c.gridy = 3;
    vertPanel.add(this.filePrefixName, c);
    c.gridy = 4;    
    vertPanel.add(this.metadataPath, c);
    
    c.gridy = 5;
    c.insets = new Insets(20, 0, 0, 0);
    vertPanel.add(blendingPanel, c);
    
    c.gridy = 6;
    vertPanel.add(checkBoxPanel, c);


    JPanel estimatedFileSizePanel = new JPanel();
    this.updateBtn = new JButton("Update");
    this.updateBtn.addActionListener(this);

    estimatedFileSizePanel.add(this.estimatedFileSizeLabel);
    estimatedFileSizePanel.add(this.updateBtn);

    c.gridy = 7;
    vertPanel.add(estimatedFileSizePanel, c);


    this.previewNoOverlapBtn = new JButton("Preview Mosaic With No Overlap");
    this.previewNoOverlapBtn.addActionListener(this);

    c.gridy = 8;
    vertPanel.add(this.previewNoOverlapBtn, c);

    mainPanel.add(vertPanel);

    add(mainPanel);

    this.useImageDirectory.addActionListener(this);
    this.outputPath.getInputField().getDocument().addDocumentListener(this);
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
    } else if (e.getSource() == this.previewNoOverlapBtn) {
      this.mainGui.displayNoOverlap();
    }

  }

  private void processDocumentEvent(DocumentEvent e)
  {
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
    this.metadataPath.setValue(params.getOutputParams().getMetadataPath());

    this.filePrefixName.setValue(params.getOutputParams().getOutFilePrefix());

    if (this.outputPath.getValue().equals(this.imageDirectory.getText())) {
      this.useImageDirectory.setSelected(true);
    } else {
      this.useImageDirectory.setSelected(false);
    }

    this.blendingType.setSelectedItem(params.getOutputParams().getBlendingMode());

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
    params.getOutputParams().setMetadataPath(this.metadataPath.getValue());
    params.getOutputParams().setOutputPath(this.outputPath.getValue());
    params.getOutputParams().setDisplayStitching(this.displayStitching.isSelected());
    params.getOutputParams().setOutputFullImage(this.outputFullImage.isSelected());
    params.getOutputParams().setOutputMeta(true);
    params.getOutputParams().setOutFilePrefix(this.filePrefixName.getValue());
    params.getOutputParams().setBlendingMode((BlendingMode)this.blendingType.getSelectedItem());
  }
}
