// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 18, 2014 1:03:55 PM EST
//
// Time-stamp: <Apr 18, 2014 1:03:55 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.gui.params.objects;

/**
 * CudaDeviceParam represents a CUDA device. This is used to manage what devices are chosen by the
 * user
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class CudaDeviceParam {

  private int id;
  private String name;
  private int devMinor;
  private int devMajor;

  /**
   * Initializes a CUDA device parameter
   *
   * @param id       the id of the CUDA device
   * @param name     the name of the CUDA device
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
