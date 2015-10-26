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
// Date: Apr 18, 2014 12:47:58 PM EST
//
// Time-stamp: <Apr 18, 2014 12:47:58 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.gui.panels.advancedTab.parallelPanels;

import jcuda.CudaException;
import gov.nist.isg.mist.stitching.gui.components.textfield.TextFieldInputPanel;
import gov.nist.isg.mist.stitching.gui.components.textfield.textFieldModel.IntModel;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.gui.params.interfaces.GUIParamFunctions;
import gov.nist.isg.mist.stitching.gui.params.objects.CudaDeviceParam;
import gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaUtils;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Creates a panel to display CUDA parameters
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class CUDAPanel extends JPanel implements GUIParamFunctions, ActionListener {

  private static final long serialVersionUID = 1L;

  private TextFieldInputPanel<Integer> numThreadsCPU;
  private CudaDeviceTableModel tableModel;
  private JTable deviceTable = new JTable();

  private JButton deviceQuery = new JButton("Execute Device Query");
  private JButton refreshTable = new JButton("Refresh Device Table");

  private GridBagConstraints c;

  private boolean isCudaAvailable;

  /**
   * Initializes the CUDA panel
   */
  public CUDAPanel() {
    super(new GridBagLayout());

    this.c = new GridBagConstraints();

    int numProc = Runtime.getRuntime().availableProcessors();
    this.numThreadsCPU =
        new TextFieldInputPanel<Integer>("CPU worker threads", Integer.toString(numProc),
            new IntModel(1, numProc));

    try {
      this.tableModel = new CudaDeviceTableModel();

      initControls();
      this.isCudaAvailable = true;

    } catch (UnsatisfiedLinkError ex) {
      Log.msg(LogType.MANDATORY, "Warning: Unable to load CUDA. Disabling CUDA execution option.");
      Log.msg(LogType.INFO, ex.getMessage());
      this.isCudaAvailable = false;
    } catch (NoClassDefFoundError ex) {
      Log.msg(LogType.MANDATORY, "Warning: Unable to load CUDA. Disabling CUDA execution option.");
      Log.msg(LogType.INFO, ex.getMessage());
      this.isCudaAvailable = false;
    } catch (CudaException ex) {
      Log.msg(LogType.MANDATORY, "Warning: Unable to load CUDA. Disabling CUDA execution option.");
      Log.msg(LogType.INFO, ex.getMessage());
      this.isCudaAvailable = false;
    }
  }

  /**
   * Gets whether CUDA is available or not.
   *
   * @return true if CUDA is available, otherwise false
   */
  public boolean isCudaAvailable() {
    return this.isCudaAvailable;
  }

  private void initControls() {


    this.c.gridy = 1;
    add(this.numThreadsCPU, this.c);
    this.deviceQuery.addActionListener(this);
    this.c.gridy = 2;
    add(this.deviceQuery, this.c);
    this.c.gridy = 3;
    add(this.refreshTable, this.c);
    this.refreshTable.addActionListener(this);
    addTable();

  }

  /**
   * Gets the selected devices from the table
   *
   * @return the selected devices as a list of cudaDeviceParams
   */
  public List<CudaDeviceParam> getSelectedDevices() {
    return this.tableModel.getSelectedDevices();
  }

  /**
   * Adds the table to the panel
   */
  public void addTable() {
    // Create table
    this.deviceTable.setModel(this.tableModel);
    this.deviceTable.setShowHorizontalLines(true);
    this.deviceTable.setShowVerticalLines(true);

    this.deviceTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

//    TableColumnAdjuster tca = new TableColumnAdjuster(this.deviceTable);
//    tca.adjustColumns();    


    this.deviceTable.setToolTipText("Select which device(s) to be used for" + " stitching");

    this.c.gridy = 4;
    JScrollPane scroll = new JScrollPane(this.deviceTable);

    // tie the size of the table to the size of the scroll pane
    Dimension prefSize = this.deviceTable.getPreferredSize();
    prefSize.width = scroll.getPreferredSize().width;
    this.deviceTable.setPreferredScrollableViewportSize(prefSize);
    this.deviceTable.setFillsViewportHeight(true);

    add(scroll, this.c);

  }

  /**
   * Gets the number of CPU threads that the user specified
   *
   * @return the number of CPU threads
   */
  public int getNumCPUThreads() {
    if (this.numThreadsCPU.getValue() <= 0) {
      this.numThreadsCPU.showError();
      return -1;
    }
    return this.numThreadsCPU.getValue();
  }

  @Override
  public void loadParamsIntoGUI(StitchingAppParams params) {
    if (this.isCudaAvailable) {
      this.numThreadsCPU.setValue(params.getAdvancedParams().getNumCPUThreads());
      this.tableModel.updateSelectedDevices(params.getAdvancedParams().getCudaDevices());
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
    if (!this.isCudaAvailable)
      return true;

    int val = getNumCPUThreads();
    List<CudaDeviceParam> cudaDevices = this.tableModel.getSelectedDevices();

    if (val < 1 || cudaDevices == null)
      return false;

    String[][] info = CudaUtils.getTableInformation();

    for (CudaDeviceParam dev : cudaDevices) {
      boolean found = false;
      for (int row = 0; row < info.length; row++) {
        try {
          int id = Integer.parseInt(info[row][CudaUtils.TBL_COL_ID]);
          String name = info[row][CudaUtils.TBL_COL_NAME];

          if (dev.getId() == id && dev.getName().equals(name)) {
            found = true;
            break;
          }

        } catch (NumberFormatException e) {
          Log.msg(LogType.MANDATORY, "Error processing device id" + " from device query");
          return false;
        }

      }

      if (!found) {
        Log.msg(LogType.MANDATORY, "Device not found from device" + " query: " + dev);
        return false;
      }
    }

    return true;
  }

  private boolean loadingParams = false;

  @Override
  public void enableLoadingParams() {
    if (this.isCudaAvailable) {
      this.loadingParams = true;
      this.numThreadsCPU.enableIgnoreErrors();
    }
  }

  @Override
  public void disableLoadingParams() {
    if (this.isCudaAvailable) {
      this.loadingParams = false;
      this.numThreadsCPU.disableIgnoreErrors();
    }
  }

  @Override
  public boolean isLoadingParams() {
    return this.loadingParams;
  }

  @Override
  public void saveParamsFromGUI(StitchingAppParams params, boolean isClosing) {
    if (this.isCudaAvailable) {
      int val = getNumCPUThreads();
      List<CudaDeviceParam> cudaDevices = this.tableModel.getSelectedDevices();

      params.getAdvancedParams().setNumCPUThreads(val);
      params.getAdvancedParams().setCudaDevices(cudaDevices);
    }

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource().equals(this.deviceQuery)) {
      CudaDeviceTableModel.deviceQuery();
    } else if (e.getSource().equals(this.refreshTable)) {
      this.tableModel.refreshModel();
    }
  }

}
