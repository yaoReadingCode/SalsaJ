package ij.plugin.filter;
import ij.*;
import ij.process.*;
import ij.io.*;


/** This plugin saves an image in tiff, gif, jpeg, bmp, png, text or raw format. */
public class Writer implements PlugInFilter {
	private String arg;
    private ImagePlus imp;
    
	@Override
    public int setup(String arg, ImagePlus imp) {
		this.arg = arg;
		this.imp = imp;
		return DOES_ALL+NO_CHANGES;
	}

	@Override
    public void run(ImageProcessor ip) {
		if ("tiff".equals(arg)) {
            new FileSaver(imp).saveAsTiff();
        } else if ("gif".equals(arg)) {
            new FileSaver(imp).saveAsGif();
        } else if ("jpeg".equals(arg)) {
            new FileSaver(imp).saveAsJpeg();
        } else if ("text".equals(arg)) {
            new FileSaver(imp).saveAsText();
        } else if ("lut".equals(arg)) {
            new FileSaver(imp).saveAsLut();
        } else if ("raw".equals(arg)) {
            new FileSaver(imp).saveAsRaw();
        } else if ("zip".equals(arg)) {
            new FileSaver(imp).saveAsZip();
        } else if ("bmp".equals(arg)) {
            new FileSaver(imp).saveAsBmp();
        } else if ("png".equals(arg)) {
            new FileSaver(imp).saveAsPng();
        } else if ("pgm".equals(arg)) {
            new FileSaver(imp).saveAsPgm();
        } else if ("fits".equals(arg)) {
            new FileSaver(imp).saveAsFits();
        }
	}
	
}


