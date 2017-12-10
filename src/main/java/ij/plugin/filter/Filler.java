package ij.plugin.filter;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.*;
import java.awt.*;

/**
 *  This plugin implements ImageJ's Fill, Clear, Clear Outside and Draw
 *  commands.
 *
 *@author     Thomas
 *@created    3 decembre 2007
 */
public class Filler implements PlugInFilter, Measurements {

	String arg;
	Roi roi;
	ImagePlus imp;
	int sliceCount;
	ImageProcessor mask;
	boolean isTextRoi;


	/**
	 *  Description of the Method
	 *
	 *@param  arg  Description of the Parameter
	 *@param  imp  Description of the Parameter
	 *@return      Description of the Return Value
	 */
	@Override
    public int setup(String arg, ImagePlus imp) {
		this.arg = arg;
		this.imp = imp;
		if (imp != null) {
			roi = imp.getRoi();
		}
		isTextRoi = roi != null && (roi instanceof TextRoi);
		IJ.register(Filler.class);
	int baseCapabilities = DOES_ALL + ROI_REQUIRED;
		if ("clear".equals(arg)) {
			if (isTextRoi || isLineSelection()) {
				return baseCapabilities;
			} else {
				return IJ.setupDialog(imp, baseCapabilities + SUPPORTS_MASKING);
			}
		} else if ("draw".equals(arg)) {
			return baseCapabilities;
		} else if ("label".equals(arg)) {
			if (Analyzer.firstParticle < Analyzer.lastParticle) {
				return baseCapabilities - ROI_REQUIRED;
			} else {
				return baseCapabilities;
			}
		} else if ("outside".equals(arg)) {
			return IJ.setupDialog(imp, baseCapabilities);
		} else {
			return IJ.setupDialog(imp, baseCapabilities + SUPPORTS_MASKING);
		}
	}


	/**
	 *  Main processing method for the Filler object
	 *
	 *@param  ip  Description of the Parameter
	 */
	@Override
    public void run(ImageProcessor ip) {
		if ("clear".equals(arg)) {
			clear(ip);
		} else if (isTextRoi && ("draw".equals(arg) || "fill".equals(arg))) {
			draw(ip);
		} else if ("fill".equals(arg)) {
			fill(ip);
		} else if ("draw".equals(arg)) {
			draw(ip);
		} else if ("label".equals(arg)) {
			label(ip);
		} else if ("outside".equals(arg)) {
			clearOutside(ip);
		}
	}


	/**
	 *  Gets the lineSelection attribute of the Filler object
	 *
	 *@return    The lineSelection value
	 */
	boolean isLineSelection() {
		return roi != null && roi.isLine();
	}


	/**
	 *  Gets the straightLine attribute of the Filler object
	 *
	 *@return    The straightLine value
	 */
	boolean isStraightLine() {
		return roi != null && roi.getType() == Roi.LINE;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  ip  Description of the Parameter
	 */
	public void clear(ImageProcessor ip) {
		ip.setColor(Toolbar.getBackgroundColor());
		if (isLineSelection()) {
			if (isStraightLine() && Line.getWidth() > 1) {
				ip.fillPolygon(roi.getPolygon());
			} else {
				roi.drawPixels();
			}
		} else {
			ip.fill();
		}// fill with background color
		ip.setColor(Toolbar.getForegroundColor());
	}


	/**
	 *  Description of the Method
	 *
	 *@param  ip  Description of the Parameter
	 */
	public void fill(ImageProcessor ip) {
		ip.setColor(Toolbar.getForegroundColor());
		if (isLineSelection()) {
			if (isStraightLine() && Line.getWidth() > 1) {
				ip.fillPolygon(roi.getPolygon());
			} else {
				roi.drawPixels(ip);
			}
		} else {
			ip.fill();
		}// fill with foreground color
	}


	/**
	 *  Description of the Method
	 *
	 *@param  ip  Description of the Parameter
	 */
	public void draw(ImageProcessor ip) {
		ip.setColor(Toolbar.getForegroundColor());
		roi.drawPixels();
		if (IJ.altKeyDown()) {
			drawLabel(ip);
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  ip  Description of the Parameter
	 */
	public void label(ImageProcessor ip) {
		if (Analyzer.getCounter() == 0) {
			//EU_HOU Bundle
			IJ.error("Label", "Measurement counter is zero");
			return;
		}
		if (Analyzer.firstParticle < Analyzer.lastParticle) {
			drawParticleLabels(ip);
		} else {
			ip.setColor(Toolbar.getForegroundColor());
		ImageCanvas ic = imp.getCanvas();
			if (ic != null) {
			double mag = ic.getMagnification();
				if (mag < 1.0) {
				int lineWidth = 1;
					lineWidth = (int) (lineWidth / mag);
					ip.setLineWidth(lineWidth);
				}
			}
			roi.drawPixels(ip);
			ip.setLineWidth(1);
			drawLabel(ip);
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  ip  Description of the Parameter
	 */
	void drawParticleLabels(ImageProcessor ip) {
	ResultsTable rt = ResultsTable.getResultsTable();
	int count = rt.getCounter();
	int first = Analyzer.firstParticle;
	int last = Analyzer.lastParticle;
		if (count == 0 || first >= count || last >= count) {
			return;
		}
		if (!rt.columnExists(ResultsTable.X_CENTROID)) {
			//EU_HOU Bundle
			IJ.error("Label", "\"Centroids\" required to label particles");
			return;
		}
		for (int i = first; i <= last; i++) {
		int x = (int) rt.getValueAsDouble(ResultsTable.X_CENTROID, i);
		int y = (int) rt.getValueAsDouble(ResultsTable.Y_CENTROID, i);
			drawLabel(imp, ip, i + 1, new Rectangle(x, y, 0, 0));
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  ip  Description of the Parameter
	 */
	void drawLabel(ImageProcessor ip) {
	int count = Analyzer.getCounter();
		if (count > 0 && roi != null) {
			drawLabel(imp, ip, count, roi.getBounds());
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  imp    Description of the Parameter
	 *@param  ip     Description of the Parameter
	 *@param  count  Description of the Parameter
	 *@param  r      Description of the Parameter
	 */
	public void drawLabel(ImagePlus imp, ImageProcessor ip, int count, Rectangle r) {
	Color foreground = Toolbar.getForegroundColor();
	Color background = Toolbar.getBackgroundColor();
		if (foreground.equals(background)) {
			foreground = Color.black;
			background = Color.white;
		}
	int size = 9;
	ImageCanvas ic = imp.getCanvas();
		if (ic != null) {
		double mag = ic.getMagnification();
			if (mag < 1.0) {
				size /= mag;
			}
		}
		if (size == 9 && r.width > 50 && r.height > 50) {
			size = 12;
		}
		ip.setFont(new Font("SansSerif", Font.PLAIN, size));
	String label = "" + count;
	int w = ip.getStringWidth(label);
	int x = r.x + r.width / 2 - w / 2;
	int y = r.y + r.height / 2 + Math.max(size / 2, 6);
	FontMetrics metrics = ip.getFontMetrics();
	int h = metrics.getHeight();
		ip.setColor(background);
		ip.setRoi(x - 1, y - h + 2, w + 1, h - 3);
		ip.fill();
		ip.resetRoi();
		ip.setColor(foreground);
		ip.drawString(label, x, y);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  ip  Description of the Parameter
	 */
	public synchronized void clearOutside(ImageProcessor ip) {
		if (isLineSelection()) {
			//EU_HOU Bundle
			IJ.error("\"Clear Outside\" does not work with line selections.");
			return;
		}
		sliceCount++;
	Rectangle r = ip.getRoi();
		if (mask == null) {
			makeMask(ip, r);
		}
		ip.setColor(Toolbar.getBackgroundColor());
	int stackSize = imp.getStackSize();
		if (stackSize > 1) {
			ip.snapshot();
		}
		ip.fill();
		ip.reset(mask);
	int width = ip.getWidth();
	int height = ip.getHeight();
		ip.setRoi(0, 0, r.x, height);
		ip.fill();
		ip.setRoi(r.x, 0, r.width, r.y);
		ip.fill();
		ip.setRoi(r.x, r.y + r.height, r.width, height - (r.y + r.height));
		ip.fill();
		ip.setRoi(r.x + r.width, 0, width - (r.x + r.width), height);
		ip.fill();
		ip.setRoi(r);// restore original ROI
		if (sliceCount == stackSize) {
			ip.setColor(Toolbar.getForegroundColor());
		Roi roi = imp.getRoi();
			imp.killRoi();
			imp.updateAndDraw();
			imp.setRoi(roi);
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  ip  Description of the Parameter
	 *@param  r   Description of the Parameter
	 */
	public void makeMask(ImageProcessor ip, Rectangle r) {
		mask = ip.getMask();
		if (mask == null) {
			mask = new ByteProcessor(r.width, r.height);
			mask.invert();
		} else {
			// duplicate mask (needed because getMask caches masks)
			mask = mask.duplicate();
		}
		mask.invert();
	}

}

