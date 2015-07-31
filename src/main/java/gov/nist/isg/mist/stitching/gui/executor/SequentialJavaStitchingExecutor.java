package gov.nist.isg.mist.stitching.gui.executor;

import java.io.FileNotFoundException;
import java.io.InvalidClassException;

import javax.swing.*;

import gov.nist.isg.mist.stitching.gui.StitchingGuiUtils;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.imagetile.java.JavaImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.JavaTileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.imagetile.memory.TileWorkerMemory;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverser;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverserFactory;

/**
 * Created by mmajursk on 7/31/2015.
 */
public class SequentialJavaStitchingExecutor<T> implements StitchingExecutorInterface<T> {

  private boolean isCanceled = false;


  @Override
  public TileGrid<ImageTile<T>> initGrid(StitchingAppParams params, int timeSlice)
      throws FileNotFoundException {

    TileGrid<ImageTile<T>> grid = null;

    if (params.getInputParams().isTimeSlicesEnabled())
    {
      try {
        grid =
            new TileGrid<ImageTile<T>>(params, timeSlice, JavaImageTile.class);
      } catch (InvalidClassException e) {
        e.printStackTrace();
      }
    }
    else
    {
      try {
        grid = new TileGrid<ImageTile<T>>(params, JavaImageTile.class);
      } catch (InvalidClassException e) {
        e.printStackTrace();
      }
    }

    ImageTile<T> tile = grid.getSubGridTile(0, 0);
    tile.readTile();

    JavaImageTile.initJavaPlan(tile);

    return grid;
  }

  @Override
  public void cancelExecution() {
    this.isCanceled = true;
  }

  @Override
  public void launchStitching(TileGrid<ImageTile<T>> grid, StitchingAppParams params,
                              JProgressBar progressBar, int timeSlice) throws Throwable {

    Log.msg(Log.LogType.MANDATORY, "Running Sequential Java Stitching");
    TileGridTraverser<ImageTile<T>> traverser = TileGridTraverserFactory.makeTraverser(
        TileGridTraverser.Traversals.DIAGONAL_CHAINED, grid);

    TileWorkerMemory memory = null;
    for (ImageTile<T> t : traverser) {
      if(this.isCanceled) return;

      t.setThreadID(0);


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
            Log.LogType.HELPFUL,
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
            Log.LogType.HELPFUL,
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

  @Override
  public boolean checkForLibs(StitchingAppParams params, boolean displayGui) {
    return true;
  }

  @Override
  public void cleanup() {

  }

  @Override
  public <T> boolean checkMemory(TileGrid<ImageTile<T>> grid, int numWorkers)
      throws FileNotFoundException {
    long requiredMemoryBytes = 0;
    long memoryPoolCount = 2;
    ImageTile<T> tile = grid.getSubGridTile(0, 0);
    tile.readTile();

    // Account for image pixel data
    requiredMemoryBytes += (long)tile.getHeight() * (long)tile.getWidth() * memoryPoolCount * 2L; // 16 bit pixel data

    // Account for image pixel data up conversion
    long byteDepth = tile.getBitDepth()/8;
    if(byteDepth != 2) {
      // if up-converting at worst case there will be numWorkers copies of the old precision pixel data
      requiredMemoryBytes += (long)numWorkers * (long)tile.getHeight() * (long)tile.getWidth() * byteDepth;
    }

    // Account for Java FFT data
    int[] n =
        {JavaImageTile.fftPlan.getFrequencySampling2().getCount(),
         JavaImageTile.fftPlan.getFrequencySampling1().getCount() * 2};
    long size = 1;
    for(int val : n)
      size *= val;
    requiredMemoryBytes += memoryPoolCount * size * 4L; // float[n1][n2]

    requiredMemoryBytes += size * 4L; // new float[fftHeight][fftWidth];

    // pad with 100MB
    requiredMemoryBytes += 100L*1024L*1024L;

    return requiredMemoryBytes < Runtime.getRuntime().maxMemory();
  }
}
