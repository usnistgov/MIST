package gov.nist.isg.mist.lib.export;


/**
 * Enum representing the different types of blending modes
 *
 * @author Tim Blattner
 * @version 1.0
 */
public enum BlendingMode {
    /**
     * Overlay blending mode
     */
    OVERLAY("Overlay", "Choose only one pixel from overlapping pixels based on highest correlation", false),

    /**
     * Average blending mode
     */
    AVERAGE("Average", "Computes the the average intensity for each image", false),

    /**
     * Linear blending mode
     */
    LINEAR("Linear", "Smoothly alters the intensity of the overlapping area between images", true);

    private String name;
    private String toolTipText;
    private boolean requiresAlpha;

    private BlendingMode(String name, String toolTipText, boolean requiresAlpha) {
        this.name = name;
        this.toolTipText = toolTipText;
        this.requiresAlpha = requiresAlpha;
    }

    /**
     * Returns if alpha is required or not
     *
     * @return true if alpha is required, otherwise false
     */
    public boolean isRequiresAlpha() {
        return this.requiresAlpha;
    }

    /**
     * Gets the tooltip text
     *
     * @return the tooltip text
     */
    public String getToolTipText() {
        return this.toolTipText;
    }

    @Override
    public String toString() {
        return this.name;
    }

}