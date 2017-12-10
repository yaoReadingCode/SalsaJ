package ij.io;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import ij.IJ;

/** ImageJ uses this class loader to load plugins and resources from the
 * plugins directory and immediate subdirectories. This class loader will
 * also load classes and resources from JAR files.
 *
 * <p> The class loader searches for classes and resources in the following order:
 * <ol>
 *  <li> Plugins directory</li>
 *  <li> Subdirectories of the Plugins directory</li>
 *  <li> JAR and ZIP files in the plugins directory and subdirectories</li>
 * </ol>
 * <p> The class loader does not recurse into subdirectories beyond the first level.
*/
public class PluginClassLoader extends ClassLoader {
    protected String path;
    protected Hashtable cache = new Hashtable();
    protected Vector jarFiles;

    /**
     * Creates a new PluginClassLoader that searches in the directory path
     * passed as a parameter. The constructor automatically finds all JAR and ZIP
     * files in the path and first level of subdirectories. The JAR and ZIP files
     * are stored in a Vector for future searches.
     * @param path the path to the plugins directory.
     */
	public PluginClassLoader(String path) {
		init(path);
	}
	
	/** This version of the constructor is used when ImageJ is launched using Java WebStart. */
	public PluginClassLoader(String path, boolean callSuper) {
		super(Thread.currentThread().getContextClassLoader());
		init(path);
	}

	void init(String path) {
		this.path = path;
		jarFiles = new Vector();
		//find all JAR files on the path and subdirectories
		File f = new File(path);
		String[] list = f.list();
		if (list==null) {
            return;
        }
        for (String aList : list) {
            f = new File(path, aList);
            if (f.isDirectory()) {
                String[] innerlist = f.list();
                if (innerlist == null) {
                    continue;
                }
                for (String anInnerlist : innerlist) {
                    File g = new File(f, anInnerlist);
                    if (g.isFile()) {
                        addJAR(g);
                    }
                }
            } else {
                addJAR(f);
            }
        }
	}

    private void addJAR(File f) {
        if (f.getName().endsWith(".jar") || f.getName().endsWith(".zip")) {
            jarFiles.addElement(f);
        }
    }

    /**
     * Returns a resource from the path or JAR files as a URL
     * @param name a resource name.
     */
    @Override
    public URL getResource(String name) {
        // try system loader first
        URL res = getSystemResource(name);
        if (res != null) {
            return res;
        }

        File resFile;

        //try plugins directory
        try {
            resFile = new File(path, name);
            if (resFile.exists()) {
              res = makeURL(resFile);
              return res; 
            }
        }
        catch (Exception ignored) {}

        //try subfolders
        resFile = new File(path);
        String[] list = resFile.list();
        if (list!=null) {
            for (String aList : list) {
                resFile = new File(path, aList);
                if (resFile.isDirectory()) {
                    try {
                        File f = new File(path + aList, name);
                        if (f.exists()) {
                            res = makeURL(f);
                            return res;
                        }
                    } catch (Exception ignored) {
                    }

                }
            }
        }

        //otherwise look in JAR files
        byte [] resourceBytes;
        for (int i=0; i<jarFiles.size(); i++) {
            try {
                File jf = (File)jarFiles.elementAt(i);
                resourceBytes = loadFromJar(jf.getPath(), name);
                if (resourceBytes != null) {
                    res = makeURL(name, jf);
                    return res;
                }
            }
            catch (MalformedURLException e) {
                IJ.error(e.toString());
            }
            catch (IOException e) {
                IJ.error(e.toString());
            }
        }
        return null;
    }
    
    // make a URL from a file
    private URL makeURL (File fil) throws MalformedURLException {
        return new URL("file","",fil.toString());
    }
    
    // make a URL from a file within a JAR
    private URL makeURL (String name, File jar) throws MalformedURLException {
        //filename.insert(0,'/');
        String sf = "file:///" + jar.toString() +
                "!/" +
                name;
        String sfu = sf.replace('\\','/');
        return new URL("jar","",sfu);
    }

    /**
     * Returns a resource from the path or JAR files as an InputStream
     * @param name a resource name.
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        //try the system loader first
        InputStream is = getSystemResourceAsStream(name);
        if (is != null) {
            return is;
        }

        File resFile;

        //try plugins directory
        resFile = new File(path, name);
        try { // read the byte codes
            is = new FileInputStream(resFile);
        }
        catch (Exception ignored) {}
        if (is != null) {
            return is;
        }

        //try subdirectories
        resFile = new File(path);
        String[] list = resFile.list();
        if (list!=null) {
            for (String aList : list) {
                resFile = new File(path, aList);
                if (resFile.isDirectory()) {
                    try {
                        File f = new File(path + aList, name);
                        is = new FileInputStream(f);
                    } catch (Exception ignored) {
                    }
                    if (is != null) {
                        return is;
                    }
                }
            }
        }

        //look in JAR files
        byte [] resourceBytes;
        for (int i=0; i<jarFiles.size(); i++) {
            try {
                File jf = (File)jarFiles.elementAt(i);
                resourceBytes = loadFromJar(jf.getPath(), name);
                if (resourceBytes != null){
                    is = new ByteArrayInputStream(resourceBytes);
                    return is;
                }
            }
            catch (Exception e) {
                IJ.error(e.toString());
            }
        }
        return null;
    }

    /**
     * Returns a Class from the path or JAR files. Classes are automatically resolved.
     * @param className a class name without the .class extension.
     */
    @Override
    public Class loadClass(String className) throws ClassNotFoundException {
        return (loadClass(className, true));
    }

    /**
     * Returns a Class from the path or JAR files. Classes are resolved if resolveIt is true.
     * @param className a String class name without the .class extension.
     *        resolveIt a boolean (should almost always be true)
     */
    @Override
    public synchronized Class loadClass(String className, boolean resolveIt) throws ClassNotFoundException {

        Class   result;
        byte[]  classBytes;

        // try the local cache of classes
        result = (Class)cache.get(className);
        if (result != null) {
            return result;
        }

        // try the system class loader
        try {
            result = super.findSystemClass(className);
            return result;
        }
        catch (Exception ignored) {}

        // Try to load it from plugins directory
        classBytes = loadClassBytes(className);
		//IJ.log("loadClass: "+ className + "  "+ (classBytes!=null?""+classBytes.length:"null"));
		if (classBytes==null) {
			result = getParent().loadClass(className);
			if (result != null) {
                return result;
            }
		}
		if (classBytes==null) {
            throw new ClassNotFoundException(className);
        }

        // Define it (parse the class file)
        result = defineClass(className, classBytes, 0, classBytes.length);
        if (result == null) {
            throw new ClassFormatError();
        }

        //Resolve if necessary
        if (resolveIt) {
            resolveClass(result);
        }

        cache.put(className, result);
        return result;
    }

    /**
     * This does the actual work of loading the bytes from the disk. Returns an
     * array of bytes that will be defined as a Class. This should be overloaded to have
     * the Class Loader look in more places.
     * @param name a class name without the .class extension.
     */

    protected byte[] loadClassBytes(String name) {
        byte [] classBytes = null;
        classBytes = loadIt(path, name);
        if (classBytes == null) {
            classBytes = loadFromSubdirectory(path, name);
            if (classBytes == null) {
                // Attempt to get the class data from the JAR files.
                for (int i=0; i<jarFiles.size(); i++) {
                    try {
                        File jf = (File)jarFiles.elementAt(i);
                        classBytes = loadClassFromJar(jf.getPath(), name);
                        if (classBytes != null) {
                            return classBytes;
                        }
                    }
                    catch (Exception e) {
                        //no problem, try the next one
                    }
                }
            }
        }
        return classBytes;
    }

    // Loads the bytes from file
    private byte [] loadIt(String path, String classname) {
        String filename = classname.replace('.','/');
        filename += ".class";
        File fullname = new File(path, filename);
        //ij.IJ.write("loadIt: " + fullname);
        try { // read the byte codes
            InputStream is = new FileInputStream(fullname);
            int bufsize = (int)fullname.length();
            byte buf[] = new byte[bufsize];
            is.read(buf, 0, bufsize);
            is.close();
            return buf;
        } catch (Exception e) {
            return null;
        }
    }

    private byte [] loadFromSubdirectory(String path, String name) {
        File f = new File(path);
        String[] list = f.list();
        if (list!=null) {
            for (String aList : list) {
                //ij.IJ.write(path+"  "+list[i]);
                f = new File(path, aList);
                if (f.isDirectory()) {
                    byte[] buf = loadIt(path + aList, name);
                    if (buf != null) {
                        return buf;
                    }
                }
            }
        }
        return null;
    }

	// Load class from a JAR file
	byte[] loadClassFromJar(String jar, String className) {
		String name = className.replace('.','/');
		name += ".class";
		return loadFromJar(jar, name);
	}

	// Load class or resource from a JAR file
	byte[] loadFromJar(String jar, String name) {
		BufferedInputStream bis = null;
		try {
			ZipFile jarFile = new ZipFile(jar);
			Enumeration entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) entries.nextElement();
        		if (entry.getName().equals(name)) {
					bis = new BufferedInputStream(jarFile.getInputStream(entry));
					int size = (int)entry.getSize();
					byte[] data = new byte[size];
                    int b=0, eofFlag=0;
                    while ((size - b) > 0) {
                        eofFlag = bis.read(data, b, size - b);
                        if (eofFlag==-1) {
                            break;
                        }
                        b += eofFlag;
                    }
					return data;
				}
			}
		}
    	catch (Exception ignored) {}
    	finally {
    		try {if (bis!=null) {
                bis.close();
            }
            }
    		catch (IOException ignored) {}
    	}
    	return null;
	}

}
