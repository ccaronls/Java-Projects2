package cc.lib.game;

import cc.lib.math.Vector2D;

/**
 * Created by chriscaron on 2/14/18.
 */

public class TestGraphics extends APGraphics {

    public TestGraphics() {
        super(100, 100);
    }

    @Override
    public void setColor(AColor color) {

    }

    @Override
    public void setColorARGB(int argb) {

    }

    @Override
    public void setColorRGBA(int rgba) {

    }

    @Override
    public void setColor(int r, int g, int b, int a) {

    }

    @Override
    public AColor getColor() {
        return null;
    }

    @Override
    public int getTextHeight() {
        return 0;
    }

    @Override
    public void setTextHeight(float height) {

    }

    @Override
    public float getTextWidth(String string) {
        return 0;
    }

    @Override
    public float drawStringLine(float x, float y, Justify hJust, String text) {
        return 0;
    }

    @Override
    public float setLineWidth(float newWidth) {
        return 0;
    }

    @Override
    public float setPointSize(float newSize) {
        return 0;
    }

    @Override
    public void drawPoints() {

    }

    @Override
    public void drawLines() {

    }

    @Override
    public void drawLineStrip() {

    }

    @Override
    public void drawLineLoop() {

    }

    @Override
    public void drawTriangles() {

    }

    @Override
    public void drawTriangleFan() {

    }

    @Override
    public void drawTriangleStrip() {

    }

    @Override
    public void drawQuadStrip() {

    }

    @Override
    public int loadImage(String assetPath, AColor transparent) {
        return 0;
    }

    @Override
    public int[] loadImageCells(String assetPath, int w, int h, int numCellsX, int numCells, boolean bordeered, AColor transparent) {
        return new int[0];
    }

    @Override
    public AImage getImage(int id) {
        return null;
    }

    @Override
    public AImage getImage(int id, int width, int height) {
        return null;
    }

    @Override
    public void deleteImage(int id) {

    }

    @Override
    public int newSubImage(int id, int x, int y, int w, int h) {
        return 0;
    }

    @Override
    public int newRotatedImage(int id, int degrees) {
        return 0;
    }

    @Override
    public int newTransformedImage(int id, IImageFilter filter) {
        return 0;
    }

    @Override
    public void enableTexture(int id) {

    }

    @Override
    public void disableTexture() {

    }

    @Override
    public void texCoord(float s, float t) {

    }

    @Override
    public boolean isTextureEnabled() {
        return false;
    }

    @Override
    public void drawImage(int imageKey, int x, int y, int w, int h) {

    }

    @Override
    public void clearScreen(AColor color) {

    }

    @Override
    public AColor makeColor(final float r, final float g, final float b, final float a) {
        return new AColor() {
            @Override
            public float getRed() {
                return r;
            }

            @Override
            public float getGreen() {
                return g;
            }

            @Override
            public float getBlue() {
                return b;
            }

            @Override
            public float getAlpha() {
                return a;
            }

            @Override
            public AColor darkened(float amount) {
                return null;
            }

            @Override
            public AColor lightened(float amount) {
                return null;
            }

            @Override
            public AColor setAlpha(float alpha) {
                return makeColor(alpha, r, g, b);
            }
        };
    }
}
