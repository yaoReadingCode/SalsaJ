package ij.plugin.filter;
import ij.*;
import ij.process.*;
import ij.measure.Calibration;

/** Implements the flip and rotate commands in the Image/Transformations submenu. */
public class Transformer implements PlugInFilter {
	
	ImagePlus imp;
	String arg;

	@Override
	public int setup(String arg, ImagePlus imp) {
		this.arg = arg;
		this.imp = imp;
		if ("fliph".equals(arg) || "flipv".equals(arg)) {
			return IJ.setupDialog(imp, DOES_ALL+NO_UNDO);
		} else {
			return DOES_ALL+NO_UNDO+NO_CHANGES;
		}
	}

	@Override
	public void run(ImageProcessor ip) {

		if ("fliph".equals(arg)) {
			ip.flipHorizontal();
			return;
		}
		
		if ("flipv".equals(arg)) {
			ip.flipVertical();
			return;
		}
		
		if ("right".equals(arg) || "left".equals(arg)) {
	    	StackProcessor sp = new StackProcessor(imp.getStack(), ip);
	    	ImageStack s2 = null;
			if ("right".equals(arg)) {
				s2 = sp.rotateRight();
			} else {
				s2 = sp.rotateLeft();
			}
	    	Calibration cal = imp.getCalibration();
	    	imp.setStack(null, s2);
	    	double pixelWidth = cal.pixelWidth;
	    	cal.pixelWidth = cal.pixelHeight;
	    	cal.pixelHeight = pixelWidth;
		}
	}

}
