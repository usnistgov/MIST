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
// Date: Apr 11, 2014 11:05:38 AM EST
//
// Time-stamp: <Apr 11, 2014 11:05:38 AM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.libraryloader;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import javax.swing.*;

import jcuda.LibUtils;
import jcuda.LibUtils.ARCHType;
import jcuda.LibUtils.OSType;

/**
 * Utility class for adding paths at runtime to the environment.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class LibraryUtils {

  public static final String JCUDA_VERSION = "6.5";
//  public static final String JCUDA_VERSION = "5.0";

  /**
   * The operating sytem
   */
  public static OSType os;

  /**
   * The architecture type
   */
  public static ARCHType arch;

  /**
   * Initializes the libraries
   */
  public static void initalize() {
    try {
      addDir("." + File.separator + "lib" + File.separator + "jcuda-" + JCUDA_VERSION);
      addDir("." + File.separator + "lib");
      os = LibUtils.calculateOS();
      arch = LibUtils.calculateArch();

      if (arch != ARCHType.X86_64) {
        if (!GraphicsEnvironment.isHeadless())
          JOptionPane.showMessageDialog(null, "Warning: 32-bit architecture detected.\n"
                  + "Unable to use FFTW or CUDA, " + "please upgrade to 64-bit if possible.\n"
                  + "Due to memory limits, saving large images may fail.", "32-bit Detected",
              JOptionPane.WARNING_MESSAGE);
      }

      switch (os) {
        case APPLE:
          break;
        case LINUX:
          break;
        case SUN:
          break;
        case UNKNOWN:
          break;
        case WINDOWS:
          break;
        default:
          break;
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void addDir(String s) throws IOException {
    try {
      // This enables the java.library.path to be modified at runtime
      // From a Sun engineer at
      // http://forums.sun.com/thread.jspa?threadID=707176
      //
      Field field = ClassLoader.class.getDeclaredField("usr_paths");
      field.setAccessible(true);
      String[] paths = (String[]) field.get(null);
      for (int i = 0; i < paths.length; i++) {
        if (s.equals(paths[i])) {
          return;
        }
      }
      String[] tmp = new String[paths.length + 1];
      System.arraycopy(paths, 0, tmp, 0, paths.length);
      tmp[paths.length] = s;
      field.set(null, tmp);
      System.setProperty("java.library.path", System.getProperty("java.library.path")
          + File.pathSeparator + s);
    } catch (IllegalAccessException e) {
      throw new IOException("Failed to get permissions to set library path");
    } catch (NoSuchFieldException e) {
      throw new IOException("Failed to get field handle to set library path");
    }
  }

}
