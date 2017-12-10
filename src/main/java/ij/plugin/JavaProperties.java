package ij.plugin;
import ij.*;
import ij.text.*;
import ij.io.OpenDialog;
import java.awt.*;
import java.util.*;
import java.applet.Applet;

/** Displays the Java system properties in a text window. */
public class JavaProperties implements PlugIn {

	StringBuffer sb = new StringBuffer();
	
	@Override
    public void run(String arg) {
		sb.append("\n");
		sb.append("Java properties applets can read:\n");
		show("java.version");
		show("java.vendor");
		if (IJ.isMacintosh()) {
            show("mrj.version");
        }
		show("os.name");
		show("os.version");
		show("os.arch");
		show("file.separator");
		show("path.separator");
		
		String s = System.getProperty("line.separator");
		char ch1, ch2;
		String str1, str2="";
		ch1 = s.charAt(0);
		if (ch1=='\r') {
            str1 = "<cr>";
        } else {
            str1 = "<lf>";
        }
		if (s.length()==2) {
			ch2 = s.charAt(1);
			if (ch2=='\r') {
                str2 = "<cr>";
            } else {
                str2 = "<lf>";
            }
		}
		sb.append("  line.separator: ").append(str1).append(str2).append("\n");
			
		Applet applet = IJ.getApplet();
		if (applet!=null) {
			sb.append("\n");
			sb.append("  code base: ").append(applet.getCodeBase()).append("\n");
			sb.append("  document base: ").append(applet.getDocumentBase()).append("\n");
			sb.append("  sample images dir: ").append(Prefs.getImagesURL()).append("\n");
			TextWindow tw = new TextWindow("Properties", new String(sb), 400, 400);
			return;
		}
		sb.append("\n");
		sb.append("Java properties only applications can read:\n");
		show("user.name");
		show("user.home");
		show("user.dir");
		show("user.country");
		show("file.encoding");
		show("java.home");
		show("java.compiler");
		show("java.class.path");
		show("java.ext.dirs");
		show("java.io.tmpdir");
		
		sb.append("\n");
		sb.append("Other properties:\n");
		String userDir = System.getProperty("user.dir");
		String userHome = System.getProperty("user.home");
		String osName = System.getProperty("os.name");
		sb.append("  IJ.getVersion: ").append(IJ.getVersion()).append("\n");
		sb.append("  IJ.isJava2: ").append(IJ.isJava2()).append("\n");
		sb.append("  IJ.isJava14: ").append(IJ.isJava14()).append("\n");
		sb.append("  IJ.isJava15: ").append(IJ.isJava15()).append("\n");
		sb.append("  IJ.isJava16: ").append(IJ.isJava16()).append("\n");
		sb.append("  IJ.isLinux: ").append(IJ.isLinux()).append("\n");
		sb.append("  IJ.isMacintosh: ").append(IJ.isMacintosh()).append("\n");
		sb.append("  IJ.isMacOSX: ").append(IJ.isMacOSX()).append("\n");
		sb.append("  IJ.isWindows: ").append(IJ.isWindows()).append("\n");
		sb.append("  IJ.isVista: ").append(IJ.isVista()).append("\n");
		sb.append("  Menus.getPlugInsPath: ").append(Menus.getPlugInsPath()).append("\n");
		sb.append("  Menus.getMacrosPath: ").append(Menus.getMacrosPath()).append("\n");
		sb.append("  Prefs.getHomeDir: ").append(Prefs.getHomeDir()).append("\n");
		sb.append("  Prefs.getThreads: ").append(Prefs.getThreads()).append(cores());
		sb.append("  Prefs.open100Percent: ").append(Prefs.open100Percent).append("\n");
		sb.append("  Prefs.blackBackground: ").append(Prefs.blackBackground).append("\n");
		sb.append("  Prefs.useJFileChooser: ").append(Prefs.useJFileChooser).append("\n");
		sb.append("  Prefs.weightedColor: ").append(Prefs.weightedColor).append("\n");
		sb.append("  Prefs.blackCanvas: ").append(Prefs.blackCanvas).append("\n");
		sb.append("  Prefs.pointAutoMeasure: ").append(Prefs.pointAutoMeasure).append("\n");
		sb.append("  Prefs.pointAutoNextSlice: ").append(Prefs.pointAutoNextSlice).append("\n");
		sb.append("  Prefs.requireControlKey: ").append(Prefs.requireControlKey).append("\n");
		sb.append("  Prefs.useInvertingLut: ").append(Prefs.useInvertingLut).append("\n");
		sb.append("  Prefs.antialiasedTools: ").append(Prefs.antialiasedTools).append("\n");
		sb.append("  Prefs.useInvertingLut: ").append(Prefs.useInvertingLut).append("\n");
		sb.append("  Prefs.intelByteOrder: ").append(Prefs.intelByteOrder).append("\n");
		sb.append("  Prefs.doubleBuffer: ").append(Prefs.doubleBuffer).append("\n");
		sb.append("  Prefs.noPointLabels: ").append(Prefs.noPointLabels).append("\n");
		sb.append("  Prefs.disableUndo: ").append(Prefs.disableUndo).append("\n");
		sb.append("  Prefs dir: ").append(Prefs.getPrefsDir()).append("\n");
		sb.append("  Current dir: ").append(OpenDialog.getDefaultDirectory()).append("\n");
		sb.append("  Sample images dir: ").append(Prefs.getImagesURL()).append("\n");
		Dimension d = IJ.getScreenSize();
		sb.append("  Screen size: ").append(d.width).append("x").append(d.height).append("\n");
		sb.append("  Memory in use: ").append(IJ.freeMemory()).append("\n");
		if (IJ.altKeyDown()) {
            doFullDump();
        }
		TextWindow tw = new TextWindow("Properties", new String(sb), 400, 500);
	}
	
	String cores() {
		int cores = Runtime.getRuntime().availableProcessors();
		if (cores==1) {
            return " (1 core)\n";
        } else {
            return " ("+cores+" cores)\n";
        }
	}
	
	void show(String property) {
		String p = System.getProperty(property);
		if (p!=null) {
            sb.append("  " + property + ": " + p+"\n");
        }
	}
	
	void doFullDump() {
		sb.append("\n");
		sb.append("All Properties:\n");
		Properties props = System.getProperties();
		for (Enumeration en=props.keys(); en.hasMoreElements();) {
			String key = (String)en.nextElement();
			sb.append("  ").append(key).append(": ").append((String) props.get(key)).append("\n");
		}
	}

}
