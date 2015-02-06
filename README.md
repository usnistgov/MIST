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

## Applicable Problem Domain

MIST is designed to stitch 2D image datasets. Therefore each slice of a higher dimensional dataset is stitched together independently of all others with no information being carried between slices. For example, MIST can stitch a time series of 2D datasets (2D+time) where the image grid for each time slice it stitched together independently of all others. The problem of 3D volumetric stitching is not handled by MIST. 
