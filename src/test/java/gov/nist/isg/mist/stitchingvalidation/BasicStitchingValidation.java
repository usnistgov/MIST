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

package gov.nist.isg.mist.stitchingvalidation;

import gov.nist.isg.mist.stitching.lib.executor.StitchingExecutor;
import gov.nist.isg.mist.stitching.lib.executor.StitchingExecutor.StitchingType;
import gov.nist.isg.mist.stitching.gui.panels.advancedTab.parallelPanels.CUDAPanel;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.lib.exceptions.StitchingException;
import gov.nist.isg.mist.stitching.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;

import java.io.File;

public class BasicStitchingValidation {

  static {
    LibraryUtils.initalize();
  }

  private static final String STITCHING_PARAMS_FILE = "stitching-params.txt";


//    private static String validationRootFolder = "C:\\majurski\\image-data\\1h_Wet_10Perc";
  private static String validationRootFolder = "C:\\majurski\\image-data\\John_Elliot\\uncompressed_synthetic_grid";
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


    CUDAPanel cudaPanel = new CUDAPanel();
//        JFrame frame = new JFrame("Select CUDA Devices");
//        JOptionPane.showMessageDialog(frame, cudaPanel);

    Log.setLogLevel(LogType.MANDATORY);
    StitchingAppParams params;


    System.out.println("Running: " + rootFolder.getAbsolutePath());
    params = new StitchingAppParams();

    File paramFile = new File(rootFolder, STITCHING_PARAMS_FILE);

    params.loadParams(paramFile);

    params.getInputParams().setImageDir(rootFolder.getAbsolutePath());
    params.getAdvancedParams().setNumCPUThreads(Runtime.getRuntime().availableProcessors());
    params.getAdvancedParams().setPlanPath(fftwPlanPath);
    params.getAdvancedParams().setFftwLibraryPath(fftwLibraryPath);
    params.getAdvancedParams().setCudaDevices(cudaPanel.getSelectedDevices());
    params.getOutputParams().setOutFilePrefix("img-");
    params.getOutputParams().setOutputFullImage(false);
    params.getOutputParams().setDisplayStitching(false);
    params.getAdvancedParams().setNumCPUThreads(24);
    params.getAdvancedParams().setUseDoublePrecision(false);


    double elapsedTime;
    double nbIter = 1;

    File metaDataPath = new File(rootFolder, "basic");
    params.getOutputParams().setOutputPath(metaDataPath.getAbsolutePath());


//    // Run Java 32bit
//    params.getAdvancedParams().setProgramType(StitchingType.JAVA);
//    params.getOutputParams().setOutFilePrefix("java32-");
//    params.getAdvancedParams().setUseDoublePrecision(false);
//    elapsedTime = 0;
//    for (int i = 0; i < nbIter; i++) {
//      long startTime = System.currentTimeMillis();
//      try {
//        (new StitchingExecutor(params)).runStitching(false, false, false);
//      } catch (StitchingException e) {
//        Log.msg(LogType.MANDATORY, e.getMessage());
//      }
//      elapsedTime = elapsedTime + (System.currentTimeMillis() - startTime);
//    }
//    System.out.println("Java 32bit Elapsed: " + (elapsedTime / nbIter) + " ms");
//
//
//    // Run Java 64bit
//    params.getAdvancedParams().setProgramType(StitchingType.JAVA);
//    params.getOutputParams().setOutFilePrefix("java64-");
//    params.getAdvancedParams().setUseDoublePrecision(true);
//    elapsedTime = 0;
//    for (int i = 0; i < nbIter; i++) {
//      long startTime = System.currentTimeMillis();
//      try {
//        (new StitchingExecutor(params)).runStitching(false, false, false);
//      } catch (StitchingException e) {
//        Log.msg(LogType.MANDATORY, e.getMessage());
//      }
//      elapsedTime = elapsedTime + (System.currentTimeMillis() - startTime);
//    }
//    System.out.println("Java 64bit Elapsed: " + (elapsedTime / nbIter) + " ms");
//
//
//
//    // Run FFTW 32bit
//    params.getAdvancedParams().setProgramType(StitchingType.FFTW);
//    params.getOutputParams().setOutFilePrefix("fftw32-");
//    params.getAdvancedParams().setUseDoublePrecision(false);
//    elapsedTime = 0;
//    for (int i = 0; i < nbIter; i++) {
//      long startTime = System.currentTimeMillis();
//      try {
//        (new StitchingExecutor(params)).runStitching(false, false, false);
//      } catch (StitchingException e) {
//        Log.msg(LogType.MANDATORY, e.getMessage());
//      }
//      elapsedTime = elapsedTime + (System.currentTimeMillis() - startTime);
//    }
//    System.out.println("FFTW 32bit Elapsed: " + (elapsedTime / nbIter) + " ms");
//
//
//
//    // Run FFTW 64bit
//    params.getAdvancedParams().setProgramType(StitchingType.FFTW);
//    params.getOutputParams().setOutFilePrefix("fftw64-");
//    params.getAdvancedParams().setUseDoublePrecision(true);
//    elapsedTime = 0;
//    for (int i = 0; i < nbIter; i++) {
//      long startTime = System.currentTimeMillis();
//      try {
//        (new StitchingExecutor(params)).runStitching(false, false, false);
//      } catch (StitchingException e) {
//        Log.msg(LogType.MANDATORY, e.getMessage());
//      }
//      elapsedTime = elapsedTime + (System.currentTimeMillis() - startTime);
//    }
//    System.out.println("FFTW 64bit Elapsed: " + (elapsedTime / nbIter) + " ms");



    // Run CUDA 32bit
    params.getAdvancedParams().setProgramType(StitchingType.CUDA);
    params.getOutputParams().setOutFilePrefix("cuda32-");
    params.getAdvancedParams().setUseDoublePrecision(false);
    elapsedTime = 0;
    for (int i = 0; i < nbIter; i++) {
      long startTime = System.currentTimeMillis();
      try {
        (new StitchingExecutor(params)).runStitching(false, false, false);
      } catch (StitchingException e) {
        Log.msg(LogType.MANDATORY, e.getMessage());
      }
      elapsedTime = elapsedTime + (System.currentTimeMillis() - startTime);
    }
    System.out.println("CUDA 32bit Elapsed: " + (elapsedTime / nbIter) + " ms");



//    // Run CUDA 64bit
//    params.getAdvancedParams().setProgramType(StitchingType.CUDA);
//    params.getOutputParams().setOutFilePrefix("cuda64-");
//    params.getAdvancedParams().setUseDoublePrecision(true);
//    elapsedTime = 0;
//    for (int i = 0; i < nbIter; i++) {
//      long startTime = System.currentTimeMillis();
//      try {
//        (new StitchingExecutor(params)).runStitching(false, false, false);
//      } catch (StitchingException e) {
//        Log.msg(LogType.MANDATORY, e.getMessage());
//      }
//      elapsedTime = elapsedTime + (System.currentTimeMillis() - startTime);
//    }
//    System.out.println("CUDA 64bit Elapsed: " + (elapsedTime / nbIter) + " ms");




    System.exit(1);
  }
}
