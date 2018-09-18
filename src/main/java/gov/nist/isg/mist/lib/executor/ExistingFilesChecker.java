// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Oct 1, 2014 1:43:18 PM EST
//
// Time-stamp: <Oct 1, 2014 1:43:18 PM tjb3>
//
// ================================================================
package gov.nist.isg.mist.lib.executor;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.gui.params.objects.RangeParam;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import ij.macro.Interpreter;


/**
 * Checks for existing files before execution
 *
 * @author Tim Blattner
 */
public class ExistingFilesChecker implements Comparator<String> {


  private StitchingAppParams params;

  /**
   * Initializes an ExistingFilesChecker
   *
   * @param params the parameters used
   */
  public ExistingFilesChecker(StitchingAppParams params) {
    this.params = params;
  }


  /**
   * Checks for existing files
   *
   * @param displayGui whether to show a gui or not
   * @return true if the user accepts the changes, otherwise false
   */
  public boolean checkExistingFiles(boolean displayGui) {
    List<String> existingOverwrittenFiles = checkOverwriteExistingOutputFiles();

    if (existingOverwrittenFiles.size() > 0) {

      Collections.sort(existingOverwrittenFiles, this);

      Log.msg(LogType.MANDATORY, "Warning: the following files will be overwritten:");
      for (String f : existingOverwrittenFiles)
        Log.msg(LogType.MANDATORY, f);


      if (displayGui && !GraphicsEnvironment.isHeadless() && !Interpreter.isBatchMode()) {

        if (existingOverwrittenFiles.size() > 0) {
          if(!params.getAdvancedParams().isSuppressModelWarningDialog()) {
            if (canOverwriteExistingFilesCheck(existingOverwrittenFiles)) {
              Log.msg(LogType.MANDATORY, "Overwritting files has been approved.");
            } else {
              Log.msg(LogType.MANDATORY, "Overwritting files has not been approved.");
              Log.msg(LogType.MANDATORY,
                      "Please modify your file prefix, meta data directory, or output directory.");
              return false;
            }
          }else{
            Log.msg(LogType.MANDATORY, "Overwritting files has been implicitly approved (model dialog was suppressed).");
          }
        }
      }
    }
    return true;
  }


  private static boolean canOverwriteExistingFilesCheck(List<String> fileList) {
    if (fileList.size() > 0) {
      JPanel panel = new JPanel(new GridBagLayout());

      JList jList = new JList(fileList.toArray());
      JScrollPane scrollPane = new JScrollPane(jList);
      scrollPane.setPreferredSize(new Dimension(500, 200));

      JLabel label = new JLabel("Warning: Would you like to overwrite the files listed above?");


      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 0;
      panel.add(scrollPane, c);

      c.gridy = 1;
      panel.add(label, c);

      int val =
          JOptionPane.showConfirmDialog(null, panel, "Warning: Overwritting files",
              JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null);

      return val == JOptionPane.YES_OPTION;
    }


    return true;
  }

  private List<String> checkOverwriteExistingOutputFiles() {
    List<String> fileList = new ArrayList<String>();

    boolean checkOutputImage = false;
    boolean checkOutputMeta = false;

    if (this.params.getOutputParams().isOutputFullImage()) {
      File outputDir = new File(this.params.getOutputParams().getOutputPath());
      if (outputDir.exists()) {
        checkOutputImage = true;
      }
    }

    if (this.params.getOutputParams().isOutputMeta()) {
      File metaDir = new File(this.params.getOutputParams().getOutputPath());

      if (metaDir.exists()) {
        checkOutputMeta = true;
      }
    }

    File logFile = this.params.getOutputParams().getLogFile();
    if (logFile.exists())
      fileList.add(logFile.getAbsolutePath());


    File statFile = this.params.getOutputParams().getStatsFile();

    if (statFile.exists())
      fileList.add(statFile.getAbsolutePath());

    if (checkOutputImage || checkOutputMeta) {

      List<RangeParam> timeSlices = this.params.getInputParams().getTimeSlices();

      for (RangeParam timeSliceParam : timeSlices) {
        int minTimeSlice = timeSliceParam.getMin();
        int maxTimeSlice = timeSliceParam.getMax();

        for (int timeSlice = minTimeSlice; timeSlice <= maxTimeSlice; timeSlice++) {
          if (checkOutputImage) {
            File imgFile = this.params.getOutputParams().getOutputImageFile(timeSlice);

            if (imgFile.exists())
              fileList.add(imgFile.getAbsolutePath());
          }

          if (checkOutputMeta) {


            File absFile = this.params.getOutputParams().getAbsPosFile(timeSlice);
            if (absFile.exists())
              fileList.add(absFile.getAbsolutePath());

            File relPosFile = this.params.getOutputParams().getRelPosFile(timeSlice);
            if (relPosFile.exists())
              fileList.add(relPosFile.getAbsolutePath());

            File relPosNoOptFile = this.params.getOutputParams().getRelPosNoOptFile(timeSlice);
            if (relPosNoOptFile.exists())
              fileList.add(relPosNoOptFile.getAbsolutePath());

          }

        }
      }

    }
    return fileList;

  }


  @Override
  public int compare(String o1, String o2) {
    try {
      String o1prefix = o1.substring(0, o1.lastIndexOf("-"));
      String o2prefix = o2.substring(0, o2.lastIndexOf("-"));

      // If the prefixes of both strings are the same, then compare ints
      // This maintains grouping while at the same time ordering by value
      if (o1prefix.equals(o2prefix)) {

        String o1Suffix = o1.substring(o1.lastIndexOf("."));
        String o2Suffix = o2.substring(o2.lastIndexOf("."));


        Integer v1 =
            Integer.parseInt(o1.substring(o1.lastIndexOf("-") + 1,
                o1.lastIndexOf(o1Suffix)));
        Integer v2 =
            Integer.parseInt(o2.substring(o2.lastIndexOf("-") + 1,
                o2.lastIndexOf(o2Suffix)));

        return v1.compareTo(v2);
      }
      return o1.compareTo(o2);

    } catch (IndexOutOfBoundsException e) {
      return o1.compareTo(o2);
    } catch (NumberFormatException e) {
      return o1.compareTo(o2);
    }

  }

}
