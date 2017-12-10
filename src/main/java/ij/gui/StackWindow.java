//EU_HOU
package ij.gui;
import ij.*;
import ij.measure.Calibration;
import java.awt.*;
import java.awt.event.*;

/**
 *  This class is an extended ImageWindow used to display image stacks.
 *
 *@author     Thomas
 *@created    29 octobre 2007
 */
public class StackWindow extends ImageWindow implements Runnable, AdjustmentListener, ActionListener, MouseWheelListener {

	/**
	 *  Description of the Field
	 */
	protected Scrollbar channelSelector, sliceSelector, frameSelector;
	/**
	 *  Description of the Field
	 */
	protected Thread thread;
	/**
	 *  Description of the Field
	 */
	protected volatile boolean done;
	/**
	 *  Description of the Field
	 */
	protected volatile int slice;
	boolean viewIn5D;
	int nChannels = 1, nSlices = 1, nFrames = 1;
	int c = 1, z = 1, t = 1;


	/**
	 *  Constructor for the StackWindow object
	 *
	 *@param  imp  Description of the Parameter
	 */
	public StackWindow(ImagePlus imp) {
		this(imp, null);
	}


	/*
	 *  EU_HOU CHANGES
	 */
	/**
	 *  Constructor for the StackWindow object
	 *
	 *@param  imp              Description of the Parameter
	 *@param  accessibleImage  Description of the Parameter
	 */
	//EU_HOU Add
	public StackWindow(ImagePlus imp, boolean accessibleImage) {
		this(imp, new ImageCanvas(imp), accessibleImage);
	}


	/**
	 *  Constructor for the StackWindow object
	 *
	 *@param  imp  Description of the Parameter
	 *@param  ic   Description of the Parameter
	 */
	public StackWindow(ImagePlus imp, ImageCanvas ic) {
		this(imp, ic, true);
	}


	/*
	 *  EU_HOU END
	 */
	/**
	 *  Constructor for the StackWindow object
	 *
	 *@param  imp              Description of the Parameter
	 *@param  ic               Description of the Parameter
	 *@param  accessibleImage  Description of the Parameter
	 */
	public StackWindow(ImagePlus imp, ImageCanvas ic, boolean accessibleImage) {
		super(imp, ic, accessibleImage);
	// add slice selection slider
	ImageStack s = imp.getStack();
	int stackSize = s.getSize();
		nSlices = stackSize;
		viewIn5D = imp.getOpenAsHyperVolume();
		imp.setOpenAsHyperVolume(false);
		if (viewIn5D) {
		int[] dim = imp.getDimensions();
			nChannels = dim[2];
			nSlices = dim[3];
			nFrames = dim[4];
		}
		if (nSlices == stackSize) {
			viewIn5D = false;
		}
		addMouseWheelListener(this);
	ImageJ ij = IJ.getInstance();
		if (nChannels > 1) {
			channelSelector = new Scrollbar(Scrollbar.HORIZONTAL, 1, 1, 1, nChannels + 1);
		Panel panel = new Panel(new BorderLayout(2, 0));
			//panel.add(new Label("c"), BorderLayout.WEST);
			//panel.add(channelSelector, BorderLayout.CENTER);
			add(channelSelector);
			if (ij != null) {
				channelSelector.addKeyListener(ij);
			}
			channelSelector.addAdjustmentListener(this);
			channelSelector.setFocusable(false);// prevents scroll bar from blinking on Windows
			channelSelector.setUnitIncrement(1);
			channelSelector.setBlockIncrement(1);
		}
		if (nSlices > 1) {
			sliceSelector = new Scrollbar(Scrollbar.HORIZONTAL, 1, 1, 1, nSlices + 1);
			add(sliceSelector);
			if (ij != null) {
				sliceSelector.addKeyListener(ij);
			}
			sliceSelector.addAdjustmentListener(this);
			sliceSelector.setFocusable(false);
		int blockIncrement = nSlices / 10;
			if (blockIncrement < 1) {
				blockIncrement = 1;
			}
			sliceSelector.setUnitIncrement(1);
			sliceSelector.setBlockIncrement(blockIncrement);
		}
		if (nFrames > 1) {
			frameSelector = new Scrollbar(Scrollbar.HORIZONTAL, 1, 1, 1, nFrames + 1);
			add(frameSelector);
			if (ij != null) {
				frameSelector.addKeyListener(ij);
			}
			frameSelector.addAdjustmentListener(this);
			frameSelector.setFocusable(false);
		int blockIncrement = nFrames / 10;
			if (blockIncrement < 1) {
				blockIncrement = 1;
			}
			frameSelector.setUnitIncrement(1);
			frameSelector.setBlockIncrement(blockIncrement);
		}
		//IJ.log(nChannels+" "+nSlices+" "+nFrames);
		pack();
		show();
	int previousSlice = imp.getCurrentSlice();
		imp.setSlice(1);
		if (previousSlice > 1 && previousSlice <= stackSize) {
			imp.setSlice(previousSlice);
		}
		thread = new Thread(this, "SliceSelector");
		thread.start();
	}


	/**
	 *  Description of the Method
	 *
	 *@param  e  Description of the Parameter
	 */
	@Override
    public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
		if (!running2) {
			//slice = sliceSelector.getValue();
			if (e.getSource() == channelSelector) {
				c = channelSelector.getValue();
			} else if (e.getSource() == sliceSelector) {
				z = sliceSelector.getValue();
			} else if (e.getSource() == frameSelector) {
				t = frameSelector.getValue();
			}
			slice = (t - 1) * nChannels * nSlices + (z - 1) * nChannels + c;
			//IJ.log(slice+" "+c+" "+z+" "+t);
			notify();
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  e  Description of the Parameter
	 */
	@Override
    public void actionPerformed(ActionEvent e) {
	}


	/**
	 *  Description of the Method
	 *
	 *@param  event  Description of the Parameter
	 */
	@Override
    public void mouseWheelMoved(MouseWheelEvent event) {
		if (viewIn5D) {
			return;
		}
		synchronized (this) {
		int slice = imp.getCurrentSlice() + event.getWheelRotation();
			if (slice < 1) {
				slice = 1;
			} else if (slice > imp.getStack().getSize()) {
				slice = imp.getStack().getSize();
			}
			imp.setSlice(slice);
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
	@Override
    public boolean close() {
		if (!super.close()) {
			return false;
		}
		synchronized (this) {
			done = true;
			notify();
		}
		return true;
	}


	/**
	 *  Displays the specified slice and updates the stack scrollbar.
	 *
	 *@param  index  Description of the Parameter
	 */
	public void showSlice(int index) {
		if (index >= 1 && index <= imp.getStackSize()) {
			imp.setSlice(index);
		}
	}


	/**
	 *  Updates the stack scrollbar.
	 */
	public void updateSliceSelector() {
		if (viewIn5D) {
			return;
		}
	int stackSize = imp.getStackSize();
	int max = sliceSelector.getMaximum();
		if (max != (stackSize + 1)) {
			sliceSelector.setMaximum(stackSize + 1);
		}
		sliceSelector.setValue(imp.getCurrentSlice());
	}


	/**
	 *  Main processing method for the StackWindow object
	 */
	@Override
    public void run() {
		while (!done) {
			synchronized (this) {
				try {
					wait();
				} catch (InterruptedException ignored) {}
			}
			if (done) {
				return;
			}
			if (slice > 0) {
			int s = slice;
				slice = 0;
				if (s != imp.getCurrentSlice()) {
					imp.setSlice(s);
				}
			}
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
	@Override
    public String createSubtitle() {
	String s = super.createSubtitle();
		if (!viewIn5D) {
			return s;
		}
		s = "";
		if (nChannels > 1) {
			s += "c:" + c + "/" + nChannels;
			if (nSlices == 1 && nFrames == 1) {
				s += "; ";
			} else {
				s += " ";
			}
		}
		if (nSlices > 1) {
			s += "z:" + z + "/" + nSlices;
			if (nFrames == 1) {
				s += "; ";
			} else {
				s += " ";
			}
		}
		if (nFrames > 1) {
			s += "t:" + t + "/" + nFrames;
			s += "; ";
		}
		if (running2) {
			return s;
		}
	int type = imp.getType();
	Calibration cal = imp.getCalibration();
		if (cal.scaled()) {
			s += IJ.d2s(imp.getWidth() * cal.pixelWidth, 2) + "x" + IJ.d2s(imp.getHeight() * cal.pixelHeight, 2)
					 + " " + cal.getUnits() + " (" + imp.getWidth() + "x" + imp.getHeight() + "); ";
		} else {
			s += imp.getWidth() + "x" + imp.getHeight() + " pixels; ";
		}
	int size = (imp.getWidth() * imp.getHeight() * imp.getStackSize()) / 1024;
		switch (type) {
						case ImagePlus.GRAY8:
						case ImagePlus.COLOR_256:
							s += "8-bit";
							break;
						case ImagePlus.GRAY16:
							s += "16-bit";
							size *= 2;
							break;
						case ImagePlus.GRAY32:
							s += "32-bit";
							size *= 4;
							break;
						case ImagePlus.COLOR_RGB:
							s += "RGB";
							size *= 4;
							break;
		}
		if (imp.isInvertedLut()) {
			s += " (inverting LUT)";
		}
		if (size >= 10000) {
			s += "; " + (int) Math.round(size / 1024.0) + "MB";
		} else if (size >= 1024) {
		double size2 = size / 1024.0;
			s += "; " + IJ.d2s(size2, (int) size2 == size2 ? 0 : 1) + "MB";
		} else {
			s += "; " + size + "K";
		}
		return s;
	}


	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
	public boolean is5D() {
		return viewIn5D;
	}

}

