/*
 * ================================================================
 * 
 * Disclaimer: IMPORTANT: This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of their official duties.
 * Pursuant to title 17 Section 105 of the United States Code this software is not subject to
 * copyright protection and is in the public domain. This is an experimental system. NIST assumes no
 * responsibility whatsoever for its use by other parties, and makes no guarantees, expressed or
 * implied, about its quality, reliability, or any other characteristic. We would appreciate
 * acknowledgement if the software is used. This software can be redistributed and/or modified
 * freely provided that any derivative works bear some notice that they are derived from it, and any
 * modified versions bear some notice that they have been modified.
 * 
 * ================================================================
 * 
 * ================================================================
 * 
 * Author: dan1 Date: Aug 16, 2013 1:27:45 PM EST
 * 
 * Time-stamp: <Aug 16, 2013 1:27:45 PM dan1>
 * 
 * ================================================================
 */

package gov.nist.isg.mist.stitching.gui.images;

import gov.nist.isg.mist.stitching.gui.StitchingGuiUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

/**
 * App image helper provides utility classes for loading images for the stitching application
 * 
 * @author Tim Blattner
 * @version 1.0
 * 
 */
public class AppImageHelper {

  
  private AppImageHelper() {}

  /**
   * Loads the specified image into an image icon
   * 
   * @param name the filename of the image
   * @return the image icon
   * @throws FileNotFoundException if name does not exist
   */
  public static ImageIcon loadImage(String name) throws FileNotFoundException {
    ImageIcon image = null;
    URL url = StitchingGuiUtils.getFigureResource(name);
    if (url != null) {
      java.awt.Image img = java.awt.Toolkit.getDefaultToolkit().createImage(url);
      if (img != null) {
        image = new ImageIcon(img);
      }
    }

    if (image == null)
      throw new FileNotFoundException("ERROR: Loading image " + name + " not found.");

    return image;
  }


  /**
   * Loads the specified image and reduces the size by the given amount.
   * 
   * @param name the filename of the image
   * @param reduceSize the amount to reduce the image size by
   * @return the reduced image size
   * @throws FileNotFoundException if the name does not exist
   * @throws IOException if an IOexception occurs
   */
  public static ImageIcon loadImage(String name, double reduceSize) throws FileNotFoundException,
      IOException {
    ImageIcon image = null;
    URL url = StitchingGuiUtils.getFigureResource(name);
    if (url != null) {
      BufferedImage bi = ImageIO.read(url);

      java.awt.Image img = java.awt.Toolkit.getDefaultToolkit().createImage(url);
      if (img != null && bi != null) {
        img =
            img.getScaledInstance((int) (bi.getWidth() * reduceSize),
                (int) (bi.getHeight() * reduceSize), Image.SCALE_AREA_AVERAGING);
        image = new ImageIcon(img);


      }
    }

    if (image == null)
      throw new FileNotFoundException("ERROR: Loading image " + name + " not found.");


    return image;
  }

  /**
   * Loads the specified image and resizes to a given width and height
   * 
   * @param name the filename of the image
   * @param width the desired width of the image
   * @param height the desired height of the image
   * @return the resized image
   * @throws FileNotFoundException if the name does not exist
   */
  public static ImageIcon loadImage(String name, int width, int height)
      throws FileNotFoundException {
    ImageIcon image = null;
    URL url = StitchingGuiUtils.getFigureResource(name);
    if (url != null) {
      java.awt.Image img = java.awt.Toolkit.getDefaultToolkit().createImage(url);
      if (img != null) {
        img = img.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING);
        image = new ImageIcon(img);

      }
    }

    if (image == null)
      throw new FileNotFoundException("ERROR: Loading image " + name + " not found.");


    return image;
  }
}
