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
public class HelpPanel extends JPanel implements GUIParamFunctions, ActionListener {

  private static final String documentationURL =
      "https://github.com/NIST-ISG/MIST/wiki";

    private static final String sourceURL =
            "https://github.com/NIST-ISG/MIST";

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

  private JComboBox loggingLevel;
  private JComboBox debugLevel;

  /**
   * Initializes the help panel
   */
  public HelpPanel() {

    this.openLocalHelp = new JButton("Open Local Help");
      HelpDocumentationViewer helpDialog = new HelpDocumentationViewer("mist-user-guide");
      this.openLocalHelp.addActionListener(helpDialog);


    this.loggingLevel = new JComboBox(LogType.values());
    this.debugLevel = new JComboBox(DebugType.values());


    this.loggingLevel.setSelectedItem(Log.getLogLevel());
    this.debugLevel.setSelectedItem(Debug.getDebugLevel());

    this.openLocalHelp.setPreferredSize(new Dimension(150, 40));

    setFocusable(false);

    initControls();
    initListeners();

  }
  
  private void initListeners()
  {
    this.loggingLevel.addActionListener(this);
    this.debugLevel.addActionListener(this);
  }

  private void initControls() {
    JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    JPanel vertPanel = new JPanel(new GridBagLayout());

    GridBagConstraints c = new GridBagConstraints();

    
    JPanel logPanel = new JPanel(new GridBagLayout());

    this.loggingLevel = new JComboBox(LogType.values());
    c.gridy = 0;
    c.anchor = GridBagConstraints.LINE_END;
    logPanel.add(new JLabel("Log Level"), c);
    c.gridy = 1;
    logPanel.add(this.loggingLevel, c);

    this.debugLevel = new JComboBox(DebugType.values());
    c.gridy = 0;

    logPanel.add(new JLabel("Debug Level"), c);
    c.gridy = 1;
    c.insets = new Insets(0, 10, 0, 0);
    logPanel.add(this.debugLevel, c);


    c.gridy = 0;
    c.insets = new Insets(10, 10, 0, 0);
    c.anchor = GridBagConstraints.FIRST_LINE_START;    
    vertPanel.add(logPanel, c);
    
    
    c.fill = GridBagConstraints.HORIZONTAL;

    c.gridy = 1;
      JLabel link = new JLabel("<html>Documentation: <a href=\"\">" + documentationURL + "</a></html>");

    link.setCursor(new Cursor(Cursor.HAND_CURSOR));
    vertPanel.add(link, c);

      c.gridy = 2;
      JLabel srclink = new JLabel("<html>Source code: <a href=\"\">" + sourceURL + "</a></html>");

      srclink.setCursor(new Cursor(Cursor.HAND_CURSOR));
      vertPanel.add(srclink, c);

    c.gridy = 3;
    JPanel helpButtonPanel = new JPanel();
    helpButtonPanel.add(this.openLocalHelp);
    vertPanel.add(helpButtonPanel, c);

      c.gridy = 4;
      vertPanel.add(new JLabel(license), c);

    mainPanel.add(vertPanel);




    add(mainPanel);

    goWebsiteDocumentation(link);
      goWebsiteSourceCode(srclink);

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

  /**
   * Gets the log level
   * @return the log level
   */
  public LogType getLogLevel() {
    return (LogType)this.loggingLevel.getSelectedItem();
  }
  
  /**
   * Gets the debug level
   * @return the debug level
   */
  public DebugType getDebugLevel() {
    return (DebugType)this.debugLevel.getSelectedItem();
  }
  
  @Override
  public void loadParamsIntoGUI(StitchingAppParams params) {
    LogType logType = params.getLogParams().getLogLevel();
    
    if (logType == null)
      logType = LogType.MANDATORY;
    
    DebugType debugType = params.getLogParams().getDebugLevel();
    
    if (debugType == null)
      debugType = DebugType.NONE;
    
    this.loggingLevel.setSelectedItem(logType);
    this.debugLevel.setSelectedItem(debugType);    
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
    LogType logLevel = this.getLogLevel();
    DebugType debugLevel = this.getDebugLevel();
   
    params.getLogParams().setLogLevel(logLevel);
    params.getLogParams().setDebugLevel(debugLevel);
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    Object src = arg0.getSource();
      if (src instanceof JComboBox) {
      JComboBox action = (JComboBox) src;

      if (action.equals(this.loggingLevel)) {
        Log.setLogLevel((LogType)action.getSelectedItem());
      } else if (action.equals(this.debugLevel)) {
        Debug.setDebugLevel((DebugType)action.getSelectedItem());
      }
    } 



  }
}
