# MIST
Microscopy Image Stitching Tool (MIST) is developed at the National Institute of Standards and Technology. The first release is an ImageJ/Fiji plugin-in. The next release will add a standalone tool. This repository contains source code for the plugin in one branch and the source code for the MATLAB prototype in another. Please use the quick navigation for installation instructions and the user guide. For more information about the tool please refer to the About MIST in the quick navigation. Note: the User Guide and Wiki are devoted to the ImageJ/Fiji plugin. The MATLAB source code contains its own help document in the 'doc' folder.

[Java Source Code](https://github.com/USNISTGOV/MIST/tree/master)

[MATLAB Source Code](https://github.com/USNISTGOV/MIST/tree/mist-matlab)

## Quick Navigation

#### - [About MIST](https://isg.nist.gov/deepzoomweb/resources/csmet/pages/image_stitching/image_stitching.html)
#### - [Wiki](https://github.com/USNISTGOV/MIST/wiki)
#### - [Installation Guide](https://github.com/USNISTGOV/MIST/wiki/Install-Guide)
#### - [User Guide](https://github.com/USNISTGOV/MIST/wiki/User-Guide)
#### - [Frequently Asked Questions](https://github.com/USNISTGOV/MIST/wiki/FAQ)
#### - [Technical Documentation](https://github.com/USNISTGOV/MIST/wiki/assets/mist-algorithm-documentation.pdf)

## Applicable Domain

MIST is designed to stitch 2D image datasets. It does not address volumetric or 3D stitching which requires a system to identify and correlate features across a third dimension (e.g., Z-axis). The tool has a facility for handling time-series data as a sequence of independent datasets.


## Sample Data Sets

We have two datasets: a 5x5 grid of image tiles and a 10x10 one.  The two datasets were acquired with 10% overlap between consecutive tiles and in two imaging modalities: phase contrast and Cy5 (a Fluorescent imaging modality with minimal background noise).  An example of each image is shown in Figure 1 below.  Grid tiling starts in the upper left corner and proceeds one row at a time from left to right; rows are ordered from top to bottom.  File names follow the pattern, `basename_r{rrr}_c{ccc}.tif`, where `r{rrr}` is the row number and `c{ccc}` the column number.  For example: `img_Cy5_r003_c004.tif` is the image acquired using Cy5 and located on row 3, column 4 on the 5x5 grid.


These datasets can be downloaded from the following links:

#### 5x5 Image Tile Dataset

#### 5x5 Image Tile Dataset

[Cy5_ImageTiles.zip ~ 32 MB](../../wiki/testdata/Small_Fluorescent_Test_Dataset.zip)

[Example Results: Cy5_ImageTiles.zip ~ 5 KB](../../wiki/testdata/Small_Fluorescent_Test_Dataset_Example_Results.zip)

[Phase_Image_Tiles.zip ~ 49 MB](../../wiki/testdata/Small_Phase_Test_Dataset.zip)

[Example Results: Phase_Image_Tiles.zip ~ 5 KB](../../wiki/testdata/Small_Phase_Test_Dataset_Example_Results.zip)


#### 10x10 Image Tile Dataset

[Cy5_ImageTiles.zip ~ 119 MB](https://isg.nist.gov/BII_2015/Stitching/Cy5_Image_Tiles.zip)

[Phase_Image_Tiles.zip ~ 195 MB](https://isg.nist.gov/BII_2015/Stitching/Phase_Image_Tiles.zip)

This dataset is hosted at and used for the [2015 BioImage Informatics Conference: Image Stitching Challenge](https://isg.nist.gov/BII_2015/webPages/pages/stitching/Stitching.html).

![Cy5 and Phase images](../../wiki/images/Cy5Phase.png)

Figure 1: Example Phase Contrast and Cy5 stitched images with auto adjusted contrast for visualization purposes.
