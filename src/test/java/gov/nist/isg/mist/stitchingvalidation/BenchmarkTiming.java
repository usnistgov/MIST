package gov.nist.isg.mist.stitchingvalidation;

import org.bridj.Pointer;

import java.io.File;
import java.util.List;

import javax.swing.*;

import gov.nist.isg.mist.stitching.gui.panels.advancedTab.parallelPanels.CUDAPanel;
import gov.nist.isg.mist.stitching.gui.params.AdvancedParameters;
import gov.nist.isg.mist.stitching.gui.params.InputParameters;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.gui.params.objects.CudaDeviceParam;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FFTW3Library;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaUtils;
import gov.nist.isg.mist.stitching.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.parallel.cpu.CPUStitchingThreadExecutor;
import gov.nist.isg.mist.stitching.lib.parallel.gpu.GPUStitchingThreadExecutor;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.SequentialTileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverser;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverserFactory;
import jcuda.driver.CUcontext;
import jcuda.driver.CUdeviceptr;

/**
 * Created by mmajursk on 11/12/2015.
 */
public class BenchmarkTiming {

  static {
    LibraryUtils.initalize();
  }

  private static final String STITCHING_PARAMS_FILE = "stitching-params.txt";


  private static String validationRootFolder = "C:\\majurski\\image-data\\1h_Wet_10Perc";
//    private static String validationRootFolder = "C:\\majurski\\image-data\\70Perc_Overlap_24h_Dry_Dataset";
  //  private static String validationRootFolder = "C:\\majurski\\image-data\\John_Elliot\\uncompressed_synthetic_grid";
  private static String fftwPlanPath = "C:\\Fiji.app\\lib\\fftw\\fftPlans";
  private static String fftwLibraryPath = "C:\\Fiji.app\\lib\\fftw";

  private static double NUMBER_ITERATIONS = 10;
  private static boolean IS_CUDA_EXCEPTIONS_ENABLED = true;
  private static int NUMBER_THREADS = 24;


  public static void main(String[] args) {
    if (args.length > 0) {
      validationRootFolder = args[0];
      if(args.length > 1) {
        fftwLibraryPath = args[1];
        if(args.length > 2) {
          fftwPlanPath = args[2];
        }
      }
    }


    // get all folders in root folder
    File rootFolder = new File(validationRootFolder);
    if (!rootFolder.exists() && !rootFolder.isDirectory()) {
      System.out.println("Error: Unable to find root folder: " + validationRootFolder);
      System.exit(1);
    }

    System.out.println("root folder setup");

    CUDAPanel cudaPanel = new CUDAPanel();
    System.out.println("Cuda Panel Created");
    JFrame frame = new JFrame("Select CUDA Devices");
    JOptionPane.showMessageDialog(frame, cudaPanel);
    System.out.println("Cuda panel JFrame and JOptionPanel setup");
    IS_CUDA_EXCEPTIONS_ENABLED = cudaPanel.isCudaExceptionsEnabled();
    NUMBER_THREADS = cudaPanel.getNumCPUThreads();
    System.out.println("CUDA options extracted");

    Log.setLogLevel(Log.LogType.NONE);
    StitchingAppParams params;


    params = new StitchingAppParams();
    params.getAdvancedParams().setEnableCudaExceptions(true);


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
    File metaDataPath = new File(rootFolder, "timeBenchmark");
    if(!metaDataPath.exists())
      metaDataPath.mkdir();
    params.getOutputParams().setOutputPath(metaDataPath.getAbsolutePath());



    // Stitch subgrid for now
    params.getInputParams().setExtentHeight(5);
    params.getInputParams().setExtentWidth(5);





    System.out.println("FFTW Sequential: " + getFftwSequentialTime(params) + " ms");
    System.out.println("CUDA Sequential: " + getCudaSequentialTime(params) + " ms");

    System.out.println("FFTW MultiThreaded: " + getFftwMultithreadedTime(params) + " ms");
    System.out.println("CUDA MultiThreaded: " + getCudaMultithreadedTime(params) + " ms");


    System.exit(1);
  }


  private static double getFftwSequentialTime(StitchingAppParams params) {
    double elapsedTime = -1;
    try {
      System.out.println("Starting Sequential FFTW");
      InputParameters ip = params.getInputParams();
      TileGridLoader loader =
          new SequentialTileGridLoader(ip.getGridWidth(), ip.getGridHeight(), 1, ip.getFilenamePattern(),
              ip.getOrigin(), ip.getNumbering());

      TileGrid<ImageTile<Pointer<Double>>> grid = new TileGrid<ImageTile<Pointer<Double>>>(ip.getStartRow(),
          ip.getStartCol(), ip.getExtentWidth(), ip.getExtentHeight(), loader, new File(ip.getImageDir()), FftwImageTile.class);

      ImageTile<Pointer<Double>> tile = grid.getSubGridTile(0, 0);

      tile.readTile();
      FftwImageTile.initPlans(tile.getWidth(), tile.getHeight(), FFTW3Library.FFTW_MEASURE, true, "test.dat");
      FftwImageTile.savePlan("test.dat");
      TileGridTraverser<ImageTile<Pointer<Double>>> gridTraverser =
          TileGridTraverserFactory.makeTraverser(TileGridTraverser.Traversals.DIAGONAL, grid);

      long startTime = System.currentTimeMillis();
      for(int i = 0; i < NUMBER_ITERATIONS; i++)
        Stitching.stitchGridFftw(gridTraverser, grid);
      elapsedTime = (System.currentTimeMillis()-startTime)/NUMBER_ITERATIONS;

      params.getOutputParams().setOutFilePrefix("fftw-sequential-");
      Stitching.outputRelativeDisplacements(grid, params.getOutputParams()
          .getRelPosNoOptFile(1, 1));
    } catch (Exception e) {
      System.out.println("Exception: " + e.getMessage());
    }
    return elapsedTime;
  }

  private static double getCudaSequentialTime(StitchingAppParams params) {
    double elapsedTime = -1;
    try {
      System.out.println("Starting Sequential CUDA");
      InputParameters ip = params.getInputParams();
      TileGridLoader loader =
          new SequentialTileGridLoader(ip.getGridWidth(), ip.getGridHeight(), 1, ip.getFilenamePattern(),
              ip.getOrigin(), ip.getNumbering());

      TileGrid<ImageTile<CUdeviceptr>> grid =
          new TileGrid<ImageTile<CUdeviceptr>>(ip.getStartRow(),
              ip.getStartCol(), ip.getExtentWidth(), ip.getExtentHeight(), loader, new File(ip.getImageDir()), CudaImageTile.class);

      CudaImageTile tile = (CudaImageTile) grid.getSubGridTile(0, 0);
      tile.readTile();

      List<CudaDeviceParam> devs = params.getAdvancedParams().getCudaDevices();
      int[] devIDs = new int[devs.size()];
      for(int i = 0; i < devs.size(); i++)
        devIDs[i] = devs.get(i).getId();

      CUcontext[] contexts = CudaUtils.initJCUDA(1, devIDs, tile, IS_CUDA_EXCEPTIONS_ENABLED);
      TileGridTraverser<ImageTile<CUdeviceptr>> gridTraverser =
          TileGridTraverserFactory.makeTraverser(TileGridTraverser.Traversals.DIAGONAL, grid);

      long startTime = System.currentTimeMillis();
      for(int i = 0; i < NUMBER_ITERATIONS; i++)
        Stitching.stitchGridCuda(gridTraverser, grid, contexts[0]);
      elapsedTime = (System.currentTimeMillis()-startTime)/NUMBER_ITERATIONS;

      params.getOutputParams().setOutFilePrefix("cuda-sequential-");
      Stitching.outputRelativeDisplacements(grid, params.getOutputParams()
          .getRelPosNoOptFile(1, 1));
    } catch (Exception e) {
      System.out.println("Exception: " + e.getMessage());
    }
    return elapsedTime;
  }

  private static double getFftwMultithreadedTime(StitchingAppParams params) {
    double elapsedTime = -1;
    try {
      System.out.println("Starting Multithreaded FFTW");
      InputParameters ip = params.getInputParams();
      TileGridLoader loader =
          new SequentialTileGridLoader(ip.getGridWidth(), ip.getGridHeight(), 1, ip.getFilenamePattern(),
              ip.getOrigin(), ip.getNumbering());

      TileGrid<ImageTile<Pointer<Double>>> grid = new TileGrid<ImageTile<Pointer<Double>>>(ip.getStartRow(),
          ip.getStartCol(), ip.getExtentWidth(), ip.getExtentHeight(), loader, new File(ip.getImageDir()), FftwImageTile.class);

      ImageTile<Pointer<Double>> tile = grid.getSubGridTile(0, 0);

      tile.readTile();
      FftwImageTile.initPlans(tile.getWidth(), tile.getHeight(), FFTW3Library.FFTW_MEASURE, true, "test.dat");
      FftwImageTile.savePlan("test.dat");

      long startTime = System.currentTimeMillis();
      for(int i = 0; i < NUMBER_ITERATIONS; i++)
        (new CPUStitchingThreadExecutor<Pointer<Double>>(1, params.getAdvancedParams().getNumCPUThreads(), tile, grid)).execute();
      elapsedTime = (System.currentTimeMillis()-startTime)/NUMBER_ITERATIONS;

      params.getOutputParams().setOutFilePrefix("fftw-multithreaded-");
      Stitching.outputRelativeDisplacements(grid, params.getOutputParams()
          .getRelPosNoOptFile(1, 1));
    } catch (Exception e) {
      System.out.println("Exception: " + e.getMessage());
    }
    return elapsedTime;
  }

  private static double getCudaMultithreadedTime(StitchingAppParams params) {
    double elapsedTime = -1;
    try {
      System.out.println("Starting Multithreaded CUDA");
      InputParameters ip = params.getInputParams();
      TileGridLoader loader =
          new SequentialTileGridLoader(ip.getGridWidth(), ip.getGridHeight(), 1, ip.getFilenamePattern(),
              ip.getOrigin(), ip.getNumbering());

      TileGrid<ImageTile<CUdeviceptr>> grid =
          new TileGrid<ImageTile<CUdeviceptr>>(ip.getStartRow(),
              ip.getStartCol(), ip.getExtentWidth(), ip.getExtentHeight(), loader, new File(ip.getImageDir()), CudaImageTile.class);

      CudaImageTile tile = (CudaImageTile) grid.getSubGridTile(0, 0);
      tile.readTile();

      List<CudaDeviceParam> devs = params.getAdvancedParams().getCudaDevices();
      int[] devIDs = new int[devs.size()];
      for(int i = 0; i < devs.size(); i++)
        devIDs[i] = devs.get(i).getId();

      CUcontext[] contexts = CudaUtils.initJCUDA(1, devIDs, tile, IS_CUDA_EXCEPTIONS_ENABLED);

      long startTime = System.currentTimeMillis();
      for(int i = 0; i < NUMBER_ITERATIONS; i++)
        (new GPUStitchingThreadExecutor<CUdeviceptr>(contexts.length, params.getAdvancedParams().getNumCPUThreads(), tile, grid, contexts, devIDs)).execute();
      elapsedTime = (System.currentTimeMillis()-startTime)/NUMBER_ITERATIONS;

      params.getOutputParams().setOutFilePrefix("cuda-multithreaded-");
      Stitching.outputRelativeDisplacements(grid, params.getOutputParams()
          .getRelPosNoOptFile(1, 1));
    } catch (Exception e) {
      System.out.println("Exception: " + e.getMessage());
    }
    return elapsedTime;
  }

}
