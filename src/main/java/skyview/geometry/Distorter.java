package skyview.geometry;


/** This class defines a non-linear distortion in the image plane.
    Normally the forward distortion converts from a fiducial
    projection plane to some distorted coordinates.  The reverse
    distortion transforms from the distorted coordinates back
    to the fiducial coordinates.
  */
public abstract class Distorter extends Transformer implements skyview.Component {
    
    /** A name for this object */
    @Override
    public String getName() {
	return "Generic Distorter";
    } 
    
    /** What does this object do? */
    @Override
    public String getDescription() {
	return "Placeholder for distortions in projection plane";
    }
    
    @Override
    public abstract Distorter inverse();
    
    /** What is the output dimensionality of a Distorter? */
    @Override
    protected int getOutputDimension() {
	return 2;
    }
    
    /** What is the input dimensionality of a Distorter? */
    @Override
    protected int getInputDimension() {
	return 2;
    }
    
}
