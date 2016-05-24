
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

import javax.swing.*;

import gov.nist.isg.mist.stitching.gui.panels.advancedTab.parallelPanels.CUDAPanel;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.lib.exceptions.StitchingException;
import gov.nist.isg.mist.stitching.lib.executor.StitchingExecutor;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.stitching.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.stitching.lib.log.Log;

/**
 * Test class for parallel MLE overlap estimation.
 *
 * @author Michael Majurski
 */
public class ParallelMleTest {

  static {
    LibraryUtils.initalize();
  }

  private static final String STITCHING_PARAMS_FILE = "stitching-params.txt";

  private static String validationRootFolder = "C:\\majurski\\image-data\\1h_Wet_10Perc";
  //  private static String validationRootFolder = "C:\\majurski\\image-data\\70Perc_Overlap_24h_Dry_Dataset";
  //  private static String validationRootFolder = "C:\\majurski\\image-data\\John_Elliot\\uncompressed_synthetic_grid";
  private static String fftwPlanPath = "C:\\Fiji.app\\lib\\fftw\\fftPlans";
  private static String fftwLibraryPath = "C:\\Fiji.app\\lib\\fftw";

  private static boolean IS_CUDA_EXCEPTIONS_ENABLED = false;
  private static int NUMBER_THREADS = 24;
  private static int NUMBER_ITERATIONS = 1;

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
    JFrame frame = new JFrame("Select CUDA Devices");
    JOptionPane.showMessageDialog(frame, cudaPanel);
    IS_CUDA_EXCEPTIONS_ENABLED = cudaPanel.isCudaExceptionsEnabled();
    NUMBER_THREADS = cudaPanel.getNumCPUThreads();

    Log.setLogLevel(Log.LogType.NONE);
    StitchingAppParams params;


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
    params.getAdvancedParams().setNumCPUThreads(NUMBER_THREADS);
    params.getAdvancedParams().setUseDoublePrecision(true);


    // Load FFTW libraries
    FftwImageTile.initLibrary(fftwLibraryPath, "", "libfftw3");


    // Setup output folder
    File metaDataPath = new File(rootFolder, "mleParallel");
    if (!metaDataPath.exists())
      metaDataPath.mkdir();
    params.getOutputParams().setOutputPath(metaDataPath.getAbsolutePath());


    // Stitch subgrid for now
//    params.getInputParams().setExtentHeight(5);
//    params.getInputParams().setExtentWidth(5);


    // Run CUDA 32bit
    params.getAdvancedParams().setProgramType(StitchingExecutor.StitchingType.CUDA);
    params.getOutputParams().setOutFilePrefix("cuda32-");
    params.getAdvancedParams().setUseDoublePrecision(false);

    try {
      (new StitchingExecutor(params)).runStitching(false, false, false);
    } catch (StitchingException e) {
      Log.msg(Log.LogType.MANDATORY, e.getMessage());
    }


    System.exit(1);
  }


}
