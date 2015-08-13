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
// characteristic. We would appreciate acknowledgment if the software
// is used. This software can be redistributed and/or modified freely
// provided that any derivative works bear some notice that they are
// derived from it, and any modified versions bear some notice that
// they have been modified.
//
// ================================================================

// ================================================================
//
// Author: tjb3
// Date: May 10, 2013 2:58:58 PM EST
//
// Time-stamp: <May 10, 2013 2:58:58 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitchingruntime;

import gov.nist.isg.mist.stitching.gui.StitchingStatistics;
import gov.nist.isg.mist.stitching.gui.executor.StitchingExecutor;
import gov.nist.isg.mist.stitching.gui.executor.StitchingExecutor.StitchingType;
import gov.nist.isg.mist.stitching.gui.panels.advancedTab.parallelPanels.CUDAPanel;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.gui.params.objects.CudaDeviceParam;
import gov.nist.isg.mist.stitching.lib.exceptions.StitchingException;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwPlanType;
import gov.nist.isg.mist.stitching.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.optimization.OptimizationUtils;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImageStitchingRunTime {

  static {
    LibraryUtils.initalize();
  }  

  private static final String STITCHING_PARAMS_FILE = "stitching-params.txt";


//  private static String validationRootFolder = "C:\\Users\\tjb3\\StitchingPaperDatasets";
//private static String fftwPlanPath = "C:\\Users\\tjb3\\Documents\\MIST-ISG\\MIST\\lib\\fftw\\fftPlans";
//  private static String fftwLibraryPath = "C:\\Users\\tjb3\\Documents\\MIST-ISG\\MIST\\lib\\fftw";

  private static String validationRootFolder = "E:\\image-data\\Stitching_Paper_Data\\datasets";
//  private static String validationRootFolder = "C:\\majurski\\image-data\\Stitching_Paper_Data";
  private static String fftwPlanPath = "C:\\Fiji.app\\lib\\fftw\\fftPlans";
  private static String fftwLibraryPath = "C:\\Fiji.app\\lib\\fftw";

  private static int NUM_RUNS = 10;

  public static void main(String [] args)
  {


    boolean useMLE = false;

    if (args.length >= 0) {
      switch(args.length)
      {
        case 1:
          validationRootFolder = args[0];
          break;
        case 2:
          validationRootFolder = args[0];
          fftwPlanPath = args[1];
          break;
        case 3:
          validationRootFolder = args[0];
          fftwPlanPath = args[1];
          fftwLibraryPath = args[2];
          break;
        case 4:
          validationRootFolder = args[0];
          fftwPlanPath = args[1];
          fftwLibraryPath = args[2];
          String tmp = args[3];
          if(tmp.contains("-mle")) {
            useMLE = true;
            System.out.println("Using MLE and outlier filtering");
          }
          break;
        default:
          System.out.println("Usage: ImageStitchingRunTime <rootFolder> <fftwPlanPath> <fftwLibraryPath> <-mle>");
          break;
      }
    }else{
      System.out.println("Usage: ImageStitchingRunTime <rootFolder> <fftwPlanPath> <fftwLibraryPath> <-mle>");
    }


    System.out.println("rootDir: \"" + validationRootFolder + "\"");
    System.out.println("fftwPlanPath: \"" + fftwPlanPath + "\"");
    System.out.println("fftwLibPath: \"" + fftwLibraryPath + "\"");


    runFolder(useMLE);


    System.exit(1);
  }

  private static void runFolder(boolean useMLE) {
    // get all folders in root folder
    File rootFolder = new File(validationRootFolder);
    if (!rootFolder.exists() && !rootFolder.isDirectory())
    {
      System.out.println("Error: Unable to find root folder: " + validationRootFolder);
      System.exit(1);
    }

    File[] roots = rootFolder.listFiles();

    CUDAPanel cudaPanel = new CUDAPanel();

//    JFrame frame = new JFrame("Select CUDA Devices");
//    JOptionPane.showMessageDialog(frame, cudaPanel);

//    Log.setLogLevel(LogType.NONE);
    Log.setLogLevel(LogType.MANDATORY);

    StitchingAppParams params;

    File runtimeResults;
    if(useMLE) {
      runtimeResults = new File(validationRootFolder + File.separator + "mle-runtimes.txt");
    }else{
      runtimeResults = new File(validationRootFolder + File.separator + "runtimes.txt");
    }
    try {
      FileWriter writer = new FileWriter(runtimeResults);
      writer.write("testCase, totalTime" + "\n");

      for (File r : roots) {

        if (!r.isDirectory())
          continue;

        if(!r.getAbsolutePath().contains("Worms"))
          continue;

        params = new StitchingAppParams();

        File paramFile = new File(r, STITCHING_PARAMS_FILE);

        if (!paramFile.exists())
          continue;

        System.out.println("Running: " + r.getAbsolutePath());

        params.loadParams(paramFile);

        List<CudaDeviceParam> cudaDevices = cudaPanel.getSelectedDevices();





        params.getInputParams().setImageDir(r.getAbsolutePath());
        params.getAdvancedParams().setNumCPUThreads(Runtime.getRuntime().availableProcessors());
        params.getAdvancedParams().setPlanPath(fftwPlanPath);
        params.getAdvancedParams().setFftwLibraryPath(fftwLibraryPath);
        params.getAdvancedParams().setFftwPlanType(FftwPlanType.MEASURE);
        params.getAdvancedParams().setCudaDevices(cudaPanel.getSelectedDevices());
        params.getOutputParams().setOutputMeta(false);
        params.getOutputParams().setOutputPath(
            r.getAbsolutePath() + File.separator + "RunTimeResults");
        // set the metadata path to the output path

        params.getOutputParams().setOutputFullImage(false);
        params.getOutputParams().setDisplayStitching(false);
//      params.getAdvancedParams().setNumCPUThreads(8);

        if(useMLE) {
          params.getAdvancedParams()
              .setOverlapComputationType(OptimizationUtils.OverlapType.MLE);
          params.getAdvancedParams()
              .setTranslationFilterType(OptimizationUtils.TranslationFilterType.Outlier);
        }else {
          params.getAdvancedParams()
              .setOverlapComputationType(OptimizationUtils.OverlapType.Heuristic);
          params.getAdvancedParams()
              .setTranslationFilterType(OptimizationUtils.TranslationFilterType.StandardDeviation);
        }


        for (StitchingType t : StitchingType.values()) {
          String testCase = t.toString() + "-" + r.getName();

          if (t == StitchingType.AUTO || t == StitchingType.JAVA)
            continue;

          if (t == StitchingType.CUDA) {
            if (!cudaPanel.isCudaAvailable())
              continue;
          }



          double totalRunTime = 0;
          for (int run = 0; run < NUM_RUNS; run++) {

            System.out.println("Run " + run + " Stitching Type: " + t + " " + testCase);

//        File metaDataPath = new File(r, t.name().toLowerCase());
//        File metaDataPath = new File(r, "seq");
//        params.getOutputParams().setMetadataPath(metaDataPath.getAbsolutePath());
            params.getAdvancedParams().setProgramType(t);
            params.getAdvancedParams().setNumFFTPeaks(2);

            StitchingExecutor executor = new StitchingExecutor(params);

            try {
              executor.runStitching(false, false, false);
            } catch (StitchingException e) {
              Log.msg(LogType.MANDATORY, e.getMessage());
            }

            StitchingStatistics stats = executor.getStitchingStatistics();
            totalRunTime += stats.getDuration(StitchingStatistics.RunTimers.TotalStitchingTime);

          }

          double avgRunTime = totalRunTime / ((double) NUM_RUNS);
          writer.write(testCase + ", " + avgRunTime + "\n");
          writer.flush();
        }

      }


      writer.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
