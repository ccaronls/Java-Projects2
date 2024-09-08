package cc.lib.game;

import cc.lib.reflector.Reflector;

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
    public final static GColor DARK_OLIVE   = new GColor(0xff808000);
    public final static GColor LIGHT_OLIVE  = new GColor(0xffcdcd00);
    public final static GColor GOLD         = new GColor(0xFFFFD700);
    public final static GColor SLIME_GREEN  = new GColor(0xffa1e203);
    public final static GColor SKY_BLUE = new GColor(0xff87ceeb);
    public final static GColor TRUE_BLUE = new GColor(0xff0073cf);

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

    public static GColor fromRGB(int rgb) {
        return new GColor(0xff000000 | rgb);
    }

    public static GColor fromARGB(int argb) {
        return new GColor(argb);
    }

    public GColor(GColor toCopy) {
        this(toCopy.argb);
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
        return argb;
    }

    /**
     * 
     * @return
     */
    public final int toRGB() {
        return 0xff000000 | argb;
    }

    /**
     * This function added for AWT but AWT color is NOT in RGBA format even though input parameter suggests it is.
     * @see java.awt.Color(int,boolean)
     *
     * @return
     */
    @Deprecated
    public final int toRGBA() {
        int alpha = (argb >>> 24) & 0xff;
        return (argb << 8) | alpha;
    }

    /**
     * Parses string of pattern [(a,)?r,g,b] into a color object
     * @param str
     * @return null of string not identified as a color
     */
    public static GColor fromString(String str) throws NumberFormatException {
        try {
            String [] parts;
            if (str.startsWith("ARGB[") && str.endsWith("]")) {
                parts = str.substring(5, str.length() - 1).split("[,]");
            } else if (str.startsWith("[") && str.endsWith("]")) {
                parts = str.substring(1, str.length() - 1).split("[,]");
            } else
                throw new Exception("string '" + str + "' not in form '[(a,)?r,g,b]'");
            if (parts.length == 3) {
                return new GColor(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            } else if (parts.length == 4) {
                return new GColor(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[0]));
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
        return String.format("ARGB[%d,%d,%d,%d]", alpha(), red(), green(), blue());
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
        return argb == c.argb;
    }

    public final boolean equalsWithinThreshold(GColor c, int threshold) {
        if (c == null)
            return false;
        if (c == this)
            return true;
        if (Math.abs(alpha()-c.alpha())>threshold)
            return false;
        if (Math.abs(red()-c.red())>threshold)
            return false;
        if (Math.abs(green()-c.green())>threshold)
            return false;
        if (Math.abs(blue()-c.blue())>threshold)
            return false;
        return true;
    }

    /**
     * Return new color that is interpolation between this and parameter
     * @param target
     * @param factor
     * @return
     */
    public final GColor interpolateTo(GColor target, float factor) {

        if (factor > 0.99)
            return this;
        if (factor < 0.01)
            return target;

        float R = Utils.clamp(getRed()   * factor + target.getRed() * (1.0f - factor), 0, 1);
        float G = Utils.clamp(getGreen() * factor + target.getGreen() * (1.0f - factor), 0, 1);
        float B = Utils.clamp(getBlue()  * factor + target.getBlue() * (1.0f - factor), 0, 1);
        float A = Utils.clamp(getAlpha() * factor + target.getAlpha() * (1.0f - factor), 0, 1);
        
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
     * Return a new color instance with RGB components of this and the specified alpha
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

    @Override
    protected boolean isImmutable() {
        return true;
    }

    /**
     * Return color with RGB components equal to 1-RGB. [.5,.5,.5] will be unchanged.
     * @return
     */
    public final GColor inverted() {
	    return new GColor(1f-getRed(), 1f-getGreen(), 1f-getBlue(), getAlpha());
    }

    public final IInterpolator<GColor> getInterpolator(GColor target) {
        return position -> interpolateTo(target, position);
    }
}
