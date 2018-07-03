// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Jul 2, 2014 1:13:27 PM EST
//
// Time-stamp: <Jul 2, 2014 1:13:27 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.tilegrid.loader;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.tilegrid.loader.TileGridLoader.LoaderType;

/**
 * Utility methods for the tile grid loader and others for parsing file patterns
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TileGridLoaderUtils {

  /**
   * The time file pattern
   */
  public static final String timePattern = "(.*)(\\{[t]+\\})(.*)";

  /**
   * Gets the number of elements in the match string
   *
   * @param filePattern the file pattern
   * @param regex       the regular expression
   * @param silent      whether to show error or not
   * @return the position pattern
   */
  public static int getNumberMatchElements(String filePattern, String regex, boolean silent) {
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(filePattern);

    // Check if regex is correct. We expect 3 groups: (*)({ppp})(*)
    if (!matcher.find() || matcher.groupCount() != 3) {
      if (!silent) {
        Log.msg(LogType.MANDATORY, "Incorrect filePattern: " + filePattern);
        Log.msg(LogType.MANDATORY, "Regex: " + regex);
        Log.msg(LogType.MANDATORY, "Regex Groups count: " + matcher.groupCount());
        while (matcher.find()) {
          Log.msg(LogType.MANDATORY, matcher.group());
        }
        throw new IllegalArgumentException("Incorect filePattern: " + filePattern);
      }
      return 0;

    }

    // The matcher should fine at group: 0 - the entire string,
    // group 1 = prefix
    // group 2 = {i}
    // group 3 = suffix
    String prefix = matcher.group(1);
    int iCount = matcher.group(2).length() - 2;
    String suffix = matcher.group(3);

    return iCount;
  }

  /**
   * Gets the pattern associated with the regex
   *
   * @param filePattern the file pattern
   * @param regex       the regular expression
   * @param silent      whether to show error or not
   * @return the position pattern
   */
  public static String getPattern(String filePattern, String regex, boolean silent) {
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(filePattern);

    // Check if regex is correct. We expect 3 groups: (*)({ppp})(*)
    if (!matcher.find() || matcher.groupCount() != 3) {
      if (!silent) {
        Log.msg(LogType.MANDATORY, "Incorrect filePattern: " + filePattern);
        Log.msg(LogType.MANDATORY, "Regex: " + regex);
        Log.msg(LogType.MANDATORY, "Regex Groups count: " + matcher.groupCount());
        while (matcher.find()) {
          Log.msg(LogType.MANDATORY, matcher.group());
        }
        throw new IllegalArgumentException("Incorect filePattern: " + filePattern);
      }
      return null;

    }

    // The matcher should fine at group: 0 - the entire string,
    // group 1 = prefix
    // group 2 = {i}
    // group 3 = suffix
    String prefix = matcher.group(1);
    int iCount = matcher.group(2).length() - 2;
    String suffix = matcher.group(3);

    return prefix + "%0" + iCount + "d" + suffix;
  }


  /**
   * Checks if the time file pattern exists
   *
   * @param filePattern the file pattern to check
   * @return true if the file pattern contains a time parameter, otherwise false
   */
  public static boolean hasTimeFilePattern(String filePattern) {
    return hasFilePattern(filePattern, timePattern);

  }

  /**
   * Checks if the file pattern exists
   *
   * @param filePattern the file pattern to check
   * @param regex       the regular expression to check
   * @return true if the file pattern contains a time parameter, otherwise false
   */
  public static boolean hasFilePattern(String filePattern, String regex) {
    Pattern patternTime = Pattern.compile(regex);
    Matcher matcherTime = patternTime.matcher(filePattern);

    if (!matcherTime.find() || matcherTime.groupCount() != 3)
      return false;
    return true;
  }

  /**
   * Parses a time slice pattern
   *
   * @param filePattern the file pattern
   * @param timeSlice   the time slice
   * @param silent      whether to output errors or not
   * @return a String that replaces the time slice pattern with the time slice
   */
  public static String parseTimeSlicePattern(String filePattern, int timeSlice, boolean silent) {
    String timeMatcher = getPattern(filePattern, timePattern, silent);
    return String.format(timeMatcher, timeSlice);
  }

  /**
   * Parses a position pattern. For row-column it assumes the row and col are the same.
   *
   * @param filePattern the file pattern
   * @param loaderType  the type of file pattern loader
   * @param startTile    the initial tile position
   * @param startRow    the initial tile position
   * @param startCol    the initial tile position
   * @param silent      whether to output errors or not
   * @return a String that replaces the time slice pattern with the time slice
   */
  public static String parsePositionPattern(String filePattern, LoaderType loaderType,
                                            int startTile, int startRow, int startCol, boolean silent) {

    String posMatcher = null;
    switch (loaderType) {
      case ROWCOL:
        String rowMatcher = RowColTileGridLoader.getRowMatcher(filePattern, silent);
        if (rowMatcher != null) {
          String colFilePattern = String.format(rowMatcher, startRow);
          posMatcher = RowColTileGridLoader.getColMatcher(colFilePattern, silent);
        }
        if (posMatcher != null)
          return String.format(posMatcher, startCol);
        break;
      case SEQUENTIAL:
        posMatcher = SequentialTileGridLoader.getPositionPattern(filePattern, silent);
        if (posMatcher != null)
          return String.format(posMatcher, startTile);
        break;

    }
    return null;
  }

  /**
   * Checks if time slice exists given a file pattern
   *
   * @param imageDir    the image directory
   * @param filePattern the file pattern containing only the time pattern
   * @param timeSlice   the time slice to check
   * @param silent      whether to output errors or not
   * @return true if the file exists, otherwise false
   */
  public static boolean checkTimeSliceTile(String imageDir, String filePattern, int timeSlice,
                                           boolean silent) {
    String timeFileName = parseTimeSlicePattern(filePattern, timeSlice, silent);
    File file = new File(imageDir, timeFileName);
    return file.exists();
  }

  /**
   * Checks to see if the starting tile exists or not
   *
   * @param imageDir       the directory where the tile exists
   * @param filePattern    the file pattern to use
   * @param startTile      the starting tile number
   * @param startRow       the starting tile row number
   * @param startCol       the starting tile column number
   * @param startTimeSlice the starting tile timeslice
   * @param loaderType     the type of tile loader
   * @param silent         whether to output errors or not
   * @return true if the tile exists, otherwise false
   */
  public static boolean checkStartTile(String imageDir, String filePattern, int startTile, int startRow, int startCol,
                                       int startTimeSlice, LoaderType loaderType, boolean silent) {

    String timeFileName = parseTimeSlicePattern(filePattern, startTimeSlice, silent);

    return checkStartTile(imageDir, timeFileName, startTile, startRow, startCol, loaderType, silent);
  }

  /**
   * Checks to see if the starting tile exists or not
   *
   * @param imageDir    the directory where the tile exists
   * @param filePattern the file pattern to use
   * @param startTile   the starting tile number
   * @param startRow       the starting tile row number
   * @param startCol       the starting tile column number
   * @param loaderType  the type of tile grid loader
   * @param silent      whether to output errors or not
   * @return true if the tile exists, otherwise false
   */
  public static boolean checkStartTile(String imageDir, String filePattern, int startTile,
                                       int startRow, int startCol,
                                       LoaderType loaderType, boolean silent) {

    String fileName = parsePositionPattern(filePattern, loaderType, startTile, startRow, startCol, silent);

    if (fileName == null)
      return false;

    File file = new File(imageDir, fileName);

    if (file.exists())
      return true;

    if (!silent) {
      Log.msg(LogType.MANDATORY, "Could not find file: " + file.getAbsolutePath());
      Log.msg(LogType.MANDATORY, "Please check your image directory, "
          + "file pattern, and start tile to ensure they match the "
          + "files in your image directory.");
    }
    return false;
  }


  public static boolean checkRowColTile(String imageDir, String filePattern, int row, int col, boolean silent) {
    String posMatcher = null;
    String rowMatcher = RowColTileGridLoader.getRowMatcher(filePattern, silent);
    if (rowMatcher != null) {
      String colFilePattern = String.format(rowMatcher, row);
      posMatcher = RowColTileGridLoader.getColMatcher(colFilePattern, silent);
    }

    if (posMatcher == null)
      return false;

    String fileName = String.format(posMatcher, col);
    File file = new File(imageDir, fileName);

    if (file.exists())
      return true;

    if (!silent) {
      Log.msg(LogType.MANDATORY, "Could not find file: " + file.getAbsolutePath());
      Log.msg(LogType.MANDATORY, "Please check your image directory, "
          + "file pattern, and start tile to ensure they match the "
          + "files in your image directory.");
    }
    return false;
  }
}
