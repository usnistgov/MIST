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

package gov.nist.isg.mist.stitchingvalidation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.lib.exceptions.StitchingException;
import gov.nist.isg.mist.lib.executor.StitchingExecutor;
import gov.nist.isg.mist.lib.executor.StitchingExecutor.StitchingType;
import gov.nist.isg.mist.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;

public class ParamsValidation {

  static {
    LibraryUtils.initalize();
  }

  private static final String STITCHING_PARAMS_FILE = "stitching-params.txt";


  private static String validationRootFolder = "C:\\majurski\\image-data\\Image_Stitching_Validation_Datasets";
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

    File[] roots = rootFolder.listFiles();

    //    CUDAPanel cudaPanel = new CUDAPanel();
    //    JFrame frame = new JFrame("Select CUDA Devices");
    //    JOptionPane.showMessageDialog(frame, cudaPanel);

    Log.setLogLevel(LogType.NONE);

    StitchingAppParams params;

    long startTime = System.currentTimeMillis();

    for (File r : roots) {

      System.out.println("Running: " + r.getAbsolutePath());
      if (!r.isDirectory())
        continue;

      params = new StitchingAppParams();

      File paramFile = new File(r, STITCHING_PARAMS_FILE);

      params.loadParams(paramFile);

      params.getInputParams().setImageDir(r.getAbsolutePath());
      params.getAdvancedParams().setNumCPUThreads(Runtime.getRuntime().availableProcessors());
      params.getAdvancedParams().setPlanPath(fftwPlanPath);
      params.getAdvancedParams().setFftwLibraryPath(fftwLibraryPath);
      //      params.getAdvancedParams().setCudaDevices(cudaPanel.getSelectedDevices());
      params.getOutputParams().setOutputFullImage(false);

      StitchingType t = StitchingType.FFTW;
      System.out.println("Stitching Type: " + t);

      File metaDataPath = new File(r, t.name().toLowerCase());
      params.getOutputParams().setOutputPath(metaDataPath.getAbsolutePath());
      params.getAdvancedParams().setProgramType(t);
      params.getOutputParams().setDisplayStitching(false);

      String basePath = params.getOutputParams().getOutputPath();
      int[] nbPeaksVals = new int[]{1, 2, 3, 4, 5, 6, 8, 10};

      for (int nP = 0; nP < nbPeaksVals.length; nP++) {
        int nbPeaks = nbPeaksVals[nP];

        params.getAdvancedParams().setNumFFTPeaks(nbPeaks);

        params.getOutputParams().setOutputPath(
            basePath + File.separator + "peaks" + nbPeaks + File.separator);

        File f = new File(params.getOutputParams().getOutputPath() + "img-statistics.txt");
        if (!f.exists()) {
          System.out.println("Nb Peaks: " + nbPeaks);
          StitchingExecutor executor = new StitchingExecutor(params);
          try {
            executor.runStitching(false, false, false);
          } catch (StitchingException e) {
            Log.msg(LogType.MANDATORY, e.getMessage());
          }
        }


      }
    }

    long endTime = System.currentTimeMillis();

    System.out.println("Total time: " + (endTime - startTime));

    File results = new File("validationDataSetResults.txt");

    FileWriter writer = null;
    try {
      writer = new FileWriter(results);
    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      if (writer != null)
        writer.write("Runtime for " + roots.length + " experiements: " + (endTime - startTime));

    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      if (writer != null)
        writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.exit(1);
  }
}
