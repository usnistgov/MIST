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
// characteristic. We would appreciate acknowledgment if the software
// is used. This software can be redistributed and/or modified freely
// provided that any derivative works bear some notice that they are
// derived from it, and any modified versions bear some notice that
// they have been modified.
//
// ================================================================

// ================================================================
//
// Author: tjb3
// Date: May 10, 2013 2:58:58 PM EST
//
// Time-stamp: <May 10, 2013 2:58:58 PM tjb3>
//
//
// ================================================================


package gov.nist.isg.mist.basic;

import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;

/**
 * Tests converting a short value to an int value so we do not have
 * negative short values.
 * @author Tim Blattner
 * @version 1.0
 *
 */
public class TestShortConversion {

  /**
   * Main test case
   * @param args
   */
  public static void main(String[] args)
  {
    Log.msg(LogType.MANDATORY, "Running test short conversion");
    boolean passed = true;
    int count = 0;
    
    for (short test = Short.MIN_VALUE; test < Short.MAX_VALUE; test++)
    {
      String binaryTest = Integer.toBinaryString(0x10000 | (test & 0xFFFF)).substring(1);
      
      boolean isNeg = test < 0; 
      
      int temp = test & 0x7fff;
            
      if (isNeg)
        temp |= 0x8000;
      
      String binaryTemp = Integer.toBinaryString(0x10000 | temp).substring(1);
      
      if (!binaryTemp.equals(binaryTest))
      {
        passed = false;
        Log.msg(LogType.MANDATORY, "test: " + test + " FAILED");
      }
      count++;
      
    }
    
    if (passed)
      Log.msg(LogType.MANDATORY, count + " values were correctly converted");
    else
      Log.msg(LogType.MANDATORY, "Test failed!");
    
  }
  
}
