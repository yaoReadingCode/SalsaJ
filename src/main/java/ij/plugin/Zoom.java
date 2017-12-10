package ij.plugin;
import ij.*;
import ij.gui.*;

import java.awt.*;

/** This plugin implements the commands in the Image/Zoom submenu. */
public class Zoom implements PlugIn{

	/** 'arg' must be "in", "out", "100%" or "orig". */
	@Override
    public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null)
			{IJ.noImage(); return;}
		ImageCanvas ic = imp.getCanvas();
		if (ic==null) {
            return;
        }
		Point loc = ic.getCursorLoc();
		int x = ic.screenX(loc.x);
		int y = ic.screenY(loc.y);
    	if ("in".equals(arg)) {
    		if (IJ.altKeyDown()) {
                view100Percent(ic);
            } else {
				ic.zoomIn(x, y);
				if (ic.getMagnification()<=1.0) {
                    imp.repaintWindow();
                }
			}
    	} else if ("out".equals(arg)) {
			ic.zoomOut(x, y);
			if (ic.getMagnification()<1.0) {
                imp.repaintWindow();
            }
    	} else if ("orig".equals(arg)) {
            ic.unzoom();
        } else if ("100%".equals(arg)) {
            view100Percent(ic);
        } else if ("to".equals(arg)) {
            zoomToSelection(imp, ic);
        } else if ("max".equals(arg)) {
			ImageWindow win = imp.getWindow();
			win.setBounds(win.getMaximumBounds());
			win.maximize();
		}
	}
	
	void view100Percent(ImageCanvas ic) {
		Point loc = ic.getCursorLoc();
		if (!IJ.altKeyDown())
			{loc.x=0; loc.y=0;}
		while(ic.getMagnification()<1.0) {
            ic.zoomIn(loc.x, loc.y);
        }
		while(ic.getMagnification()>1.0) {
            ic.zoomOut(loc.x, loc.y);
        }
	}
	
	void zoomToSelection(ImagePlus imp, ImageCanvas ic) {
		Roi roi = imp.getRoi();
		ic.unzoom();
		if (roi==null) {
            return;
        }
		Rectangle w = imp.getWindow().getBounds();
		Rectangle r = roi.getBounds();
		double mag = ic.getMagnification();
		int marginw = (int)((w.width - mag * imp.getWidth()));
		int marginh = (int)((w.height - mag * imp.getHeight()));
		int x = r.x+r.width/2;
		int y = r.y+r.height/2;
		mag = ImageCanvas.getHigherZoomLevel(mag);
		while(r.width*mag<w.width - marginw && r.height*mag<w.height - marginh) {
			ic.zoomIn(ic.screenX(x), ic.screenY(y));
			double cmag = ic.getMagnification();
			if (cmag==32.0) {
                break;
            }
			mag = ImageCanvas.getHigherZoomLevel(cmag);
			w = imp.getWindow().getBounds();
		}
	}
	
}

