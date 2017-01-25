// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



package gov.nist.isg.mist.mle;

import java.io.File;

import javax.swing.*;

import gov.nist.isg.mist.gui.panels.advancedTab.parallelPanels.CUDAPanel;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.lib.exceptions.StitchingException;
import gov.nist.isg.mist.lib.executor.StitchingExecutor;
import gov.nist.isg.mist.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.lib.log.Log;

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
      params.getInputParams().setAssembleFromMetadata(false);
      (new StitchingExecutor(params)).runStitching(false, false);
    } catch (StitchingException e) {
      Log.msg(Log.LogType.MANDATORY, e.getMessage());
    }


    System.exit(1);
  }


}
