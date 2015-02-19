# MIST
Microscopy Image Stitching Tool (MIST) is developed at the National Institute of Standards and Technology. The first release is an ImageJ/Fiji plugin-in. The next release will add a standalone tool. Please use the quick navigation for installation instructions and the user guide. For more information about the tool please refer to the About MIST in the quick navigation.

## Quick Navigation

|[About MIST](https://isg.nist.gov/deepzoomweb/resources/csmet/pages/image_stitching/image_stitching.html)|[Wiki](https://github.com/NIST-ISG/MIST/wiki)|[Installation Guide](https://github.com/NIST-ISG/MIST/wiki/Install-Guide)|[User Guide](https://github.com/NIST-ISG/MIST/wiki/User-Guide)|Technical Documentation (Coming Soon)|
|----------------------------|--------------------|----------------------------|----------------------------|----------------------------|

## Applicable Domain

MIST is designed to stitch 2D image datasets. Therefore each slice of a higher dimensional dataset is stitched together independently of all others with no information being carried between slices. For example, MIST can stitch a time series of 2D datasets (2D+time) where the image grid for each time slice it stitched together independently of all others. The problem of 3D volumetric stitching is not handled by MIST.


## Sample Data Sets



We have two datasets: a 5x5 grid of images and a 10x10 grid of images. These datasets were acquired with 10% overlap between consecutive tiles and in two imaging modalities: phase contrast and Cy5 (a Fluorescent imaging modality with minimal background noise). An example of each image is shown in Figure 1 below. Grid tiling starts in the upper left corner and moves horizontally (x or column direction). The shift between rows is done by combing, meaning in an mxn grid the next tile acquired after the nth right tile on row i is the first tile on the left of row i+1. The naming convention of the files follows the one done by MicroManager in the format: file_name_r{rrr}_c{ccc}.tif where r{rrr} is the row number and c{ccc} is the column number of the tile in the 5x5 grid. For example: img_Cy5_r003_c004.tif is the image acquired using Cy5 and located on row 3, column 4 on the 5x5 grid.

These datasets can be downloaded from the following links:

#### 5x5 Image Tile Dataset

[Cy5_ImageTiles.zip ~ 54 MB](../../wiki/testdata/Small_Fluorescent_Test_Dataset.zip)

[Phase_Image_Tiles.zip ~ 83 MB](../../wiki/testdata/Small_Phase_Test_Dataset.zip)

#### 10x10 Image Tile Dataset

[Cy5_ImageTiles.zip ~ 119 MB](https://isg.nist.gov/BII_2015/Stitching/Cy5_Image_Tiles.zip)

[Phase_Image_Tiles.zip ~ 195 MB](https://isg.nist.gov/BII_2015/Stitching/Phase_Image_Tiles.zip)

This dataset is hosted at and used for the [2015 BioImage Informatics Conference: Image Stitching Challenge](https://isg.nist.gov/BII_2015/webPages/pages/stitching/Stitching.html). 

![Cy5 and Phase images](../../wiki/images/Cy5Phase.png)

Figure 1: Example Phase Contrast and Cy5 stitched images with auto adjusted contrast for visualization purposes.
