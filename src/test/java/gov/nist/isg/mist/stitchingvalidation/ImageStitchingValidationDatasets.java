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

package gov.nist.isg.mist.stitchingvalidation;

import gov.nist.isg.mist.stitching.lib.executor.StitchingExecutor;
import gov.nist.isg.mist.stitching.lib.executor.StitchingExecutor.StitchingType;
import gov.nist.isg.mist.stitching.gui.panels.advancedTab.parallelPanels.CUDAPanel;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.lib.exceptions.StitchingException;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;

import javax.swing.*;

import java.io.File;

public class ImageStitchingValidationDatasets {

  static {
    LibraryUtils.initalize();
  }

  private static final String STITCHING_PARAMS_FILE = "stitching-params.txt";


  private static String validationRootFolder = "E:\\image-data\\Image_Stitching_Validation_Datasets";
  private static String fftwPlanPath = "C:\\Fiji.app\\lib\\fftw\\fftPlans";
  private static String fftwLibraryPath = "C:\\Fiji.app\\lib\\fftw";


  public static void main(String[] args) {
    if (args.length > 0) {
      validationRootFolder = args[0];
    }


    // get all folders in root folder
    File rootFolder = new File(validationRootFolder);
    if (!rootFolder.exists() && !rootFolder.isDirectory()) {
      System.out.println("Error: Unable to find root folder: " + validationRootFolder);
      System.exit(1);
    }

    File[] roots = rootFolder.listFiles();

    CUDAPanel cudaPanel = new CUDAPanel();

//    JFrame frame = new JFrame("Select CUDA Devices");
//    JOptionPane.showMessageDialog(frame, cudaPanel);

    Log.setLogLevel(LogType.MANDATORY);

    StitchingAppParams params;

    for (File r : roots) {


      if (!r.isDirectory())
        continue;

      System.out.println("Running: " + r.getAbsolutePath());
      params = new StitchingAppParams();

      File paramFile = new File(r, STITCHING_PARAMS_FILE);

      params.loadParams(paramFile);

      params.getInputParams().setImageDir(r.getAbsolutePath());
      params.getAdvancedParams().setNumCPUThreads(Runtime.getRuntime().availableProcessors());
      params.getAdvancedParams().setPlanPath(fftwPlanPath);
      params.getAdvancedParams().setFftwLibraryPath(fftwLibraryPath);
      params.getAdvancedParams().setCudaDevices(cudaPanel.getSelectedDevices());
      params.getOutputParams().setOutputFullImage(false);
      params.getOutputParams().setDisplayStitching(false);
      params.getAdvancedParams().setNumCPUThreads(24);


      params.getOutputParams().setOutputFullImage(true);
      boolean assembleFromMetadata = true;
      params.getInputParams().setAssembleFromMetadata(assembleFromMetadata);

      for (StitchingType t : StitchingType.values()) {
        if (t == StitchingType.AUTO)
          continue;

        if (t == StitchingType.CUDA) {
          if (!cudaPanel.isCudaAvailable())
            continue;
        }


        File metaDataPath = new File(r, "PRECISION");
        params.getOutputParams().setOutputPath(metaDataPath.getAbsolutePath());
        params.getInputParams().setGlobalPositionsFile(metaDataPath.getAbsolutePath() + File.separator + t.name().toLowerCase() + "32-global-positions-0.txt");


        // Run the single precision version
        System.out.println("Stitching Type: " + t + "32");
        params.getAdvancedParams().setUseDoublePrecision(false);
        params.getOutputParams().setOutFilePrefix(t.name().toLowerCase() + "32-");
        params.getAdvancedParams().setProgramType(t);

        try {
          new StitchingExecutor(params).runStitching(assembleFromMetadata, false, false);
        } catch (StitchingException e) {
          Log.msg(LogType.MANDATORY, e.getMessage());
        }


        params.getInputParams().setGlobalPositionsFile(metaDataPath.getAbsolutePath() + File.separator + t.name().toLowerCase() + "64-global-positions-0.txt");
        // Run the double precision version
        System.out.println("Stitching Type: " + t + "64");
        params.getAdvancedParams().setUseDoublePrecision(true);
        params.getOutputParams().setOutFilePrefix(t.name().toLowerCase() + "64-");
        params.getAdvancedParams().setProgramType(t);

        try {
          new StitchingExecutor(params).runStitching(assembleFromMetadata, false, false);
        } catch (StitchingException e) {
          Log.msg(LogType.MANDATORY, e.getMessage());
        }

      }
    }


    System.exit(1);
  }
}
