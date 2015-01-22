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
// Date: Aug 1, 2013 4:23:01 PM EST
//
// Time-stamp: <Aug 1, 2013 4:23:01 PM tjb3>
//
//
// ================================================================

package main.gov.nist.isg.mist.stitching.lib.tilegrid;

import java.io.File;
import java.io.InvalidClassException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import main.gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import main.gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import main.gov.nist.isg.mist.stitching.lib.log.Log;
import main.gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import main.gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader;

/**
 * Class that generates a grid of tiles. Given an origin and numbering strategy.
 * <p>
 * The grid can be initialized using two methods:
 * 
 * <pre>
 * <code>
 * (1) Using the constructor: TileGrid(int startRow, int startCol, int extentWidth, int extentHeight,
      TileGridLoader gridLoader, File imageDir, Class<?> classType)
 * <p>
 * (2) Using the constructors: TileGrid(StitchingAppParams params, 
 * 		Class&lt;?&gt; classType)
 *    or TileGrid(StitchingAppParams params, int timeSlice,
 * 			Class&lt;?&gt; classType)
 * </code>
 * </pre>
 * <p>
 * 
 * The TileGridLoader class is used to construct the names of tiles in the TileGrid
 * 
 * A sub-grid can be created using the variouis constructors. You can decompose a grid of tiles into
 * independent sections using:
 * 
 * <pre>
 * <code>partitionGrid(int numSlices, GridDecomposition type)
 * </code>
 * </pre>
 * 
 * Example usage:
 * 
 * <pre>
 * <code>
 * int startRow = 0;
 * int startCol = 0;
 * int extentWidth = 4;
 * int extentHeight = 4;
 * File tileDir = new File("C:\\Data\\dataDir");
 * TileGrid&lt;ImageTile&lt;CUdeviceptr&gt;&gt; grid = null;
 * 
 * SequentialTileGridLoader(int gridWidth, int gridHeight, int startTile, String filePattern,
      GridOrigin origin, GridDirection direction)
 * 
 * try {     
 *     grid = new TileGrid&lt;ImageTile&lt;CUdeviceptr&gt;&gt;(startRow, startCol, 
 *     extentWidth, extentHeight, 
 *     new SequentialTileGridLoader(16, 22, 1, "F_{pppp}.tif", GridOrigin.UL, GridDirection.ROW), 
 *     , newtileDir, 
 *     JCUDAImageTile.class);
 * } catch (InvalidClassException e) {
 *    Log.msg(LogType.MANDATORY, e.getMessage());			
 * }
 * 
 * int numGPUs = 2;
 * 
 * List&lt;TileGrid&lt;ImageTile&lt;T&gt;&gt;&gt; grids = grid.partitionGrid(numGPUs, 
 * 			GridDecomposition.HORIZONTAL);
 * </code>
 * </pre>
 * <p>
 * There are two methods for access data. One using subGrid, which will shift based on the starting
 * row and starting column specified.
 * 
 * <pre>
 * <code>grid.getSubGridTile(row, col);</code>
 * </pre>
 * <p>
 * or by accessing the grid using the plate width and plate height of the grid with
 * 
 * <pre>
 * <code>grid.getTile(row, col);</code>
 * </pre>
 * 
 * <p>
 * Also provided are utility functions to verify if a tile exists or not inside of a subgrid by
 * using
 * 
 * <pre>
 * <code>grid.hasTile(row, col);</code>
 * </pre>
 * 
 * @author Tim Blattner
 * @version 1.0
 * @param <T> must be ImageTile type
 * 
 */
public class TileGrid<T extends ImageTile<?>> {

  /**
   * Grid decomposition types.
   * 
   * @author Tim Blattner
   * @version 1.0
   */
  public static enum GridDecomposition {
    /**
     * Horizontal decomposition
     */
    HORIZONTAL("Horizontal"),

    /**
     * Vertical decomposition
     */
    VERTICAL("Vertical"),

    /**
     * Block decomposition
     */
    BLOCK("Block");
    private GridDecomposition(final String text) {
      this.text = text;
    }

    private final String text;

    @Override
    public String toString() {
      return this.text;
    }

  }

  private TileGridLoader gridLoader;
  private File imageDir;
  private T[][] tiles;
  private int extentWidth;
  private int extentHeight;
  private int startRow;
  private int startCol;

  /**
   * Initialize TileSubGrid starting at row and column, and hold extentWidth and extentHeight
   * rows/columns
   * 
   * @param startRow the start row for the subgrid
   * @param startCol the start column for the subgrid
   * @param extentWidth the width of the subgrid
   * @param extentHeight the height of the subgrid
   * @param gridLoader The tile grid loader
   * @param imageDir image directory
   * @param classType the type of object
   * @throws InvalidClassException
   */
  public TileGrid(int startRow, int startCol, int extentWidth, int extentHeight,
      TileGridLoader gridLoader, File imageDir, Class<?> classType) throws InvalidClassException {
    this.startRow = startRow;
    this.startCol = startCol;
    this.extentWidth = extentWidth;
    this.extentHeight = extentHeight;
    this.gridLoader = gridLoader;
    this.imageDir = imageDir;

    this.initImageTileGrid(classType);
  }

  /**
   * Initializes the tile grid
   * 
   * @param params the stitching parameters
   * @param classType the class type
   * @throws InvalidClassException
   */
  public TileGrid(StitchingAppParams params, Class<?> classType) throws InvalidClassException {
    this(params.getInputParams().getStartRow(), params.getInputParams().getStartCol(), params.getInputParams().getExtentWidth(), params.getInputParams()
        .getExtentHeight(), params.getInputParams().getTileGridLoader(), new File(params.getInputParams().getImageDir()), classType);
  }

  /**
   * Initializes the tile grid 
   * 
   * @param params the stitching parameters
   * @param timeSlice the time slice
   * @param classType the class type
   * @throws InvalidClassException
   */
  public TileGrid(StitchingAppParams params, int timeSlice, Class<?> classType)
      throws InvalidClassException {
    this(params.getInputParams().getStartRow(), params.getInputParams().getStartCol(), params.getInputParams().getExtentWidth(), params.getInputParams()
        .getExtentHeight(), params.getInputParams().getTileGridLoader(timeSlice), new File(params.getInputParams().getImageDir()),
        classType);
  }

  /**
   * References of the original TileGrid, but with different startRow, col, width, and height
   * 
   * @param original the original reference tile grid
   * @param startRow the start row of the new grid
   * @param startCol the start column of the new grid
   * @param extentWidth the extent width of the new grid
   * @param extentHeight the extent height of the new grid
   */
  public TileGrid(TileGrid<T> original, int startRow, int startCol, int extentWidth,
      int extentHeight) {
    this.tiles = original.getTiles();
    this.gridLoader = original.getGridLoader();
    this.imageDir = original.getImageDir();
    this.startCol = startCol + original.getStartCol();
    this.startRow = startRow + original.getStartRow();
    this.extentHeight = extentHeight;
    this.extentWidth = extentWidth;
  }

  @Override
  public String toString() {
    return "SubGrid: startRow: " + this.startRow + " startCol: " + this.startCol + " width: " + this.extentWidth
        + " height: " + this.extentHeight;
  }

  /**
   * Converts the image tiles to a list
   * 
   * @return the list of tiles
   */
  public List<T> convertToList() {
    List<T> gridList = new ArrayList<T>();
    for (int r = 0; r < this.extentHeight; r++) {
      for (int c = 0; c < this.getExtentWidth(); c++) {
        gridList.add(this.getSubGridTile(r, c));
      }
    }

    return gridList;
  }

  /**
   * @return the image directory
   */
  public File getImageDir() {
    return this.imageDir;
  }

  /**
   * @return the tile grid loader
   */
  public TileGridLoader getGridLoader() {
    return this.gridLoader;
  }

  /**
   * @return the array of tiles
   */
  public T[][] getTiles() {
    return this.tiles;
  }

  /**
   * @return the subgrid size (extentWidth * extentHeight)
   */
  public int getSubGridSize() {
    return this.extentWidth * this.extentHeight;
  }

  /**
   * Gets a tile inside of a sub-grid at the row and column
   * 
   * @param row the row in the subgrid (0 to extentHeight-1)
   * @param col the column in the subgrid (0 to extentWidth-1)
   * @return the image tile at the given row and column
   */
  public T getSubGridTile(int row, int col) {
    return getTile(row + this.startRow, col + this.startCol);
  }

  /**
   * Gets neighbors of this tile
   * 
   * @param tile the tile to check neighbors for
   * @return the neighbors of the tile
   */
  public List<T> getNeighbors(T tile) {
    List<T> tiles = new ArrayList<T>();

    int row = tile.getRow();
    int col = tile.getCol();

    // North
    if (hasTile(row - 1, col))
      tiles.add(getTile(row - 1, col));

    // South
    if (hasTile(row + 1, col))
      tiles.add(getTile(row + 1, col));

    // West
    if (hasTile(row, col - 1))
      tiles.add(getTile(row, col - 1));

    // East
    if (hasTile(row, col + 1))
      tiles.add(getTile(row, col + 1));

    return tiles;
  }

  /**
   * @return the extentWidth
   */
  public int getExtentWidth() {
    return this.extentWidth;
  }

  /**
   * @return the extentHeight
   */
  public int getExtentHeight() {
    return this.extentHeight;
  }

  /**
   * @return the startRow
   */
  public int getStartRow() {
    return this.startRow;
  }

  /**
   * @return the startCol
   */
  public int getStartCol() {
    return this.startCol;
  }

  /**
   * Initializes ImageTile Grid
   * 
   * @param the type of object
   * @throws InvalidClassException
   */
  @SuppressWarnings("unchecked")
  private void initImageTileGrid(Class<?> imageTileClass) throws InvalidClassException {    

    this.tiles = (T[][]) Array.newInstance(imageTileClass, this.gridLoader.getGridHeight(), this.gridLoader.getGridWidth());
    Constructor<?> constructor = null;

    try
    {
      constructor = imageTileClass.getConstructor(File.class, // File 
          int.class, // row
          int.class, // col
          int.class, // extentWidth
          int.class, // extentHeight
          int.class, // startRow
          int.class, // startCol
          boolean.class); // readImage
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    }
    
    if (constructor == null)
    {
      Log.msg(LogType.MANDATORY, "Error: constructor has changed. initImageTileGrid unable to find constructor using reflection.");
      throw new InvalidClassException("Unable to load constructor.");
    }
      
    for (int r = 0; r < this.gridLoader.getGridHeight(); r++) {
      for (int c = 0; c < this.gridLoader.getGridWidth(); c++) {

        String fileName = this.gridLoader.getTileName(r, c);

        try {
          this.tiles[r][c] = (T) constructor.newInstance(new File(this.imageDir, fileName), r, c, this.extentWidth, this.extentHeight,
              this.startRow, this.startCol, false);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }


  /**
   * Gets the tile
   * 
   * @param row the row location
   * @param col the column location
   * @return the tile
   */
  public T getTile(int row, int col) {
    return this.tiles[row][col];
  }

  /**
   * Checks if the tile exists in the grid
   * 
   * @param tile the tile to check
   * @return true if the tile exists, otherwise false
   */
  public boolean hasTile(ImageTile<?> tile) {
    return hasTile(tile.getRow(), tile.getCol());
  }

  /**
   * Checks if the tile's row/column exists in the grid
   * 
   * @param row the row in the grid
   * @param col the column in the grid
   * @return true if the tile exists in the grid, otherwise false
   */
  public boolean hasTile(int row, int col) {
    if (col >= this.startCol && col < this.extentWidth + this.startCol && row >= this.startRow
        && row < this.extentHeight + this.startRow)
      return true;
    return false;
  }

  /**
   * Prints the numbering grid
   */
  public void printNumberGrid() {
    this.gridLoader.printNumberGrid();
  }

  /**
   * Gets a human readable statistics of the global tile grid
   * 
   * @return the grid stats
   */
  public String getGridStats() {
    return this.gridLoader.toString();
  }

  /**
   * Partitions the grid into numSlices. The method will reduce numSlices to create
   * even-sized partitions.
   * 
   * @param numSlices the number slices to decompose
   * @param type the type of grid decomposition
   * @return a list of TileGrids that contain a startRow/startCol and width and height based on the
   *         decomposition type.
   */
  public List<TileGrid<T>> partitionGrid(int numSlices, GridDecomposition type) {

    int sliceStartRow = 0;
    int sliceStartCol = 0;
    int sliceExtentWidth = 0;
    int sliceExtentHeight = 0;
    int sliceWidth = 0;
    int sliceHeight = 0;

    int numSlicesVertical = 0;
    int numSlicesHorizontal = 0;

    switch (type) {
      case BLOCK:
        int coef = (int) Math.sqrt(numSlices);

        while ((numSlices % coef) != 0) {
          coef--;
        }

        if (this.getExtentHeight() > this.getExtentWidth()) {
          numSlicesVertical = numSlices / coef;
          numSlicesHorizontal = coef;
        } else {
          numSlicesVertical = coef;
          numSlicesHorizontal = numSlices / coef;
        }

        sliceWidth = (int) Math.ceil((double) this.getExtentWidth() / (double) coef);

        sliceHeight = (int) Math.ceil((double) this.getExtentHeight() / (double) coef);

        numSlicesHorizontal =
            (int) Math.ceil(((double) this.getExtentHeight() / (double) sliceHeight));

        numSlicesVertical = (int) Math.ceil(((double) this.getExtentWidth() / (double) sliceWidth));

        break;
      case HORIZONTAL:
        numSlicesVertical = 1;

        numSlicesHorizontal =
            (numSlices < this.getExtentHeight() ? numSlices : this.getExtentHeight());

        sliceWidth = this.getExtentWidth();

        sliceHeight =
            (int) Math.ceil((double) this.getExtentHeight() / (double) numSlicesHorizontal);

        numSlicesHorizontal =
            (int) Math.ceil(((double) this.getExtentHeight() / (double) sliceHeight));

        break;
      case VERTICAL:
        numSlicesVertical = (numSlices < this.getExtentWidth() ? numSlices : this.getExtentWidth());

        numSlicesHorizontal = 1;

        sliceWidth = (int) Math.ceil((double) this.getExtentWidth() / (double) numSlicesVertical);

        sliceHeight = this.getExtentHeight();

        numSlicesVertical = (int) Math.ceil(((double) this.getExtentWidth() / (double) sliceWidth));

        break;
      default:
        break;

    }

    List<TileGrid<T>> newGrids =
        new ArrayList<TileGrid<T>>(numSlicesVertical * numSlicesHorizontal);

    for (int h = 0; h < numSlicesHorizontal; h++) {
      for (int v = 0; v < numSlicesVertical; v++) {
        sliceStartRow = h * sliceHeight;
        sliceStartCol = v * sliceWidth;

        if (sliceStartCol + sliceWidth > this.getExtentWidth())
          sliceExtentWidth = this.getExtentWidth() - sliceStartCol;
        else
          sliceExtentWidth = sliceWidth;

        if (sliceStartRow + sliceHeight > this.getExtentHeight())
          sliceExtentHeight = this.getExtentHeight() - sliceStartRow;
        else
          sliceExtentHeight = sliceHeight;

        TileGrid<T> nextSlice =
            new TileGrid<T>(this, sliceStartRow, sliceStartCol, sliceExtentWidth, sliceExtentHeight);
        newGrids.add(nextSlice);

      }

    }

    return newGrids;

  }
}
