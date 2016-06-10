// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.


package gov.nist.isg.mist.optimization.model.overlap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.log.Log;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.optimization.model.StageModel;
import gov.nist.isg.mist.optimization.model.TranslationFilter;

/**
 * Abstract class interface for all overlap estimation executors.
 *
 * @author Michael Majurski
 */
public abstract class OverlapExecutorInterface<T> implements Thread.UncaughtExceptionHandler {

  private boolean exceptionThrown;
  private Throwable workerThrowable;
  private TileGrid.DisplacementValue displacementValue;
  private TileGrid.Direction direction;
  private double range;
  private double[] translations;
  private TileGrid<ImageTile<T>> grid;

  protected double overlap;

  /**
   * Abstract class interface for all overlap estimation executors.
   *
   * @param grid              the TileGrid from which to compute the overlap.
   * @param direction         the direction in which to compute to overlap.
   * @param displacementValue which displacement component of the given direction to use in
   *                          computing the overlap.
   */
  public OverlapExecutorInterface(TileGrid<ImageTile<T>> grid,
                                  TileGrid.Direction direction,
                                  TileGrid.DisplacementValue displacementValue) {
    this.grid = grid;
    this.direction = direction;
    this.displacementValue = displacementValue;

    // get valid range for translations given the direction
    range = StageModel.getTranslationRange(getGrid(), getDisplacementValue());
    // get list of translations for this direction and displacement value
    List<Integer> translationList = getTranslationsFromGrid(range, false);
    // remove translations that are out of range, or consist of a single pixel
    Iterator<Integer> itr = translationList.iterator();
    while (itr.hasNext()) {
      Integer val = itr.next();
      if (val <= 1 || val >= (range - 1))
        itr.remove();
    }

    // output translations to the log
    Log.msg(Log.LogType.INFO, "Translation used for MLE " + getDirection() + ":");
    Log.msg(Log.LogType.INFO, translationList.toString());

    // extract the translations into an primitive array
    translations = new double[translationList.size()];
    for (int i = 0; i < translationList.size(); i++)
      translations[i] = translationList.get(i);

    this.overlap = Double.NaN;
  }


  public abstract void execute();

  public abstract void cancel();

  public abstract double getOverlap();


  public TileGrid.DisplacementValue getDisplacementValue() {
    return displacementValue;
  }

  public TileGrid.Direction getDirection() {
    return direction;
  }

  public double[] getTranslations() {
    return this.translations;
  }

  public double getRange() {
    return this.range;
  }


  protected TileGrid<ImageTile<T>> getGrid() {
    return this.grid;
  }

  /**
   * Extracts a List of translations from the image grid
   *
   * @param range                            the range (maximum dimension of the source image along
   *                                         the dir and dispValue)
   * @param filterLowCorrelationTranslations whether to filter out low correlation translations
   * @return List of Integers containing the relevant translations
   */
  protected List<Integer> getTranslationsFromGrid(double range, boolean filterLowCorrelationTranslations) {
    // allocate list to hold the translations
    List<Integer> translations = new ArrayList<Integer>();

    // gather all relevant translations into an array
    for (int row = 0; row < grid.getExtentHeight(); row++) {
      for (int col = 0; col < grid.getExtentWidth(); col++) {
        ImageTile<T> tile = grid.getSubGridTile(row, col);
        switch (direction) {
          case North:
            if (tile.getNorthTranslation() != null) {
              int t = 0;
              switch (displacementValue) {
                case X:
                  t = tile.getNorthTranslation().getX();
                  break;
                case Y:
                  t = tile.getNorthTranslation().getY();
                  break;
              }

              if (filterLowCorrelationTranslations) {
                if (t > 0 && t < range
                    && tile.getNorthTranslation().getCorrelation() >= TranslationFilter.CorrelationThreshold)
                  translations.add(t);
              } else {
                if (t > 0 && t < range)
                  translations.add(t);
              }
            }
            break;
          case West:
            if (tile.getWestTranslation() != null) {
              int t = 0;
              switch (displacementValue) {
                case X:
                  t = tile.getWestTranslation().getX();
                  break;
                case Y:
                  t = tile.getWestTranslation().getY();
                  break;
              }
              if (filterLowCorrelationTranslations) {
                if (t > 0 && t < range
                    && tile.getWestTranslation().getCorrelation() >= TranslationFilter.CorrelationThreshold)
                  translations.add(t);
              } else {
                if (t > 0 && t < range)
                  translations.add(t);
              }
            }
            break;
        }
      }
    }

    return translations;
  }


  /**
   * If an uncaught exception happens in a worker this notifies the parent.
   *
   * @param t worker thread
   * @param e throwable thrown
   */
  @Override
  public void uncaughtException(Thread t, Throwable e) {
    this.exceptionThrown = true;
    this.workerThrowable = e;
    this.cancel();
  }

  /**
   * Determine if an uncaught worker exception was thrown.
   *
   * @return whether or not any of the workers have thrown an uncaught exception.
   */
  public boolean isExceptionThrown() {
    return this.exceptionThrown;
  }

  /**
   * Get a reference to the worker exception thrown, if any.
   *
   * @return the exception thrown by a worker, or null.
   */
  public Throwable getWorkerThrowable() {
    return this.workerThrowable;
  }


}
