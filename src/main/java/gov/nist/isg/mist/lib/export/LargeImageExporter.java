// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



// ================================================================
//
// Author: tjb3
// Date: Apr 25, 2014 4:11:36 PM EST
//
// Time-stamp: <Apr 25, 2014 4:11:36 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.lib.export;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JProgressBar;

import gov.nist.isg.mist.gui.StitchingGuiUtils;
import gov.nist.isg.mist.lib.common.Array2DView;
import gov.nist.isg.mist.lib.export.blend.Blender;
import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.log.Log.LogType;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverser;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverser.Traversals;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverserFactory;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * Class for exporting large images using a blending mode
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class LargeImageExporter<T> {


  /**
   * Enum representing the different types of blending modes
   *
   * @author Tim Blattner
   * @version 1.0
   */
  public enum BlendingMode {
    /**
     * Overlay blending mode
     */
    OVERLAY("Overlay", "Choose only one pixel from overlapping pixels based on highest correlation", false),

    /**
     * Average blending mode
     */
    AVERAGE("Average", "Computes the the average intensity for each image", false),

    /**
     * Linear blending mode
     */
    LINEAR("Linear", "Smoothly alters the intensity of the overlapping area between images", true);

    private String name;
    private String toolTipText;
    private boolean requiresAlpha;

    private BlendingMode(String name, String toolTipText, boolean requiresAlpha) {
      this.name = name;
      this.toolTipText = toolTipText;
      this.requiresAlpha = requiresAlpha;
    }

    /**
     * Returns if alpha is required or not
     *
     * @return true if alpha is required, otherwise false
     */
    public boolean isRequiresAlpha() {
      return this.requiresAlpha;
    }

    /**
     * Gets the tooltip text
     *
     * @return the tooltip text
     */
    public String getToolTipText() {
      return this.toolTipText;
    }

    @Override
    public String toString() {
      return this.name;
    }

  }

  private Blender blender;
  private TileGrid<ImageTile<T>> grid;
  private int startX;
  private int startY;
  private int endX;
  private int endY;
  private JProgressBar progressBar;
  private volatile boolean isCancelled;

  /**
   * Creates a large image exporter with a specific blending function
   *
   * @param grid        the grid of images
   * @param startX      the start x position
   * @param startY      the start y position
   * @param width       the width of the final image
   * @param height      the height of the final image
   * @param blender     the blending function
   * @param progressBar the progress bar
   */
  public LargeImageExporter(TileGrid<ImageTile<T>> grid, int startX, int startY, int width,
                            int height, Blender blender, JProgressBar progressBar) {
    this.grid = grid;
    this.startX = startX;
    this.startY = startY;
    this.blender = blender;
    this.endX = startX + width;
    this.endY = startY + height;
    this.progressBar = progressBar;
    this.isCancelled = false;
  }

  /**
   * Exports image to file, if the file is null, then it will return the image plus object without
   * saving it to file
   *
   * @param file the file to export or null if save is not needed
   * @return the image plus object that was created after the export
   */
  public ImagePlus exportImage(File file) throws FileNotFoundException {
    // for each image tile get the region ...
    TileGridTraverser<ImageTile<T>> traverser =
        TileGridTraverserFactory.makeTraverser(Traversals.ROW, this.grid);

    StitchingGuiUtils.updateProgressBar(this.progressBar, false, null, "Blending tiles...", 0,
        this.grid.getExtentHeight() * this.grid.getExtentWidth(), 0, false);

    List<ImageTile<T>> sortedTileList = new ArrayList<ImageTile<T>>();
    for (ImageTile<T> tile : traverser) {
      sortedTileList.add(tile);
    }

    // Sorts tiles  from smallest correlation tile to largest correlation tile
    // This enables painting the highest correlation tiles last.
    Collections.sort(sortedTileList, new Comparator<ImageTile<T>>() {

      @Override
      public int compare(ImageTile<T> t1, ImageTile<T> t2) {
        return Double.compare(t1.getTileCorrelation(), t2.getTileCorrelation());
      }

    });

    for (ImageTile<T> tile : sortedTileList) {

      if (this.isCancelled)
        return null;

      if (tile.getWidth() == 0 || tile.getHeight() == 0)
        tile.readTile();

      int absX = tile.getAbsXPos();
      int absY = tile.getAbsYPos();
      int absEndX = absX + tile.getWidth();
      int absEndY = absY + tile.getHeight();

      // Determine tile start and end positions within the selected
      // region
      int tileStartX = (absX >= this.startX) ? absX : this.startX;
      int tileStartY = (absY >= this.startY) ? absY : this.startY;

      int tileEndX = (absEndX <= this.endX) ? absEndX : this.endX;
      int tileEndY = (absEndY <= this.endY) ? absEndY : this.endY;

      int tileWidth = tileEndX - tileStartX;
      int tileHeight = tileEndY - tileStartY;

      if (tileWidth <= 0 || tileHeight <= 0) {
        tile.releasePixelsNow();

        StitchingGuiUtils.incrementProgressBar(this.progressBar);

        continue;
      }

      // Translate the absolute coordinates back into tile coordinates
      int tileX = tileStartX - absX;
      int tileY = tileStartY - absY;

      if (tileX < 0 || tileY < 0) {
        tile.releasePixelsNow();

        StitchingGuiUtils.incrementProgressBar(this.progressBar);

        continue;
      }

      tile.readTile();
      Array2DView arrayView = new Array2DView(tile, tileY, tileHeight, tileX, tileWidth);
      this.blender.blend(tileStartX, tileStartY, arrayView, tile);

      tile.releasePixelsNow();

      StitchingGuiUtils.incrementProgressBar(this.progressBar);
    }

    if (this.progressBar != null) {
      if (file == null) {
        StitchingGuiUtils.updateProgressBar(this.progressBar, true, "Preparing image for Fiji",
            null, 0, 0, 0, false);

      } else {
        StitchingGuiUtils.updateProgressBar(this.progressBar, true, "Saving image to disk", null,
            0, 0, 0, false);
      }
    }

    this.blender.postProcess();

    return saveFile(file);
  }

  /**
   * Exports image to file, if the file is null, then it will return the image plus object without
   * saving it to file
   *
   * @param file the file to export or null if save is not needed
   * @return the image plus object that was created after the export
   */
  public ImagePlus exportImageNoOverlap(File file) throws FileNotFoundException {

    StitchingGuiUtils.updateProgressBar(this.progressBar, false, null,
        "Blending tiles...", 0, this.grid.getExtentHeight() * this.grid.getExtentWidth(), 0,
        false);

    for (int row = 0; row < this.grid.getExtentHeight(); row++) {
      for (int col = 0; col < this.grid.getExtentWidth(); col++) {

        if (this.isCancelled)
          return null;

        ImageTile<T> tile = this.grid.getSubGridTile(row, col);
        tile.readTile();

        int absX = col * tile.getWidth();
        int absY = row * tile.getHeight();

        Array2DView arrayView = new Array2DView(tile, 0, tile.getHeight(), 0, tile.getWidth());
        this.blender.blend(absX, absY, arrayView, tile);

        tile.releasePixelsNow();

        StitchingGuiUtils.incrementProgressBar(this.progressBar);
      }
    }


    if (this.progressBar != null) {
      if (file == null) {
        StitchingGuiUtils.updateProgressBar(this.progressBar, true, "Preparing image for Fiji",
            null, 0, 0, 0, false);

      } else {
        StitchingGuiUtils.updateProgressBar(this.progressBar, true, "Saving image to disk", null,
            0, 0, 0, false);
      }
    }

    this.blender.postProcess();

    return saveFile(file);
  }


  /**
   * Writes the file to disk, if file is null, then it will only return the ImagePlus object
   * associated with the large image
   *
   * @param file the file to save or null if no save is needed
   * @return the image plus object
   */
  public ImagePlus saveFile(File file) {
    ImageProcessor ip = this.blender.getResult();
    if (ip != null) {
      if (file == null) {
        Log.msg(LogType.MANDATORY, "Generating display image");
        return new ImagePlus("", ip);
      }

      Log.msg(LogType.MANDATORY, "Saving tiles to file: " + file.getAbsolutePath());
      ImagePlus img = new ImagePlus(file.getName(), ip);
      IJ.saveAs(img, "tiff", file.getAbsolutePath());
      return img;
    }

    return null;
  }

  public void cancel() {
    this.isCancelled = true;
  }

  /**
   * Exports an image to disk, or if file is null then returns the ImagePlus object associated with
   * the export
   *
   * @param grid        the grid of images
   * @param startX      the start X position
   * @param startY      the start Y position
   * @param width       the width of the large image
   * @param height      the height of the large image
   * @param blender     the blending function
   * @param file        the file or null if no save is needed
   * @param progressBar the progress bar
   * @return the ImagePlus object associated with the export
   */
  public static <T> ImagePlus exportImage(TileGrid<ImageTile<T>> grid, int startX, int startY,
                                          int width, int height, Blender blender, File file, JProgressBar progressBar) throws FileNotFoundException {
    LargeImageExporter<T> exporter =
        new LargeImageExporter<T>(grid, startX, startY, width, height, blender, progressBar);
    return exporter.exportImage(file);
  }

  /**
   * Exports an image to disk, or if file is null then returns the ImagePlus object associated with
   * the export
   *
   * @param grid    the grid of images
   * @param startX  the start X position
   * @param startY  the start Y position
   * @param width   the width of the large image
   * @param height  the height of the large image
   * @param blender the blending function
   * @param file    the file or null if no save is needed
   */
  public static <T> void exportImage(TileGrid<ImageTile<T>> grid, int startX, int startY,
                                     int width, int height, Blender blender, File file) throws FileNotFoundException {
    LargeImageExporter<T> exporter =
        new LargeImageExporter<T>(grid, startX, startY, width, height, blender, null);
    exporter.exportImage(file);

  }


  /**
   * Exports an image to disk, or if file is null then returns the ImagePlus object associated with
   * the export
   *
   * @param grid        the grid of images
   * @param startX      the start X position
   * @param startY      the start Y position
   * @param width       the width of the large image
   * @param height      the height of the large image
   * @param blender     the blending function
   * @param file        the file or null if no save is needed
   * @param progressBar the progress bar
   * @return the ImagePlus object associated with the export
   */
  public static <T> ImagePlus exportImageNoOverlap(TileGrid<ImageTile<T>> grid, int startX,
                                                   int startY, int width, int height, Blender blender, File file, JProgressBar progressBar) throws FileNotFoundException {
    LargeImageExporter<T> exporter =
        new LargeImageExporter<T>(grid, startX, startY, width, height, blender, progressBar);
    return exporter.exportImageNoOverlap(file);
  }

  /**
   * Exports an image to disk, or if file is null then returns the ImagePlus object associated with
   * the export
   *
   * @param grid    the grid of images
   * @param startX  the start X position
   * @param startY  the start Y position
   * @param width   the width of the large image
   * @param height  the height of the large image
   * @param blender the blending function
   * @param file    the file or null if no save is needed
   */
  public static <T> void exportImageNoOverlap(TileGrid<ImageTile<T>> grid, int startX, int startY,
                                              int width, int height, Blender blender, File file) throws FileNotFoundException {
    LargeImageExporter<T> exporter =
        new LargeImageExporter<T>(grid, startX, startY, width, height, blender, null);
    exporter.exportImageNoOverlap(file);

  }


}
