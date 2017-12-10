package ij.plugin;
import ij.*;
import ij.gui.*;
import ij.io.*;
import ij.util.*;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

/**
 *  Implements the Plugins/Shortcuts/Install... command.
 *
 *@author     Thomas
 *@created    3 decembre 2007
 */
public class Installer implements PlugIn {

	private static String[] menus = {"Shortcuts", "Plugins", "Import", "Save As",
			"Filters", "Tools", "Utilities"};
	private final static String TITLE = "Installer";
	private static String command = "";
	private static String shortcut = "";
	private static String defaultPlugin = "";
	private static String menuStr = menus[0];


	/**
	 *  Main processing method for the Installer object
	 *
	 *@param  arg  Description of the Parameter
	 */
	@Override
    public void run(String arg) {
		installPlugin();
	}


	/**
	 *  Description of the Method
	 */
	void installPlugin() {
	String[] plugins = getPlugins();
		if (plugins == null || plugins.length == 0) {
			//EU_HOU Bundle
			IJ.error("No plugins found");
			return;
		}
	//EU_HOU Bundle
	GenericDialog gd = new GenericDialog("Install Plugin", IJ.getInstance());
		gd.addChoice("Plugin:", plugins, defaultPlugin);
		gd.addChoice("Menu:", menus, menuStr);
		gd.addStringField("Command:", command, 16);
		gd.addStringField("Shortcut:", shortcut, 3);
		gd.addStringField("Argument:", "", 12);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}
	String plugin = gd.getNextChoice();
		menuStr = gd.getNextChoice();
		command = gd.getNextString();
		shortcut = gd.getNextString();
	String argument = gd.getNextString();
		IJ.register(Installer.class);
		defaultPlugin = plugin;
		if ("".equals(command)) {
			//EU_HOU Bundle
			IJ.showMessage(TITLE, "Command required");
			return;
		}
		if (shortcut.length() > 1) {
			shortcut = shortcut.replace('f', 'F');
		}
	char menu = ' ';
		if (menuStr.equals(menus[0])) {
			menu = Menus.SHORTCUTS_MENU;
		} else if (menuStr.equals(menus[1])) {
			menu = Menus.PLUGINS_MENU;
		} else if (menuStr.equals(menus[2])) {
			menu = Menus.IMPORT_MENU;
		} else if (menuStr.equals(menus[3])) {
			menu = Menus.SAVE_AS_MENU;
		} else if (menuStr.equals(menus[4])) {
			menu = Menus.FILTERS_MENU;
		} else if (menuStr.equals(menus[5])) {
			menu = Menus.TOOLS_MENU;
		} else if (menuStr.equals(menus[6])) {
			menu = Menus.UTILITIES_MENU;
		}
		if (!"".equals(argument)) {
			plugin += "(\"" + argument + "\")";
		}
	int err = Menus.installPlugin(plugin, menu, command, shortcut, IJ.getInstance());
		switch (err) {
						case Menus.COMMAND_IN_USE:
							//EU_HOU Bundle
							IJ.showMessage(TITLE, "The command \"" + command + "\" \nis already being used.");
							return;
						case Menus.INVALID_SHORTCUT:
							//EU_HOU Bundle
							IJ.showMessage(TITLE, "The shortcut must be a single character or \"F1\"-\"F12\".");
							return;
						case Menus.SHORTCUT_IN_USE:
							//EU_HOU Bundle
							IJ.showMessage("The \"" + shortcut + "\" shortcut is already being used.");
							return;
						default:
							command = "";
							shortcut = "";
							break;
		}
		if (!plugin.endsWith(")")) {
			installAbout(plugin);
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  plugin  Description of the Parameter
	 */
	void installAbout(String plugin) {
	boolean hasShowAboutMethod = false;
	PluginClassLoader loader = new PluginClassLoader(Menus.getPlugInsPath());
		try {
		Class c = loader.loadClass(plugin);
		//EU_HOU Bundle
		Method m = c.getDeclaredMethod("showAbout");
			if (m != null) {
				hasShowAboutMethod = true;
			}
		} catch (Exception ignored) {}
		//IJ.write("installAbout: "+plugin+" "+hasShowAboutMethod);
		if (hasShowAboutMethod) {
			//EU_HOU Bundle
			Menus.installPlugin(plugin + "(\"about\")", Menus.ABOUT_MENU, plugin + "...", "", IJ.getInstance());
		}
	}


	/**
	 *  Gets the plugins attribute of the Installer object
	 *
	 *@return    The plugins value
	 */
	String[] getPlugins() {
	String path = Menus.getPlugInsPath();
		if (path == null) {
			return null;
		}
	File f = new File(path);
	String[] list = f.list();
		if (list == null) {
			return null;
		}
	Vector v = new Vector();
        for (String aList : list) {
            String className = aList;
            boolean isClassFile = className.endsWith(".class");
            if (className.indexOf('_') >= 0 && isClassFile && className.indexOf('$') < 0) {
                className = className.substring(0, className.length() - 6);
                v.addElement(className);
            } else {
                if (!isClassFile) {
                    getSubdirectoryPlugins(path, className, v);
                }
            }
        }
		list = new String[v.size()];
		v.copyInto(list);
		StringSorter.sort(list);
		return list;
	}


	/**
	 *  Looks for plugins in a subdirectorie of the plugins directory.
	 *
	 *@param  path  Description of the Parameter
	 *@param  dir   Description of the Parameter
	 *@param  v     Description of the Parameter
	 */
	void getSubdirectoryPlugins(String path, String dir, Vector v) {
		//IJ.write("getSubdirectoryPlugins: "+path+dir);
		if (dir.endsWith(".java")) {
			return;
		}
	File f = new File(path, dir);
		if (!f.isDirectory()) {
			return;
		}
	String[] list = f.list();
		if (list == null) {
			return;
		}
		dir += "/";
        for (String aList : list) {
            String name = aList;
            if (name.indexOf('_') >= 0 && name.endsWith(".class") && name.indexOf('$') < 0) {
                name = name.substring(0, name.length() - 6);// remove ".class"
                v.addElement(name);
                //IJ.write("File: "+f+"/"+name);
            }
        }
	}

}

