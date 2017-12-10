package ij.plugin.frame;
import java.awt.*;
import java.awt.event.*;

import ij.*;
import ij.plugin.*;
import ij.gui.*;

/** Displays a window that allows the user to set the font, size and style. */
public class Fonts extends PlugInFrame implements PlugIn, ItemListener {

	private static String[] sizes = {"8","9","10","12","14","18","24","28","36","48","60","72"};
	private static int[] isizes = {8,9,10,12,14,18,24,28,36,48,60,72};
	private Panel panel;
	private Choice font;
	private Choice size;
	private Choice style;
	private Checkbox checkbox;
	private static Frame instance;

	public Fonts() {
		super("Fonts");
		if (instance!=null) {
			instance.toFront();
			return;
		}
		WindowManager.addWindow(this);
		instance = this;
		setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
		
		font = new Choice();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		String[] fonts = ge.getAvailableFontFamilyNames();
		font.add("SansSerif");
		font.add("Serif");
		font.add("Monospaced");
        for (String f : fonts) {
            if (!("SansSerif".equals(f) || "Serif".equals(f) || "Monospaced".equals(f))) {
                font.add(f);
            }
        }
		font.select(TextRoi.getFont());
		font.addItemListener(this);
		add(font);

		size = new Choice();
        for (String size1 : sizes) {
            size.add(size1);
        }
		size.select(getSizeIndex());
		size.addItemListener(this);
		add(size);
		
		style = new Choice();
		style.add("Plain");
		style.add("Bold");
		style.add("Italic");
		style.add("Bold+Italic");
		int i = TextRoi.getStyle();
		String s = "Plain";
        switch (i) {
            case Font.BOLD:
                s = "Bold";
                break;
            case Font.ITALIC:
                s = "Italic";
                break;
            case (Font.BOLD + Font.ITALIC):
                s = "Bold+Italic";
                break;
        }
		style.select(s);
		style.addItemListener(this);
		add(style);
		
		checkbox = new Checkbox("Smooth", TextRoi.isAntialiased());
		add(checkbox);
		checkbox.addItemListener(this);

		pack();
		GUI.center(this);
		show();
		IJ.register(Fonts.class);
	}
	
	int getSizeIndex() {
		int size = TextRoi.getSize();
		int index=0;
		for (int i=0; i<isizes.length; i++) {
			if (size>=isizes[i]) {
                index = i;
            }
		}
		return index;
	}
	
	@Override
    public void itemStateChanged(ItemEvent e) {
		String fontName = font.getSelectedItem();
		int fontSize = Integer.parseInt(size.getSelectedItem());
		String styleName = style.getSelectedItem();
		int fontStyle = Font.PLAIN;
		if ("Bold".equals(styleName)) {
            fontStyle = Font.BOLD;
        } else if ("Italic".equals(styleName)) {
            fontStyle = Font.ITALIC;
        } else if ("Bold+Italic".equals(styleName)) {
            fontStyle = Font.BOLD+Font.ITALIC;
        }
		TextRoi.setFont(fontName, fontSize, fontStyle, checkbox.getState());
		IJ.showStatus(fontSize+" point "+fontName + " " + styleName);
	}
	
    @Override
    public void windowClosing(WindowEvent e) {
		super.windowClosing(e);
		instance = null;
	}

}