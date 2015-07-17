package gov.nist.isg.mist.mle;

import java.io.File;

import javax.swing.*;

import gov.nist.isg.mist.stitching.gui.executor.StitchingExecutor;
import gov.nist.isg.mist.stitching.gui.panels.advancedTab.parallelPanels.CUDAPanel;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.lib.exceptions.StitchingException;
import gov.nist.isg.mist.stitching.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.optimization.OptimizationUtils;

/**
 * Created by mmajurski on 7/9/15.
 */
public class MLEOverlapEstimation {


  static {
    LibraryUtils.initalize();
  }

  private static final String STITCHING_PARAMS_FILE = "stitching-params.txt";


//  private static String stitchingImageDataFolder = "/Users/mmajurski/Workspace/image-data/Image_Stitching_Validation_Datasets/SSEA4_5x_w01_c3";
//  private static String fftwPlanPath = "/Applications/Fiji.app/lib/fftw/fftPlans";
//  private static String fftwLibraryPath = "/usr/local/lib/libfftw3.dylib";

  private static String validationRootFolder = "E:\\image-data\\Image_Stitching_Validation_Datasets\\";
  private static String fftwPlanPath = "C:\\Fiji.app\\lib\\fftw\\fftPlans\\";
  private static String fftwLibraryPath = "C:\\Fiji.app\\lib\\fftw\\";



  public static void main(String [] args) {


    File rootFolder = new File(validationRootFolder);
    if (!rootFolder.exists() && !rootFolder.isDirectory())
    {
      System.out.println("Error: Unable to find root folder: " + validationRootFolder);
      System.exit(1);
    }

    File[] roots = rootFolder.listFiles();

    CUDAPanel cudaPanel = new CUDAPanel();
//
//    JFrame frame = new JFrame("Select CUDA Devices");
//    JOptionPane.showMessageDialog(frame, cudaPanel);


    Log.setLogLevel(Log.LogType.MANDATORY);
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
      params.getAdvancedParams().setNumCPUThreads(8);


      StitchingExecutor.StitchingType t = StitchingExecutor.StitchingType.CUDA;
      System.out.println("Stitching Type: " + t);
      File metaDataPath = new File(r, "mleTest");
      params.getOutputParams().setMetadataPath(metaDataPath.getAbsolutePath());
      params.getOutputParams().setOutputPath(metaDataPath.getAbsolutePath());
      params.getAdvancedParams().setProgramType(t);

      params.getAdvancedParams().setOverlapComputationType(OptimizationUtils.OverlapType.MLE);

      StitchingExecutor executor = new StitchingExecutor(params);

      try {
        executor.runStitching(false, false, false);
      } catch (StitchingException e)
      {
        Log.msg(Log.LogType.MANDATORY, e.getMessage());
      }
    }

    System.exit(1);
  }

}
