package ij.plugin.frame;
import java.awt.*;
import java.awt.event.*;
import ij.*;
import ij.plugin.*;

/**  This is a closeable window that plugins can extend. */
public class PlugInFrame extends Frame implements PlugIn, WindowListener, FocusListener {

	String title;
	
	public PlugInFrame(String title) {
		super(title);
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		this.title = title;
		ImageJ ij = IJ.getInstance();
		addWindowListener(this);
 		addFocusListener(this);
		if (IJ.isLinux()) {
            setBackground(ImageJ.backgroundColor);
        }
		if (ij!=null) {
			Image img = ij.getIconImage();
			if (img!=null) {
                try {setIconImage(img);} catch (Exception e) {}
            }
		}
	}
	
	@Override
    public void run(String arg) {
	}
	
    @Override
    public void windowClosing(WindowEvent e) {
    	if (e.getSource()==this) {
            close();
        }
    }
    
    /** Closes this window. */
    public void close() {
		setVisible(false);
		dispose();
		WindowManager.removeWindow(this);
    }

    @Override
    public void windowActivated(WindowEvent e) {
		if (IJ.isMacintosh() && IJ.getInstance()!=null) {
			IJ.wait(10); // may be needed for Java 1.4 on OS X
			setMenuBar(Menus.getMenuBar());
		}
		WindowManager.setWindow(this);
	}

	@Override
    public void focusGained(FocusEvent e) {
		//IJ.log("PlugInFrame: focusGained");
		WindowManager.setWindow(this);
	}

    @Override
    public void windowOpened(WindowEvent e) {}
    @Override
    public void windowClosed(WindowEvent e) {}
    @Override
    public void windowIconified(WindowEvent e) {}
    @Override
    public void windowDeiconified(WindowEvent e) {}
    @Override
    public void windowDeactivated(WindowEvent e) {}
	@Override
    public void focusLost(FocusEvent e) {}
}