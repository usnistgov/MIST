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
// Date: Aug 1, 2013 4:11:44 PM EST
//
// Time-stamp: <Aug 1, 2013 4:11:44 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.imagetile;

import gov.nist.isg.mist.stitching.gui.StitchingGuiUtils;
import gov.nist.isg.mist.stitching.lib.common.Array2DView;
import gov.nist.isg.mist.stitching.lib.common.CorrelationTriple;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.fftw.FftwStitching;
import gov.nist.isg.mist.stitching.lib.imagetile.java.JavaImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.java.JavaStitching;
import gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.jcuda.CudaStitching;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.CudaTileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.FftwTileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.JavaTileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverser;
import jcuda.Sizeof;
import jcuda.driver.*;
import gov.nist.isg.mist.stitching.lib.memorypool.CudaAllocator;
import gov.nist.isg.mist.stitching.lib.memorypool.DynamicMemoryPool;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;

/**
 * Utility functions for stitching image tiles.
 * 
 * @author Tim Blattner
 * @version 1.0
 */
public class Stitching {

  /**
   * Whether to use hillclimbing or not
   */
  public static boolean USE_HILLCLIMBING = true;

  /**
   * The number of FFT peaks to check
   */
  public static int NUM_PEAKS = 2;

  /**
   * The correlation threshold for checking the number of peaks
   */
  @Deprecated
  public static final double CORR_THRESHOLD = 0.9;

  /**
   * Defintes hill climbing direction using cartesian coordinates when observering a two dimensional
   * grid where the upper left corner is 0,0. Moving north -1 in the y-direction, south +1 in the
   * y-direction, west -1 in the x-direction, and east +1 in the x-direction.
   * 
   * @author Tim Blattner
   * @version 1.0
   * 
   */
  enum HillClimbDirection {
    North(0, -1), South(0, 1), East(1, 0), West(-1, 0), NorthEast(1, -1), NorthWest(-1, -1), SouthEast(
        1, 1), SouthWest(-1, 1), NoMove(0, 0);

    private int xDir;
    private int yDir;

    private HillClimbDirection(int x, int y) {
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
   * @param t1 the neighboring tile
   * @param t2 the current tile
   * @param memory the tile worker memory
   * @return the correlation triple between these two tiles
   */
  public static <T> CorrelationTriple phaseCorrelationImageAlignment(ImageTile<T> t1,
      ImageTile<T> t2, TileWorkerMemory memory) throws FileNotFoundException {

    if (t1 instanceof JavaImageTile)
      return JavaStitching.phaseCorrelationImageAlignment((JavaImageTile) t1, (JavaImageTile) t2,
          memory);
    else if (t1 instanceof FftwImageTile)
      return FftwStitching.phaseCorrelationImageAlignment((FftwImageTile) t1, (FftwImageTile) t2,
          memory);
    else if (t1 instanceof CudaImageTile)
      return CudaStitching.phaseCorrelationImageAlignment((CudaImageTile) t1, (CudaImageTile) t2,
          memory, null);
    else
      return null;
  }


  /**
   * Computes the phase correlation between two images using Java
   * 
   * @param t1 the neighboring tile
   * @param t2 the current tile
   * @param memory the tile worker memory
   * @return the correlation triple between these two tiles
   */
  public static CorrelationTriple phaseCorrelationImageAlignmentJava(JavaImageTile t1,
      JavaImageTile t2, TileWorkerMemory memory) throws FileNotFoundException {
    return JavaStitching.phaseCorrelationImageAlignment(t1, t2,
        memory);
  }

  /**
   * Computes the phase correlation between two images using FFTW
   * 
   * @param t1 the neighboring tile
   * @param t2 the current tile
   * @param memory the tile worker memory
   * @return the correlation triple between these two tiles
   */
  public static CorrelationTriple phaseCorrelationImageAlignmentFftw(FftwImageTile t1,
      FftwImageTile t2, TileWorkerMemory memory) throws FileNotFoundException {
    return FftwStitching.phaseCorrelationImageAlignment(t1, t2, memory);
  }

  /**
   * Computes the phase correlation between images using CUDA
   * 
   * @param t1 the neighboring tile
   * @param t2 the current tile
   * @param memory the tile worker memory
   * @param stream the CUDA stream
   * @return the correlation triple between these two tiles
   */
  public static CorrelationTriple phaseCorrelationImageAlignmentCuda(CudaImageTile t1,
      CudaImageTile t2, TileWorkerMemory memory, CUstream stream) throws FileNotFoundException {
    return CudaStitching.phaseCorrelationImageAlignment(t1, t2, memory, stream);
  }

  /**
   * Stitchings a grid of tiles using a traverser
   *
   * @param <T>
   * @param traverser the traverser on how to traverse the grid
   * @param grid the grid of tiles to stitch
   */
  public static <T> void stitchGridJava(TileGridTraverser<ImageTile<T>> traverser,
                                        TileGrid<ImageTile<T>> grid) throws FileNotFoundException
  {
    Stitching.stitchGridJava(traverser, grid, null);
  }

  /**
   * Stitchings a grid of tiles using a traverser
   * 
   * @param <T>
   * @param traverser the traverser on how to traverse the grid
   * @param grid the grid of tiles to stitch
   * @param progressBar the progressBar (or null to ignore)
   */
  public static <T> void stitchGridJava(TileGridTraverser<ImageTile<T>> traverser,
      TileGrid<ImageTile<T>> grid, JProgressBar progressBar) throws FileNotFoundException {
    TileWorkerMemory memory = null;
    for (ImageTile<T> t : traverser) {
      t.setThreadID(0);


      if (!t.isTileRead())
        t.readTile();

      if (memory == null) {
        memory = new JavaTileWorkerMemory(t);
      }
      int row = t.getRow();
      int col = t.getCol();

      t.computeFft();

      if (col > grid.getStartCol()) {

        ImageTile<T> west = grid.getTile(row, col - 1);
        t.setWestTranslation(Stitching.phaseCorrelationImageAlignment(west, t, memory));

        Log.msgNoTime(
            LogType.HELPFUL,
            " pciam_W(\"" + t.getFileName() + "\",\"" + west.getFileName() + "\"): "
            + t.getWestTranslation());


        west.releaseFftMemory();
        west.releasePixels();

        if (progressBar != null)
          StitchingGuiUtils.incrementProgressBar(progressBar);

      }

      if (row > grid.getStartRow()) {
        ImageTile<T> north = grid.getTile(row - 1, col);

        t.setNorthTranslation(Stitching.phaseCorrelationImageAlignment(north, t, memory));

        Log.msgNoTime(
            LogType.HELPFUL,
            " pciam_N(\"" + north.getFileName() + "\",\"" + t.getFileName() + "\"): "
            + t.getNorthTranslation());


        north.releaseFftMemory();
        north.releasePixels();

        if (progressBar != null)
          StitchingGuiUtils.incrementProgressBar(progressBar);
      }

      t.releaseFftMemory();
      t.releasePixels();

    }

  }

  /**
   * Stitching a grid of tiles using a traverser
   * 
   * @param <T>
   * @param traverser the traverser on how to traverse the grid
   * @param grid the grid of tiles to stitch
   */
  public static <T> void stitchGridFftw(TileGridTraverser<ImageTile<T>> traverser,
      TileGrid<ImageTile<T>> grid) throws FileNotFoundException {
    TileWorkerMemory memory = null;
    for (ImageTile<?> t : traverser) {
      t.setThreadID(0);

      if (!t.isTileRead())
        t.readTile();

      if (memory == null) {
        memory = new FftwTileWorkerMemory(t);
      }
      int row = t.getRow();
      int col = t.getCol();

      t.computeFft();

      if (col > grid.getStartCol()) {
        ImageTile<?> west = grid.getTile(row, col - 1);
        t.setWestTranslation(Stitching.phaseCorrelationImageAlignmentFftw((FftwImageTile) west,
                                                                          (FftwImageTile) t,
                                                                          memory));

        Log.msgNoTime(
            LogType.HELPFUL,
            " pciam_W(\"" + t.getFileName() + "\",\"" + west.getFileName() + "\"): "
            + t.getWestTranslation());

        t.decrementFftReleaseCount();
        west.decrementFftReleaseCount();

        if (t.getFftReleaseCount() == 0)
          t.releaseFftMemory();

        if (west.getFftReleaseCount() == 0)
          west.releaseFftMemory();

      }

      if (row > grid.getStartRow()) {
        ImageTile<?> north = grid.getTile(row - 1, col);

        t.setNorthTranslation(Stitching.phaseCorrelationImageAlignmentFftw((FftwImageTile) north,
            (FftwImageTile) t, memory));

        Log.msgNoTime(
            LogType.HELPFUL,
            " pciam_N(\"" + north.getFileName() + "\",\"" + t.getFileName() + "\"): "
                + t.getNorthTranslation());

        t.decrementFftReleaseCount();
        north.decrementFftReleaseCount();

        if (t.getFftReleaseCount() == 0)
          t.releaseFftMemory();

        if (north.getFftReleaseCount() == 0)
          north.releaseFftMemory();

      }
    }

  }

  /**
   * Stitching a grid of tiles using a traverser
   * 
   * @param traverser the traverser on how to traverse the grid
   * @param grid the grid of tiles to stitch
   * @param context the GPU context
   */
  public static void stitchGridCuda(TileGridTraverser<ImageTile<CUdeviceptr>> traverser,
      TileGrid<ImageTile<CUdeviceptr>> grid, CUcontext context) throws FileNotFoundException {
    TileWorkerMemory memory = null;
    DynamicMemoryPool<CUdeviceptr> memoryPool = null;

    JCudaDriver.cuCtxSetCurrent(context);

    int dev = 0;
    CUstream stream = new CUstream();
    JCudaDriver.cuStreamCreate(stream, CUstream_flags.CU_STREAM_DEFAULT);

    CudaImageTile.bindBwdPlanToStream(stream, dev);
    CudaImageTile.bindFwdPlanToStream(stream, dev);

    double pWidth = grid.getExtentWidth();
    double pHeight = grid.getExtentHeight();
    int memoryPoolSize = (int) Math.ceil(Math.sqrt(pWidth * pWidth + pHeight * pHeight)) + 20;

    for (ImageTile<CUdeviceptr> t : traverser) {
      t.setDev(dev);
      t.setThreadID(0);

      if (!t.isTileRead())
        t.readTile();

      if (memoryPool == null) {
        int[] size = {CudaImageTile.fftSize * Sizeof.DOUBLE * 2};

        memoryPool =
            new DynamicMemoryPool<CUdeviceptr>(memoryPoolSize, false, new CudaAllocator(), size);
      }

      if (memory == null)
        memory = new CudaTileWorkerMemory(t);

      int row = t.getRow();
      int col = t.getCol();

      t.allocateFftMemory(memoryPool);

      t.computeFft(memoryPool, memory, stream);

      if (col > grid.getStartCol()) {
        ImageTile<CUdeviceptr> west = grid.getTile(row, col - 1);
        t.setWestTranslation(Stitching.phaseCorrelationImageAlignmentCuda((CudaImageTile) west,
            (CudaImageTile) t, memory, stream));

        Log.msg(LogType.HELPFUL, " pciam_W(\"" + t.getFileName() + "\",\"" + west.getFileName()
            + "\"): " + t.getWestTranslation());

        t.decrementFftReleaseCount();
        west.decrementFftReleaseCount();

        if (west.getFftReleaseCount() == 0) {
          west.releaseFftMemory(memoryPool);
        }

      }

      if (row > grid.getStartRow()) {
        ImageTile<CUdeviceptr> north = grid.getTile(row - 1, col);

        t.setNorthTranslation(Stitching.phaseCorrelationImageAlignmentCuda((CudaImageTile) north,
            (CudaImageTile) t, memory, stream));

        Log.msg(LogType.HELPFUL, " pciam_N(\"" + north.getFileName() + "\",\"" + t.getFileName()
            + "\"): " + t.getNorthTranslation());

        t.decrementFftReleaseCount();
        north.decrementFftReleaseCount();

        if (north.getFftReleaseCount() == 0) {

          north.releaseFftMemory(memoryPool);

        }

      }

      if (t.getFftReleaseCount() == 0) {
        t.releaseFftMemory(memoryPool);
      }
    }

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

          writer.write("file: " + t.getFileName() + "; corr: " + t.getTileCorrelationStr()
              + "; position: (" + t.getAbsXPos() + ", " + t.getAbsYPos() + "); grid: ("
              + t.getCol() + ", " + t.getRow() + ");" + newLine);
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
        if (!matcher.find())
        {
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
            }
            else {
              Log.msg(LogType.MANDATORY, "Error: Unknown key: " + key);
              parseError = true;
              break;
            }
          }
          else
          {
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
   * analyze the 4 possible positions relative to eachother: { {y, x}, {y, w - x}, {h - y, x}, {h -
   * y, w - x}};
   * 
   * 
   * @param t1 image 1 (neighbor)
   * @param t2 image 2 (current)
   * @param x the x max position
   * @param y the y max position
   * @return the relative displacement along the x and y axis and the correlation
   */
  public static CorrelationTriple peakCrossCorrelationUD(ImageTile<?> t1, ImageTile<?> t2, int x,
      int y) {
    int w = t1.getWidth();
    int h = t1.getHeight();
    List<CorrelationTriple> corrList = new ArrayList<CorrelationTriple>();

    int[][] dims = { {y, x}, {y, w - x}, {h - y, x}, {h - y, w - x}};

    for (int i = 0; i < 4; i++) {
      int nr = dims[i][0];
      int nc = dims[i][1];

      Array2DView a1 = new Array2DView(t1, nr, h - nr, nc, w - nc);
      Array2DView a2 = new Array2DView(t2, 0, h - nr, 0, w - nc);

      double peak = crossCorrelation(a1, a2);

      if (Double.isNaN(peak) || Double.isInfinite(peak)) {
        peak = -1.0;
      }

      corrList.add(new CorrelationTriple(peak, nc, nr));
    }

    for (int i = 0; i < 4; i++) {
      int nr = dims[i][0];
      int nc = dims[i][1];

      Array2DView a1 = new Array2DView(t1, nr, h - nr, 0, w - nc);
      Array2DView a2 = new Array2DView(t2, 0, h - nr, nc, w - nc);

      double peak = crossCorrelation(a1, a2);

      if (Double.isNaN(peak) || Double.isInfinite(peak)) {
        peak = -1.0;
      }

      corrList.add(new CorrelationTriple(peak, -nc, nr));

    }

    if (corrList.size() == 0)
      return new CorrelationTriple(Double.NEGATIVE_INFINITY, 0, 0);

    return Collections.max(corrList);
  }

  /**
   * Wrapper that computes the up/down CCF at a given x and y location between two image tile's
   * 
   * @param x the x location
   * @param y the y location
   * @param i1 the first image tile (north/west neighbor)
   * @param i2 the second image tile (current)
   * @return the CorrelationTriple from position x,y
   */
  public static CorrelationTriple computeCCF_UD(int x, int y, ImageTile<?> i1, ImageTile<?> i2) {
    return computeCCF_UD(x, x, y, y, i1, i2);
  }

  /**
   * Computes the up/down CCF values inside a bounding box, returning the best CCF value (one with
   * the highest correlation)
   * 
   * @param minBoundX the minimum x value of the bounding box
   * @param maxBoundX the maximum x value of the bounding box
   * @param minBoundY the minimum y value of the bounding box
   * @param maxBoundY the maximum y value of the bounding box
   * @param i1 the first image for CCF computation (north/west neighbor)
   * @param i2 the second image for CCF computation (current)
   * @return the highest CorrelationTriple within the bounding box
   */
  public static CorrelationTriple computeCCF_UD(int minBoundX, int maxBoundX, int minBoundY,
      int maxBoundY, ImageTile<?> i1, ImageTile<?> i2) {
    int width = i1.getWidth();
    int height = i1.getHeight();

    double maxPeak = Double.NEGATIVE_INFINITY;
    int x = minBoundX;
    int y = minBoundY;

    for (int i = minBoundY; i <= maxBoundY; i++) {
      for (int j = minBoundX; j <= maxBoundX; j++) {
        Array2DView a1, a2;
        double peak;

        if (j >= 0) {
          a1 = new Array2DView(i1, i, height - i, j, width - j);
          a2 = new Array2DView(i2, 0, height - i, 0, width - j);
        } else {
          a1 = new Array2DView(i1, i, height - i, 0, width + j);
          a2 = new Array2DView(i2, 0, height - i, -j, width + j);
        }

        peak = Stitching.crossCorrelation(a1, a2);
        if (peak > maxPeak) {
          x = j;
          y = i;
          maxPeak = peak;
        }
      }
    }

    if (Double.isInfinite(maxPeak)) {
      x = minBoundX;
      y = minBoundY;
      maxPeak = -1.0;
    }

    return new CorrelationTriple(maxPeak, x, y);
  }

  /**
   * Computes the up/down CCF values inside a bounding box, returning the best CCF value (one with
   * the highest correlation)
   * 
   * @param minBoundX the minimum x value of the bounding box
   * @param maxBoundX the maximum x value of the bounding box
   * @param minBoundY the minimum y value of the bounding box
   * @param maxBoundY the maximum y value of the bounding box
   * @param i1 the first image for CCF computation (north/west neighbor)
   * @param i2 the second image for CCF computation (current)
   * @param fileStr the file string to save the CCF
   * @return the highest correlation triple within the bounding box
   */
  public static CorrelationTriple computeCCF_UDAndSave(int minBoundX, int maxBoundX, int minBoundY,
      int maxBoundY, ImageTile<?> i1, ImageTile<?> i2, String fileStr) {

    File file = new File(fileStr);
    int width = i1.getWidth();
    int height = i1.getHeight();

    double maxPeak = Double.NEGATIVE_INFINITY;
    int x = minBoundX;
    int y = minBoundY;

    try {
      FileWriter writer = new FileWriter(file);

      for (int i = minBoundY; i <= maxBoundY; i++) {
        for (int j = minBoundX; j <= maxBoundX; j++) {
          Array2DView a1, a2;
          double peak;

          if (j >= 0) {
            a1 = new Array2DView(i1, i, height - i, j, width - j);
            a2 = new Array2DView(i2, 0, height - i, 0, width - j);
          } else {
            a1 = new Array2DView(i1, i, height - i, 0, width + j);
            a2 = new Array2DView(i2, 0, height - i, -j, width + j);
          }

          peak = Stitching.crossCorrelation(a1, a2);
          writer.write(peak + ",");
          if (peak > maxPeak) {
            x = j;
            y = i;
            maxPeak = peak;
          }

        }
        writer.write("\n");
      }

      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (Double.isInfinite(maxPeak)) {
      x = minBoundX;
      y = minBoundY;
      maxPeak = -1.0;
    }

    return new CorrelationTriple(maxPeak, x, y);
  }

  /**
   * Computes cross correlation search with hill climbing (up-down)
   * 
   * @param minBoundX min x boundary
   * @param maxBoundX max x boundary
   * @param minBoundY min y boundary
   * @param maxBoundY max y bounadary
   * @param startX start x position for hill climb
   * @param startY start y position for hill climb
   * @param i1 the first image for CCF computation (north/west neighbor)
   * @param i2 the second image for CCF computation (current)
   * @return the highest correlation triple within the bounding box using hill climbing
   */
  public static <T> CorrelationTriple computeCCF_HillClimbing_UD(int minBoundX, int maxBoundX,
      int minBoundY, int maxBoundY, int startX, int startY, ImageTile<T> i1, ImageTile<T> i2) {
    int width = i1.getWidth();
    int height = i1.getHeight();

    int curX = startX;
    int curY = startY;
    double curPeak = Double.NaN;


    minBoundY = Math.max(minBoundY, 0);
    minBoundY = Math.min(minBoundY, height);

    maxBoundY = Math.max(maxBoundY, 0);
    maxBoundY = Math.min(maxBoundY, height);

    minBoundX = Math.max(minBoundX, -width);
    minBoundX = Math.min(minBoundX, width);

    maxBoundX = Math.max(maxBoundX, -width);
    maxBoundX = Math.min(maxBoundX, width);



    // create array of peaks +1 for inclusive, +2 for each end
    int yLength = maxBoundY - minBoundY + 1 + 2;
    int xLength = maxBoundX - minBoundX + 1 + 2;

    double[][] peaks = new double[yLength][xLength];

    boolean foundPeak = false;

    // Compute hill climbing
    while (!foundPeak
        && ((curX <= maxBoundX && curX >= minBoundX) || (curY <= maxBoundY && curY >= minBoundY))) {

      // translate to 0-based index coordinates
      int curYIndex = curY - minBoundY;
      int curXIndex = curX - minBoundX;

        // check current
        if (Double.isNaN(curPeak)) {
          curPeak = getCCFUD(i1, i2, curX, curY, height, width);
          peaks[curYIndex][curXIndex] = curPeak;
        }

      HillClimbDirection direction = HillClimbDirection.NoMove;

      // Check each direction and move based on highest correlation
      for (HillClimbDirection dir : HillClimbDirection.values()) {
        // Skip NoMove direction
        if (dir == HillClimbDirection.NoMove)
          continue;

        double peak = Double.NEGATIVE_INFINITY;

        // Check if moving dir is in bounds
        if (curY + dir.getYDir() >= minBoundY && curY + dir.getYDir() <= maxBoundY
            && curX + dir.getXDir() >= minBoundX && curX + dir.getXDir() <= maxBoundX) {

          // Check if we have already computed the peak at dir
          if (peaks[curYIndex + dir.getYDir()][curXIndex + dir.getXDir()] == 0.0) {
            peak = getCCFUD(i1, i2, curX + dir.getXDir(), curY + dir.getYDir(), height, width);


            peaks[curYIndex + dir.getYDir()][curXIndex + dir.getXDir()] = peak;
          } else {
            peak = peaks[curYIndex + dir.getYDir()][curXIndex + dir.getXDir()];
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
   * Computes the cross correlation function (up-down)
   * 
   * @param i1 the first image for CCF computation (north/west neighbor)
   * @param i2 the second image for CCF computation (current)
   * @param x the x position
   * @param y the y position
   * @param height the height of the image
   * @param width the width of the image
   * @return the correlation
   */
  public static double getCCFUD(ImageTile<?> i1, ImageTile<?> i2, int x, int y, int height,
      int width) {
    Array2DView a1, a2;

    if (y < 0)
      y = 0;

    if (x >= 0) {
      a1 = new Array2DView(i1, y, height - y, x, width - x);
      a2 = new Array2DView(i2, 0, height - y, 0, width - x);

    } else {
      a1 = new Array2DView(i1, y, height - y, 0, width + x);
      a2 = new Array2DView(i2, 0, height - y, -x, width + x);
    }

    return Stitching.crossCorrelation(a1, a2);
  }


  /**
   * Complex the peak cross correlation (left/right) between two images
   * 
   * @param t1 image 1 (neighbor)
   * @param t2 image 2 (current)
   * @param x the x max position
   * @param y the y max position
   * @return the relative displacement along the x and y axis and the correlation
   */
  public static CorrelationTriple peakCrossCorrelationLR(ImageTile<?> t1, ImageTile<?> t2, int x,
      int y) {
    int w = t1.getWidth();
    int h = t1.getHeight();
    List<CorrelationTriple> corrList = new ArrayList<CorrelationTriple>();

    int[][] dims = { {y, x}, {y, w - x}, {h - y, x}, {h - y, w - x}};

    for (int i = 0; i < 4; i++) {
      int nr = dims[i][0];
      int nc = dims[i][1];

      Array2DView a1 = new Array2DView(t1, nr, h - nr, nc, w - nc);
      Array2DView a2 = new Array2DView(t2, 0, h - nr, 0, w - nc);

      double peak = crossCorrelation(a1, a2);

      if (Double.isNaN(peak) || Double.isInfinite(peak)) {
        peak = -1.0;
      }


      corrList.add(new CorrelationTriple(peak, nc, nr));

    }

    for (int i = 0; i < 4; i++) {
      int nr = dims[i][0];
      int nc = dims[i][1];

      Array2DView a1 = new Array2DView(t1, 0, h - nr, nc, w - nc);
      Array2DView a2 = new Array2DView(t2, nr, h - nr, 0, w - nc);

      double peak = crossCorrelation(a1, a2);

      if (Double.isNaN(peak) || Double.isInfinite(peak)) {
        peak = -1.0;
      }

      corrList.add(new CorrelationTriple(peak, nc, -nr));

    }

    if (corrList.size() == 0)
      return new CorrelationTriple(Double.NEGATIVE_INFINITY, 0, 0);

    return Collections.max(corrList);
  }

  /**
   * Computes cross correlation search with hill climbing (left-right)
   * 
   * @param minBoundX min x boundary
   * @param maxBoundX max x boundary
   * @param minBoundY min y boundary
   * @param maxBoundY max y bounadary
   * @param startX start x position for hill climb
   * @param startY start y position for hill climb
   * @param i1 the first image for CCF computation (north/west neighbor)
   * @param i2 the second image for CCF computation (current)
   * @return the highest correlation triple within the bounding box using hill climbing
   */
  public static CorrelationTriple computeCCF_HillClimbing_LR(int minBoundX, int maxBoundX,
      int minBoundY, int maxBoundY, int startX, int startY, ImageTile<?> i1, ImageTile<?> i2) {
    int width = i1.getWidth();
    int height = i1.getHeight();

    int curX = startX;
    int curY = startY;
    double curPeak = Double.NaN;

    minBoundY = Math.max(minBoundY, -height);
    minBoundY = Math.min(minBoundY, height);

    maxBoundY = Math.max(maxBoundY, -height);
    maxBoundY = Math.min(maxBoundY, height);

    minBoundX = Math.max(minBoundX, 0);
    minBoundX = Math.min(minBoundX, width);

    maxBoundX = Math.max(maxBoundX, 0);
    maxBoundX = Math.min(maxBoundX, width);

    // create array of peaks +1 for inclusive, +2 for each end
    int yLength = maxBoundY - minBoundY + 1 + 2;
    int xLength = maxBoundX - minBoundX + 1 + 2;

    double[][] peaks = new double[yLength][xLength];

    boolean foundPeak = false;

    // Compute hill climbing
    while (!foundPeak
        && ((curX <= maxBoundX && curX >= minBoundX) || (curY <= maxBoundY && curY >= minBoundY))) {

      // translate to 0-based index coordinates
      int curYIndex = curY - minBoundY;
      int curXIndex = curX - minBoundX;

      // check current
      if (Double.isNaN(curPeak)) {
        curPeak = getCCFLR(i1, i2, curX, curY, height, width);
        peaks[curYIndex][curXIndex] = curPeak;
      }

      HillClimbDirection direction = HillClimbDirection.NoMove;

      // Check each direction and move based on highest correlation
      for (HillClimbDirection dir : HillClimbDirection.values()) {
        // Skip NoMove direction
        if (dir == HillClimbDirection.NoMove)
          continue;

        double peak = Double.NEGATIVE_INFINITY;

        // Check if moving dir is in bounds
        if (curY + dir.getYDir() >= minBoundY && curY + dir.getYDir() <= maxBoundY
            && curX + dir.getXDir() >= minBoundX && curX + dir.getXDir() <= maxBoundX) {

          // Check if we have already computed the peak at dir
          if (peaks[curYIndex + dir.getYDir()][curXIndex + dir.getXDir()] == 0.0) {
            peak = getCCFLR(i1, i2, curX + dir.getXDir(), curY + dir.getYDir(), height, width);
            peaks[curYIndex + dir.getYDir()][curXIndex + dir.getXDir()] = peak;
          } else {
            peak = peaks[curYIndex + dir.getYDir()][curXIndex + dir.getXDir()];
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
   * Wrapper that computes the left/right CCF at a given x and y location between two image tile's
   * 
   * @param x the x location
   * @param y the y location
   * @param i1 the first image tile (north/west neighbor)
   * @param i2 the second image tile (current)
   * @return the correlation triple at x, y
   */
  public static CorrelationTriple computeCCF_LR(int x, int y, ImageTile<?> i1, ImageTile<?> i2) {
    return computeCCF_LR(x, x, y, y, i1, i2);
  }

  /**
   * Computes the left/right CCF values inside a bounding box, returning the best CCF value (one
   * with the highest correlation)
   * 
   * @param minBoundX the minimum x value of the bounding box
   * @param maxBoundX the maximum x value of the bounding box
   * @param minBoundY the minimum y value of the bounding box
   * @param maxBoundY the maximum y value of the bounding box
   * @param i1 the first image for CCF computation (north/west neighbor)
   * @param i2 the second image for CCF computation (current)
   * @return the highest correlation triple within the bounding box
   */
  public static CorrelationTriple computeCCF_LR(int minBoundX, int maxBoundX, int minBoundY,
      int maxBoundY, ImageTile<?> i1, ImageTile<?> i2) {
    int width = i1.getWidth();
    int height = i1.getHeight();

    double maxPeak = Double.NEGATIVE_INFINITY;
    int x = minBoundX;
    int y = minBoundY;

    for (int j = minBoundX; j <= maxBoundX; j++) {
      for (int i = minBoundY; i <= maxBoundY; i++) {
        Array2DView a1, a2;
        double peak;

        if (i >= 0) {
          a1 = new Array2DView(i1, i, height - i, j, width - j);
          a2 = new Array2DView(i2, 0, height - i, 0, width - j);

        } else {
          a1 = new Array2DView(i1, 0, height + i, j, width - j);
          a2 = new Array2DView(i2, -i, height + i, 0, width - j);
        }

        peak = Stitching.crossCorrelation(a1, a2);
        if (peak > maxPeak) {
          x = j;
          y = i;
          maxPeak = peak;
        }
      }
    }

    if (Double.isInfinite(maxPeak)) {
      x = minBoundX;
      y = minBoundY;
      maxPeak = -1.0;
    }

    return new CorrelationTriple(maxPeak, x, y);
  }

  /**
   * Computes the left/right CCF values inside a bounding box, returning the best CCF value (one
   * with the highest correlation)
   * 
   * @param minBoundX the minimum x value of the bounding box
   * @param maxBoundX the maximum x value of the bounding box
   * @param minBoundY the minimum y value of the bounding box
   * @param maxBoundY the maximum y value of the bounding box
   * @param i1 the first image for CCF computation (north/west neighbor)
   * @param i2 the second image for CCF computation (current)
   * @param fileName the file name to save the CCF
   * @return the highest correlation triple within the bounding box
   */
  public static CorrelationTriple computeCCF_LRAndSave(int minBoundX, int maxBoundX, int minBoundY,
      int maxBoundY, ImageTile<?> i1, ImageTile<?> i2, String fileName) {
    int width = i1.getWidth();
    int height = i1.getHeight();

    double maxPeak = Double.NEGATIVE_INFINITY;
    int x = minBoundX;
    int y = minBoundY;

    try {
      FileWriter writer = new FileWriter(fileName);

      for (int j = minBoundX; j <= maxBoundX; j++) {
        for (int i = minBoundY; i <= maxBoundY; i++) {
          Array2DView a1, a2;
          double peak;

          if (i >= 0) {
            a1 = new Array2DView(i1, i, height - i, j, width - j);
            a2 = new Array2DView(i2, 0, height - i, 0, width - j);

          } else {
            a1 = new Array2DView(i1, 0, height + i, j, width - j);
            a2 = new Array2DView(i2, -i, height + i, 0, width - j);
          }

          peak = Stitching.crossCorrelation(a1, a2);

          writer.write(peak + ",");

          if (peak > maxPeak) {
            x = j;
            y = i;
            maxPeak = peak;
          }
        }

        writer.write("\n");
      }

      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (Double.isInfinite(maxPeak)) {
      x = minBoundX;
      y = minBoundY;
      maxPeak = -1.0;
    }

    return new CorrelationTriple(maxPeak, x, y);
  }

  /**
   * Compute the cross correlation function (left-right)
   * 
   * @param i1 the first image for CCF computation (north/west neighbor)
   * @param i2 the second image for CCF computation (current)
   * @param x the x position
   * @param y the y position
   * @param height the height of the image
   * @param width the width of the image
   * @return the correlation
   */
  public static double getCCFLR(ImageTile<?> i1, ImageTile<?> i2, int x, int y, int height,
      int width) {
    Array2DView a1, a2;

    if (x < 0)
      x = 0;

    if (y >= 0) {
      a1 = new Array2DView(i1, y, height - y, x, width - x);
      a2 = new Array2DView(i2, 0, height - y, 0, width - x);

    } else {
      a1 = new Array2DView(i1, 0, height + y, x, width - x);
      a2 = new Array2DView(i2, -y, height + y, 0, width - x);
    }

    return Stitching.crossCorrelation(a1, a2);
  }

  /**
   * Computes the cross correlation between two arrays
   * 
   * @param a1 double array 1
   * @param a2 double array 2
   * @return the cross correlation
   */
  public static double crossCorrelation(Array2DView a1, Array2DView a2) {
    double sum_prod = 0.0;
    double sum1 = 0.0;
    double sum2 = 0.0;
    double norm1 = 0.0;
    double norm2 = 0.0;
    double a1_ij;
    double a2_ij;

    int n_rows = a1.getViewHeight();
    int n_cols = a2.getViewWidth();

    int sz = n_rows * n_cols;

    for (int i = 0; i < n_rows; i++)
      for (int j = 0; j < n_cols; j++) {
        a1_ij = a1.get(i, j);
        a2_ij = a2.get(i, j);
        sum_prod += a1_ij * a2_ij;
        sum1 += a1_ij;
        sum2 += a2_ij;
        norm1 += a1_ij * a1_ij;
        norm2 += a2_ij * a2_ij;
      }

    double numer = sum_prod - sum1 * sum2 / sz;
    double denom = Math.sqrt((norm1 - sum1 * sum1 / sz) * (norm2 - sum2 * sum2 / sz));

    double val = numer / denom;

    if (Double.isNaN(val) || Double.isInfinite(val)) {
      val = -1.0;
    }

    return val;
  }

}
