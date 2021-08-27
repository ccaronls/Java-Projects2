package cc.lib.game;

/**
 * Created by chriscaron on 2/14/18.
 */

public class TestGraphics extends APGraphics {

    public TestGraphics() {
        super(100, 100);
    }

    @Override
    public void setColor(GColor color) {

    }

    @Override
    public void setColorARGB(int argb) {

    }

    @Override
    public void setColor(int r, int g, int b, int a) {

    }

    @Override
    public GColor getColor() {
        return null;
    }

    @Override
    public float getTextHeight() {
        return 0;
    }

    @Override
    public float setTextHeight(float height) {
        return 0;
    }

    @Override
    public float getTextWidth(String string) {
        return 0;
    }

    @Override
    public void setTextStyles(TextStyle... styles) {

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
    public void drawRects() {

    }

    @Override
    public void drawFilledRects() {

    }

    @Override
    public int loadImage(String assetPath, GColor transparent) {
        return 0;
    }

    @Override
    public int[] loadImageCells(String assetPath, int w, int h, int numCellsX, int numCells, boolean bordered, GColor transparent) {
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
    public void clearScreen(GColor color) {

    }

    @Override
    public void setClipRect(float x, float y, float w, float h) {

    }

    @Override
    public GRectangle getClipRect() {
        return null;
    }

    @Override
    public void clearClip() {

    }

    @Override
    public void setTransparencyFilter(float alpha) {

    }

    @Override
    public void removeFilter() {

    }

    @Override
    public void drawRoundedRect(float x, float y, float w, float h, float radius) {

    }

    @Override
    public void drawFilledRoundedRect(float x, float y, float w, float h, float radius) {

    }

    @Override
    public void drawWedge(float cx, float cy, float radius, float startDegrees, float sweepDegrees) {

    }

    @Override
    public void drawArc(float x, float y, float radius, float startDegrees, float sweepDegrees) {

    }

    @Override
    public void drawOval(float x, float y, float w, float h) {

    }

    @Override
    public void drawFilledOval(float x, float y, float w, float h) {

    }

    @Override
    public void drawImage(int imageKey) {

    }

    @Override
    public GColor getBackgroundColor() {
        return null;
    }

    @Override
    public void drawDashedLine(float x0, float y0, float x1, float y1, float thickness, float dashLength) {

    }

    @Override
    public void setTintFilter(GColor inColor, GColor outColor) {

    }
}
