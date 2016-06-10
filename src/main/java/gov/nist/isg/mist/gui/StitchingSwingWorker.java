// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 1:34:00 PM EST
//
// Time-stamp: <Apr 18, 2014 1:34:00 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui;

import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.SwingWorker;

import gov.nist.isg.mist.MISTMain;
import gov.nist.isg.mist.MISTMain.ExecutionType;
import gov.nist.isg.mist.StitchingGUIFrame;
import gov.nist.isg.mist.gui.params.StitchingAppParams;
import gov.nist.isg.mist.lib.executor.StitchingExecutor;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;

/**
 * Creates a separate thread for managing stitching execution. This thread is separate from the main
 * thread to enable continued interaction with GUI elements
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class StitchingSwingWorker extends SwingWorker<Void, Void> {

  private StitchingExecutor executor;
  private Thread executorThread;
  private ExecutionType executionType;
  private StitchingGUIFrame stitchingGUI;
  private StitchingAppParams params;

  /**
   * Initializes the stitching execution
   *
   * @param stitchingGUI the stitching application GUI
   * @param type         the type of execution to be done
   */
  public StitchingSwingWorker(StitchingGUIFrame stitchingGUI, ExecutionType type) {
    this.params = new StitchingAppParams();


    this.executor = new StitchingExecutor(stitchingGUI, type, this.params);
    this.executionType = type;
    this.stitchingGUI = stitchingGUI;

  }

  @Override
  protected void done() {
    switch (this.executionType) {
      case LoadParams:
        runLoadParams();
        break;
      case SaveParams:
        runSaveParams();
        break;
      default:
        // do nothing
        break;
    }

    super.done();
    if (this.executorThread != null && this.executorThread.isAlive()) {
      this.executor.cancelExecution();
    }
  }

  @Override
  protected Void doInBackground() throws Exception {

    switch (this.executionType) {
      case LoadParams:
        // do nothing, the load params will be performed in the done method
        break;
      case SaveParams:
        // do nothing, the save params will be performed in the done method
        break;
      case PreviewNoOverlap:
        this.executorThread = new Thread(this.executor);
        this.executorThread.start();
        this.executorThread.join();
        break;
      case RunStitching:
      case RunStitchingMacro:
      case RunStitchingFromMeta:
        MISTMain.enableStitching();
        this.executorThread = new Thread(this.executor);
        this.executorThread.start();
        this.executorThread.join();

        break;
      case RunStitchingFromMetaMacro:
      default:
        break;

    }

    return null;
  }


  private void runSaveParams() {
    Log.msg(LogType.MANDATORY, "Checking Parameters for save");


    if (this.stitchingGUI.checkAndParseGUI(this.params)) {

      JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
      chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      chooser.setMultiSelectionEnabled(false);

      int val = chooser.showSaveDialog(this.stitchingGUI);
      if (val == JFileChooser.APPROVE_OPTION) {
        File file = chooser.getSelectedFile();
        if (!file.exists()) {
          try {
            file.createNewFile();
          } catch (IOException e1) {
            Log.msg(LogType.MANDATORY, e1.getMessage());
          }
        }

        Log.msg(LogType.MANDATORY, "Saving Parameters");
        this.params.saveParams(file);
      }
    } else {
      Log.msg(LogType.MANDATORY, "Stitching parameter check"
          + " failed. Check the console for information. "
          + "(increase logging level for more details)");
    }

  }

  private void runLoadParams() {
    Log.msg(LogType.MANDATORY, "Loading Parameters from file");

    JFileChooser chooser = new JFileChooser(this.stitchingGUI.getInputPanel().getImageDirectory());
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    chooser.setMultiSelectionEnabled(false);

    int val = chooser.showOpenDialog(this.stitchingGUI);
    if (val == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();

      Log.msg(LogType.MANDATORY, "Loading Parameters");
      if (this.params.loadParams(file)) {
        this.params.printParams();

        this.stitchingGUI.loadParamsIntoGUI(StitchingSwingWorker.this.params);
      }


    }
  }

}
