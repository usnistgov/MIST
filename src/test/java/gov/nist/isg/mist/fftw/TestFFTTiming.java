package gov.nist.isg.mist.fftw;



import org.bridj.Pointer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import edu.mines.jtk.mosaic.Tile;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FFTW3Library;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwImageTile;

import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwPlanType;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.FftwTileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.memorypool.DynamicMemoryPool;
import gov.nist.isg.mist.stitching.lib.memorypool.PointerAllocator;
import gov.nist.isg.mist.stitching.lib32.imagetile.fftw.FFTW3Library32;
import gov.nist.isg.mist.stitching.lib32.imagetile.fftw.FftwImageTile32;
import gov.nist.isg.mist.stitching.lib32.imagetile.memory.FftwTileWorkerMemory32;
import gov.nist.isg.mist.stitching.lib32.memorypool.PointerAllocator32;

/**
 * Created by mmajursk on 10/28/2015.
 */
public class TestFFTTiming {

  public static FftwPlanType planType = FftwPlanType.MEASURE;


  public static void run(int nbItr) throws IOException {
    File file = new File("C:\\majurski\\image-data\\1h_Wet_10Perc\\KB_2012_04_13_1hWet_10Perc_IR_00001.tif");
    String defaultPlanPath = "C:\\majurski\\NISTGithub\\MIST\\lib\\fftw\\fftPlans\\";

    Log.setLogLevel(Log.LogType.NONE);


    FftwImageTile32.initLibrary("C:\\majurski\\NISTGithub\\MIST\\lib\\fftw", "", "libfftw3f");
    FftwImageTile32 tile32 = new FftwImageTile32(file);
    String defaultPlanNamef = tile32.getWidth() + "x" + tile32.getHeight() + planType.toString() + "Plan_f.dat";
    FftwImageTile32.initPlans(tile32.getWidth(), tile32.getHeight(), planType.getVal(), true, defaultPlanPath + defaultPlanNamef);
    FftwImageTile32.savePlan(defaultPlanPath + defaultPlanNamef);

    long startTime32 = System.currentTimeMillis();
    for(int i = 0; i < nbItr; i++) {
      tile32.computeFft();
      tile32.releaseFftMemory();
    }
    System.out.println("32bit took: " + ((System.currentTimeMillis()-startTime32)/nbItr) + " ms");

    tile32.releaseFftMemory();
    FftwImageTile32.destroyPlans();



    FftwImageTile.initLibrary("C:\\majurski\\NISTGithub\\MIST\\lib\\fftw", "", "libfftw3");
    FftwImageTile tile = new FftwImageTile(file);
    String defaultPlanName = tile.getWidth() + "x" + tile.getHeight() + planType.toString() + "Plan.dat";
    FftwImageTile.initPlans(tile.getWidth(), tile.getHeight(), planType.getVal(), true, defaultPlanPath + defaultPlanName);
    FftwImageTile.savePlan(defaultPlanPath + defaultPlanName);

    long startTime64 = System.currentTimeMillis();
    for(int i = 0; i < nbItr; i++) {
      tile.computeFft();
      tile.releaseFftMemory();
    }
    System.out.println("64bit took: " + ((System.currentTimeMillis()-startTime64)/nbItr) + " ms");


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
