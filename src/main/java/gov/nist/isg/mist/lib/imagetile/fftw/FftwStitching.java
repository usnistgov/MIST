// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: May 10, 2013 2:59:05 PM EST
//
// Time-stamp: <May 10, 2013 2:59:05 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.imagetile.fftw;

import org.bridj.Pointer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gov.nist.isg.mist.lib.common.CorrelationTriple;
import gov.nist.isg.mist.lib.imagetile.Stitching;
import gov.nist.isg.mist.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.lib.imagetile.utilfns.UtilFnsStitching;
import gov.nist.isg.mist.lib.log.Debug;
import gov.nist.isg.mist.lib.log.Debug.DebugType;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;

/**
 * Utility functions for doing image stitching using FFTWImageTiles.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class FftwStitching {

  /**
   * Computes the phase correlatoin image alignment between two images
   *
   * @param t1     image 1
   * @param t2     image 2
   * @param memory the tile worker memory
   * @return the best relative displacement along the x and y axis and the correlation between two
   * images
   */
  public static CorrelationTriple phaseCorrelationImageAlignment(FftwImageTile t1,
                                                                 FftwImageTile t2, TileWorkerMemory memory) {

    // If one of the two images does not exists, then a translation cannot exist
    if (!t1.fileExists() || !t2.fileExists())
      return new CorrelationTriple(-1.0, 0, 0);

    Pointer<Double> pcm = peakCorrelationMatrix(t1, t2, memory);

    List<CorrelationTriple> peaks;

    peaks = UtilFnsStitching.multiPeakCorrelationMatrix(pcm, Stitching.NUM_PEAKS, t1.getWidth(),
        t1.getHeight(), memory.getPeaks());

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
   * Computes the peak correlatoin matrix between two images
   *
   * @param t1     image 1
   * @param t2     image 2
   * @param memory the tile worker memory
   * @return the peak correlation matrix
   */
  public static Pointer<Double> peakCorrelationMatrix(FftwImageTile t1, FftwImageTile t2,
                                                      TileWorkerMemory memory) {
    if (!t1.hasFft())
      t1.computeFft();

    if (!t2.hasFft())
      t2.computeFft();

    Pointer<Double> pcmIn = memory.getPCMInMemory();
    Pointer<Double> pcmOut = memory.getPCMPMemory();
    UtilFnsStitching
        .computePhaseCorrelation(t1.getFft(), t2.getFft(), pcmIn, FftwImageTile.fftSize);

    FFTW3Library.fftw_execute_dft_c2r(FftwImageTile.plan_bwd, pcmIn, pcmOut);

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
