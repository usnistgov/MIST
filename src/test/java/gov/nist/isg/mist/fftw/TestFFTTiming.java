// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



package gov.nist.isg.mist.fftw;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import gov.nist.isg.mist.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.lib.imagetile.fftw.FftwPlanType;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib32.imagetile.fftw.FftwImageTile32;

/**
 * Test class
 *
 * @author Michael Majurski
 */
public class TestFFTTiming {

  public static FftwPlanType planType = FftwPlanType.MEASURE;


  public static void run(int nbItr) throws IOException {
    File file = new File("C:\\majurski\\image-data\\1h_Wet_10Perc\\KB_2012_04_13_1hWet_10Perc_IR_00001.tif");
    String defaultPlanPath = "C:\\majurski\\NISTGithub\\MISTMain\\lib\\fftw\\fftPlans\\";

    Log.setLogLevel(Log.LogType.NONE);


    FftwImageTile32.initLibrary("C:\\majurski\\NISTGithub\\MISTMain\\lib\\fftw", "", "libfftw3f");
    FftwImageTile32 tile32 = new FftwImageTile32(file);
    String defaultPlanNamef = tile32.getWidth() + "x" + tile32.getHeight() + planType.toString() + "Plan_f.dat";
    FftwImageTile32.initPlans(tile32.getWidth(), tile32.getHeight(), planType.getVal(), true, defaultPlanPath + defaultPlanNamef);
    FftwImageTile32.savePlan(defaultPlanPath + defaultPlanNamef);

    long startTime32 = System.currentTimeMillis();
    for (int i = 0; i < nbItr; i++) {
      tile32.computeFft();
      tile32.releaseFftMemory();
    }
    System.out.println("32bit took: " + ((System.currentTimeMillis() - startTime32) / nbItr) + " ms");

    tile32.releaseFftMemory();
    FftwImageTile32.destroyPlans();


    FftwImageTile.initLibrary("C:\\majurski\\NISTGithub\\MISTMain\\lib\\fftw", "", "libfftw3");
    FftwImageTile tile = new FftwImageTile(file);
    String defaultPlanName = tile.getWidth() + "x" + tile.getHeight() + planType.toString() + "Plan.dat";
    FftwImageTile.initPlans(tile.getWidth(), tile.getHeight(), planType.getVal(), true, defaultPlanPath + defaultPlanName);
    FftwImageTile.savePlan(defaultPlanPath + defaultPlanName);

    long startTime64 = System.currentTimeMillis();
    for (int i = 0; i < nbItr; i++) {
      tile.computeFft();
      tile.releaseFftMemory();
    }
    System.out.println("64bit took: " + ((System.currentTimeMillis() - startTime64) / nbItr) + " ms");


    tile.releaseFftMemory();
    FftwImageTile.destroyPlans();

  }


  /**
   * Executes the test case
   *
   * @param args not used
   */
  public static void main(String[] args) {
    try {

      int nbItr = 50;

      planType = FftwPlanType.MEASURE;
      System.out.println("FFTW Measure Plan");
      TestFFTTiming.run(nbItr);

      planType = FftwPlanType.PATIENT;
      System.out.println("FFTW Patient Plan");
      TestFFTTiming.run(nbItr);

      planType = FftwPlanType.EXHAUSTIVE;
      System.out.println("FFTW Exhaustive Plan");
      TestFFTTiming.run(nbItr);

    } catch (FileNotFoundException e) {
      System.out.println("error");
    } catch (IOException e) {
      System.out.println("error");
    }
  }
}
