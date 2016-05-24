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
// Date: Oct 1, 2014 1:51:04 PM EST
//
// Time-stamp: <Oct 1, 2014 1:51:04 PM tjb3>
//
// ================================================================
package gov.nist.isg.mist.stitching.gui.params.utils;

import java.util.List;
import java.util.prefs.Preferences;

import gov.nist.isg.mist.stitching.gui.params.objects.CudaDeviceParam;
import gov.nist.isg.mist.stitching.gui.params.objects.RangeParam;
import gov.nist.isg.mist.stitching.lib.executor.StitchingExecutor.StitchingType;
import gov.nist.isg.mist.stitching.lib.export.LargeImageExporter.BlendingMode;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching.TranslationRefinementType;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwPlanType;
import gov.nist.isg.mist.stitching.lib.log.Debug.DebugType;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridDirection;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridOrigin;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.LoaderType;

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
