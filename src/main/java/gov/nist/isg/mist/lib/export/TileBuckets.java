package gov.nist.isg.mist.lib.export;


import gov.nist.isg.mist.lib.imagetile.ImageTile;
import gov.nist.isg.mist.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverser;
import gov.nist.isg.mist.lib.tilegrid.traverser.TileGridTraverserFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Class to store ImageTiles that overlap with tiles during export
 *
 * @author Derek Juba
 * @version 1.0
 */
public class TileBuckets<T> {
    private int numTileRows;
    private int numTileCols;
    private int tileHeight;
    private int tileWidth;
    private int searchHeight;
    private int searchWidth;
    private List<List<List<ImageTile<T>>>> tileBuckets;
    private boolean withOverlap;

    /**
     * Initializes the TileBuckets data structure
     * @param numTileRows the number of rows in the grid of tiles
     * @param numTileCols the number of columns in the grid of tiles
     * @param tileHeight the height of a tile
     * @param tileWidth the width of a tile
     * @param withOverlap Whether there is overlap or not
     */
    public TileBuckets(int numTileRows, int numTileCols, int tileHeight, int tileWidth, boolean withOverlap) {
        this.numTileRows = numTileRows;
        this.numTileCols = numTileCols;

        this.tileHeight = tileHeight;
        this.tileWidth  = tileWidth;

        this.withOverlap = withOverlap;

        this.searchHeight = 0;
        this.searchWidth  = 0;

        // Allocate row array
        this.tileBuckets = new ArrayList<List<List<ImageTile<T>>>>(numTileRows);

        // Allocate col arrays
        for (int tileRow = 0; tileRow < numTileRows; ++tileRow) {
            this.tileBuckets.add(new ArrayList<List<ImageTile<T>>>(numTileCols));
        }

        // Allocate tile arrays
        for (int tileRow = 0; tileRow < numTileRows; ++tileRow) {
            for (int tileCol = 0; tileCol < numTileCols; ++tileCol) {
                this.tileBuckets.get(tileRow).add(new ArrayList<ImageTile<T>>());
            }
        }
    }

    /**
     * Adds all ImageTiles in a grid to their appropriate buckets
     * @param grid the grid of ImageTiles
     */
    public void addTiles(TileGrid<ImageTile<T>> grid) {
        // Compute search radius

        ImageTile<T> existingTile = grid.getTileThatExists();
        existingTile.readTile();

        this.searchHeight = (int)Math.ceil((double)existingTile.getHeight() / this.tileHeight);
        this.searchWidth  = (int)Math.ceil((double)existingTile.getWidth()  / this.tileWidth);

        // Add tiles to buckets

        // for each image tile get the region ...
        TileGridTraverser<ImageTile<T>> traverser =
                TileGridTraverserFactory.makeTraverser(TileGridTraverser.Traversals.ROW, grid);

        int imageTileWidth = existingTile.getWidth();
        int imageTileHeight = existingTile.getHeight();

        for (ImageTile<T> tile : traverser) {

            int tileRow = 0;
            int tileCol = 0;

            if (withOverlap) {
                tileRow = tile.getAbsYPos() / this.tileHeight;
                tileCol = tile.getAbsXPos() / this.tileWidth;
            } else {
                tileRow = tile.getRow() * imageTileHeight / this.tileHeight;
                tileCol = tile.getCol() * imageTileWidth / this.tileWidth;
            }

            this.tileBuckets.get(tileRow).get(tileCol).add(tile);
        }
    }

    /**
     * Gets list of ImageTiles that overlap in the specified row column bucket
     * @param tileRow the row
     * @param tileCol the column
     * @return the list of ImageTiles that overlap
     */
    public List<ImageTile<T>> getPotentialOverlapTiles(int tileRow, int tileCol) {
        int bucketRowStart = tileRow - this.searchHeight;
        int bucketColStart = tileCol - this.searchWidth;

        // All possible overlapping tiles are to the North and West, so do not search South or East
        int bucketRowEnd = tileRow + 1;
        int bucketColEnd = tileCol + 1;

        if (bucketRowStart < 0) bucketRowStart = 0;
        if (bucketColStart < 0) bucketColStart = 0;

        if (bucketRowEnd > numTileRows) bucketRowEnd = numTileRows;
        if (bucketColEnd > numTileCols) bucketColEnd = numTileCols;

        List<ImageTile<T>> tiles = new ArrayList<ImageTile<T>>();

        for (int bucketRow = bucketRowStart; bucketRow < bucketRowEnd; ++bucketRow) {
            for (int bucketCol = bucketColStart; bucketCol < bucketColEnd; ++bucketCol) {
                tiles.addAll(tileBuckets.get(bucketRow).get(bucketCol));
            }
        }

        // Sorts tiles  from smallest correlation tile to largest correlation tile
        // This enables painting the highest correlation tiles last.
        Collections.sort(tiles, new Comparator<ImageTile<T>>() {

            @Override
            public int compare(ImageTile<T> t1, ImageTile<T> t2) {
                return Double.compare(t1.getTileCorrelation(), t2.getTileCorrelation());
            }

        });

        return tiles;
    }

}
