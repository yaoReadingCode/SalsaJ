//EU_HOU
package ij.io;

import java.io.*;
import java.util.zip.*;
import ij.*;
import ij.process.*;
import ij.measure.Calibration;
import ij.plugin.filter.Analyzer;
import ij.plugin.JpegWriter;

/**
 * Saves images in tiff, gif, jpeg, raw, zip and text format.
 *
 * @author Thomas
 * @created 3 decembre 2007
 */
public class FileSaver {

    private static String defaultDirectory = null;
    private ImagePlus imp;
    private FileInfo fi;
    private String name;
    private String directory;

    public static final int DEFAULT_JPEG_QUALITY = 85;
    private static int jpegQuality;

    static {
        setJpegQuality(ij.Prefs.getInt(ij.Prefs.JPEG, DEFAULT_JPEG_QUALITY));
    }

    /**
     * Constructs a FileSaver from an ImagePlus.
     *
     * @param imp Description of the Parameter
     */
    public FileSaver(ImagePlus imp) {
        this.imp = imp;
        fi = imp.getFileInfo();
    }

    /**
     * Resaves the image. Calls saveAsTiff() if this is a new image, not a TIFF,
     * a stack or if the image was loaded using a URL. Returns false if
     * saveAsTiff() is called and the user selects cancel in the file save
     * dialog box.
     *
     * @return Description of the Return Value
     */
    public boolean save() {
        FileInfo ofi = null;
        if (imp != null) {
            ofi = imp.getOriginalFileInfo();
        }
        boolean validName = ofi != null && imp.getTitle().equals(ofi.fileName);
        if (validName && ofi.fileFormat == FileInfo.TIFF && imp.getStackSize() == 1 && ofi.nImages == 1 && (ofi.url == null || "".equals(ofi.url))) {
            name = imp.getTitle();
            directory = ofi.directory;
            String path = directory + name;
            File f = new File(path);
            if (!IJ.macroRunning() && f != null && f.exists()) {
                //EU_HOU Bundle
                if (!IJ.showMessageWithCancel("Save as TIFF", "The file " + ofi.fileName + " already exists.\nDo you want to replace it?")) {
                    return false;
                }
            }
            //EU_HOU Bundle
            IJ.showStatus("Saving " + path);
            return saveAsTiff(path);
        } else {
            return saveAsTiff();
        }
    }

    /**
     * Gets the path attribute of the FileSaver object
     *
     * @param type Description of the Parameter
     * @param extension Description of the Parameter
     * @return The path value
     */
    String getPath(String type, String extension) {
        name = imp.getTitle();
        //EU_HOU Bundle
        SaveDialog sd = new SaveDialog("Save as " + type, name, extension);
        name = sd.getFileName();
        if (name == null) {
            return null;
        }
        directory = sd.getDirectory();
        imp.startTiming();
        return directory + name;
    }

    /**
     * Save the image or stack in TIFF format using a save file dialog. Returns
     * false if the user selects cancel.
     *
     * @return Description of the Return Value
     */
    public boolean saveAsTiff() {
        String path = getPath("TIFF", ".tif");
        if (path == null) {
            return false;
        }
        if (fi.nImages > 1) {
            return saveAsTiffStack(path);
        } else {
            return saveAsTiff(path);
        }
    }

    /**
     * Save the image in TIFF format using the specified path.
     *
     * @param path Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean saveAsTiff(String path) {
        fi.nImages = 1;
        Object info = imp.getProperty("Info");
        if (info != null && (info instanceof String)) {
            fi.info = (String) info;
        }
        fi.description = getDescriptionString();
        try {
            TiffEncoder file = new TiffEncoder(fi);
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path)));
            file.write(out);
            out.close();
        } catch (IOException e) {
            showErrorMessage(e);
            return false;
        }
        updateImp(fi, FileInfo.TIFF);
        return true;
    }

    /**
     * Save the stack as a multi-image TIFF using the specified path.
     *
     * @param path Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean saveAsTiffStack(String path) {
        if (fi.nImages == 1) {//EU_HOU Bundle
            IJ.error("This is not a stack");
            return false;
        }
        if (fi.pixels == null && imp.getStack().isVirtual()) {//EU_HOU Bundle
            IJ.error("Save As Tiff", "Virtual stacks not supported.");
            return false;
        }
        Object info = imp.getProperty("Info");
        if (info != null && (info instanceof String)) {
            fi.info = (String) info;
        }
        fi.description = getDescriptionString();
        fi.sliceLabels = imp.getStack().getSliceLabels();
        try {
            TiffEncoder file = new TiffEncoder(fi);
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path)));
            file.write(out);
            out.close();
        } catch (IOException e) {
            showErrorMessage(e);
            return false;
        }
        updateImp(fi, FileInfo.TIFF);
        return true;
    }

    /**
     * Uses a save file dialog to save the image or stack as a TIFF in a ZIP
     * archive. Returns false if the user selects cancel.
     *
     * @return Description of the Return Value
     */
    public boolean saveAsZip() {
        String path = getPath("TIFF/ZIP", ".zip");
        return path != null && saveAsZip(path);
    }

    /**
     * Save the image or stack in TIFF/ZIP format using the specified path.
     *
     * @param path Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean saveAsZip(String path) {
        //fi.nImages = 1;
        if (!path.endsWith(".zip")) {
            path = path + ".zip";
        }
        if (name == null) {
            name = imp.getTitle();
        }
        if (name.endsWith(".zip")) {
            name = name.substring(0, name.length() - 4);
        }
        if (!name.endsWith(".tif")) {
            name = name + ".tif";
        }
        fi.description = getDescriptionString();
        Object info = imp.getProperty("Info");
        if (info != null && (info instanceof String)) {
            fi.info = (String) info;
        }
        fi.sliceLabels = imp.getStack().getSliceLabels();
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(path));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
            zos.putNextEntry(new ZipEntry(name));
            TiffEncoder te = new TiffEncoder(fi);
            te.write(out);
            out.close();
        } catch (IOException e) {
            showErrorMessage(e);
            return false;
        }
        updateImp(fi, FileInfo.TIFF);
        return true;
    }

    /**
     * Description of the Method
     *
     * @param imp Description of the Parameter
     * @return Description of the Return Value
     */
    public static boolean okForGif(ImagePlus imp) {
        int type = imp.getType();
        if (type == ImagePlus.COLOR_RGB) {
            //EU_HOU Bundle
            IJ.error("To save as Gif, the image must be converted to \"8-bit Color\".");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Save the image in GIF format using a save file dialog. Returns false if
     * the user selects cancel or the image is not 8-bits.
     *
     * @return Description of the Return Value
     */
    public boolean saveAsGif() {
        if (!okForGif(imp)) {
            return false;
        }
        String path = getPath("GIF", ".gif");
        return path != null && saveAsGif(path);
    }

    /**
     * Save the image in Gif format using the specified path. Returns false if
     * the image is not 8-bits or there is an I/O error.
     *
     * @param path Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean saveAsGif(String path) {
        if (!okForGif(imp)) {
            return false;
        }
        ImagePlus tempImage = WindowManager.getTempCurrentImage();
        WindowManager.setTempCurrentImage(imp);
        IJ.runPlugIn("ij.plugin.GifWriter", path);
        WindowManager.setTempCurrentImage(tempImage);
        updateImp(fi, FileInfo.GIF_OR_JPG);
        return true;
    }

    /**
     * Always returns true.
     *
     * @param imp Description of the Parameter
     * @return Description of the Return Value
     */
    public static boolean okForJpeg(ImagePlus imp) {
        return true;
    }

    /**
     * Save the image in JPEG format using a save file dialog. Returns false if
     * the user selects cancel.
     *
     * @return Description of the Return Value
     * @see ij.plugin.JpegWriter#setQuality
     * @see ij.plugin.JpegWriter#getQuality
     */
    public boolean saveAsJpeg() {
        String type = "JPEG (" + getJpegQuality() + ")";
        String path = getPath(type, ".jpg");
        return path != null && saveAsJpeg(path);
    }

    /**
     * Save the image in JPEG format using the specified path.
     *
     * @param path Description of the Parameter
     * @return Description of the Return Value
     * @see ij.plugin.JpegWriter#setQuality
     * @see ij.plugin.JpegWriter#getQuality
     */
    public boolean saveAsJpeg(String path) {
        String err = JpegWriter.save(imp, path, jpegQuality);
        if (err == null && !(imp.getType() == ImagePlus.GRAY16 || imp.getType() == ImagePlus.GRAY32)) {
            updateImp(fi, FileInfo.GIF_OR_JPG);
        }
        return true;
    }

    /**
     * Save the image in BMP format using a save file dialog. Returns false if
     * the user selects cancel.
     *
     * @return Description of the Return Value
     */
    public boolean saveAsBmp() {
        String path = getPath("BMP", ".bmp");
        return path != null && saveAsBmp(path);
    }

    /**
     * Save the image in BMP format using the specified path.
     *
     * @param path Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean saveAsBmp(String path) {
        ImagePlus tempImage = WindowManager.getTempCurrentImage();
        WindowManager.setTempCurrentImage(imp);
        IJ.runPlugIn("ij.plugin.BMP_Writer", path);
        WindowManager.setTempCurrentImage(tempImage);
        return true;
    }

    /**
     * Saves grayscale images in PGM (portable graymap) format and RGB images in
     * PPM (portable pixmap) format, using a save file dialog. Returns false if
     * the user selects cancel.
     *
     * @return Description of the Return Value
     */
    public boolean saveAsPgm() {
        String extension = imp.getBitDepth() == 24 ? ".pnm" : ".pgm";
        String path = getPath("PGM", extension);
        return path != null && saveAsPgm(path);
    }

    /**
     * Saves grayscale images in PGM (portable graymap) format and RGB images in
     * PPM (portable pixmap) format, using the specified path.
     *
     * @param path Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean saveAsPgm(String path) {
        ImagePlus tempImage = WindowManager.getTempCurrentImage();
        WindowManager.setTempCurrentImage(imp);
        IJ.runPlugIn("ij.plugin.PNM_Writer", path);
        WindowManager.setTempCurrentImage(tempImage);
        return true;
    }

    /**
     * Save the image in PNG format using a save file dialog. Returns false if
     * the user selects cancel. Requires java 1.4 or later.
     *
     * @return Description of the Return Value
     */
    public boolean saveAsPng() {
        if (!IJ.isJava14()) {
            //EU_HOU Bundle
            IJ.error("Save As PNG", "Java 1.4 or later required");
            return false;
        }
        String path = getPath("PNG", ".png");
        return path != null && saveAsPng(path);
    }

    /**
     * Save the image in PNG format using the specified path. Requires Java 1,4
     * or later.
     *
     * @param path Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean saveAsPng(String path) {
        ImagePlus tempImage = WindowManager.getTempCurrentImage();
        WindowManager.setTempCurrentImage(imp);
        IJ.runPlugIn("ij.plugin.PNG_Writer", path);
        WindowManager.setTempCurrentImage(tempImage);
        return true;
    }

    /**
     * Save the image in FITS format using a save file dialog. Returns false if
     * the user selects cancel.
     *
     * @return Description of the Return Value
     */
    public boolean saveAsFits() {
        if (!okForFits(imp)) {
            return false;
        }
        String path = getPath("FITS", ".fits");
        return path != null && saveAsFits(path);
    }

    /**
     * Save the image in FITS format using the specified path.
     *
     * @param path Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean saveAsFits(String path) {
        if (!okForFits(imp)) {
            return false;
        }
        ImagePlus tempImage = WindowManager.getTempCurrentImage();
        WindowManager.setTempCurrentImage(imp);
        IJ.runPlugIn("ij.plugin.FITS_Writer", path);
        WindowManager.setTempCurrentImage(tempImage);
        return true;
    }

    /**
     * Description of the Method
     *
     * @param imp Description of the Parameter
     * @return Description of the Return Value
     */
    public static boolean okForFits(ImagePlus imp) {
        if (imp.getBitDepth() == 24) {
            //EU_HOU Bundle
            IJ.error("FITS Writer", "Grayscale image required");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Save the image or stack as raw data using a save file dialog. Returns
     * false if the user selects cancel.
     *
     * @return Description of the Return Value
     */
    public boolean saveAsRaw() {
        String path = getPath("Raw", ".raw");
        if (path == null) {
            return false;
        }
        if (imp.getStackSize() == 1) {
            return saveAsRaw(path);
        } else {
            return saveAsRawStack(path);
        }
    }

    /**
     * Save the image as raw data using the specified path.
     *
     * @param path Description of the Parameter
     * @return Description of the Return Value
     */
    /**
     * Save the image as raw data using the specified path.
     *
     * @param path Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean saveAsRaw(String path) {
        fi.nImages = 1;
        fi.intelByteOrder = Prefs.intelByteOrder;
        boolean signed16Bit = false;
        short[] pixels = null;
        int n = 0;
        try {
            signed16Bit = imp.getCalibration().isSigned16Bit();
            if (signed16Bit) {
                pixels = (short[]) imp.getProcessor().getPixels();
                n = imp.getWidth() * imp.getHeight();
                for (int i = 0; i < n; i++) {
                    pixels[i] = (short) (pixels[i] - 32768);
                }
            }
            ImageWriter file = new ImageWriter(fi);
            OutputStream out = new BufferedOutputStream(new FileOutputStream(path));
            file.write(out);
            out.close();
        } catch (IOException e) {
            showErrorMessage(e);
            return false;
        }
        if (signed16Bit) {
            for (int i = 0; i < n; i++) {
                pixels[i] = (short) (pixels[i] + 32768);
            }
        }
        updateImp(fi, FileInfo.RAW);
        return true;
    }

    /**
     * Save the stack as raw data using the specified path.
     *
     * @param path Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean saveAsRawStack(String path) {
        if (fi.nImages == 1) {//EU_HOU Bundle
            IJ.write("This is not a stack");
            return false;
        }
        fi.intelByteOrder = Prefs.intelByteOrder;
        boolean signed16Bit = false;
        Object[] stack = null;
        int n = 0;
        try {
            signed16Bit = imp.getCalibration().isSigned16Bit();
            if (signed16Bit) {
                stack = (Object[]) fi.pixels;
                n = imp.getWidth() * imp.getHeight();
                for (int slice = 0; slice < fi.nImages; slice++) {
                    short[] pixels = (short[]) stack[slice];
                    for (int i = 0; i < n; i++) {
                        pixels[i] = (short) (pixels[i] - 32768);
                    }
                }
            }
            ImageWriter file = new ImageWriter(fi);
            OutputStream out = new BufferedOutputStream(new FileOutputStream(path));
            file.write(out);
            out.close();
        } catch (IOException e) {
            showErrorMessage(e);
            return false;
        }
        if (signed16Bit) {
            for (int slice = 0; slice < fi.nImages; slice++) {
                short[] pixels = (short[]) stack[slice];
                for (int i = 0; i < n; i++) {
                    pixels[i] = (short) (pixels[i] + 32768);
                }
            }
        }
        updateImp(fi, FileInfo.RAW);
        return true;
    }

    /**
     * Save the image as tab-delimited text using a save file dialog. Returns
     * false if the user selects cancel.
     *
     * @return Description of the Return Value
     */
    public boolean saveAsText() {
        String path = getPath("Text", ".txt");
        return path != null && saveAsText(path);
    }

    /**
     * Save the image as tab-delimited text using the specified path.
     *
     * @param path Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean saveAsText(String path) {
        try {
            Calibration cal = imp.getCalibration();
            int precision = Analyzer.getPrecision();
            TextEncoder file = new TextEncoder(imp.getProcessor(), cal, precision);
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path)));
            file.write(out);
            out.close();
        } catch (IOException e) {
            showErrorMessage(e);
            return false;
        }
        return true;
    }

    /**
     * Save the current LUT using a save file dialog. Returns false if the user
     * selects cancel.
     *
     * @return Description of the Return Value
     */
    public boolean saveAsLut() {
        if (imp.getType() == ImagePlus.COLOR_RGB) {
            //EU_HOU Bundle
            IJ.error("RGB Images do not have a LUT.");
            return false;
        }
        String path = getPath("LUT", ".lut");
        return path != null && saveAsLut(path);
    }

    /**
     * Save the current LUT using the specified path.
     *
     * @param path Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean saveAsLut(String path) {
        LookUpTable lut = imp.createLut();
        int mapSize = lut.getMapSize();
        if (mapSize == 0) {
            //EU_HOU Bundle
            IJ.error("RGB Images do not have a LUT.");
            return false;
        }
        if (mapSize < 256) {
            //EU_HOU Bundle
            IJ.error("Cannot save LUTs with less than 256 entries.");
            return false;
        }
        byte[] reds = lut.getReds();
        byte[] greens = lut.getGreens();
        byte[] blues = lut.getBlues();
        byte[] pixels = new byte[768];
        for (int i = 0; i < 256; i++) {
            pixels[i] = reds[i];
            pixels[i + 256] = greens[i];
            pixels[i + 512] = blues[i];
        }
        FileInfo fi = new FileInfo();
        fi.width = 768;
        fi.height = 1;
        fi.pixels = pixels;

        try {
            ImageWriter file = new ImageWriter(fi);
            OutputStream out = new FileOutputStream(path);
            file.write(out);
            out.close();
        } catch (IOException e) {
            showErrorMessage(e);
            return false;
        }
        return true;
    }

    /**
     * Description of the Method
     *
     * @param fi Description of the Parameter
     * @param fileFormat Description of the Parameter
     */
    private void updateImp(FileInfo fi, int fileFormat) {
        imp.changes = false;
        if (name != null) {
            fi.fileFormat = fileFormat;
            fi.fileName = name;
            fi.directory = directory;
            //if (fileFormat==fi.TIFF)
            //	fi.offset = TiffEncoder.IMAGE_START;
            fi.description = null;
            imp.setTitle(name);
            imp.setFileInfo(fi);
        }
    }

    /**
     * Description of the Method
     *
     * @param e Description of the Parameter
     */
    void showErrorMessage(IOException e) {
        String msg = e.getMessage();
        if (msg.length() > 100) {
            msg = msg.substring(0, 100);
        }
        //EU_HOU Bundle
        IJ.error("FileSaver", "An error occured writing the file.\n \n" + msg);
    }

    /**
     * Returns a string containing information about the specified image.
     *
     * @return The descriptionString value
     */
    public String getDescriptionString() {
        Calibration cal = imp.getCalibration();
        StringBuffer sb = new StringBuffer(100);
        sb.append("ImageJ=" + ImageJ.VERSION + "\n");
        if (fi.nImages > 1 && fi.fileType != FileInfo.RGB48) {
            sb.append("images=").append(fi.nImages).append("\n");
        }
        int channels = imp.getNChannels();
        if (channels > 1) {
            sb.append("channels=").append(channels).append("\n");
        }
        int slices = imp.getNSlices();
        if (slices > 1) {
            sb.append("slices=").append(slices).append("\n");
        }
        int frames = imp.getNFrames();
        if (frames > 1) {
            sb.append("frames=").append(frames).append("\n");
        }
        if (fi.unit != null) {
            sb.append("unit=").append(fi.unit).append("\n");
        }
        if (fi.valueUnit != null && fi.calibrationFunction != Calibration.CUSTOM) {
            sb.append("cf=").append(fi.calibrationFunction).append("\n");
            if (fi.coefficients != null) {
                for (int i = 0; i < fi.coefficients.length; i++) {
                    sb.append("c").append(i).append("=").append(fi.coefficients[i]).append("\n");
                }
            }
            sb.append("vunit=").append(fi.valueUnit).append("\n");
            if (cal.zeroClip()) {
                sb.append("zeroclip=true\n");
            }
        }

        // get stack z-spacing and fps
        if (fi.nImages > 1) {
            if (fi.pixelDepth != 0.0 && fi.pixelDepth != 1.0) {
                sb.append("spacing=").append(fi.pixelDepth).append("\n");
            }
            if (cal.fps != 0.0) {
                if ((int) cal.fps == cal.fps) {
                    sb.append("fps=").append((int) cal.fps).append("\n");
                } else {
                    sb.append("fps=").append(cal.fps).append("\n");
                }
            }
            sb.append("loop=").append(cal.loop ? "true" : "false").append("\n");
            if (cal.frameInterval != 0.0) {
                if ((int) cal.frameInterval == cal.frameInterval) {
                    sb.append("finterval=").append((int) cal.frameInterval).append("\n");
                } else {
                    sb.append("finterval=").append(cal.frameInterval).append("\n");
                }
            }
            if (!"sec".equals(cal.getTimeUnit())) {
                sb.append("tunit=").append(cal.getTimeUnit()).append("\n");
            }
        }

        // get min and max display values
        ImageProcessor ip = imp.getProcessor();
        double min = ip.getMin();
        double max = ip.getMax();
        int type = imp.getType();
        boolean enhancedLut = (type == ImagePlus.GRAY8 || type == ImagePlus.COLOR_256) && (min != 0.0 || max != 255.0);
        if (enhancedLut || type == ImagePlus.GRAY16 || type == ImagePlus.GRAY32) {
            sb.append("min=").append(min).append("\n");
            sb.append("max=").append(max).append("\n");
        }

        // get non-zero origins
        if (cal.xOrigin != 0.0) {
            sb.append("xorigin=").append(cal.xOrigin).append("\n");
        }
        if (cal.yOrigin != 0.0) {
            sb.append("yorigin=").append(cal.yOrigin).append("\n");
        }
        if (cal.zOrigin != 0.0) {
            sb.append("zorigin=").append(cal.zOrigin).append("\n");
        }
        if (cal.info != null && cal.info.length() <= 64 && cal.info.indexOf('=') == -1 && cal.info.indexOf('\n') == -1) {
            sb.append("info=").append(cal.info).append("\n");
        }
        sb.append((char) 0);
        return new String(sb);
    }

    /**
     * Specifies the image quality (0-100). 0 is poorest image quality, highest
     * compression, and 100 is best image quality, lowest compression.
     */
    public static void setJpegQuality(int quality) {
        jpegQuality = quality;
        if (jpegQuality < 0) {
            jpegQuality = 0;
        }
        if (jpegQuality > 100) {
            jpegQuality = 100;
        }
    }

    /**
     * Returns the current JPEG quality setting (0-100).
     */
    public static int getJpegQuality() {
        return jpegQuality;
    }

}
