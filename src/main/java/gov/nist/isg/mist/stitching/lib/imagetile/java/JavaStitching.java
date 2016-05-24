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

package gov.nist.isg.mist.stitching.lib.imagetile.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gov.nist.isg.mist.stitching.lib.common.CorrelationTriple;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.imagetile.utilfns.UtilFnsStitching;
import gov.nist.isg.mist.stitching.lib.log.Debug;
import gov.nist.isg.mist.stitching.lib.log.Debug.DebugType;

/**
 * f * @author Tim Blattner
 *
 * @version 2013.08.7
 */
public class JavaStitching {

  /**
   * Computes the phase correlation image alignment between two images
   *
   * @param t1     image 1
   * @param t2     image 2
   * @param memory the tile worker memory
   * @return the best relative displacement along the x and y axis and the correlation between two
   * images
   */
  public static CorrelationTriple phaseCorrelationImageAlignment(JavaImageTile t1,
                                                                 JavaImageTile t2, TileWorkerMemory memory) {
    // If one of the two images does not exists, then a translation cannot exist
    if (!t1.fileExists() || !t2.fileExists())
      return new CorrelationTriple(-1.0, 0, 0);


    float[][] pcm = memory.getArrayMemory();
    pcm = peakCorrelationMatrix(t1, t2, pcm);

    List<CorrelationTriple> peaks =
        UtilFnsStitching.multiPeakCorrelationMatrixNoSort(pcm, Stitching.NUM_PEAKS, t1.getWidth(),
            t2.getHeight());
    List<CorrelationTriple> multi_ccfs = new ArrayList<CorrelationTriple>(Stitching.NUM_PEAKS);
    for (int i = 0; i < peaks.size(); i++) {
      if (t1.isSameRowAs(t2))
        multi_ccfs.add(Stitching.peakCrossCorrelationLR(t1, t2, peaks.get(i).getX(), peaks.get(i)
            .getY()));
      else if (t1.isSameColAs(t2))
        multi_ccfs.add(Stitching.peakCrossCorrelationUD(t1, t2, peaks.get(i).getX(), peaks.get(i)
            .getY()));
      Debug.msg(DebugType.INFO, multi_ccfs.get(i).toString());

    }

    return Collections.max(multi_ccfs);

  }


  /**
   * Computes the peak correlation matrix between two images
   *
   * @param t1  image 1
   * @param t2  image 2
   * @param ncc the normalized cross correlation matrix
   * @return the peak correlation matrix
   */
  public static float[][] peakCorrelationMatrix(JavaImageTile t1, JavaImageTile t2, float[][] ncc) {
    if (!t1.hasFft())
      t1.computeFft();

    if (!t2.hasFft())
      t2.computeFft();

    ncc = UtilFnsStitching.computePhaseCorrelationJava(t1.getFft(), t2.getFft(), ncc);

    ncc = JavaImageTile.fftPlan.applyInverse(ncc);

    return ncc;
  }


}
