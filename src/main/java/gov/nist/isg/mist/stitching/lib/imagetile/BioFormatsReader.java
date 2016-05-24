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

package gov.nist.isg.mist.stitching.lib.imagetile;

import java.io.File;

import gov.nist.isg.mist.stitching.lib.log.Log;
import ij.ImagePlus;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;

/**
 * Class to hold static methods for reading images with BioFormats library
 *
 * @author Michael Majurski
 */
public class BioFormatsReader {

  public static ImagePlus readImage(String filepath) {
    ImagePlus imp;

    File file = new File(filepath);
    Log.msg(Log.LogType.INFO, "Loading " + file.getName() + " using BioFormats");

    try {
      ImporterOptions options = new ImporterOptions();
      options.setId(file.getAbsolutePath());
      options.setSplitChannels(false);
      options.setSplitTimepoints(false);
      options.setSplitFocalPlanes(false);
      options.setAutoscale(false);
      options.setVirtual(false);

      ImagePlus[] tmp = BF.openImagePlus(options);
      imp = tmp[0];


    } catch (Exception e) {
      Log.msg(Log.LogType.MANDATORY, "Cannot open image using BioFormats");
      return null;
    }


    return imp;
  }
}
