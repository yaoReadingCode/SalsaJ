package skyview.geometry.projecter;


/** This class implements the Zenithal Equal Area (ZEA)
 *  projection.  Note that the tangent point
 *  is assumed to be at the north pole.
 *  This class assumes preallocated arrays for
 *  maximum efficiency.
 */

import skyview.geometry.Projecter;
import skyview.geometry.Deprojecter;
import skyview.geometry.Transformer;

public class ZeaProjecter extends Projecter {

    /** Get a name for the component */
    @Override
	public String getName() {
	return "ZeaProjecter";
    }
    
    /** Get a description for the component */
    @Override
	public String getDescription() {
	return "Zenithal Equal Area projecter";
    }
    
    /** Get this inverse of the transformation */
    @Override
	public Deprojecter inverse() {
	return new ZeaProjecter.ZeaDeprojecter();
    }

    /** Is this an inverse of some other transformation? */
    @Override
	public boolean isInverse(Transformer t) {
	return "ZeaDeprojecter".equals(t.getName());
    }
    
    /** Project a point from the sphere to the plane.
     *  @param sphere a double[3] unit vector
     *  @param plane  a double[2] preallocated vector.
     */
    @Override
	public final void transform(double[] sphere, double[] plane) {
	
	if (Double.isNaN(sphere[2])) {
	    plane[0] = Double.NaN;
	    plane[1] = Double.NaN;
	} else {
	    double num = 2*(1-sphere[2]);
	    if (num < 0) {
		num = 0;
	    }
	    double denom = sphere[0]*sphere[0] + sphere[1]*sphere[1];
	    if (denom == 0) {
		plane[0] = 0;
		plane[1] = 0;
	    } else {
	        double ratio = Math.sqrt(num) / 
	                   Math.sqrt(sphere[0]*sphere[0] + sphere[1]*sphere[1]);
	        plane[0] = ratio * sphere[0];
	        plane[1] = ratio * sphere[1];
	    }
	    
	}
    }
    
    
    public class ZeaDeprojecter extends Deprojecter {
	
	/** Get the name of the component */
	@Override
	public String getName() {
	    return "ZeaDeprojecter";
	}
	
	/** Get the description of the compontent */
	@Override
	public String getDescription() {
	    return "Zenithal equal area deprojecter";
	}
	
	/** Get the inverse transformation */
	@Override
	public Projecter inverse() {
	    return ZeaProjecter.this;
	}
	 
        /** Is this an inverse of some other transformation? */
        @Override
		public boolean isInverse(Transformer t) {
	    return "ZeaProjecter".equals(t.getName());
        }
	
	
        /** Deproject a point from the plane to the sphere.
         *  @param plane a double[2] vector in the tangent plane.
         *  @param spehre a preallocated double[3] vector.
         */
        @Override
		public final void transform(double[] plane, double[] sphere) {
	
	    double r = Math.sqrt(plane[0]*plane[0] + plane[1]*plane[1]);
	
	    if (r > 2 || Double.isNaN(plane[0])) {
	        sphere[0] = Double.NaN;
	        sphere[1] = Double.NaN;
	        sphere[2] = Double.NaN;
	    
	    } else {
	        sphere[2]  = 1 - r*r/2;
	        double ratio = (1-sphere[2]*sphere[2]);
		if (ratio > 0) {
		    ratio = Math.sqrt(ratio)/r;
		} else {
		    ratio = 0;
		}
	        sphere[0] = ratio * plane[0];
	        sphere[1] = ratio * plane[1];
	    }
	}
    }
}
