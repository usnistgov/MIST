// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 23, 2014 1:42:13 PM EST
//
// Time-stamp: <Apr 23, 2014 1:42:13 PM tjb3>
//
//
// ================================================================
package gov.nist.isg.mist.gui.panels.inputTab;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import gov.nist.isg.mist.gui.images.AppImageHelper;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoader;

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

    TileGridLoader.LoaderType filenameLoaderType = (TileGridLoader.LoaderType) this.gridType.getSelectedItem();

    if (filenameLoaderType.equals(TileGridLoader.LoaderType.ROWCOL)) {
      origin = origin.replace(" ", "");
      this.fileName = "RowCol_" + origin + ".png";
    } else {
      // this is a sequential file loader
      origin = origin.replace(" ", "");
      numbering = numbering.replace(" ", "");
      this.fileName = origin + "_" + numbering + ".png";
    }


    try {
//      this.icon = AppImageHelper.loadImage(this.fileName, 300, 200);
      this.icon = AppImageHelper.loadImage(this.fileName, 240, 160);
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
