# MIST
Microscopy Image Stitching Tool for Fiji was developed at the National Institute for Standards and Technology. Please use the quick navigation for installation instructions and the user guide. For more information about the tool please refer to the About MIST in the quick navigation.

## Quick Navigation

|[About MIST](https://isg.nist.gov/deepzoomweb/resources/csmet/pages/image_stitching/image_stitching.html)|[Wiki](https://github.com/NIST-ISG/MIST/wiki)|[Installation Guide](https://github.com/NIST-ISG/MIST/wiki/Install-Guide)|[User Guide](https://github.com/NIST-ISG/MIST/wiki/User-Guide)|Technical Documentation (Coming Soon)|
|----------------------------|--------------------|----------------------------|----------------------------|----------------------------|

## Applicable Problem Domain

MIST is designed to stitch 2D image datasets. Therefore each slice of a higher dimensional dataset is stitched together independently of all others with no information being carried between slices. For example, MIST can stitch a time series of 2D datasets (2D+time) where the image grid for each time slice it stitched together independently of all others. The problem of 3D volumetric stitching is not handled by MIST.


## Sample Data Sets

### 5x5 Image Tile Dataset

This dataset is a grid of 5x5 image tiles of a stem cell colony experiment. The images were acquired with 10% overlap in two imaging modalities: phase contrast and Cy5. The participants can choose which channel is their registration channel depending on the algorithm they are using. Phase contrast has more noise and artifacts in the background area while Cy5 does not. Grid tiling starts in the upper left corner and moves horizontally (x or column direction). The shift between rows is done by combing, meaning the next tile acquired after the 10th right tile on row i is the first tile on the left of row i+1. The naming convention of the files follows the one done by MicroManager in the format: file_name_r{rrr}_c{ccc}.tif where r{rrr} is the row number and c{ccc} is the column number of the tile in the 5x5 grid. For example: img_Cy5_r003_c004.tif is the image acquired using Cy5 and located on row 3, column 4 on the 5x5 grid.

[Cy5_ImageTiles.zip ~ 54 MB](../../wiki/testdata/Small_Fluorescent_Test_Dataset.zip)

[Phase_Image_Tiles.zip ~ 83 MB](../../wiki/testdata/Small_Phase_Test_Dataset.zip)

### 10x10 Image Tile Dataset

[2015 BioImage Informatics Conference: Image Stitching Challenge](https://isg.nist.gov/BII_2015/webPages/pages/stitching/Stitching.html)

This dataset is a grid of 10x10 image tiles of a stem cell colony experiment. The images were acquired with 10% overlap in two imaging modalities: phase contrast and Cy5. The participants can choose which channel is their registration channel depending on the algorithm they are using. Phase contrast has more noise and artifacts in the background area while Cy5 does not. Grid tiling starts in the upper left corner and moves horizontally (x or column direction). The shift between rows is done by combing, meaning the next tile acquired after the 10th right tile on row i is the first tile on the left of row i+1. The naming convention of the files follows the one done by MicroManager in the format: file_name_r{rrr}_c{ccc}.tif where r{rrr} is the row number and c{ccc} is the column number of the tile in the 10x10 grid. For example: img_Cy5_r009_c006.tif is the image acquired using Cy5 and located on row 9, column 6 on the 10x10 grid.

[Cy5_ImageTiles.zip ~ 119 MB](https://isg.nist.gov/BII_2015/Stitching/Cy5_Image_Tiles.zip)

[Phase_Image_Tiles.zip ~ 195 MB](https://isg.nist.gov/BII_2015/Stitching/Phase_Image_Tiles.zip)

<!--![Cy5 and Phase images](../../wiki/images/Cy5Phase.png)-->
![Cy5 and Phase images](../../wiki/images/InputWindow.png)
