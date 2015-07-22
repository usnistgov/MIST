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
// Date: Oct 1, 2014 1:55:52 PM EST
//
// Time-stamp: <Oct 1, 2014 1:55:52 PM tjb3>
//
// ================================================================
package gov.nist.isg.mist.stitching.gui.params;

import gov.nist.isg.mist.stitching.gui.params.interfaces.StitchingAppParamFunctions;
import gov.nist.isg.mist.stitching.gui.params.objects.CudaDeviceParam;
import gov.nist.isg.mist.stitching.gui.params.utils.MacroUtils;
import gov.nist.isg.mist.stitching.gui.params.utils.PreferencesUtils;
import gov.nist.isg.mist.stitching.gui.params.utils.StitchingParamUtils;
import gov.nist.isg.mist.stitching.gui.executor.StitchingExecutor.StitchingType;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwPlanType;
import gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaUtils;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.optimization.GlobalOptimization.GlobalOptimizationType;
import gov.nist.isg.mist.stitching.lib.optimization.OptimizationUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * AdvancedParameters are the advanced parameters for Stitching
 * @author Tim Blattner
 *
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
  private static final String GLOBAL_OPT = "globalOpt";
  private static final String USE_HILL_CLIMBING = "useHillClimbing";
  private static final String STAGE_REPEATABILITY = "stageRepeatability";
  private static final String HORIZONTAL_OVERLAP = "horizontalOverlap";
  private static final String VERTICAL_OVERLAP = "verticalOverlap";
  private static final String NUM_FFT_PEAKS = "numFFTPeaks";
  private static final String OVERLAP_UNCERTAINTY = "overlapUncertainty";
      
  
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
  private GlobalOptimizationType globalOpt;
  private boolean useHillClimbing;
  private OptimizationUtils.OverlapType overlapComputationType;

  // Advanced options
  private int stageRepeatability;
  private double horizontalOverlap;
  private double verticalOverlap;
  private int numFFTPeaks;
  private double overlapUncertainty;

  public AdvancedParameters()
  {
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

    // Global Optimization
    this.globalOpt = GlobalOptimizationType.DEFAULT;
    this.useHillClimbing = true;


    // Advanced options
    this.stageRepeatability = 0;
    this.horizontalOverlap = Double.NaN;
    this.verticalOverlap = Double.NaN;
    this.numFFTPeaks = 0;
    this.overlapUncertainty = Double.NaN;
    // default overlap computation method is the MLE
    this.overlapComputationType = OptimizationUtils.OverlapType.MLE;
  }

  @Override
  public boolean checkParams() {
    return checkParallelParams() && checkGlobalOptimizationParams();
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

  private boolean checkGlobalOptimizationParams() {
    if (this.globalOpt == null)
      return false;
    return true;

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
    if (this.numCPUThreads > 0)
      return true;
    return false;
  }


  @Override
  public boolean loadParams(File file) {
    try
    {
      boolean noErrors = true;

      Log.msg(LogType.MANDATORY, "Loading advanced parameters");

      FileReader fr = new FileReader(file.getAbsolutePath());

      BufferedReader br = new BufferedReader(fr);


      String line = null;
      while ((line = br.readLine()) != null) {
        String[] contents = line.split(":", 2);

        if (contents.length > 1) {
          contents[0] = contents[0].trim();
          contents[1] = contents[1].trim();

          try
          {
            if (contents[0].equals(PROGRAM_TYPE))
              this.programType = StitchingType.valueOf(contents[1].toUpperCase());
            else if (contents[0].equals(NUM_CPU_THREADS))
              this.numCPUThreads = StitchingParamUtils.loadInteger(contents[1], this.numCPUThreads);
            else if (contents[0].equals(LOAD_FFTW_PLAN))
              this.loadFFTWPlan = StitchingParamUtils.loadBoolean(contents[1], this.loadFFTWPlan);
            else if (contents[0].equals(FFTW_PLAN_TYPE))
              this.fftwPlanType = FftwPlanType.valueOf(contents[1].toUpperCase());
            else if (contents[0].equals(FFTW_LIBRARY_NAME))
              this.fftwLibraryName = contents[1];
            else if (contents[0].equals(PLAN_PATH))
              this.planPath = contents[1];
            else if (contents[0].equals(FFTW_LIBRARY_PATH))
              this.fftwLibraryPath = contents[1];
            else if (contents[0].equals(FFTW_LIBRARY_FILENAME))
              this.fftwLibraryFileName = contents[1];
            else if (contents[0].equals(SAVE_FFTW_PLAN))
              this.saveFFTWPlan = StitchingParamUtils.loadBoolean(contents[1], this.saveFFTWPlan);
            else if (contents[0].equals(GLOBAL_OPT))
              this.globalOpt = GlobalOptimizationType.valueOf(contents[1].toUpperCase());
            else if (contents[0].equals(USE_HILL_CLIMBING))
              this.useHillClimbing = StitchingParamUtils.loadBoolean(contents[1], this.useHillClimbing);          
            else if (contents[0].equals(STAGE_REPEATABILITY))
              this.stageRepeatability = StitchingParamUtils.loadInteger(contents[1], this.stageRepeatability);
            else if (contents[0].equals(HORIZONTAL_OVERLAP))
              this.horizontalOverlap = StitchingParamUtils.loadDouble(contents[1], this.horizontalOverlap);
            else if (contents[0].equals(VERTICAL_OVERLAP))
              this.verticalOverlap = StitchingParamUtils.loadDouble(contents[1], this.verticalOverlap);
            else if (contents[0].equals(NUM_FFT_PEAKS))
              this.numFFTPeaks = StitchingParamUtils.loadInteger(contents[1], this.numFFTPeaks);
            else if (contents[0].equals(OVERLAP_UNCERTAINTY))
              this.overlapUncertainty = StitchingParamUtils.loadDouble(contents[1], this.overlapUncertainty);
            else if (contents[0].startsWith(CUDA_DEVICE)) {
              if (this.cudaDevices == null)
                this.cudaDevices = new ArrayList<CudaDeviceParam>();

              String[] cudaDevice = contents[1].split(",");
              if (cudaDevice.length == 4) {
                try {
                  int id = Integer.parseInt(cudaDevice[0].trim());
                  String name = cudaDevice[1];
                  int major = Integer.parseInt(cudaDevice[2].trim());
                  int minor = Integer.parseInt(cudaDevice[3].trim());

                  this.cudaDevices.add(new CudaDeviceParam(id, name, minor, major));
                } catch (NumberFormatException e) {
                  Log.msg(LogType.MANDATORY, "Error parsing config file line: " + line);
                }
              } else {
                Log.msg(LogType.MANDATORY, "Error parsing config file line: " + line);
              }
            }    
          } catch (IllegalArgumentException e)
          {
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
    this.globalOpt = PreferencesUtils.loadPrefGlobalOptimizationType(pref, GLOBAL_OPT, this.globalOpt.name());
    this.useHillClimbing = pref.getBoolean(USE_HILL_CLIMBING, this.useHillClimbing);    
    this.stageRepeatability = pref.getInt(STAGE_REPEATABILITY, this.stageRepeatability);
    this.horizontalOverlap = pref.getDouble(HORIZONTAL_OVERLAP, this.horizontalOverlap);
    this.verticalOverlap = pref.getDouble(VERTICAL_OVERLAP, this.verticalOverlap);
    this.numFFTPeaks = pref.getInt(NUM_FFT_PEAKS, this.numFFTPeaks);
    this.overlapUncertainty = pref.getDouble(OVERLAP_UNCERTAINTY, this.overlapUncertainty);

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
    Log.msg(logLevel, GLOBAL_OPT + ": " + this.globalOpt);
    Log.msg(logLevel, USE_HILL_CLIMBING + ": " + this.useHillClimbing);

    Log.msg(logLevel, STAGE_REPEATABILITY + ": " + this.stageRepeatability);
    Log.msg(logLevel, HORIZONTAL_OVERLAP + ": " + this.horizontalOverlap);
    Log.msg(logLevel, VERTICAL_OVERLAP + ": " + this.verticalOverlap);
    Log.msg(logLevel, NUM_FFT_PEAKS + ": " + this.numFFTPeaks);
    Log.msg(logLevel, OVERLAP_UNCERTAINTY + ": " + this.overlapUncertainty);


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
    this.globalOpt = MacroUtils.loadMacroGlobalOptimizationType(macroOptions, GLOBAL_OPT, this.globalOpt.name());
    this.useHillClimbing = MacroUtils.loadMacroBoolean(macroOptions, USE_HILL_CLIMBING, this.useHillClimbing);    
    this.stageRepeatability = MacroUtils.loadMacroInteger(macroOptions, STAGE_REPEATABILITY, this.stageRepeatability);
    this.horizontalOverlap = MacroUtils.loadMacroDouble(macroOptions, HORIZONTAL_OVERLAP, this.horizontalOverlap);
    this.verticalOverlap = MacroUtils.loadMacroDouble(macroOptions, VERTICAL_OVERLAP, this.verticalOverlap);
    this.numFFTPeaks = MacroUtils.loadMacroInteger(macroOptions, NUM_FFT_PEAKS, this.numFFTPeaks);
    this.overlapUncertainty = MacroUtils.loadMacroDouble(macroOptions, OVERLAP_UNCERTAINTY, this.overlapUncertainty);

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
    MacroUtils.recordString(GLOBAL_OPT + ": ", this.globalOpt.name());
    MacroUtils.recordBoolean(USE_HILL_CLIMBING + ": ", this.useHillClimbing);
    MacroUtils.recordInteger(STAGE_REPEATABILITY + ": ", this.stageRepeatability);
    MacroUtils.recordDouble(HORIZONTAL_OVERLAP + ": ", this.horizontalOverlap);
    MacroUtils.recordDouble(VERTICAL_OVERLAP + ": ", this.verticalOverlap);
    MacroUtils.recordInteger(NUM_FFT_PEAKS + ": ", this.numFFTPeaks);
    MacroUtils.recordDouble(OVERLAP_UNCERTAINTY + ": ", this.overlapUncertainty);

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
    pref.put(GLOBAL_OPT, this.globalOpt.name());
    pref.putBoolean(USE_HILL_CLIMBING, this.useHillClimbing);   
    pref.putInt(STAGE_REPEATABILITY, this.stageRepeatability);
    pref.putDouble(HORIZONTAL_OVERLAP, this.horizontalOverlap);
    pref.putDouble(VERTICAL_OVERLAP, this.verticalOverlap);
    pref.putInt(NUM_FFT_PEAKS, this.numFFTPeaks);
    pref.putDouble(OVERLAP_UNCERTAINTY, this.overlapUncertainty);        
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
      fw.write(GLOBAL_OPT + ": " + this.globalOpt.name() + newLine);
      fw.write(USE_HILL_CLIMBING + ": " + this.useHillClimbing + newLine);      

      fw.write(STAGE_REPEATABILITY + ": " + this.stageRepeatability + newLine);
      fw.write(HORIZONTAL_OVERLAP + ": " + this.horizontalOverlap + newLine);
      fw.write(VERTICAL_OVERLAP + ": " + this.verticalOverlap + newLine);
      fw.write(NUM_FFT_PEAKS + ": " + this.numFFTPeaks + newLine);
      fw.write(OVERLAP_UNCERTAINTY + ": " + this.overlapUncertainty + newLine);


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
   * @return the globalOpt
   */
  public GlobalOptimizationType getGlobalOpt() {
    return this.globalOpt;
  }

  /**
   * @param globalOpt the globalOpt to set
   */
  public void setGlobalOpt(GlobalOptimizationType globalOpt) {
    this.globalOpt = globalOpt;
  }

  /**
   * @param optType the overlap computation type to set
   */
  public void setOverlapComputationType(OptimizationUtils.OverlapType optType) {
    this.overlapComputationType = optType;
  }

  public OptimizationUtils.OverlapType getOverlapComputationType() {
    return this.overlapComputationType;
  }


  /**
   * @return the useHillClimbing
   */
  public boolean isUseHillClimbing() {
    return this.useHillClimbing;
  }

  /**
   * @param useHillClimbing the useHillClimbing to set
   */
  public void setUseHillClimbing(boolean useHillClimbing) {
    this.useHillClimbing = useHillClimbing;
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


}
