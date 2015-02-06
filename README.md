# MIST
Microscopy Image Stitching Tool for Fiji and standalone

## Fiji Installation

There are two methods for installing through Fiji: 

1. ImageJ update site
2. Manual installation

#### ImageJ update site

To add MIST to Fiji go to: 

1. **Help->Update Fiji->Manage update sites**. 
2. Locate MIST, and check the check box. 
3. Select **Apply changes**. 
4. Restart Fiji
5. You should now have the MIST stitching plugin in **Plugins->Stitching->MIST**.

#### Manual installation

1. Download the zip file from https://nist.gov/location/of/mist/zip 
2. Extract the contents of the zip file to the Fiji.app folder. This should add the necessary jars, libs, and plugin. 
3. Restart Fiji
4. You should now have the MIST stitching plugin in **Plugins->Stitching->MIST**.

#### Fiji Users Guide

https://nist.gov/path/to/userguide

##  Standalone Installation

The standalone version allows you to launch the application either through the command-line or through the included execute scripts. This version also supports headless systems that pass the stitching parameters as a paramter.

1. Download the zip file from https://nist.gov/location/of/mist/zip.
2. Extract contents to a folder
3. Double click run.bat for Windows users or run.sh for Linux/MacOS users

#### Command-line Users Guide

1. Download the zip file from https://nist.gov/location/of/mist/zip.
2. Extract contents to a folder
3. From command-line execute **./run.bat <params>** or **./run.sh <params>** ; Use --help to view list of parameters

## Sample Data Sets

[2015 BioImage Informatics Conference: Image Stitching Challenge](https://isg.nist.gov/BII_2015/webPages/pages/stitching/Stitching.html)

The challenge dataset is a grid of 10x10 image tiles of a stem cell colony experiment. The images were acquired with 10% overlap in two imaging modalities: phase contrast and Cy5. The participants can choose which channel is their registration channel depending on the algorithm they are using. Phase contrast has more noise and artifacts in the background area while Cy5 does not. Grid tiling starts in the upper left corner and moves horizontally (x or column direction). The shift between rows is done by combing, meaning the next tile acquired after the 10th right tile on row i is the first tile on the left of row i+1. The naming convention of the files follows the one done by MicroManager in the format: file_name_r{rrr}_c{ccc}.tif where r{rrr} is the row number and c{ccc} is the column number of the tile in the 10x10 grid. For example: img_Cy5_r009_c006.tif is the image acquired using Cy5 and located on row 9, column 6 on the 10x10 grid.

The results of any stitching algorithm will be submitted to the challenge chair in a csv file containing the image name and its global position within the stitching image. The global position csv file contains one image position per line formatted as such: “<image name>, <x coordinate>, <y coordinate>”. For example: “img_Cy5_r001_c001.tif, 0, 40”. An example global position file, “global-positions.txt”, is available for reference (see the URL link below).

[Cy5_ImageTiles.zip ~ 119 MB](https://isg.nist.gov/BII_2015/Stitching/Cy5_Image_Tiles.zip)

[Phase_Image_Tiles.zip ~ 195 MB](https://isg.nist.gov/BII_2015/Stitching/Phase_Image_Tiles.zip)

## Applicable Problem Domain

MIST is designed to stitch 2D image datasets. Therefore each slice of a higher dimensional dataset is stitched together independently of all others with no information being carried between slices. For example, MIST can stitch a time series of 2D datasets (2D+time) where the image grid for each time slice it stitched together independently of all others. The problem of 3D volumetric stitching is not handled by MIST. 
