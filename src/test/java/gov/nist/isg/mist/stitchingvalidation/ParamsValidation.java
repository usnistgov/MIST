
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
// Date: May 10, 2013 2:58:58 PM EST
//
// Time-stamp: <May 10, 2013 2:58:58 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitchingvalidation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.lib.exceptions.StitchingException;
import gov.nist.isg.mist.stitching.lib.executor.StitchingExecutor;
import gov.nist.isg.mist.stitching.lib.executor.StitchingExecutor.StitchingType;
import gov.nist.isg.mist.stitching.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;

public class ParamsValidation {

  static {
    LibraryUtils.initalize();
  }

  private static final String STITCHING_PARAMS_FILE = "stitching-params.txt";


  private static String validationRootFolder = "C:\\majurski\\image-data\\Image_Stitching_Validation_Datasets";
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

    //    CUDAPanel cudaPanel = new CUDAPanel();
    //    JFrame frame = new JFrame("Select CUDA Devices");
    //    JOptionPane.showMessageDialog(frame, cudaPanel);

    Log.setLogLevel(LogType.NONE);

    StitchingAppParams params;

    long startTime = System.currentTimeMillis();

    for (File r : roots) {

      System.out.println("Running: " + r.getAbsolutePath());
      if (!r.isDirectory())
        continue;

      params = new StitchingAppParams();

      File paramFile = new File(r, STITCHING_PARAMS_FILE);

      params.loadParams(paramFile);

      params.getInputParams().setImageDir(r.getAbsolutePath());
      params.getAdvancedParams().setNumCPUThreads(Runtime.getRuntime().availableProcessors());
      params.getAdvancedParams().setPlanPath(fftwPlanPath);
      params.getAdvancedParams().setFftwLibraryPath(fftwLibraryPath);
      //      params.getAdvancedParams().setCudaDevices(cudaPanel.getSelectedDevices());
      params.getOutputParams().setOutputFullImage(false);

      StitchingType t = StitchingType.FFTW;
      System.out.println("Stitching Type: " + t);

      File metaDataPath = new File(r, t.name().toLowerCase());
      params.getOutputParams().setOutputPath(metaDataPath.getAbsolutePath());
      params.getAdvancedParams().setProgramType(t);
      params.getOutputParams().setDisplayStitching(false);

      String basePath = params.getOutputParams().getOutputPath();
      int[] nbPeaksVals = new int[]{1, 2, 3, 4, 5, 6, 8, 10};

      for (int nP = 0; nP < nbPeaksVals.length; nP++) {
        int nbPeaks = nbPeaksVals[nP];

        params.getAdvancedParams().setNumFFTPeaks(nbPeaks);

        params.getOutputParams().setOutputPath(
            basePath + File.separator + "peaks" + nbPeaks + File.separator);

        File f = new File(params.getOutputParams().getOutputPath() + "img-statistics.txt");
        if (!f.exists()) {
          System.out.println("Nb Peaks: " + nbPeaks);
          StitchingExecutor executor = new StitchingExecutor(params);
          try {
            executor.runStitching(false, false, false);
          } catch (StitchingException e) {
            Log.msg(LogType.MANDATORY, e.getMessage());
          }
        }


      }
    }

    long endTime = System.currentTimeMillis();

    System.out.println("Total time: " + (endTime - startTime));

    File results = new File("validationDataSetResults.txt");

    FileWriter writer = null;
    try {
      writer = new FileWriter(results);
    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      if (writer != null)
        writer.write("Runtime for " + roots.length + " experiements: " + (endTime - startTime));

    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      if (writer != null)
        writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.exit(1);
  }
}
