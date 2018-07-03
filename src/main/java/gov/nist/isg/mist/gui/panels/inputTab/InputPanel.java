// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 1:00:29 PM EST
//
// Time-stamp: <Apr 18, 2014 1:00:29 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui.panels.inputTab;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import gov.nist.isg.mist.gui.components.dropdown.DropDownPanel;
import gov.nist.isg.mist.gui.components.filechooser.DirectoryChooserPanel;
import gov.nist.isg.mist.gui.components.filechooser.FileChooserPanel;
import gov.nist.isg.mist.gui.components.helpDialog.HelpDocumentationViewer;
import gov.nist.isg.mist.gui.components.textfield.TextFieldInputPanel;
import gov.nist.isg.mist.gui.components.textfield.textFieldModel.*;
import gov.nist.isg.mist.gui.panels.outputTab.OutputPanel;
import gov.nist.isg.mist.gui.panels.subgrid.SubgridPanel;
import gov.nist.isg.mist.gui.params.OutputParameters;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.gui.params.interfaces.GUIParamFunctions;
import gov.nist.isg.mist.gui.params.objects.RangeParam;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.tilegrid.loader.RowColTileGridLoader;
import gov.nist.isg.mist.lib.tilegrid.loader.SequentialTileGridLoader;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoader.GridDirection;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoader.GridOrigin;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoader.LoaderType;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoaderUtils;
import ij.ImagePlus;

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
  private static final String gridStartColHelp = "The number used to identify the first image in a column within the grid. " +
          "Column numbering usually starts at 0 or 1, but any positive integer is possible.\n - Numbering must be sequential." + "\n" +
          "- Example: img_r{rr}_c{cc}.tif can have the column numbering start at 5 and go to 10. So the range of image names would be 'img_r{rr}_c05.tif' to 'img_r{rr}_c10.tif'\n" +
          "- The value being specified in this GUI field is the start of the number range.";
  private static final String gridStartRowHelp = "The number used to identify the first image in a row within the grid. " +
          "Row numbering usually starts at 0 or 1, but any positive integer is possible.\n - Numbering must be sequential." + "\n" +
          "- Example: img_r{rr}_c{cc}.tif can have the row numbering start at 2 and go to 5. So the range of image names would be 'img_r02_c{cc}.tif' to 'img_r05_c{cc}.tif'\n" +
          "- The value being specified in this GUI field is the start of the number range.";
  private static final String gridTileStartHelp = "The number used to identify the first image in the grid. Image numbering usually starts at 0 or 1, " +
          "but any positive integer is possible. \n- Numbering must be sequential.\n" +
          "- Example: img_pos0{ppp}_c01.tif numbering can range from 0 to 4. So the range of image names would be 'img_pos0000_c01.tif' to 'img_pos0004_c01.tif'\n" +
          "- The value being specified in this GUI field is the start of the number range.";
  private static final String timeslicesHelp = "The number of timeslices to stitch. \n" +
      "Leave this field blank to stitch all timeslices. \n\n" +
      "To stitch timeslices you must add the special format text \"{ttt}\" to the Filename Pattern. \n\n" +
      "If there is no special format text in the Filename Pattern then this field must be blank. \n\n" +
      "This input supports a comma separated list and/or range using a '-'. \n" +
      "Example: \n" +
      "- \"1-25, 35, 45\" stitches timeslices 1 through 25, 35, and 45.\n" +
      "- \"\" stitches all available timeslices\n" +
      "- \"3\" stitches timeslice 3\n" +
      "- \"0\" stitches timeslice 0";
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
  private TextFieldInputPanel<Integer> plateStartCol;
  private TextFieldInputPanel<Integer> plateStartRow;
  private TextFieldInputPanel<Integer> plateStartTile;
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
  private int startTileNumberRow;
  private int startTileNumberCol;
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


    this.plateStartCol =
            new TextFieldInputPanel<Integer>("Column Start Tile", "0", new IntModel(0, Integer.MAX_VALUE), gridStartColHelp);

    this.plateHeight =
        new TextFieldInputPanel<Integer>("Grid Height", "4", new UpdateSubGridModel(1,
            Integer.MAX_VALUE, subgridPanel), gridHeightHelp);

    this.plateStartRow =
            new TextFieldInputPanel<Integer>("Row Start Tile", "0", new IntModel(0, Integer.MAX_VALUE), gridStartRowHelp);

    this.plateStartTile =
            new TextFieldInputPanel<Integer>("Grid Start Tile", "0", new IntModel(0, Integer.MAX_VALUE), gridTileStartHelp);

//    this.discoverGridButton = new JButton("Discover Width/Height");
//    this.discoverGridButton.addActionListener(this);
//    this.discoverGridButton.setToolTipText("<html>Attempt to automatically discover the width and height<br>of a row-column acquisition given a valid filename<br>pattern and image directory.</html>");

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
    acquisitionPanel.add(this.plateStartCol, c);
    acquisitionPanel.add(this.plateStartTile, c);
    c.gridy = 5;
    acquisitionPanel.add(this.plateStartRow, c);
    c.gridy = 6;
    acquisitionPanel.add(this.timeSlices, c);
    c.gridy = 7;
//    acquisitionPanel.add(this.discoverGridButton, c);
    c.gridx = 1;
    c.gridy = 2;
    c.gridheight = 6;
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
   * Gets the row-col start column number that the user specified
   *
   * @return the start tile number for the columns
   */
  public int getPlateStartCol() {
    return this.plateStartCol.getValue();
  }

  /**
   * Gets the row-col start row number that the user specified
   *
   * @return the start tile number for the rows
   */
  public int getPlateStartRow() {
    return this.plateStartRow.getValue();
  }

  /**
   * Gets the sequential start tile that the user specified
   *
   * @return the start tile number
   */
  public int getPlateStartTile() {
    return this.plateStartTile.getValue();
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
            this.startTileNumber, this.startTileNumberRow, this.startTileNumberCol, false);

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
              this.startTileNumberRow, this.startTileNumberCol, false);

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
    int startTile = this.getPlateStartTile();
    int startTileRow = this.getPlateStartRow();
    int startTileCol = this.getPlateStartCol();
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
          TileGridLoaderUtils.parseTimeSlicePattern(filePattern, startTile, isClosing);
      if (!TileGridLoaderUtils.checkStartTile(imageDirectory, positionPattern, startTile, startTileRow, startTileCol, this.getLoaderType(), true)) {
        // Check start time position
        for (int timeSlice = 0; timeSlice < 2; timeSlice++) {

          if (TileGridLoaderUtils.checkStartTile(imageDirectory, filePattern, startTile, startTileRow, startTileCol, this.getLoaderType(), true)) {
            this.startTimeSlice = timeSlice;
            this.startTileNumber = startTile;
            this.startTileNumberRow = startTileRow;
            this.startTileNumberCol = startTileCol;
            foundTimeSlice = true;
            break;
          }
        }

        if (!foundTimeSlice) {
          if (!isClosing) {
            queryStartTimeslice();
          }
        }
      }


      // Search for endTimeSlice
      findEndTimeslice(imageDirectory, filePattern);

    } else {
      this.hasTimeSlices = false;

      // if no timeslice iterator is found, clear the timslices field
      if (!this.timeSlices.getInputText().isEmpty()) {
        // if timeslices are not empty, but they contains just "0", then a timeslice iterator is not required
        if (!this.timeSlices.getInputText().contentEquals("0")) {
          this.timeSlices.showError();
          Log.msg(LogType.MANDATORY, "No time-slice iterator found. Conflict between filename pattern and Timeslices.");
          return false;
        }
      }

      // Test that the first element of the grid exists
      if (TileGridLoaderUtils.checkStartTile(imageDirectory, filePattern, startTile, startTileRow,
              startTileCol, this.getLoaderType(), isClosing)) {
        this.startTileNumber = startTile;
        this.startTileNumberRow = startTileRow;
        this.startTileNumberCol = startTileCol;
      }else{
        if(this.getLoaderType() == LoaderType.ROWCOL) {
          this.plateStartRow.showError();
          this.plateStartCol.showError();
        }else{
          this.plateStartTile.showError();
        }
        return false;
      }


      // check that the last tile in the grid exists (to ensure width and height are correct)
      int endTile = startTile + (this.getPlateWidth() * this.getPlateHeight() - 1);
      int endTileRow = startTileRow + this.getPlateHeight() - 1;
      int endTileCol = startTileCol + this.getPlateWidth() - 1;
      if (!TileGridLoaderUtils.checkStartTile(imageDirectory, filePattern, endTile, endTileRow,
              endTileCol, this.getLoaderType(), isClosing)) {
        this.plateWidth.showError();
        this.plateHeight.showError();
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
    this.globalPositionFile.setValue(params.getInputParams().getGlobalPositionsFile());
    this.plateStartTile.setValue(params.getInputParams().getStartTile());
    this.plateStartRow.setValue(params.getInputParams().getStartTileRow());
    this.plateStartCol.setValue(params.getInputParams().getStartTileCol());

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
        || this.timeSlices.hasError() || this.plateStartTile.hasError() || this.plateStartRow.hasError() || this.plateStartCol.hasError())
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
    this.plateStartTile.enableIgnoreErrors();
    this.plateStartRow.enableIgnoreErrors();
    this.plateStartCol.enableIgnoreErrors();
  }

  @Override
  public void disableLoadingParams() {
    this.loadingParams = false;
    this.plateWidth.disableIgnoreErrors();
    this.plateHeight.disableIgnoreErrors();
    this.filenamePattern.disableIgnoreErrors();
    this.plateStartTile.disableIgnoreErrors();
    this.plateStartRow.disableIgnoreErrors();
    this.plateStartCol.disableIgnoreErrors();
  }

  @Override
  public boolean isLoadingParams() {
    return this.loadingParams;
  }

  @Override
  public void saveParamsFromGUI(StitchingAppParams params, boolean isClosing) {
    int plateWidth = this.getPlateWidth();
    int plateHeight = this.getPlateHeight();
    int startTileNumber = this.getPlateStartTile();
    int startTileNumberRow = this.getPlateStartRow();
    int startTileNumberCol = this.getPlateStartCol();

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
        this.startTileNumberRow = 0;
        this.startTileNumberCol = 0;
        this.hasTimeSlices = false;
      }
    }

    if (timeSliceParam.size() == 0) {
      timeSliceParam.add(new RangeParam(this.startTimeSlice, this.endTimeSlice));
    }


    params.getInputParams().setGridWidth(plateWidth);
    params.getInputParams().setGridHeight(plateHeight);
    params.getInputParams().setStartTile(startTileNumber);
    params.getInputParams().setStartTileRow(startTileNumberRow);
    params.getInputParams().setStartTileCol(startTileNumberCol);
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

          this.plateStartCol.setEnabled(true);
          this.plateStartCol.setVisible(true);
          this.plateStartRow.setEnabled(true);
          this.plateStartRow.setVisible(true);
          this.plateStartTile.setEnabled(false);
          this.plateStartTile.setVisible(false);
          break;
        case SEQUENTIAL:
          this.filenamePattern.setValidator(this.sequentialValidator);
          this.filenamePattern.setHelpText(filePatternSequentialHelp);
          this.directionPanel.getComboBox().setSelectedItem(this.savedDirection);
          this.directionPanel.setEnabled(true);

          this.directionPanel.getComboBox().removeItem(NA_STRING);

          this.plateStartCol.setEnabled(false);
          this.plateStartCol.setVisible(false);
          this.plateStartRow.setEnabled(false);
          this.plateStartRow.setVisible(false);
          this.plateStartTile.setEnabled(true);
          this.plateStartTile.setVisible(true);
          break;
      }
    } else if (e.getSource() == this.assembleFromMeta) {
      this.updateGlobalPositionFile();
    }
  }


  private void updateGlobalPositionFile() {
    this.globalPositionFile.setEnabled(this.isAssembleWithMetadata());
    if (this.isAssembleWithMetadata()) {
      // Update the field
      this.globalPositionFile.setValue(this.outputPanel.getOutputPath().getValue() + File.separator + this.outputPanel.getPrefix() + OutputParameters.absPosFilename + "-{t}" + OutputParameters.metadataSuffix);
    }
  }

}
