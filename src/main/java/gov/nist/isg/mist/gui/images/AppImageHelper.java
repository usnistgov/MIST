// NIST-developed software is provided by NIST as a public service. You may use, copy and distribute copies of the software in any medium, provided that you keep intact this entire notice. You may improve, modify and create derivative works of the software or any portion of the software, and you may copy and distribute such modifications or works. Modified works should carry a notice stating that you changed the software and should note the date and nature of any such change. Please explicitly acknowledge the National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing the software and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and the unavailability or interruption of operation. This software is not intended to be used in any situation where a failure could cause risk of injury or damage to property. The software developed by NIST employees is not subject to copyright protection within the United States.



package gov.nist.isg.mist.gui.images;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import gov.nist.isg.mist.gui.StitchingGuiUtils;

/**
 * App image helper provides utility classes for loading images for the stitching application
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class AppImageHelper {


  private AppImageHelper() {
  }

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
   * @param name       the filename of the image
   * @param reduceSize the amount to reduce the image size by
   * @return the reduced image size
   * @throws FileNotFoundException if the name does not exist
   * @throws IOException           if an IOexception occurs
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
   * @param name   the filename of the image
   * @param width  the desired width of the image
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
