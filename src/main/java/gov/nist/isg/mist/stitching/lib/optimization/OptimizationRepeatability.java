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
// Date: Apr 11, 2014 11:04:25 AM EST
//
// Time-stamp: <Apr 11, 2014 11:04:25 AM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.optimization;

import gov.nist.isg.mist.stitching.gui.StitchingGuiUtils;
import gov.nist.isg.mist.stitching.gui.StitchingStatistics;
import gov.nist.isg.mist.stitching.gui.params.StitchingAppParams;
import gov.nist.isg.mist.stitching.lib.common.CorrelationTriple;
import gov.nist.isg.mist.stitching.lib.common.MinMaxElement;
import gov.nist.isg.mist.stitching.lib.exceptions.GlobalOptimizationException;
import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib.imagetile.Stitching;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.optimization.OptimizationUtils.Direction;
import gov.nist.isg.mist.stitching.lib.optimization.OptimizationUtils.DisplacementValue;
import gov.nist.isg.mist.stitching.lib.optimization.workflow.data.OptimizationData;
import gov.nist.isg.mist.stitching.lib.optimization.workflow.tasks.OptimizationRepeatabilityWorker;
import gov.nist.isg.mist.stitching.lib.optimization.workflow.tasks.BookKeeper;
import gov.nist.isg.mist.stitching.lib.optimization.workflow.tasks.TileProducer;
import gov.nist.isg.mist.stitching.lib.statistics.StatisticUtils.OP_TYPE;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverser;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverser.Traversals;
import gov.nist.isg.mist.stitching.lib.tilegrid.traverser.TileGridTraverserFactory;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * Entry point for the global optimization: 'Optimization Repeatability'.
 * 
 * @author Tim Blattner
 * @version 1.0
 * @param <T>
 */
public class OptimizationRepeatability<T> implements Thread.UncaughtExceptionHandler {

  public enum MissingSwitch
  {
    Median,
    Search
  }

  /**
   * The maximum repeatability.
   */
  public static int MaxRepeatability = 10;

  private static final MissingSwitch missingSwitch =  MissingSwitch.Median;
  private static final double OverlapError = 5.0;

  private TileGrid<ImageTile<T>> grid;
  private JProgressBar progressBar;
  private int userDefinedRepeatability;
  private boolean isUserDefinedRepeatability;
  private double userDefinedHorizontalOverlap;
  private double userDefinedVerticalOverlap;
  private double userDefinedOverlapError;
  private StitchingAppParams params;
  private StitchingStatistics stitchingStatistics;

  private List<OptimizationRepeatabilityWorker<T>> optimizationWorkers;
  private TileProducer<T> producer;
  private BookKeeper<T> bk;
  private List<Thread> executionThreads;
  private volatile boolean isCancelled;


  private volatile boolean exceptionThrown;
  private Throwable workerThrowable;


  /**
   * 
   * Initializes the optimization technique using repeatability
   * 
   * @param grid the grid of image tiles
   * @param progressBar a progress bar (or null if no progress bar is needed)
   * @param params the stitching app parameters
   * @param stitchingStatistics the statistics file
   */
  public OptimizationRepeatability(TileGrid<ImageTile<T>> grid, JProgressBar progressBar, StitchingAppParams params, StitchingStatistics stitchingStatistics)
  {
    this.grid = grid;    
    this.progressBar = progressBar;
    this.params = params;
    this.userDefinedRepeatability = params.getAdvancedParams().getRepeatability();
    this.userDefinedHorizontalOverlap = params.getAdvancedParams().getHorizontalOverlap();
    this.userDefinedVerticalOverlap = params.getAdvancedParams().getVerticalOverlap();

    this.userDefinedOverlapError = params.getAdvancedParams().getOverlapUncertainty();


    this.isUserDefinedRepeatability = this.userDefinedRepeatability != 0;
    this.stitchingStatistics = stitchingStatistics;
    this.isCancelled = false;

    this.exceptionThrown = false;
    this.workerThrowable = null;
  }


  /**
   * Computes a global optimization based on computed repeatability for an image grid. This method
   * first, computes the repeatability based on 'good' tile translations. Then using the
   * repeatability provides a search range for computing an exhaustive cross correlation.
   * @throws GlobalOptimizationException
   */
  public void computeGlobalOptimizationRepeatablitySequential() throws GlobalOptimizationException, FileNotFoundException {
    StitchingGuiUtils.updateProgressBar(this.progressBar, true, "Computing Repeatability",
                                        "Optimization...", 0, 0, 0, false);

    File directory = new File(this.params.getOutputParams().getMetadataPath());
    if(this.params.getOutputParams().isOutputMeta())
      directory.mkdirs();

    File preOptPosFile = this.params.getOutputParams().getRelPosNoOptFile(this.stitchingStatistics.getCurrentTimeslice());
    if(this.params.getOutputParams().isOutputMeta())
      Stitching.outputRelativeDisplacementsNoOptimization(this.grid, preOptPosFile);

    double percOverlapError = OverlapError;

    if (!Double.isNaN(this.userDefinedOverlapError))
      percOverlapError = this.userDefinedOverlapError;

    int computedRepeatability;

    int repeatabilityNorth = correctTranslationsModel(percOverlapError, Direction.North);

    int repeatabilityWest = correctTranslationsModel(percOverlapError, Direction.West);

//    // Save to x,y starting point output folder TODO: Might remove this or make it an official meta output
//    File hillClimbPosFile = this.params.getOutputParams().getHillClimbPositionFile(this.stitchingStatistics.getCurrentTimeslice());
//    if(this.params.getOutputParams().isOutputMeta())
//      Stitching.outputRelativeDisplacements(this.grid, hillClimbPosFile);

    computedRepeatability = repeatabilityNorth > repeatabilityWest ? repeatabilityNorth : repeatabilityWest;

    if (this.isUserDefinedRepeatability) {
      computedRepeatability = this.userDefinedRepeatability;
    }


    if (this.isCancelled) {
      return;
    }

    computedRepeatability = 2 * computedRepeatability + 1;

    Log.msg(LogType.HELPFUL, "Calculated Repeatability: " + computedRepeatability);

    StitchingGuiUtils.updateProgressBar(this.progressBar, false, null, "Optimization...", 0,
                                        this.grid.getExtentHeight() * this.grid.getExtentWidth(), 0, false);

    TileGridTraverser<ImageTile<T>> traverser =
        TileGridTraverserFactory.makeTraverser(Traversals.DIAGONAL, this.grid);

    // loop over the image tiles
    for (ImageTile<T> t : traverser) {
      if(!t.isTileRead()) t.readTile();

      int row = t.getRow();
      int col = t.getCol();

      // optimize with west neighbor
      if (col > grid.getStartCol()) {
        ImageTile<T> west = grid.getTile(row, col - 1);
        if(!west.isTileRead()) west.readTile();
        CorrelationTriple westTrans = t.getWestTranslation();

        int xMin = westTrans.getX() - computedRepeatability;
        int xMax = westTrans.getX() + computedRepeatability;
        int yMin = westTrans.getY() - computedRepeatability;
        int yMax = westTrans.getY() + computedRepeatability;

        double oldCorr = westTrans.getCorrelation();
        CorrelationTriple bestWest;
        try {
          if (Stitching.USE_HILLCLIMBING) {

            if(Stitching.USE_EXHAUSTIVE_INSTEAD_OF_HILLCLIMB_SEARCH) {
              bestWest =
                  Stitching.computeCCF_Exhaustive_LR(xMin, xMax, yMin, yMax, westTrans.getX(),
                                                     westTrans.getY(), west, t);
            }else{
              bestWest =
                  Stitching.computeCCF_HillClimbing_LR(xMin, xMax, yMin, yMax, westTrans.getX(),
                                                       westTrans.getY(), west, t);
            }

          } else {
            bestWest = Stitching.computeCCF_LR(xMin, xMax, yMin, yMax, west, t);
          }

          t.setWestTranslation(bestWest);

          if (!Double.isNaN(oldCorr)) {
            // If the old correlation was a number, then it was a good translation.
            // Increment the new translation by the value of the old correlation to increase beyond 1
            // This will enable these tiles to have higher priority in minimum spanning tree search
            t.getWestTranslation().incrementCorrelation(Math.floor(oldCorr));
          }

          if (t.getTileCorrelation() < bestWest.getCorrelation())
            t.setTileCorrelation(bestWest.getCorrelation());

        } catch (NullPointerException e) {
        } finally {
          if (progressBar != null)
            StitchingGuiUtils.incrementProgressBar(progressBar);

          west.releasePixels();
        }
      }


      // optimize with north neighbor
      if (row > grid.getStartRow()) {
        ImageTile<T> north = grid.getTile(row - 1, col);
        if(!north.isTileRead()) north.readTile();
        CorrelationTriple northTrans = t.getNorthTranslation();

        int xMin = northTrans.getX() - computedRepeatability;
        int xMax = northTrans.getX() + computedRepeatability;
        int yMin = northTrans.getY() - computedRepeatability;
        int yMax = northTrans.getY() + computedRepeatability;

        double oldCorr = northTrans.getCorrelation();
        CorrelationTriple bestNorth;
        try {
          if (Stitching.USE_HILLCLIMBING) {

            if(Stitching.USE_EXHAUSTIVE_INSTEAD_OF_HILLCLIMB_SEARCH) {
              bestNorth =
                  Stitching.computeCCF_Exhaustive_UD(xMin, xMax, yMin, yMax, northTrans.getX(),
                                                     northTrans.getY(), north, t);
            }else{
              bestNorth =
                  Stitching.computeCCF_HillClimbing_UD(xMin, xMax, yMin, yMax, northTrans.getX(),
                                                       northTrans.getY(), north, t);
            }

          } else {
            bestNorth = Stitching.computeCCF_UD(xMin, xMax, yMin, yMax, north, t);
          }

          t.setNorthTranslation(bestNorth);

          if (!Double.isNaN(oldCorr)) {
            // If the old correlation was a number, then it was a good translation.
            // Increment the new translation by the value of the old correlation to increase beyond 1
            // This will enable these tiles to have higher priority in minimum spanning tree search
            t.getNorthTranslation().incrementCorrelation(Math.floor(oldCorr));
          }

          if (t.getTileCorrelation() < bestNorth.getCorrelation())
            t.setTileCorrelation(bestNorth.getCorrelation());

        } catch (NullPointerException e) {
        } finally {
          if (progressBar != null)
            StitchingGuiUtils.incrementProgressBar(progressBar);

          north.releasePixels();
        }



      }


      t.releasePixels();
    }




  }

  /**
   * Computes a global optimization based on computed repeatability for an image grid. This method
   * first, computes the repeatability based on 'good' tile translations. Then using the
   * repeatability provides a search range for computing an exhaustive cross correlation.
   * @throws GlobalOptimizationException 
   */
  public void computeGlobalOptimizationRepeatablity() throws GlobalOptimizationException, FileNotFoundException {

    StitchingGuiUtils.updateProgressBar(this.progressBar, true, "Computing Repeatability",
        "Optimization...", 0, 0, 0, false);

    File directory = new File(this.params.getOutputParams().getMetadataPath());
    if(this.params.getOutputParams().isOutputMeta())
      directory.mkdirs();

    File preOptPosFile = this.params.getOutputParams().getRelPosNoOptFile(this.stitchingStatistics.getCurrentTimeslice());
    if(this.params.getOutputParams().isOutputMeta())
      Stitching.outputRelativeDisplacementsNoOptimization(this.grid, preOptPosFile);

    double percOverlapError = OverlapError;

    if (!Double.isNaN(this.userDefinedOverlapError))
      percOverlapError = this.userDefinedOverlapError;

    int computedRepeatability;

    int repeatabilityNorth = correctTranslationsModel(percOverlapError, Direction.North);

    int repeatabilityWest = correctTranslationsModel(percOverlapError, Direction.West);

//    // Save to x,y starting point output folder TODO: Might remove this or make it an official meta output
//    File hillClimbPosFile = this.params.getOutputParams().getHillClimbPositionFile(this.stitchingStatistics.getCurrentTimeslice());
//    if(this.params.getOutputParams().isOutputMeta())
//      Stitching.outputRelativeDisplacements(this.grid, hillClimbPosFile);
    
    computedRepeatability = repeatabilityNorth > repeatabilityWest ? repeatabilityNorth : repeatabilityWest;

    if (this.isUserDefinedRepeatability) {
      computedRepeatability = this.userDefinedRepeatability;
    }


    if (this.isCancelled) {
      return;
    }

    computedRepeatability = 2 * computedRepeatability + 1;

    Log.msg(LogType.HELPFUL, "Calculated Repeatability: " + computedRepeatability);
    
    StitchingGuiUtils.updateProgressBar(this.progressBar, false, null, "Optimization...", 0,
        this.grid.getExtentHeight() * this.grid.getExtentWidth(), 0, false);

    // Reset pixel release counts if we must manage pixel data memory
    if (ImageTile.freePixelData()) {
      for (int r = 0; r < this.grid.getExtentHeight(); r++) {
        for (int c = 0; c < this.grid.getExtentWidth(); c++) {
          this.grid.getSubGridTile(r, c).resetPixelReleaseCount(this.grid.getExtentWidth(),
              this.grid.getExtentHeight(), this.grid.getStartRow(), this.grid.getStartCol());
          this.grid.getSubGridTile(r, c).releasePixels();
        }
      }
    }

    int numThreads = this.params.getAdvancedParams().getNumCPUThreads();

    this.executionThreads = new ArrayList<Thread>();
    this.optimizationWorkers = new ArrayList<OptimizationRepeatabilityWorker<T>>();

    TileGridTraverser<ImageTile<T>> traverser =
        TileGridTraverserFactory.makeTraverser(Traversals.DIAGONAL, this.grid);



    Semaphore sem = null;

    if (ImageTile.freePixelData())
    {
      int numPermits = Math.min(grid.getExtentWidth(), grid.getExtentHeight()) + 2 + numThreads;
      sem = new Semaphore(numPermits, true);

    }

    BlockingQueue<OptimizationData<T>> tileQueue = new ArrayBlockingQueue<OptimizationData<T>>(this.grid.getSubGridSize()*2);
    BlockingQueue<OptimizationData<T>> bkQueue = new ArrayBlockingQueue<OptimizationData<T>>(this.grid.getSubGridSize()*2);

    this.producer = new TileProducer<T>(traverser, bkQueue, sem);
    this.bk = new BookKeeper<T>(bkQueue, tileQueue, sem, grid);

    Thread tmp;
    for (int i = 0; i < numThreads; i++) {
      OptimizationRepeatabilityWorker<T> worker = new OptimizationRepeatabilityWorker<T>(tileQueue, bkQueue, this.grid, computedRepeatability, this.progressBar);
      this.optimizationWorkers.add(worker);
      tmp = new Thread(worker);
      tmp.setUncaughtExceptionHandler(this);
      tmp.setName("OptimizationWorker");
      this.executionThreads.add(tmp);
    }

    Thread producer = new Thread(this.producer);
    producer.setName("OptimizationProducer");
    Thread bk = new Thread(this.bk);
    bk.setName("OptimizationBk");

    producer.start();
    bk.start();

    for (Thread thread : this.executionThreads)
      thread.start();

    try {
      producer.join();
      bk.join();

      for (Thread thread : this.executionThreads) {

        thread.join();

      }
    } catch (InterruptedException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }

  }

  /**
   * Cancels all running threads for the global optimization
   */
  public void cancelOptimization() {
    this.isCancelled = true;
    if (this.optimizationWorkers != null) {
      for (OptimizationRepeatabilityWorker<T> worker : this.optimizationWorkers)
          worker.cancelExecution();
    }

    if (this.bk != null)
      this.bk.cancel();

    if (this.producer != null)
      this.producer.cancel();

  }


  private MuSigmaTuple getOverlap(Direction dir, DisplacementValue dispValue) throws FileNotFoundException {

    OptimizationUtils.OverlapType overlapComputationType = this.params.getAdvancedParams().getOverlapComputationType();

    MuSigmaTuple translationsModel = OptimizationUtils.getOverlap(this.grid, dir, dispValue, overlapComputationType, this.userDefinedVerticalOverlap);

    return translationsModel;
  }

  /**
   * Correlations translations of a grid based on a filtering step that enables estimating the
   * repeatability and backlash of a microscope.
   * 
   * @param percOverlapError the percent overlap error
   * @param dir the direction
   * @return the computed repeatability
   * @throws GlobalOptimizationException 
   */
  public int correctTranslationsModel(double percOverlapError, Direction dir) throws GlobalOptimizationException, FileNotFoundException {
    DisplacementValue dispValue = null;
    switch (dir) {
      case North:
        dispValue = DisplacementValue.Y;
        if (this.grid.getExtentHeight() == 1)
          return 0;
        break;
      case West:
        dispValue = DisplacementValue.X;
        if (this.grid.getExtentWidth() == 1)
          return 0;
        break;
      default:
        break;

    }


    // get the overlap for the current direction
    // TODO change the function name
    MuSigmaTuple translationsModel = getOverlap(dir, dispValue);
    // compute the image overlap from the translations model (mu, sigma)
    double range = OptimizationUtils.getOverlapRange(grid, dispValue);
    double overlap = Math.round(100 * (1.0 - translationsModel.mu / range));
    double userDefinedPercOverlapError = this.params.getAdvancedParams().getOverlapUncertainty();
    if(!Double.isNaN(translationsModel.sigma) && Double.isNaN(userDefinedPercOverlapError)) {
      // percent overlap uncertainty is within 3 sigma or 99% of normal distribution
      percOverlapError = 100 * 3 * translationsModel.sigma / range;
      percOverlapError = Math.min(percOverlapError , 5.0); // limit to max of 5 percent overlap error
      System.out.println("Percent Overlap Uncertainty: " + percOverlapError);
    }


    this.stitchingStatistics.setComputedOverlap(dir, overlap);

    // check that an overlap value has been computed
    if(Double.isNaN(overlap)) {
      Log.msg(LogType.MANDATORY, "Warning: Unable to compute overlap for " + dir
                                 + " direction. Please set your overlap in the advanced options");
      throw new GlobalOptimizationException("Unable to compute overlap for " + dir + " direction.");
    }
    // limit the overlap to reasonable values
    overlap = Math.max(percOverlapError, Math.min(overlap, 100.0 - percOverlapError));
    this.stitchingStatistics.setOverlap(dir, overlap);
    Log.msg(LogType.VERBOSE, "Computed overlap: " + overlap);


    Log.msg(LogType.INFO, "Correcting translations: " + dir.name());

    HashSet<ImageTile<T>> validTranslations;
    validTranslations = OptimizationUtils.filterTranslations(this.grid, dir, percOverlapError,
                                                             overlap,
                                                             this.params.getAdvancedParams()
                                                                 .getNumCPUThreads(),
                                                             this.stitchingStatistics);

    if (validTranslations.size() == 0) {
      Log.msg(LogType.MANDATORY, "Warning: no good translations found for " + dir
          + " direction. Estimated translations generated from the overlap.");

      if(this.isUserDefinedRepeatability) {
        Log.msg(LogType.MANDATORY, "Warning: no good translations found for " + dir
                                   + " direction. Repeatability has been set to " +
                                   this.userDefinedRepeatability + " (advanced options value).");
      }else{
        Log.msg(LogType.MANDATORY, "Warning: no good translations found for " + dir
                                   + " direction. Repeatability has been set to zero.");
      }

      // replace with translation estimated from overlap
      OptimizationUtils.replaceTranslationFromOverlap(this.grid, dir, dispValue, overlap);
      int r = 0;
      if(this.isUserDefinedRepeatability)
        r = this.userDefinedRepeatability;

      this.stitchingStatistics.setRepeatability(dir, r);
      this.stitchingStatistics.setNumValidTilesAfterFilter(dir, 0);

      Log.msg(LogType.MANDATORY, "Please check the statistics file for more details.");

      return r;
    }

    MinMaxElement minMaxVal = null;
    List<MinMaxElement> minMaxList = null;

    Log.msg(LogType.INFO, "Computing min/max combinations using " + validTranslations.size()
        + " valid translations");

    switch (dir) {
      case North:
//        minMaxVal = OptimizationUtils.getMinMaxValidTiles(validTranslations, dir, DisplacementValue.X);

        minMaxVal = OptimizationUtils.getNoOutlierMinMaxValidTiles(validTranslations, dir, DisplacementValue.X);

        minMaxList =
            OptimizationUtils.getMinMaxValidPerRow(this.grid, validTranslations, dir,
                                                   DisplacementValue.Y);
        break;
      case West:
//        minMaxVal = OptimizationUtils.getMinMaxValidTiles(validTranslations, dir, DisplacementValue.Y);

        minMaxVal = OptimizationUtils.getNoOutlierMinMaxValidTiles(validTranslations, dir,
                                                                   DisplacementValue.Y);

        minMaxList =
            OptimizationUtils.getMinMaxValidPerCol(this.grid, validTranslations, dir, DisplacementValue.X);
        break;
      default:
        break;
    }

    Log.msg(LogType.INFO, "Computing Repeatability");
    int repeatability1 = OptimizationUtils.getRepeatability(minMaxVal);
    int repeatability2 = OptimizationUtils.getRepeatability(minMaxList);

    int repeatability = repeatability1 > repeatability2 ? repeatability1 : repeatability2;

    if (this.isUserDefinedRepeatability) {
      Log.msg(LogType.MANDATORY, "Computed repeatability: " + repeatability + " Overridden by user specified repeatability: " + this.userDefinedRepeatability);
      repeatability = this.userDefinedRepeatability;
    }else{
      if (repeatability > MaxRepeatability) {
        Log.msg(LogType.MANDATORY, "Warning: the computed repeatability (" + repeatability
                                   + ") is larger than the max repeatability (" + MaxRepeatability
                                   + ").");
      }
    }

    this.stitchingStatistics.setRepeatability(dir, repeatability);


    Log.msg(LogType.HELPFUL, "Repeatability for " + dir.name() + ": " + repeatability);

    // Re-filter based on repeatability, the grid gets updated here
    switch (dir) {
      case North:
        OptimizationUtils.removeInvalidTranslationsPerRow(this.grid, validTranslations, repeatability,
            dir);
        break;
      case West:
        OptimizationUtils.removeInvalidTranslationsPerCol(this.grid, validTranslations, repeatability,
            dir);
        break;
      default:
        break;

    }

    this.stitchingStatistics
    .setNumValidTilesAfterFilter(dir, validTranslations.size());

    if (validTranslations.size() == 0) {
      Log.msg(LogType.MANDATORY, "Warning: no good translations found for " + dir
                                 + " direction. Estimated translations generated from the overlap.");

      if(this.isUserDefinedRepeatability) {
        Log.msg(LogType.MANDATORY, "Warning: no good translations found for " + dir
                                   + " direction. Repeatability has been set to zero.");
      }else{
        Log.msg(LogType.MANDATORY, "Warning: no good translations found for " + dir
                                   + " direction. Repeatability has been set to " +
                                   this.userDefinedRepeatability + " (advanced options value).");
      }

      // replace with translation estimated from overlap
      OptimizationUtils.replaceTranslationFromOverlap(this.grid, dir, dispValue, overlap);
      int r = 0;
      if(this.isUserDefinedRepeatability)
        r = this.userDefinedRepeatability;

      Log.msg(LogType.MANDATORY, "Please check the statistics file for more details.");

      return r;
    }


    Log.msg(LogType.VERBOSE, "Fixing translations");
    List<Integer> missingRowOrCol = null;
    switch (dir) {
      case North:
        this.stitchingStatistics.setNumRowsCols(dir, grid.getExtentHeight());
        missingRowOrCol = OptimizationUtils.fixInvalidTranslationsPerRow(this.grid, dir, OP_TYPE.MEDIAN);
        break;
      case West:
        this.stitchingStatistics.setNumRowsCols(dir, grid.getExtentWidth());
        missingRowOrCol = OptimizationUtils.fixInvalidTranslationsPerCol(this.grid, dir, OP_TYPE.MEDIAN);
        break;
      default:
        break;
    }


    this.stitchingStatistics.setEmptyRowsCols(dir, missingRowOrCol);

    if (missingRowOrCol != null && missingRowOrCol.size() > 0) {
      
      switch(missingSwitch)
      {
        case Median:
          CorrelationTriple median = OptimizationUtils.computeOp(validTranslations, dir, OP_TYPE.MEDIAN);
          
          switch(dir)           
          {
            case North:
              OptimizationUtils.replaceTranslationsRow(this.grid, median, missingRowOrCol);
              break;
            case West:
              OptimizationUtils.replaceTranslationsCol(this.grid, median, missingRowOrCol);
              break;
            default:
              break;
            
          }
          
          
          break;
        case Search:
          CorrelationTriple min = OptimizationUtils.computeOp(validTranslations, dir, OP_TYPE.MIN);
          CorrelationTriple max = OptimizationUtils.computeOp(validTranslations, dir, OP_TYPE.MAX);

          Log.msg(LogType.INFO, "Min backlash: " + min.toXYString() + " - " + repeatability);

          Log.msg(LogType.INFO, "Max backlash: " + max.toXYString() + " + " + repeatability);

          Log.msg(LogType.HELPFUL, "Missing " + missingRowOrCol.size() + " entire rows or cols for "
              + dir.name() + ": " + Arrays.toString(missingRowOrCol.toArray()));

          switch (dir) {
            case North:
              OptimizationUtils.updateEmptyRows(this.grid, overlap, percOverlapError, repeatability,
                  min, max, missingRowOrCol);

              break;
            case West:
              OptimizationUtils.updateEmptyColumns(this.grid, overlap, percOverlapError, repeatability,
                  min, max, missingRowOrCol);

              break;
            default:
              break;

          }
          break;
        default:
          break;
        
      }
      

    }

    return repeatability;
  }

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    this.exceptionThrown = true;
    this.workerThrowable = e;

    this.cancelOptimization();
  }

  public boolean isExceptionThrown() {
    return this.exceptionThrown;
  }
  public Throwable getWorkerThrowable() {
    return this.workerThrowable;
  }

}
