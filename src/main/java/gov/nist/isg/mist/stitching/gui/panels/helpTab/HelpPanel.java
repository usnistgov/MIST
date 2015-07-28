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
// Date: Apr 18, 2014 12:53:27 PM EST
//
// Time-stamp: <Apr 18, 2014 12:53:27 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.gui.panels.helpTab;

import gov.nist.isg.mist.stitching.gui.components.helpDialog.HelpDocumentationViewer;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.gui.params.interfaces.GUIParamFunctions;
import gov.nist.isg.mist.stitching.lib.log.Debug;
import gov.nist.isg.mist.stitching.lib.log.Debug.DebugType;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Creates the help panel
 * 
 * @author Tim Blattner
 * @version 1.0
 * 
 */
public class HelpPanel extends JPanel implements GUIParamFunctions {

  private static final String documentationURL =
      "https://github.com/NIST-ISG/MIST/wiki";

    private static final String sourceURL =
            "https://github.com/NIST-ISG/MIST";


    private static final String aboutUsURL =
            "https://isg.nist.gov/deepzoomweb/resources/csmet/pages/image_stitching/image_stitching.html";

    private static final String testDatasetURL =
            "https://github.com/NIST-ISG/MIST#sample-data-sets";

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
      JLabel srclink = new JLabel("<html><a href=\"" + sourceURL +"\">" + "Source Code" + "</a></html>");

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
  public void loadParamsIntoGUI(StitchingAppParams params) { }

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
  public void saveParamsFromGUI(StitchingAppParams params, boolean isClosing) { }


}
