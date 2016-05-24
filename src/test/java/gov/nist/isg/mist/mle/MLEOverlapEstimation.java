
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

package gov.nist.isg.mist.mle;

import java.io.File;

import gov.nist.isg.mist.stitching.gui.panels.advancedTab.parallelPanels.CUDAPanel;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.lib.exceptions.StitchingException;
import gov.nist.isg.mist.stitching.lib.executor.StitchingExecutor;
import gov.nist.isg.mist.stitching.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.stitching.lib.log.Log;

/**
 * Test class for MLE overlap estimation.
 *
 * @author Michael Majurski
 */
public class MLEOverlapEstimation {


  static {
    LibraryUtils.initalize();
  }

  private static final String STITCHING_PARAMS_FILE = "stitching-params.txt";


//  private static String validationRootFolder = "/Users/mmajurski/Workspace/image-data/Image_Stitching_Validation_Datasets";
//  private static String fftwPlanPath = "/Applications/Fiji.app/lib/fftw/fftPlans";
//  private static String fftwLibraryPath = "/usr/local/lib/libfftw3.3.dylib";

  private static String validationRootFolder = "C:\\majurski\\image-data\\Image_Stitching_Validation_Datasets\\";
  private static String fftwPlanPath = "C:\\Fiji.app\\lib\\fftw\\fftPlans\\";
  private static String fftwLibraryPath = "C:\\Fiji.app\\lib\\fftw\\";


  public static void main(String[] args) {


    File rootFolder = new File(validationRootFolder);
    if (!rootFolder.exists() && !rootFolder.isDirectory()) {
      System.out.println("Error: Unable to find root folder: " + validationRootFolder);
      System.exit(1);
    }

    File[] roots = rootFolder.listFiles();

    CUDAPanel cudaPanel = new CUDAPanel();

    Log.setLogLevel(Log.LogType.MANDATORY);
    StitchingAppParams params;


    for (File r : roots) {
      if (!r.isDirectory())
        continue;

//      if(r.getAbsolutePath().contains("1h_Wet")) continue;
//      if(r.getAbsolutePath().contains("24h_Dry")) continue;
//      if(r.getAbsolutePath().contains("KB_")) continue;
//      if(r.getAbsolutePath().contains("Keana_Scott_gauss3")) continue;


      System.out.println("Running: " + r.getAbsolutePath());
      params = new StitchingAppParams();

      File paramFile = new File(r, STITCHING_PARAMS_FILE);

      params.loadParams(paramFile);

      params.getInputParams().setImageDir(r.getAbsolutePath());
      params.getAdvancedParams().setNumCPUThreads(Runtime.getRuntime().availableProcessors());
      params.getAdvancedParams().setPlanPath(fftwPlanPath);
      params.getAdvancedParams().setFftwLibraryPath(fftwLibraryPath);

      if (cudaPanel.isCudaAvailable())
        params.getAdvancedParams().setCudaDevices(cudaPanel.getSelectedDevices());

      params.getOutputParams().setOutputFullImage(false);
      params.getOutputParams().setDisplayStitching(false);
      params.getAdvancedParams().setNumCPUThreads(8);

      StitchingExecutor.StitchingType t = StitchingExecutor.StitchingType.CUDA;
      if (r.getAbsolutePath().contains("Keana_Scott_"))
        t = StitchingExecutor.StitchingType.FFTW;


      StitchingExecutor executor;

      // Run the MLE stitching version
      System.out.println("Stitching Type: " + t);
      File metaDataPath = new File(r, "mleTest");

      params.getOutputParams().setOutputPath(metaDataPath.getAbsolutePath());
      params.getAdvancedParams().setProgramType(t);

      executor = new StitchingExecutor(params);
      try {
        executor.runStitching(false, false, false);
      } catch (StitchingException e) {
        Log.msg(Log.LogType.MANDATORY, e.getMessage());
      }

    }

    System.exit(1);
  }

}
