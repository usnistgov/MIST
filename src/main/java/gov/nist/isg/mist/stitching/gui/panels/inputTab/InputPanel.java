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

import gov.nist.isg.mist.stitching.gui.components.dropdown.DropDownPanel;
import gov.nist.isg.mist.stitching.gui.components.filechooser.FileChooserPanel;
import gov.nist.isg.mist.stitching.gui.panels.subgrid.SubgridPanel;
import gov.nist.isg.mist.stitching.gui.params.OutputParameters;
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
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

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

  private static final String filenameTypeHelp = "Used to specify the type of filename pattern of "
      + "the acquired images.\n\n"
      + "Sequential:\n"
      + "Sequential images have a single number representing their position within the image grid.\n"
      + "Example: img_pos0001_c01.tif = img_pos0{ppp}_c01.tif\n\n"
      + "{ppp} - Special text that represents position numbering between 0 and 999."
      + " Increase the number of p's to represent larger numbers. \n\n"
      + "Row-Column:\n"
      + "Row-Column images have a pair of numbers representing their position within the image grid.\n"
      + "Example: img_r01_c01_channel01.tif = img_r{rr}_c{cc}_channel01.tif\n\n"
      + "{rr} - Special text that represents row numbering between 0 and 99."
      + " Increase the number of r's to represent larger numbers. \n\n"
      + "{cc} - Special text that represents column numbering between 0 and 99."
      + " Increase the number of c's to represent larger numbers. \n\n";
  private static final String gridWidthHelp = "-The number of images in a row. \n-The number of "
      + "columns. \n-The width of the image grid.";
  private static final String gridHeightHelp = "-The number of images in a column. \n-The number of"
      + " rows. \n-The height of the image grid.";
  private static final String timeslicesHelp = "The number of timeslices to stitch. Leave this "
      + "field blank to stitch all timeslices. \n\nTo stitch timeslices you must add the special "
      + "format text \"{ttt}\" to the Filename Pattern. \n\nThis input supports a comma separated "
      + "list and/or range using a '-'. Example: \"1-25, 35, 45\" stitches timeslices 1 through 25,"
      + " 35, and 45.";
  private static final String originHelp = "The starting point of the microscope scan. This "
      + "specifies the origin for the grid of images.";
  private static final String directionHelp = "The direction and pattern of the microscope stage "
      + "movement during acquisition.";
  private static final String globalPositionsFileHelp = "Generates the image mosaic using metadata "
      + "from a previous run.\n\nImportant: you must specify the global-positions file to use. "
      + "If your filename pattern has a time slice iterator \"{tt}\" the global-positions file "
      + "specified must also.\n\nIt is possible to perform multichannel stitching by stitching a "
      + "registration channel and then using this functionality to assemble the secondary channels.";


//  // To remove the "?" help buttons set the help text to null
//  private static final String filePatternSequentialHelp = null;
//  private static final String filePatternRowColHelp = null;
//  private static final String filenameTypeHelp = null;
//  private static final String gridHeightHelp = null;
//  private static final String gridWidthHelp = null;
//  private static final String timeslicesHelp = null;
//  private static final String originHelp = null;
//  private static final String directionHelp = null;
//  private static final String globalPositionsFileHelp = null;


  private static final long serialVersionUID = 1L;

  private static final String NA_STRING = "N/A for Row-Column";

  private TextFieldInputPanel<Integer> plateWidth;
  private TextFieldInputPanel<Integer> plateHeight;
  private JButton discoverGridButton;
  private TextFieldInputPanel<String> filenamePattern;
  private DirectoryChooserPanel fileChooser;
  private TextFieldInputPanel<List<RangeParam>> timeSlices;


  private DropDownPanel filenamePatternTypeDropDown;
  private DropDownPanel originPanel;
  private DropDownPanel directionPanel;


  private JCheckBox assembleFromMeta;
  private FileChooserPanel globalPositionFile;

  private int startTimeSlice;
  private int endTimeSlice;
  private int startTileNumber;
  private boolean hasTimeSlices;

  private boolean positionLoaded;

  private TextFieldModel<String> sequentialValidator;
  private TextFieldModel<String> rowColValidator;

  private GridDirection savedDirection;

  private OutputPanel outputPanel;

  /**
   * Initializes the input panel
   *
   * @param subgridPanel the advanced panel that contains the sub-grid params
   * @param outputPanel  the output panel that contains image size
   */
  public InputPanel(SubgridPanel subgridPanel, OutputPanel outputPanel) {
    this.outputPanel = outputPanel;

    this.positionLoaded = false;

    this.filenamePatternTypeDropDown = new DropDownPanel("Filename Pattern Type", LoaderType.values(), filenameTypeHelp);
    this.filenamePatternTypeDropDown.addComboBoxActionListener(this);


    this.sequentialValidator =
        new RegexResetImageSizeModel(SequentialTileGridLoader.positionPattern,
            SequentialTileGridLoader.patternExample, outputPanel, "%");

    this.rowColValidator =
        new DualRegexResetImageSizeModel(RowColTileGridLoader.rowPattern,
            RowColTileGridLoader.colPattern, RowColTileGridLoader.patternExample, outputPanel, "%");


    this.filenamePattern =
        new TextFieldInputPanel<String>("Filename Pattern", "img_{pppp}.tif", 30, this.sequentialValidator,
            filePatternSequentialHelp);
    this.plateWidth =
        new TextFieldInputPanel<Integer>("Grid Width", "4", new UpdateSubGridModel(1,
            Integer.MAX_VALUE, subgridPanel), gridWidthHelp);
    this.plateHeight =
        new TextFieldInputPanel<Integer>("Grid Height", "4", new UpdateSubGridModel(1,
            Integer.MAX_VALUE, subgridPanel), gridHeightHelp);
    this.discoverGridButton = new JButton("Discover Width/Height");
    this.discoverGridButton.addActionListener(this);
    this.discoverGridButton.setToolTipText("<html>Attempt to automatically discover the width and height<br>of a row-column acquisition given a valid filename<br>pattern and image directory.</html>");

    this.fileChooser = new DirectoryChooserPanel("Image Directory");


    this.originPanel = new DropDownPanel("Starting Point", GridOrigin.values(), originHelp);
    this.originPanel.addComboBoxActionListener(this);
    this.directionPanel = new DropDownPanel("Direction", GridDirection.values(), directionHelp);
    this.directionPanel.addComboBoxActionListener(this);

    this.assembleFromMeta = new JCheckBox("Assemble from metadata", false);
    this.assembleFromMeta.setToolTipText("<html>Composes image based on metadata."
        + "<br>Uses the global-positions file specified.</html>");
    this.assembleFromMeta.addActionListener(this);

    this.globalPositionFile = new FileChooserPanel("Global Positions File", globalPositionsFileHelp);

    this.timeSlices =
        new TextFieldInputPanel<List<RangeParam>>("Timeslices", "", new TimeslicesModel(), timeslicesHelp);

    init();
  }


  private void init() {
    JPanel inputPanel = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();

    JPanel numberingOriginPanel = new JPanel();

    numberingOriginPanel.add(originPanel);
    numberingOriginPanel.add(directionPanel);


    JButton qButton = new JButton("Help?");
    HelpDocumentationViewer helpDialog = new HelpDocumentationViewer("input-parameters");
    qButton.addActionListener(helpDialog);

    updateGlobalPositionFile();


    // setup the acquisitions panel
    JPanel acquisitionPanel = new JPanel(new GridBagLayout());
    acquisitionPanel.setBorder(new TitledBorder(new LineBorder(Color.BLACK), "Acquisition Setup"));

    c.gridy = 0;
    c.gridx = 0;
    c.gridwidth = 2;
    c.anchor = GridBagConstraints.LINE_START;
    c.insets = new Insets(-5, 0, 0, 0);
    acquisitionPanel.add(filenamePatternTypeDropDown, c);
//    acquisitionPanel.add(filePatternTypePanel, c);
    c.gridy = 1;
    acquisitionPanel.add(numberingOriginPanel, c);
    c.gridwidth = 1;
    c.insets = new Insets(0, 0, 0, 0);

    c.anchor = GridBagConstraints.CENTER;
    c.gridy = 2;
    c.insets = new Insets(10, 0, 0, 0);
    acquisitionPanel.add(this.plateWidth, c);
    c.insets = new Insets(0, 0, 0, 0);
    c.gridy = 3;
    acquisitionPanel.add(this.plateHeight, c);
    c.gridy = 4;
    acquisitionPanel.add(this.timeSlices, c);
    c.gridy = 5;
    acquisitionPanel.add(this.discoverGridButton, c);
    c.gridx = 1;
    c.gridy = 2;
    c.gridheight = 4;
    OrientationPanel orientationPanel = new OrientationPanel(this);
    acquisitionPanel.add(orientationPanel, c);
    c.gridheight = 1;


    // setup the Image Location panel
    JPanel inputFolderPanel = new JPanel(new GridBagLayout());
    inputFolderPanel.setBorder(
        new TitledBorder(new LineBorder(Color.BLACK), "Input Folder"));
    c.gridy = 0;
    c.gridx = 0;
    c.gridwidth = 1;
    c.anchor = GridBagConstraints.LINE_START;
    inputFolderPanel.add(this.filenamePattern, c);
    c.gridy = 1;
    inputFolderPanel.add(this.fileChooser, c);


    // setup the Multi-Channel Stitching panel
    JPanel multiChannelPanel = new JPanel(new GridBagLayout());
    multiChannelPanel.setBorder(
        new TitledBorder(new LineBorder(Color.BLACK), "Multi-Channel Stitching"));
    c.gridy = 0;
    c.gridx = 0;
    c.gridwidth = 1;
    c.anchor = GridBagConstraints.LINE_START;
    multiChannelPanel.add(this.assembleFromMeta, c);
    c.gridy = 1;
    multiChannelPanel.add(globalPositionFile, c);


    // add everything to the input panel
    c.anchor = GridBagConstraints.NORTHEAST;
    c.gridy = 0;
    c.insets = new Insets(0, 0, 0, 0);
    inputPanel.add(qButton, c);

    c.gridy = 1;
    c.anchor = GridBagConstraints.CENTER;
    c.fill = GridBagConstraints.HORIZONTAL;
    inputPanel.add(acquisitionPanel, c);
    c.gridy = 2;
    c.insets = new Insets(10, 0, 0, 0);
    inputPanel.add(inputFolderPanel, c);
    c.gridy = 3;
    inputPanel.add(multiChannelPanel, c);
    c.insets = new Insets(0, 0, 0, 0);
    c.anchor = GridBagConstraints.LINE_START;
    c.fill = GridBagConstraints.NONE;


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
    return this.originPanel.getComboBox();
  }

  /**
   * Gets the grid numbering JComboBox
   *
   * @return the grid numbering JComboBox
   */
  public JComboBox getGridNumberingComponent() {
    return this.directionPanel.getComboBox();
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
   * Gets the global position file panel
   *
   * @return the global position file panel
   */
  public FileChooserPanel getGlobalPositionFile() {
    return this.globalPositionFile;
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
    return (GridOrigin) this.originPanel.getComboBox().getSelectedItem();
  }

  /**
   * Gets the file pattern loader type
   *
   * @return the file pattern loader type
   */
  public LoaderType getLoaderType() {
    return (LoaderType) this.filenamePatternTypeDropDown.getComboBox().getSelectedItem();
  }

  /**
   * Gets the file pattern loader
   *
   * @return the file pattern loader
   */
  public JComboBox getFilenamePatternType() {
    return this.filenamePatternTypeDropDown.getComboBox();
  }


  /**
   * Gets the grid numbering that the user specified
   *
   * @return the grid numbering
   */
  public GridDirection getNumbering() {
    Object val = this.directionPanel.getComboBox().getSelectedItem();
    if (val.equals(NA_STRING))
      return (GridDirection.HORIZONTALCOMBING);
    else
      return (GridDirection) val;
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

      // check if the current startTimeSlice and startTileNumber are valid
      positionPattern =
          TileGridLoaderUtils.parseTimeSlicePattern(filePattern, this.startTimeSlice, isClosing);
      if (!TileGridLoaderUtils.checkStartTile(imageDirectory, positionPattern, this.startTileNumber, this.getLoaderType(), true)) {
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
      }


      // Search for endTimeSlice
      findEndTimeslice(imageDirectory, filePattern);

    } else {
      this.hasTimeSlices = false;

      // check the current startTileNumber
      if (!TileGridLoaderUtils.checkStartTile(imageDirectory, filePattern, this.startTileNumber,
          this.getLoaderType(), isClosing)) {

        // check 0,1 based tile numbering
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
    this.globalPositionFile.setValue(params.getInputParams().getGlobalPositionsFile());

    GridDirection numbering = params.getInputParams().getNumbering();

    if (numbering == null)
      numbering = GridDirection.HORIZONTALCOMBING;

    this.savedDirection = numbering;
    this.directionPanel.getComboBox().setSelectedItem(numbering);

    GridOrigin gridOrigin = params.getInputParams().getOrigin();
    if (gridOrigin == null)
      gridOrigin = GridOrigin.UR;
    this.originPanel.getComboBox().setSelectedItem(gridOrigin);

    this.fileChooser.setValue(params.getInputParams().getImageDir());

    this.timeSlices.setValue(params.getInputParams().getTimeSlicesStr());

    LoaderType lt = params.getInputParams().getFilenamePatternLoaderType();

    if (lt == null)
      lt = LoaderType.SEQUENTIAL;

    this.filenamePatternTypeDropDown.getComboBox().setSelectedItem(lt);

    updateGlobalPositionFile();

    this.globalPositionFile.setValue(params.getInputParams().getGlobalPositionsFile());

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

    if (!updateStartPositionAndTimeslice(false))
      return false;

    // ensure that if the filename has timeslices that the global positions file to be loaded has timeslies
    if (this.isAssembleWithMetadata()) {
      // determine whether the global positions file has a time slice iterator
      String str = TileGridLoaderUtils.getPattern(this.getGlobalPositionFile().getValue(),
          TileGridLoaderUtils.timePattern, true);
      // str will be null if there was no timeslice iterator, and non null if there was

      if (this.hasTimeSlices) {
        // ensure that the global positions file has timeslices
        if (str == null) {
          // global positions file does not have a timeslice iterator when it should
          Log.msg(LogType.MANDATORY, "Timeslice iterator \"{tt}\" mismatch between global "
              + "positions file and filename pattern. \nEither both the "
              + "filename pattern and the global positions file must have "
              + "time slice iterators or neither can have time slice iterators.");
          this.filenamePattern.showError();
          this.globalPositionFile.showError();
          return false;
        } else {
          // check tha thte number of timeslice iterators matches between the filename and the global positions
          int nDigitsFn = TileGridLoaderUtils.getNumberMatchElements(this.getFilePattern(), TileGridLoaderUtils.timePattern, true);
          int nDigitsGFn = TileGridLoaderUtils.getNumberMatchElements(this.getGlobalPositionFile().getValue(), TileGridLoaderUtils.timePattern, true);

          if (nDigitsFn != nDigitsGFn) {
            // global positions file has a different number of timeslice iterators than the filename
            Log.msg(LogType.MANDATORY, "Timeslice iterator count \"{tt}\" mismatch between global "
                + "positions file and filename pattern. \nThe number of timeslice iterators must match between the"
                + "filename pattern and the global positions file.");
            this.filenamePattern.showError();
            this.globalPositionFile.showError();
            return false;
          }
        }
      } else {
        // ensure that the global positions file does not have timeslices
        if (str != null) {
          // global positions file has a timeslice iterator when it should not
          Log.msg(LogType.MANDATORY, "Timeslice iterator \"{tt}\" mismatch between global "
              + "positions file and filename pattern. \nEither both the "
              + "filename pattern and the global positions file must have "
              + "time slice iterators or neither can have time slice iterators.");
          this.filenamePattern.showError();
          this.globalPositionFile.showError();
          return false;
        }
      }
    }


    return true;
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
    if (this.isAssembleWithMetadata())
      params.getInputParams().setGlobalPositionsFile(this.globalPositionFile.getValue());
    else
      params.getInputParams().setGlobalPositionsFile("");
    params.getInputParams().setFilenamePatternLoaderType(lt);
    params.getInputParams().setTimeSlices(timeSliceParam);
    params.getInputParams().setTimeSlicesEnabled(this.hasTimeSlices);

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == this.filenamePatternTypeDropDown.getComboBox()) {
      LoaderType lt = (LoaderType) this.filenamePatternTypeDropDown.getComboBox().getSelectedItem();

      switch (lt) {
        case ROWCOL:
          this.filenamePattern.setValidator(this.rowColValidator);
          this.filenamePattern.setHelpText(filePatternRowColHelp);
          this.savedDirection = this.getNumbering();
          this.directionPanel.getComboBox().setSelectedItem(GridDirection.HORIZONTALCOMBING);
          this.directionPanel.setEnabled(false);

          this.directionPanel.getComboBox().addItem(NA_STRING);
          this.directionPanel.getComboBox().setSelectedItem(NA_STRING);

          this.discoverGridButton.setEnabled(true);
          break;
        case SEQUENTIAL:
          this.filenamePattern.setValidator(this.sequentialValidator);
          this.filenamePattern.setHelpText(filePatternSequentialHelp);
          this.directionPanel.getComboBox().setSelectedItem(this.savedDirection);
          this.directionPanel.setEnabled(true);

          this.directionPanel.getComboBox().removeItem(NA_STRING);

          this.discoverGridButton.setEnabled(false);
          break;
      }
    } else if (e.getSource() == this.assembleFromMeta) {
      this.updateGlobalPositionFile();
    } else if (e.getSource() == this.discoverGridButton) {
      this.discoverGridSize();
    }
  }

  private void discoverGridSize() {
    // this is only possible with the row/col loader type
    if (getLoaderType() != LoaderType.ROWCOL)
      return;

    // check that the filename pattern is valid
    if (this.filenamePattern.hasError())
      return;

    // validate that the image directory exists
    String imgDir = getImageDirectory();
    String filePattern = getFilePattern();

    // handle time series case
    if (TileGridLoaderUtils.hasTimeFilePattern(filePattern)) {
      this.hasTimeSlices = true;

      boolean foundTimeSlice = false;
      for (int timeSlice = 0; timeSlice < 2; timeSlice++) {
        String positionPattern =
            TileGridLoaderUtils.parseTimeSlicePattern(filePattern, timeSlice, false);
        if (checkPosition(imgDir, positionPattern)) {
          this.startTimeSlice = timeSlice;
          foundTimeSlice = true;
          break;
        }
      }

      if (!foundTimeSlice) {
        queryStartTimeslice();
      }

      filePattern =
          TileGridLoaderUtils.parseTimeSlicePattern(filePattern, this.startTimeSlice, false);
    }

    if (!checkPosition(imgDir, filePattern)) {
      queryStartTileNumber();

      if (!TileGridLoaderUtils.checkStartTile(imgDir, filePattern, this.startTileNumber, LoaderType.ROWCOL, true)) {
        this.plateWidth.setValue(0);
        this.plateHeight.setValue(0);
        return;
      }
    }

    // perform discovery to find width and height of the grid
    int stride = 100;
    int maxRow = this.startTileNumber;
    int minRow = this.startTileNumber;

    int maxCol = this.startTileNumber;
    int minCol = this.startTileNumber;

    int prevVal = this.startTileNumber;
    boolean found = false;


    // perform discovery starting at a known tile where row and column index is this.startTileNumber

    // find the maxRow
    while (!found) {
      if (TileGridLoaderUtils.checkRowColTile(imgDir, filePattern, maxRow, this.startTileNumber, true)) {
        prevVal = maxRow;
        maxRow += stride;
      } else {
        if (stride == 1)
          found = true;
        else
          stride = (int) Math.ceil(stride / 2.0);

        maxRow = prevVal;
      }
    }

    // find the maxRow
    found = false;
    stride = 100;
    prevVal = this.startTileNumber;
    while (!found) {
      if (TileGridLoaderUtils.checkRowColTile(imgDir, filePattern, minRow, this.startTileNumber, true)) {
        prevVal = minRow;
        minRow -= stride;
      } else {
        if (stride == 1)
          found = true;
        else
          stride = (int) Math.ceil(stride / 2.0);

        minRow = prevVal;
      }
    }

    // find the maxCol
    found = false;
    stride = 100;
    prevVal = this.startTileNumber;
    while (!found) {
      if (TileGridLoaderUtils.checkRowColTile(imgDir, filePattern, this.startTileNumber, maxCol, true)) {
        prevVal = maxCol;
        maxCol += stride;
      } else {
        if (stride == 1)
          found = true;
        else
          stride = (int) Math.ceil(stride / 2.0);

        maxCol = prevVal;
      }
    }

    // find the minCol
    found = false;
    stride = 100;
    prevVal = this.startTileNumber;
    while (!found) {
      if (TileGridLoaderUtils.checkRowColTile(imgDir, filePattern, this.startTileNumber, minCol, true)) {
        prevVal = minCol;
        minCol -= stride;
      } else {
        if (stride == 1)
          found = true;
        else
          stride = (int) Math.ceil(stride / 2.0);

        minCol = prevVal;
      }
    }


    this.plateHeight.setValue(maxRow - minRow + 1);
    this.plateWidth.setValue(maxCol - minCol + 1);
  }

  private void updateGlobalPositionFile() {
    this.globalPositionFile.setEnabled(this.isAssembleWithMetadata());
    if (this.isAssembleWithMetadata()) {
      // Update the field
      this.globalPositionFile.setValue(this.outputPanel.getOutputPath().getValue() + File.separator + this.outputPanel.getPrefix() + OutputParameters.absPosFilename + "-{t}" + OutputParameters.metadataSuffix);
    }
  }

}
