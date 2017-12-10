package ij.plugin.filter;
import ij.*;
import ij.gui.*;
import ij.process.*;

/** This plugin implements the Invert, Smooth, Sharpen, Find Edges, 
	and Add Noise commands. */
public class Filters implements PlugInFilter {
	
	private static double sd = Prefs.getDouble(Prefs.NOISE_SD, 25.0);
	private String arg;
	private ImagePlus imp;
	private int slice;
	private boolean canceled;

	@Override
    public int setup(String arg, ImagePlus imp) {
		this.arg = arg;
		this.imp = imp;
		if (imp!=null) {
			Roi roi = imp.getRoi();
			if (roi!=null && !roi.isArea()) {
                imp.killRoi(); // ignore any line selection
            }
		}
		return IJ.setupDialog(imp, DOES_ALL-DOES_8C+SUPPORTS_MASKING);
	}

	@Override
    public void run(ImageProcessor ip) {
	
		if ("invert".equals(arg)) {
	 		ip.invert();
	 		return;
	 	}
	 	
		if ("smooth".equals(arg)) {
			ip.setSnapshotCopyMode(true);
	 		ip.smooth();
			ip.setSnapshotCopyMode(false);
	 		return;
	 	}
	 	
		if ("sharpen".equals(arg)) {
			ip.setSnapshotCopyMode(true);
	 		ip.sharpen();
			ip.setSnapshotCopyMode(false);
	 		return;
	 	}
	 	
		if ("edge".equals(arg)) {
			ip.setSnapshotCopyMode(true);
			ip.findEdges();
			ip.setSnapshotCopyMode(false);
	 		return;
		}
						
	 	if ("add".equals(arg)) {
	 		ip.noise(25.0);
	 		return;
	 	}
	 	
	 	if ("noise".equals(arg)) {
	 		if (canceled) {
                return;
            }
	 		slice++;
	 		if (slice==1) {
				GenericDialog gd = new GenericDialog("Gaussian Noise");
				gd.addNumericField("Standard Deviation:", sd, 2);
				gd.showDialog();
				if (gd.wasCanceled()) {
					canceled = true;
					return;
				}
				sd = gd.getNextNumber();
			}
	 		ip.noise(sd);
	 		IJ.register(Filters.class);
        }
        	 	
	}
	
	/** Returns the default standard deviation used by Process/Noise/Add Specified Noise. */
	public static double getSD() {
		return sd;
	}
	
}
