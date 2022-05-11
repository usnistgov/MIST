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

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JProgressBar;

import gov.nist.isg.mist.gui.StitchingGuiUtils;
import gov.nist.isg.mist.lib.common.Array2DView;
import gov.nist.isg.mist.lib.export.tileblender.TileAverageBlend;
import gov.nist.isg.mist.lib.export.tileblender.TileBlender;
import gov.nist.isg.mist.lib.export.tileblender.TileLinearBlend;
import gov.nist.isg.mist.lib.export.tileblender.TileOverlayBlend;
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
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.formats.out.OMETiffWriter;
import loci.formats.services.OMEXMLService;
import ome.units.UNITS;
import ome.units.quantity.Length;
import ome.units.unit.Unit;
import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.enums.PixelType;
import ome.xml.model.primitives.PositiveInteger;

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

  private BlendingMode blendingMode;
  private MicroscopyUnits unit;
  private double unitX;
  private double unitY;

  private TileGrid<ImageTile<T>> grid;
  private int tileDim;
  private int startX;
  private int startY;
  private int endX;
  private int endY;
  private int imageWidth;
  private int imageHeight;
  private JProgressBar progressBar;
  private volatile boolean isCancelled;

  private int imageType;
  private double alpha;

  /**
   * Creates a large image exporter with a specific blending function
   *
   * @param grid        the grid of images
   * @param tileDim    the dimensions of the tiles
   * @param imageType   the type of image
   * @param startX      the start x position
   * @param startY      the start y position
   * @param width       the width of the final image
   * @param height      the height of the final image
   * @param blendingMode     the blending mode
   * @param progressBar the progress bar
   * @param unit the microscopy unit to save in metadata
   * @param unitX the x size based around unit
   * @param unitY the x size based around unit
   * @param alpha the alpha value for linear blend
   */
  public LargeImageExporter(TileGrid<ImageTile<T>> grid, int tileDim, int imageType, int startX, int startY, int width,
                            int height, BlendingMode blendingMode, MicroscopyUnits unit, double unitX, double unitY, double alpha, JProgressBar progressBar) {
    this.grid = grid;
    this.tileDim = tileDim;
    this.imageType = imageType;
    this.startX = startX;
    this.startY = startY;
    this.blendingMode = blendingMode;
    this.imageWidth = width;
    this.imageHeight = height;
    this.endX = startX + width;
    this.endY = startY + height;
    this.progressBar = progressBar;
    this.unit = unit;
    this.unitX = unitX;
    this.unitY = unitY;
    this.alpha = alpha;
    this.isCancelled = false;
  }

  private File doExport(File file, boolean withOverlap) {
    String filePath = "temp.ome.tif";

    if (file != null) {
      filePath = file.getAbsolutePath();
    }

    File outputFile = new File(filePath);

    if (outputFile.exists()) {
      boolean deleted = outputFile.delete();
      if (!deleted) {
        Log.msg(LogType.INFO, "Failed to delete existing file: " + outputFile.getAbsolutePath());
      }
    }


    int numChannels = 1;
    int samplesPerChannel = 1;
    int numBytesPerChannel = 1;
    PixelType pixelType = null;
    boolean interleaved = false;

    switch(this.imageType) {
      case ImagePlus.GRAY8:
        numBytesPerChannel = 1;
        pixelType = PixelType.UINT8;
        break;
      case ImagePlus.GRAY16:
        numBytesPerChannel = 2;
        pixelType = PixelType.UINT16;
        break;
      case ImagePlus.COLOR_RGB:
        samplesPerChannel = 3;
//        numChannels = 4;
        numBytesPerChannel = 3;
        pixelType = PixelType.UINT8;
        interleaved = true;
        break;
      case ImagePlus.GRAY32:
      default:
        numBytesPerChannel = 4;
        pixelType = PixelType.FLOAT;
        break;
    }

    // for each image tile get the region ...
    TileGridTraverser<ImageTile<T>> traverser =
            TileGridTraverserFactory.makeTraverser(Traversals.ROW, this.grid);

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

    TileBlender tileBlender = null;

    switch(this.blendingMode)
    {
      case OVERLAY:
        tileBlender = new TileOverlayBlend(numBytesPerChannel, this.imageType);
        break;
      case AVERAGE:
        tileBlender = new TileAverageBlend(numBytesPerChannel, this.imageType);
        break;
      case LINEAR:
        ImageTile<T> tile = sortedTileList.get(0);
        tile.readTile();
        tileBlender = new TileLinearBlend(numBytesPerChannel, this.imageType, tile.getWidth(), tile.getHeight(), this.alpha);
        break;
    }


    // Compute number of tiles
    int numTilesRow = (int)Math.ceil((double)this.imageHeight / this.tileDim);
    int numTilesCol = (int)Math.ceil((double)this.imageWidth / this.tileDim);

    StitchingGuiUtils.updateProgressBar(this.progressBar, false, null, "Blending tiles...", 0,
            numTilesRow * numTilesCol, 0, false);

    ServiceFactory factory = null;
    try {
      factory = new ServiceFactory();
      OMEXMLService service = factory.getInstance(OMEXMLService.class);
      IMetadata omexml = service.createOMEXMLMetadata();

      omexml.setImageID("Image:0", 0);
      omexml.setPixelsID("Pixels:0", 0);

      omexml.setPixelsBinDataBigEndian(Boolean.TRUE, 0, 0);

      omexml.setPixelsDimensionOrder(DimensionOrder.XYCZT, 0);
      omexml.setPixelsType(pixelType, 0);
      omexml.setPixelsSizeX(new PositiveInteger(this.imageWidth), 0);
      omexml.setPixelsSizeY(new PositiveInteger(this.imageHeight), 0);
      omexml.setPixelsSizeZ(new PositiveInteger(1), 0);

      omexml.setPixelsSizeC(new PositiveInteger(numChannels * samplesPerChannel), 0);
      omexml.setPixelsSizeT(new PositiveInteger(1), 0);

      omexml.setChannelID("Channel:0:" + 0, 0,  0);
      omexml.setChannelSamplesPerPixel(new PositiveInteger(samplesPerChannel), 0, 0);

//      for (int channel = 0; channel < numChannels; channel++) {
//        omexml.setChannelID("Channel:0:" + channel, 0,  channel);
//        omexml.setChannelSamplesPerPixel(new PositiveInteger(samplesPerChannel), 0, channel);
//      }

      Unit<Length> unit = this.unit.getUnit();

      Length physicalSizeX = new Length(this.unitX, unit);
      Length physicalSizeY = new Length(this.unitY, unit);
      Length physicalSizeZ = new Length(1.0, unit);

      omexml.setPixelsPhysicalSizeX(physicalSizeX, 0);
      omexml.setPixelsPhysicalSizeY(physicalSizeY, 0);
      omexml.setPixelsPhysicalSizeZ(physicalSizeZ, 0);

      OMETiffWriter omeTiffWriter = new OMETiffWriter();
      omeTiffWriter.setMetadataRetrieve(omexml);
      omeTiffWriter.setInterleaved(interleaved);
      omeTiffWriter.setBigTiff(true);
      omeTiffWriter.setCompression(OMETiffWriter.COMPRESSION_UNCOMPRESSED);


      int actualTileSizeX = omeTiffWriter.setTileSizeX(this.tileDim);
      int actualTileSizeY = omeTiffWriter.setTileSizeY(this.tileDim);

      omeTiffWriter.setId(filePath);

      long initTime = 0L;
      long blendCallTime = 0L;
      long blendTime = 0L;
      long postProcessTime = 0L;
      long closeTime = 0L;

      // Loop over each tile to fill them in
      for (int tileRow = 0; tileRow < numTilesRow; ++tileRow) {
        int tileStartY = tileRow * actualTileSizeY;
//        int tileEndY = Math.min(tileStartY + actualTileSizeY, this.imageHeight);

        for (int tileCol = 0; tileCol < numTilesCol; ++tileCol) {
          int tileStartX = tileCol * actualTileSizeX;
//          int tileEndX = Math.min(tileStartX + actualTileSizeX, this.imageWidth);

          int tileSizeX = actualTileSizeX;
          int tileSizeY = actualTileSizeY;

          if (tileStartX + tileSizeX > this.imageWidth) {
            tileSizeX = this.imageWidth - tileStartX;
          }

          if (tileStartY + tileSizeY > this.imageHeight) {
            tileSizeY = this.imageHeight - tileStartY;
          }

          long startInit = System.currentTimeMillis();
          tileBlender.init(tileSizeX, tileSizeY);
          initTime += System.currentTimeMillis() - startInit;

          Rectangle2D tileRect = new Rectangle(tileStartX, tileStartY, tileSizeX, tileSizeY);

          long startBlendTime = System.currentTimeMillis();

          for (ImageTile<T> tile : sortedTileList) {
            if (this.isCancelled)
              return file;

            int absX = tile.getCol() * tile.getWidth();
            int absY = tile.getRow() * tile.getHeight();

            if (withOverlap) {
              absX = tile.getAbsXPos();
              absY = tile.getAbsYPos();
//              int absEndX = absX + tile.getWidth();
//              int absEndY = absY + tile.getHeight();
            }

            if (tile.getWidth() == 0 || tile.getHeight() == 0)
              tile.readTile();

            Rectangle2D imageTileRect = new Rectangle(absX, absY, tile.getWidth(), tile.getHeight());

            Rectangle2D intersect = tileRect.createIntersection(imageTileRect);

            // Check if intersection has height and width, if it doesn't then check next tile
            if (intersect.getHeight() <= 0 || intersect.getWidth() <=0) {
              tile.releasePixels();
              continue;
            }

            int absImageTileStartX = (int)intersect.getX();
            int absImageTileStartY = (int)intersect.getY();
            int copyWidth = (int)intersect.getWidth();
            int copyHeight = (int)intersect.getHeight();

            // Clip width to edge of image
            if (absImageTileStartX + copyWidth > this.imageWidth) {
              copyWidth = this.imageWidth - absImageTileStartX;
            }

            // Clip height to edge of image
            if (absImageTileStartY + copyHeight > this.imageHeight) {
              copyHeight = this.imageHeight - absImageTileStartY;
            }

            // Translate the absolute coordinates back into tile and view coordinates
            int tileX = absImageTileStartX - tileStartX;
            int tileY = absImageTileStartY - tileStartY;
            int viewX = absImageTileStartX - absX;
            int viewY = absImageTileStartY - absY;

            if (tileX < 0 || tileY < 0 || viewX < 0 || viewY < 0) {
              tile.releasePixels();
              continue;
            }

            tile.readTile();
            Array2DView arrayView = new Array2DView(tile, viewY, copyHeight, viewX, copyWidth);

            long blendCallStart = System.currentTimeMillis();
            tileBlender.blend(tileX, tileY, arrayView, tile);
            blendCallTime += System.currentTimeMillis() - blendCallStart;

            tile.releasePixels();

          }

          blendTime += System.currentTimeMillis() - startBlendTime;

          int writeXSize = actualTileSizeX;
          int writeYSize = actualTileSizeY;

          if (tileStartX + actualTileSizeX > this.imageWidth) {
            writeXSize = this.imageWidth - tileStartX;
          }

          if (tileStartY + actualTileSizeY > this.imageHeight) {
            writeYSize = this.imageHeight - tileStartY;
          }


          long startPostProc = System.currentTimeMillis();
          tileBlender.postProcess(tileStartX, tileStartY, writeXSize, writeYSize, omeTiffWriter);
          postProcessTime += System.currentTimeMillis() - startPostProc;
          StitchingGuiUtils.incrementProgressBar(this.progressBar);
        }


      }

      StitchingGuiUtils.updateProgressBar(this.progressBar, true, "Finalizing Write");
      long startClose = System.currentTimeMillis();
      omeTiffWriter.close();
      closeTime += System.currentTimeMillis() - startClose;

      Log.msg(LogType.MANDATORY, "Blending Profile: Init Time: " + initTime + " Blend Time: " + blendTime + " Blend Call Time: " + blendCallTime +  " Post Proccess Time: " + postProcessTime + " Closing Time: " + closeTime);

    } catch (DependencyException e) {
      e.printStackTrace();
    } catch (ServiceException e) {
      e.printStackTrace();
    } catch (FormatException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return outputFile;

  }


  /**
   * Exports image to file, if the file is null, then it will return the image plus object without
   * saving it to file
   *
   * @param file the file to export or null if save is not needed
   * @return the image plus object that was created after the export
   */
  public File exportImage(File file) throws FileNotFoundException {
    File exportedFile = this.doExport(file, true);

    return exportedFile;
  }

  /**
   * Exports image to file, if the file is null, then it will return the image plus object without
   * saving it to file
   *
   * @param file the file to export or null if save is not needed
   * @return the image plus object that was created after the export
   */
  public File exportImageNoOverlap(File file) throws FileNotFoundException {

    File exportedFile = this.doExport(file, false);
    return exportedFile;
  }

  public void cancel() {
    this.isCancelled = true;
  }

  /**
   * Exports an image to disk, or if file is null then returns the ImagePlus object associated with
   * the export
   *
   * @param grid        the grid of images
   * @param tileDim     the tile dimension
   * @param startX      the start X position
   * @param startY      the start Y position
   * @param width       the width of the large image
   * @param height      the height of the large image
   * @param blendingMode  the blending mode
   * @param alpha       the alpha value for linear blend
   * @param unit    the unit of measurement
   * @param unitX  the x dim unit value
   * @param unitY  the y dim unit value
   * @param file        the file or null if no save is needed
   * @param progressBar the progress bar
   * @return the File object associated with the export
   */
  public static <T> File exportImage(TileGrid<ImageTile<T>> grid, int tileDim, int imageType, int startX, int startY,
                                          int width, int height, BlendingMode blendingMode, double alpha, MicroscopyUnits unit, double unitX, double unitY, File file, JProgressBar progressBar) throws FileNotFoundException {
    LargeImageExporter<T> exporter =
        new LargeImageExporter<T>(grid, tileDim, imageType, startX, startY, width, height, blendingMode, unit, unitX, unitY, alpha, progressBar);
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
   * @param blendingMode the blending function
   * @param alpha       the alpha value for linear blend
   * @param unit    the unit of measurement
   * @param unitX  the x dim unit value
   * @param unitY  the y dim unit value
   * @param file    the file or null if no save is needed
   */
  public static <T> void exportImage(TileGrid<ImageTile<T>> grid, int tileDim, int imageType, int startX, int startY,
                                     int width, int height, BlendingMode blendingMode, double alpha, MicroscopyUnits unit, double unitX, double unitY,File file) throws FileNotFoundException {
    LargeImageExporter<T> exporter =
        new LargeImageExporter<T>(grid, tileDim, imageType, startX, startY, width, height, blendingMode, unit, unitX, unitY, alpha, null);
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
   * @param blendingMode     the blending mode
   * @param alpha       the alpha value for linear blend
   * @param unit    the unit of measurement
   * @param unitX  the x dim unit value
   * @param unitY  the y dim unit value
   * @param file        the file or null if no save is needed
   * @param progressBar the progress bar
   * @return the File object associated with the export
   */
  public static <T> File exportImageNoOverlap(TileGrid<ImageTile<T>> grid, int tileDim, int imageType, int startX,
                                                   int startY, int width, int height, BlendingMode blendingMode, double alpha, MicroscopyUnits unit, double unitX, double unitY, File file, JProgressBar progressBar) throws FileNotFoundException {
    LargeImageExporter<T> exporter =
        new LargeImageExporter<T>(grid, tileDim, imageType, startX, startY, width, height, blendingMode, unit, unitX, unitY, alpha, progressBar);
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
   * @param blendingMode the blending mode
   * @param alpha   the alpha value for linear blend
   * @param unit    the unit of measurement
   * @param unitX  the x dim unit value
   * @param unitY  the y dim unit value
   * @param file    the file or null if no save is needed
   */
  public static <T> void exportImageNoOverlap(TileGrid<ImageTile<T>> grid, int tileDim, int imageType, int startX, int startY,
                                              int width, int height, BlendingMode blendingMode, double alpha, MicroscopyUnits unit, double unitX, double unitY, File file) throws FileNotFoundException {
    LargeImageExporter<T> exporter =
        new LargeImageExporter<T>(grid, tileDim, imageType, startX, startY, width, height, blendingMode, unit, unitX, unitY, alpha,null);
    exporter.exportImageNoOverlap(file);

  }


}
