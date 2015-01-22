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
// Date: Oct 1, 2014 1:43:18 PM EST
//
// Time-stamp: <Oct 1, 2014 1:43:18 PM tjb3>
//
// ================================================================
package main.gov.nist.isg.mist.stitching.gui.executor;

import ij.macro.Interpreter;

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

import main.gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import main.gov.nist.isg.mist.stitching.gui.params.objects.RangeParam;
import main.gov.nist.isg.mist.stitching.lib.log.Log;
import main.gov.nist.isg.mist.stitching.lib.log.Log.LogType;


/**
 * Checks for existing files before execution
 * @author Tim Blattner
 *
 */
public class ExistingFilesChecker implements Comparator<String> {



  private StitchingAppParams params;

  /**
   * Initializes an ExistingFilesChecker
   * @param params the parameters used
   */
  public ExistingFilesChecker(StitchingAppParams params)
  {
    this.params = params;
  }


  /**
   * Checks for existing files
   * @param displayGui whether to show a gui or not
   * @return true if the user accepts the changes, otherwise false
   */
  public boolean checkExistingFiles(boolean displayGui)
  {
    List<String> existingOverwrittenFiles = checkOverwriteExistingOutputFiles();

    if (existingOverwrittenFiles.size() > 0) {
      
      Collections.sort(existingOverwrittenFiles, this);

      Log.msg(LogType.MANDATORY, "Warning: the following files will be overwritten:");
      for (String f : existingOverwrittenFiles)
        Log.msg(LogType.MANDATORY, f);


      if (displayGui && !GraphicsEnvironment.isHeadless() && !Interpreter.isBatchMode()) {

        if (existingOverwrittenFiles.size() > 0) {
          if (canOverwriteExistingFilesCheck(existingOverwrittenFiles)) {
            Log.msg(LogType.MANDATORY, "Overwritting files has been approved.");          
          } else {
            Log.msg(LogType.MANDATORY, "Overwritting files has not been approved.");
            Log.msg(LogType.MANDATORY,
                "Please modify your file prefix, meta data directory, or output directory.");
            return false;
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


      if (val == JOptionPane.YES_OPTION) {
        return true;
      }
      return false;
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
      File metaDir = new File(this.params.getOutputParams().getMetadataPath());

      if (metaDir.exists()) {
        checkOutputMeta = true;
      }
    }

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
