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
// Date: Apr 18, 2014 1:00:29 PM EST
//
// Time-stamp: <Apr 18, 2014 1:00:29 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.gui.panels.inputTab;

import gov.nist.isg.mist.stitching.gui.panels.subgrid.SubgridPanel;
import ij.ImagePlus;
import gov.nist.isg.mist.stitching.gui.components.filechooser.DirectoryChooserPanel;
import gov.nist.isg.mist.stitching.gui.components.helpDialog.HelpDocumentationViewer;
import gov.nist.isg.mist.stitching.gui.components.textfield.TextFieldInputPanel;
import gov.nist.isg.mist.stitching.gui.components.textfield.textFieldModel.*;
import gov.nist.isg.mist.stitching.gui.panels.outputTab.OutputPanel;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.gui.params.interfaces.GUIParamFunctions;
import gov.nist.isg.mist.stitching.gui.params.objects.RangeParam;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.RowColTileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.SequentialTileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridDirection;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridOrigin;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.LoaderType;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoaderUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

/**
 * Creates the input panel
 * 
 * @author Tim Blattner
 * @version 1.0
 * 
 */
public class InputPanel extends JPanel implements GUIParamFunctions, ActionListener {


  private static final String filePatternSequentialHelp = "Used to identify specific image"
      + " files within a directory.\n\n"
      + "Example 1: Img_pos001_time001.tif = Img_pos{ppp}_time{ttt}.tif\n"
      + "Example 2: Img_pos0001_c01.tif = Img_pos00{pp}_c01.tif\n\n"
      + "{ppp} - Special text that represents position numbering between 0 and 999."
      + " Increase the number of p's to represent larger numbers. \n\n"
      + "{ttt} - (optional) Special text that represents timeslice numbering between 0 and 999. "
      + "{ttt} can be used for z stacks as well as timeslices. Each value of "
      + "{ttt} will be stitched independently. Increase the number of t's to "
      + "represent larger numbers.";


  private static final String filePatternRowColHelp = "Used to identify specific image"
      + " files within a directory.\n\n"
      + "Example 1: Img_r01_c01_time001.tif = Img_r{rr}_c{cc}_time{ttt}.tif\n"
      + "Example 2: Img_r1_c1_channel01.tif = Img_r{r}_c{c}_channel01.tif\n\n"
      + "{rr} - Special text that represents row numbering between 0 and 99."
      + " Increase the number of r's to represent larger numbers. \n\n"
      + "{cc} - Special text that represents column numbering between 0 and 99."
      + " Increase the number of c's to represent larger numbers. \n\n"
      + "{ttt} - (optional) Special text that represents timeslice numbering between 0 and 999. "
      + "{ttt} can be used for z stacks as well as timeslices. Each value of "
      + "{ttt} will be stitched independently. Increase the number of t's to "
      + "represent larger numbers.";

  private static final long serialVersionUID = 1L;

  private TextFieldInputPanel<Integer> plateWidth;
  private TextFieldInputPanel<Integer> plateHeight;
  private TextFieldInputPanel<String> filenamePattern;
  private DirectoryChooserPanel fileChooser;
  private TextFieldInputPanel<List<RangeParam>> timeSlices;

  private JComboBox filenamePatternType;

  private JComboBox origin;
  private JComboBox gridNumbering;

  private JCheckBox assembleFromMeta;

  private int startTimeSlice;
  private int endTimeSlice;
  private int startTileNumber;
  private boolean hasTimeSlices;

  private boolean positionLoaded;

  private TextFieldModel<String> sequentialValidator;
  private TextFieldModel<String> rowColValidator;

  private GridDirection savedDirection;

  /**
   * Initializes the input panel
   * 
   * @param subgridPanel the advanced panel that contains the sub-grid params
   * @param outputPanel the output panel that contains image size
   */
  public InputPanel(SubgridPanel subgridPanel, OutputPanel outputPanel) {
    
    this.positionLoaded = false;

    this.filenamePatternType = new JComboBox(LoaderType.values());

    this.sequentialValidator =
        new RegexResetImageSizeModel(SequentialTileGridLoader.positionPattern,
            SequentialTileGridLoader.patternExample, outputPanel);

    this.rowColValidator =
        new DualRegexResetImageSizeModel(RowColTileGridLoader.rowPattern,
            RowColTileGridLoader.colPattern, RowColTileGridLoader.patternExample, outputPanel);


    this.filenamePattern =
        new TextFieldInputPanel<String>("Filename Pattern", "F_{pppp}.tif", 30, this.sequentialValidator,
            filePatternSequentialHelp);
    this.plateWidth =
        new TextFieldInputPanel<Integer>("Grid Width", "4", new UpdateSubGridModel(1,
            Integer.MAX_VALUE, subgridPanel));
    this.plateHeight =
        new TextFieldInputPanel<Integer>("Grid Height", "4", new UpdateSubGridModel(1,
            Integer.MAX_VALUE, subgridPanel));
    this.fileChooser = new DirectoryChooserPanel("Image Directory");

    this.origin = new JComboBox(GridOrigin.values());
    this.gridNumbering = new JComboBox(GridDirection.values());

    this.assembleFromMeta = new JCheckBox("Assemble from metadata", false);
    this.assembleFromMeta.setToolTipText("<html>Composes image based on metadata."
        + "<br>Uses absolute positions file from metadata directory</html>");

    this.timeSlices =
        new TextFieldInputPanel<List<RangeParam>>("Timeslices", "", new TimeslicesModel());

    init();
  }

  private void init() {
    JPanel inputPanel = new JPanel();

    GridBagLayout layout = new GridBagLayout();

    inputPanel.setLayout(layout);

    GridBagConstraints c = new GridBagConstraints();


    JPanel filePatternTypePanel = new JPanel();
    filePatternTypePanel.add(new JLabel("Filename Pattern Type"));
    filePatternTypePanel.add(this.filenamePatternType);
    this.filenamePatternType.addActionListener(this);

    JPanel widthHeightPanel = new JPanel();
    widthHeightPanel.add(this.plateWidth);
    widthHeightPanel.add(this.plateHeight);

    JPanel numberingOriginPanel = new JPanel();

    JPanel numberingPanel = new JPanel();
    numberingPanel.add(new JLabel("Direction: "));
    numberingPanel.add(this.gridNumbering);

    JPanel originPanel = new JPanel();
    originPanel.add(new JLabel("Starting Point: "));
    originPanel.add(this.origin);

    numberingOriginPanel.add(originPanel);
    numberingOriginPanel.add(numberingPanel);
    
    
    JButton qButton = new JButton("?");
    HelpDocumentationViewer helpDialog = new HelpDocumentationViewer("InputParameters");
    qButton.addActionListener(helpDialog);
    
    
    c.anchor = GridBagConstraints.NORTHEAST;
    c.gridy = 0;
    inputPanel.add(qButton, c);
    
    c.anchor = GridBagConstraints.LINE_START;        
    c.gridy = 1;
    inputPanel.add(filePatternTypePanel, c);

    c.gridy = 2;
    inputPanel.add(this.filenamePattern, c);

    c.gridy = 3;
    inputPanel.add(this.fileChooser, c);

    c.gridy = 4;
    inputPanel.add(numberingOriginPanel, c);

    c.gridy = 5;
    inputPanel.add(this.assembleFromMeta, c);

    c.gridy = 6;
    inputPanel.add(widthHeightPanel, c);

    c.gridy = 7;
    inputPanel.add(this.timeSlices, c);

    OrientationPanel orientationPanel = new OrientationPanel(this);

    c.gridy = 8;
    c.anchor = GridBagConstraints.CENTER;
    inputPanel.add(orientationPanel, c);

    add(inputPanel);

    this.savedDirection = this.getNumbering();

  }

  /**
   * Gets whether to assemble using metadata or not
   * 
   * @return true if assemble from metadata, otherwise false
   */
  public boolean isAssembleWithMetadata() {
    return this.assembleFromMeta.isSelected();
  }

  /**
   * Gets the origin JComboBox
   * 
   * @return the origin JComboBox
   */
  public JComboBox getOriginComponent() {
    return this.origin;
  }

  /**
   * Gets the grid numbering JComboBox
   * 
   * @return the grid numbering JComboBox
   */
  public JComboBox getGridNumberingComponent() {
    return this.gridNumbering;
  }

  /**
   * Gets the directory chooser panel
   * 
   * @return the directory chooser panel
   */
  public DirectoryChooserPanel getDirectoryChooserPanel() {
    return this.fileChooser;
  }



  /**
   * Gets the plate width that the user specified
   * 
   * @return the plate width
   */
  public int getPlateWidth() {
    return this.plateWidth.getValue();
  }

  /**
   * Gets the plate height that the user specified
   * 
   * @return the plate height
   */
  public int getPlateHeight() {
    return this.plateHeight.getValue();
  }


  /**
   * Gets the image directory that the user specified
   * 
   * @return the image directory
   */
  public String getImageDirectory() {
    return this.fileChooser.getValue();
  }

  /**
   * Gets the file pattern that the user specified
   * 
   * @return the file pattern
   */
  public String getFilePattern() {
    return this.filenamePattern.getValue();
  }

  /**
   * Get the grid origin that the user specified
   * 
   * @return the grid origin
   */
  public GridOrigin getOrigin() {
    return (GridOrigin)this.origin.getSelectedItem();
  }

  /**
   * Gets the file pattern loader type
   * 
   * @return the file pattern loader type
   */
  public LoaderType getLoaderType() {
    return (LoaderType)this.filenamePatternType.getSelectedItem();
  }

  /**
   * Gets the grid numbering that the user specified
   * 
   * @return the grid numbering
   */
  public GridDirection getNumbering() {    
    return (GridDirection)this.gridNumbering.getSelectedItem();
  }

  private boolean queryStartTileNumber() {
    boolean done = false;
    while (!done) {
      String msg =
          JOptionPane.showInputDialog(this, "Unable to determine "
              + "starting tile index. Please input starting tile index.", "Starting tile index",
              JOptionPane.QUESTION_MESSAGE);

      if (msg == null)
        return false;

      try {
        this.startTileNumber = Integer.parseInt(msg);
        return true;
      } catch (NumberFormatException e) {
        Log.msg(LogType.MANDATORY, "Please enter integer value into text field.");
      }
    }

    return false;
  }

  private boolean queryStartTimeslice() {
    boolean done = false;
    while (!done) {
      String msg =
          JOptionPane.showInputDialog(this, "Unable to determine "
              + "starting time slice. Please input starting time slice.", "Starting time slice",
              JOptionPane.QUESTION_MESSAGE);

      if (msg == null)
        return false;

      try {
        this.startTimeSlice = Integer.parseInt(msg);
        return true;
      } catch (NumberFormatException e) {
        Log.msg(LogType.MANDATORY, "Please enter integer value into text field.");
      }
    }

    return false;
  }

  private void findEndTimeslice(String imageDir, String filePattern) {
    String timeFilePattern =
        TileGridLoaderUtils.parsePositionPattern(filePattern, this.getLoaderType(),
            this.startTileNumber, false);

    int stride = 100;

    int endSlice = this.startTimeSlice;
    int prevSlice = endSlice;
    boolean found = false;

    while (!found) {
      // Check if endSlice exists as a file
      if (TileGridLoaderUtils.checkTimeSliceTile(imageDir, timeFilePattern, endSlice, false)) {
        // increment by stride
        prevSlice = endSlice;
        endSlice += stride;
      } else {
        // If the endSlice did not exist and the stride is one, then we are done
        if (stride == 1)
          found = true;

        // decrement stride
        stride = (int) Math.ceil(stride / 2.0);

        endSlice = prevSlice;
      }
    }

    this.endTimeSlice = endSlice;

  }

  private boolean checkPosition(String imageDir, String filePattern) {
    // check 0 and 1 position
    if (TileGridLoaderUtils.checkStartTile(imageDir, filePattern, 0, this.getLoaderType(), true)) {
      this.startTileNumber = 0;
      return true;
    } else if (TileGridLoaderUtils.checkStartTile(imageDir, filePattern, 1, this.getLoaderType(),
        true)) {
      this.startTileNumber = 1;
      return true;
    } else {
      return false;
    }
  }

  /**
   * Gets the initial image's file size
   * 
   * @return the size of the initial image
   */
  public int getInitialImageFileSize() {
    // String filePattern = this.getFilePattern();
    // String imageDirectory = this.getImageDirectory();
    int size = 0;
    if (updateStartPositionAndTimeslice(false)) {
      String fPath = null;
      if (this.hasTimeSlices) {
        fPath = TileGridLoaderUtils.parseTimeSlicePattern(getFilePattern(), this.startTimeSlice, false);
      }

      if (fPath == null) {
        fPath = getFilePattern();
      }

      fPath =
          TileGridLoaderUtils.parsePositionPattern(fPath, this.getLoaderType(), this.startTileNumber,
              false);

      if (fPath != null) {
        fPath = this.getImageDirectory() + File.separator + fPath;
        ImagePlus img = new ImagePlus(fPath);
        int[] dims = img.getDimensions();

        size = 1;
        for (int dim : dims)
          size = size * dim;

        this.positionLoaded = false;
      }
    }
    return size;
  }

  private boolean updateStartPositionAndTimeslice(boolean isClosing) {
    int plateWidth = this.getPlateWidth();
    int plateHeight = this.getPlateHeight();
    String filePattern = this.getFilePattern();
    String imageDirectory = this.getImageDirectory();
    GridOrigin origin = this.getOrigin();
    GridDirection numbering = this.getNumbering();

    if (!isClosing) {
      Log.msg(LogType.HELPFUL, "width: " + plateWidth);
      Log.msg(LogType.HELPFUL, "height: " + plateHeight);
      Log.msg(LogType.HELPFUL, "filePattern: " + filePattern);
      Log.msg(LogType.HELPFUL, "imageDirectory: " + imageDirectory);
      Log.msg(LogType.HELPFUL, "gridOrigin: " + origin);
      Log.msg(LogType.HELPFUL, "gridNumbering: " + numbering);
    }

    String positionPattern = filePattern;

    // Check if time slices are in the file pattern.
    if (TileGridLoaderUtils.hasTimeFilePattern(filePattern)) {
      this.hasTimeSlices = true;

      boolean foundTimeSlice = false;
      // Check start time position
      for (int timeSlice = 0; timeSlice < 2; timeSlice++) {
        positionPattern =
            TileGridLoaderUtils.parseTimeSlicePattern(filePattern, timeSlice, isClosing);
        if (checkPosition(imageDirectory, positionPattern)) {
          this.startTimeSlice = timeSlice;
          foundTimeSlice = true;
          break;
        }
      }

      if (!foundTimeSlice) {
        if (!isClosing) {
          queryStartTileNumber();
          queryStartTimeslice();
        }

        if (!TileGridLoaderUtils.checkStartTile(imageDirectory, filePattern, this.startTileNumber,
            this.startTimeSlice, this.getLoaderType(), isClosing)) {
          this.filenamePattern.showError();
          return false;
        }
      }

      // Search for endTimeSlice
      findEndTimeslice(imageDirectory, filePattern);

    } else {
      this.hasTimeSlices = false;
      if (!checkPosition(imageDirectory, positionPattern)) {
        if (!isClosing)
          queryStartTileNumber();
      }

      if (!TileGridLoaderUtils.checkStartTile(imageDirectory, filePattern, this.startTileNumber,
          this.getLoaderType(), isClosing)) {
        this.filenamePattern.showError();
        return false;
      }

    }

    this.positionLoaded = true;
    return true;
  }


  @Override
  public void loadParamsIntoGUI(StitchingAppParams params) {
    enableLoadingParams();
    this.plateWidth.setValue(params.getInputParams().getGridWidth());
    this.plateHeight.setValue(params.getInputParams().getGridHeight());
    this.filenamePattern.setValue(params.getInputParams().getFilenamePattern());
    this.assembleFromMeta.setSelected(params.getInputParams().isAssembleFromMetadata());

    GridDirection numbering = params.getInputParams().getNumbering();    
    
    if (numbering == null)
      numbering = GridDirection.HORIZONTALCOMBING;
    
    this.savedDirection = numbering;
    this.gridNumbering.setSelectedItem(numbering);

    GridOrigin gridOrigin = params.getInputParams().getOrigin();
    if (gridOrigin == null)
      gridOrigin = GridOrigin.UR;
    this.origin.setSelectedItem(gridOrigin);

    this.fileChooser.setValue(params.getInputParams().getImageDir());

    this.timeSlices.setValue(params.getInputParams().getTimeSlicesStr());

    LoaderType lt = params.getInputParams().getFilenamePatternLoaderType();

    if (lt == null)
      lt = LoaderType.SEQUENTIAL;

    this.filenamePatternType.setSelectedItem(lt);


    disableLoadingParams();
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

    if (this.plateWidth.hasError() || this.plateHeight.hasError() || this.filenamePattern.hasError()
        || this.timeSlices.hasError())
      return false;

    String imageDirectory = this.getImageDirectory();
    GridOrigin origin = this.getOrigin();
    GridDirection numbering = this.getNumbering();
    LoaderType lt = this.getLoaderType();

    if (imageDirectory == null || origin == null || numbering == null || lt == null) {
      return false;
    }

    if (updateStartPositionAndTimeslice(false)) {
      return true;
    }
    return false;
  }

  private boolean loadingParams = false;

  @Override
  public void enableLoadingParams() {
    this.loadingParams = true;
    this.plateWidth.enableIgnoreErrors();
    this.plateHeight.enableIgnoreErrors();
    this.filenamePattern.enableIgnoreErrors();
  }

  @Override
  public void disableLoadingParams() {
    this.loadingParams = false;
    this.plateWidth.disableIgnoreErrors();
    this.plateHeight.disableIgnoreErrors();
    this.filenamePattern.disableIgnoreErrors();
  }

  @Override
  public boolean isLoadingParams() {
    return this.loadingParams;
  }

  @Override
  public void saveParamsFromGUI(StitchingAppParams params, boolean isClosing) {
    int plateWidth = this.getPlateWidth();
    int plateHeight = this.getPlateHeight();
    String filePattern = this.getFilePattern();
    String imageDirectory = this.getImageDirectory();
    GridOrigin origin = this.getOrigin();
    GridDirection numbering = this.getNumbering();
    LoaderType lt = this.getLoaderType();

    List<RangeParam> timeSliceParam = this.timeSlices.getValue();


    if (!this.positionLoaded) {
      if (!updateStartPositionAndTimeslice(isClosing)) {
        if (!isClosing)
          Log.msg(LogType.MANDATORY, "Error parsing starting position. Using defaults.");
        this.startTimeSlice = 1;
        this.endTimeSlice = 1;
        this.startTileNumber = 1;
        this.hasTimeSlices = false;
      }
    }
    
    if (timeSliceParam.size() == 0) {
      timeSliceParam.add(new RangeParam(this.startTimeSlice, this.endTimeSlice));
    }


    params.getInputParams().setGridWidth(plateWidth);
    params.getInputParams().setGridHeight(plateHeight);
    params.getInputParams().setStartTile(this.startTileNumber);
    params.getInputParams().setFilenamePattern(filePattern);
    params.getInputParams().setImageDir(imageDirectory);
    params.getInputParams().setOrigin(origin);
    params.getInputParams().setNumbering(numbering);
    params.getInputParams().setAssembleFromMetadata(this.assembleFromMeta.isSelected());
    params.getInputParams().setFilenamePatternLoaderType(lt);
    params.getInputParams().setTimeSlices(timeSliceParam);
    params.getInputParams().setTimeSlicesEnabled(this.hasTimeSlices);    

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == this.filenamePatternType) {
      LoaderType lt = (LoaderType)this.filenamePatternType.getSelectedItem();

      switch (lt) {
        case ROWCOL:
          this.filenamePattern.setValidator(this.rowColValidator);
          this.filenamePattern.setHelpText(filePatternRowColHelp);
          this.savedDirection = this.getNumbering();
          this.gridNumbering.setSelectedItem(GridDirection.HORIZONTALCOMBING);
          this.gridNumbering.setEnabled(false);
          break;
        case SEQUENTIAL:
          this.filenamePattern.setValidator(this.sequentialValidator);
          this.filenamePattern.setHelpText(filePatternSequentialHelp);
          this.gridNumbering.setSelectedItem(this.savedDirection);
          this.gridNumbering.setEnabled(true);
          break;
      }
    }
  }

}
