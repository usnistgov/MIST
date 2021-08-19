// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



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

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
//import java.lang.invoke.MethodHandles;
//import java.lang.invoke.VarHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import javax.swing.JOptionPane;

import gov.nist.isg.mist.lib.log.Log;
import jcuda.LibUtils;
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
  public static LibUtils.ArchType arch;

  /**
   * Gets the major version number for Java
   * @return the major version number
   */
  private static int getJavaMajorVersionNumber() {
    String version = System.getProperty("java.version");
    if(version.startsWith("1.")) {
      version = version.substring(2, 3);
    } else {
      int dot = version.indexOf(".");
      if(dot != -1) { version = version.substring(0, dot); }
    } return Integer.parseInt(version);
  }

  /**
   * Initializes the libraries
   */
  public static void initalize() {
    try {
//      addDir("." + File.separator + "lib" + File.separator + "jcuda");
      addDir("." + File.separator + "lib");
    } catch (IOException e) {

      System.err.println("Failed to add required library paths for FFTW. If you want to use FFTW make sure you set the correct library path in the advanced options. " +
              "Possibly adding FFTW library to your system paths. " + e.getMessage());
      Log.msg(Log.LogType.INFO, "Failed to add required library paths for FFTW. If you want to use FFTW make sure you set the correct library path in the advanced options. " +
              "Possibly adding FFTW library to your system paths. " + e.getMessage());
    }

      os = LibUtils.calculateOS();
      arch = LibUtils.calculateArch();

      if (arch != LibUtils.ArchType.X86_64) {
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

  }

  private static void addDir(String s) throws IOException {
    // This enables the java.library.path to be modified at runtime
    // From a Sun engineer at
    // http://forums.sun.com/thread.jspa?threadID=707176
    //
      try {
        Field field = ClassLoader.class.getDeclaredField("usr_paths");

        field.setAccessible(true);
        String[] paths = (String[]) field.get(null);
        for (String path : paths) {
          if (s.equals(path)) {
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
