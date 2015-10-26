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
// Date: Apr 18, 2014 12:49:26 PM EST
//
// Time-stamp: <Apr 18, 2014 12:49:26 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.gui.panels.advancedTab.parallelPanels;

import jcuda.LibUtils;
import jcuda.LibUtils.OSType;
import gov.nist.isg.mist.stitching.gui.components.buttongroup.ButtonGroupPanel;
import gov.nist.isg.mist.stitching.gui.components.filechooser.FileChooserPanel;
import gov.nist.isg.mist.stitching.gui.components.textfield.TextFieldInputPanel;
import gov.nist.isg.mist.stitching.gui.components.textfield.textFieldModel.IntModel;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.gui.params.interfaces.GUIParamFunctions;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwPlanType;

import javax.swing.*;

import java.awt.*;
import java.io.File;

/**
 * Cretes a panel to select FFTW options
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class FFTWPanel extends JPanel implements GUIParamFunctions {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  private TextFieldInputPanel<Integer> numThreadsCPU;
  private ButtonGroupPanel fftwPlanGroup;
  private FileChooserPanel fftwLibraryPath;

  private FileChooserPanel savedPlan;
  private JCheckBox savePlanToFile = new JCheckBox("Save Plan?");
  private JCheckBox loadPlanFromFile = new JCheckBox("Load Plan?");

  /**
   * Initializes the FFTW panel
   */
  public FFTWPanel() {
    initControls();
  }

  private void initControls() {
    this.fftwPlanGroup = new ButtonGroupPanel(FftwPlanType.values(), "FFTW Plan Type");

    this.fftwLibraryPath =
        new FileChooserPanel("FFTW Library File", null, System.getProperty("user.dir") + File.separator
            + "lib" + File.separator + "fftw" + File.separator + "libfftw3.dll");

    this.savedPlan =
        new FileChooserPanel("Plan Location (or file)", null, System.getProperty("user.dir")
            + File.separator + "lib" + File.separator + "fftw" + File.separator + "fftPlans");

    int numProc = Runtime.getRuntime().availableProcessors();
    this.numThreadsCPU =
        new TextFieldInputPanel<Integer>("CPU worker threads", Integer.toString(numProc),
            new IntModel(1, numProc));

    this.savePlanToFile.setSelected(true);
    this.loadPlanFromFile.setSelected(true);
    this.fftwPlanGroup.setValue(FftwPlanType.MEASURE.toString());

    JPanel subPanel = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();

    c.gridy = 0;
    c.anchor = GridBagConstraints.FIRST_LINE_START;
    subPanel.add(this.numThreadsCPU, c);

    JPanel saveLoadPanel = new JPanel();
    saveLoadPanel.add(this.savePlanToFile);
    saveLoadPanel.add(this.loadPlanFromFile);

    c.gridy = 1;
    subPanel.add(this.fftwPlanGroup, c);

    c.gridy = 2;
    subPanel.add(saveLoadPanel, c);

    c.gridy = 3;
    subPanel.add(this.fftwLibraryPath, c);

    c.gridy = 4;
    subPanel.add(this.savedPlan, c);

    add(subPanel, c);
  }

  /**
   * Gets the number of CPU threads the user specified
   *
   * @return the number of CPU threads
   */
  public int getNumCPUThreads() {
    return this.numThreadsCPU.getValue();

  }

  /**
   * Gets the user specified FFTW plan type
   *
   * @return the FFTW plan type
   */
  public FftwPlanType getPlanType() {
    return FftwPlanType.valueOf(this.fftwPlanGroup.getValue().toUpperCase());
  }

  /**
   * Gets whether or not the user wishes the save the plan to file
   *
   * @return true if the user wants the plan to be saved
   */
  public boolean getSavePlanToFile() {
    return this.savePlanToFile.isSelected();
  }

  /**
   * Gets whether or not the user wishes the load the plan from file
   *
   * @return true if the user wants the plan to be loaded
   */
  public boolean getLoadPlanFromFile() {
    return this.loadPlanFromFile.isSelected();
  }

  /**
   * Gets the path for the plan that the user specified
   *
   * @return the plan path
   */
  public String getSavedPlanPath() {
    return this.savedPlan.getValue();
  }

  /**
   * Gets the FFTW library path that the user specified
   *
   * @return the path to the library
   */
  public String getFftwLibraryPath() {
    File f = this.fftwLibraryPath.getFile();

    File fParent = f.getParentFile();

    if (fParent == null)
      return ".";

    return fParent.getAbsolutePath();
  }

  /**
   * Gets the FFTW library name that the user specified
   *
   * @return the name of the library
   */
  public String getFftwLibraryName() {
    File f = this.fftwLibraryPath.getFile();
    String libraryName = f.getName();

    if (libraryName.indexOf(".") > 0)
      libraryName = libraryName.substring(0, libraryName.lastIndexOf("."));

    // If the o/s is linux
    OSType os = LibUtils.calculateOS();
    switch (os) {
      case APPLE:
      case LINUX:
        // remove lib prefix
        if (libraryName.startsWith("lib"))
          libraryName = libraryName.substring(3, libraryName.length());

        break;
      case SUN:
      case UNKNOWN:
      case WINDOWS:
        break;
      default:
        break;

    }

    return libraryName;

  }

  /**
   * Gets the FFTW library filename that the user specified
   *
   * @return the FFTW library filename
   */
  public String getFftwLibraryFileName() {
    return this.fftwLibraryPath.getFile().getName();
  }

  @Override
  public void loadParamsIntoGUI(StitchingAppParams params) {
    this.numThreadsCPU.setValue(params.getAdvancedParams().getNumCPUThreads());
    this.savedPlan.setValue(params.getAdvancedParams().getPlanPath());
    this.loadPlanFromFile.setSelected(params.getAdvancedParams().isLoadFFTWPlan());
    this.savePlanToFile.setSelected(params.getAdvancedParams().isSaveFFTWPlan());
    this.fftwPlanGroup.setValue(params.getAdvancedParams().getFftwPlanType().toString());
    this.fftwLibraryPath.setValue(params.getAdvancedParams().getFftwLibraryPath() + File.separator
        + params.getAdvancedParams().getFftwLibraryFileName());
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
    int val = getNumCPUThreads();

    if (val < 1)
      return false;

    FftwPlanType plantype = getPlanType();
    String planPath = getSavedPlanPath();
    String fftwLibraryPath = getFftwLibraryPath();
    String fftwLibraryName = getFftwLibraryName();

    if (plantype == null || planPath == null || fftwLibraryPath == null || fftwLibraryName == null)
      return false;

    return true;
  }

  private boolean loadingParams = false;

  @Override
  public void enableLoadingParams() {
    this.loadingParams = true;
    this.numThreadsCPU.enableIgnoreErrors();
  }

  @Override
  public void disableLoadingParams() {
    this.loadingParams = false;
    this.numThreadsCPU.disableIgnoreErrors();
  }

  @Override
  public boolean isLoadingParams() {
    return this.loadingParams;
  }

  @Override
  public void saveParamsFromGUI(StitchingAppParams params, boolean isClosing) {
    int val = getNumCPUThreads();
    FftwPlanType plantype = getPlanType();
    boolean savePlanToFile = getSavePlanToFile();
    String planPath = getSavedPlanPath();
    String fftwLibraryPath = getFftwLibraryPath();
    String fftwLibraryName = getFftwLibraryName();
    String fftwLibraryFileName = getFftwLibraryFileName();
    boolean loadPlanFromFile = getLoadPlanFromFile();

    params.getAdvancedParams().setNumCPUThreads(val);
    params.getAdvancedParams().setLoadFFTWPlan(loadPlanFromFile);
    params.getAdvancedParams().setSaveFFTWPlan(savePlanToFile);
    params.getAdvancedParams().setFftwPlanType(plantype);
    params.getAdvancedParams().setPlanPath(planPath);
    params.getAdvancedParams().setFftwLibraryPath(fftwLibraryPath);
    params.getAdvancedParams().setFftwLibraryName(fftwLibraryName);
    params.getAdvancedParams().setFftwLibraryFileName(fftwLibraryFileName);
  }

}
