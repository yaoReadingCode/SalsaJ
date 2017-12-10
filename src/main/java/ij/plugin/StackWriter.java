package ij.plugin;
import java.util.*;
import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.measure.Calibration;

/**
 *  Writes the slices of stack as separate files.
 *
 *@author     Thomas
 *@created    3 decembre 2007
 */
public class StackWriter implements PlugIn {

	//private static String defaultDirectory = null;
	private static String[] choices = {"BMP", "FITS", "GIF", "JPEG", "PGM", "PNG", "Raw", "Text", "TIFF", "ZIP"};
	private static String fileType = "TIFF";
	private static int ndigits = 4;
	private static int startAt;
	private static boolean useLabels;
	//private static boolean startAtZero;

	/**
	 *  Main processing method for the StackWriter object
	 *
	 *@param  arg  Description of the Parameter
	 */
	@Override
    public void run(String arg) {
	ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null || (imp != null && imp.getStackSize() < 2)) {
			//EU_HOU Bundle
			IJ.error("Stack Writer", "This command requires a stack.");
			return;
		}
	int stackSize = imp.getStackSize();
	String name = imp.getTitle();
	int dotIndex = name.lastIndexOf(".");
		if (dotIndex >= 0) {
			name = name.substring(0, dotIndex);
		}
//EU_HOU Bundle
	GenericDialog gd = new GenericDialog("Save Image Sequence");
		gd.addChoice("Format:", choices, fileType);
		gd.addStringField("Name:", name, 12);
		gd.addNumericField("Start At:", startAt, 0);
		gd.addNumericField("Digits (1-8):", ndigits, 0);
		gd.addCheckbox("Use Slice Labels as File Names", useLabels);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}
		fileType = gd.getNextChoice();
		name = gd.getNextString();
		startAt = (int) gd.getNextNumber();
		if (startAt < 0) {
			startAt = 0;
		}
		ndigits = (int) gd.getNextNumber();
		useLabels = gd.getNextBoolean();
	int number = 0;
		if (ndigits < 1) {
			ndigits = 1;
		}
		if (ndigits > 8) {
			ndigits = 8;
		}
	int maxImages = (int) Math.pow(10, ndigits);
		if (stackSize > maxImages && !useLabels) {
			//EU_HOU Bundle
			IJ.error("Stack Writer", "More than " + ndigits
					 + " digits are required to generate \nunique file names for " + stackSize + " images.");
			return;
		}
	String format = fileType.toLowerCase(Locale.US);
		if ("gif".equals(format) && !FileSaver.okForGif(imp)) {
			return;
		} else if ("fits".equals(format) && !FileSaver.okForFits(imp)) {
			return;
		}

		if ("text".equals(format)) {
			format = "text image";
		}
	String extension = "." + format;
		if ("tiff".equals(format)) {
			extension = ".tif";
		} else if ("text image".equals(format)) {
			extension = ".txt";
		}

	String digits = getDigits(number);
	//EU_HOU Bundle
	SaveDialog sd = new SaveDialog("Save Image Sequence", name + digits + extension, extension);
	String name2 = sd.getFileName();
		if (name2 == null) {
			return;
		}
	String directory = sd.getDirectory();

	ImageStack stack = imp.getStack();
	ImagePlus imp2 = new ImagePlus();
		imp2.setTitle(imp.getTitle());
	Calibration cal = imp.getCalibration();
	int nSlices = stack.getSize();
	String path;
	String label = null;
		for (int i = 1; i <= nSlices; i++) {
			//EU_HOU Bundle
			IJ.showStatus("writing: " + i + "/" + nSlices);
			IJ.showProgress((double) i / nSlices);
			imp2.setProcessor(null, stack.getProcessor(i));
		String label2 = stack.getSliceLabel(i);
			if (label2 != null && label2.contains("\n")) {
				imp2.setProperty("Info", label2);
			} else {
			Properties props = imp2.getProperties();
				if (props != null) {
					props.remove("Info");
				}
			}
			imp2.setCalibration(cal);
			digits = getDigits(number++);
			if (useLabels) {
				label = stack.getShortSliceLabel(i);
				if (label != null && "".equals(label)) {
					label = null;
				}
			}
			if (label == null) {
				path = directory + name + digits + extension;
			} else {
				path = directory + label + extension;
			}
			WindowManager.setTempCurrentImage(imp2);
			IJ.saveAs(format, path);
		}
		WindowManager.setTempCurrentImage(null);
		IJ.showStatus("");
		IJ.showProgress(1.0);
		IJ.register(StackWriter.class);
	}


	/**
	 *  Gets the digits attribute of the StackWriter object
	 *
	 *@param  n  Description of the Parameter
	 *@return    The digits value
	 */
	String getDigits(int n) {
	String digits = "00000000" + (startAt + n);
		return digits.substring(digits.length() - ndigits);
	}

}

