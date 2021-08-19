// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 12:58:32 PM EST
//
// Time-stamp: <Apr 18, 2014 12:58:32 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui.panels.advancedTab;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;

import javax.swing.AbstractButton;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import gov.nist.isg.mist.gui.components.buttongroup.ButtonGroupPanel;
import gov.nist.isg.mist.gui.panels.advancedTab.parallelPanels.CUDAPanel;
import gov.nist.isg.mist.gui.panels.advancedTab.parallelPanels.FFTWPanel;
import gov.nist.isg.mist.gui.panels.advancedTab.parallelPanels.JavaPanel;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.gui.params.interfaces.GUIParamFunctions;
import gov.nist.isg.mist.lib.executor.StitchingExecutor.StitchingType;
import gov.nist.isg.mist.lib.libraryloader.LibraryUtils;
import jcuda.LibUtils;

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
//  private CUDAPanel cudaPanel = null;
  private JavaPanel javaPanel = null;

  private GridBagConstraints c;

  /**
   * Initializes the parallel options panel
   */
  public ParallelOptPane() {

    this.programPanel = new JPanel(new GridBagLayout());
    this.programPanel.setBorder(new TitledBorder(new LineBorder(Color.BLACK), "Per Program Options"));
    this.c = new GridBagConstraints();

    // remove the program type NoOverlap from the list of advanced options so the GUI does not
    // display it
    List<StitchingType> stitchingTypes = new ArrayList<StitchingType>(Arrays.asList(StitchingType
        .values()));
    stitchingTypes.remove(StitchingType.NOOVERLAP);


    this.stitchingExecutionType =
        new ButtonGroupPanel(stitchingTypes.toArray(), "Stitching Program");
    this.stitchingExecutionType.setBorder(new TitledBorder(new LineBorder(Color.BLACK), "Stitching Program"));


    this.fftwPanel = new FFTWPanel();
//    this.cudaPanel = new CUDAPanel();
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
    
    if (LibraryUtils.arch != LibUtils.ArchType.X86_64) {
      this.stitchingExecutionType.disableAllButtonsExcept(StitchingType.JAVA.toString());
    }
//    else if (!this.cudaPanel.isCudaAvailable()) {
//      this.stitchingExecutionType.disableButton(StitchingType.CUDA.toString());
//    }
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
//      if (this.cudaPanel != null)
//        this.cudaPanel.setVisible(false);
      if (this.javaPanel != null)
        this.javaPanel.setVisible(false);
    } else if (e.getActionCommand().equals(StitchingType.FFTW.toString())) {
      if (this.fftwPanel != null) {
        this.programPanel.add(this.fftwPanel, this.c);
        this.fftwPanel.setVisible(true);
      }
//      if (this.cudaPanel != null)
//        this.cudaPanel.setVisible(false);
      if (this.javaPanel != null)
        this.javaPanel.setVisible(false);
    }
//    else if (e.getActionCommand().equals(StitchingType.CUDA.toString())) {
//      if (this.fftwPanel != null)
//        this.fftwPanel.setVisible(false);
//      if (this.cudaPanel != null) {
//        this.programPanel.add(this.cudaPanel, this.c);
//        this.cudaPanel.setVisible(true);
//      }
//      if (this.javaPanel != null)
//        this.javaPanel.setVisible(false);
//    }
    else if (e.getActionCommand().equals(StitchingType.JAVA.toString())) {
      if (this.fftwPanel != null)
        this.fftwPanel.setVisible(false);
//      if (this.cudaPanel != null)
//        this.cudaPanel.setVisible(false);
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
//      case CUDA:
//        if (this.cudaPanel != null && this.cudaPanel.isCudaAvailable())
//          this.cudaPanel.loadParamsIntoGUI(params);
//        break;
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
//        if (this.cudaPanel != null && this.cudaPanel.isCudaAvailable())
//          checkPanel &= this.cudaPanel.checkGUIArgs();
        if (this.fftwPanel != null)
          checkPanel &= this.fftwPanel.checkGUIArgs();
        if (this.javaPanel != null)
          checkPanel &= this.javaPanel.checkGUIArgs();

        break;
      case JAVA:
        if (this.javaPanel != null)
          checkPanel = this.javaPanel.checkGUIArgs();
        break;
//      case CUDA:
//        if (this.cudaPanel != null && this.cudaPanel.isCudaAvailable())
//          checkPanel = this.cudaPanel.checkGUIArgs();
//        break;
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
//    if (this.cudaPanel != null)
//      this.cudaPanel.enableLoadingParams();
    if (this.javaPanel != null)
      this.javaPanel.enableLoadingParams();
  }

  @Override
  public void disableLoadingParams() {
    this.loadingParams = false;

    if (this.fftwPanel != null)
      this.fftwPanel.disableLoadingParams();
//    if (this.cudaPanel != null)
//      this.cudaPanel.disableLoadingParams();
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
//      case CUDA:
//        if (this.cudaPanel != null && this.cudaPanel.isCudaAvailable())
//          this.cudaPanel.saveParamsFromGUI(params, isClosing);
//        break;
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
