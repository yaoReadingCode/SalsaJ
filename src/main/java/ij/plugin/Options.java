package ij.plugin;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.io.*;

import java.awt.*;

/**
 *  This plugin implements most of the commands in the Edit/Options sub-menu.
 *
 *@author     Thomas
 *@created    3 decembre 2007
 */
public class Options implements PlugIn {

	/**
	 *  Main processing method for the Options object
	 *
	 *@param  arg  Description of the Parameter
	 */
	@Override
    public void run(String arg) {
		if ("misc".equals(arg)) {
			miscOptions();
        } else if ("line".equals(arg)) {
			lineWidth();
        } else if ("io".equals(arg)) {
			io();
        } else if ("conv".equals(arg)) {
			conversions();
        } else if ("display".equals(arg)) {
			appearance();
        }
	}

	// Miscellaneous Options
	/**
	 *  Description of the Method
	 */
	void miscOptions() {
	//EU_HOU Bundle
	String key = IJ.isMacintosh() ? "Command" : "Control";
	GenericDialog gd = new GenericDialog("Miscellaneous Options", IJ.getInstance());
		gd.addStringField("Divide by Zero Value:", "" + FloatBlitter.divideByZeroValue, 10);
		gd.addCheckbox("Use Pointer Cursor", Prefs.usePointerCursor);
		gd.addCheckbox("Hide \"Process Stack?\" Dialog", IJ.hideProcessStackDialog);
		//gd.addCheckbox("Antialiased_Text", Prefs.antialiasedText);
		gd.addCheckbox("Antialiased_Tool Icons", Prefs.antialiasedTools);
		gd.addCheckbox("Require " + key + " Key for Shortcuts", Prefs.requireControlKey);
		gd.addCheckbox("Debug Mode", IJ.debugMode);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}

	String divValue = gd.getNextString();
		if ("infinity".equalsIgnoreCase(divValue) || "infinite".equalsIgnoreCase(divValue)) {
			FloatBlitter.divideByZeroValue = Float.POSITIVE_INFINITY;
		} else if ("NaN".equalsIgnoreCase(divValue)) {
			FloatBlitter.divideByZeroValue = Float.NaN;
		} else if ("max".equalsIgnoreCase(divValue)) {
			FloatBlitter.divideByZeroValue = Float.MAX_VALUE;
		} else {
		Float f;
			try {
				f = new Float(divValue);
			} catch (NumberFormatException e) {
				f = null;
			}
			if (f != null) {
				FloatBlitter.divideByZeroValue = f;
			}
		}
		IJ.register(FloatBlitter.class);

		Prefs.usePointerCursor = gd.getNextBoolean();
		IJ.hideProcessStackDialog = gd.getNextBoolean();
	//Prefs.antialiasedText = gd.getNextBoolean();
	boolean antialiasedTools = gd.getNextBoolean();
	boolean change = antialiasedTools != Prefs.antialiasedTools;
		Prefs.antialiasedTools = antialiasedTools;
		if (change) {
			Toolbar.getInstance().repaint();
		}
		Prefs.requireControlKey = gd.getNextBoolean();
		IJ.debugMode = gd.getNextBoolean();
	}


	/**
	 *  Description of the Method
	 */
	void lineWidth() {
	int width = (int) IJ.getNumber("Line Width:", Line.getWidth());
		if (width == IJ.CANCELED) {
			return;
		}
		Line.setWidth(width);
	ImagePlus imp = WindowManager.getCurrentImage();
		if (imp != null && imp.isProcessor()) {
		ImageProcessor ip = imp.getProcessor();
			ip.setLineWidth(1);//Line.getWidth()
		Roi roi = imp.getRoi();
			if (roi != null && roi.isLine()) {
				imp.draw();
			}
		}
	}

	// Input/Output options
	/**
	 *  Description of the Method
	 */
	void io() {
	//EU_HOU Bundle
	GenericDialog gd = new GenericDialog("I/O Options");
		gd.addNumericField("JPEG Quality (0-100):", FileSaver.getJpegQuality(), 0, 3, "");
		gd.addNumericField("GIF Transparent Index (0-255):", GifWriter.getTransparentIndex(), 0, 3, "");
		gd.addStringField("File Extension for Tables:", Prefs.get("options.ext", ".xls"), 4);
		gd.addCheckbox("Use JFileChooser to Open/Save", Prefs.useJFileChooser);
		gd.addCheckbox("Export Raw in Intel Byte Order", Prefs.intelByteOrder);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}
	int quality = (int) gd.getNextNumber();
	int transparentIndex = (int) gd.getNextNumber();
		if (quality < 0) {
			quality = 0;
		}
		if (quality > 100) {
			quality = 100;
		}
		FileSaver.setJpegQuality(quality);
		GifWriter.setTransparentIndex(transparentIndex);
	String extension = gd.getNextString();
		if (!extension.startsWith(".")) {
			extension = "." + extension;
		}
		Prefs.set("options.ext", extension);
		Prefs.useJFileChooser = gd.getNextBoolean();
		Prefs.intelByteOrder = gd.getNextBoolean();
    }

	// Conversion Options
	/**
	 *  Description of the Method
	 */
	void conversions() {
	double[] weights = ColorProcessor.getWeightingFactors();
	boolean weighted = !(weights[0] == 1d / 3d && weights[1] == 1d / 3d && weights[2] == 1d / 3d);
	//boolean weighted = !(Math.abs(weights[0]-1d/3d)<0.0001 && Math.abs(weights[1]-1d/3d)<0.0001 && Math.abs(weights[2]-1d/3d)<0.0001);
	//EU_HOU Bundle
	GenericDialog gd = new GenericDialog("Conversion Options");
		gd.addCheckbox("Scale When Converting", ImageConverter.getDoScaling());
	String prompt = "Weighted RGB Conversions";
		if (weighted) {
			prompt += " (" + IJ.d2s(weights[0]) + "," + IJ.d2s(weights[1]) + "," + IJ.d2s(weights[2]) + ")";
		}
		gd.addCheckbox(prompt, weighted);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}
		ImageConverter.setDoScaling(gd.getNextBoolean());
		Prefs.weightedColor = gd.getNextBoolean();
		if (!Prefs.weightedColor) {
			ColorProcessor.setWeightingFactors(1d / 3d, 1d / 3d, 1d / 3d);
		} else if (Prefs.weightedColor && !weighted) {
			ColorProcessor.setWeightingFactors(0.299, 0.587, 0.114);
		}
    }


	/**
	 *  Description of the Method
	 */
	void appearance() {
	//EU_HOU Bundle
	GenericDialog gd = new GenericDialog("Appearance", IJ.getInstance());
		gd.addCheckbox("Interpolate Zoomed Images", Prefs.interpolateScaledImages);
		gd.addCheckbox("Open Images at 100%", Prefs.open100Percent);
		gd.addCheckbox("Black Canvas", Prefs.blackCanvas);
		gd.addCheckbox("No Image Border", Prefs.noBorder);
		gd.addCheckbox("Use Inverting Lookup Table", Prefs.useInvertingLut);
		gd.addCheckbox("Double Buffer Selections", Prefs.doubleBuffer);
		gd.addNumericField("Menu Font Size:", Menus.getFontSize(), 0, 3, "points");
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}
	boolean interpolate = gd.getNextBoolean();
		Prefs.open100Percent = gd.getNextBoolean();
	boolean blackCanvas = gd.getNextBoolean();
	boolean noBorder = gd.getNextBoolean();
	boolean useInvertingLut = gd.getNextBoolean();
		Prefs.doubleBuffer = gd.getNextBoolean();
	int menuSize = (int) gd.getNextNumber();
		if (interpolate != Prefs.interpolateScaledImages) {
			Prefs.interpolateScaledImages = interpolate;
		ImagePlus imp = WindowManager.getCurrentImage();
			if (imp != null) {
				imp.draw();
			}
		}
		if (blackCanvas != Prefs.blackCanvas) {
			Prefs.blackCanvas = blackCanvas;
		ImagePlus imp = WindowManager.getCurrentImage();
			if (imp != null) {
			ImageWindow win = imp.getWindow();
				if (win != null) {
					if (Prefs.blackCanvas) {
						win.setForeground(Color.white);
						win.setBackground(Color.black);
					} else {
						win.setForeground(Color.black);
						win.setBackground(Color.white);
					}
					imp.repaintWindow();
				}
			}
		}
		if (noBorder != Prefs.noBorder) {
			Prefs.noBorder = noBorder;
		ImagePlus imp = WindowManager.getCurrentImage();
			if (imp != null) {
				imp.repaintWindow();
			}
		}
		if (useInvertingLut != Prefs.useInvertingLut) {
			invertLuts(useInvertingLut);
			Prefs.useInvertingLut = useInvertingLut;
		}
		if (Prefs.doubleBuffer && IJ.isMacOSX()) {
			//EU_HOU Bundle
			IJ.error("Double-buffering is built into Mac OS X.");
			Prefs.doubleBuffer = false;
		}
		if (menuSize != Menus.getFontSize() && !IJ.isMacintosh()) {
			Menus.setFontSize(menuSize);
			//EU_HOU Bundle
			IJ.showMessage("Appearance", "Restart ImageJ to use the new font size");
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  useInvertingLut  Description of the Parameter
	 */
	void invertLuts(boolean useInvertingLut) {
	int[] list = WindowManager.getIDList();
		if (list == null) {
			return;
		}
        for (int aList : list) {
            ImagePlus imp = WindowManager.getImage(aList);
            if (imp == null) {
                return;
            }
            ImageProcessor ip = imp.getProcessor();
            if (useInvertingLut != ip.isInvertedLut() && !ip.isColorLut()) {
                ip.invertLut();
                int nImages = imp.getStackSize();
                if (nImages == 1) {
                    ip.invert();
                } else {
                    ImageStack stack2 = imp.getStack();
                    for (int slice = 1; slice <= nImages; slice++) {
                        stack2.getProcessor(slice).invert();
                    }
                    stack2.setColorModel(ip.getColorModel());
                }
            }
        }
	}

}

