package cc.lib.game;

import cc.lib.utils.Reflector;

/**
 * Abstract color class to support applet/android variations
 * 
 * @author ccaron
 *
 */
public abstract class AColor extends Reflector<AColor> {

	/**
	 * Return red component value between 0-1
	 * @return
	 */
    public abstract float getRed();
    
    /**
     * Return green component value between 0-1
     * @return
     */
    public abstract float getGreen();
    
    /**
     * Return blue component value between 0-1
     * @return
     */
    public abstract float getBlue();
    
    /**
     * Return alpha component value between 0-1
     * @return
     */
    public abstract float getAlpha();
    
    /**
     * 
     * @param g
     * @param amount value between 0-1 to indcate amount of RGB to remove
     * @return
     */
    public final AColor darken(AGraphics g, float amount) {
        float R = amount * getRed();
        float G = amount * getGreen();
        float B = amount * getBlue();
        R = Utils.clamp(getRed() - R, 0, 255);
        G = Utils.clamp(getGreen() - G, 0, 255);
        B = Utils.clamp(getBlue() - B, 0, 255);
        return g.makeColor(R, G, B, getAlpha());
    }

    /**
     * 
     * @param g
     * @param amount value between 0-1 to indcate amount of RGB to add
     * @return
     */
    public final AColor lighten(AGraphics g, float amount) {
        float R = amount * getRed();
        float G = amount * getGreen();
        float B = amount * getBlue();
        R = Utils.clamp(getRed() + R, 0, 255);
        G = Utils.clamp(getGreen() + G, 0, 255);
        B = Utils.clamp(getBlue() + B, 0, 255);
        return g.makeColor(R, G, B, getAlpha());
    }
    
    /**
     * Return a when aWeight == 1.0
     * Return b when aWeight == 0.0
     * @param g
     * @param target
     * @param aWeight
     * @return
     */
    public final AColor interpolate(AGraphics g, AColor target, float aWeight) {
        if (aWeight > 0.99)
            return this;
        if (aWeight < 0.01)
            return target;
        float bWeight = 1.0f - aWeight;
        float newAlpha = (aWeight * getAlpha() + bWeight * target.getAlpha());
        float newRed   = (aWeight * getRed() + bWeight * target.getRed());
        float newGreen = (aWeight * getGreen() + bWeight * target.getGreen());
        float newBlue  = (aWeight * getBlue() + bWeight * target.getBlue());
        return g.makeColor(newRed,  newGreen, newBlue, newAlpha);
    }

    /**
     * 
     * @return
     */
    public final int toARGB() {
        return (((int)getAlpha()*255)<<24) | (((int)getRed()*255) << 16) | (((int)getGreen()*255)<<8) | (((int)getBlue()*255) << 0);
    }

    /**
     * 
     * @return
     */
    public final int toRGB() {
        return (((int)getRed()*255) << 16) | (((int)getGreen()*255)<<8) | (((int)getBlue()*255) << 0);
    }

    public abstract AColor darkened(float amount);
    
    public abstract AColor lightened(float amount);
    
    @Override
    public final String toString() {
        return "ARGB[" + getAlpha() + "," + getRed() + "," + getGreen() + "," + getBlue() + "]";
    }
    
    @Override
    public final boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof AColor))
            return false;
        if (o == this)
        	return true;
        AColor c = (AColor)o;
        return c.toARGB() == toARGB();
    }

    public final AColor interpolateTo(AGraphics g, AColor inner, float factor) {
        
        float R = getRed()   * factor + inner.getRed() * (1.0f - factor);
        float G = getGreen() * factor + inner.getGreen() * (1.0f - factor);
        float B = getBlue()  * factor + inner.getBlue() * (1.0f - factor);
        float A = getAlpha() * factor + inner.getAlpha() * (1.0f - factor);
        
        return g.makeColor(R, G, B, A);
    }

	public abstract AColor setAlpha(float alpha);

	/**
	 * return a color with its components summed.
	 * 
	 * @param g
	 * @param other
	 * @return
	 */
	public final AColor add(AGraphics g, AColor other) {
		return g.makeColor(Math.min(1, getRed() + other.getRed()),
    		Math.min(1, getGreen() + other.getGreen()),
    		Math.min(1, getBlue() + other.getBlue()),
    		Math.min(1, getAlpha() + other.getAlpha()));
	}
}
