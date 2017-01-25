// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.




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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import gov.nist.isg.mist.gui.StitchingStatistics;
import gov.nist.isg.mist.gui.panels.advancedTab.parallelPanels.CUDAPanel;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.gui.params.objects.CudaDeviceParam;
import gov.nist.isg.mist.lib.exceptions.StitchingException;
import gov.nist.isg.mist.lib.executor.StitchingExecutor;
import gov.nist.isg.mist.lib.executor.StitchingExecutor.StitchingType;
import gov.nist.isg.mist.lib.imagetile.fftw.FftwPlanType;
import gov.nist.isg.mist.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;

public class ImageStitchingRunTime {

  static {
    LibraryUtils.initalize();
  }

  private static final String STITCHING_PARAMS_FILE = "stitching-params.txt";


//  private static String validationRootFolder = "C:\\Users\\tjb3\\StitchingPaperDatasets";
//private static String fftwPlanPath = "C:\\Users\\tjb3\\Documents\\MISTMain-ISG\\MISTMain\\lib\\fftw\\fftPlans";
//  private static String fftwLibraryPath = "C:\\Users\\tjb3\\Documents\\MISTMain-ISG\\MISTMain\\lib\\fftw";

  private static String validationRootFolder = "E:\\image-data\\Stitching_Paper_Data\\datasets";
  //  private static String validationRootFolder = "C:\\majurski\\image-data\\Stitching_Paper_Data";
//  private static String validationRootFolder = "E:\\image-data\\ImageJ_Conference";
  private static String fftwPlanPath = "C:\\Fiji.app\\lib\\fftw\\fftPlans";
  private static String fftwLibraryPath = "C:\\Fiji.app\\lib\\fftw";

  private static int NUM_RUNS = 1;

  public static void main(String[] args) {


    boolean useMLE = false;

    if (args.length >= 0) {
      switch (args.length) {
        case 1:
          validationRootFolder = args[0];
          break;
        case 2:
          validationRootFolder = args[0];
          fftwPlanPath = args[1];
          break;
        case 3:
          validationRootFolder = args[0];
          fftwPlanPath = args[1];
          fftwLibraryPath = args[2];
          break;
        case 4:
          validationRootFolder = args[0];
          fftwPlanPath = args[1];
          fftwLibraryPath = args[2];
          String tmp = args[3];
          if (tmp.contains("-mle")) {
            useMLE = true;
            System.out.println("Using MLE and outlier filtering");
          }
          break;
        default:
          System.out.println("Usage: ImageStitchingRunTime <rootFolder> <fftwPlanPath> <fftwLibraryPath> <-mle>");
          break;
      }
    } else {
      System.out.println("Usage: ImageStitchingRunTime <rootFolder> <fftwPlanPath> <fftwLibraryPath> <-mle>");
    }


    System.out.println("rootDir: \"" + validationRootFolder + "\"");
    System.out.println("fftwPlanPath: \"" + fftwPlanPath + "\"");
    System.out.println("fftwLibPath: \"" + fftwLibraryPath + "\"");

    runFolder();


    System.exit(1);
  }

  private static void runFolder() {
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

    Log.setLogLevel(LogType.NONE);
//    Log.setLogLevel(LogType.MANDATORY);

    StitchingAppParams params;

    File runtimeResults;
    runtimeResults = new File(validationRootFolder + File.separator + "runtimes.txt");
    try {
      FileWriter writer = new FileWriter(runtimeResults);
      writer.write("testCase, totalTime" + "\n");

      for (File r : roots) {

        if (!r.isDirectory())
          continue;


        params = new StitchingAppParams();

        File paramFile = new File(r, STITCHING_PARAMS_FILE);

        if (!paramFile.exists())
          continue;

        System.out.println("Running: " + r.getAbsolutePath());

        params.loadParams(paramFile);

        List<CudaDeviceParam> cudaDevices = cudaPanel.getSelectedDevices();


        params.getInputParams().setImageDir(r.getAbsolutePath());
        params.getAdvancedParams().setNumCPUThreads(Runtime.getRuntime().availableProcessors());
        params.getAdvancedParams().setPlanPath(fftwPlanPath);
        params.getAdvancedParams().setFftwLibraryPath(fftwLibraryPath);
        params.getAdvancedParams().setFftwPlanType(FftwPlanType.MEASURE);
        params.getAdvancedParams().setCudaDevices(cudaPanel.getSelectedDevices());
        params.getOutputParams().setOutputMeta(false);
        params.getOutputParams().setOutputPath(
            r.getAbsolutePath() + File.separator + "RunTimeResults");
        // set the metadata path to the output path

        params.getOutputParams().setOutputFullImage(true);
        params.getOutputParams().setDisplayStitching(false);
//      params.getAdvancedParams().setNumCPUThreads(8);


        for (StitchingType t : StitchingType.values()) {
          String testCase = t.toString() + "-" + r.getName();

          if (t == StitchingType.AUTO || t == StitchingType.JAVA || t == StitchingType.FFTW)
            continue;

          if (t == StitchingType.CUDA) {
            if (!cudaPanel.isCudaAvailable())
              continue;
          }


          double totalRunTime = 0;
          for (int run = 0; run < NUM_RUNS; run++) {

            System.out.println("Run " + run + " Stitching Type: " + t + " " + testCase);

//        File metaDataPath = new File(r, t.name().toLowerCase());
//        File metaDataPath = new File(r, "seq");
//        params.getOutputParams().setMetadataPath(metaDataPath.getAbsolutePath());
            params.getAdvancedParams().setProgramType(t);
            params.getAdvancedParams().setNumFFTPeaks(2);

            StitchingExecutor executor = new StitchingExecutor(params);

            try {
              params.getInputParams().setAssembleFromMetadata(false);
              executor.runStitching(false, false);
            } catch (StitchingException e) {
              Log.msg(LogType.MANDATORY, e.getMessage());
            }

            StitchingStatistics stats = executor.getStitchingStatistics();
            totalRunTime += stats.getDuration(StitchingStatistics.RunTimers.TotalStitchingTime);

          }

          double avgRunTime = totalRunTime / ((double) NUM_RUNS);
          writer.write(testCase + ", " + avgRunTime + "\n");
          writer.flush();
        }

      }


      writer.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
