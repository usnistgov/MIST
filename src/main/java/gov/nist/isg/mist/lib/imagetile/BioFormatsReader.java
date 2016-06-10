// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.


package gov.nist.isg.mist.lib.imagetile;

import java.io.File;

import gov.nist.isg.mist.lib.log.Log;
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
