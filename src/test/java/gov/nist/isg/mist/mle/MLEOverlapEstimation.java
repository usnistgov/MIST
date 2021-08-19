// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



package gov.nist.isg.mist.mle;

import java.io.File;

import gov.nist.isg.mist.gui.panels.advancedTab.parallelPanels.CUDAPanel;
import gov.nist.isg.mist.gui.panels.advancedTab.parallelPanels.FFTWPanel;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.lib.exceptions.StitchingException;
import gov.nist.isg.mist.lib.executor.StitchingExecutor;
import gov.nist.isg.mist.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.lib.log.Log;

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

    FFTWPanel fftwPanel = new FFTWPanel();

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

//      if (cudaPanel.isCudaAvailable())
//        params.getAdvancedParams().setCudaDevices(cudaPanel.getSelectedDevices());

      params.getOutputParams().setOutputFullImage(false);
      params.getOutputParams().setDisplayStitching(false);
      params.getAdvancedParams().setNumCPUThreads(8);

      StitchingExecutor.StitchingType t = StitchingExecutor.StitchingType.FFTW;
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
        params.getInputParams().setAssembleFromMetadata(false);
        executor.runStitching(false, false);
      } catch (StitchingException e) {
        Log.msg(Log.LogType.MANDATORY, e.getMessage());
      }

    }

    System.exit(1);
  }

}
