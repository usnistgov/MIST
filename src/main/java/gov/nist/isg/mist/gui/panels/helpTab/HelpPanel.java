// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 12:53:27 PM EST
//
// Time-stamp: <Apr 18, 2014 12:53:27 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui.panels.helpTab;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import gov.nist.isg.mist.gui.components.helpDialog.HelpDocumentationViewer;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.gui.params.interfaces.GUIParamFunctions;

/**
 * Creates the help panel
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class HelpPanel extends JPanel implements GUIParamFunctions {

  private static final String documentationURL =
      "https://github.com/USNISTGOV/MIST/wiki";

  private static final String sourceURL =
      "https://github.com/USNISTGOV/MIST";


  private static final String aboutUsURL =
      "https://isg.nist.gov";

  private static final String testDatasetURL =
      "https://github.com/USNISTGOV/MIST#sample-data-sets";

  private static final String license =
      "<html>This software was developed at the National Institute of Standards and<br>" +
          "Technology by employees of the Federal Government in the course of<br>" +
          "their official duties. Pursuant to title 17 Section 105 of the United<br>" +
          "States Code this software is not subject to copyright protection and is<br>" +
          "in the public domain. This software is an experimental system. NIST<br>" +
          "assumes no responsibility whatsoever for its use by other parties, and<br>" +
          "makes no guarantees, expressed or implied, about its quality, reliability,<br>" +
          "or any other characteristic. We would appreciate acknowledgement if the<br>" +
          "software is used.</html>";


  private static final long serialVersionUID = 1L;


  private JButton openLocalHelp;


  /**
   * Initializes the help panel
   */
  public HelpPanel() {

    this.openLocalHelp = new JButton("Open Local Help Documentation");
    HelpDocumentationViewer helpDialog = new HelpDocumentationViewer("mist-user-guide");
    this.openLocalHelp.addActionListener(helpDialog);


    this.openLocalHelp.setPreferredSize(new Dimension(220, 40));

    setFocusable(false);

    initControls();

  }


  private void initControls() {
    JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    JPanel vertPanel = new JPanel(new GridBagLayout());

    GridBagConstraints c = new GridBagConstraints();


    c.gridy = 0;
    c.insets = new Insets(10, 10, 0, 0);
    c.anchor = GridBagConstraints.FIRST_LINE_START;

    c.fill = GridBagConstraints.HORIZONTAL;


    JPanel helpButtonPanel = new JPanel();
    helpButtonPanel.add(this.openLocalHelp);
    vertPanel.add(helpButtonPanel, c);


    c.gridy = 1;
    JLabel aboutUsLink = new JLabel("<html><a href=\"" + aboutUsURL + "\">" + "About MIST" + "</a></html>");
    aboutUsLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
    vertPanel.add(aboutUsLink, c);

    c.gridy = 2;
    JLabel link = new JLabel("<html><a href=\"" + documentationURL + "\">" + "Online Documentation" + "</a></html>");

    link.setCursor(new Cursor(Cursor.HAND_CURSOR));
    vertPanel.add(link, c);

    c.gridy = 3;
    JLabel srclink = new JLabel("<html><a href=\"" + sourceURL + "\">" + "Source Code" + "</a></html>");

    srclink.setCursor(new Cursor(Cursor.HAND_CURSOR));
    vertPanel.add(srclink, c);

    c.gridy = 4;
    JLabel examplesLink = new JLabel("<html><a href=\"" + testDatasetURL + "\">" + "Sample Datasets" + "</a></html>");
    examplesLink.setCursor(new Cursor(Cursor.HAND_CURSOR));

    vertPanel.add(examplesLink, c);


    c.gridy = 5;
    vertPanel.add(new JLabel(license), c);

    mainPanel.add(vertPanel);


    add(mainPanel);

    goWebsiteDocumentation(link);
    goWebsiteSourceCode(srclink);
    goWebsiteAboutUs(aboutUsLink);
    goWebsiteExamples(examplesLink);

  }

  private static void goWebsiteDocumentation(JLabel website) {
    website.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        try {
          Desktop.getDesktop().browse(new URI(documentationURL));
        } catch (URISyntaxException ex) {
        } catch (IOException ex) {

        }
      }
    });
  }

  private static void goWebsiteSourceCode(JLabel website) {
    website.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        try {
          Desktop.getDesktop().browse(new URI(sourceURL));
        } catch (URISyntaxException ex) {
        } catch (IOException ex) {

        }
      }
    });
  }

  private static void goWebsiteAboutUs(JLabel website) {
    website.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        try {
          Desktop.getDesktop().browse(new URI(aboutUsURL));
        } catch (URISyntaxException ex) {
        } catch (IOException ex) {

        }
      }
    });
  }

  private static void goWebsiteExamples(JLabel website) {
    website.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        try {
          Desktop.getDesktop().browse(new URI(testDatasetURL));
        } catch (URISyntaxException ex) {
        } catch (IOException ex) {

        }
      }
    });
  }


  @Override
  public void loadParamsIntoGUI(StitchingAppParams params) {
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
    return true;

  }

  private boolean loadingParams = false;

  @Override
  public void enableLoadingParams() {
    this.loadingParams = true;
  }

  @Override
  public void disableLoadingParams() {
    this.loadingParams = false;

  }

  @Override
  public boolean isLoadingParams() {
    return this.loadingParams;
  }

  @Override
  public void saveParamsFromGUI(StitchingAppParams params, boolean isClosing) {
  }


}
