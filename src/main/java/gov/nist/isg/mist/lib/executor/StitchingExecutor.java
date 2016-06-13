// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 1:34:00 PM EST
//
// Time-stamp: <Apr 18, 2014 1:34:00 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.executor;

import org.bridj.Pointer;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

import gov.nist.isg.mist.MISTMain;
import gov.nist.isg.mist.MISTMain.ExecutionType;
import gov.nist.isg.mist.StitchingGUIFrame;
import gov.nist.isg.mist.gui.StitchingGuiUtils;
import gov.nist.isg.mist.gui.StitchingStatistics;
import gov.nist.isg.mist.gui.StitchingStatistics.RunTimers;
import gov.nist.isg.mist.gui.StitchingStatusFrame;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.gui.params.objects.RangeParam;
import gov.nist.isg.mist.lib.exceptions.EmptyGridException;
import gov.nist.isg.mist.lib.exceptions.StitchingException;
import gov.nist.isg.mist.lib.export.LargeImageExporter;
import gov.nist.isg.mist.lib.export.blend.AverageBlend;
import gov.nist.isg.mist.lib.export.blend.Blender;
import gov.nist.isg.mist.lib.export.blend.LinearBlend;
import gov.nist.isg.mist.lib.export.blend.OverlayBlend;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.Stitching;
import gov.nist.isg.mist.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.lib.tilegrid.TileGridUtils;
import gov.nist.isg.mist.lib32.executor.CudaStitchingExecutor32;
import gov.nist.isg.mist.optimization.GlobalOptimization;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.MessageDialog;
import ij.macro.Interpreter;
import ij.plugin.frame.Recorder;
import jcuda.CudaException;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.JCudaDriver;
import jcuda.jcufft.JCufft;

/**
 * StitchingExecutor is the thread that executes stitching.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class StitchingExecutor implements Runnable {

  /**
   * Type of stitching execution to be done
   *
   * @author Tim Blattner
   * @version 1.0
   */
  public static enum StitchingType {
    /**
     * Automatically determines stitching type
     */
    AUTO("Auto"),

    /**
     * Selects JAVA version
     */
    JAVA("JAVA"),

    /**
     * Selects FFTW version
     */
    FFTW("FFTW"),

    /**
     * Selects CUDA version
     */
    CUDA("CUDA");

    StitchingType(final String text) {
      this.text = text;
    }

    private final String text;

    @Override
    public String toString() {
      return this.text;
    }
  }

  /**
   * The stitching statistics for stitching
   */
  private StitchingStatistics stitchingStatistics;

  private static final String outOfMemoryMessage =
      "Error: Insufficient memory to execute stitching.\n" + "Please increase JVM maximum memory.";


  private StitchingGUIFrame stitchingGUI;
  private StitchingStatusFrame stitchingExecutionFrame;

  private JProgressBar progressBar;
  private JLabel progressLabel;
  private StitchingAppParams params;
  private ExecutionType executionType;

  GlobalOptimization globalOptimization;

  private LargeImageExporter<?> imageExporter;

  private volatile boolean isCancelled;

  private StitchingExecutorInterface<?> executor;


  /**
   * Initializes the stitching execution with only params
   *
   * @param params the stitching app parameters
   */
  public StitchingExecutor(StitchingAppParams params) {
    this(null, null, params);
  }

  /**
   * Initializes the stitching execution
   *
   * @param stitchingGUI the stitching application GUI
   * @param type         the type of execution to be done
   * @param params       the stitching app parameters
   */
  public StitchingExecutor(StitchingGUIFrame stitchingGUI, ExecutionType type, StitchingAppParams params) {
    this.params = params;
    this.stitchingGUI = stitchingGUI;
    this.executionType = type;
    this.progressBar = null;
    this.progressLabel = null;
    this.isCancelled = false;
    this.imageExporter = null;
  }

  @Override
  public void run() {

    try {
      switch (executionType) {
        case RunStitching:
          runStitchingWithGUI();
          break;
        case RunStitchingMacro:
          runStitchingWithMacro();
          break;
        case RunStitchingFromMeta:
          runStitchingFromMeta();
          break;
        case PreviewNoOverlap:
          try {
            previewNoOverlap();
          } catch (FileNotFoundException e) {
            Log.msg(LogType.MANDATORY, "File not found: " + e.getMessage() + " Cancelling preview.");
          }
          break;
        case LoadParams:
        case RunStitchingFromMetaMacro:
        case SaveParams:
        default:
          break;

      }
    } catch (StitchingException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }

    if (stitchingExecutionFrame != null)
      stitchingExecutionFrame.dispose();
    MISTMain.disableStitching();
  }

  /**
   * Cancels execution
   */
  public void cancelExecution() {
    isCancelled = true;
    if (executor != null) {
      executor.cancelExecution();
    }

    cancelOptimization();
    cancelExport();
  }


  /**
   * Cancels the optimization
   */
  public void cancelOptimization() {
    if (globalOptimization != null)
      globalOptimization.cancel();

  }

  public void cancelExport() {
    if (imageExporter != null)
      imageExporter.cancel();
  }


  private void runStitchingWithGUI() throws StitchingException {
    Log.msg(LogType.MANDATORY, "Checking args for stitching:");

    if (stitchingGUI.checkAndParseGUI(params)) {
      Log.msg(LogType.MANDATORY, "Arg check passed");
      if (Recorder.record || Recorder.recordInMacros) {
        Log.msg(LogType.MANDATORY, "Recording macros");
        params.recordMacro();
      }

      runStitching(false, true, false);
    } else {
      Log.msg(LogType.MANDATORY, "Stitching parameter check failed. "
          + "Invalid values are highlighted in red");
    }
  }

  private void runStitchingWithMacro() throws StitchingException {
    params.loadMacro();

    if (params.checkParams()) {
      if (params.getInputParams().isAssembleFromMetadata())
        runStitchingFromMeta();
      else
        runStitching(false, true, false);
    } else {
      Log.msg(LogType.MANDATORY, "Stitching parameter check failed. "
          + "Check the console for information. (increase logging " + "level for more details)");
    }
  }

  private void runStitchingFromMeta() throws StitchingException {
    Log.msg(LogType.MANDATORY, "Checking args for stitching:");

    if (stitchingGUI == null || stitchingGUI.checkAndParseGUI(params)) {
      Log.msg(LogType.MANDATORY, "Arg check passed");
      if (Recorder.record || Recorder.recordInMacros) {
        Log.msg(LogType.MANDATORY, "Recording macros");
        params.recordMacro();
      }

      assembleFromMeta();
    } else {
      Log.msg(LogType.MANDATORY, "Stitching parameter check failed. "
          + "Invalid values are highlighted in red");
    }


  }

  private void assembleFromMeta() throws StitchingException {

    params.getOutputParams().setOutputMeta(false);

    runStitching(true, true, false);
  }

  /**
   * Executes stitching
   *
   * @param assembleFromMeta            whether to assemble from meta data or not
   * @param displayGui                  whether to display any gui or not
   * @param stopExecutionIfFileNotFound sets whether to throws a stitching exception if a file not
   *                                    found exception is found
   */
  @SuppressWarnings("unchecked")
  public <T> void runStitching(boolean assembleFromMeta, boolean displayGui,
                               boolean stopExecutionIfFileNotFound) throws StitchingException {
    stitchingStatistics = new StitchingStatistics(params);
    if (GraphicsEnvironment.isHeadless() || Interpreter.isBatchMode() || MISTMain.runHeadless)
      displayGui = false;

    StitchingExecutorInterface<T> stitchingExecutorInf = null;

    if (assembleFromMeta) {
      stitchingExecutorInf = (StitchingExecutorInterface<T>) new AssembleFromMetaExecutor<Pointer<Double>>();
      params.getOutputParams().setOutputMeta(false); // if assembling from meta, don't also output meta
    } else {
      switch (params.getAdvancedParams().getProgramType()) {
        case AUTO:
          // initialize and check fftw first
          if (params.getAdvancedParams().isUseDoublePrecision()) {
            String libFN = params.getAdvancedParams().getFftwLibraryFileName();
            if (libFN.startsWith("libfftw3f")) {
              params.getAdvancedParams().setFftwLibraryFileName("libfftw3" + libFN.substring(9));
            }
            libFN = params.getAdvancedParams().getFftwLibraryName();
            if (libFN.startsWith("libfftw3f")) {
              params.getAdvancedParams().setFftwLibraryName("libfftw3" + libFN.substring(9));
            }

            stitchingExecutorInf = (StitchingExecutorInterface<T>) new FftwStitchingExecutor<Pointer<Double>>(this);
          } else {
            String libFN = params.getAdvancedParams().getFftwLibraryFileName();
            if (!libFN.startsWith("libfftw3f") && libFN.startsWith("libfftw3")) {
              params.getAdvancedParams().setFftwLibraryFileName("libfftw3f" + libFN.substring(8));
            }
            libFN = params.getAdvancedParams().getFftwLibraryName();
            if (!libFN.startsWith("libfftw3f") && libFN.startsWith("libfftw3")) {
              params.getAdvancedParams().setFftwLibraryName("libfftw3f" + libFN.substring(8));
            }

            stitchingExecutorInf = (StitchingExecutorInterface<T>) new FftwStitchingExecutor<Pointer<Float>>(this);
          }

          // If the libs are not available for FFTW, then check Java
          if (!stitchingExecutorInf.checkForLibs(params, displayGui)) {
            stitchingExecutorInf = (StitchingExecutorInterface<T>) new JavaStitchingExecutor<float[][]>();
          }

          break;
        case CUDA:
          if (params.getAdvancedParams().isUseDoublePrecision()) {
            stitchingExecutorInf = (StitchingExecutorInterface<T>) new CudaStitchingExecutor<CUdeviceptr>(this);
          } else {
            stitchingExecutorInf = (StitchingExecutorInterface<T>) new CudaStitchingExecutor32<CUdeviceptr>(this);
          }

          JCufft.setExceptionsEnabled(params.getAdvancedParams().isEnableCudaExceptions());
          JCudaDriver.setExceptionsEnabled(params.getAdvancedParams().isEnableCudaExceptions());

          break;
        case FFTW:
          if (params.getAdvancedParams().isUseDoublePrecision()) {
            String libFN = params.getAdvancedParams().getFftwLibraryFileName();
            if (libFN.startsWith("libfftw3f")) {
              params.getAdvancedParams().setFftwLibraryFileName("libfftw3" + libFN.substring(9));
            }
            libFN = params.getAdvancedParams().getFftwLibraryName();
            if (libFN.startsWith("libfftw3f")) {
              params.getAdvancedParams().setFftwLibraryName("libfftw3" + libFN.substring(9));
            }

            stitchingExecutorInf = (StitchingExecutorInterface<T>) new FftwStitchingExecutor<Pointer<Double>>(this);
          } else {
            String libFN = params.getAdvancedParams().getFftwLibraryFileName();
            if (!libFN.startsWith("libfftw3f") && libFN.startsWith("libfftw3")) {
              params.getAdvancedParams().setFftwLibraryFileName("libfftw3f" + libFN.substring(8));
            }
            libFN = params.getAdvancedParams().getFftwLibraryName();
            if (!libFN.startsWith("libfftw3f") && libFN.startsWith("libfftw3")) {
              params.getAdvancedParams().setFftwLibraryName("libfftw3f" + libFN.substring(8));
            }

            stitchingExecutorInf = (StitchingExecutorInterface<T>) new FftwStitchingExecutor<Pointer<Float>>(this);
          }
          break;
        case JAVA:
          stitchingExecutorInf = (StitchingExecutorInterface<T>) new JavaStitchingExecutor<float[][]>();
          break;
        default:
          break;

      }
    }

    executor = stitchingExecutorInf;

    if (stitchingExecutorInf == null || !stitchingExecutorInf.checkForLibs(params, displayGui))
      return;


    // Check for overwriting files, and confirm with user
    ExistingFilesChecker fileChecker = new ExistingFilesChecker(params);
    if (!fileChecker.checkExistingFiles(displayGui))
      return;


    if (!params.getInputParams().isSuppressSubGridWarning()) {
      // open dialog telling the user if they are stitching with a subgrid
      int dH = params.getInputParams().getGridHeight() - params.getInputParams().getExtentHeight();
      int dW = params.getInputParams().getGridWidth() - params.getInputParams().getExtentWidth();
      if (dH != 0 || dW != 0) {
        Log.msg(LogType.MANDATORY, "MIST is stitching a Sub Grid.");
        if (displayGui && !GraphicsEnvironment.isHeadless() && !Interpreter.isBatchMode()) {
          String[] options = {"Ok",
              "Cancel"};
          int n = JOptionPane.showOptionDialog(stitchingGUI,
              "MIST is stitching a Sub Grid.",
              "Warning: SubGrid",
              JOptionPane.YES_NO_OPTION,
              JOptionPane.WARNING_MESSAGE,
              null,
              options,
              options[0]);
          if (n == 1) {
            cancelExecution();
            return;
          }
        }

      }
    }


    if (displayGui && !GraphicsEnvironment.isHeadless() && !Interpreter.isBatchMode()) {
      if (stitchingGUI != null)
        stitchingGUI.performExit();


      stitchingExecutionFrame = new StitchingStatusFrame(this);

      progressBar = stitchingExecutionFrame.getProgressBar();
      progressLabel = stitchingExecutionFrame.getProgressLabel();


      stitchingExecutionFrame.display();
    }


    Log.msg(LogType.MANDATORY, "STITCHING BEGINS!");
    stitchingStatistics.setExecutionType(params.getAdvancedParams().getProgramType());
    stitchingStatistics.startEndToEndTimer();

    Stitching.USE_BIOFORMATS = params.getAdvancedParams().isUseBioFormats();

    if (params.getAdvancedParams().getNumFFTPeaks() != 0) {
      Stitching.NUM_PEAKS = params.getAdvancedParams().getNumFFTPeaks();
    }

    List<RangeParam> timeSlices = params.getInputParams().getTimeSlices();
    int group = 0;
    for (RangeParam timeSliceParam : timeSlices) {
      group++;

      int minTimeSlice = timeSliceParam.getMin();
      int maxTimeSlice = timeSliceParam.getMax();

      for (int timeSlice = minTimeSlice; timeSlice <= maxTimeSlice; timeSlice++) {
        if (isCancelled) {
          cancelExecution();
          return;
        }

        stitchingStatistics.setCurrentTimeSlice(timeSlice);
        stitchingStatistics.addTimeSlice(timeSlice);


        if (params.getInputParams().isTimeSlicesEnabled()) {
          StitchingGuiUtils.updateProgressLabel(progressLabel, timeSlice,
              maxTimeSlice, group, timeSlices.size());
        }


        boolean runSequential = false;
        TileGrid<ImageTile<T>> grid;
        boolean optimizationSuccessful = false;
        try {
          // init the grid, which ensures that at least one image tile exists within the image grid
          grid = stitchingExecutorInf.initGrid(params, timeSlice);

          if (grid == null)
            return;


          ImageTile.disableFreePixelData();
          // Check if there is enough memory to process this grid
          if (!executor.checkMemory(grid, params.getAdvancedParams().getNumCPUThreads())) {
            ImageTile.enableFreePixelData();
            stitchingStatistics.setIsEnableFreeingPixelData(true);
            Log.msg(LogType.MANDATORY,
                "Insufficient memory to hold all image tiles in memory, turning on the freeing of pixel data");

            if (!executor.checkMemory(grid, params.getAdvancedParams().getNumCPUThreads())) {
              Log.msg(LogType.MANDATORY,
                  "Insufficient memory to perform stitching with " + params
                      .getAdvancedParams().getNumCPUThreads()
                      + " threads, attempting backoff for timeslice: "
                      + timeSlice);
              Log.msg(LogType.MANDATORY,
                  "SUGGESTION: Try lowering the number of compute threads which lowers the memory requirements");

              // perform thread count backoff to find what maximum number of threads can be supported
              for (int n = params.getAdvancedParams().getNumCPUThreads(); n >= 1; n--) {
                params.getAdvancedParams().setNumCPUThreads(n);
                if (executor.checkMemory(grid, params.getAdvancedParams().getNumCPUThreads()))
                  break;
              }
              Log.msg(LogType.MANDATORY,
                  "Attempting to perform stitching with " + params
                      .getAdvancedParams().getNumCPUThreads() + " threads.");

              // check if the 1 thread method has sufficient memory, if not run sequential stitching
              if (params.getAdvancedParams().getNumCPUThreads() == 1) {
                if (!executor.checkMemory(grid, params.getAdvancedParams().getNumCPUThreads())) {
                  // only run sequential stitching if not assembling from metadata
                  if (!params.getInputParams().isAssembleFromMetadata()) {
                    Log.msg(LogType.MANDATORY,
                        "Attempting to use sequential stitching, this version is expected to " +
                            "take awhile (see FAQ for suggestions)");

                    runSequential = true;
                    stitchingExecutorInf =
                        (StitchingExecutorInterface<T>) new SequentialJavaStitchingExecutor<float[][]>();
                    grid = stitchingExecutorInf.initGrid(params, timeSlice);
                    // update the executor reference because it has been changed to sequential
                    executor = stitchingExecutorInf;

                    stitchingStatistics.setIsRunSequential(true);
                  }
                }
              }
            }
          }


          stitchingStatistics.startTimer(RunTimers.TotalStitchingTime);
          initProgressBar();
          stitchingStatistics.startTimer(RunTimers.RelativeDisplacementTime);

          stitchingExecutorInf.launchStitching(grid, params, progressBar, timeSlice);

          stitchingStatistics.stopTimer(RunTimers.RelativeDisplacementTime);

          if (!isCancelled) {
            globalOptimization = new GlobalOptimization<T>(grid, progressBar,
                params, stitchingStatistics, runSequential, assembleFromMeta);
            optimizationSuccessful = globalOptimization.optimize();
          }


          stitchingStatistics.stopTimer(RunTimers.TotalStitchingTime);


        } catch (OutOfMemoryError e) {
          showError(outOfMemoryMessage);
          Log.msg(LogType.MANDATORY,
              "SUGGESTION: Try lowering the number of compute threads which lowers the memory requirements");
          throw new StitchingException("Out of memory thrown: " + outOfMemoryMessage, e);
        } catch (CudaException e) {
          showError("CUDA exception thrown: " + e.getMessage());
          throw new StitchingException("CUDA exception thrown: " + e.getMessage(), e);
        } catch (FileNotFoundException e) {
          Log.msg(LogType.MANDATORY,
              "Error unable to find file: " + e.getMessage() + ". Skipping timeslice: "
                  + timeSlice);

          if (stopExecutionIfFileNotFound)
            throw new StitchingException("Error unable to find file: " + e.getMessage() +
                ". Failed at timeslice: " + timeSlice, e);
          else
            continue;
        } catch (IllegalArgumentException e) {
          throw new StitchingException("Illegal argument: " + e.getMessage(), e);

        } catch (EmptyGridException e) {
          Log.msg(LogType.MANDATORY, "Unable to find any images in the image grid.");
          Log.msg(LogType.MANDATORY, "Skipping time slice: " + timeSlice);
          if (stopExecutionIfFileNotFound)
            throw new StitchingException("Empty image grid encountered: " + e.getMessage() +
                ". Failed at timeslice: " + timeSlice, e);
          else
            continue;

        } catch (Throwable e) {
          Log.msg(LogType.MANDATORY, "Error occurred in stitching worker: ");
          Log.msg(LogType.MANDATORY, e.toString());
          for (StackTraceElement st : e.getStackTrace())
            Log.msg(LogType.MANDATORY, st.toString());
          throw new StitchingException("Error occurred in stitching worker", e);
        }


        if (params.getInputParams().isTimeSlicesEnabled()) {
          Log.msg(
              LogType.MANDATORY,
              "Completed Stitching in "
                  + stitchingStatistics.getDuration(RunTimers.TotalStitchingTime) + "ms" +
                  " time slice: " + timeSlice + " of " + maxTimeSlice);
        } else {
          Log.msg(
              LogType.MANDATORY,
              "Completed Stitching in "
                  + stitchingStatistics.getDuration(RunTimers.TotalStitchingTime) + "ms");
        }

        // Always create the output directory
        File outputDir = new File(params.getOutputParams().getOutputPath());
        outputDir.mkdirs();

        if (optimizationSuccessful) {
          try {
            if (params.getOutputParams().isOutputMeta()) {
              outputMeta(grid, progressBar, timeSlice);
            }

            if (checkOutputGridMemory(grid)) {
              outputGrid(grid, progressBar, timeSlice);
            } else {
              if (params.getOutputParams().isOutputFullImage())
                Log.msg(LogType.MANDATORY, "Not enough memory to create output stitched image.");
            }
          } catch (FileNotFoundException e) {
            Log.msg(LogType.MANDATORY,
                "Unable find file: " + e.getMessage() + ". Cancelling writing full image.");
          }
        }

        releaseTiles(grid);
      }
    }

    stitchingStatistics.stopEndToEndTimer();
    if(params.getOutputParams().isOutputMeta()) {
      stitchingStatistics.writeStatistics(params.getOutputParams().getStatsFile());
      stitchingStatistics.writeLog(params.getOutputParams().getLogFile());
    }
    Log.msg(LogType.MANDATORY, "Done");


    if (displayGui) {

      int displayWarningDialog = 0;
      String accumulatedWarningString = "";
      for (RangeParam timeSliceParam : timeSlices) {
        int minTimeSlice = timeSliceParam.getMin();
        int maxTimeSlice = timeSliceParam.getMax();

        for (int timeSlice = minTimeSlice; timeSlice <= maxTimeSlice; timeSlice++) {
          String str = stitchingStatistics.runErrorChecks(timeSlice);
          if (!str.contains(StitchingStatistics.ErrorReportStatus.PASSED.toString())) {
            displayWarningDialog++;
            accumulatedWarningString = accumulatedWarningString + str + "\n";
          }
        }
      }

      if (displayWarningDialog > 0) {
        File statsFile = params.getOutputParams().getStatsFile();
        String warnStr = "Stitching experiment(s) generated warnings:\n \n";
        if (displayWarningDialog == 1) {
          warnStr = warnStr + accumulatedWarningString + "\n \n";
        }
        warnStr = warnStr + "For more details check the log or the statistics file:\n" + statsFile
            .getAbsolutePath();
        new MessageDialog(IJ.getInstance(), "Stitching Warning", warnStr);
      }
    }

    executor.cleanup();
  }


  /**
   * Initializes the progress bar based on the total translations of extent-width and
   * extent-height.
   */
  public void initProgressBar() {
    final int totalTranslations =
        (params.getInputParams().getExtentHeight() * (params.getInputParams().getExtentWidth() - 1))
            + ((params.getInputParams().getExtentHeight() - 1) * params.getInputParams().getExtentWidth());

    StitchingGuiUtils.updateProgressBar(progressBar, false, null, "Stitching...", 0,
        totalTranslations, 0, false);
  }


  private static <T> void releaseTiles(TileGrid<ImageTile<T>> grid) {
    TileGridUtils.releaseTiles(grid);
  }

  private <T> void outputGrid(TileGrid<ImageTile<T>> grid, final JProgressBar progress,
                              int timeSlice) throws FileNotFoundException {

    if (isCancelled)
      return;

    ImagePlus img = null;

    if (params.getOutputParams().isOutputFullImage()) {
      img = saveFullImage(grid, progress, timeSlice);

      if (img == null)
        return;
    }

    if (params.getOutputParams().isDisplayStitching())
      displayFullImage(grid, progress, img);

    StitchingGuiUtils.updateProgressBarCompleted(progress);

    Log.msg(LogType.MANDATORY, "Completed output options for slice " + timeSlice + ".");
  }

  private <T> void outputMeta(TileGrid<ImageTile<T>> grid, final JProgressBar progress,
                              int timeSlice) {

    File metaDir = new File(params.getOutputParams().getOutputPath());
    metaDir.mkdirs();

    StitchingGuiUtils.updateProgressBar(progress, true, "Outputting metadata");

    int nDigits = params.getInputParams().getNumberTimeSliceDigits();

    // abs positions
    Stitching.outputAbsolutePositions(grid, params.getOutputParams().getAbsPosFile(timeSlice, nDigits));

    // relative positions
    Stitching.outputRelativeDisplacements(grid,
        params.getOutputParams().getRelPosFile(timeSlice, nDigits));

    // relative positions no optimization
    if (params.getOutputParams().isOutputMeta())
      Stitching.outputRelativeDisplacementsNoOptimization(grid, params.getOutputParams()
          .getRelPosNoOptFile(timeSlice, nDigits));
  }

  private <T> ImagePlus saveFullImage(TileGrid<ImageTile<T>> grid, final JProgressBar progress,
                                      int timeSlice) throws FileNotFoundException {
    ImagePlus img = null;
    StitchingGuiUtils.updateProgressBar(progress, true, "Writing Full Image");

    int nDigits = params.getInputParams().getNumberTimeSliceDigits();

    File imageFile = params.getOutputParams().getOutputImageFile(timeSlice, nDigits);

    ImageTile<T> initImg = grid.getTileThatExists();
    initImg.readTile();

    int width = TileGridUtils.getFullImageWidth(grid, initImg.getWidth());
    int height = TileGridUtils.getFullImageHeight(grid, initImg.getHeight());

    Log.msg(LogType.MANDATORY, "Writing full image to: " + imageFile.getAbsolutePath()
        + "  Width: " + width + " Height: " + height);

    stitchingStatistics.startTimer(RunTimers.OutputFullImageTileTime);
    Blender blend = null;

    try {
      switch (params.getOutputParams().getBlendingMode()) {
        case AVERAGE:
          blend = new AverageBlend();
          break;
        case LINEAR:
          initImg.readTile();
          blend = new LinearBlend(initImg.getWidth(), initImg.getHeight(), params.getOutputParams
              ().getBlendingAlpha());
          break;
        case OVERLAY:
          blend = new OverlayBlend();
          break;
      }

      if (blend != null) {
        blend.init(width, height, initImg.getImagePlus());


        imageExporter = new LargeImageExporter<T>(grid, 0, 0, width, height, blend, progress);
        img = imageExporter.exportImage(imageFile);


        stitchingStatistics.stopTimer(RunTimers.OutputFullImageTileTime);

        Log.msg(LogType.MANDATORY, "Finished saving full image: " + imageFile.getAbsolutePath());
      } else {
        Log.msg(LogType.MANDATORY, "Error: Unable to initialize blending mode: " + params
            .getOutputParams().getBlendingMode());
      }

    } catch (OutOfMemoryError e) {
      Log.msg(LogType.MANDATORY, "Error: Insufficient memory to save image.");
      showError("Out of memory error: " + e.getMessage());

      return null;
    } catch (NegativeArraySizeException e) {
      Log.msg(LogType.MANDATORY, "Error: Java does not support sizes of size width*height > "
          + Integer.MAX_VALUE);
      return null;
    }

    initImg.releasePixels();

    return img;
  }

  private <T> void displayFullImage(TileGrid<ImageTile<T>> grid, final JProgressBar progress,
                                    ImagePlus img) throws FileNotFoundException {
    if (img == null) {
      ImageTile<T> initImg = grid.getTileThatExists();
      initImg.readTile();

      int width = TileGridUtils.getFullImageWidth(grid, initImg.getWidth());
      int height = TileGridUtils.getFullImageHeight(grid, initImg.getHeight());

      if (isCancelled)
        return;

      Blender blend = null;
      try {
        switch (params.getOutputParams().getBlendingMode()) {
          case AVERAGE:
            blend = new AverageBlend();
            break;
          case LINEAR:
            initImg.readTile();
            blend =
                new LinearBlend(initImg.getWidth(), initImg.getHeight(),
                    params.getOutputParams().getBlendingAlpha());
            break;
          case OVERLAY:
            blend = new OverlayBlend();
            break;
        }
      } catch (OutOfMemoryError e) {
        Log.msg(LogType.MANDATORY, "Error: Insufficient memory to save image.");
        showError(e.getMessage());
        return;
      } catch (NegativeArraySizeException e) {
        Log.msg(LogType.MANDATORY, "Error: Java does not support sizes of size width*height > "
            + Integer.MAX_VALUE);
        return;
      }

      if (blend != null) {

        if (isCancelled)
          return;

        try {
          blend.init(width, height, initImg.getImagePlus());

          imageExporter = new LargeImageExporter<T>(grid, 0, 0, width, height, blend, progress);
          img = imageExporter.exportImage(null);
        } catch (OutOfMemoryError e) {
          Log.msg(LogType.MANDATORY, "Error: Insufficient memory for image.");
          showError("Out of memory error: " + e.getMessage());
          return;
        } catch (NegativeArraySizeException e) {
          Log.msg(LogType.MANDATORY, "Error: Java does not support sizes of size width*height > "
              + Integer.MAX_VALUE);
          showError("Error: Java does not support sizes of size width*height > "
              + Integer.MAX_VALUE);

          return;
        }

      } else {
        Log.msg(LogType.MANDATORY, "Error: Unable to initialize blending mode: " + params
            .getOutputParams().getBlendingMode());
      }

      initImg.releasePixels();
    }

    if (img == null) {
      Log.msg(LogType.MANDATORY, "Error: Unable to display image.");
    } else {
      img.setTitle(params.getOutputParams().getOutFilePrefix() + "Full_Stitching_Image");
      img.show();
    }
  }

  private <T> void previewNoOverlap() throws FileNotFoundException {
    Log.msg(LogType.MANDATORY, "Checking args for preview:");

    if (stitchingGUI.checkAndParseGUI(params)) {
      Log.msg(LogType.MANDATORY, "Arg check passed");
    } else {
      Log.msg(LogType.MANDATORY, "Stitching parameter check failed. "
          + "Invalid values are highlighted in red");
      return;
    }

    if (!GraphicsEnvironment.isHeadless()) {

      stitchingExecutionFrame = new StitchingStatusFrame(this);

      progressBar = stitchingExecutionFrame.getProgressBar();
      progressLabel = stitchingExecutionFrame.getProgressLabel();

      stitchingExecutionFrame.display();

    }

    if (isCancelled)
      return;

    TileGrid<ImageTile<T>> grid = null;
    int timeSlice = 1;

    if (params.getInputParams().isTimeSlicesEnabled()) {
      String val =
          JOptionPane.showInputDialog(null, "Please specify which timeslice you "
              + "would like to preview.", "Timeslice User Input", JOptionPane.OK_CANCEL_OPTION);

      if (val == null) {
        Log.msg(LogType.MANDATORY, "Cancelling preview");
        return;
      }

      try {
        timeSlice = Integer.parseInt(val);
      } catch (NumberFormatException e) {
        Log.msg(LogType.MANDATORY, val + " is not a valid timeslice number. (Must be an integer)");
        return;
      }

      try {
        grid = new TileGrid<ImageTile<T>>(params, timeSlice, FftwImageTile.class);
      } catch (InvalidClassException e) {
        e.printStackTrace();
      }

      if (grid == null) {
        Log.msg(LogType.MANDATORY, "Error creating tile grid.");
        return;
      }
    } else {
      try {
        grid = new TileGrid<ImageTile<T>>(params, FftwImageTile.class);
      } catch (InvalidClassException e) {
        e.printStackTrace();
      }

      if (grid == null) {
        Log.msg(LogType.MANDATORY, "Error creating tile grid.");
        return;
      }
    }

    ImagePlus img = null;

    ImageTile<T> initImg = grid.getTileThatExists();
    initImg.readTile();

    int width = grid.getExtentWidth() * initImg.getWidth();
    int height = grid.getExtentHeight() * initImg.getHeight();

    Log.msg(LogType.MANDATORY, "Preparing preview image size: " + width + "x" + height);

    try {
      StitchingGuiUtils.updateProgressBar(progressBar, true, "Initializing image buffer...");

      Blender blend = new OverlayBlend();

      blend.init(width, height, initImg.getImagePlus());

      imageExporter = new LargeImageExporter<T>(grid, 0, 0, width, height, blend, progressBar);
      img = imageExporter.exportImageNoOverlap(null);

    } catch (OutOfMemoryError e) {
      Log.msg(LogType.MANDATORY, "Error: Insufficient memory for image.");
      showError("Out of memory error: " + e.getMessage());
      return;
    } catch (NegativeArraySizeException e) {
      Log.msg(LogType.MANDATORY, "Error: Java does not support sizes of size width*height > "
          + Integer.MAX_VALUE);
      return;
    }


    if (img == null) {
      Log.msg(LogType.MANDATORY, "Error: Unable to display image.");
    } else {
      img.setTitle(params.getOutputParams().getOutFilePrefix() + "Full_Stitching_Image");
      img.show();
    }

    initImg.releasePixels();

    StitchingGuiUtils.updateProgressBarCompleted(progressBar);

    Log.msg(LogType.MANDATORY, "Completed output no overlap options for slice " + timeSlice + ".");
  }


  /**
   * Gets the stitching statistics associated with this stitching executor
   *
   * @return the stitching statistics
   */
  public StitchingStatistics getStitchingStatistics() {
    return stitchingStatistics;
  }


  private static void showError(String details) {
    Log.msg(LogType.MANDATORY, details);

    if (!GraphicsEnvironment.isHeadless()) {
      JOptionPane.showMessageDialog(null, "Error: Cancelling stitching.\n"
          + "Check log for more details.", "Error", JOptionPane.ERROR_MESSAGE);
    }
  }


  public <T> boolean checkOutputGridMemory(TileGrid<ImageTile<T>> grid)
      throws FileNotFoundException {

    ImageTile<T> tile = grid.getTileThatExists();
    tile.readTile();

    long width = TileGridUtils.getFullImageWidth(grid, tile.getWidth());
    long height = TileGridUtils.getFullImageHeight(grid, tile.getHeight());

    long numberPixels = width * height;
    if (numberPixels >= (long) Integer.MAX_VALUE)
      return false;

    long byteDepth = tile.getBitDepth() / 8;

    // Account for the memory required to hold a single image
    // Output image is build by read, copy into output, free sequentially
    long requiredMemoryBytes = tile.getHeight() * tile.getWidth() * byteDepth;

    switch (params.getOutputParams().getBlendingMode()) {
      case OVERLAY:
        // requires enough memory to hold the output image
        requiredMemoryBytes += numberPixels * byteDepth; // output image matches bit depth
        break;

      case AVERAGE:
        // Account for average blend data
        if (byteDepth == 3) {
          requiredMemoryBytes +=
              numberPixels * 3 * 4; // sums = new float[numChannels][height][width];
          requiredMemoryBytes +=
              numberPixels * 3 * 4; // counts = new int[numChannels][height][width];
        } else {
          requiredMemoryBytes +=
              numberPixels * 4; // sums = new float[numChannels][height][width];
          requiredMemoryBytes +=
              numberPixels * 4; // counts = new int[numChannels][height][width];
        }
        requiredMemoryBytes += numberPixels * byteDepth; // output image matches bit depth

        break;
      case LINEAR:
        // Account for the pixel weights
        requiredMemoryBytes +=
            tile.getHeight() * tile.getWidth()
                * 8; // lookupTable = new double[initImgHeight][initImgWidth];

        // Account for average blend data
        if (byteDepth == 3) {
          requiredMemoryBytes +=
              numberPixels * 8; // pixelSums = new double[numChannels][height][width];
          requiredMemoryBytes +=
              numberPixels * 8; // weightSums = new double[numChannels][height][width];
        } else {
          requiredMemoryBytes +=
              numberPixels * 8; // pixelSums = new double[numChannels][height][width];
          requiredMemoryBytes +=
              numberPixels * 8; // weightSums = new double[numChannels][height][width];
        }

        requiredMemoryBytes += numberPixels * byteDepth; // output image matches bit depth
        break;

      default:
        break;
    }

    // pad with 10%
    requiredMemoryBytes *= 1.1;

    return requiredMemoryBytes < Runtime.getRuntime().maxMemory();
  }


}
