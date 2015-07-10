package gov.nist.isg.mist.mle;

import java.io.File;

import gov.nist.isg.mist.stitching.gui.executor.StitchingExecutor;
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


  private static String stitchingImageDataFolder = "/Users/mmajurski/Workspace/image-data/Image_Stitching_Validation_Datasets/SSEA4_5x_w01_c3";
  private static String fftwPlanPath = "/Applications/Fiji.app/lib/fftw/fftPlans";
  private static String fftwLibraryPath = "/usr/local/lib/libfftw3.dylib";


  public static void main(String [] args) {


    File f = new File(stitchingImageDataFolder);

//    Log.setLogLevel(Log.LogType.NONE);
    Log.setLogLevel(Log.LogType.MANDATORY);

    StitchingAppParams params;


    System.out.println("Running: " + f.getAbsolutePath());
    params = new StitchingAppParams();

    File paramFile = new File(f, STITCHING_PARAMS_FILE);

    params.loadParams(paramFile);

    params.getInputParams().setImageDir(f.getAbsolutePath());
    params.getAdvancedParams().setNumCPUThreads(Runtime.getRuntime().availableProcessors());
    params.getAdvancedParams().setPlanPath(fftwPlanPath);
    params.getAdvancedParams().setFftwLibraryPath(fftwLibraryPath);

    params.getOutputParams().setOutputFullImage(true);
    params.getOutputParams().setDisplayStitching(false);
//      params.getAdvancedParams().setNumCPUThreads(8);

    StitchingExecutor.StitchingType t = StitchingExecutor.StitchingType.JAVA;
    System.out.println("Stitching Type: " + t);
    File metaDataPath = new File(f, "mleTest");
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



    System.exit(1);
  }

}
