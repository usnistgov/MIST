// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Nov 18, 2014 11:26:37 AM EST
//
// Time-stamp: <Nov 18, 2014 11:26:37 AM tjb3>
//
// ================================================================

package gov.nist.isg.mist.gui.components.helpDialog;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import gov.nist.isg.mist.gui.StitchingGuiUtils;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;

/**
 * Simple help documentation class that opens the HTML help documentation in the O/S default web
 * browswer
 *
 * @author tjb3
 */
public class HelpDocumentationViewer implements ActionListener {


  private static final String helpDocumentation = "Combined-User-Install-Guide.md.html";
  private static boolean isMainDocumentationLoaded = false;
  private static File mainDocumentationTempFile;

  private File htmlAncorLoader;
  private String tag;

  /**
   * Constructs the viewer
   *
   * @param tag the html tag to point to
   */
  public HelpDocumentationViewer(String tag) {
    this.tag = tag;
    try {
      this.htmlAncorLoader = File.createTempFile(tag, ".html");
      this.htmlAncorLoader.deleteOnExit();
    } catch (IOException e) {
      Log.msg(LogType.MANDATORY, "Error creating temporary documentation file: " + e.getMessage());
    }


    if (!isMainDocumentationLoaded)
      mainDocumentationTempFile = StitchingGuiUtils.loadCompressedResource(helpDocumentation, ".html");

    createHtmlAncorLoader();

  }

  @Override
  public void actionPerformed(ActionEvent arg0) {

    if (Desktop.isDesktopSupported()) {
      try {

        Desktop.getDesktop().browse(this.htmlAncorLoader.toURI());
      } catch (IOException e) {
        Log.msg(LogType.MANDATORY, "Error: IOException - " + e.getMessage());
      }
    }
  }

  private void createHtmlAncorLoader() {
    String contents = "<html><head><meta http-equiv=\"refresh\" content=\"0;url="
        + mainDocumentationTempFile.getName() + "#" + this.tag + "\" /></head></html>";

    try {
      FileWriter writer = new FileWriter(this.htmlAncorLoader);
      writer.write(contents);
      writer.close();
    } catch (IOException e) {
      Log.msg(LogType.MANDATORY, "Error writing to temporary file: " + e.getMessage());
    }

  }

}

