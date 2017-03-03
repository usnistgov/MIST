// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 2:29:21 PM EST
//
// Time-stamp: <Apr 18, 2014 2:29:21 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import gov.nist.isg.mist.gui.StitchingSwingWorker;
import gov.nist.isg.mist.gui.params.AdvancedParameters;
import gov.nist.isg.mist.gui.params.InputParameters;
import gov.nist.isg.mist.gui.params.LoggingParameters;
import gov.nist.isg.mist.gui.params.OutputParameters;
import gov.nist.isg.mist.lib.libraryloader.LibraryUtils;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import ij.IJ;
import ij.ImageJ;
import ij.Macro;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;

/**
 * Creates the main NIST image sitching application. Run as a standalone application or a Fiji
 * plugin. Also can be run as a macro.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class MISTMain implements PlugIn {

  /**
   * The macro recorder commmand
   */
  public static String recorderCommand;

  /**
   * Enumeration of execution types to run
   *
   * @author Tim Blattner
   * @version 1.0
   */
  public enum ExecutionType {
    /**
     * Executes load parameters
     */
    LoadParams,

    /**
     * Executes save parameters
     */
    SaveParams,

    /**
     * Executes stitching
     */
    RunStitching,

    /**
     * Executes stitching with macro
     */
    RunStitchingMacro,

    /**
     * Composes image from metadata
     */
    RunStitchingFromMeta,

    /**
     * Composes image from metadata with macro
     */
    RunStitchingFromMetaMacro,

    /**
     * Previews full image without overlap
     */
    PreviewNoOverlap;
  }

  static {
    LibraryUtils.initalize();
  }

  private static boolean macro;
  private static String macroOptions;
  private static boolean stitching;
  public static boolean runHeadless = false;


  /**
   * Starts the application. If a macro is running, then it will execute using the macro parameters.
   * Otherwise it will use the GUI.
   *
   * @param app the stitching gui
   */
  public static void runApp(StitchingGUIFrame app) {
    stitching = false;
    macroOptions = Macro.getOptions();
    macro = macroOptions != null;

    if (macro) {
      executeStitchingWithMacro();
    } else {
      if (app == null) {
        Log.msg(LogType.MANDATORY, "Error: app GUI was not initialized.");
        return;
      }

      app.display();
    }
  }


  /**
   * Starts the application in headless mode. Currently app must be executed using a Fiji macro in
   * order to launch in headless mode.
   */
  public static void runAppHeadless() {
    stitching = false;
    macroOptions = Macro.getOptions();
    macro = macroOptions != null;
    runHeadless = true;

    if (macro) {
      executeStitchingWithMacro();
    } else {
      Log.msg(LogType.MANDATORY, "When in headless mode, app must be launched as Macro");
    }
  }

  /**
   * Checks if stitching is being executed or not
   *
   * @return true if stitching is executing
   */
  public static boolean isStitching() {
    return stitching;
  }

  /**
   * Enables the flag to indicate that stitching is executing
   */
  public static void enableStitching() {
    stitching = true;
  }

  /**
   * Disables the flag to indicate that stitching is executing
   */
  public static void disableStitching() {
    stitching = false;
  }

  /**
   * Checks if the run is a macro or not
   *
   * @return true if the run is a macro
   */
  public static boolean isMacro() {
    return macro;
  }

  /**
   * Gets the macro options
   *
   * @return the macro options
   */
  public static String getMacroOptions() {
    return macroOptions;
  }

  private static void executeStitchingWithMacro() {
    //Log.msg(LogType.MANDATORY, "Executing stitching using macro");
    ExecutionType type = ExecutionType.RunStitchingMacro;
    StitchingSwingWorker executor = new StitchingSwingWorker(null, type);
    executor.execute();
    try {
      executor.get();
    } catch (InterruptedException e) {
      Log.msg(LogType.MANDATORY, "Macro stitching has been cancelled");
    } catch (ExecutionException e) {
      Log.msg(LogType.MANDATORY, "Macro stitching has been cancelled " + e.getMessage());
    } catch (CancellationException e) {
      Log.msg(LogType.MANDATORY, "Macro stitching has been cancelled " + e.getMessage());
    }
  }

  // ///////////////////////////////////////////////////////////////
  // ///////////////////// MAIN ENTRY POINT ////////////////////////
  // ///////////////////////////////////////////////////////////////

  /**
   * Main entry point for executing
   *
   * @param args not used
   */
  public static void main(String[] args) {


    // if this is being run from command line
    if(args.length > 0) {
      if(args[0].equals("-h") || args[0].equals("-help")) {
        Log.msg(LogType.MANDATORY, MISTMain.getCommandLineHelp());
        return;
      }

      Log.msg(LogType.VERBOSE,"MISTMain.main: parsing args");
      for(int i = 0; i < args.length; i++) {
        Log.msg(LogType.MANDATORY,args[i]);
      }

      MISTMain.macroOptions = "";
      for(int i = 0; i < args.length; i++) {
        MISTMain.macroOptions += args[i].trim() + " ";
      }
      MISTMain.macro = false;
      MISTMain.runHeadless = false;
      MISTMain.executeStitchingWithMacro();

    }else {
      // Launch ImageJ window
      ImageJ.main(args);
      if (GraphicsEnvironment.isHeadless()) {
        MISTMain.runAppHeadless();
      } else {
        StitchingGUIFrame gui = new StitchingGUIFrame();
        MISTMain.runApp(gui);
      }
    }


  }

  @Override
  public void run(String arg) {

    //Log.msg(LogType.MANDATORY, "Launching MIST");

    recorderCommand = Recorder.getCommand();
    Recorder.setCommand(null);

    if (GraphicsEnvironment.isHeadless()) {
      MISTMain.runAppHeadless();
    } else {
      StitchingGUIFrame gui = new StitchingGUIFrame();
      MISTMain.runApp(gui);
    }
  }


  private static String getCommandLineHelp() {
    String str = InputParameters.getParametersCommandLineHelp();
    str += "\r\n";
    str += OutputParameters.getParametersCommandLineHelp();
    str += "\r\n";
    str += LoggingParameters.getParametersCommandLineHelp();
    str += "\r\n";
    str += AdvancedParameters.getParametersCommandLineHelp();
    return str;
  }

}
