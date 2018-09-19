package cc.lib.game;

import cc.lib.utils.Reflector;

/**
 * Abstract color class to support applet/android variations
 * 
 * @author ccaron
 *
 */
public final class GColor extends Reflector<GColor> {

    static {
        addAllFields(GColor.class);
    }

    public final static GColor BLACK       = new GColor(0f,0,0,1);
    public final static GColor WHITE       = new GColor(1f,1,1,1);
    public final static GColor RED         = new GColor(1f,0,0,1);
    public final static GColor BLUE        = new GColor(0f,0,1,1);
    public final static GColor GREEN       = new GColor(0f,1,0,1);
    public final static GColor CYAN        = new GColor(0f,1,1,1);
    public final static GColor MAGENTA     = new GColor(1f,0,1,1);
    public final static GColor YELLOW      = new GColor(1f,1,0,1);
    public final static GColor ORANGE      = new GColor(1f,0.4f,0,1);
    public final static GColor GRAY      	= new GColor(0.6f, 0.6f, 0.6f, 1);
    public final static GColor LIGHT_GRAY 	= new GColor(0.8f, 0.8f, 0.8f, 1);
    public final static GColor DARK_GRAY 	= new GColor(0.4f, 0.4f, 0.4f, 1);
    public final static GColor PINK         = new GColor(255, 175, 175);
    public final static GColor BROWN        = new GColor(165,42,42);
    public final static GColor CHOCOLATE    = new GColor(210,105,30);
    public final static GColor TRANSPARENT = new GColor(0f,0,0,0);
    public final static GColor TRANSLUSCENT_BLACK = BLACK.withAlpha(128);

    private int argb = 0;

    public GColor() { }

    public GColor(int r, int g, int b, int a) {
        set(r, g, b, a);
    }

    public GColor(int r, int g, int b) {
        this(r, g,b, 255);
    }

    public GColor(int argb) {
        this.argb = argb;
    }

    public GColor(float r, float g, float b, float a) {
        set(Math.round(Utils.clamp(r, 0, 1)*255),
                Math.round(Utils.clamp(g, 0, 1)*255),
                Math.round(Utils.clamp(b, 0, 1)*255),
                Math.round(Utils.clamp(a, 0, 1)*255));
    }

	/**
	 * Return red component value between 0-1
	 * @return
	 */
    public float getRed() {
        return ((float)red())/255;
    }
    
    /**
     * Return green component value between 0-1
     * @return
     */
    public float getGreen() {
        return ((float)green())/255;
    }
    
    /**
     * Return blue component value between 0-1
     * @return
     */
    public float getBlue() {
        return ((float)blue())/255;
    }

    /**
     * Return alpha component value between 0-1
     * @return
     */
    public float getAlpha() {
        return ((float)alpha())/255;
    }

    public int red() {
        return (argb>>16) & 0xff;
    }

    public int green() {
        return (argb>>8)&0xff;
    }

    public int blue() {
        return (argb>>0)&0xff;
    }

    public int alpha() {
        return (argb>>>24)&0xff;
    }

    public void set(int r, int g, int b, int a) {
        argb = (Utils.clamp(a, 0, 255)<<24)
                | (Utils.clamp(r, 0, 255)<<16)
                | (Utils.clamp(g, 0, 255)<<8)
                | Utils.clamp(b, 0, 255);
    }

    /**
     * 
     * @param amount value between 0-1 to indcate amount of RGB to remove
     * @return
     */
    public final GColor darkened(float amount) {
        if (amount < 0.01f)
            return this;
        float R = amount * getRed();
        float G = amount * getGreen();
        float B = amount * getBlue();
        R = Utils.clamp(getRed() - R, 0, 255);
        G = Utils.clamp(getGreen() - G, 0, 255);
        B = Utils.clamp(getBlue() - B, 0, 255);
        return new GColor(R, G, B, getAlpha());
    }

    /**
     * Return new color that is lightened of this
     *
     * @param amount value between 0-1 to indcate amount of RGB to add
     * @return
     */
    public final GColor lightened(float amount) {
        if (amount < 0.01f)
            return this;
        float R = amount * getRed();
        float G = amount * getGreen();
        float B = amount * getBlue();
        R = Utils.clamp(getRed() + R, 0, 255);
        G = Utils.clamp(getGreen() + G, 0, 255);
        B = Utils.clamp(getBlue() + B, 0, 255);
        return new GColor(R, G, B, getAlpha());
    }

    /**
     * 
     * @return
     */
    public final int toARGB() {
        return (((int)(getAlpha()*255))<<24) | (((int)(getRed()*255)) << 16) | (((int)(getGreen()*255))<<8) | (((int)(getBlue()*255)) << 0);
    }

    /**
     * 
     * @return
     */
    public final int toRGB() {
        return (((int)getRed()*255) << 16) | (((int)getGreen()*255)<<8) | (((int)getBlue()*255) << 0);
    }

    /**
     * Parses string of pattern [(a,)?r,g,b] into a color object
     * @param str
     * @return null of string not identified as a color
     */
    public static GColor fromString(String str) throws NumberFormatException {
        try {
            if (str.startsWith("[") && str.endsWith("]")) {
                String[] parts = str.substring(1, str.length() - 1).split("[,]");
                if (parts.length == 3) {
                    return new GColor(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                } else if (parts.length == 4) {
                    return new GColor(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[0]));
                }
            }
            throw new Exception("string '" + str + "' not in form '[(a,)?r,g,b]'");
        } catch (NumberFormatException e) {
            throw e;
        } catch (Exception e) {
            throw new NumberFormatException(e.getMessage());
        }
    }

    @Override
    public final String toString() {
        return "[" + alpha() + "," + red() + "," + green() + "," + blue() + "]";
    }

    @Override
    public final boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof GColor))
            return false;
        if (o == this)
        	return true;
        GColor c = (GColor)o;
        return c.toARGB() == toARGB();
    }

    /**
     * Return new color that is interpolation between this and parameter
     * @param target
     * @param factor
     * @return
     */
    public final GColor interpolateTo(GColor target, float factor) {

        if (factor > 0.99)
            return target;
        if (factor < 0.01)
            return this;

        float R = getRed()   * factor + target.getRed() * (1.0f - factor);
        float G = getGreen() * factor + target.getGreen() * (1.0f - factor);
        float B = getBlue()  * factor + target.getBlue() * (1.0f - factor);
        float A = getAlpha() * factor + target.getAlpha() * (1.0f - factor);
        
        return new GColor(R, G, B, A);
    }

    /**
     * Return a new color instance with RGB components of this but specified alpha
     *
     * @param alpha
     * @return
     */
	public final GColor withAlpha(float alpha) {
	    return new GColor(red(), green(), blue(), Math.round(alpha*255));
    }

    /**
     * Return a new color instance with RGB components of this but specified alpha
     *
     * @param alpha
     * @return
     */
    public final GColor withAlpha(int alpha) {
        return new GColor(red(), green(), blue(), alpha);
    }

    /**
	 * return a color with its components summed.
	 * 
	 * @param other
	 * @return
	 */
	public final GColor add(GColor other) {
		return new GColor(Math.min(1, getRed() + other.getRed()),
    		Math.min(1, getGreen() + other.getGreen()),
    		Math.min(1, getBlue() + other.getBlue()),
    		Math.min(1, getAlpha() + other.getAlpha()));
	}
}
