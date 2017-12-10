package ij.plugin;
import ij.*;
import ij.process.*;
import ij.gui.*;

/**
 *  Implements the conversion commands in the Image/Type submenu.
 *
 *@author     Thomas
 *@created    3 decembre 2007
 */
public class Converter implements PlugIn {

	/**
	 *  obsolete
	 */
	public static boolean newWindowCreated;
	private ImagePlus imp;


	/**
	 *  Main processing method for the Converter object
	 *
	 *@param  arg  Description of the Parameter
	 */
	@Override
    public void run(String arg) {
		imp = WindowManager.getCurrentImage();
		if (imp != null) {
			if (imp.lock()) {//v1.24f
				convert(arg);
				imp.unlock();
			}
		} else {
			IJ.noImage();
		}
	}


	/**
	 *  Converts the ImagePlus to the specified image type. The string argument
	 *  corresponds to one of the labels in the Image/Type submenu ("8-bit",
	 *  "16-bit", "32-bit", "8-bit Color", "RGB Color", "RGB Stack" or "HSB
	 *  Stack").
	 *
	 *@param  item  Description of the Parameter
	 */
	public void convert(String item) {
	int type = imp.getType();
	ImageStack stack = null;
		if (imp.getStackSize() > 1) {
			stack = imp.getStack();
		}
	//EU_HOU Bundle
	String msg = "Converting to " + item;
		IJ.showStatus(msg + "...");
	long start = System.currentTimeMillis();
	Roi roi = imp.getRoi();
		imp.killRoi();
	boolean saveChanges = imp.changes;
		imp.changes = IJ.getApplet() == null;//if not applet, set 'changes' flag
	ImageWindow win = imp.getWindow();
		try {
			if (stack != null) {
				// do stack conversions
				//EU_HOU Bundle
				if (stack.isRGB() && "RGB Color".equals(item)) {
					new ImageConverter(imp).convertRGBStackToRGB();
					if (win != null) {
						new ImageWindow(imp, imp.getCanvas());
					}// replace StackWindow with ImageWindow
				} else if (stack.isHSB() && "RGB Color".equals(item)) {
					new ImageConverter(imp).convertHSBToRGB();
					if (win != null) {
						new ImageWindow(imp, imp.getCanvas());
					}
					//EU_HOU Bundle
				} else if ("8-bit".equals(item)) {
					new StackConverter(imp).convertToGray8();
				} else if ("16-bit".equals(item)) {
					new StackConverter(imp).convertToGray16();
				} else if ("32-bit".equals(item)) {
					new StackConverter(imp).convertToGray32();
				} else if ("RGB Color".equals(item)) {
					new StackConverter(imp).convertToRGB();
				} else if ("8-bit Color".equals(item)) {
				int nColors = getNumber();
					if (nColors != 0) {
						new StackConverter(imp).convertToIndexedColor(nColors);
					}
				} else {
					throw new IllegalArgumentException();
				}
			} else {
				// do single image conversions
				Undo.setup(Undo.TYPE_CONVERSION, imp);
			ImageConverter ic = new ImageConverter(imp);
				//EU_HOU Bundle
				if ("8-bit".equals(item)) {
					ic.convertToGray8();
				} else if ("16-bit".equals(item)) {
					ic.convertToGray16();
				} else if ("32-bit".equals(item)) {
					ic.convertToGray32();
				} else if ("RGB Stack".equals(item)) {
					Undo.reset();// Reversible; no need for Undo
					ic.convertToRGBStack();
				} else if ("HSB Stack".equals(item)) {
					Undo.reset();
					ic.convertToHSB();
				} else if ("RGB Color".equals(item)) {
					ic.convertToRGB();
				} else if ("8-bit Color".equals(item)) {
				int nColors = getNumber();
					start = System.currentTimeMillis();
					if (nColors != 0) {
						ic.convertRGBtoIndexedColor(nColors);
					}
				} else {
					imp.changes = saveChanges;
					return;
				}
				IJ.showProgress(1.0);
			}

		} catch (IllegalArgumentException e) {
			unsupportedConversion(imp);
			IJ.showStatus("");
			Undo.reset();
			imp.changes = saveChanges;
			Menus.updateMenus();
			Macro.abort();
			return;
		}
		if (roi != null) {
			imp.setRoi(roi);
		}
		IJ.showTime(imp, start, "");
		imp.repaintWindow();
		Menus.updateMenus();
	}


	/**
	 *  Description of the Method
	 *
	 *@param  imp  Description of the Parameter
	 */
	void unsupportedConversion(ImagePlus imp) {
		IJ.error("Converter",
				"Supported Conversions:\n" +
				" \n" +
				"8-bit -> 16-bit*\n" +
				"8-bit -> 32-bit*\n" +
				"8-bit -> RGB Color*\n" +
				"16-bit -> 8-bit*\n" +
				"16-bit -> 32-bit*\n" +
				"16-bit -> RGB Color*\n" +
				"32-bit -> 8-bit*\n" +
				"32-bit -> 16-bit\n" +
				"32-bit -> RGB Color*\n" +
				"8-bit Color -> 8-bit (grayscale)*\n" +
				"8-bit Color -> RGB Color\n" +
				"RGB Color -> 8-bit (grayscale)*\n" +
				"RGB Color -> 8-bit Color*\n" +
				"RGB Color -> RGB Stack\n" +
				"RGB Color -> HSB Stack\n" +
				"RGB Stack -> RGB Color\n" +
				"HSB Stack -> RGB Color\n" +
				" \n" +
				"* works with stacks\n"
				);
	}


	/**
	 *  Gets the number attribute of the Converter object
	 *
	 *@return    The number value
	 */
	int getNumber() {
		if (imp.getType() != ImagePlus.COLOR_RGB) {
			return 256;
		}
	//EU_HOU Bundle
	GenericDialog gd = new GenericDialog("MedianCut");
		//EU_HOU Bundle
		gd.addNumericField("Number of Colors (2-256):", 256, 0);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return 0;
		}
	int n = (int) gd.getNextNumber();
		if (n < 2) {
			n = 2;
		}
		if (n > 256) {
			n = 256;
		}
		return n;
	}

}

