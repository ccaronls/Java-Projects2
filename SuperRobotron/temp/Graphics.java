package cc.game.superrobotron.swing;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.ImageObserver;
import java.text.AttributedCharacterIterator;

import cc.game.superrobotron.Color;
import cc.game.superrobotron.AGraphics;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.swing.ImageMgr;

class Graphics extends AGraphics {
    
    private final java.awt.Graphics g;
    private final ImageMgr mImages;
    private final int mWidth, mHeight;
    
    Graphics(java.awt.Graphics g, Component comp) {
        this.g = g;
        this.mImages = new ImageMgr(comp);
        mWidth = comp.getWidth();
        mHeight = comp.getHeight();
    }
    
    @Override
    public void setColor(Color color) {
        g.setColor(color.color);
    }

    @Override
    public void drawDisk(int x, int y, int radius) {
        g.fillOval(Math.round(x-radius), Math.round(y-radius), Math.round(radius*2), Math.round(radius*2));
    }

    @Override
    public void drawOval(int x, int y, int w, int h) {
        g.drawOval(Math.round(x), Math.round(y), w, h);
    }

    @Override
    public void drawRect(int x, int y, int w, int h, int thickness) {
        Utils.drawRect(g, x, y, w, h, thickness);
    }

    @Override
    public void drawCircle(int x, int y, int r) {
        g.drawOval(x-r, y-r, r*2, r*2);
    }

    @Override
    public void drawImage(int imageKey, int x, int y, int w, int h) {
        mImages.drawImage(g, imageKey, x, y, w, h);
    }

    @Override
    public void drawString(int x, int y, Justify horz, Justify vert, String text) {
        Utils.drawJustifiedString(g, x, y, horz, vert, text);
    }

    public void drawQuad(int x, int y, int w, int h) {
        g.fillRect(x, y, w, h);
    }

    public void drawFilledOval(int x, int y, int w, int h) {
        g.fillOval(x, y, w, h);
    }

    public int getTextHeight() {
        return Utils.getFontHeight(g);
    }

    public void translate(int dx, int dy) {
        g.translate(dx, dy)
;    }

    public void drawFilledPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        g.fillPolygon(xPoints, yPoints, nPoints);
    }

    public void drawRect(int x, int y, int w, int h) {
        g.drawRect(x, y, w, h);
    }

    public void drawLine(int x0, int y0, int x1, int y1, int thickness) {
        Utils.drawLine(g, x0, y0, x1, y1, thickness);
    }

    public void drawLineStrip(int[] x_pts, int[] y_pts, int thickness) {
        Utils.drawLineStrip(g, x_pts, y_pts, thickness);
    }

    public int loadImage(String fileOrResourceName, Color transparent) {
        return mImages.loadImage(fileOrResourceName, transparent == null ? null : transparent.color);
    }

    public int[] loadImageCells(String file, int w, int h, int numCellsX, int numCellsY, boolean celled, Color transparent) {
        return mImages.loadImageCells(file, w, h, numCellsX, numCellsY, celled, transparent == null ? null : transparent.color);
    }

    public Color getColor() {
        return new Color(g.getColor());
    }
    
    public void clearScreen(Color clearColor) {
        Color c = getColor();
        setColor(clearColor);
        g.fillRect(0, 0, getWidth(), getHeight());
        setColor(c);
    }

    public int getWidth() {
        return mWidth;
    }
    
    public int getHeight() {
        return mHeight;
    }

    @Override
    public int getStringWidth(String s) {
        return Utils.getStringWidth(g, s);
    }

    
}
