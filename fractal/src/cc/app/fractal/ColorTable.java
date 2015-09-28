package cc.app.fractal;

import cc.lib.game.Utils;

public final class ColorTable {

    static final int LENGTH = 256;
    
    interface Filter {
        int filterRGB(int rgb);
    }
    
    Scale currentScale = Scale.GRAY_SCALE;
    
    ColorTable(Scale scale) {
        setScale(scale);
    }
    
    enum Scale {
        GRAY_SCALE,
        RED_SCALE,
        GREEN_SCALE,
        BLUE_SCALE,
        RAINBOW_SCALE,
    }
    
    private int [] colorTable = new int[LENGTH];
    
    int [] getColorTable() {
        return colorTable;
    }
    
    void filter(Filter filter) {
        for (int i=0; i<colorTable.length; i++) {
            colorTable[i] = filter.filterRGB(colorTable[i]);
        }
    }
    
    static int toARGB(int r, int g, int b) {
        return (255 << 24) | (r << 16) | (g << 8) | b;
    }
    
    void setScale(Scale scale) {
        switch (scale) {
            case GRAY_SCALE:
                for (int i=0; i<LENGTH; i++) {
                    colorTable[i] = toARGB(i, i, i);
                }
                break;
            case RED_SCALE:
                for (int i=0; i<LENGTH; i++) {
                    colorTable[i] = toARGB(i, 0, 0);
                }
                break;
            case GREEN_SCALE:
                for (int i=0; i<LENGTH; i++) {
                    colorTable[i] = 255<<24 | i<<8;
                }
                break;
            case BLUE_SCALE:
                for (int i=0; i<LENGTH; i++) {
                    colorTable[i] = toARGB(0, 0, i);
                }
                break;
            case RAINBOW_SCALE:
                for (int i=0; i<32; i++) {
                    colorTable[i]     = toARGB(i*8, 0, 0);
                    colorTable[32+i]  = toARGB(255-(i*8), (i*8), 0);
                    colorTable[64+i]  = toARGB(0, 255-(i*8), (i*8));
                    colorTable[96+i]  = toARGB(i*8, 0, 255-(i*8));
                    colorTable[128+i] = toARGB(255-(i*8), (i*8), (i*8));
                    colorTable[160+i] = toARGB(i*8, 255-(i*8), 255);
                    colorTable[192+i] = toARGB(255, i*8, 255-(i*8));
                    colorTable[224+i] = toARGB(255, 255, i*8);
                }
                break;
        }
        currentScale = scale;
    }
    
    void invertColors() {
        for (int i=0; i<this.colorTable.length; i++) {
            colorTable[i] = invert(colorTable[i]);
        }
    }
    
    void brightenColors() {
        for (int i=0; i<this.colorTable.length; i++) {
            colorTable[i] = brighten(colorTable[i]);
        }
    }

    void darkenColors() {
        for (int i=0; i<this.colorTable.length; i++) {
            colorTable[i] = darken(colorTable[i]);
        }
    }
    
    void rotateColors() {
    	int t = colorTable[0];
    	for (int i=0; i<this.colorTable.length-1; i++) {
    		colorTable[i] = colorTable[i+1];
    	}
    	colorTable[colorTable.length-1] = t;
    }

    int invert(int c) {     
        int r = 255 - ((c >> 16) & 0xff);
        int g = 255 - ((c >> 8)  & 0xff);
        int b = 255 - ((c >> 0)  & 0xff);
        return toARGB(r, g, b);
    }
    
    int clamp(int x, int min, int max) {
        if (x < min)
            x = min;
        else if (x > max)
            x = max;
        return x;
    }

    int scaleColor(int c, float amount) {
        int r = (c >> 16) & 0xff; 
        int g = (c >> 8) & 0xff; 
        int b = (c >> 0) & 0xff;
        float R = amount * (float)r;
        float G = amount * (float)g;
        float B = amount * (float)b;
        r = clamp(Math.round(R), 0, 255);
        g = clamp(Math.round(G), 0, 255);
        g = clamp(Math.round(B), 0, 255);
        return toARGB(r, g, b); 
    }

    int scaleColor2(int c, int amount) {
        int r = (c >> 16) & 0xff; 
        int g = (c >> 8) & 0xff; 
        int b = (c >> 0) & 0xff;
        if (r > 0)
        	r += amount;
        if (g > 0)
        	g += amount;
        if (b > 0)
        	b += amount;
        return redistributeRGB(r, g, b);
    }
    
    private int redistributeRGB(int r, int g, int b) {
        int mx = Utils.max(r, g, b);
        int mn = Utils.min(r, g, b);
        if (mx <= 255 && mn >= 0)
            return toARGB(r, g, b);
        int total = r + g + b;
        if (total >= 3 * 255)
            return toARGB(255, 255, 255);
        if (total <= 0)
            return toARGB(0, 0, 0);
        if (mx > 255) {
            float x = (3f * 255 - total) / (3f * mx - total);
            float gray = 255f - x * mx;
            return toARGB(Math.round(gray + x * r), Math.round(gray + x * g), Math.round(gray + x * b));
        } else if (mn < 0) {
            float x = (3f * 0 - total) / (3f * mn - total);
            float gray = 0f - x * mn;
            return toARGB(Math.round(gray + x * r), Math.round(gray + x * g), Math.round(gray + x * b));
        }
        return toARGB(r, g, b);
    }

    int brighten(int c) {
        return scaleColor2(c, 10);
    }
    
    int darken(int c) {
        return scaleColor2(c, -10);
    }    
    
    Scale getScale() {
        return this.currentScale;
    }
}
