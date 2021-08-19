// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.




package gov.nist.isg.mist.stitchingvalidation;

import java.io.File;

import gov.nist.isg.mist.gui.panels.advancedTab.parallelPanels.CUDAPanel;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.lib.exceptions.StitchingException;
import gov.nist.isg.mist.lib.executor.StitchingExecutor;
import gov.nist.isg.mist.lib.executor.StitchingExecutor.StitchingType;
import gov.nist.isg.mist.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;

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
//    params.getAdvancedParams().setProgramType(StitchingType.CUDA);
//    params.getOutputParams().setOutFilePrefix("cuda32-");
//    params.getAdvancedParams().setUseDoublePrecision(false);
//    elapsedTime = 0;
//    for (int i = 0; i < nbIter; i++) {
//      long startTime = System.currentTimeMillis();
//      try {
//        params.getInputParams().setAssembleFromMetadata(false);
//        (new StitchingExecutor(params)).runStitching(false, false);
//      } catch (StitchingException e) {
//        Log.msg(LogType.MANDATORY, e.getMessage());
//      }
//      elapsedTime = elapsedTime + (System.currentTimeMillis() - startTime);
//    }
//    System.out.println("CUDA 32bit Elapsed: " + (elapsedTime / nbIter) + " ms");


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
