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

package gov.nist.isg.mist.lib32.imagetile.jcuda;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gov.nist.isg.mist.lib.common.CorrelationTriple;
import gov.nist.isg.mist.lib.imagetile.Stitching;
import gov.nist.isg.mist.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.lib.log.Debug;
import gov.nist.isg.mist.lib.log.Debug.DebugType;
import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.CUstream;
import jcuda.driver.JCudaDriver;
import jcuda.jcufft.JCufft;

/**
 * Utility functions for doing image stitching using JCUDAImageTiles.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class CudaStitching32 {

  private static final int NUM_THREADS = 256;
  private static final int MAX_BLOCKS = 64;

  /**
   * Computes the phase correlatoin image alignment between two images
   *
   * @param t1     image 1
   * @param t2     image 2
   * @param memory the tile worker memory
   * @param stream the CUDA stream
   * @return the best relative displacement along the x and y axis and the correlation between two
   * images
   */
  public static CorrelationTriple phaseCorrelationImageAlignment(CudaImageTile32 t1,
                                                                 CudaImageTile32 t2, TileWorkerMemory memory, CUstream stream) {

    // If one of the two images does not exists, then a translation cannot exist
    if (!t1.fileExists() || !t2.fileExists())
      return new CorrelationTriple(-1.0, 0, 0);

    CUdeviceptr pcm = memory.getPcm();

    peakCorrelationMatrix(t1, t2, pcm, memory, stream, t1.getDev());

    List<CorrelationTriple> peaks =
        multiPeakCorrelationMatrix(pcm, Stitching.NUM_PEAKS, t1.getWidth(), t1.getHeight(), memory,
            stream, t1.getDev());

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
   * Computes the peak correlatoin matrix between two images
   *
   * @param t1     image 1
   * @param t2     image 2 * @return the peak correlation matrix
   * @param pcm    the phase correlation matrix
   * @param memory the tile worker memory
   * @param stream the CUDA stream
   * @param dev    the GPU device
   */
  public static void peakCorrelationMatrix(CudaImageTile32 t1, CudaImageTile32 t2, CUdeviceptr pcm,
                                           TileWorkerMemory memory, CUstream stream, int dev) {
    int numThreads = NUM_THREADS;

    int numBlocks = (int) Math.ceil((double) CudaImageTile32.fftSize / (double) numThreads);

    CUdeviceptr ptr = memory.getPcmIn();

    Pointer kernelParams =
        Pointer.to(Pointer.to(ptr), Pointer.to(t1.getFft()), Pointer.to(t2.getFft()),
            Pointer.to(new int[]{CudaImageTile32.fftSize}));

    JCudaDriver.cuLaunchKernel(CudaImageTile32.elt_prod_function[dev], numBlocks, 1, 1, numThreads,
        1, 1, 0, stream, kernelParams, null);

    JCufft.cufftExecC2R(CudaImageTile32.plan_bwd[dev], ptr, pcm);

  }

  /**
   * Computes the peak correlation matrix between two images
   *
   * @param t1     image 1
   * @param t2     image 2
   * @param pcm    the phase correlation matrix
   * @param memory the tile worker memory
   * @param stream the CUDA stream
   * @param dev    the GPU device
   */
  public static void peakCorrelationMatrix(CUdeviceptr t1, CudaImageTile32 t2, CUdeviceptr pcm,
                                           TileWorkerMemory memory, CUstream stream, int dev) {
    int numThreads = NUM_THREADS;

    int numBlocks = (int) Math.ceil((double) CudaImageTile32.fftSize / (double) numThreads);

    CUdeviceptr ptr = memory.getPcmIn();

    Pointer kernelParams =
        Pointer.to(Pointer.to(ptr), Pointer.to(t1), Pointer.to(t2.getFft()),
            Pointer.to(new int[]{CudaImageTile32.fftSize}));

    JCudaDriver.cuLaunchKernel(CudaImageTile32.elt_prod_function[dev], numBlocks, 1, 1, numThreads,
        1, 1, 0, stream, kernelParams, null);

    JCufft.cufftExecC2R(CudaImageTile32.plan_bwd[dev], ptr, pcm);

  }

  /**
   * Finds the n max peaks in the phase correlation matrix that are a distance of 10 from
   * eachother.
   *
   * @param pcm    the phase correlation matrix
   * @param nPeaks the number of peaks to be found
   * @param width  the width of the image
   * @param height the height of the image
   * @param memory the tile worker memory
   * @param stream the CUDA stream
   * @param dev    the GPU device number
   * @return a list of the correlation triples
   */
  public static List<CorrelationTriple> multiPeakCorrelationMatrix(CUdeviceptr pcm, int nPeaks,
                                                                   int width, int height, TileWorkerMemory memory, CUstream stream, int dev) {

    List<CorrelationTriple> peaks = new ArrayList<CorrelationTriple>();

    ByteBuffer indices = getMultiMaxIdx(pcm, nPeaks, width, height, memory, stream, dev);

    for (int i = 0; i < nPeaks; i++) {
      int idx = indices.getInt();
      int y = idx / width;
      int x = idx % width;

      peaks.add(new CorrelationTriple(0.0, x, y));
      Debug.msg(DebugType.VERBOSE, "Found multimax peak " + i + " ( " + x + ", " + y + ")");
    }

    return peaks;
  }

  /**
   * Finds the n max peaks in the phase correlation matrix
   *
   * @param pcm    the phase correlation matrix
   * @param nPeaks the number of peaks to be found
   * @param width  the width of the image
   * @param height the height of the image
   * @param memory the tile worker memory
   * @param stream the CUDA stream
   * @param dev    the GPU device number
   * @return array of indices
   */
  public static int[] multiPeakCorrelationMatrixIndices(CUdeviceptr pcm, int nPeaks, int width,
                                                        int height, TileWorkerMemory memory, CUstream stream, int dev) {

    int[] vals = new int[nPeaks];

    ByteBuffer indices = getMultiMaxIdx(pcm, nPeaks, width, height, memory, stream, dev);

    for (int i = 0; i < nPeaks; i++) {
      vals[i] = indices.getInt();

    }

    return vals;
  }

  /**
   * Finds multiple max indices
   *
   * getMultiMaxIdx was adapted from the CUDA SDK examples for parallel reduction:
   * http://developer.download.nvidia.com/compute/cuda/1.1-Beta/x86_website/projects/reduction/doc/reduction.pdf
   *
   * @param c      GPU device input memory
   * @param nPeaks the number of peaks to find
   * @param width  the width of the image
   * @param height the height of hte image
   * @param memory the GPU memory
   * @param stream the CUDA stream used
   * @param dev    the thread ID used
   * @return the index where the max exists
   */
  public static ByteBuffer getMultiMaxIdx(CUdeviceptr c, int nPeaks, int width, int height,
                                          TileWorkerMemory memory, CUstream stream, int dev) {
    int size = width * height;
    CUdeviceptr out = memory.getMultiMaxOut();
    CUdeviceptr idxOut = memory.getMultiIdxOut();
    CUdeviceptr idxFilter = memory.getIdxFilter();

    int nFilter = 0;

    while (nFilter < nPeaks) {
      int numThreads = NUM_THREADS;
      int reduceBlocks = (size + (numThreads * 2 - 1)) / (numThreads * 2);
      Pointer kernelParams =
          Pointer.to(Pointer.to(c), Pointer.to(out), Pointer.to(idxOut),
              Pointer.to(new int[]{width}), Pointer.to(new int[]{height}),
              Pointer.to(new int[]{numThreads}), Pointer.to(idxFilter),
              Pointer.to(new int[]{nFilter}));

      JCudaDriver.cuLaunchKernel(CudaImageTile32.reduce_max_filter_main[dev], reduceBlocks, 1, 1,
          numThreads, 1, 1, 0, stream, kernelParams, null);

      int remain_size = reduceBlocks;

      while (remain_size > 1) {
        int blocks = 0, threads = 0;
        threads = getNumThreads(remain_size, NUM_THREADS);
        blocks = getNumBlocks(remain_size, MAX_BLOCKS, threads);
        multiMax_filter_cases(out, out, idxOut, remain_size, width, idxFilter, nFilter, threads,
            blocks, stream, dev);

        remain_size = (remain_size + (threads * 2 - 1)) / (threads * 2);

      }

      nFilter++;
      JCudaDriver.cuStreamSynchronize(stream);

    }

    ByteBuffer val = memory.getFilterBuffer();
    val.rewind();
    JCudaDriver.cuMemcpyDtoHAsync(Pointer.to(val), idxFilter, nPeaks * Sizeof.INT, stream);
    JCudaDriver.cuStreamSynchronize(stream);
    return val;
  }

  /*
   * multiMax_filter_cases was adapted from the CUDA SDK examples for parallel reduction:
   * http://developer.download.nvidia.com/compute/cuda/1.1-Beta/x86_website/projects/reduction/doc/reduction.pdf
   */
  private static final void multiMax_filter_cases(CUdeviceptr in, CUdeviceptr out,
                                                  CUdeviceptr index, int size, int width, CUdeviceptr idxFilter, int nFilter, int threads,
                                                  int blocks, CUstream stream, int dev) {
    Pointer kernelParams = null;

    // switch for power of 2 threads for reduction
    switch (threads) {
      case 512:
        kernelParams =
            Pointer
                .to(Pointer.to(in), Pointer.to(out), Pointer.to(index),
                    Pointer.to(new int[]{size}), Pointer.to(new int[]{width}),
                    Pointer.to(new int[]{512}), Pointer.to(idxFilter),
                    Pointer.to(new int[]{nFilter}));

        break;
      case 256:
        kernelParams =
            Pointer
                .to(Pointer.to(in), Pointer.to(out), Pointer.to(index),
                    Pointer.to(new int[]{size}), Pointer.to(new int[]{width}),
                    Pointer.to(new int[]{256}), Pointer.to(idxFilter),
                    Pointer.to(new int[]{nFilter}));

        break;
      case 128:
        kernelParams =
            Pointer
                .to(Pointer.to(in), Pointer.to(out), Pointer.to(index),
                    Pointer.to(new int[]{size}), Pointer.to(new int[]{width}),
                    Pointer.to(new int[]{128}), Pointer.to(idxFilter),
                    Pointer.to(new int[]{nFilter}));

        break;
      case 64:
        kernelParams =
            Pointer.to(Pointer.to(in), Pointer.to(out), Pointer.to(index),
                Pointer.to(new int[]{size}), Pointer.to(new int[]{width}),
                Pointer.to(new int[]{64}), Pointer.to(idxFilter), Pointer.to(new int[]{nFilter}));

        break;
      case 32:
        kernelParams =
            Pointer.to(Pointer.to(in), Pointer.to(out), Pointer.to(index),
                Pointer.to(new int[]{size}), Pointer.to(new int[]{width}),
                Pointer.to(new int[]{32}), Pointer.to(idxFilter), Pointer.to(new int[]{nFilter}));

        break;
      case 16:
        kernelParams =
            Pointer.to(Pointer.to(in), Pointer.to(out), Pointer.to(index),
                Pointer.to(new int[]{size}), Pointer.to(new int[]{width}),
                Pointer.to(new int[]{16}), Pointer.to(idxFilter), Pointer.to(new int[]{nFilter}));

        break;
      case 8:
        kernelParams =
            Pointer.to(Pointer.to(in), Pointer.to(out), Pointer.to(index),
                Pointer.to(new int[]{size}), Pointer.to(new int[]{width}),
                Pointer.to(new int[]{8}), Pointer.to(idxFilter), Pointer.to(new int[]{nFilter}));

        break;
      case 4:
        kernelParams =
            Pointer.to(Pointer.to(in), Pointer.to(out), Pointer.to(index),
                Pointer.to(new int[]{size}), Pointer.to(new int[]{width}),
                Pointer.to(new int[]{4}), Pointer.to(idxFilter), Pointer.to(new int[]{nFilter}));

        break;
      case 2:
        kernelParams =
            Pointer.to(Pointer.to(in), Pointer.to(out), Pointer.to(index),
                Pointer.to(new int[]{size}), Pointer.to(new int[]{width}),
                Pointer.to(new int[]{2}), Pointer.to(idxFilter), Pointer.to(new int[]{nFilter}));

        break;
      case 1:
        kernelParams =
            Pointer.to(Pointer.to(in), Pointer.to(out), Pointer.to(index),
                Pointer.to(new int[]{size}), Pointer.to(new int[]{width}),
                Pointer.to(new int[]{1}), Pointer.to(idxFilter), Pointer.to(new int[]{nFilter}));

        break;
    }

    if (kernelParams != null) {
      JCudaDriver.cuLaunchKernel(CudaImageTile32.reduce_max_filter_final[dev], blocks, 1, 1, threads,
          1, 1, 0, stream, kernelParams, null);
    }

  }

  /**
   * Finds the max index inside of the pointer
   *
   * getMaxIdx was adapted from the CUDA SDK examples for parallel reduction:
   * http://developer.download.nvidia.com/compute/cuda/1.1-Beta/x86_website/projects/reduction/doc/reduction.pdf
   *
   * @param c      the pointer
   * @param size   the size of the pointer array
   * @param memory the tile worker memory
   * @param stream the CUDA stream
   * @param dev    the GPU device number
   * @return the index where the max exists inside the pointer
   */
  public static int getMaxIdx(CUdeviceptr c, int size, TileWorkerMemory memory, CUstream stream,
                              int dev) {

    CUdeviceptr out = memory.getMaxOut();
    CUdeviceptr idxOut = memory.getIdxOut();
    int numThreads = NUM_THREADS;
    int reduceBlocks = (size + (numThreads * 2 - 1)) / (numThreads * 2);
    Pointer kernelParams =
        Pointer.to(Pointer.to(c), Pointer.to(out), Pointer.to(idxOut),
            Pointer.to(new int[]{size}), Pointer.to(new int[]{numThreads}));

    JCudaDriver.cuLaunchKernel(CudaImageTile32.reduce_max_main[dev], reduceBlocks, 1, 1, numThreads,
        1, 1, 0, stream, kernelParams, null);

    int remain_size = reduceBlocks;

    while (remain_size > 1) {
      int blocks = 0, threads = 0;
      threads = getNumThreads(remain_size, NUM_THREADS);
      blocks = getNumBlocks(remain_size, MAX_BLOCKS, threads);

      reduce_max_final_cases(out, out, idxOut, remain_size, threads, blocks, stream, dev);

      remain_size = (remain_size + (threads * 2 - 1)) / (threads * 2);

    }

    ByteBuffer val = memory.getIndexBuffer();
    val.rewind();
    JCudaDriver.cuMemcpyDtoHAsync(Pointer.to(val), idxOut, Sizeof.INT, stream);

    JCudaDriver.cuStreamSynchronize(stream);
    return val.getInt(0);
  }

  /*
   * reduce_max_final_cases was adapted from the CUDA SDK examples for parallel reduction:
   * http://developer.download.nvidia.com/compute/cuda/1.1-Beta/x86_website/projects/reduction/doc/reduction.pdf
   */
  private static final void reduce_max_final_cases(CUdeviceptr in, CUdeviceptr out,
                                                   CUdeviceptr index, int size, int threads, int blocks, CUstream stream, int dev) {
    Pointer kernelParams = null;

    // switch for cases that are powers of 2
    switch (threads) {
      case 512:
        kernelParams =
            Pointer.to(Pointer.to(in), Pointer.to(out), Pointer.to(index),
                Pointer.to(new int[]{size}), Pointer.to(new int[]{512}));
        break;
      case 256:
        kernelParams =
            Pointer.to(Pointer.to(in), Pointer.to(out), Pointer.to(index),
                Pointer.to(new int[]{size}), Pointer.to(new int[]{256}));
        break;
      case 128:
        kernelParams =
            Pointer.to(Pointer.to(in), Pointer.to(out), Pointer.to(index),
                Pointer.to(new int[]{size}), Pointer.to(new int[]{128}));
        break;
      case 64:
        kernelParams =
            Pointer.to(Pointer.to(in), Pointer.to(out), Pointer.to(index),
                Pointer.to(new int[]{size}), Pointer.to(new int[]{64}));
        break;
      case 32:
        kernelParams =
            Pointer.to(Pointer.to(in), Pointer.to(out), Pointer.to(index),
                Pointer.to(new int[]{size}), Pointer.to(new int[]{32}));
        break;
      case 16:
        kernelParams =
            Pointer.to(Pointer.to(in), Pointer.to(out), Pointer.to(index),
                Pointer.to(new int[]{size}), Pointer.to(new int[]{16}));
        break;
      case 8:
        kernelParams =
            Pointer.to(Pointer.to(in), Pointer.to(out), Pointer.to(index),
                Pointer.to(new int[]{size}), Pointer.to(new int[]{8}));
        break;
      case 4:
        kernelParams =
            Pointer.to(Pointer.to(in), Pointer.to(out), Pointer.to(index),
                Pointer.to(new int[]{size}), Pointer.to(new int[]{4}));
        break;
      case 2:
        kernelParams =
            Pointer.to(Pointer.to(in), Pointer.to(out), Pointer.to(index),
                Pointer.to(new int[]{size}), Pointer.to(new int[]{2}));
        break;
      case 1:
        kernelParams =
            Pointer.to(Pointer.to(in), Pointer.to(out), Pointer.to(index),
                Pointer.to(new int[]{size}), Pointer.to(new int[]{1}));
        break;
    }

    if (kernelParams != null) {
      JCudaDriver.cuLaunchKernel(CudaImageTile32.reduce_max_final[dev], blocks, 1, 1, threads, 1, 1,
          0, stream, kernelParams, null);
    }

  }

  /*
   * nextPow2 was adapted from the CUDA SDK examples for parallel reduction:
   * http://developer.download.nvidia.com/compute/cuda/1.1-Beta/x86_website/projects/reduction/doc/reduction.pdf
   */
  private static int nextPow2(int x) {
    x = x - 1;
    x |= x >> 1;
    x |= x >> 2;
    x |= x >> 4;
    x |= x >> 8;
    x |= x >> 16;

    return x + 1;
  }

  /*
   * getNumBlocks was adapted from the CUDA SDK examples for parallel reduction:
   * http://developer.download.nvidia.com/compute/cuda/1.1-Beta/x86_website/projects/reduction/doc/reduction.pdf
   */
  private static int getNumBlocks(int size, int maxBlocks, int threads) {
    int blocks = (size + (threads * 2 - 1)) / (threads * 2);
    return (maxBlocks < blocks) ? maxBlocks : blocks;
  }

  /*
   * getNumThreads was adapted from the CUDA SDK examples for parallel reduction:
   * http://developer.download.nvidia.com/compute/cuda/1.1-Beta/x86_website/projects/reduction/doc/reduction.pdf
   */
  private static int getNumThreads(int size, int maxThreads) {
    return (size < maxThreads * 2) ? nextPow2(size) : maxThreads;
  }

}
