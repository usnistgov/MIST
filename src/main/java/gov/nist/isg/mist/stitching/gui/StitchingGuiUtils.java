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


// ================================================================
//
// Author: tjb3
// Date: Apr 25, 2014 3:51:23 PM EST
//
// Time-stamp: <Apr 25, 2014 3:51:23 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.gui;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;

/**
 * Utility class for managing a progress bar
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class StitchingGuiUtils {

  private static String figureLoc = "/figs/";
  private static String documentationLoc = "/docs/";


  /**
   * Loads a compressed resource (handles files inside jar such as html or pdf) The result is a
   * temporary file that will be deleted once the jvm exits
   *
   * @param file      the file to load from resource
   * @param extension the extension of the temporary file
   * @return a temporary file that is deleted on jvm exit (or null if error)
   */
  public static File loadCompressedResource(String file, String extension) {
    InputStream is = null;
    OutputStream os = null;
    File out = null;

    try {
      is = StitchingGuiUtils.class.getResourceAsStream(documentationLoc + file);

      out = File.createTempFile(file, extension);
      out.deleteOnExit();
      os = new FileOutputStream(out);

      final byte[] buf = new byte[1024];
      int len = 0;
      while ((len = is.read(buf)) > 0) {
        os.write(buf, 0, len);
      }

      os.flush();

    } catch (Exception e) {
      Log.msg(LogType.MANDATORY, "Error openning help file: " + e.getMessage());
    } finally {
      try {
        if (os != null)
          os.close();

        if (is != null)
          is.close();


      } catch (IOException e) {
        Log.msg(LogType.MANDATORY, "Error closing help file");
      }
    }

    return out;

  }

  public static URL getFigureResource(String file) {
    return StitchingGuiUtils.class.getResource(figureLoc + file);
  }

  /**
   * Updates the progress bar
   *
   * @param progressBar   the progress bar
   * @param indeterminate whether the progress bar is indeterminite or not
   * @param progressStr   the progress bar string
   */
  public static void updateProgressBar(JProgressBar progressBar, boolean indeterminate,
                                       String progressStr) {
    updateProgressBar(progressBar, indeterminate, progressStr, null, 0, 0, 0, false);
  }

  /**
   * Updates the progress bar to a state that is completed
   *
   * @param progressBar the progress bar
   */
  public static void updateProgressBarCompleted(final JProgressBar progressBar) {
    updateProgressBar(progressBar, false, "Completed", null, 0, 0, 0, true);
  }

  /**
   * Updates the progress bar
   *
   * @param progressBar   the progress bar
   * @param indeterminate whetheer the progress bar is indeterminite or not
   * @param progressStr   the progress bar string
   * @param title         the title of the progress bar
   * @param min           the minimum value for the progress
   * @param max           the maximum value for the progress
   * @param val           the current value of the progress
   * @param emptyBorder   whether to show an empty border or not
   */
  public static void updateProgressBar(final JProgressBar progressBar, final boolean indeterminate,
                                       final String progressStr, final String title, final int min, final int max, final int val,
                                       final boolean emptyBorder) {
    if (progressBar == null)
      return;

    try {
      EventQueue.invokeAndWait(new Runnable() {
        @Override
        public void run() {
          progressBar.setIndeterminate(indeterminate);
          progressBar.setString(progressStr);
          if (title != null) {
            progressBar.setMinimum(min);
            progressBar.setMaximum(max);
            progressBar.setValue(val);

            TitledBorder borderTitle;
            borderTitle = BorderFactory.createTitledBorder(title);
            borderTitle.setTitleJustification(TitledBorder.CENTER);
            progressBar.setBorder(borderTitle);
          } else if (emptyBorder) {
            progressBar.setBorder(BorderFactory.createEmptyBorder());
          }

        }
      });
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Increments the progress bar by one
   *
   * @param progressBar the progress bar
   */
  public static void incrementProgressBar(final JProgressBar progressBar) {
    if (progressBar == null)
      return;

    EventQueue.invokeLater(new Runnable() {

      @Override
      public void run() {
        progressBar.firePropertyChange("progress", progressBar.getValue(),
            progressBar.getValue() + 1);
      }
    });
  }

  public static void updateProgressLabel(JLabel progressLabel, int curTimeSlice, int maxTimeSlice, int curGroup, int maxGroup) {
    if (progressLabel == null)
      return;

    progressLabel.setText("<html>Progress:" + "<br>Time slice: " + curTimeSlice + " of "
        + maxTimeSlice + "<br>Group: " + curGroup + " of " + maxGroup + "</html>");
  }


}
