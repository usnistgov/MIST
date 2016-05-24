// Disclaimer: IMPORTANT: This software was developed at the National
// Institute of Standards and Technology by employees of the Federal
// Government in the course of their official duties. Pursuant to
// title 17 Section 105 of the United States Code this software is not
// subject to copyright protection and is in the public domain. This
// is an experimental system. NIST assumes no responsibility
// whatsoever for its use by other parties, and makes no guarantees,
// expressed or implied, about its quality, reliability, or any other
// characteristic. We would appreciate acknowledgement if the software
// is used. This software can be redistributed and/or modified freely
// provided that any derivative works bear some notice that they are
// derived from it, and any modified versions bear some notice that
// they have been modified.


// ================================================================
//
// Author: tjb3
// Date: May 10, 2013 2:59:05 PM EST
//
// Time-stamp: <May 10, 2013 2:59:05 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib32.imagetile.fftw;

import org.bridj.Pointer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gov.nist.isg.mist.stitching.lib.common.CorrelationTriple;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.log.Debug;
import gov.nist.isg.mist.stitching.lib.log.Debug.DebugType;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib32.imagetile.utilfns.UtilFnsStitching32;

/**
 * Utility functions for doing image stitching using FFTWImageTiles32.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class FftwStitching32 {

  /**
   * Computes the phase correlation image alignment between two images
   *
   * @param t1     image 1
   * @param t2     image 2
   * @param memory the tile worker memory
   * @return the best relative displacement along the x and y axis and the correlation between two
   * images
   */
  public static CorrelationTriple phaseCorrelationImageAlignment(FftwImageTile32 t1,
                                                                 FftwImageTile32 t2, TileWorkerMemory memory) {

    // If one of the two images does not exists, then a translation cannot exist
    if (!t1.fileExists() || !t2.fileExists())
      return new CorrelationTriple(-1.0, 0, 0);

    Pointer<Float> pcm = peakCorrelationMatrix(t1, t2, memory);

    List<CorrelationTriple> peaks;

    peaks = UtilFnsStitching32.multiPeakCorrelationMatrix(pcm, Stitching.NUM_PEAKS,
        t1.getWidth(), t1.getHeight(), memory.getPeaks());

    List<CorrelationTriple> multi_ccfs = new ArrayList<CorrelationTriple>();
    for (int i = 0; i < peaks.size(); i++) {
      CorrelationTriple peak = peaks.get(i);

      if (t1.isSameRowAs(t2))
        multi_ccfs.add(Stitching.peakCrossCorrelationLR(t1, t2, peak.getX(), peak.getY()));
      else if (t1.isSameColAs(t2))
        multi_ccfs.add(Stitching.peakCrossCorrelationUD(t1, t2, peak.getX(), peak.getY()));

      Debug.msg(DebugType.INFO, multi_ccfs.get(i).toString());
    }

    return Collections.max(multi_ccfs);
  }

  /**
   * Computes the peak correlation matrix between two images
   *
   * @param t1     image 1
   * @param t2     image 2
   * @param memory the tile worker memory
   * @return the peak correlation matrix
   */
  public static Pointer<Float> peakCorrelationMatrix(FftwImageTile32 t1, FftwImageTile32 t2,
                                                     TileWorkerMemory memory) {
    if (!t1.hasFft())
      t1.computeFft();

    if (!t2.hasFft())
      t2.computeFft();

    Pointer<Float> pcmIn = memory.getPCMInMemory();
    Pointer<Float> pcmOut = memory.getPCMPMemory();
    UtilFnsStitching32
        .computePhaseCorrelation(t1.getFft(), t2.getFft(), pcmIn, FftwImageTile32.fftSize);

    FFTW3Library32.fftwf_execute_dft_c2r(FftwImageTile32.plan_bwd, pcmIn, pcmOut);

    return pcmOut;
  }

  /**
   * Outputs a pointer to a file
   *
   * @param filePath the path where the file is to be written
   * @param ptr      the pointer you are writing
   * @param width    the width of the pointer
   * @param height   the height of the pointer
   */
  public static void outputToFile(String filePath, Pointer<Double> ptr, int width, int height) {
    File file = new File(filePath);

    try {
      PrintWriter out = new PrintWriter(new FileWriter(file));
      for (int r = 0; r < height; r++) {
        for (int c = 0; c < width; c++) {
          double real = ptr.getDoubleAtIndex((r * width + c));

          if (c == width - 1) {
            out.print(real);
          } else {
            out.print(real + ",");
          }
        }
        out.println();
      }
      out.close();

    } catch (IOException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }

  }

}
