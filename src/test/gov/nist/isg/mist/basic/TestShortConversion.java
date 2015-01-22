package test.gov.nist.isg.mist.basic;

import main.gov.nist.isg.mist.stitching.lib.log.Log;
import main.gov.nist.isg.mist.stitching.lib.log.Log.LogType;

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
