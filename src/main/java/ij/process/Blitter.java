package ij.process;
import java.awt.Color;

/** ImageJ bit blitting classes must implement this interface. */
public interface Blitter {

	/** dst=src */
    int COPY = 0;
	
	/** dst=255-src (8-bits and RGB) */
    int COPY_INVERTED = 1;
	
	/** Copies with white pixels transparent. */
    int COPY_TRANSPARENT = 2;
	
	/** dst=dst+src */
    int ADD = 3;
	
	/** dst=dst-src */
    int SUBTRACT = 4;
		
	/** dst=src*src */
    int MULTIPLY = 5;
	
	/** dst=dst/src */
    int DIVIDE = 6;
	
	/** dst=(dst+src)/2 */
    int AVERAGE = 7;
	
	/** dst=abs(dst-src) */
    int DIFFERENCE = 8;
	
	/** dst=dst AND src */
    int AND = 9;
	
	/** dst=dst OR src */
    int OR = 10;
	
	/** dst=dst XOR src */
    int XOR = 11;
	
	/** dst=min(dst,src) */
    int MIN = 12;
	
	/** dst=max(dst,src) */
    int MAX = 13;

	/** Sets the transparent color used in the COPY_TRANSPARENT
		mode (default is Color.white). */
    void setTransparentColor(Color c);

	/** Copies the image in 'src' to (x,y) using the specified mode. */
    void copyBits(ImageProcessor src, int x, int y, int mode);

}