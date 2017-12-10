package ij.plugin;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.Calibration;
import ij.macro.Interpreter;
import ij.io.FileInfo;

/**
 *  Implements the AddSlice, DeleteSlice and "Convert Windows to Stack"
 *  commands.
 *
 *@author     Thomas
 *@created    3 decembre 2007
 */
public class StackEditor implements PlugIn {
	final static int C = 0, Z = 1, T = 2;
	ImagePlus imp;
	int nSlices, width, height;


	/**
	 *  Main processing method for the StackEditor object
	 *
	 *@param  arg  Description of the Parameter
	 */
	@Override
    public void run(String arg) {
		imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.noImage();
			return;
		}
		nSlices = imp.getStackSize();
		width = imp.getWidth();
		height = imp.getHeight();

		if ("tostack".equals(arg)) {
			convertImagesToStack();
		} else if ("add".equals(arg)) {
			addSlice();
		} else if ("delete".equals(arg)) {
			deleteSlice();
		} else if ("toimages".equals(arg)) {
			convertStackToImages(imp);
		} else if ("stackto5d".equals(arg)) {
			convertStackTo5D(imp);
		} else if ("5dtostack".equals(arg)) {
			convert5DToStack(imp);
		}
	}


	/**
	 *  Adds a feature to the Slice attribute of the StackEditor object
	 */
	void addSlice() {
		if (!imp.lock()) {
			return;
		}
	int id = 0;
	ImageStack stack = imp.getStack();
		if (stack.getSize() == 1) {
		String label = stack.getSliceLabel(1);
			if (label != null && label.contains("\n")) {
				stack.setSliceLabel(null, 1);
			}
			id = imp.getID();
		}
	ImageProcessor ip = imp.getProcessor();
	int n = imp.getCurrentSlice();
		if (IJ.altKeyDown()) {
			n--;
		}// insert in front of current slice
		stack.addSlice(null, ip.createProcessor(width, height), n);
		imp.setStack(null, stack);
		imp.setSlice(n + 1);
		imp.unlock();
		if (id != 0) {
			IJ.selectWindow(id);
		}// prevents macros from failing
	}


	/**
	 *  Description of the Method
	 */
	void deleteSlice() {
		if (nSlices < 2) {
			//EU_HOU Bundle
			IJ.error("\"Delete Slice\" requires a stack");
			return;
		}
		if (!imp.lock()) {
			return;
		}
	ImageStack stack = imp.getStack();
	int n = imp.getCurrentSlice();
		stack.deleteSlice(n);
		imp.setStack(null, stack);
		if (n-- < 1) {
			n = 1;
		}
		imp.setSlice(n);
		imp.unlock();
	}


	/**
	 *  Description of the Method
	 */
	public void convertImagesToStack() {
	int[] wList = WindowManager.getIDList();
		if (wList == null) {
			//EU_HOU Bundle
			IJ.error(IJ.getPluginBundle().getString("NoImgErr"));
			return;
		}

	int count = 0;
	ImagePlus[] image = new ImagePlus[wList.length];
        for (int aWList : wList) {
            ImagePlus imp = WindowManager.getImage(aWList);
            if (imp.getStackSize() == 1) {
                image[count++] = imp;
            }
        }
		if (count < 2) {
			//EU_HOU Bundle
			IJ.error(IJ.getPluginBundle().getString("OneImgErr"));
			return;
		}

	Calibration cal2 = image[0].getCalibration();
		for (int i = 0; i < (count - 1); i++) {
			if (image[i].getType() != image[i + 1].getType()) {
				//EU_HOU Bundle
				IJ.error(IJ.getPluginBundle().getString("ImgTypeErr"));
				return;
			}
			if (image[i].getWidth() != image[i + 1].getWidth()
					 || image[i].getHeight() != image[i + 1].getHeight()) {
				//EU_HOU Bundle
				IJ.error(IJ.getPluginBundle().getString("ImgSizeErr"));
				return;
			}
		Calibration cal = image[i].getCalibration();
			if (!image[i].getCalibration().equals(cal2)) {
				cal2 = null;
			}
		}

	int width = image[0].getWidth();
	int height = image[0].getHeight();
	double min = Double.MAX_VALUE;
	double max = -Double.MAX_VALUE;
	ImageStack stack = new ImageStack(width, height);
	FileInfo fi = image[0].getOriginalFileInfo();
		if (fi != null && fi.directory == null) {
			fi = null;
		}
		for (int i = 0; i < count; i++) {
		ImageProcessor ip = image[i].getProcessor();
			if (ip.getMin() < min) {
				min = ip.getMin();
			}
			if (ip.getMax() > max) {
				max = ip.getMax();
			}
		String label = image[i].getTitle();
		String info = (String) image[i].getProperty("Info");
			if (info != null) {
				label += "\n" + info;
			}
			if (fi != null) {
			FileInfo fi2 = image[i].getOriginalFileInfo();
				if (fi2 != null && !fi.directory.equals(fi2.directory)) {
					fi = null;
				}
			}
			stack.addSlice(label, ip);
			image[i].changes = false;
			image[i].close();
		}
	//EU_HOU Bundle
	ImagePlus imp = new ImagePlus(IJ.getPluginBundle().getString("StackTitle"), stack);
		if (imp.getType() == ImagePlus.GRAY16 || imp.getType() == ImagePlus.GRAY32) {
			imp.getProcessor().setMinAndMax(min, max);
		}
		if (cal2 != null) {
			imp.setCalibration(cal2);
		}
		if (fi != null) {
			fi.fileName = "";
			fi.nImages = imp.getStackSize();
			imp.setFileInfo(fi);
		}
		imp.show();
	}


	/**
	 *  Description of the Method
	 *
	 *@param  imp  Description of the Parameter
	 */
	public void convertStackToImages(ImagePlus imp) {
		if (nSlices < 2) {
			//EU_HOU Bundle
			IJ.error("\"Convert Stack to Images\" requires a stack");
			return;
		}
		if (!imp.lock()) {
			return;
		}
	ImageStack stack = imp.getStack();
	int size = stack.getSize();
		if (size > 30 && !IJ.macroRunning()) {
		//EU_HOU Bundle
		boolean ok = IJ.showMessageWithCancel("Convert to Images?",
					"Are you sure you want to convert this\nstack to "
					 + size + " separate windows?");
			if (!ok) {
				imp.unlock();
				return;
			}
		}
	Calibration cal = imp.getCalibration();
	CompositeImage cimg = imp instanceof CompositeImage ? (CompositeImage) imp : null;
		for (int i = 1; i <= size; i++) {
		String label = stack.getShortSliceLabel(i);
		String title = label != null && !"".equals(label) ? label : getTitle(imp, i);
		ImageProcessor ip = stack.getProcessor(i);
			if (cimg != null) {
				ip.setMinAndMax(cimg.getMin(i), cimg.getMax(i));
			}
		ImagePlus imp2 = new ImagePlus(title, ip);
			imp2.setCalibration(cal);
			imp2.show();
		}
		imp.changes = false;
	ImageWindow win = imp.getWindow();
		if (win != null) {
			win.close();
		} else if (Interpreter.isBatchMode()) {
			Interpreter.removeBatchModeImage(imp);
		}
		imp.unlock();
	}


	/**
	 *  Gets the title attribute of the StackEditor object
	 *
	 *@param  imp  Description of the Parameter
	 *@param  n    Description of the Parameter
	 *@return      The title value
	 */
	String getTitle(ImagePlus imp, int n) {
	String digits = "00000000" + n;
		return imp.getShortTitle() + "-" + digits.substring(digits.length() - 4, digits.length());
	}


	/**
	 *  Displays the current stack in a 5D window. Based on the Stack_to_Image5D
	 *  class in Joachim Walter's Image5D plugin.
	 *
	 *@param  imp  Description of the Parameter
	 */
	void convertStackTo5D(ImagePlus imp) {
	int nChannels = imp.getNChannels();
	int nSlices = imp.getNSlices();
	int nFrames = imp.getNFrames();
	int stackSize = imp.getStackSize();
		if (stackSize == 1) {
			//EU_HOU Bundle
			IJ.error("Stack to 5D", "Stack required");
			return;
		}

	// The choices that are initially displayed:
	// c, z, t and the respective dimension sizes.
	int first = C;

	// The choices that are initially displayed:
	// c, z, t and the respective dimension sizes.
	int middle = Z;

	// The choices that are initially displayed:
	// c, z, t and the respective dimension sizes.
	int last = T;
	int nFirst = nChannels;
	int nMiddle = nSlices;
	int nLast = nFrames;

		// Different choices, if only one dimension is >1
		if (nChannels <= 1 && nSlices <= 1 && nFrames > 1) {
			first = T;
			middle = Z;
			last = C;
			nFirst = stackSize;
			nMiddle = 1;
			nLast = 1;
		} else if (nChannels <= 1 && nFrames <= 1 && nSlices > 1) {
			first = Z;
			middle = C;
			last = T;
			nFirst = stackSize;
			nMiddle = 1;
			nLast = 1;
		}

	String[] dimensions = new String[]{"c", "z", "t"};
	boolean goOn = true;
		do {
			goOn = true;
		//EU_HOU Bundle
		GenericDialog gd = new GenericDialog("Convert Stack to 5D");
			gd.addChoice("3rd Dimension:", dimensions, dimensions[first]);
			gd.addChoice("4th Dimension:", dimensions, dimensions[middle]);
			gd.addNumericField("3rd_Dimension_Size:", nFirst, 0, 8, "");
			gd.addNumericField("4th_Dimension_Size:", nMiddle, 0, 8, "");
			gd.showDialog();
			if (gd.wasCanceled()) {
				return;
			}

			first = gd.getNextChoiceIndex();
			middle = gd.getNextChoiceIndex();
			nFirst = (int) gd.getNextNumber();
			nMiddle = (int) gd.getNextNumber();
			if (first == middle) {
				//EU_HOU Bundle
				IJ.error("Please do not select two identical dimensions!");
				goOn = false;
				continue;
			}

		// Determine type of third dimension.
		boolean[] thirdChoice = {true, true, true};
			thirdChoice[first] = false;
			thirdChoice[middle] = false;
			for (int i = 0; i < 3; i++) {
				if (thirdChoice[i]) {
					last = i;
					break;
				}
			}

		double dLast = (double) stackSize / (double) nFirst / (double) nMiddle;
			nLast = (int) dLast;
			if (nLast != dLast) {
				//EU_HOU Bundle
				IJ.error("channels*slices*frames!=stackSize");
				goOn = false;
            }
		} while (!goOn);

		nChannels = 1;
		nSlices = 1;
		nFrames = 1;
		switch (first) {
						case 0:
							nChannels = nFirst;
							break;
						case 1:
							nSlices = nFirst;
							break;
						case 2:
							nFrames = nFirst;
							break;
		}
		switch (middle) {
						case 0:
							nChannels = nMiddle;
							break;
						case 1:
							nSlices = nMiddle;
							break;
						case 2:
							nFrames = nMiddle;
							break;
		}
		switch (last) {
						case 0:
							nChannels = nLast;
							break;
						case 1:
							nSlices = nLast;
							break;
						case 2:
							nFrames = nLast;
							break;
		}

	Object[] images1 = imp.getStack().getImageArray();
	Object[] images2 = new Object[images1.length];
		System.arraycopy(images1, 0, images2, 0, images1.length);
	int[] index = new int[3];
		for (index[2] = 0; index[2] < nFrames; ++index[2]) {
			for (index[1] = 0; index[1] < nSlices; ++index[1]) {
				for (index[0] = 0; index[0] < nChannels; ++index[0]) {
				//img5d.setCurrentPosition(0, 0, index[0], index[1], index[2]);
				int dstIndex = index[0] + index[1] * nChannels + index[2] * nChannels * nSlices;
				int srcIndex = index[first] + index[middle] * nFirst + index[last] * nFirst * nMiddle;
					//img5d.setPixels(imp.getStack().getPixels(stackPosition));
					images1[dstIndex] = images2[srcIndex];
				}
			}
		}
		imp.setDimensions(index[0], index[1], index[2]);
		imp.setOpenAsHyperVolume(true);
		new StackWindow(imp);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  imp  Description of the Parameter
	 */
	void convert5DToStack(ImagePlus imp) {
		imp.setOpenAsHyperVolume(false);
		new StackWindow(imp);
	}

}

