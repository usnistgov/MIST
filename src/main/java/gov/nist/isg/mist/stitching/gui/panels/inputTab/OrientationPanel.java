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
// Date: Apr 23, 2014 1:42:13 PM EST
//
// Time-stamp: <Apr 23, 2014 1:42:13 PM tjb3>
//
//
// ================================================================
package gov.nist.isg.mist.stitching.gui.panels.inputTab;

import gov.nist.isg.mist.stitching.gui.images.AppImageHelper;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;

/**
 * Adds the orientation icons into a JPanel
 * 
 * @author Tim Blattner
 * @version 1.0
 */
public class OrientationPanel extends JPanel implements ActionListener {

  private static final long serialVersionUID = 1L;
  private JComboBox originComponent;
  private JComboBox gridNumberingComponent;
  private JComboBox gridType;
  private String fileName;

  private JLabel picLabel;
  private ImageIcon icon;

  /**
   * Initializes the panel and hooks into the orientation parameters
   * 
   * @param inputPanel the input panel
   */
  public OrientationPanel(InputPanel inputPanel) {
    super(new BorderLayout());
    this.originComponent = inputPanel.getOriginComponent();
    this.gridNumberingComponent = inputPanel.getGridNumberingComponent();
    this.gridType = inputPanel.getFilenamePatternType();

    updateFilename();

    this.gridNumberingComponent.addActionListener(this);
    this.originComponent.addActionListener(this);

    add(this.picLabel, BorderLayout.CENTER);

  }

  private void updateFilename() {
    String origin = this.originComponent.getSelectedItem().toString();
    String numbering = this.gridNumberingComponent.getSelectedItem().toString();

    TileGridLoader.LoaderType filenameLoaderType = (TileGridLoader.LoaderType)this.gridType.getSelectedItem();

    if(filenameLoaderType.equals(TileGridLoader.LoaderType.ROWCOL)) {
      origin = origin.replace(" ", "");
      this.fileName = "RowCol_" + origin + ".png";
    }else{
      // this is a sequential file loader
      origin = origin.replace(" ", "");
      numbering = numbering.replace(" ", "");
      this.fileName = origin + "_" + numbering + ".png";
    }


    try {
      this.icon = AppImageHelper.loadImage(this.fileName, 300, 200);
    } catch (FileNotFoundException e) {
      Log.msg(LogType.MANDATORY, "ERROR: Orentation panel image file not found.");
    }
    if (this.picLabel == null) {
      this.picLabel = new JLabel(this.icon);
    } else {
      this.picLabel.setIcon(this.icon);
    }
    this.repaint();

  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    updateFilename();
  }
}
