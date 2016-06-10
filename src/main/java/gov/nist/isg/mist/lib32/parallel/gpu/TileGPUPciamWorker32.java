// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 11, 2014 12:28:44 PM EST
//
// Time-stamp: <Apr 11, 2014 12:28:44 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib32.parallel.gpu;

import java.awt.GraphicsEnvironment;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.PriorityBlockingQueue;

import javax.swing.JOptionPane;

import gov.nist.isg.mist.lib.executor.StitchingExecutor;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.imagetile.Stitching;
import gov.nist.isg.mist.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.lib.log.Debug;
import gov.nist.isg.mist.lib.log.Debug.DebugType;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.parallel.common.StitchingTask;
import gov.nist.isg.mist.lib.parallel.common.StitchingTask.TaskType;
import gov.nist.isg.mist.lib32.imagetile.jcuda.CudaImageTile32;
import gov.nist.isg.mist.lib32.imagetile.jcuda.CudaStitching32;
import jcuda.Sizeof;
import jcuda.driver.CUcontext;
import jcuda.driver.CUdevice;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.CUresult;
import jcuda.driver.CUstream;
import jcuda.driver.CUstream_flags;
import jcuda.driver.JCudaDriver;

/**
 * Class that computes the PCIAM (phase correlation image alignment method) of a tile on the GPU.
 * One thread per GPU is used.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TileGPUPciamWorker32<T> implements Runnable {

  private PriorityBlockingQueue<StitchingTask<T>> workQueue;
  private PriorityBlockingQueue<StitchingTask<T>> bkQueue;
  private PriorityBlockingQueue<StitchingTask<T>> ccfQueue;

  private static boolean bkDone = false;

  private TileWorkerMemory memory;

  private CUstream stream;

  private CUcontext context;
  private CUdevice device;

  private int devID;
  private int threadID;

  private CUcontext[] peerContexts;
  private int[] peerDevIds;

  private HashSet<Integer> nonPeerDevIds;
  private HashMap<Integer, CUcontext> peerContextMap;
  private CUdeviceptr devMem;

  private StitchingExecutor executor;

  private int tileWidth;
  private int tileHeight;

  private volatile boolean isCancelled;


  /**
   * Initializes a tile worker pool for computing PCIAM and FFT computations
   *
   * @param workQueue    the work queue
   * @param bkQueue      the bookkeeper queue
   * @param ccfQueue     the CCF queue
   * @param memory       the tile worker memory
   * @param tileWidth    the width of the image tile
   * @param tileHeight   the height of the image tile
   * @param devID        the device ID associated with this thread
   * @param threadID     the thread ID associated with this thread
   * @param context      the context associated with this thread
   * @param peerContexts the array of all contexts used
   * @param peerDevIds   the array of all device IDs used
   */
  public TileGPUPciamWorker32(PriorityBlockingQueue<StitchingTask<T>> workQueue,
                              PriorityBlockingQueue<StitchingTask<T>> bkQueue, PriorityBlockingQueue<StitchingTask<T>> ccfQueue,
                              TileWorkerMemory memory,
                              int tileWidth, int tileHeight, int devID, int threadID, CUcontext context, CUcontext[] peerContexts,
                              int[] peerDevIds) {
    bkDone = false;
    this.memory = memory;
    this.workQueue = workQueue;
    this.bkQueue = bkQueue;
    this.ccfQueue = ccfQueue;
    this.context = context;
    this.device = new CUdevice();
    JCudaDriver.cuDeviceGet(this.device, devID);
    this.devID = devID;
    this.threadID = threadID;
    this.peerContexts = peerContexts;
    this.peerDevIds = peerDevIds;
    this.nonPeerDevIds = new HashSet<Integer>();
    this.devMem = new CUdeviceptr();
    this.peerContextMap = new HashMap<Integer, CUcontext>();
    this.isCancelled = false;
    this.tileWidth = tileWidth;
    this.tileHeight = tileHeight;
  }

  @Override
  public void run() {
    JCudaDriver.cuCtxSetCurrent(this.context);

    // Allocate extra device memory for non peer-to-peer copy
    int res = JCudaDriver.cuMemAlloc(this.devMem, CudaImageTile32.fftSize * Sizeof.FLOAT * 2);
    checkCudaOutOfMemoryError(res);

    // Allocate phase correlation matrix memory
    CUdeviceptr pcm = new CUdeviceptr();
    res = JCudaDriver.cuMemAlloc(pcm, this.tileWidth * this.tileHeight * Sizeof.FLOAT);
    checkCudaOutOfMemoryError(res);

    this.stream = new CUstream();
    JCudaDriver.cuStreamCreate(this.stream, CUstream_flags.CU_STREAM_DEFAULT);
    CudaImageTile32.bindBwdPlanToStream(this.stream, this.threadID);

    CUdevice dev = new CUdevice();
    JCudaDriver.cuDeviceGet(dev, this.devID);

    // Create lookup table for which peer GPUs have peer to peer copy
    if (this.peerContexts != null) {
      for (int i = 0; i < this.peerContexts.length; i++) {
        CUcontext ctx = this.peerContexts[i];
        if (ctx != this.context) {
          CUdevice peerDev = new CUdevice();
          JCudaDriver.cuDeviceGet(peerDev, this.peerDevIds[i]);
          int[] canAccessPeer = new int[]{0};
          JCudaDriver.cuDeviceCanAccessPeer(canAccessPeer, dev, peerDev);

          if (canAccessPeer[0] == 0) {
            this.nonPeerDevIds.add(this.peerDevIds[i]);
            this.peerContextMap.put(this.peerDevIds[i], ctx);

          } else {
            JCudaDriver.cuCtxEnablePeerAccess(ctx, 0);
          }
        }

      }
    }


    try {
      while (!this.isCancelled && (!bkDone || this.workQueue.size() > 0)) {
        StitchingTask<T> task = this.workQueue.take();

        Debug.msg(DebugType.VERBOSE,
            "WP Task acquired: " + task.getTask() + "  size: " + this.workQueue.size());
        if (task.getTask() == TaskType.PCIAM_NORTH || task.getTask() == TaskType.PCIAM_WEST) {
          ImageTile<T> tile = task.getTile();
          ImageTile<T> neighbor = task.getNeighbor();


          int neighborDev = neighbor.getDev();
          int tileDev = tile.getDev();

          // Check if neighbor memory is on another GPU and if
          // peer to peer is not available
          if (neighborDev != tileDev && this.nonPeerDevIds.contains(neighborDev)) {
            CudaImageTile32 cudaTile = (CudaImageTile32) neighbor;
            CUdeviceptr fft = cudaTile.getFft();

            // copy device to device
            JCudaDriver.cuMemcpyPeerAsync(this.devMem, this.context, fft,
                this.peerContextMap.get(cudaTile.getDev()), CudaImageTile32.fftSize * Sizeof.FLOAT * 2,
                this.stream);

            CudaStitching32.peakCorrelationMatrix(this.devMem, (CudaImageTile32) tile, pcm, this.memory, this.stream,
                this.threadID);
          } else {
            CudaStitching32.peakCorrelationMatrix((CudaImageTile32) neighbor, (CudaImageTile32) tile,
                pcm, this.memory, this.stream, this.threadID);
          }


          int[] indices;
          indices =
              CudaStitching32.multiPeakCorrelationMatrixIndices(pcm, Stitching.NUM_PEAKS,
                  tile.getWidth(), tile.getHeight(), this.memory, this.stream, this.threadID);

          task.setTask(TaskType.BK_CHECK_MEM);
          this.bkQueue.put(task);

          StitchingTask<T> ccfTask =
              new StitchingTask<T>(tile, neighbor, indices, this.devID, this.threadID,
                  TaskType.CCF);
          this.ccfQueue.put(ccfTask);

        } else if (task.getTask() == TaskType.BK_DONE) {
          synchronized (this) {
            bkDone = true;
          }
        } else if (task.getTask() == TaskType.CANCELLED) {
          this.isCancelled = true;
        }

      }

      Debug.msg(DebugType.HELPFUL, "PCIAM Done");

      // Signal other workers that things are done
      this.workQueue.put(new StitchingTask<T>(null, null, TaskType.SENTINEL));

    } catch (InterruptedException e) {
      Log.msg(LogType.MANDATORY, "Interrupted PCIAM worker");
    }


    if (this.stream != null) {
      JCudaDriver.cuStreamDestroy(this.stream);
    }

    if (this.devMem != null)
      JCudaDriver.cuMemFree(this.devMem);

    if (pcm != null)
      JCudaDriver.cuMemFree(pcm);

  }

  /**
   * Cancels the task
   */
  public void cancel() {
    this.workQueue.put(new StitchingTask<T>(null, null, TaskType.CANCELLED));
  }

  private void checkCudaOutOfMemoryError(int res) {
    if (res == CUresult.CUDA_ERROR_OUT_OF_MEMORY) {
      Log.msg(LogType.MANDATORY, "Error: Insufficient graphics memory to complete stitching.");
      if (!GraphicsEnvironment.isHeadless()) {
        JOptionPane.showMessageDialog(null,
            "Error: Insufficient graphics memory to complete stitching.");
      }

      if (this.executor == null)
        System.exit(1);
      else
        this.executor.cancelExecution();

    }


  }

}
