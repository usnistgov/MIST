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

package main.gov.nist.isg.mist.stitching.gui.panels.helpTab;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import main.gov.nist.isg.mist.stitching.gui.StitchingGuiUtils;
import main.gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import main.gov.nist.isg.mist.stitching.gui.params.interfaces.GUIParamFunctions;
import main.gov.nist.isg.mist.stitching.lib.log.Debug;
import main.gov.nist.isg.mist.stitching.lib.log.Log;
import main.gov.nist.isg.mist.stitching.lib.log.Debug.DebugType;
import main.gov.nist.isg.mist.stitching.lib.log.Log.LogType;

/**
 * Creates the help panel
 * 
 * @author Tim Blattner
 * @version 1.0
 * 
 */
public class HelpPanel extends JPanel implements GUIParamFunctions, ActionListener {

  private static final String aboutUsText = "MIST Fiji Plugin:";
  private static final String helpDocumentationStr = "StitchingDocumentation.pdf";

  private static final String documentationURL =
      "http://bluegrit.cs.umbc.edu/~tblatt1/NISTStitching/UserGuide/index.html";

  private static final long serialVersionUID = 1L;

  private JLabel aboutUs;

  private JButton openPdfHelpButton;

  private JComboBox loggingLevel;
  private JComboBox debugLevel;

  /**
   * Initializes the help panel
   */
  public HelpPanel() {
    this.aboutUs = new JLabel(aboutUsText);

    this.openPdfHelpButton = new JButton("Open Help (PDF)");


    this.loggingLevel = new JComboBox(LogType.values());
    this.debugLevel = new JComboBox(DebugType.values());


    this.loggingLevel.setSelectedItem(Log.getLogLevel());
    this.debugLevel.setSelectedItem(Debug.getDebugLevel());

    this.openPdfHelpButton.setPreferredSize(new Dimension(150, 40));    

    setFocusable(false);

    initControls();
    initListeners();

  }
  
  private void initListeners()
  {
    this.openPdfHelpButton.addActionListener(this);
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
    vertPanel.add(this.aboutUs, c);


    c.gridy = 2;
    vertPanel.add(new JLabel("Created at the National Institute of Standards and Technology."), c);


    c.gridy = 3;
    vertPanel.add(new JLabel("Documentation:"), c);

    c.gridy = 4;
    JLabel link = new JLabel("<html><a href=\"\">" + documentationURL + "</a></html>");
    link.setCursor(new Cursor(Cursor.HAND_CURSOR));
    vertPanel.add(link, c);

    c.gridy = 5;
    JPanel helpButtonPanel = new JPanel();
    helpButtonPanel.add(this.openPdfHelpButton);
    vertPanel.add(helpButtonPanel, c);
    mainPanel.add(vertPanel);

    add(mainPanel);

    goWebsite(link);

  }

  private static void goWebsite(JLabel website) {
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

    if (src.equals(this.openPdfHelpButton))
    {

      if (Desktop.isDesktopSupported())
      {
        File file = StitchingGuiUtils.loadCompressedResource(helpDocumentationStr, ".pdf");          

        try {
          Desktop.getDesktop().open(file);
        } catch (IOException e) {
          Log.msg(LogType.MANDATORY, "Error loading help documentation: " + e.getMessage());

        }
      }
      else
      {
        Log.msg(LogType.MANDATORY, "Error: Unsupported desktop for openning pdf");
      }      
    }

    else if (src instanceof JComboBox) {
      JComboBox action = (JComboBox) src;

      if (action.equals(this.loggingLevel)) {
        Log.setLogLevel((LogType)action.getSelectedItem());
      } else if (action.equals(this.debugLevel)) {
        Debug.setDebugLevel((DebugType)action.getSelectedItem());
      }
    } 



  }
}
