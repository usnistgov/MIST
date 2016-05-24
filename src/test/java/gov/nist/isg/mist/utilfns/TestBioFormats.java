
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

package gov.nist.isg.mist.utilfns;

import java.io.File;

import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.imagetile.java.JavaImageTile;

/**
 * Test class for BioFormats image reader.
 *
 * @author Michael Majurski
 */
public class TestBioFormats {

  public static void main(String args[]) {

//    File file = new File("C:\\majurski\\image-data\\BioFormats_Examples\\leica_stack\\leica_stack_Series014_z000_ch00.tif");
    File file = new File("C:\\majurski\\image-data\\BioFormats_Examples\\2chZT\\2chZT.lsm");

    Stitching.USE_BIOFORMATS = true;
    ImageTile tile = new JavaImageTile(file);
    tile.readTile();


  }
}
