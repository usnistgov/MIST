// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 12:47:58 PM EST
//
// Time-stamp: <Apr 18, 2014 12:47:58 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui.panels.advancedTab.parallelPanels;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import gov.nist.isg.mist.gui.components.textfield.TextFieldInputPanel;
import gov.nist.isg.mist.gui.components.textfield.textFieldModel.IntModel;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.gui.params.interfaces.GUIParamFunctions;
import gov.nist.isg.mist.gui.params.objects.CudaDeviceParam;
import gov.nist.isg.mist.lib.imagetile.jcuda.CudaUtils;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import jcuda.CudaException;

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

  private JCheckBox enableCudaExceptions = new JCheckBox("Enable CUDA Exceptions? (Debugging Only)");

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

  public boolean isCudaExceptionsEnabled() {
    return this.enableCudaExceptions.isSelected();
  }

  private void initControls() {


    this.c.gridy = 1;
    add(this.enableCudaExceptions, this.c);
    this.c.gridy = 2;
    add(this.numThreadsCPU, this.c);
    this.deviceQuery.addActionListener(this);
    this.c.gridy = 3;
    add(this.deviceQuery, this.c);
    this.c.gridy = 4;
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

    this.c.gridy = 5;
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
      this.enableCudaExceptions.setSelected(params.getAdvancedParams().isEnableCudaExceptions());
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
      params.getAdvancedParams().setEnableCudaExceptions(this.enableCudaExceptions.isSelected());
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
