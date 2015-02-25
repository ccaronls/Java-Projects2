package cc.game.superrobotron;

import cc.lib.game.Utils;

public class Color {

    public static final Color CYAN = new Color(java.awt.Color.cyan);
    public static final Color WHITE = new Color(java.awt.Color.white);
    public static final Color GREEN = new Color(java.awt.Color.green);
    public static final Color ORANGE = new Color(java.awt.Color.orange);
    public static final Color YELLOW = new Color(java.awt.Color.yellow);
    public static final Color BLACK = new Color(java.awt.Color.black);
    public static final Color RED = new Color(java.awt.Color.red);
    public static final Color BLUE = new Color(java.awt.Color.blue);
    public static final Color DARK_GRAY = new Color(java.awt.Color.darkGray);
    public static final Color LIGHT_GRAY = new Color(java.awt.Color.lightGray);
    
    public final java.awt.Color color;
    
    public Color(int r, int g, int b, int a) {
        color = new java.awt.Color(r,g,b,a);
    }
    public Color(int r, int g, int b) {
        color = new java.awt.Color(r,g,b,255);
    }
    public Color(java.awt.Color color) {
        this.color = color;
    }
    public Color lightened(float amount) {
        return new Color(Utils.lighten(color, amount));
    }
    public Color darkened(float amount) {
        return new Color(Utils.darken(color, amount));
    }
    public int getRed() {
        return color.getRed();
    }
    public int getGreen() {
        return color.getGreen();
    }
    public int getBlue() {
        return color.getBlue();
    }
    public Color interpolateTo(Color inner, float factor) {
        return new Color(Utils.interpolate(color, inner.color, factor));
    }

}
