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
// Date: Apr 18, 2014 12:58:32 PM EST
//
// Time-stamp: <Apr 18, 2014 12:58:32 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.gui.panels.advancedTab;

import gov.nist.isg.mist.stitching.gui.panels.advancedTab.parallelPanels.CUDAPanel;
import gov.nist.isg.mist.stitching.gui.panels.advancedTab.parallelPanels.FFTWPanel;
import gov.nist.isg.mist.stitching.gui.panels.advancedTab.parallelPanels.JavaPanel;
import jcuda.LibUtils.ARCHType;
import gov.nist.isg.mist.stitching.gui.components.buttongroup.ButtonGroupPanel;
import gov.nist.isg.mist.stitching.lib.executor.StitchingExecutor.StitchingType;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.gui.params.interfaces.GUIParamFunctions;
import gov.nist.isg.mist.stitching.lib.libraryloader.LibraryUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;

/**
 * Creates the parallel options panel
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class ParallelOptPane implements ActionListener, GUIParamFunctions {

  private ButtonGroupPanel stitchingExecutionType;

  private JPanel programPanel;
  private FFTWPanel fftwPanel = null;
  private CUDAPanel cudaPanel = null;
  private JavaPanel javaPanel = null;

  private GridBagConstraints c;

  /**
   * Initializes the parallel options panel
   */
  public ParallelOptPane() {

    this.programPanel = new JPanel(new GridBagLayout());
    this.programPanel.setBorder(new TitledBorder(new LineBorder(Color.BLACK), "Per Program Options"));
    this.c = new GridBagConstraints();

    this.stitchingExecutionType =
        new ButtonGroupPanel(StitchingType.values(), "Stitching Program");
    this.stitchingExecutionType.setBorder(new TitledBorder(new LineBorder(Color.BLACK), "Stitching Program"));


    this.fftwPanel = new FFTWPanel();
    this.cudaPanel = new CUDAPanel();
    this.javaPanel = new JavaPanel();

    initListeners();
    initParallelOptions();

    Enumeration<AbstractButton> abs = this.stitchingExecutionType.getButtonGroup().getElements();
    while (abs.hasMoreElements()) {
      AbstractButton btn = abs.nextElement();
      if (btn.isSelected()) {
        btn.doClick();
        break;
      }
    }
  }

  /**
   * Gets the type of execution
   *
   * @return the button group associated with the stitching execution type
   */
  public ButtonGroupPanel getStitchingTypePanel() {
    return this.stitchingExecutionType;
  }

  /**
   * Gets the panel for the program selector
   *
   * @return the program panel
   */
  public JPanel getProgramPanel() {
    return this.programPanel;
  }

  private void initParallelOptions() {

    this.stitchingExecutionType.enableAllButtons();

    if (LibraryUtils.arch != ARCHType.X86_64) {
      this.stitchingExecutionType.disableAllButtonsExcept(StitchingType.JAVA.toString());
    } else if (!this.cudaPanel.isCudaAvailable()) {
      this.stitchingExecutionType.disableButton(StitchingType.CUDA.toString());
    }
  }

  private void initListeners() {
    Enumeration<AbstractButton> abs = this.stitchingExecutionType.getButtonGroup().getElements();
    while (abs.hasMoreElements()) {
      abs.nextElement().addActionListener(this);
    }

    this.stitchingExecutionType.getRadioButtons()[0].setSelected(true);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    this.c.gridy = 2;
    if (e.getActionCommand().equals(StitchingType.AUTO.toString())) {
      if (this.fftwPanel != null)
        this.fftwPanel.setVisible(false);
      if (this.cudaPanel != null)
        this.cudaPanel.setVisible(false);
      if (this.javaPanel != null)
        this.javaPanel.setVisible(false);
    } else if (e.getActionCommand().equals(StitchingType.FFTW.toString())) {
      if (this.fftwPanel != null) {
        this.programPanel.add(this.fftwPanel, this.c);
        this.fftwPanel.setVisible(true);
      }
      if (this.cudaPanel != null)
        this.cudaPanel.setVisible(false);
      if (this.javaPanel != null)
        this.javaPanel.setVisible(false);
    } else if (e.getActionCommand().equals(StitchingType.CUDA.toString())) {
      if (this.fftwPanel != null)
        this.fftwPanel.setVisible(false);
      if (this.cudaPanel != null) {
        this.programPanel.add(this.cudaPanel, this.c);
        this.cudaPanel.setVisible(true);
      }
      if (this.javaPanel != null)
        this.javaPanel.setVisible(false);
    } else if (e.getActionCommand().equals(StitchingType.JAVA.toString())) {
      if (this.fftwPanel != null)
        this.fftwPanel.setVisible(false);
      if (this.cudaPanel != null)
        this.cudaPanel.setVisible(false);
      if (this.javaPanel != null) {
        this.programPanel.add(this.javaPanel, this.c);
        this.javaPanel.setVisible(true);
      }
    }
    this.programPanel.validate();
  }

  /**
   * Gets the program type that is selected by the user
   *
   * @return the program type that is selected
   */
  public StitchingType getProgramType() {
    return StitchingType.valueOf(this.stitchingExecutionType.getValue().toUpperCase());
  }

  @Override
  public void loadParamsIntoGUI(StitchingAppParams params) {

    StitchingType type = params.getAdvancedParams().getProgramType();

    Enumeration<AbstractButton> abs = this.stitchingExecutionType.getButtonGroup().getElements();
    while (abs.hasMoreElements()) {
      AbstractButton btn = abs.nextElement();
      if (btn.getActionCommand().equals(type.toString())) {
        btn.doClick();
        break;
      }
    }

    switch (type) {
      case AUTO:
        break;
      case JAVA:
        if (this.javaPanel != null)
          this.javaPanel.loadParamsIntoGUI(params);
        break;
      case CUDA:
        if (this.cudaPanel != null && this.cudaPanel.isCudaAvailable())
          this.cudaPanel.loadParamsIntoGUI(params);
        break;
      case FFTW:
        if (this.fftwPanel != null)
          this.fftwPanel.loadParamsIntoGUI(params);
        break;
      default:
        break;

    }
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
    StitchingType type = getProgramType();
    boolean checkPanel = true;
    switch (type) {
      case AUTO:
        if (this.cudaPanel != null && this.cudaPanel.isCudaAvailable())
          checkPanel &= this.cudaPanel.checkGUIArgs();
        if (this.fftwPanel != null)
          checkPanel &= this.fftwPanel.checkGUIArgs();
        if (this.javaPanel != null)
          checkPanel &= this.javaPanel.checkGUIArgs();

        break;
      case JAVA:
        if (this.javaPanel != null)
          checkPanel = this.javaPanel.checkGUIArgs();
        break;
      case CUDA:
        if (this.cudaPanel != null && this.cudaPanel.isCudaAvailable())
          checkPanel = this.cudaPanel.checkGUIArgs();
        break;
      case FFTW:
        if (this.fftwPanel != null)
          checkPanel = this.fftwPanel.checkGUIArgs();
        break;
      default:
        break;
    }

    return checkPanel;
  }

  private boolean loadingParams = false;

  @Override
  public void enableLoadingParams() {
    this.loadingParams = true;

    if (this.fftwPanel != null)
      this.fftwPanel.enableLoadingParams();
    if (this.cudaPanel != null)
      this.cudaPanel.enableLoadingParams();
    if (this.javaPanel != null)
      this.javaPanel.enableLoadingParams();
  }

  @Override
  public void disableLoadingParams() {
    this.loadingParams = false;

    if (this.fftwPanel != null)
      this.fftwPanel.disableLoadingParams();
    if (this.cudaPanel != null)
      this.cudaPanel.disableLoadingParams();
    if (this.javaPanel != null)
      this.javaPanel.disableLoadingParams();
  }

  @Override
  public boolean isLoadingParams() {
    return this.loadingParams;
  }

  @Override
  public void saveParamsFromGUI(StitchingAppParams params, boolean isClosing) {
    StitchingType type = getProgramType();
    switch (type) {
      case AUTO:
        params.getAdvancedParams().setNumCPUThreads(Runtime.getRuntime().availableProcessors());
        break;
      case JAVA:
        if (this.javaPanel != null)
          this.javaPanel.saveParamsFromGUI(params, isClosing);
        break;
      case CUDA:
        if (this.cudaPanel != null && this.cudaPanel.isCudaAvailable())
          this.cudaPanel.saveParamsFromGUI(params, isClosing);
        break;
      case FFTW:
        if (this.fftwPanel != null)
          this.fftwPanel.saveParamsFromGUI(params, isClosing);
        break;
      default:
        params.getAdvancedParams().setNumCPUThreads(Runtime.getRuntime().availableProcessors());
        break;
    }
    params.getAdvancedParams().setProgramType(type);
  }

}
