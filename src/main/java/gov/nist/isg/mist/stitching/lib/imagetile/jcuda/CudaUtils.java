// ================================================================
//
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
//
// ================================================================

// ================================================================
//
// Author: tjb3
// Date: Apr 7, 2014 6:03:46 PM EST
//
// Time-stamp: <Apr 7, 2014 6:03:46 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.imagetile.jcuda;

import gov.nist.isg.mist.stitching.gui.params.objects.CudaDeviceParam;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import jcuda.CudaException;
import jcuda.driver.CUcontext;
import jcuda.driver.CUdevice;
import jcuda.driver.JCudaDriver;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static jcuda.driver.CUdevice_attribute.*;
import static jcuda.driver.JCudaDriver.*;

/**
 * Utility class for initializing JCUDA.
 * 
 * @author Tim Blattner
 * @version 1.0
 */
public class CudaUtils {

  /**
   * The number of columns in the CUDA table
   */
  public static final int TBL_COL_SIZE = 4;


  private static final int CHAR_LEN = 1024;


  /**
   * The index in the CUDA table for the is selected
   */
  public static final int TBL_COL_SELECTED = 0;

  /**
   * The index in the CUDA table for the ID
   */
  public static final int TBL_COL_ID = 1;

  /**
   * The index in the CUDA table for the name
   */
  public static final int TBL_COL_NAME = 2;

  /**
   * The index in the CUDA table for the compute capabilities
   */
  public static final int TBL_COL_CAPABIL = 3;



  private static boolean initialized = false;
  private static CUdevice[] devices = null;
  private static CUcontext[] contexts = null;

  /**
   * Initializes GPUs
   * 
   * @param nGPUs the number of GPUs
   * @param gpuIds the array of GPU ids
   * @return the contexts to the GPUs
   */
  public static CUcontext[] initGPUs(int nGPUs, int[] gpuIds) {
    Log.msg(LogType.INFO, "Initializing GPU contexts");

    CUcontext[] ret = new CUcontext[nGPUs];
    devices = new CUdevice[nGPUs];

    for (int i = 0; i < nGPUs; i++) {
      devices[i] = new CUdevice();
      cuDeviceGet(devices[i], gpuIds[i]);

      printDescription(devices[i], i);

      ret[i] = new CUcontext();

      cuCtxCreate(ret[i], 0, devices[i]);

    }

    return ret;
  }

  /**
   * Checks if GPUs have been initialized or not
   * 
   * @return true if GPUs have been initialized, otherwise false
   */
  public static boolean isGPUInitialized() {
    return initialized;
  }

  /**
   * Gets the GPU contexts
   * 
   * @return the GPU contexts
   */
  public static CUcontext[] getGPUContexts() {
    return contexts;
  }

  /**
   * Initializes JCUDA
   * 
   * @param devices the list of devices to initialize
   * @param initTile the initial image tile
   * @return the contexts to the GPUs, or null if intiailization failed
   */
  public static CUcontext[] initJCUDA(List<CudaDeviceParam> devices, ImageTile<?> initTile) {
    int nGPUs = devices.size();
    int[] gpuIDs = new int[nGPUs];

    for (int i = 0; i < devices.size(); i++) {
      gpuIDs[i] = devices.get(i).getId();
    }

    return initJCUDA(nGPUs, gpuIDs, initTile);

  }

  /**
   * Initializes JCUDA
   * 
   * @param nGPUs the number of GPUs to initialize
   * @param gpuIDs the array of GPU ids
   * @param initTile the initial image tile
   * @return the context to the GPUs, or null if intiailization failed
   */
  public static CUcontext[] initJCUDA(int nGPUs, int[] gpuIDs, ImageTile<?> initTile) {    
    if (contexts != null)
      return contexts;

    Log.msg(LogType.INFO, "Attempting to initialize GPU");
    cuInit(0);

    contexts = initGPUs(nGPUs, gpuIDs);

    Log.msg(LogType.INFO, "Initializing GPU functions");
    CudaImageTile.initFunc(nGPUs);
    for (int i = 0; i < nGPUs; i++) {
      Log.msg(LogType.INFO, "Initializing functions for GPU " + i);
      try {
        if (!CudaImageTile.initPlans(initTile.getWidth(), initTile.getHeight(), contexts[i], i))
          return null;
      } catch (IOException e) {
        Log.msg(LogType.MANDATORY, "Unable to load CUDA PTX file.");
        return null;
      }

    }

    initialized = true;
    return contexts;

  }

  /**
   * Destroys the GPU contexts
   * 
   * @param nGPUs the number of GPUs
   */
  public static void destroyJCUDA(int nGPUs) {
    for (int i = 0; i < nGPUs; i++) {
      CudaImageTile.destroyPlans(i);
      JCudaDriver.cuCtxDestroy(contexts[i]);
    }

    contexts = null;
  }

  /**
   * Gets GPU table information
   * 
   * @return the array of GPU information for displaying in a table
   * @throws UnsatisfiedLinkError
   * @throws NoClassDefFoundError
   * @throws CudaException 
   */
  public static String[][] getTableInformation() throws UnsatisfiedLinkError, NoClassDefFoundError, CudaException {
    setExceptionsEnabled(true);

    cuInit(0);
    int[] count = {0};
    JCudaDriver.cuDeviceGetCount(count);
    int deviceCount = count[0];
    String[][] ret = new String[deviceCount][TBL_COL_SIZE];

    for (int i = 0; i < deviceCount; i++) {
      CUdevice device = new CUdevice();
      cuDeviceGet(device, i);

      // Obtain the device name
      byte deviceName[] = new byte[CHAR_LEN];
      cuDeviceGetName(deviceName, deviceName.length, device);
      String name = createString(deviceName);

      // Obtain the compute capability
      int majorArray[] = {0};
      int minorArray[] = {0};
      cuDeviceComputeCapability(majorArray, minorArray, device);
      int major = majorArray[0];
      int minor = minorArray[0];

      ret[i][TBL_COL_ID] = Integer.toString(i);
      ret[i][TBL_COL_NAME] = name;
      ret[i][TBL_COL_CAPABIL] = Integer.toString(major) + "." + Integer.toString(minor);
    }
    setExceptionsEnabled(false);
    return ret;
  }

  /**
   * Outputs device query stats Modified from: http://www.jcuda.org/samples/JCudaDeviceQuery.java
   */
  public static void deviceQuery() {
    cuInit(0);
    int[] count = {0};
    JCudaDriver.cuDeviceGetCount(count);
    int deviceCount = count[0];
    Log.msgNoTime(LogType.MANDATORY, "Found " + deviceCount + " device(s)");

    for (int i = 0; i < deviceCount; i++) {
      CUdevice device = new CUdevice();
      cuDeviceGet(device, i);

      // Obtain the device name
      byte deviceName[] = new byte[CHAR_LEN];
      cuDeviceGetName(deviceName, deviceName.length, device);
      String name = createString(deviceName);

      // Obtain the compute capability
      int majorArray[] = {0};
      int minorArray[] = {0};
      cuDeviceComputeCapability(majorArray, minorArray, device);
      int major = majorArray[0];
      int minor = minorArray[0];

      Log.msgNoTime(LogType.MANDATORY, "Device " + i + ": " + name + " with Compute Capability "
          + major + "." + minor);

      // Obtain the device attributes
      int array[] = {0};
      List<Integer> attributes = getAttributes();
      for (Integer attribute : attributes) {
        String description = getAttributeDescription(attribute);
        cuDeviceGetAttribute(array, attribute, device);
        int value = array[0];

        Log.msgNoTime(LogType.MANDATORY, String.format("    %-52s : %d", description, value));
      }
    }
  }

  /**
   * Prints description of device Modified from: http://www.jcuda.org/samples/JCudaDeviceQuery.java
   * 
   * @param device the CUDA device
   * @param dev the dev ID
   */
  public static void printDescription(CUdevice device, int dev) {
    byte deviceName[] = new byte[CHAR_LEN];

    cuDeviceGetName(deviceName, deviceName.length, device);
    String name = createString(deviceName);

    // Obtain the compute capability
    int majorArray[] = {0};
    int minorArray[] = {0};
    cuDeviceComputeCapability(majorArray, minorArray, device);
    int major = majorArray[0];
    int minor = minorArray[0];

    Log.msgNoTime(LogType.INFO, "Device " + dev + ": " + name + " with Compute Capability " + major
        + "." + minor);

    // Obtain the device attributes
    int array[] = {0};
    List<Integer> attributes = getAttributes();
    for (Integer attribute : attributes) {
      String description = getAttributeDescription(attribute);
      cuDeviceGetAttribute(array, attribute, device);
      int value = array[0];

      Log.msgNoTime(LogType.INFO, String.format("    %-52s : %d", description, value));
    }
  }

  /**
   * Returns a short description of the given CUdevice_attribute constant Obtained from JCUDA
   * deviceQuery example: http://www.jcuda.org/samples/JCudaDeviceQuery.java
   * 
   * @return A short description of the given constant
   */
  private static String getAttributeDescription(int attribute) {
    switch (attribute) {
      case CU_DEVICE_ATTRIBUTE_MAX_THREADS_PER_BLOCK:
        return "Maximum number of threads per block";
      case CU_DEVICE_ATTRIBUTE_MAX_BLOCK_DIM_X:
        return "Maximum x-dimension of a block";
      case CU_DEVICE_ATTRIBUTE_MAX_BLOCK_DIM_Y:
        return "Maximum y-dimension of a block";
      case CU_DEVICE_ATTRIBUTE_MAX_BLOCK_DIM_Z:
        return "Maximum z-dimension of a block";
      case CU_DEVICE_ATTRIBUTE_MAX_GRID_DIM_X:
        return "Maximum x-dimension of a grid";
      case CU_DEVICE_ATTRIBUTE_MAX_GRID_DIM_Y:
        return "Maximum y-dimension of a grid";
      case CU_DEVICE_ATTRIBUTE_MAX_GRID_DIM_Z:
        return "Maximum z-dimension of a grid";
      case CU_DEVICE_ATTRIBUTE_MAX_SHARED_MEMORY_PER_BLOCK:
        return "Maximum shared memory per thread block in bytes";
      case CU_DEVICE_ATTRIBUTE_TOTAL_CONSTANT_MEMORY:
        return "Total constant memory on the device in bytes";
      case CU_DEVICE_ATTRIBUTE_WARP_SIZE:
        return "Warp size in threads";
      case CU_DEVICE_ATTRIBUTE_MAX_PITCH:
        return "Maximum pitch in bytes allowed for memory copies";
      case CU_DEVICE_ATTRIBUTE_MAX_REGISTERS_PER_BLOCK:
        return "Maximum number of 32-bit registers per thread block";
      case CU_DEVICE_ATTRIBUTE_CLOCK_RATE:
        return "Clock frequency in kilohertz";
      case CU_DEVICE_ATTRIBUTE_TEXTURE_ALIGNMENT:
        return "Alignment requirement";
      case CU_DEVICE_ATTRIBUTE_MULTIPROCESSOR_COUNT:
        return "Number of multiprocessors on the device";
      case CU_DEVICE_ATTRIBUTE_KERNEL_EXEC_TIMEOUT:
        return "Whether there is a run time limit on kernels";
      case CU_DEVICE_ATTRIBUTE_INTEGRATED:
        return "Device is integrated with host memory";
      case CU_DEVICE_ATTRIBUTE_CAN_MAP_HOST_MEMORY:
        return "Device can map host memory into CUDA address space";
      case CU_DEVICE_ATTRIBUTE_COMPUTE_MODE:
        return "Compute mode";
      case CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE1D_WIDTH:
        return "Maximum 1D texture width";
      case CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE2D_WIDTH:
        return "Maximum 2D texture width";
      case CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE2D_HEIGHT:
        return "aximum 2D texture height";
      case CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE3D_WIDTH:
        return "Maximum 3D texture width";
      case CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE3D_HEIGHT:
        return "Maximum 3D texture height";
      case CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE3D_DEPTH:
        return "Maximum 3D texture depth";
      case CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE2D_LAYERED_WIDTH:
        return "Maximum 2D layered texture width";
      case CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE2D_LAYERED_HEIGHT:
        return "Maximum 2D layered texture height";
      case CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE2D_LAYERED_LAYERS:
        return "Maximum layers in a 2D layered texture";
      case CU_DEVICE_ATTRIBUTE_SURFACE_ALIGNMENT:
        return "Alignment requirement for surfaces";
      case CU_DEVICE_ATTRIBUTE_CONCURRENT_KERNELS:
        return "Device can execute multiple kernels concurrently";
      case CU_DEVICE_ATTRIBUTE_ECC_ENABLED:
        return "Device has ECC support enabled";
      case CU_DEVICE_ATTRIBUTE_PCI_BUS_ID:
        return "PCI bus ID of the device";
      case CU_DEVICE_ATTRIBUTE_PCI_DEVICE_ID:
        return "PCI device ID of the device";
      case CU_DEVICE_ATTRIBUTE_TCC_DRIVER:
        return "Device is using TCC driver model";
      case CU_DEVICE_ATTRIBUTE_MEMORY_CLOCK_RATE:
        return "Peak memory clock frequency in kilohertz";
      case CU_DEVICE_ATTRIBUTE_GLOBAL_MEMORY_BUS_WIDTH:
        return "Global memory bus width in bits";
      case CU_DEVICE_ATTRIBUTE_L2_CACHE_SIZE:
        return "Size of L2 cache in bytes";
      case CU_DEVICE_ATTRIBUTE_MAX_THREADS_PER_MULTIPROCESSOR:
        return "Maximum resident threads per multiprocessor";
      case CU_DEVICE_ATTRIBUTE_ASYNC_ENGINE_COUNT:
        return "Number of asynchronous engines";
      case CU_DEVICE_ATTRIBUTE_UNIFIED_ADDRESSING:
        return "Device shares a unified address space with the host";
      case CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE1D_LAYERED_WIDTH:
        return "Maximum 1D layered texture width";
      case CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE1D_LAYERED_LAYERS:
        return "Maximum layers in a 1D layered texture";
      case CU_DEVICE_ATTRIBUTE_PCI_DOMAIN_ID:
        return "PCI domain ID of the device";
    }
    return "(UNKNOWN ATTRIBUTE)";
  }

  /**
   * Returns a list of all CUdevice_attribute constants Obtained from:
   * http://www.jcuda.org/samples/JCudaDeviceQuery.java
   * 
   * @return A list of all CUdevice_attribute constants
   */
  private static List<Integer> getAttributes() {
    List<Integer> list = new ArrayList<Integer>();
    list.add(CU_DEVICE_ATTRIBUTE_MAX_THREADS_PER_BLOCK);
    list.add(CU_DEVICE_ATTRIBUTE_MAX_BLOCK_DIM_X);
    list.add(CU_DEVICE_ATTRIBUTE_MAX_BLOCK_DIM_Y);
    list.add(CU_DEVICE_ATTRIBUTE_MAX_BLOCK_DIM_Z);
    list.add(CU_DEVICE_ATTRIBUTE_MAX_GRID_DIM_X);
    list.add(CU_DEVICE_ATTRIBUTE_MAX_GRID_DIM_Y);
    list.add(CU_DEVICE_ATTRIBUTE_MAX_GRID_DIM_Z);
    list.add(CU_DEVICE_ATTRIBUTE_MAX_SHARED_MEMORY_PER_BLOCK);
    list.add(CU_DEVICE_ATTRIBUTE_TOTAL_CONSTANT_MEMORY);
    list.add(CU_DEVICE_ATTRIBUTE_WARP_SIZE);
    list.add(CU_DEVICE_ATTRIBUTE_MAX_PITCH);
    list.add(CU_DEVICE_ATTRIBUTE_MAX_REGISTERS_PER_BLOCK);
    list.add(CU_DEVICE_ATTRIBUTE_CLOCK_RATE);
    list.add(CU_DEVICE_ATTRIBUTE_TEXTURE_ALIGNMENT);
    list.add(CU_DEVICE_ATTRIBUTE_MULTIPROCESSOR_COUNT);
    list.add(CU_DEVICE_ATTRIBUTE_KERNEL_EXEC_TIMEOUT);
    list.add(CU_DEVICE_ATTRIBUTE_INTEGRATED);
    list.add(CU_DEVICE_ATTRIBUTE_CAN_MAP_HOST_MEMORY);
    list.add(CU_DEVICE_ATTRIBUTE_COMPUTE_MODE);
    list.add(CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE1D_WIDTH);
    list.add(CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE2D_WIDTH);
    list.add(CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE2D_HEIGHT);
    list.add(CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE3D_WIDTH);
    list.add(CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE3D_HEIGHT);
    list.add(CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE3D_DEPTH);
    list.add(CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE2D_LAYERED_WIDTH);
    list.add(CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE2D_LAYERED_HEIGHT);
    list.add(CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE2D_LAYERED_LAYERS);
    list.add(CU_DEVICE_ATTRIBUTE_SURFACE_ALIGNMENT);
    list.add(CU_DEVICE_ATTRIBUTE_CONCURRENT_KERNELS);
    list.add(CU_DEVICE_ATTRIBUTE_ECC_ENABLED);
    list.add(CU_DEVICE_ATTRIBUTE_PCI_BUS_ID);
    list.add(CU_DEVICE_ATTRIBUTE_PCI_DEVICE_ID);
    list.add(CU_DEVICE_ATTRIBUTE_TCC_DRIVER);
    list.add(CU_DEVICE_ATTRIBUTE_MEMORY_CLOCK_RATE);
    list.add(CU_DEVICE_ATTRIBUTE_GLOBAL_MEMORY_BUS_WIDTH);
    list.add(CU_DEVICE_ATTRIBUTE_L2_CACHE_SIZE);
    list.add(CU_DEVICE_ATTRIBUTE_MAX_THREADS_PER_MULTIPROCESSOR);
    list.add(CU_DEVICE_ATTRIBUTE_ASYNC_ENGINE_COUNT);
    list.add(CU_DEVICE_ATTRIBUTE_UNIFIED_ADDRESSING);
    list.add(CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE1D_LAYERED_WIDTH);
    list.add(CU_DEVICE_ATTRIBUTE_MAXIMUM_TEXTURE1D_LAYERED_LAYERS);
    list.add(CU_DEVICE_ATTRIBUTE_PCI_DOMAIN_ID);
    return list;
  }

  /**
   * Creates a String from a zero-terminated string in a byte array Obtained from:
   * http://www.jcuda.org/samples/JCudaDeviceQuery.java
   * 
   * @param bytes The byte array
   * @return The String
   */
  private static String createString(byte bytes[]) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < bytes.length; i++) {
      char c = (char) bytes[i];
      if (c == 0) {
        break;
      }
      sb.append(c);
    }
    return sb.toString();
  }

}
