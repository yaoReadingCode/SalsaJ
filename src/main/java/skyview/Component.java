
package skyview;

/**
 * This class defines the basics structural methods which all
 * Java classes are required to include.  These include
 * metadata and serialization requirements.
 */
public interface Component extends java.io.Serializable {

    /**
     * Get the name of this component.
     */
    String getName();

    /**
     * Get the description of this component.
     */
    String getDescription();
}
