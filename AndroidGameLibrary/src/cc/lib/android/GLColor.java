package cc.lib.android;


import cc.lib.game.AColor;

public final class GLColor extends AColor {

    // Colors
    public static final GLColor RED = new GLColor(1,0,0);
    public static final GLColor GREEN = new GLColor(0,1,0);
    public static final GLColor BLUE = new GLColor(0,0,1);
    public static final GLColor CYAN = new GLColor(0,1,1);
    public static final GLColor YELLOW = new GLColor(1,1,0);
    public static final GLColor MAGENTA = new GLColor(1,0,1);
    public static final GLColor WHITE = new GLColor(1,1,1);
    public static final GLColor BLACK = new GLColor(0,0,0);
    public static final GLColor ORANGE = new GLColor(1,0.6f,0);
    public static final GLColor DARK_GRAY = new GLColor(0.35f, 0.35f, 0.35f);
    public static final GLColor LIGHT_GRAY = new GLColor(0.8f, 0.8f, 0.8f);

    static {
    	addField(GLColor.class, "argb");
    }
    
    public GLColor() {
    	this(0);
    }
    
    private final int argb;
    
    public GLColor(GLColor c) {
        argb = c.argb;
    }
    
    public GLColor(int c) {
        this.argb = c;
    }
    
    public GLColor(float r, float g, float b) {
        this(r,g,b,1);
    }
    
    public GLColor(float r, float g, float b, float a) {
        argb = DroidUtils.colorToARGB(r, g, b, a);
    }

    public int getRedB() { return ((argb&0x00ff0000)>>16); } 
    public int getGreenB() { return ((argb&0x0000ff00)>>8); } 
    public int getBlueB() { return ((argb&0x000000ff)>>0); } 
    public int getAlphaB() { return ((argb&0xff000000)>>>24); } 
    
    public float getRed() { return ((float)getRedB() / 255); } 
    public float getGreen() { return ((float)getGreenB() / 255); } 
    public float getBlue() { return ((float)getBlueB() / 255); } 
    public float getAlpha() { return ((float)getAlphaB() / 255); } 
    
    /**
     * 
     * @return
     */
    public int toInt() {
        return argb;
    }
    
    /**
     * 
     * @param darkenAmount value between [0-1]
     * @return
     */
    public GLColor darkened(float darkenAmount) {
        float R = darkenAmount * getRed();
        float G = darkenAmount * getGreen();
        float B = darkenAmount * getBlue();
        R = DroidUtils.clamp(getRed() - R, 0, 1);
        G = DroidUtils.clamp(getGreen() - G, 0, 1);
        B = DroidUtils.clamp(getBlue() - B, 0, 1);
        return new GLColor(R,G,B,getAlpha());       
    }
    
    public GLColor lightened(float lightenAmount) {
        float R = lightenAmount * getRed();
        float G = lightenAmount * getGreen();
        float B = lightenAmount * getBlue();
        R = DroidUtils.clamp(getRed() + R, 0, 1);
        G = DroidUtils.clamp(getGreen() + G, 0, 1);
        B = DroidUtils.clamp(getBlue() + B, 0, 1);
        return new GLColor(R,G,B,getAlpha());       
    }
    
    /**
     * Return a color that is a blend of this and other.
     * formulae: thisWeight*this + 1/thisWeight*other
     * 
     * @param other
     * @param thisWeight
     * @return
     */
    public GLColor interpolateTo(GLColor other, float thisWeight) {
        if (thisWeight > 0.99)
            return this;
        if (thisWeight < 0.01)
            return other;
        float bWeight = 1.0f - thisWeight;
        float newAlpha = thisWeight * getAlpha() + bWeight * other.getAlpha();
        float newRed   = thisWeight * getRed() + bWeight * other.getRed();
        float newGreen = thisWeight * getGreen() + bWeight * other.getGreen();
        float newBlue  = thisWeight * getBlue() + bWeight * other.getBlue();
        return new GLColor(newRed,  newGreen, newBlue, newAlpha);            
    }

	@Override
	public AColor setAlpha(float alpha) {
		if (alpha == getAlpha())
			return this;
		return new GLColor(getRed(), getGreen(), getBlue(), alpha);
	}

    
}