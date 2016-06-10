// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



package gov.nist.isg.mist.stitchingruntime;

import java.io.File;

import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.lib.exceptions.StitchingException;
import gov.nist.isg.mist.lib.executor.StitchingExecutor;
import gov.nist.isg.mist.lib.imagetile.fftw.FftwPlanType;
import gov.nist.isg.mist.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.lib.log.Log;

/**
 * Created by mmajursk on 5/13/2016.
 */
public class LowMemoryStitchingEvaluation {

  static {
    LibraryUtils.initalize();
  }

  private static final String STITCHING_PARAMS_FILE = "stitching-params.txt";

  private static String datasetFolder =
      "E:\\image-data\\MIST_Performance_Evaluation\\low-memory\\imgs\\";

  private static String fftwPlanPath = "C:\\Fiji.app\\lib\\fftw\\fftPlans";
  private static String fftwLibraryPath = "C:\\Fiji.app\\lib\\fftw";


  public static void main(String[] args) {


    if (args.length >= 0) {
      switch (args.length) {
        case 1:
          datasetFolder = args[0];
          break;
        case 2:
          datasetFolder = args[0];
          fftwPlanPath = args[1];
          break;
        case 3:
          datasetFolder = args[0];
          fftwPlanPath = args[1];
          fftwLibraryPath = args[2];
          break;
        default:
          System.out.println("Usage: ImageStitchingRunTime <datasetFolder> <fftwPlanPath> " +
              "<fftwLibraryPath>");
          break;
      }
    } else {
      System.out.println("Usage: ImageStitchingRunTime <datasetFolder> <fftwPlanPath> <fftwLibraryPath>");
    }

    System.out.println("datasetFolder: \"" + datasetFolder + "\"");
    System.out.println("fftwPlanPath: \"" + fftwPlanPath + "\"");
    System.out.println("fftwLibPath: \"" + fftwLibraryPath + "\"");

    runFolder();


    System.exit(1);
  }

  private static void runFolder() {
    // get all folders in root folder
    File folder = new File(datasetFolder);
    if (!folder.exists() && !folder.isDirectory()) {
      System.out.println("Error: Unable to find root folder: " + datasetFolder);
      System.exit(1);
    }

    Log.setLogLevel(Log.LogType.MANDATORY);
    StitchingAppParams params;

    if (!folder.isDirectory())
      return;

    params = new StitchingAppParams();
    File paramFile = new File(folder, STITCHING_PARAMS_FILE);

    if (!paramFile.exists())
      return;
    params.loadParams(paramFile);

//    List<CudaDeviceParam> cudaDevices = cudaPanel.getSelectedDevices();

    params.getInputParams().setImageDir(folder.getAbsolutePath());
    params.getAdvancedParams().setNumCPUThreads(Runtime.getRuntime().availableProcessors());
    params.getAdvancedParams().setPlanPath(fftwPlanPath);
    params.getAdvancedParams().setFftwLibraryPath(fftwLibraryPath);
    params.getAdvancedParams().setFftwPlanType(FftwPlanType.MEASURE);
//    params.getAdvancedParams().setCudaDevices(cudaPanel.getSelectedDevices());
    params.getOutputParams().setOutputPath(folder.getAbsolutePath() + File.separator + "results");

    params.getOutputParams().setOutputMeta(true);
    params.getOutputParams().setOutputFullImage(false);
    params.getOutputParams().setDisplayStitching(false);
//      params.getAdvancedParams().setNumCPUThreads(8);

    params.getAdvancedParams().setProgramType(StitchingExecutor.StitchingType.AUTO);
    params.getAdvancedParams().setNumFFTPeaks(2);

    StitchingExecutor executor = new StitchingExecutor(params);

    try {
      executor.runStitching(false, false, false);
    } catch (StitchingException e) {
      Log.msg(Log.LogType.MANDATORY, e.getMessage());
    }

//    StitchingStatistics stats = executor.getStitchingStatistics();

  }

}

