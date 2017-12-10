package ij.plugin;
import ij.*;
import ij.text.*;
import ij.util.*;
import java.util.*;
import java.awt.event.*;

/** Lists ImageJ commands or keyboard shortcuts in a text window. */
public class CommandLister implements PlugIn {

	@Override
    public void run(String arg) {
		if ("shortcuts".equals(arg)) {
            listShortcuts();
        } else {
            listCommands();
        }
	}
	
	public void listCommands() {
		Hashtable commands = Menus.getCommands();
		Vector v = new Vector();
		for (Enumeration en=commands.keys(); en.hasMoreElements();) {
			String command = (String)en.nextElement();
			v.addElement(command+"\t"+ commands.get(command));
		}
		showList("Commands", "Command\tPlugin", v);
	}

	public void listShortcuts() {
		Hashtable shortcuts = Menus.getShortcuts();
		Vector v = new Vector();
		addShortcutsToVector(shortcuts, v);
		Hashtable macroShortcuts = Menus.getMacroShortcuts();
		addShortcutsToVector(macroShortcuts, v);
		showList("Keyboard Shortcuts", "Hot Key\tCommand", v);
	}
	
	void addShortcutsToVector(Hashtable shortcuts, Vector v) {
		for (Enumeration en=shortcuts.keys(); en.hasMoreElements();) {
			Integer key = (Integer)en.nextElement();
			int keyCode = key;
			boolean upperCase = false;
			if (keyCode>200) {
				upperCase = true;
				keyCode -= 200;
			}
			String shortcut = KeyEvent.getKeyText(keyCode);
			if (!upperCase && shortcut.length()==1) {
				char c = shortcut.charAt(0);
				if (c>=65 && c<=90) {
                    c += 32;
                }
				char[] chars = new char[1];
				chars[0] = c;
				shortcut = new String(chars);
			}
			if (shortcut.length()>1) {
                shortcut = " " + shortcut;
            }
			v.addElement(shortcut+"\t"+ shortcuts.get(key));
		}
	}

	void showList(String title, String headings, Vector v) {
		String[] list = new String[v.size()];
		v.copyInto(list);
		StringSorter.sort(list);
		StringBuilder sb = new StringBuilder();
        for (String aList : list) {
            sb.append(aList);
            sb.append("\n");
        }
		TextWindow tw = new TextWindow(title, headings, sb.toString(), 600, 500);
	}
}
