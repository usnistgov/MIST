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
// Date: Apr 18, 2014 12:46:11 PM EST
//
// Time-stamp: <Apr 18, 2014 12:46:11 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.gui.panels.advancedTab.parallelPanels;

import gov.nist.isg.mist.stitching.gui.params.objects.CudaDeviceParam;
import gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaUtils;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a table that shows all the available CUDA enabled GPUs.
 * 
 * @author Tim Blattner
 * @version 1.0
 * 
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

        if (devFound)
          this.selected[row] = true;
        else
          this.selected[row] = false;
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
