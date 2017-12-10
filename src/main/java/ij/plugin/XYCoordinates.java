package ij.plugin;

import java.io.*;
import ij.*;
import ij.process.*;
import ij.io.*;
import ij.gui.*;

/**
 *  Writes the XY coordinates and pixel values of all non-background pixels to a
 *  tab-delimited text file. Backround is assumed to be the value of the pixel
 *  in the upper left corner of the image.
 *
 *@author     Thomas
 *@created    3 decembre 2007
 */
public class XYCoordinates implements PlugIn {

	static boolean processStack;


	/**
	 *  Main processing method for the XYCoordinates object
	 *
	 *@param  arg  Description of the Parameter
	 */
	@Override
    public void run(String arg) {
        System.out.println("XYCoordinates.run");
        ImagePlus imp = IJ.getImage();
        ImageProcessor ip = imp.getProcessor();
        int width = imp.getWidth();
        int height = imp.getHeight();
        double background = ip.getPixelValue(0, 0);
        String bg;
        if (ip instanceof ColorProcessor) {
            int c = ip.getPixel(0, 0);
            int r = (c & 0xff0000) >> 16;
            int g = (c & 0xff00) >> 8;
            int b = c & 0xff;
            bg = r + "," + g + "," + b;
        } else {
            if ((int) background == background) {
                bg = IJ.d2s(background, 0);
            } else {
                bg = "" + background;
            }
        }
        imp.killRoi();

        int slices = imp.getStackSize();
        //EU_HOU Bundle
        String msg =
                "This plugin writes to a text file the XY coordinates and\n"
                        + "pixel value of all non-background pixels. Backround\n"
                        + "is assumed to be the value of the pixel in the\n"
                        + "upper left corner of the image.\n"
                        + " \n"
                        + "    Width: " + width + "\n"
                        + "    Height: " + height + "\n"
                        + (slices > 1 ? "    Depth: " + slices + "\n" : "")
                        + "    Background value: " + bg + "\n";
//EU_HOU Bundle
        GenericDialog gd = new GenericDialog("Save XY Coordinates");
        gd.addMessage(msg);
        if (slices > 1) {
            //EU_HOU Bundle
            gd.addCheckbox("Process all " + slices + " images", processStack);
        }
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        processStack = slices > 1 && gd.getNextBoolean();
        if (!processStack) {
            slices = 1;
        }
//EU_HOU Bundle
        SaveDialog sd = new SaveDialog("Save Coordinates as Text...", imp.getTitle(), ".txt");
        String name = sd.getFileName();
        if (name == null) {
            return;
        }
        String directory = sd.getDirectory();
        PrintWriter pw = null;
        try {
            FileOutputStream fos = new FileOutputStream(directory + name);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            pw = new PrintWriter(bos);
        } catch (IOException e) {
            IJ.write("" + e);
            return;
        }
//EU_HOU Bundle
        IJ.showStatus("Saving coordinates...");
        int count = 0;
        float v;
        int c;
        int r;
        int g;
        int b;
        int type = imp.getType();
        ImageStack stack = imp.getStack();
        for (int z = 0; z < slices; z++) {
            if (slices > 1) {
                ip = stack.getProcessor(z + 1);
            }
            String zstr = slices > 1 ? z + "\t" : "";
            for (int y = height - 1; y >= 0; y--) {
                for (int x = 0; x < width; x++) {
                    v = ip.getPixelValue(x, y);
                    if (v != background) {
                        switch (type) {
                            case ImagePlus.GRAY32:
                                pw.println(x + "\t" + (height - 1 - y) + "\t" + zstr + v);
                                break;
                            case ImagePlus.COLOR_RGB:
                                c = ip.getPixel(x, y);
                                r = (c & 0xff0000) >> 16;
                                g = (c & 0xff00) >> 8;
                                b = c & 0xff;
                                pw.println(x + "\t" + (height - 1 - y) + "\t" + zstr + r + "\t" + g + "\t" + b);
                                break;
                            default:
                                pw.println(x + "\t" + (height - 1 - y) + "\t" + zstr + (int) v);
                                break;
                        }
                        count++;
                    }
                }// x
                if (slices == 1 && y % 10 == 0) {
                    IJ.showProgress((double) (height - y) / height);
                }
            }// y
            if (slices > 1) {
                IJ.showProgress(z + 1, slices);
            }
            String img = slices > 1 ? "-" + (z + 1) : "";
            IJ.log(imp.getTitle() + img + ": " + count + " pixels (" + IJ.d2s(count * 100.0 / (width * height)) + "%)\n");
            count = 0;
        }// z
        IJ.showProgress(1.0);
        IJ.showStatus("");
        pw.close();
    }

}

