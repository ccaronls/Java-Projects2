package cc.lib.android;

import cc.lib.game.AGraphics;
import cc.lib.game.GDimension;
import cc.lib.game.Utils;

public class DroidUtils extends Utils {

    /**
     * 
     * @param color
     * @return
     */
    public static String colorToString(GLColor color) {
        return String.valueOf(color.getRed()) + "," + String.valueOf(color.getGreen()) + "," + String.valueOf(color.getBlue());
    }

    /**
     * 
     * @param line
     * @return
     */
    public static GLColor stringToColor(String line) {
        try {
            String [] parts = line.split(",");
            return new GLColor(Float.parseFloat(parts[0]),
                             Float.parseFloat(parts[1]), 
                             Float.parseFloat(parts[2]));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return GLColor.RED;
    }

    /**
     * 
     * @param color
     * @return
     */
    public static int colorToARGB(float red, float green, float blue, float alpha) {
        int a = Math.round(alpha*255);
        int r = Math.round(red*255);
        int g = Math.round(green*255);
        int b = Math.round(blue*255);
        int d = ((a << 24) & 0xff000000) | 
                ((r << 16) & 0x00ff0000) | 
                ((g << 8)  & 0x0000ff00) | 
                ((b << 0)  & 0x000000ff);
        return d;
    }
    
    public static void multiply(float [] glMatrix16, float [] glVertex4) {
    	float x = glVertex4[0];
    	float y = glVertex4[1];
    	float z = glVertex4[2];
    	float w = glVertex4[3];
    	glVertex4[0] = x*glMatrix16[0] + y*glMatrix16[4] + z*glMatrix16[8] + w*glMatrix16[12];
    	glVertex4[1] = x*glMatrix16[1] + y*glMatrix16[5] + z*glMatrix16[9] + w*glMatrix16[13];
    	glVertex4[2] = x*glMatrix16[2] + y*glMatrix16[6] + z*glMatrix16[10] + w*glMatrix16[14];
    	glVertex4[3] = x*glMatrix16[3] + y*glMatrix16[7] + z*glMatrix16[11] + w*glMatrix16[15];
    }

    public static void debugAssert(boolean expression, String message) {
    	if (BuildConfig.DEBUG && !expression)
    		throw new AssertionError(message);
    }
    
    /*
/**
     * Detemine the minimum rectangle to hold the given text.
     * \n is a delim for each line.
     * @param g
     * @param txt
     * @return
     */
    public static GDimension computeTextDimension(AGraphics g, String txt) {
        String [] lines = txt.split("\n");
        int width = 0;
        final int height = g.getTextHeight() * lines.length;
        for (int i=0; i<lines.length; i++) {
            int w = Math.round(g.getTextWidth(lines[i]));
            if (w > width)
                width = w;
        }
        return new GDimension(width, height);
    }    
}
