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
// Date: Apr 18, 2014 1:03:55 PM EST
//
// Time-stamp: <Apr 18, 2014 1:03:55 PM tjb3>
//
//
// ================================================================

package main.gov.nist.isg.mist.stitching.gui.params.objects;

/**
 * CudaDeviceParam represents a CUDA device. This is used to manage what devices are chosen by the
 * user
 * 
 * @author Tim Blattner
 * @version 1.0
 * 
 */
public class CudaDeviceParam {

  private int id;
  private String name;
  private int devMinor;
  private int devMajor;

  /**
   * Initializes a CUDA device parameter
   * 
   * @param id the id of the CUDA device
   * @param name the name of the CUDA device
   * @param devMinor the device minor version
   * @param devMajor the device major version
   */
  public CudaDeviceParam(int id, String name, int devMinor, int devMajor) {
    this.id = id;
    this.name = name.trim();
    this.devMinor = devMinor;
    this.devMajor = devMajor;
  }

  @Override
  public String toString() {
    return "Device " + this.id + ": " + this.name + " with Compute Capability " + this.devMajor + "." + this.devMinor;
  }

  /**
   * @return the id
   */
  public int getId() {
    return this.id;
  }

  /**
   * @param id the id to set
   */
  public void setId(int id) {
    this.id = id;
  }

  /**
   * @return the name
   */
  public String getName() {
    return this.name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the devMinor
   */
  public int getDevMinor() {
    return this.devMinor;
  }

  /**
   * @param devMinor the devMinor to set
   */
  public void setDevMinor(int devMinor) {
    this.devMinor = devMinor;
  }

  /**
   * @return the devMajor
   */
  public int getDevMajor() {
    return this.devMajor;
  }

  /**
   * @param devMajor the devMajor to set
   */
  public void setDevMajor(int devMajor) {
    this.devMajor = devMajor;
  }

}
