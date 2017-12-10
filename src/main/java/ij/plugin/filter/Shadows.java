package ij.plugin.filter;
import ij.*;
import ij.process.*;

/** Implements the commands in the Process/Shadows submenu. */
public class Shadows implements PlugInFilter {
	
	String arg;
	ImagePlus imp;

	@Override
    public int setup(String arg, ImagePlus imp) {
		this.arg = arg;
		this.imp = imp;
		if (imp!=null && imp.getStackSize()>1 && "demo".equals(arg))
			{IJ.error("This demo does not work with stacks."); return DONE;}
		return IJ.setupDialog(imp, DOES_ALL+SUPPORTS_MASKING);
	}

	@Override
    public void run(ImageProcessor ip) {
		if ("demo".equals(arg)) {
			IJ.resetEscape();
			while (!IJ.escapePressed()) {
				north(ip); imp.updateAndDraw(); ip.reset();
				northeast(ip); imp.updateAndDraw(); ip.reset();
				east(ip); imp.updateAndDraw(); ip.reset();
				southeast(ip); imp.updateAndDraw(); ip.reset();
				south(ip); imp.updateAndDraw(); ip.reset();
				southwest(ip); imp.updateAndDraw(); ip.reset();
				west(ip); imp.updateAndDraw(); ip.reset();
				northwest(ip); imp.updateAndDraw(); ip.reset();
			}
		}
		else if ("north".equals(arg)) {
            north(ip);
        } else if ("northeast".equals(arg)) {
            northeast(ip);
        } else if ("east".equals(arg)) {
            east(ip);
        } else if ("southeast".equals(arg)) {
            southeast(ip);
        } else if ("south".equals(arg)) {
            south(ip);
        } else if ("southwest".equals(arg)) {
            southwest(ip);
        } else if ("west".equals(arg)) {
            west(ip);
        } else if ("northwest".equals(arg)) {
            northwest(ip);
        }

	}
		
		
		public void north(ImageProcessor ip) {
			int[] kernel = {1,2,1, 0,1,0,  -1,-2,-1};
			ip.convolve3x3(kernel);
		}

		public void south(ImageProcessor ip) {
			int[] kernel = {-1,-2,-1,  0,1,0,  1,2,1};
			ip.convolve3x3(kernel);
		}

		public void east(ImageProcessor ip) {
			int[] kernel = {-1,0,1,  -2,1,2,  -1,0,1};
			ip.convolve3x3(kernel);
		}

		public void west(ImageProcessor ip) {
			int[] kernel = {1,0,-1,  2,1,-2,  1,0,-1};
			ip.convolve3x3(kernel);
		}

		public void northwest(ImageProcessor ip) {
			int[] kernel = {2,1,0,  1,1,-1,  0,-1,-2};
			ip.convolve3x3(kernel);
		}

		public void southeast(ImageProcessor ip) {
			int[] kernel = {-2,-1,0,  -1,1,1,  0,1,2};
			ip.convolve3x3(kernel);
		}
		
		public void northeast(ImageProcessor ip) {
			int[] kernel = {0,1,2,  -1,1,1,  -2,-1,0};
			ip.convolve3x3(kernel);
		}
		
		public void southwest(ImageProcessor ip) {
			int[] kernel = {0,-1,-2,  1,1,-1,  2,1,0};
			ip.convolve3x3(kernel);
		}
}
