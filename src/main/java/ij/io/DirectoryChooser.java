package ij.io;
import ij.*;
import ij.util.Java2;
import java.awt.*;
import java.io.*;
import javax.swing.*;

/** This class displays a dialog box that allows the user can select a directory. */ 
 public class DirectoryChooser {
 	private String directory;
 	private static String defaultDir;
 
 	/** Display a dialog using the specified title. */
 	public DirectoryChooser(String title) {
 		if (IJ.isMacOSX() && IJ.isJava14()) {
            getDirectoryUsingFileDialog(title);
        } else {
 			if (EventQueue.isDispatchThread()) {
                getDirectoryUsingJFileChooserOnThisThread(title);
            } else {
                getDirectoryUsingJFileChooser(title);
            }
 		}
 	}
 	
	// runs JFileChooser on event dispatch thread to avoid possible thread deadlocks
 	void getDirectoryUsingJFileChooser(final String title) {
		Java2.setSystemLookAndFeel();
		try {
			EventQueue.invokeAndWait(new Runnable() {
				@Override
                public void run() {
					JFileChooser chooser = new JFileChooser();
					if (defaultDir!=null) {
                        chooser.setCurrentDirectory(new File(defaultDir));
                    }
					chooser.setDialogTitle(title);
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					chooser.setApproveButtonText("Select");
					if (chooser.showOpenDialog(null)==JFileChooser.APPROVE_OPTION) {
						File dir = chooser.getCurrentDirectory();
						File file = chooser.getSelectedFile();
						directory = dir.getPath();
						if (!directory.endsWith(File.separator)) {
                            directory += File.separator;
                        }
						defaultDir = directory;
						String fileName = file.getName();
						if (fileName.contains(":\\")) {
                            directory = defaultDir = fileName;
                        } else {
                            directory += fileName+File.separator;
                        }
					}
				}
			});
		} catch (Exception ignored) {}
	}
 
	// Choose a directory using JFileChooser on the current thread
 	void getDirectoryUsingJFileChooserOnThisThread(final String title) {
		Java2.setSystemLookAndFeel();
		try {
			JFileChooser chooser = new JFileChooser();
			if (defaultDir!=null) {
                chooser.setCurrentDirectory(new File(defaultDir));
            }
			chooser.setDialogTitle(title);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setApproveButtonText("Select");
			if (chooser.showOpenDialog(null)==JFileChooser.APPROVE_OPTION) {
				File dir = chooser.getCurrentDirectory();
				File file = chooser.getSelectedFile();
				directory = dir.getPath();
				if (!directory.endsWith(File.separator)) {
                    directory += File.separator;
                }
				defaultDir = directory;
				String fileName = file.getName();
				if (fileName.contains(":\\")) {
                    directory = defaultDir = fileName;
                } else {
                    directory += fileName+File.separator;
                }
			}
		} catch (Exception ignored) {}
	}

 	// On Mac OS X, we can select directories using the native file open dialog
 	void getDirectoryUsingFileDialog(String title) {
 		boolean saveUseJFC = Prefs.useJFileChooser;
 		Prefs.useJFileChooser = false;
		System.setProperty("apple.awt.fileDialogForDirectories", "true");
		OpenDialog od = new OpenDialog(title, null);
		if (od.getDirectory()==null) {
            directory = null;
        } else {
            directory = od.getDirectory() + od.getFileName() + "/";
        }
		System.setProperty("apple.awt.fileDialogForDirectories", "false");
 		Prefs.useJFileChooser = saveUseJFC;
	}

 	/** Returns the directory selected by the user. */
 	public String getDirectory() {
 		//IJ.log("getDirectory: "+directory);
 		return directory;
 	}
 	
}
