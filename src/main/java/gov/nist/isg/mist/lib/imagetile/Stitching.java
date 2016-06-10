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
// Date: Aug 1, 2013 4:11:44 PM EST
//
// Time-stamp: <Aug 1, 2013 4:11:44 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.imagetile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gov.nist.isg.mist.correlation.CorrelationUtils;
import gov.nist.isg.mist.lib.common.CorrelationTriple;
import gov.nist.isg.mist.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.lib.imagetile.fftw.FftwStitching;
import gov.nist.isg.mist.lib.imagetile.java.JavaImageTile;
import gov.nist.isg.mist.lib.imagetile.java.JavaStitching;
import gov.nist.isg.mist.lib.imagetile.jcuda.CudaImageTile;
import gov.nist.isg.mist.lib.imagetile.jcuda.CudaStitching;
import gov.nist.isg.mist.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.stitching.lib32.imagetile.fftw.FftwImageTile32;
import gov.nist.isg.mist.stitching.lib32.imagetile.fftw.FftwStitching32;
import gov.nist.isg.mist.stitching.lib32.imagetile.java.JavaImageTile32;
import gov.nist.isg.mist.stitching.lib32.imagetile.java.JavaStitching32;
import gov.nist.isg.mist.stitching.lib32.imagetile.jcuda.CudaImageTile32;
import gov.nist.isg.mist.stitching.lib32.imagetile.jcuda.CudaStitching32;
import jcuda.driver.CUstream;

/**
 * Utility functions for stitching image tiles.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class Stitching {


  /**
   * Whether to use BioFormats to read the images
   */
  public static boolean USE_BIOFORMATS = false;
  /**
   * The number of FFT peaks to check
   */
  public static int NUM_PEAKS = 2;


  /**
   * Defines the Normalized Cross Correlation search mechanism. Within the stage model repeatability
   * bound the normalized cross correlation is optimized. The optimization mechanism is defined
   * here.
   *
   * @author Michael Majurski
   */
  public static enum TranslationRefinementType {
    /**
     * Performs a single hill climb starting at the computed or estimated translation.
     */
    SINGLE_HILL_CLIMB("Single Hill Climb"),

    /**
     * Performs multiple hill climbs. The first starts from the computed or estimated translation .
     * The next n (defined by parameter) hill climbs start at randomly selected points within the
     * search bounds.
     */
    MULTI_POINT_HILL_CLIMB("Multipoint Hill Climb"),

    /**
     * Performs an exhaustive search of the stage model repeatability bounds to find the optimal
     * cross correlation offset.
     */
    EXHAUSTIVE("Exhaustive");

    private TranslationRefinementType(final String text) {
      this.text = text;
    }

    private final String text;


    @Override
    public String toString() {
      return this.text;
    }
  }


  /**
   * Defintes hill climbing direction using cartesian coordinates when observing a two dimensional
   * grid where the upper left corner is 0,0. Moving north -1 in the y-direction, south +1 in the
   * y-direction, west -1 in the x-direction, and east +1 in the x-direction.
   *
   * @author Tim Blattner
   * @version 1.0
   */
  enum HillClimbDirection {
    North(0, -1), South(0, 1), East(1, 0), West(-1, 0), NoMove(0, 0);

    private int xDir;
    private int yDir;

    HillClimbDirection(int x, int y) {
      this.xDir = x;
      this.yDir = y;
    }

    public int getXDir() {
      return this.xDir;
    }

    public int getYDir() {
      return this.yDir;
    }

  }

  /**
   * Computes the phase correlation between two images
   *
   * @param t1     the neighboring tile
   * @param t2     the current tile
   * @param memory the tile worker memory
   * @return the correlation triple between these two tiles
   */
  public static <T> CorrelationTriple phaseCorrelationImageAlignment(ImageTile<T> t1,
                                                                     ImageTile<T> t2,
                                                                     TileWorkerMemory memory) {

    if (t1 instanceof JavaImageTile)
      return JavaStitching.phaseCorrelationImageAlignment((JavaImageTile) t1, (JavaImageTile) t2,
          memory);
    else if (t1 instanceof FftwImageTile)
      return FftwStitching.phaseCorrelationImageAlignment((FftwImageTile) t1, (FftwImageTile) t2,
          memory);
    else if (t1 instanceof CudaImageTile)
      return CudaStitching.phaseCorrelationImageAlignment((CudaImageTile) t1, (CudaImageTile) t2,
          memory, null);
    else if (t1 instanceof JavaImageTile32)
      return JavaStitching32.phaseCorrelationImageAlignment((JavaImageTile32) t1, (JavaImageTile32) t2,
          memory);
    else if (t1 instanceof FftwImageTile32)
      return FftwStitching32.phaseCorrelationImageAlignment((FftwImageTile32) t1, (FftwImageTile32) t2,
          memory);
    else if (t1 instanceof CudaImageTile32)
      return CudaStitching32.phaseCorrelationImageAlignment((CudaImageTile32) t1, (CudaImageTile32) t2,
          memory, null);
    else
      return null;
  }


  /**
   * Computes the phase correlation between two images using Java
   *
   * @param t1     the neighboring tile
   * @param t2     the current tile
   * @param memory the tile worker memory
   * @return the correlation triple between these two tiles
   */
  public static CorrelationTriple phaseCorrelationImageAlignmentJava(JavaImageTile t1,
                                                                     JavaImageTile t2, TileWorkerMemory memory) {
    return JavaStitching.phaseCorrelationImageAlignment(t1, t2, memory);
  }


  /**
   * Computes the phase correlation between two images using FFTW
   *
   * @param t1     the neighboring tile
   * @param t2     the current tile
   * @param memory the tile worker memory
   * @return the correlation triple between these two tiles
   */
  public static CorrelationTriple phaseCorrelationImageAlignmentFftw(FftwImageTile t1,
                                                                     FftwImageTile t2, TileWorkerMemory memory) {
    return FftwStitching.phaseCorrelationImageAlignment(t1, t2, memory);
  }


  /**
   * Computes the phase correlation between two images using FFTW32
   *
   * @param t1     the neighboring tile
   * @param t2     the current tile
   * @param memory the tile worker memory
   * @return the correlation triple between these two tiles
   */
  public static CorrelationTriple phaseCorrelationImageAlignmentFftw(FftwImageTile32 t1,
                                                                     FftwImageTile32 t2,
                                                                     TileWorkerMemory memory) {
    return FftwStitching32.phaseCorrelationImageAlignment(t1, t2, memory);
  }


  /**
   * Computes the phase correlation between images using CUDA
   *
   * @param t1     the neighboring tile
   * @param t2     the current tile
   * @param memory the tile worker memory
   * @param stream the CUDA stream
   * @return the correlation triple between these two tiles
   */
  public static CorrelationTriple phaseCorrelationImageAlignmentCuda(CudaImageTile t1,
                                                                     CudaImageTile t2, TileWorkerMemory memory, CUstream stream) {
    return CudaStitching.phaseCorrelationImageAlignment(t1, t2, memory, stream);
  }

  /**
   * Computes the phase correlation between images using CUDA
   *
   * @param t1     the neighboring tile
   * @param t2     the current tile
   * @param memory the tile worker memory
   * @param stream the CUDA stream
   * @return the correlation triple between these two tiles
   */
  public static CorrelationTriple phaseCorrelationImageAlignmentCuda(CudaImageTile32 t1,
                                                                     CudaImageTile32 t2,
                                                                     TileWorkerMemory memory,
                                                                     CUstream stream) {
    return CudaStitching32.phaseCorrelationImageAlignment(t1, t2, memory, stream);
  }


  /**
   * Prints the absolute positions of all tiles in a grid. Requires logging level of helpful.
   *
   * @param grid the grid of tiles to print their absolute positions
   */
  public static <T> void printAbsolutePositions(TileGrid<ImageTile<T>> grid) {
    for (int r = 0; r < grid.getExtentHeight(); r++) {
      for (int c = 0; c < grid.getExtentWidth(); c++) {
        ImageTile<T> t = grid.getSubGridTile(r, c);

        Log.msg(LogType.HELPFUL, t.getFileName() + ": (" + t.getAbsXPos() + ", " + t.getAbsYPos()
            + ") corr: " + t.getTileCorrelation());

      }
    }
  }

  /**
   * Prints the relative displacements of all tiles in a grid. Requires logging level of helpful.
   *
   * @param grid the grid of tiles to print their relative displacements
   */
  public static <T> void printRelativeDisplacements(TileGrid<ImageTile<T>> grid) {
    for (int r = 0; r < grid.getExtentHeight(); r++) {
      for (int c = 0; c < grid.getExtentWidth(); c++) {
        ImageTile<T> t = grid.getSubGridTile(r, c);

        if (c > 0) {
          ImageTile<T> west = grid.getSubGridTile(r, c - 1);

          Log.msg(LogType.HELPFUL, " pciam_W(\"" + t.getFileName() + "\",\"" + west.getFileName()
              + "\"): " + t.getWestTranslation());

        }

        if (r > 0) {
          ImageTile<T> north = grid.getSubGridTile(r - 1, c);

          Log.msg(LogType.HELPFUL, " pciam_N(\"" + north.getFileName() + "\",\"" + t.getFileName()
              + "\"): " + t.getNorthTranslation());

        }
      }
    }
  }

  /**
   * Prints the absolute positions of all tiles in a grid. Requires logging level of helpful.
   *
   * @param grid the grid of tiles to print their absolute positions
   * @param file the file to save the absolute positions
   */
  public static <T> void outputAbsolutePositions(TileGrid<ImageTile<T>> grid, File file) {
    Log.msg(LogType.MANDATORY, "Writing global positions to: " + file.getAbsolutePath());

    try {
      String newLine = "\n";
      FileWriter writer = new FileWriter(file);

      for (int r = 0; r < grid.getExtentHeight(); r++) {
        for (int c = 0; c < grid.getExtentWidth(); c++) {
          ImageTile<T> t = grid.getSubGridTile(r, c);
          if (t.fileExists()) {
            writer.write("file: " + t.getFileName() + "; corr: " + t.getTileCorrelationStr()
                + "; position: (" + t.getAbsXPos() + ", " + t.getAbsYPos() + "); grid: ("
                + t.getCol() + ", " + t.getRow() + ");" + newLine);
          }
        }
      }

      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Parses an absolute displacement file into an TileGrid
   *
   * @param grid the grid of tiles
   * @param file the absolute position file
   * @return true if the parsing was successful, otherwise false
   */
  public static <T> boolean parseAbsolutePositions(TileGrid<ImageTile<T>> grid, File file) {
    // Read file line by line and parse as key-value pair
    boolean parseError = false;
    try {
      FileReader reader = new FileReader(file);

      BufferedReader br = new BufferedReader(reader);

      String line;

      String patternStr = "(\\S+): (\\S+|\\(\\S+, \\S+\\));";
      Pattern pattern = Pattern.compile(patternStr);


      while ((line = br.readLine()) != null) {
        String tileName = "";
        double corr = 0.0;
        int xPos = 0;
        int yPos = 0;
        int gridRow = 0;
        int gridCol = 0;
        Matcher matcher = pattern.matcher(line);
        if (!matcher.find()) {
          Log.msg(LogType.MANDATORY, "Error: unable to parse line: " + line);
          Log.msg(LogType.MANDATORY, "Error parsing absolute positions: " + file.getAbsolutePath());
          br.close();
          return false;
        }

        matcher.reset();
        while (matcher.find()) {
          if (matcher.groupCount() == 2) {
            String key = matcher.group(1);
            String value = matcher.group(2);

            if (key.equals("file")) {
              tileName = value;
            } else if (key.equals("corr")) {
              try {
                corr = Double.parseDouble(value);
              } catch (NumberFormatException e) {
                Log.msg(LogType.MANDATORY, "Unable to parse correlation for " + tileName);
                parseError = true;
              }
            } else if (key.equals("position")) {
              value = value.replace("(", "");
              value = value.replace(")", "");
              String[] posSplit = value.split(",");
              try {
                xPos = Integer.parseInt(posSplit[0].trim());
                yPos = Integer.parseInt(posSplit[1].trim());
              } catch (NumberFormatException e) {
                Log.msg(LogType.MANDATORY, "Unable to parse position for " + tileName);
                parseError = true;
              }
            } else if (key.equals("grid")) {
              value = value.replace("(", "");
              value = value.replace(")", "");
              String[] gridSplit = value.split(",");
              try {
                gridCol = Integer.parseInt(gridSplit[0].trim());
                gridRow = Integer.parseInt(gridSplit[1].trim());
              } catch (NumberFormatException e) {
                Log.msg(LogType.MANDATORY, "Unable to parse grid position for " + tileName);
                parseError = true;
              }
            } else {
              Log.msg(LogType.MANDATORY, "Error: Unknown key: " + key);
              parseError = true;
              break;
            }
          } else {
            Log.msg(LogType.MANDATORY, "Error: unable to parse line: " + line);
            parseError = true;
            break;
          }
        }

        if (parseError) {
          Log.msg(LogType.MANDATORY, "Error parsing absolute positions: " + file.getAbsolutePath());
          br.close();
          return false;
        }

        // Get the tile at grid position and set the necessary values
        if (grid != null && !parseError) {
          ImageTile<T> tile = grid.getTile(gridRow, gridCol);
          tile.setAbsXPos(xPos);
          tile.setAbsYPos(yPos);
          tile.setTileCorrelation(corr);
          // tile.set
        }

      }

      reader.close();
      br.close();
    } catch (FileNotFoundException e) {
      Log.msg(LogType.MANDATORY, "Unable to find file: " + file.getAbsolutePath());
    } catch (IOException e) {
      e.printStackTrace();
    }

    return true;
  }

  /**
   * Prints the relative displacements of all tiles in a grid. Requires logging level of helpful.
   *
   * @param grid the grid of tiles to print their relative displacements
   * @param file the file to output the relative displacements
   */
  public static <T> void outputRelativeDisplacements(TileGrid<ImageTile<T>> grid, File file) {
    Log.msg(LogType.MANDATORY, "Writing relative positions to: " + file.getAbsolutePath());
    try {
      String newLine = "\n";
      FileWriter writer = new FileWriter(file);

      for (int r = 0; r < grid.getExtentHeight(); r++) {
        for (int c = 0; c < grid.getExtentWidth(); c++) {
          ImageTile<T> t = grid.getSubGridTile(r, c);

          if (c > 0) {

            ImageTile<T> west = grid.getSubGridTile(r, c - 1);
            writer.write("west, " + t.getFileName() + ", " + west.getFileName() + ", "
                + t.getWestTranslation().toCSVString() + newLine);

          }

          if (r > 0) {
            ImageTile<T> north = grid.getSubGridTile(r - 1, c);
            writer.write("north, " + t.getFileName() + ", " + north.getFileName() + ", "
                + t.getNorthTranslation().toCSVString() + newLine);
          }
        }
      }

      writer.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Prints the relative displacements of all tiles in a grid. Requires logging level of helpful.
   *
   * @param grid the grid of tiles to print their relative displacements
   * @param file the file to output the relative displacements no optimization
   */
  public static <T> void outputRelativeDisplacementsNoOptimization(TileGrid<ImageTile<T>> grid,
                                                                   File file) {
    Log.msg(LogType.MANDATORY, "Writing relative positions " + "(no optimization) to: " + file.getAbsolutePath());
    try {
      String newLine = "\n";
      FileWriter writer = new FileWriter(file);

      for (int r = 0; r < grid.getExtentHeight(); r++) {
        for (int c = 0; c < grid.getExtentWidth(); c++) {
          ImageTile<T> t = grid.getSubGridTile(r, c);

          if (c > 0) {
            ImageTile<T> west = grid.getSubGridTile(r, c - 1);
            writer.write("west, " + t.getFileName() + ", " + west.getFileName() + ", "
                + t.getPreOptimizationWestTranslation().toCSVString() + newLine);
          }

          if (r > 0) {
            ImageTile<T> north = grid.getSubGridTile(r - 1, c);
            writer.write("north, " + t.getFileName() + ", " + north.getFileName() + ", "
                + t.getPreOptimizationNorthTranslation().toCSVString() + newLine);
          }
        }
      }

      writer.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Complex the peak cross correlation (up/down) between two images. Given an x,y position, we
   * analyze the 4 possible positions relative to each other: { {y, x}, {y, w - x}, {h - y, x}, {h -
   * y, w - x}};
   *
   * @param t1 image 1 (neighbor)
   * @param t2 image 2 (current)
   * @param x  the x max position
   * @param y  the y max position
   * @return the relative displacement along the x and y axis and the correlation
   */
  public static CorrelationTriple peakCrossCorrelationUD(ImageTile<?> t1, ImageTile<?> t2, int x,
                                                         int y) {
    int w = t1.getWidth();
    int h = t1.getHeight();
    List<CorrelationTriple> corrList = new ArrayList<CorrelationTriple>();

    // a given correlation triple between two images can have multiple interpretations
    // In the general case the translation from t1 to t2 can be any (x,y) so long as the two
    // images overlap. Therefore, given an input (x,y) where x and y are positive by definition
    // of the translation, we need to check 16 possible translations to find the correct
    // interpretation of the translation offset magnitude (x,y). The general case of 16
    // translations arise from the four Fourier transform possibilities, [(x, y); (x, H-y); (W-x,
    // y); (W-x,H-y)] and the four direction possibilities (+-x, +-y) = [(x,y); (x,-y); (-x,y);
    // (-x,-y)].
    // Because we know t1 and t2 form an up down pair, we can limit this search to the 8
    // possible combinations by only considering (+-x,y).
    int[][] dims = {{y, x}, {y, w - x}, {h - y, x}, {h - y, w - x},
        {y, (-x)}, {y, -(w - x)}, {h - y, (-x)}, {h - y, -(w - x)}};


    for (int i = 0; i < dims.length; i++) {
      int nr = dims[i][0];
      int nc = dims[i][1];

      double peak = CorrelationUtils.computeCrossCorrelation(t1, t2, nc, nr);
      if (Double.isNaN(peak) || Double.isInfinite(peak)) {
        peak = -1.0;
      }

      corrList.add(new CorrelationTriple(peak, nc, nr));
    }


    if (corrList.size() == 0)
      return new CorrelationTriple(Double.NEGATIVE_INFINITY, 0, 0);

    return Collections.max(corrList);
  }


  /**
   * Compute the peak cross correlation (left/right) between two images
   *
   * @param t1 image 1 (neighbor)
   * @param t2 image 2 (current)
   * @param x  the x max position
   * @param y  the y max position
   * @return the relative displacement along the x and y axis and the correlation
   */
  public static CorrelationTriple peakCrossCorrelationLR(ImageTile<?> t1, ImageTile<?> t2, int x,
                                                         int y) {
    int w = t1.getWidth();
    int h = t1.getHeight();
    List<CorrelationTriple> corrList = new ArrayList<CorrelationTriple>();

    // a given correlation triple between two images can have multiple interpretations
    // In the general case the translation from t1 to t2 can be any (x,y) so long as the two
    // images overlap. Therefore, given an input (x,y) where x and y are positive by definition
    // of the translation, we need to check 16 possible translations to find the correct
    // interpretation of the translation offset magnitude (x,y). The general case of 16
    // translations arise from the four Fourier transform possibilities, [(x, y); (x, H-y); (W-x,
    // y); (W-x,H-y)] and the four direction possibilities (+-x, +-y) = [(x,y); (x,-y); (-x,y);
    // (-x,-y)].
    // Because we know t1 and t2 form a left right pair, we can limit this search to the 8
    // possible combinations by only considering (x,+-y).
    int[][] dims = {{y, x}, {y, w - x}, {h - y, x}, {h - y, w - x},
        {(-y), x}, {(-y), w - x}, {-(h - y), x}, {-(h - y), w - x}};

    for (int i = 0; i < dims.length; i++) {
      int nr = dims[i][0];
      int nc = dims[i][1];

      double peak = CorrelationUtils.computeCrossCorrelation(t1, t2, nc, nr);
      if (Double.isNaN(peak) || Double.isInfinite(peak))
        peak = -1.0;

      corrList.add(new CorrelationTriple(peak, nc, nr));
    }

    if (corrList.size() == 0)
      return new CorrelationTriple(Double.NEGATIVE_INFINITY, 0, 0);

    return Collections.max(corrList);
  }


  /**
   * Computes cross correlation search with hill climbing
   *
   * @param minBoundX min x boundary
   * @param maxBoundX max x boundary
   * @param minBoundY min y boundary
   * @param maxBoundY max y bounadary
   * @param startX    start x position for hill climb
   * @param startY    start y position for hill climb
   * @param i1        the first image for CCF computation (north/west neighbor)
   * @param i2        the second image for CCF computation (current)
   * @param cache     2D array of doubles holding computed ncc values
   * @return the highest correlation triple within the bounding box using hill climbing
   */
  private static CorrelationTriple computeCCF_HillClimbingWorker(int minBoundX, int maxBoundX,
                                                                 int minBoundY, int maxBoundY,
                                                                 int startX, int startY,
                                                                 ImageTile<?> i1, ImageTile<?> i2,
                                                                 double[][] cache) {

    int curX = startX;
    int curY = startY;
    double curPeak = Double.NaN;

    // create array of peaks +1 for inclusive
    int yLength = maxBoundY - minBoundY + 1;
    int xLength = maxBoundX - minBoundX + 1;

    if (cache == null)
      cache = new double[yLength][xLength];

    // Compute hill climbing
    boolean foundPeak = false;
    while (!foundPeak) {

      // translate to 0-based index coordinates
      int curYIndex = curY - minBoundY;
      int curXIndex = curX - minBoundX;

      // check current
      if (Double.isNaN(curPeak)) {
        curPeak = CorrelationUtils.computeCrossCorrelation(i1, i2, curX, curY);
        cache[curYIndex][curXIndex] = curPeak;
      }

      HillClimbDirection direction = HillClimbDirection.NoMove;

      // Check each direction and move based on highest correlation
      for (HillClimbDirection dir : HillClimbDirection.values()) {
        // Skip NoMove direction
        if (dir == HillClimbDirection.NoMove)
          continue;

        double peak;

        // Check if moving dir is in bounds
        if (curY + dir.getYDir() >= minBoundY && curY + dir.getYDir() <= maxBoundY
            && curX + dir.getXDir() >= minBoundX && curX + dir.getXDir() <= maxBoundX) {

          // Check if we have already computed the peak at dir
          if (cache[curYIndex + dir.getYDir()][curXIndex + dir.getXDir()] == 0.0) {
            peak = CorrelationUtils.computeCrossCorrelation(i1, i2, curX + dir.getXDir(), curY + dir.getYDir());
            cache[curYIndex + dir.getYDir()][curXIndex + dir.getXDir()] = peak;
          } else {
            peak = cache[curYIndex + dir.getYDir()][curXIndex + dir.getXDir()];
          }

          // check if dir gives us the best peak
          if (peak > curPeak) {
            curPeak = peak;
            direction = dir;
          }
        }
      }

      // if the direction did not move, then we are done
      if (direction == HillClimbDirection.NoMove) {
        foundPeak = true;
      } else {
        curX += direction.getXDir();
        curY += direction.getYDir();
      }
    }

    if (Double.isInfinite(curPeak)) {
      curX = minBoundX;
      curY = minBoundY;
      curPeak = -1.0;
    }

    return new CorrelationTriple(curPeak, curX, curY);
  }


  /**
   * Computes cross correlation search with hill climbing
   *
   * @param minBoundX min x boundary
   * @param maxBoundX max x boundary
   * @param minBoundY min y boundary
   * @param maxBoundY max y bounadary
   * @param startX    start x position for hill climb
   * @param startY    start y position for hill climb
   * @param i1        the first image for CCF computation (north/west neighbor)
   * @param i2        the second image for CCF computation (current)
   * @return the highest correlation triple within the bounding box using hill climbing
   */
  public static CorrelationTriple computeCCF_HillClimbing(int minBoundX, int maxBoundX,
                                                          int minBoundY, int maxBoundY,
                                                          int startX, int startY,
                                                          ImageTile<?> i1, ImageTile<?> i2) {
    int width = i1.getWidth();
    int height = i1.getHeight();
    minBoundY = clampToValidBounds(minBoundY, height);
    maxBoundY = clampToValidBounds(maxBoundY, height);
    minBoundX = clampToValidBounds(minBoundX, width);
    maxBoundX = clampToValidBounds(maxBoundX, width);

    // create array of peaks +1 for inclusive
    int yLength = maxBoundY - minBoundY + 1;
    int xLength = maxBoundX - minBoundX + 1;

    double[][] cache = new double[yLength][xLength];

    CorrelationTriple triple = Stitching.computeCCF_HillClimbingWorker(minBoundX, maxBoundX,
        minBoundY, maxBoundY, startX, startY, i1, i2, cache);

    return triple;
  }


  /**
   * Computes cross correlation search with hill climbing
   *
   * @param minBoundX      min x boundary
   * @param maxBoundX      max x boundary
   * @param minBoundY      min y boundary
   * @param maxBoundY      max y bounadary
   * @param startX         start x position for hill climb
   * @param startY         start y position for hill climb
   * @param numStartPoints the number of random starting points to check
   * @param i1             the first image for CCF computation (north/west neighbor)
   * @param i2             the second image for CCF computation (current)
   * @return the highest correlation triple within the bounding box using hill climbing
   */
  public static CorrelationTriple computeCCF_MultiPoint_HillClimbing(int minBoundX, int maxBoundX,
                                                                     int minBoundY, int maxBoundY,
                                                                     int startX, int startY,
                                                                     int numStartPoints,
                                                                     ImageTile<?> i1, ImageTile<?> i2) {
    int width = i1.getWidth();
    int height = i1.getHeight();
    minBoundY = clampToValidBounds(minBoundY, height);
    maxBoundY = clampToValidBounds(maxBoundY, height);
    minBoundX = clampToValidBounds(minBoundX, width);
    maxBoundX = clampToValidBounds(maxBoundX, width);

    // create array of peaks +1 for inclusive
    int yLength = maxBoundY - minBoundY + 1;
    int xLength = maxBoundX - minBoundX + 1;
    double[][] cache = new double[yLength][xLength];

    int rangeX = Math.abs(maxBoundX - minBoundX);
    int rangeY = Math.abs(maxBoundY - minBoundY);
    List<CorrelationTriple> results = new ArrayList<CorrelationTriple>();

    // evaluate the starting point hill climb
    CorrelationTriple triple = Stitching.computeCCF_HillClimbingWorker(minBoundX, maxBoundX,
        minBoundY, maxBoundY, startX, startY, i1, i2, cache);
    results.add(triple);

    // perform the random starting point multipoint hill climbing
    for (int i = 0; i < numStartPoints; i++) {
      int curStartX = (int) Math.round(Math.random() * rangeX + Math.min(minBoundX, maxBoundX));
      int curStartY = (int) Math.round(Math.random() * rangeY + Math.min(minBoundY, maxBoundY));

      triple = Stitching.computeCCF_HillClimbingWorker(minBoundX, maxBoundX,
          minBoundY, maxBoundY, curStartX, curStartY, i1, i2, cache);
      results.add(triple);
    }


    // find the best correlation and translation from the hill climb ending points
    CorrelationTriple bestTriple = new CorrelationTriple(-1.0, startX, startY);
    for (CorrelationTriple t : results) {
      if (t.getCorrelation() > bestTriple.getCorrelation())
        bestTriple = t;
    }

    // determine how many converged
    int numConverged = 0;
    for (CorrelationTriple t : results) {
      if (t.getX() == bestTriple.getX() && t.getY() == bestTriple.getY())
        numConverged++;
    }

    Log.msg(LogType.INFO, "Translation HIll Climb (" + i1.getFileName() + "," + i2.getFileName() +
        ") had " + numConverged + "/" + results.size() +
        " hill climbs converge with best corr: " + bestTriple.getCorrelation());

    return bestTriple;
  }


  /**
   * Computes cross correlation search with hill climbing
   *
   * @param minBoundX min x boundary
   * @param maxBoundX max x boundary
   * @param minBoundY min y boundary
   * @param maxBoundY max y boundary
   * @param startX    start x position for hill climb
   * @param startY    start y position for hill climb
   * @param i1        the first image for CCF computation (north/west neighbor)
   * @param i2        the second image for CCF computation (current)
   * @return the highest correlation triple within the bounding box using hill climbing
   */
  public static CorrelationTriple computeCCF_Exhaustive(int minBoundX, int maxBoundX,
                                                        int minBoundY, int maxBoundY,
                                                        int startX, int startY,
                                                        ImageTile<?> i1, ImageTile<?> i2) {
    int width = i1.getWidth();
    int height = i1.getHeight();

    int maxX = startX;
    int maxY = startY;
    double curPeak = Double.NaN;
    double maxPeak = Double.NEGATIVE_INFINITY;

    minBoundY = clampToValidBounds(minBoundY, height);
    maxBoundY = clampToValidBounds(maxBoundY, height);
    minBoundX = clampToValidBounds(minBoundX, width);
    maxBoundX = clampToValidBounds(maxBoundX, width);

    for (int curX = minBoundX; curX <= maxBoundX; curX++) {
      for (int curY = minBoundY; curY <= maxBoundY; curY++) {

        curPeak = CorrelationUtils.computeCrossCorrelation(i1, i2, curX, curY);
        if (curPeak >= maxPeak) {
          maxPeak = curPeak;
          maxX = curX;
          maxY = curY;
        }
      }
    }

    if (Double.isNaN(maxPeak) || Double.isInfinite(curPeak)) {
      maxX = startX;
      maxY = startY;
      maxPeak = -1.0;
    }

    return new CorrelationTriple(maxPeak, maxX, maxY);
  }


  private static int clampToValidBounds(int val, int dimSize) {
    val = Math.max(val, -(dimSize - 1));
    val = Math.min(val, dimSize - 1);
    return val;
  }


}
