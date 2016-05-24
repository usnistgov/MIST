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


package gov.nist.isg.mist.stitchingruntime;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import gov.nist.isg.mist.stitching.gui.StitchingStatistics;
import gov.nist.isg.mist.stitching.gui.panels.advancedTab.parallelPanels.CUDAPanel;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.gui.params.objects.CudaDeviceParam;
import gov.nist.isg.mist.stitching.lib.exceptions.StitchingException;
import gov.nist.isg.mist.stitching.lib.executor.StitchingExecutor;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwPlanType;
import gov.nist.isg.mist.stitching.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.stitching.lib.log.Log;

/**
 * Created by mmajursk on 5/13/2016.
 */
public class LowMemoryStitchingEvaluation {

  static {
    LibraryUtils.initalize();
  }

  private static final String STITCHING_PARAMS_FILE = "stitching-params.txt";

  private static String datasetFolder =
      "E:\\image-data\\MIST_Performance_Evaluation\\low-memory\\imgs\\";

  private static String fftwPlanPath = "C:\\Fiji.app\\lib\\fftw\\fftPlans";
  private static String fftwLibraryPath = "C:\\Fiji.app\\lib\\fftw";


  public static void main(String[] args) {


    if (args.length >= 0) {
      switch (args.length) {
        case 1:
          datasetFolder = args[0];
          break;
        case 2:
          datasetFolder = args[0];
          fftwPlanPath = args[1];
          break;
        case 3:
          datasetFolder = args[0];
          fftwPlanPath = args[1];
          fftwLibraryPath = args[2];
          break;
        default:
          System.out.println("Usage: ImageStitchingRunTime <datasetFolder> <fftwPlanPath> " +
              "<fftwLibraryPath>");
          break;
      }
    } else {
      System.out.println("Usage: ImageStitchingRunTime <datasetFolder> <fftwPlanPath> <fftwLibraryPath>");
    }

    System.out.println("datasetFolder: \"" + datasetFolder + "\"");
    System.out.println("fftwPlanPath: \"" + fftwPlanPath + "\"");
    System.out.println("fftwLibPath: \"" + fftwLibraryPath + "\"");

    runFolder();


    System.exit(1);
  }

  private static void runFolder() {
    // get all folders in root folder
    File folder = new File(datasetFolder);
    if (!folder.exists() && !folder.isDirectory()) {
      System.out.println("Error: Unable to find root folder: " + datasetFolder);
      System.exit(1);
    }

    Log.setLogLevel(Log.LogType.MANDATORY);
    StitchingAppParams params;

    if (!folder.isDirectory())
      return;

    params = new StitchingAppParams();
    File paramFile = new File(folder, STITCHING_PARAMS_FILE);

    if (!paramFile.exists())
      return;
    params.loadParams(paramFile);

//    List<CudaDeviceParam> cudaDevices = cudaPanel.getSelectedDevices();

    params.getInputParams().setImageDir(folder.getAbsolutePath());
    params.getAdvancedParams().setNumCPUThreads(Runtime.getRuntime().availableProcessors());
    params.getAdvancedParams().setPlanPath(fftwPlanPath);
    params.getAdvancedParams().setFftwLibraryPath(fftwLibraryPath);
    params.getAdvancedParams().setFftwPlanType(FftwPlanType.MEASURE);
//    params.getAdvancedParams().setCudaDevices(cudaPanel.getSelectedDevices());
    params.getOutputParams().setOutputPath(folder.getAbsolutePath() + File.separator + "results");

    params.getOutputParams().setOutputMeta(true);
    params.getOutputParams().setOutputFullImage(false);
    params.getOutputParams().setDisplayStitching(false);
//      params.getAdvancedParams().setNumCPUThreads(8);

    params.getAdvancedParams().setProgramType(StitchingExecutor.StitchingType.AUTO);
    params.getAdvancedParams().setNumFFTPeaks(2);

    StitchingExecutor executor = new StitchingExecutor(params);

    try {
      executor.runStitching(false, false, false);
    } catch (StitchingException e) {
      Log.msg(Log.LogType.MANDATORY, e.getMessage());
    }

//    StitchingStatistics stats = executor.getStitchingStatistics();

  }

}

