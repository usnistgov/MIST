// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 12:46:11 PM EST
//
// Time-stamp: <Apr 18, 2014 12:46:11 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui.panels.advancedTab.parallelPanels;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import gov.nist.isg.mist.gui.params.objects.CudaDeviceParam;
import gov.nist.isg.mist.lib.imagetile.jcuda.CudaUtils;

/**
 * Creates a table that shows all the available CUDA enabled GPUs.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class CudaDeviceTableModel extends AbstractTableModel {

  private static final long serialVersionUID = 1L;

  private String[][] info;
  private boolean[] selected;

  private String[] headers = {"Selected?", "ID", " Name", "Compute Capability"};


  /**
   * Initializes the table
   */
  public CudaDeviceTableModel() {
    this.info = CudaUtils.getTableInformation();

    initSelected();
  }

  /**
   * Checks the devices that were selected in the past
   *
   * @param selectedDevices the selected devices
   */
  public void updateSelectedDevices(List<CudaDeviceParam> selectedDevices) {
    if (selectedDevices == null)
      return;

    for (int row = 0; row < this.info.length; row++) {
      String id = this.info[row][CudaUtils.TBL_COL_ID];
      String name = this.info[row][CudaUtils.TBL_COL_NAME];

      boolean devFound = false;

      if (id != null && name != null) {
        for (CudaDeviceParam dev : selectedDevices) {

          if (id.equals(Integer.toString(dev.getId())) && name.equals(dev.getName())) {
            devFound = true;
            break;
          }
        }

        this.selected[row] = devFound;
      }

    }

    this.fireTableDataChanged();
  }

  /**
   * Gets a list of the devices that the user selected
   *
   * @return the list of selected devices
   */
  public List<CudaDeviceParam> getSelectedDevices() {
    List<CudaDeviceParam> devices = new ArrayList<CudaDeviceParam>();
    for (int row = 0; row < this.info.length; row++) {
      if (this.selected[row]) {
        int id = Integer.parseInt(this.info[row][CudaUtils.TBL_COL_ID]);
        String name = this.info[row][CudaUtils.TBL_COL_NAME];
        String majMinor = this.info[row][CudaUtils.TBL_COL_CAPABIL];

        String[] majMinorSplit = majMinor.split("\\.");
        int minor = Integer.parseInt(majMinorSplit[1]);
        int major = Integer.parseInt(majMinorSplit[0]);

        devices.add(new CudaDeviceParam(id, name, minor, major));
      }
    }

    return devices;
  }

  /**
   * Initializes the selected devices
   */
  public void initSelected() {
    if (this.info.length > 0)
      this.selected = new boolean[this.info.length];
    else
      this.selected = null;

  }

  /**
   * Refreshes the table
   */
  public void refreshModel() {
    this.info = CudaUtils.getTableInformation();
    initSelected();
    this.fireTableDataChanged();
  }

  /**
   * Runs a device query that prints to the console
   */
  public static void deviceQuery() {
    CudaUtils.deviceQuery();
  }

  @Override
  public Class<?> getColumnClass(int column) {
    Class<?> clazz = String.class;
    switch (column) {
      case 0:
        clazz = Boolean.class;
        break;
    }

    return clazz;
  }

  @Override
  public boolean isCellEditable(int row, int column) {
    return column == 0;

  }

  @Override
  public void setValueAt(Object aValue, int row, int column) {
    if (aValue instanceof Boolean && column == CudaUtils.TBL_COL_SELECTED) {
      this.selected[row] = (Boolean) aValue;
      fireTableCellUpdated(row, column);
    }
  }

  @Override
  public int getColumnCount() {
    if (this.info.length > 0)
      return this.info[0].length;
    return 0;
  }

  @Override
  public int getRowCount() {
    return this.info.length;
  }

  @Override
  public Object getValueAt(int row, int col) {
    if (this.info == null || this.info.length == 0 || this.selected == null)
      return null;
    else if (col == CudaUtils.TBL_COL_SELECTED)
      return this.selected[row];
    else
      return this.info[row][col];
  }

  @Override
  public String getColumnName(int col) {
    return this.headers[col];
  }

}
