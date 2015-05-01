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
import gov.nist.isg.mist.stitching.lib.exceptions.StitchingException;
import gov.nist.isg.mist.stitching.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ImageStitchingRunTime {

  static {
    LibraryUtils.initalize();
  }  

  private static final String STITCHING_PARAMS_FILE = "statistics.txt";


  private static String validationRootFolder = "C:\\Users\\tjb3\\StitchingPaperDatasets";
  private static String fftwPlanPath = "C:\\Users\\tjb3\\Documents\\MIST-ISG\\MIST\\lib\\fftw\\fftPlans";
  private static String fftwLibraryPath = "C:\\Users\\tjb3\\Documents\\MIST-ISG\\MIST\\lib\\fftw";
  private static int NUM_RUNS = 10;

  public static void main(String [] args)
  {


    if (args.length > 0)
    {
      validationRootFolder = args[0];
    }


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

    File runtimeResults = new File(validationRootFolder + "\\runtimes.txt");
    try {
      FileWriter writer = new FileWriter(runtimeResults);
      writer.write("testCase, relDispTime, globalOptTime, fullImageTime, totalTime" + "\n");

      for (File r : roots) {

        if (!r.isDirectory())
          continue;

        params = new StitchingAppParams();

        File paramFile = new File(r, STITCHING_PARAMS_FILE);

        if (!paramFile.exists())
          continue;

        System.out.println("Running: " + r.getAbsolutePath());

        params.loadParams(paramFile);

        params.getInputParams().setImageDir(r.getAbsolutePath());
        params.getAdvancedParams().setNumCPUThreads(Runtime.getRuntime().availableProcessors());
        params.getAdvancedParams().setPlanPath(fftwPlanPath);
        params.getAdvancedParams().setFftwLibraryPath(fftwLibraryPath);
        params.getAdvancedParams().setCudaDevices(cudaPanel.getSelectedDevices());
        params.getOutputParams().setOutputMeta(false);
        params.getOutputParams().setOutputPath(r.getAbsolutePath() + "\\results");

//      params.getOutputParams().setOutputFullImage(false);
//      params.getOutputParams().setDisplayStitching(false);
//      params.getAdvancedParams().setNumCPUThreads(8);


        for (StitchingType t : StitchingType.values()) {
          String testCase = t.toString() + "-" + r.getName();

          long globalOptTime = 0L;
          long outputFullImageTime = 0L;
          long relDispTime = 0L;
          long totalTime = 0L;

          if (t == StitchingType.AUTO || t == StitchingType.JAVA)
            continue;

          if (t == StitchingType.CUDA) {
            if (!cudaPanel.isCudaAvailable())
              continue;
          }

          for (int run = 0; run < NUM_RUNS; run++) {


            System.out.println("Run " + run + " Stitching Type: " + t);

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
            globalOptTime += stats.getDuration(StitchingStatistics.RunTimers.GlobalOptimizationTime);
            outputFullImageTime += stats.getDuration(StitchingStatistics.RunTimers.OutputFullImageTileTime);
            relDispTime += stats.getDuration(StitchingStatistics.RunTimers.RelativeDisplacementTime);
            totalTime += stats.getDuration(StitchingStatistics.RunTimers.TotalStitchingTime);

          }

          writer.write(testCase + ", " + relDispTime/NUM_RUNS + ", " + globalOptTime/NUM_RUNS + ", " + outputFullImageTime/NUM_RUNS + ", " + totalTime/NUM_RUNS + "\n");
          writer.flush();
        }

      }

      writer.close();

    } catch (IOException e) {
      e.printStackTrace();
    }

    System.exit(1);
  }
}
