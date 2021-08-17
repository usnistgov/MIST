// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Oct 1, 2014 1:46:41 PM EST
//
// Time-stamp: <Oct 1, 2014 1:46:41 PM tjb3>
//
// ================================================================
package gov.nist.isg.mist.gui.params.utils;

import java.util.List;

import gov.nist.isg.mist.gui.params.objects.CudaDeviceParam;
import gov.nist.isg.mist.gui.params.objects.RangeParam;
import gov.nist.isg.mist.lib.executor.StitchingExecutor.StitchingType;
import gov.nist.isg.mist.lib.export.LargeImageExporter.BlendingMode;
import gov.nist.isg.mist.lib.export.MicroscopyUnits;
import gov.nist.isg.mist.lib.imagetile.Stitching.TranslationRefinementType;
import gov.nist.isg.mist.lib.imagetile.fftw.FftwPlanType;
import gov.nist.isg.mist.lib.log.Debug.DebugType;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoader.GridDirection;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoader.GridOrigin;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoader.LoaderType;
import ij.Macro;
import ij.plugin.frame.Recorder;


/**
 * Macro utility functions
 *
 * @author Tim Blattner
 */
public class MacroUtils {

  /**
   * Loads a macro integer
   *
   * @param options the macro options
   * @param key     the key value
   * @param def     the default value
   * @return the loaded integer
   */
  public static int loadMacroInteger(String options, String key, int def) {
    try {
      return Integer.parseInt(Macro.getValue(options, key.toLowerCase(), Integer.toString(def)));
    } catch (NumberFormatException ex) {
      Log.msg(LogType.MANDATORY, "Error parsing macro: " + key + " must be an integer");
      return def;
    }
  }

  /**
   * Loads a macro double
   *
   * @param options the macro options
   * @param key     the key value
   * @param def     the default value
   * @return the loaded double
   */
  public static double loadMacroDouble(String options, String key, double def) {
    try {
      return Double.parseDouble(Macro.getValue(options, key.toLowerCase(), Double.toString(def)));
    } catch (NumberFormatException ex) {
      Log.msg(LogType.MANDATORY, "Error parsing macro: " + key + " must be a double");
      return def;
    }
  }

  /**
   * Loads a macro String
   *
   * @param options the macro options
   * @param key     the key value
   * @param def     the default value
   * @return the loaded String
   */
  public static String loadMacroString(String options, String key, String def) {
    return Macro.getValue(options, key.toLowerCase(), def);
  }

  /**
   * Loads a macro origin enum
   *
   * @param options the macro options
   * @param key     the key value
   * @param def     the default value
   * @return the loaded origin enum
   */
  public static GridOrigin loadMacroGridOrigin(String options, String key, String def) {
    String res = Macro.getValue(options, key.toLowerCase(), def);

    GridOrigin origin = GridOrigin.valueOf(res.toUpperCase());

    if (origin == null) {
      Log.msg(LogType.MANDATORY, "Error parsing macro: " + key + " must be valid GridOrigin");

    }

    return origin;

  }

  /**
   * Loads a macro numbering enum
   *
   * @param options the macro options
   * @param key     the key value
   * @param def     the default value
   * @return the loaded numbering enum
   */
  public static GridDirection loadMacroGridNumbering(String options, String key, String def) {
    String res = Macro.getValue(options, key.toLowerCase(), def);

    GridDirection numbering = GridDirection.valueOf(res.toUpperCase());

    if (numbering == null) {
      Log.msg(LogType.MANDATORY, "Error parsing macro: " + key + " must be valid direction");

    }

    return numbering;

  }

  /**
   * Loads a macro loader type enum
   *
   * @param options the macro options
   * @param key     the key value
   * @param def     the default value
   * @return the loaded loader type enum
   */
  public static LoaderType loadMacroLoaderType(String options, String key, String def) {
    String res = Macro.getValue(options, key.toLowerCase(), def);
    LoaderType loader = LoaderType.valueOf(res.toUpperCase());

    if (loader == null) {
      Log.msg(LogType.MANDATORY, "Error parsing macro: " + key + " must be valid LoaderType");
    }

    return loader;
  }

  /**
   * Loads a macro boolean
   *
   * @param options the macro options
   * @param key     the key value
   * @param def     the default value
   * @return the loaded boolean
   */
  public static boolean loadMacroBoolean(String options, String key, boolean def) {
    try {
      return Boolean
          .parseBoolean(Macro.getValue(options, key.toLowerCase(), Boolean.toString(def)));
    } catch (NumberFormatException ex) {
      Log.msg(LogType.MANDATORY, "Error parsing macro: " + key + " must be a boolean");
      return def;
    }
  }

  /**
   * Loads a macro timeslices
   *
   * @param options the macro options
   * @param key     the key value
   * @return the loaded timeslices
   */
  public static List<RangeParam> loadMacroTimeslices(String options, String key) {
    String res = Macro.getValue(options, key.toLowerCase(), "");

    return RangeParam.parseTimeSlices(res);
  }

  /**
   * Loads a macro program type enum
   *
   * @param options the macro options
   * @param key     the key value
   * @param def     the default value
   * @return the loaded program type enum
   */
  public static StitchingType loadMacroProgramType(String options, String key, String def) {
    String res = Macro.getValue(options, key.toLowerCase(), def);

    StitchingType progType = StitchingType.valueOf(res.toUpperCase());

    if (progType == null) {
      Log.msg(LogType.MANDATORY, "Error parsing macro: " + key + " must be valid StitchingType");
    }

    return progType;
  }

  /**
   * Loads a macro translation refinement type enum
   *
   * @param options the macro options
   * @param key     the key value
   * @param def     the default value
   * @return the loaded program type enum
   */
  public static TranslationRefinementType loadTranslationRefinementType(String options, String key, String def) {
    String res = Macro.getValue(options, key.toLowerCase(), def);

    TranslationRefinementType type = TranslationRefinementType.valueOf(res.toUpperCase());

    if (type == null) {
      Log.msg(LogType.MANDATORY, "Error parsing macro: " + key + " must be valid TranslationRefinementType");
    }

    return type;
  }

  /**
   * Loads a macro plan type
   *
   * @param options the macro options
   * @param key     the key value
   * @param def     the default value
   * @return the loaded plan type enum
   */
  public static FftwPlanType loadMacroFFTWPlanType(String options, String key, String def) {
    String res = Macro.getValue(options, key.toLowerCase(), def);

    FftwPlanType type = FftwPlanType.valueOf(res.toUpperCase());

    if (type == null) {
      Log.msg(LogType.MANDATORY, "Error parsing macro: " + key + " must be valid FFTWPlanType");
    }

    return type;
  }

  /**
   * Loads a macro blending mode enum
   *
   * @param options the macro options
   * @param key     the key value
   * @param def     the default value
   * @return the loaded blending mode enum
   */
  public static BlendingMode loadMacroBlendingModeType(String options, String key, String def) {
    String res = Macro.getValue(options, key.toLowerCase(), def);

    BlendingMode type = BlendingMode.valueOf(res.toUpperCase());

    if (type == null) {
      Log.msg(LogType.MANDATORY, "Error parsing macro: " + key + " must be valid BlendingMode");
    }

    return type;
  }


  /**
   * Loads a macro microscopy unit enum
   *
   * @param options the macro options
   * @param key     the key value
   * @param def     the default value
   * @return the loaded blending mode enum
   */
  public static MicroscopyUnits loadMacroMicroscopyUnits(String options, String key, String def) {
    String res = Macro.getValue(options, key.toLowerCase(), def);

    MicroscopyUnits type = MicroscopyUnits.valueOf(res.toUpperCase());

    if (type == null) {
      Log.msg(LogType.MANDATORY, "Error parsing macro: " + key + " must be valid MicroscopyUnits");
    }

    return type;
  }

  /**
   * Loads a macro log type enum
   *
   * @param options the macro options
   * @param key     the key value
   * @param def     the default value
   * @return the loaded log type enum
   */
  public static LogType loadMacroLogType(String options, String key, String def) {
    String res = Macro.getValue(options, key.toLowerCase(), def);

    LogType type = LogType.valueOf(res.toUpperCase());

    if (type == null) {
      Log.msg(LogType.MANDATORY, "Error parsing macro: " + key + " must be valid LogType");
    }

    return type;
  }

  /**
   * Loads a macro debug type enum
   *
   * @param options the macro options
   * @param key     the key value
   * @param def     the default value
   * @return the loaded debug type enum
   */
  public static DebugType loadMacroDebugType(String options, String key, String def) {
    String res = Macro.getValue(options, key.toLowerCase(), def);

    DebugType type = DebugType.valueOf(res.toUpperCase());

    if (type == null) {
      Log.msg(LogType.MANDATORY, "Error parsing macro: " + key + " must be valid DebugType");
    }

    return type;
  }


  /**
   * Loads a macro Cuda devices
   *
   * @param options the macro options
   * @param dev     the cuda device number
   * @return the loaded Cuda devices
   */
  public static CudaDeviceParam loadMacroCUDADevice(String options, int dev) {
    String device = "cudadevice" + dev;

    String idStr = Macro.getValue(options, device + "id", null);
    if (idStr == null)
      return null;

    String devName = Macro.getValue(options, device + "name", null);
    if (devName == null)
      return null;

    String majorStr = Macro.getValue(options, device + "major", null);
    if (majorStr == null)
      return null;

    String minorStr = Macro.getValue(options, device + "minor", null);
    if (minorStr == null)
      return null;

    try {
      int id = Integer.parseInt(idStr);
      int major = Integer.parseInt(majorStr);
      int minor = Integer.parseInt(minorStr);

      return new CudaDeviceParam(id, devName, minor, major);
    } catch (NumberFormatException e) {
      Log.msg(LogType.MANDATORY, "Error parsing macro: CUDA device id, "
          + "major, and minor must be integers");
      return null;
    }
  }


  /**
   * Records an integer into a macro
   *
   * @param key the key for the macro
   * @param val the value for the macro
   */
  public static void recordInteger(String key, int val) {
    Recorder.recordOption(key, Integer.toString(val));
  }

  /**
   * Records an double into a macro
   *
   * @param key the key for the macro
   * @param val the value for the macro
   */
  public static void recordDouble(String key, double val) {
    Recorder.recordOption(key, Double.toString(val));
  }

  /**
   * Records an string into a macro
   *
   * @param key the key for the macro
   * @param val the value for the macro
   */
  public static void recordString(String key, String val) {
    if (val.equals(""))
      val = "[]";
    if (val.length() >= 3 && Character.isLetter(val.charAt(0))
        && val.charAt(1) == ':' && val.charAt(2) == '\\')
      val = val.replaceAll("\\\\", "\\\\\\\\"); // replace "\" with "\\" in Windows file paths
    Recorder.recordOption(key, val);
  }

  /**
   * Records an boolean into a macro
   *
   * @param key the key for the macro
   * @param val the value for the macro
   */
  public static void recordBoolean(String key, boolean val) {
    Recorder.recordOption(key, Boolean.toString(val));
  }

  /**
   * Records an timeslices into a macro
   *
   * @param timeSlices the time clies
   */
  public static void recordTimeslices(List<RangeParam> timeSlices) {
    String timeSliceStr = "";
    for (int i = 0; i < timeSlices.size(); i++) {
      if (i == timeSlices.size() - 1)
        timeSliceStr += timeSlices.get(i);
      else
        timeSliceStr += timeSlices.get(i) + ",";
    }
    Recorder.recordOption("timeSlices", timeSliceStr);
  }

  /**
   * Records an Cuda devices into a macro
   *
   * @param cudaDevices the cuda devices
   */
  public static void recordCUDADevices(List<CudaDeviceParam> cudaDevices) {
    if (cudaDevices != null) {
      for (int i = 0; i < cudaDevices.size(); i++) {
        CudaDeviceParam dev = cudaDevices.get(i);

        Recorder.recordOption("cudaDevice" + i + "id", Integer.toString(dev.getId()));
        Recorder.recordOption("cudaDevice" + i + "name", dev.getName());
        Recorder.recordOption("cudaDevice" + i + "major", Integer.toString(dev.getDevMajor()));
        Recorder.recordOption("cudaDevice" + i + "minor", Integer.toString(dev.getDevMinor()));

      }
    }
  }


}
