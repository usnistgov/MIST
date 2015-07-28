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

  private static String validationRootFolder = "E:\\image-data\\Stitching_Paper_Data";
  private static String fftwPlanPath = "C:\\Fiji.app\\lib\\fftw\\fftPlans";
  private static String fftwLibraryPath = "C:\\Fiji.app\\lib\\fftw";

  private static int NUM_RUNS = 10;

  public static void main(String [] args)
  {

    if (args.length >= 0)
    {
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
        default:
          System.out.println("Usage: ImageStitchingRunTime <rootFolder> <fftwPlanPath> <fftwLibraryPath>");
          break;
      }
    }


    System.out.println("rootDir: \"" + validationRootFolder + "\"");
    System.out.println("fftwPlanPath: \"" + fftwPlanPath + "\"");
    System.out.println("fftwLibPath: \"" + fftwLibraryPath + "\"");


    // get all folders in root folder
    File rootFolder = new File(validationRootFolder);
    if (!rootFolder.exists() && !rootFolder.isDirectory())
    {
      System.out.println("Error: Unable to find root folder: " + validationRootFolder);
      System.exit(1);
    }    

    File[] roots = rootFolder.listFiles();

    CUDAPanel cudaPanel = new CUDAPanel();

    JFrame frame = new JFrame("Select CUDA Devices");
    JOptionPane.showMessageDialog(frame, cudaPanel);

    Log.setLogLevel(LogType.NONE);
//    Log.setLogLevel(LogType.MANDATORY);

    StitchingAppParams params;

    File runtimeResults = new File(validationRootFolder + File.separator + "runtimes.txt");
    try {
      FileWriter writer = new FileWriter(runtimeResults);
      writer.write("testCase, totalTime" + "\n");

      for (File r : roots) {

        if (!r.isDirectory())
          continue;

//        if (!r.getName().contains("Paper_Sample")) {
//          System.out.println("Skipping " + r.getName());
//          continue;
//        }





        params = new StitchingAppParams();

        File paramFile = new File(r, STITCHING_PARAMS_FILE);

        if (!paramFile.exists())
          continue;

        System.out.println("Running: " + r.getAbsolutePath());

        params.loadParams(paramFile);

        List<CudaDeviceParam> cudaDevices = cudaPanel.getSelectedDevices();



        List<CudaDeviceParam> run780 = new ArrayList<CudaDeviceParam>();
        List<CudaDeviceParam> runOneC2070 = new ArrayList<CudaDeviceParam>();
        List<CudaDeviceParam> runTwoC2070 = new ArrayList<CudaDeviceParam>();

        for (CudaDeviceParam dev : cudaDevices)
        {
          if (dev.getName().contains("780")) {
            System.out.println("Adding " + dev.getName() + " to 780 test");
            run780.add(dev);
          }
          if (dev.getName().contains("Tesla") && runOneC2070.size() == 0) {
            System.out.println("Adding " + dev.getName() + " to one 2070 test");
            runOneC2070.add(dev);
          }
          if(dev.getName().contains("Tesla")) {
            System.out.println("Adding " + dev.getName() + " to two 2070 test");
            runTwoC2070.add(dev);
          }
        }



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

        params.getAdvancedParams().setOverlapComputationType(OptimizationUtils.OverlapType.Heuristic);
        params.getAdvancedParams().setTranslationFilterType(OptimizationUtils.TranslationFilterType.StandardDeviation);


        for (StitchingType t : StitchingType.values()) {
          String testCase = t.toString() + "-" + r.getName();

          if (t == StitchingType.AUTO || t == StitchingType.JAVA)
            continue;

          if (t == StitchingType.CUDA) {
            if (!cudaPanel.isCudaAvailable())
              continue;
          }



          for (int cudaRun = 0; cudaRun < 3; cudaRun++) {
            double totalRunTime = 0;

            if (t != StitchingType.CUDA)
              cudaRun = 3;
            else
            {
              switch(cudaRun) {
                case 0:
                  params.getAdvancedParams().setCudaDevices(run780);
                  testCase = t.toString() + "-" + r.getName() + "-GTX780";
                  break;
                case 1:
                  params.getAdvancedParams().setCudaDevices(runOneC2070);
                  testCase = t.toString() + "-" + r.getName() + "-1C2070";

                  break;
                case 2:
                  params.getAdvancedParams().setCudaDevices(runTwoC2070);
                  testCase = t.toString() + "-" + r.getName() + "-2C2070";
                  break;
              }
            }
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

      }

      writer.close();

    } catch (IOException e) {
      e.printStackTrace();
    }

    System.exit(1);
  }
}
