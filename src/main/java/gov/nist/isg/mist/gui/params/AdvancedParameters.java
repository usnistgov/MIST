// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Oct 1, 2014 1:55:52 PM EST
//
// Time-stamp: <Oct 1, 2014 1:55:52 PM tjb3>
//
// ================================================================
package gov.nist.isg.mist.gui.params;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import gov.nist.isg.mist.MISTMain;
import gov.nist.isg.mist.gui.params.interfaces.StitchingAppParamFunctions;
import gov.nist.isg.mist.gui.params.objects.CudaDeviceParam;
import gov.nist.isg.mist.gui.params.utils.MacroUtils;
import gov.nist.isg.mist.gui.params.utils.PreferencesUtils;
import gov.nist.isg.mist.gui.params.utils.StitchingParamUtils;
import gov.nist.isg.mist.lib.executor.StitchingExecutor.StitchingType;
import gov.nist.isg.mist.lib.imagetile.Stitching;
import gov.nist.isg.mist.lib.imagetile.Stitching.TranslationRefinementType;
import gov.nist.isg.mist.lib.imagetile.fftw.FftwPlanType;
import gov.nist.isg.mist.lib.imagetile.jcuda.CudaUtils;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;

/**
 * AdvancedParameters are the advanced parameters for Stitching
 *
 * @author Tim Blattner
 */
public class AdvancedParameters implements StitchingAppParamFunctions {

  private static final String PROGRAM_TYPE = "programType";
  private static final String NUM_CPU_THREADS = "numCPUThreads";
  private static final String LOAD_FFTW_PLAN = "loadFFTWPlan";
  private static final String SAVE_FFTW_PLAN = "saveFFTWPlan";
  private static final String FFTW_PLAN_TYPE = "fftwPlanType";
  private static final String PLAN_PATH = "planPath";
  private static final String FFTW_LIBRARY_PATH = "fftwLibraryPath";
  private static final String FFTW_LIBRARY_NAME = "fftwLibraryName";
  private static final String FFTW_LIBRARY_FILENAME = "fftwLibraryFilename";
  private static final String CUDA_DEVICE = "cudaDevice";
  private static final String STAGE_REPEATABILITY = "stageRepeatability";
  private static final String HORIZONTAL_OVERLAP = "horizontalOverlap";
  private static final String VERTICAL_OVERLAP = "verticalOverlap";
  private static final String NUM_FFT_PEAKS = "numFFTPeaks";
  private static final String OVERLAP_UNCERTAINTY = "overlapUncertainty";
  private static final String IS_USE_DOUBLE_PRECISION = "isUseDoublePrecision";
  private static final String IS_USE_BIOFORMATS = "isUseBioFormats";
  private static final String IS_ENABLE_CUDA_EXCEPTIONS = "isEnableCudaExceptions";
  private static final String TRANSLATION_REFINEMENT_TYPE = "translationRefinementMethod";
  private static final String NUM_TRANS_REFINEMENT_START_POINTS =
      "numTranslationRefinementStartPoints";
  private static final String RUN_HEADLESS = "headless";


  private StitchingType programType;
  private int numCPUThreads;

  // FFTW Options
  private boolean loadFFTWPlan;
  private boolean saveFFTWPlan;
  private FftwPlanType fftwPlanType;
  private String planPath;
  private String fftwLibraryPath;
  private String fftwLibraryName;
  private String fftwLibraryFileName;

  // CUDA Options
  private List<CudaDeviceParam> cudaDevices;

  // Global Optimization
  private TranslationRefinementType translationRefinementType = TranslationRefinementType
      .SINGLE_HILL_CLIMB;
  private int numTranslationRefinementStartPoints = 16;


  // Advanced options
  private int stageRepeatability;
  private double horizontalOverlap;
  private double verticalOverlap;
  private int numFFTPeaks;
  private double overlapUncertainty;
  private boolean useDoublePrecision;
  private boolean useBioFormats;
  private boolean enableCudaExceptions;

  public AdvancedParameters() {
    this.programType = StitchingType.AUTO;
    this.numCPUThreads = Runtime.getRuntime().availableProcessors();

    // FFTW Options
    this.loadFFTWPlan = true;
    this.saveFFTWPlan = true;
    this.fftwPlanType = FftwPlanType.MEASURE;
    this.planPath =
        System.getProperty("user.dir") + File.separator + "lib" + File.separator + "fftw"
            + File.separator + "fftPlans";
    this.fftwLibraryPath =
        System.getProperty("user.dir") + File.separator + "lib" + File.separator + "fftw";
    this.fftwLibraryName = "libfftw3";
    this.fftwLibraryFileName = "libfftw3.dll";
    File f = new File(this.planPath);
    f.mkdirs();

    // CUDA Options
    this.cudaDevices = new ArrayList<CudaDeviceParam>();


    // Advanced options
    this.stageRepeatability = 0;
    this.horizontalOverlap = Double.NaN;
    this.verticalOverlap = Double.NaN;
    this.numFFTPeaks = 0;
    this.overlapUncertainty = Double.NaN;
    this.translationRefinementType = Stitching.TranslationRefinementType.SINGLE_HILL_CLIMB;
  }

  @Override
  public boolean checkParams() {
    return checkParallelParams();
  }

  private boolean checkParallelParams() {
    if (this.numCPUThreads <= 0 || this.programType == null)
      return false;
    boolean check = false;
    switch (this.programType) {
      case AUTO:
        check = true;
        break;
      case JAVA:
        check = checkJavaParams();
        break;
      case CUDA:
        check = checkCUDAParams();
        break;
      case FFTW:
        check = checkFFTWParams();
        break;
      default:
        break;

    }
    return check;
  }


  private boolean checkCUDAParams() {
    String[][] info = CudaUtils.getTableInformation();

    if (this.cudaDevices == null) {
      Log.msg(LogType.MANDATORY, "No CUDA device selected");
      return false;
    }

    if (info == null || info.length == 0) {
      Log.msg(LogType.MANDATORY, "No CUDA devices detected");
      return false;
    }

    for (CudaDeviceParam dev : this.cudaDevices) {
      boolean found = false;
      for (int row = 0; row < info.length; row++) {
        try {
          int id = Integer.parseInt(info[row][CudaUtils.TBL_COL_ID]);
          String name = info[row][CudaUtils.TBL_COL_NAME];

          if (dev.getId() == id && dev.getName().equals(name)) {
            found = true;
            break;
          }

        } catch (NumberFormatException e) {
          Log.msg(LogType.MANDATORY, "Error processing device id" + " from device query");
          return false;
        }

      }

      if (!found) {
        Log.msg(LogType.MANDATORY, "Device not found from device query: " + dev);
        return false;
      }
    }

    return true;
  }

  private boolean checkFFTWParams() {
    if (this.planPath != null && this.fftwPlanType != null && this.fftwLibraryPath != null
        && this.fftwLibraryName != null && this.fftwLibraryFileName != null) {
      return true;
    }
    return false;
  }

  private boolean checkJavaParams() {
    return this.numCPUThreads > 0;
  }


  @Override
  public boolean loadParams(File file) {
    try {
      boolean noErrors = true;

      Log.msg(LogType.MANDATORY, "Loading advanced parameters");

      FileReader fr = new FileReader(file.getAbsolutePath());

      BufferedReader br = new BufferedReader(fr);


      String line = null;
      while ((line = br.readLine()) != null) {
        String[] contents = line.split(":", 2);

        if (contents.length > 1) {
          try {
            loadParameter(contents[0],contents[1]);
          } catch (IllegalArgumentException e) {
            Log.msg(LogType.MANDATORY, "Unable to parse line: " + line);
            Log.msg(LogType.MANDATORY, "Error parsing advanced option: " + e.getMessage());
            noErrors = false;
          }
        }
      }

      br.close();

      fr.close();

      return noErrors;

    } catch (IOException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }
    return false;
  }

  /**
   * Load the value into the parameter defined by key.
   * @param key the parameter name to overwrite with value
   * @param value the value to save into the parameter defined by key
   */
  public void loadParameter(String key, String value) {
    key = key.trim();
    value = value.trim();

    if (key.equals(PROGRAM_TYPE))
      this.programType = StitchingType.valueOf(value.toUpperCase());
    else if (key.equals(NUM_CPU_THREADS))
      this.numCPUThreads = StitchingParamUtils.loadInteger(value, this.numCPUThreads);
    else if (key.equals(LOAD_FFTW_PLAN))
      this.loadFFTWPlan = StitchingParamUtils.loadBoolean(value, this.loadFFTWPlan);
    else if (key.equals(FFTW_PLAN_TYPE))
      this.fftwPlanType = FftwPlanType.valueOf(value.toUpperCase());
    else if (key.equals(FFTW_LIBRARY_NAME))
      this.fftwLibraryName = value;
    else if (key.equals(PLAN_PATH))
      this.planPath = value;
    else if (key.equals(FFTW_LIBRARY_PATH))
      this.fftwLibraryPath = value;
    else if (key.equals(FFTW_LIBRARY_FILENAME))
      this.fftwLibraryFileName = value;
    else if (key.equals(SAVE_FFTW_PLAN))
      this.saveFFTWPlan = StitchingParamUtils.loadBoolean(value, this.saveFFTWPlan);
    else if (key.equals(STAGE_REPEATABILITY))
      this.stageRepeatability = StitchingParamUtils.loadInteger(value, this.stageRepeatability);
    else if (key.equals(HORIZONTAL_OVERLAP))
      this.horizontalOverlap = StitchingParamUtils.loadDouble(value, this.horizontalOverlap);
    else if (key.equals(VERTICAL_OVERLAP))
      this.verticalOverlap = StitchingParamUtils.loadDouble(value, this.verticalOverlap);
    else if (key.equals(NUM_FFT_PEAKS))
      this.numFFTPeaks = StitchingParamUtils.loadInteger(value, this.numFFTPeaks);
    else if (key.equals(OVERLAP_UNCERTAINTY))
      this.overlapUncertainty = StitchingParamUtils.loadDouble(value, this.overlapUncertainty);
    else if (key.equals(IS_USE_DOUBLE_PRECISION))
      this.useDoublePrecision = StitchingParamUtils.loadBoolean(value, this.useDoublePrecision);
    else if (key.equals(IS_USE_BIOFORMATS))
      this.useBioFormats = StitchingParamUtils.loadBoolean(value, this.useBioFormats);
    else if (key.equals(IS_ENABLE_CUDA_EXCEPTIONS))
      this.enableCudaExceptions = StitchingParamUtils.loadBoolean(value, this.enableCudaExceptions);
    else if (key.equals(TRANSLATION_REFINEMENT_TYPE))
      this.translationRefinementType = TranslationRefinementType.valueOf
          (value.toUpperCase());
    else if (key.equals(NUM_TRANS_REFINEMENT_START_POINTS))
      this.numTranslationRefinementStartPoints = StitchingParamUtils.loadInteger
          (value, this.numTranslationRefinementStartPoints);
    else if (key.equals(RUN_HEADLESS))
      MISTMain.runHeadless = true;
    else if (key.startsWith(CUDA_DEVICE)) {
      if (this.cudaDevices == null)
        this.cudaDevices = new ArrayList<CudaDeviceParam>();

      String[] cudaDevice = value.split(",");
      if (cudaDevice.length == 4) {
        try {
          int id = Integer.parseInt(cudaDevice[0].trim());
          String name = cudaDevice[1];
          int major = Integer.parseInt(cudaDevice[2].trim());
          int minor = Integer.parseInt(cudaDevice[3].trim());

          this.cudaDevices.add(new CudaDeviceParam(id, name, minor, major));
        } catch (NumberFormatException e) {
          Log.msg(LogType.MANDATORY, "Error parsing config: " + key + " = " + value);
        }
      } else {
        Log.msg(LogType.MANDATORY, "Error parsing config: " + key + " = " + value);
      }
    }

  }

  @Override
  public boolean loadParams(Preferences pref) {

    this.programType = PreferencesUtils.loadPrefProgramType(pref, PROGRAM_TYPE, this.programType.name());
    this.numCPUThreads = pref.getInt(NUM_CPU_THREADS, this.numCPUThreads);
    this.loadFFTWPlan = pref.getBoolean(LOAD_FFTW_PLAN, this.loadFFTWPlan);
    this.fftwPlanType = PreferencesUtils.loadPrefFFTWPlanType(pref, FFTW_PLAN_TYPE, this.fftwPlanType.name());
    this.fftwLibraryName = pref.get(FFTW_LIBRARY_NAME, this.fftwLibraryName);
    this.planPath = pref.get(PLAN_PATH, this.planPath);
    this.fftwLibraryPath = pref.get(FFTW_LIBRARY_PATH, this.fftwLibraryPath);
    this.fftwLibraryFileName = pref.get(FFTW_LIBRARY_FILENAME, this.fftwLibraryFileName);
    this.saveFFTWPlan = pref.getBoolean(SAVE_FFTW_PLAN, this.saveFFTWPlan);
    this.stageRepeatability = pref.getInt(STAGE_REPEATABILITY, this.stageRepeatability);
    this.horizontalOverlap = pref.getDouble(HORIZONTAL_OVERLAP, this.horizontalOverlap);
    this.verticalOverlap = pref.getDouble(VERTICAL_OVERLAP, this.verticalOverlap);
    this.numFFTPeaks = pref.getInt(NUM_FFT_PEAKS, this.numFFTPeaks);
    this.overlapUncertainty = pref.getDouble(OVERLAP_UNCERTAINTY, this.overlapUncertainty);
    this.useDoublePrecision = pref.getBoolean(IS_USE_DOUBLE_PRECISION, this.useDoublePrecision);
    this.useBioFormats = pref.getBoolean(IS_USE_BIOFORMATS, this.useBioFormats);
    this.enableCudaExceptions = pref.getBoolean(IS_ENABLE_CUDA_EXCEPTIONS, this.enableCudaExceptions);
    this.translationRefinementType = PreferencesUtils.loadPrefTransRefineType(pref,
        TRANSLATION_REFINEMENT_TYPE, this.translationRefinementType.name());
//    MISTMain.runHeadless = pref.getBoolean(RUN_HEADLESS, MISTMain.runHeadless);
    this.numTranslationRefinementStartPoints = pref.getInt(NUM_TRANS_REFINEMENT_START_POINTS,
        this.numTranslationRefinementStartPoints);

    if (this.cudaDevices == null)
      this.cudaDevices = new ArrayList<CudaDeviceParam>();

    int devNum = 0;

    while (devNum < 100) {
      CudaDeviceParam dev = PreferencesUtils.loadPrefCUDADevice(pref, devNum);
      if (dev == null)
        break;

      this.cudaDevices.add(dev);
      devNum++;
    }

    return true;
  }

  @Override
  public void printParams(LogType logLevel) {
    Log.msg(logLevel, PROGRAM_TYPE + ": " + this.programType);
    Log.msg(logLevel, NUM_CPU_THREADS + ": " + this.numCPUThreads);
    Log.msg(logLevel, LOAD_FFTW_PLAN + ": " + this.loadFFTWPlan);
    Log.msg(logLevel, SAVE_FFTW_PLAN + ": " + this.saveFFTWPlan);
    Log.msg(logLevel, FFTW_PLAN_TYPE + ": " + this.fftwPlanType);
    Log.msg(logLevel, FFTW_LIBRARY_NAME + ": " + this.fftwLibraryName);
    Log.msg(logLevel, FFTW_LIBRARY_FILENAME + ": " + this.fftwLibraryFileName);
    Log.msg(logLevel, PLAN_PATH + ": " + this.planPath);
    Log.msg(logLevel, FFTW_LIBRARY_PATH + ": " + this.fftwLibraryPath);

    Log.msg(logLevel, STAGE_REPEATABILITY + ": " + this.stageRepeatability);
    Log.msg(logLevel, HORIZONTAL_OVERLAP + ": " + this.horizontalOverlap);
    Log.msg(logLevel, VERTICAL_OVERLAP + ": " + this.verticalOverlap);
    Log.msg(logLevel, NUM_FFT_PEAKS + ": " + this.numFFTPeaks);
    Log.msg(logLevel, OVERLAP_UNCERTAINTY + ": " + this.overlapUncertainty);
    Log.msg(logLevel, IS_USE_DOUBLE_PRECISION + ": " + this.useDoublePrecision);
    Log.msg(logLevel, IS_USE_BIOFORMATS + ": " + this.useBioFormats);
    Log.msg(logLevel, IS_ENABLE_CUDA_EXCEPTIONS + ": " + this.enableCudaExceptions);
    Log.msg(logLevel, TRANSLATION_REFINEMENT_TYPE + ": " + this.translationRefinementType);
    Log.msg(logLevel, NUM_TRANS_REFINEMENT_START_POINTS + ": " + this.numTranslationRefinementStartPoints);
    Log.msg(logLevel, RUN_HEADLESS + ": " + MISTMain.runHeadless);


    if (this.cudaDevices != null) {
      for (CudaDeviceParam dev : this.cudaDevices) {
        Log.msg(logLevel, "cudaDevice: " + dev);
      }
    }
  }

  @Override
  public void loadMacro(String macroOptions) {
    this.programType = MacroUtils.loadMacroProgramType(macroOptions, PROGRAM_TYPE, this.programType.name());
    this.numCPUThreads = MacroUtils.loadMacroInteger(macroOptions, NUM_CPU_THREADS, this.numCPUThreads);
    this.loadFFTWPlan = MacroUtils.loadMacroBoolean(macroOptions, LOAD_FFTW_PLAN, this.loadFFTWPlan);
    this.fftwPlanType = MacroUtils.loadMacroFFTWPlanType(macroOptions, FFTW_PLAN_TYPE, this.fftwPlanType.name());
    this.fftwLibraryName = MacroUtils.loadMacroString(macroOptions, FFTW_LIBRARY_NAME, this.fftwLibraryName);
    this.planPath = MacroUtils.loadMacroString(macroOptions, PLAN_PATH, this.planPath);
    this.fftwLibraryPath = MacroUtils.loadMacroString(macroOptions, FFTW_LIBRARY_PATH, this.fftwLibraryPath);
    this.fftwLibraryFileName = MacroUtils.loadMacroString(macroOptions, FFTW_LIBRARY_FILENAME, this.fftwLibraryFileName);
    this.saveFFTWPlan = MacroUtils.loadMacroBoolean(macroOptions, SAVE_FFTW_PLAN, this.saveFFTWPlan);
    this.stageRepeatability = MacroUtils.loadMacroInteger(macroOptions, STAGE_REPEATABILITY, this.stageRepeatability);
    this.horizontalOverlap = MacroUtils.loadMacroDouble(macroOptions, HORIZONTAL_OVERLAP, this.horizontalOverlap);
    this.verticalOverlap = MacroUtils.loadMacroDouble(macroOptions, VERTICAL_OVERLAP, this.verticalOverlap);
    this.numFFTPeaks = MacroUtils.loadMacroInteger(macroOptions, NUM_FFT_PEAKS, this.numFFTPeaks);
    this.overlapUncertainty = MacroUtils.loadMacroDouble(macroOptions, OVERLAP_UNCERTAINTY, this.overlapUncertainty);
    this.useDoublePrecision = MacroUtils.loadMacroBoolean(macroOptions, IS_USE_DOUBLE_PRECISION, this.useDoublePrecision);
    this.useBioFormats = MacroUtils.loadMacroBoolean(macroOptions, IS_USE_BIOFORMATS, this.useBioFormats);
    this.enableCudaExceptions = MacroUtils.loadMacroBoolean(macroOptions, IS_ENABLE_CUDA_EXCEPTIONS, this.enableCudaExceptions);
    this.translationRefinementType = MacroUtils.loadTranslationRefinementType(macroOptions,
        TRANSLATION_REFINEMENT_TYPE, this.translationRefinementType.name());
    MISTMain.runHeadless = MacroUtils.loadMacroBoolean(macroOptions, RUN_HEADLESS, MISTMain.runHeadless);
    this.numTranslationRefinementStartPoints = MacroUtils.loadMacroInteger(macroOptions,
        NUM_TRANS_REFINEMENT_START_POINTS, this.numTranslationRefinementStartPoints);

    if (this.cudaDevices == null)
      this.cudaDevices = new ArrayList<CudaDeviceParam>();

    int devNum = 0;

    while (devNum < 100) {
      CudaDeviceParam dev = MacroUtils.loadMacroCUDADevice(macroOptions, devNum);
      if (dev == null)
        break;

      this.cudaDevices.add(dev);
      devNum++;
    }
  }

  @Override
  public void recordMacro() {
    MacroUtils.recordString(PROGRAM_TYPE + ": ", this.programType.name());
    MacroUtils.recordInteger(NUM_CPU_THREADS + ": ", this.numCPUThreads);
    MacroUtils.recordBoolean(LOAD_FFTW_PLAN + ": ", this.loadFFTWPlan);
    MacroUtils.recordBoolean(SAVE_FFTW_PLAN + ": ", this.saveFFTWPlan);
    MacroUtils.recordString(FFTW_PLAN_TYPE + ": ", this.fftwPlanType.name());
    MacroUtils.recordString(FFTW_LIBRARY_NAME + ": ", this.fftwLibraryName);
    MacroUtils.recordString(FFTW_LIBRARY_FILENAME + ": ", this.fftwLibraryFileName);
    MacroUtils.recordString(PLAN_PATH + ": ", this.planPath);
    MacroUtils.recordString(FFTW_LIBRARY_PATH + ": ", this.fftwLibraryPath);
    MacroUtils.recordInteger(STAGE_REPEATABILITY + ": ", this.stageRepeatability);
    MacroUtils.recordDouble(HORIZONTAL_OVERLAP + ": ", this.horizontalOverlap);
    MacroUtils.recordDouble(VERTICAL_OVERLAP + ": ", this.verticalOverlap);
    MacroUtils.recordInteger(NUM_FFT_PEAKS + ": ", this.numFFTPeaks);
    MacroUtils.recordDouble(OVERLAP_UNCERTAINTY + ": ", this.overlapUncertainty);
    MacroUtils.recordBoolean(IS_USE_DOUBLE_PRECISION + ": ", this.useDoublePrecision);
    MacroUtils.recordBoolean(IS_USE_BIOFORMATS + ": ", this.useBioFormats);
    MacroUtils.recordBoolean(IS_ENABLE_CUDA_EXCEPTIONS + ": ", this.enableCudaExceptions);
    MacroUtils.recordString(TRANSLATION_REFINEMENT_TYPE + ": ", this.translationRefinementType.name());
    MacroUtils.recordInteger(NUM_TRANS_REFINEMENT_START_POINTS + ": ", this.numTranslationRefinementStartPoints);
    MacroUtils.recordBoolean(RUN_HEADLESS + ": ", MISTMain.runHeadless);

    MacroUtils.recordCUDADevices(this.cudaDevices);
  }

  @Override
  public void saveParams(Preferences pref) {
    pref.put(PROGRAM_TYPE, this.programType.name());
    pref.putInt(NUM_CPU_THREADS, this.numCPUThreads);
    pref.putBoolean(LOAD_FFTW_PLAN, this.loadFFTWPlan);
    pref.putBoolean(SAVE_FFTW_PLAN, this.saveFFTWPlan);
    pref.put(FFTW_PLAN_TYPE, this.fftwPlanType.name());
    pref.put(FFTW_LIBRARY_NAME, this.fftwLibraryName);
    pref.put(FFTW_LIBRARY_FILENAME, this.fftwLibraryFileName);
    pref.put(PLAN_PATH, this.planPath);
    pref.put(FFTW_LIBRARY_PATH, this.fftwLibraryPath);
    pref.putInt(STAGE_REPEATABILITY, this.stageRepeatability);
    pref.putDouble(HORIZONTAL_OVERLAP, this.horizontalOverlap);
    pref.putDouble(VERTICAL_OVERLAP, this.verticalOverlap);
    pref.putInt(NUM_FFT_PEAKS, this.numFFTPeaks);
    pref.putDouble(OVERLAP_UNCERTAINTY, this.overlapUncertainty);
    pref.putBoolean(IS_USE_DOUBLE_PRECISION, this.useDoublePrecision);
    pref.putBoolean(IS_USE_BIOFORMATS, this.useBioFormats);
    pref.putBoolean(IS_ENABLE_CUDA_EXCEPTIONS, this.enableCudaExceptions);
    pref.put(TRANSLATION_REFINEMENT_TYPE, this.translationRefinementType.name());
    pref.putInt(NUM_TRANS_REFINEMENT_START_POINTS, this.numTranslationRefinementStartPoints);
//    pref.putBoolean(RUN_HEADLESS, MISTMain.runHeadless);
    PreferencesUtils.recordPrefCUDADevices(pref, this.cudaDevices);
  }

  @Override
  public boolean saveParams(FileWriter fw) {
    String newLine = "\n";
    try {
      fw.write(PROGRAM_TYPE + ": " + this.programType.name() + newLine);
      fw.write(NUM_CPU_THREADS + ": " + this.numCPUThreads + newLine);
      fw.write(LOAD_FFTW_PLAN + ": " + this.loadFFTWPlan + newLine);
      fw.write(SAVE_FFTW_PLAN + ": " + this.saveFFTWPlan + newLine);
      fw.write(FFTW_PLAN_TYPE + ": " + this.fftwPlanType.name() + newLine);
      fw.write(FFTW_LIBRARY_NAME + ": " + this.fftwLibraryName + newLine);
      fw.write(FFTW_LIBRARY_FILENAME + ": " + this.fftwLibraryFileName + newLine);
      fw.write(PLAN_PATH + ": " + this.planPath + newLine);
      fw.write(FFTW_LIBRARY_PATH + ": " + this.fftwLibraryPath + newLine);

      fw.write(STAGE_REPEATABILITY + ": " + this.stageRepeatability + newLine);
      fw.write(HORIZONTAL_OVERLAP + ": " + this.horizontalOverlap + newLine);
      fw.write(VERTICAL_OVERLAP + ": " + this.verticalOverlap + newLine);
      fw.write(NUM_FFT_PEAKS + ": " + this.numFFTPeaks + newLine);
      fw.write(OVERLAP_UNCERTAINTY + ": " + this.overlapUncertainty + newLine);
      fw.write(IS_USE_DOUBLE_PRECISION + ": " + this.useDoublePrecision + newLine);
      fw.write(IS_USE_BIOFORMATS + ": " + this.useBioFormats + newLine);
      fw.write(IS_ENABLE_CUDA_EXCEPTIONS + ": " + this.enableCudaExceptions + newLine);
      fw.write(TRANSLATION_REFINEMENT_TYPE + ": " + this.translationRefinementType.name() + newLine);
      fw.write(NUM_TRANS_REFINEMENT_START_POINTS + ": " +
          this.numTranslationRefinementStartPoints + newLine);
      fw.write(RUN_HEADLESS + ": " + MISTMain.runHeadless + newLine);

      if (this.cudaDevices != null) {
        for (CudaDeviceParam dev : this.cudaDevices) {
          fw.write(CUDA_DEVICE + dev.getId() + ": " + dev.getId() + ", " + dev.getName() + ", "
              + dev.getDevMajor() + ", " + dev.getDevMinor() + "\n");
        }
      }

      return true;

    } catch (IOException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }
    return false;
  }

  /**
   * @return the programType
   */
  public StitchingType getProgramType() {
    return this.programType;
  }

  /**
   * @param programType the programType to set
   */
  public void setProgramType(StitchingType programType) {
    this.programType = programType;
  }

  /**
   * @return the numCPUThreads
   */
  public int getNumCPUThreads() {
    return this.numCPUThreads;
  }

  /**
   * @param numCPUThreads the numCPUThreads to set
   */
  public void setNumCPUThreads(int numCPUThreads) {
    this.numCPUThreads = numCPUThreads;
  }

  /**
   * @return the loadFFTWPlan
   */
  public boolean isLoadFFTWPlan() {
    return this.loadFFTWPlan;
  }

  /**
   * @param loadFFTWPlan the loadFFTWPlan to set
   */
  public void setLoadFFTWPlan(boolean loadFFTWPlan) {
    this.loadFFTWPlan = loadFFTWPlan;
  }

  /**
   * @return the saveFFTWPlan
   */
  public boolean isSaveFFTWPlan() {
    return this.saveFFTWPlan;
  }

  /**
   * @param saveFFTWPlan the saveFFTWPlan to set
   */
  public void setSaveFFTWPlan(boolean saveFFTWPlan) {
    this.saveFFTWPlan = saveFFTWPlan;
  }

  /**
   * @return the fftwPlanType
   */
  public FftwPlanType getFftwPlanType() {
    return this.fftwPlanType;
  }

  /**
   * @param fftwPlanType the fftwPlanType to set
   */
  public void setFftwPlanType(FftwPlanType fftwPlanType) {
    this.fftwPlanType = fftwPlanType;
  }

  /**
   * @return the planPath
   */
  public String getPlanPath() {
    return this.planPath;
  }

  /**
   * @param planPath the planPath to set
   */
  public void setPlanPath(String planPath) {
    this.planPath = planPath;
  }

  /**
   * @return the fftwLibraryPath
   */
  public String getFftwLibraryPath() {
    return this.fftwLibraryPath;
  }

  /**
   * @param fftwLibraryPath the fftwLibraryPath to set
   */
  public void setFftwLibraryPath(String fftwLibraryPath) {
    this.fftwLibraryPath = fftwLibraryPath;
  }

  /**
   * @return the fftwLibraryName
   */
  public String getFftwLibraryName() {
    return this.fftwLibraryName;
  }

  /**
   * @param fftwLibraryName the fftwLibraryName to set
   */
  public void setFftwLibraryName(String fftwLibraryName) {
    this.fftwLibraryName = fftwLibraryName;
  }

  /**
   * @return the fftwLibraryFileName
   */
  public String getFftwLibraryFileName() {
    return this.fftwLibraryFileName;
  }

  /**
   * @param fftwLibraryFileName the fftwLibraryFileName to set
   */
  public void setFftwLibraryFileName(String fftwLibraryFileName) {
    this.fftwLibraryFileName = fftwLibraryFileName;
  }

  /**
   * @return the cudaDevices
   */
  public List<CudaDeviceParam> getCudaDevices() {
    return this.cudaDevices;
  }

  /**
   * @param cudaDevices the cudaDevices to set
   */
  public void setCudaDevices(List<CudaDeviceParam> cudaDevices) {
    this.cudaDevices = cudaDevices;
  }


  /**
   * @return the repeatability
   */
  public int getRepeatability() {
    return this.stageRepeatability;
  }

  /**
   * @param repeatability the repeatability to set
   */
  public void setRepeatability(int repeatability) {
    this.stageRepeatability = repeatability;
  }

  /**
   * @return the horizontalOverlap
   */
  public double getHorizontalOverlap() {
    return this.horizontalOverlap;
  }

  /**
   * @param horizontalOverlap the horizontalOverlap to set
   */
  public void setHorizontalOverlap(double horizontalOverlap) {
    this.horizontalOverlap = horizontalOverlap;
  }

  /**
   * @return the verticalOverlap
   */
  public double getVerticalOverlap() {
    return this.verticalOverlap;
  }

  /**
   * @param verticalOverlap the verticalOverlap to set
   */
  public void setVerticalOverlap(double verticalOverlap) {
    this.verticalOverlap = verticalOverlap;
  }

  /**
   * @return the numFFTPeaks
   */
  public int getNumFFTPeaks() {
    return this.numFFTPeaks;
  }

  /**
   * @param numFFTPeaks the numFFTPeaks to set
   */
  public void setNumFFTPeaks(int numFFTPeaks) {
    this.numFFTPeaks = numFFTPeaks;
  }

  public boolean isUseDoublePrecision() {
    return this.useDoublePrecision;
  }

  public void setUseDoublePrecision(boolean val) {
    this.useDoublePrecision = val;
  }

  public boolean isUseBioFormats() {
    return this.useBioFormats;
  }

  public void setUseBioFormats(boolean val) {
    this.useBioFormats = val;
  }

  public boolean isEnableCudaExceptions() {
    return this.enableCudaExceptions;
  }

  public void setEnableCudaExceptions(boolean val) {
    this.enableCudaExceptions = val;
  }

  public TranslationRefinementType getTranslationRefinementType() {
    return this.translationRefinementType;
  }

  public void setTranslationRefinementType(TranslationRefinementType t) {
    this.translationRefinementType = t;
  }

  public int getNumTranslationRefinementStartPoints() {
    return this.numTranslationRefinementStartPoints;
  }

  public void setNumTranslationRefinementStartPoints(int val) {
    this.numTranslationRefinementStartPoints = val;
  }

  /**
   * @return the overlapUncertainty
   */
  public double getOverlapUncertainty() {
    return this.overlapUncertainty;
  }

  /**
   * @param overlapUncertainty the overlapUncertainty to set
   */
  public void setOverlapUncertainty(double overlapUncertainty) {
    this.overlapUncertainty = overlapUncertainty;
  }


  public static String getParametersCommandLineHelp() {
    String line = "\r\n";
    String str = "********* Advanced Parameters *********";
    str += line;
    str += PROGRAM_TYPE + "=" + line;
    str += NUM_CPU_THREADS + "=" + line;
    str += LOAD_FFTW_PLAN + "=" + line;
    str += SAVE_FFTW_PLAN + "=" + line;
    str += FFTW_PLAN_TYPE + "=" + line;
    str += PLAN_PATH + "=" + line;
    str += FFTW_LIBRARY_PATH + "=" + line;
    str += FFTW_LIBRARY_NAME + "=" + line;
    str += FFTW_LIBRARY_FILENAME + "=" + line;
    str += CUDA_DEVICE + "=" + line;
    str += STAGE_REPEATABILITY + "=" + line;
    str += HORIZONTAL_OVERLAP + "=" + line;
    str += VERTICAL_OVERLAP + "=" + line;
    str += NUM_FFT_PEAKS + "=" + line;
    str += OVERLAP_UNCERTAINTY + "=" + line;
    str += IS_USE_DOUBLE_PRECISION + "=" + line;
    str += IS_USE_BIOFORMATS + "=" + line;
    str += IS_ENABLE_CUDA_EXCEPTIONS + "=" + line;
    str += TRANSLATION_REFINEMENT_TYPE + "=" + line;
    str += NUM_TRANS_REFINEMENT_START_POINTS + "=" + line;
    str += RUN_HEADLESS + "=" + line;
    return str;
  }

}
