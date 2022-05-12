// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Oct 1, 2014 1:51:04 PM EST
//
// Time-stamp: <Oct 1, 2014 1:51:04 PM tjb3>
//
// ================================================================
package gov.nist.isg.mist.gui.params.utils;

import java.util.List;
import java.util.prefs.Preferences;

import gov.nist.isg.mist.gui.params.objects.CudaDeviceParam;
import gov.nist.isg.mist.gui.params.objects.RangeParam;
import gov.nist.isg.mist.lib.executor.StitchingExecutor.StitchingType;
import gov.nist.isg.mist.lib.export.BlendingMode;
import gov.nist.isg.mist.lib.export.CompressionMode;
import gov.nist.isg.mist.lib.export.MicroscopyUnits;
import gov.nist.isg.mist.lib.imagetile.Stitching.TranslationRefinementType;
import gov.nist.isg.mist.lib.imagetile.fftw.FftwPlanType;
import gov.nist.isg.mist.lib.log.Debug.DebugType;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoader.GridDirection;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoader.GridOrigin;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoader.LoaderType;

/**
 * Preferences utility functions
 *
 * @author Tim Blattner
 */
public class PreferencesUtils {

  /**
   * Loads the grid origin from preferences
   *
   * @param pref the preferences
   * @param key  the key value
   * @param def  the default value
   * @return the grid origin
   */
  public static GridOrigin loadPrefGridOrigin(Preferences pref, String key, String def) {
    String res = pref.get(key, def);

    GridOrigin origin = null;

    try {
      origin = GridOrigin.valueOf(res.toUpperCase());
    } catch (IllegalArgumentException e) {
    }

    if (origin == null) {
      Log.msg(LogType.MANDATORY, "Error parsing preferences: " + key + " must be valid GridOrigin");
      origin = GridOrigin.UL;
    }

    return origin;

  }

  /**
   * Loads the grid numbering from preferences
   *
   * @param pref the preferences
   * @param key  the key value
   * @param def  the default value
   * @return the grid numbering
   */
  public static GridDirection loadPrefGridNumbering(Preferences pref, String key, String def) {
    String res = pref.get(key, def);

    GridDirection numbering = null;

    try {
      numbering = GridDirection.valueOf(res.toUpperCase());
    } catch (IllegalArgumentException e) {
    }
    if (numbering == null) {
      Log.msg(LogType.MANDATORY, "Error parsing preferences: " + key + " must be valid Direction");
      numbering = GridDirection.HORIZONTALCOMBING;
    }

    return numbering;

  }

  /**
   * Loads the time slices from preferences
   *
   * @param pref the preferences
   * @param key  the key
   * @return the list of time slices
   */
  public static List<RangeParam> loadPrefTimeslices(Preferences pref, String key) {
    String res = pref.get(key, "");

    return RangeParam.parseTimeSlices(res);
  }

  /**
   * Loads the program type from preferences
   *
   * @param pref the preferences
   * @param key  the key value
   * @param def  the default value
   * @return the program type
   */
  public static StitchingType loadPrefProgramType(Preferences pref, String key, String def) {
    String res = pref.get(key, def);

    StitchingType progType = StitchingType.valueOf(res.toUpperCase());

    if (progType == null) {
      Log.msg(LogType.MANDATORY, "Error parsing preferences: " + key + " must be valid StitchingType");
    }

    return progType;
  }

  /**
   * Loads the translation refinement type from preferences
   *
   * @param pref the preferences
   * @param key  the key value
   * @param def  the default value
   * @return the program type
   */
  public static TranslationRefinementType loadPrefTransRefineType(Preferences pref,
                                                                  String key, String def) {
    String res = pref.get(key, def);

    TranslationRefinementType type = TranslationRefinementType.valueOf(res.toUpperCase());

    if (type == null)
      Log.msg(LogType.MANDATORY, "Error parsing preferences: " + key + " must be valid TranslationRefinementType");

    return type;
  }

  /**
   * Loads the fftw plan type from preferences
   *
   * @param pref the preferences
   * @param key  the key value
   * @param def  the default value
   * @return the fftw plan type
   */
  public static FftwPlanType loadPrefFFTWPlanType(Preferences pref, String key, String def) {
    String res = pref.get(key, def);

    FftwPlanType type = FftwPlanType.valueOf(res.toUpperCase());

    if (type == null) {
      Log.msg(LogType.MANDATORY, "Error parsing preferences: " + key + " must be valid FFTWPlanType");
    }

    return type;
  }

  /**
   * Loads the blending mode from preferences
   *
   * @param pref the preferences
   * @param key  the key value
   * @param def  the default value
   * @return the blending mode
   */
  public static BlendingMode loadPrefBlendingModeType(Preferences pref, String key, String def) {
    String res = pref.get(key, def);

    BlendingMode type = BlendingMode.valueOf(res.toUpperCase());

    if (type == null) {
      Log.msg(LogType.MANDATORY, "Error parsing preferences: " + key + " must be valid BlendingMode");
    }

    return type;
  }

  /**
   * Loads microscopy units
   * @param pref the preferences
   * @param key the key value
   * @param def the default value
   * @return the microscopy unit
   */
  public static MicroscopyUnits loadPrefMicroscopyUnitsType(Preferences pref, String key, String def) {
    String res = pref.get(key, def);

    MicroscopyUnits unit = MicroscopyUnits.valueOf(res.toUpperCase());
    if (unit == null) {
      Log.msg(LogType.MANDATORY, "Error parsing preferences: " + key + " must be valid MicroscopyUnit");
    }

    return unit;
  }

  /**
   * Loads compression mode
   * @param pref the preferences
   * @param key the key value
   * @param def the default value
   * @return the compression mode
   */
  public static CompressionMode loadPrefCompressionMode(Preferences pref, String key, String def) {
    String res = pref.get(key, def);

    CompressionMode mode = CompressionMode.valueOf(res.toUpperCase());
    if (mode == null) {
      Log.msg(LogType.MANDATORY, "Error parsing preferences: " + key + " must be valid CompressionMode");
    }

    return mode;
  }

  /**
   * Loads the logging type from preferences
   *
   * @param pref the preferences
   * @param key  the key value
   * @param def  the default value
   * @return the global optimization
   */
  public static LogType loadPrefLogType(Preferences pref, String key, String def) {
    String res = pref.get(key, def);

    LogType type = LogType.valueOf(res.toUpperCase());

    if (type == null) {
      Log.msg(LogType.MANDATORY, "Error parsing preferences: " + key +
          " must be valid LogType");
    }

    return type;
  }

  /**
   * Loads the debug type from preferences
   *
   * @param pref the preferences
   * @param key  the key value
   * @param def  the default value
   * @return the global optimization
   */
  public static DebugType loadPrefDebugType(Preferences pref, String key, String def) {
    String res = pref.get(key, def);

    DebugType type = DebugType.valueOf(res.toUpperCase());

    if (type == null) {
      Log.msg(LogType.MANDATORY, "Error parsing preferences: " + key +
          " must be valid DebugType");
    }

    return type;
  }

  /**
   * Loads the file pattern loader type from preferences
   *
   * @param pref the preferences
   * @param key  the key value
   * @param def  the default value
   * @return the file pattern loader type
   */
  public static LoaderType loadFilePatternLoaderType(Preferences pref, String key, String def) {
    String res = pref.get(key, def);

    LoaderType type = LoaderType.valueOf(res.toUpperCase());

    if (type == null) {
      Log.msg(LogType.MANDATORY, "Error parsing preferences: " + key + " must be valid LoaderType");
    }

    return type;
  }

  /**
   * Loads the time slices from preferences
   *
   * @param pref the preferences
   * @param dev  the Cuda device id
   * @return the Cuda device param
   */
  public static CudaDeviceParam loadPrefCUDADevice(Preferences pref, int dev) {
    String device = "cudaDevice" + dev;

    String idStr = pref.get(device + "id", null);
    if (idStr == null)
      return null;

    String devName = pref.get(device + "name", null);
    if (devName == null)
      return null;

    String majorStr = pref.get(device + "major", null);
    if (majorStr == null)
      return null;

    String minorStr = pref.get(device + "minor", null);
    if (minorStr == null)
      return null;

    try {
      int id = Integer.parseInt(idStr);
      int major = Integer.parseInt(majorStr);
      int minor = Integer.parseInt(minorStr);

      return new CudaDeviceParam(id, devName, minor, major);
    } catch (NumberFormatException e) {
      Log.msg(LogType.MANDATORY, "Error parsing preferences: CUDA device id, "
          + "major, and minor must be integers");
      return null;
    }
  }

  /**
   * Records timeslices to preferences
   *
   * @param pref       the preferences
   * @param timeSlices the list of time slices
   */
  public static void recordPrefTimeslices(Preferences pref, List<RangeParam> timeSlices) {
    String timeSliceStr = "";
    for (int i = 0; i < timeSlices.size(); i++) {
      if (i == timeSlices.size() - 1)
        timeSliceStr += timeSlices.get(i);
      else
        timeSliceStr += timeSlices.get(i) + ",";
    }
    pref.put("timeSlices", timeSliceStr);
  }

  /**
   * Records Cuda devices to preferences
   *
   * @param pref        the preferences
   * @param cudaDevices the cuda device param
   */
  public static void recordPrefCUDADevices(Preferences pref, List<CudaDeviceParam> cudaDevices) {
    if (cudaDevices != null) {
      for (int i = 0; i < cudaDevices.size(); i++) {
        CudaDeviceParam dev = cudaDevices.get(i);

        pref.put("cudaDevice" + i + "id", Integer.toString(dev.getId()));
        pref.put("cudaDevice" + i + "name", dev.getName());
        pref.put("cudaDevice" + i + "major", Integer.toString(dev.getDevMajor()));
        pref.put("cudaDevice" + i + "minor", Integer.toString(dev.getDevMinor()));
      }
    }
  }

}
