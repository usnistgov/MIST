package gov.nist.isg.mist.mle;

import java.io.File;
import java.util.List;

import javax.swing.*;

import gov.nist.isg.mist.stitching.gui.panels.advancedTab.parallelPanels.CUDAPanel;
import gov.nist.isg.mist.stitching.gui.params.InputParameters;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.gui.params.objects.CudaDeviceParam;
import gov.nist.isg.mist.stitching.lib.exceptions.StitchingException;
import gov.nist.isg.mist.stitching.lib.executor.StitchingExecutor;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaUtils;
import gov.nist.isg.mist.stitching.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.parallel.gpu.GPUStitchingThreadExecutor;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.SequentialTileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader;
import jcuda.driver.CUcontext;
import jcuda.driver.CUdeviceptr;

/**
 * Created by mmajursk on 11/13/2015.
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
    if(!metaDataPath.exists())
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
