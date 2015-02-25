package cc.game.superrobotron;

import cc.lib.game.*;

public abstract class AGraphics {

    /**
     * Set the current render color
     * @param c
     */
    public abstract void setColor(Color c);

    /**
     * Draw a justified string using the current render color
     * @param x
     * @param y
     * @param hJust
     * @param vJust
     * @param s
     */
    public abstract void drawString(int x, int y, Justify hJust, Justify vJust, String s);

    /**
     * Convenience method to draw string top justified
     * @param x
     * @param y
     * @param hJust
     * @param s
     */
    public void drawString(int x, int y, Justify hJust, String s) {
        drawString(x,y,hJust, Justify.TOP, s);
    }

    /**
     * Convenience method to draw a left/top justified string
     * @param x
     * @param y
     * @param s
     */
    public void drawString(int x, int y, String s) {
        drawString(x,y,Justify.LEFT,Justify.TOP,s);
    }

    /**
     * Draw the string and return width of the string
     * @param x
     * @param y
     * @param hJust
     * @param string
     * @return
     */
    public int drawStringLine(int x, int y, Justify hJust, String string) {
        this.drawString(x, y, hJust, string);
        return this.getStringWidth(string);
    }

    /**
     * Return the width of a string in pixels
     * @param s
     * @return
     */
    public abstract int getStringWidth(String s);
    
    public abstract void drawDisk(int x, int y, int radius);

    public abstract void drawRect(int x, int y, int w, int h, int thickness);

    public abstract void drawImage(int imageKey, int x, int y, int w, int h);

    public abstract void drawCircle(int x, int y, int radius);

    public abstract void drawQuad(int x0, int y0, int x1, int y1);

    public abstract int getTextHeight();

    public abstract void drawOval(int x, int y, int w, int h);

    public abstract void drawFilledOval(int x, int y, int w, int h);

    public abstract void drawLine(int x0, int y0, int x1, int y1, int lineThickness);

    public abstract void translate(int x, int y);

    public abstract void drawFilledPolygon(int[] ptsX, int[] ptsY, int lineThickness);

    public abstract void drawLineStrip(int[] pts_x, int[] pts_y, int thickness);

    public abstract int loadImage(String fileName, Color black);

    public abstract int[] loadImageCells(String string, int width, int height, int cellsX, int cellsY, boolean outlined, Color transparent);

    public abstract void clearScreen(Color fillColor);

    public void drawRect(int x, int y, int w, int h) {
        this.drawRect(x, y, w, h,1);
    }

    public void drawLine(int x0, int y0, int x1, int y1) {
        this.drawLine(x0, y0, x1, y1, 1);
    }


}
