package cc.lib.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.RGBImageFilter;

import cc.lib.game.*;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;

public final class AWTGraphics extends AGraphics {

    private final Graphics g;
    private final AWTRenderer r;
    private final ImageMgr images;
    private final Component comp;
    //private int textureId = -1;
    private float mLineThickness = 1;
    private float mPointSize = 1;
    
    public AWTGraphics(Graphics g, Component comp) {
        super(comp.getWidth(), comp.getHeight());
        this.g = g;
        this.r = new AWTRenderer(this);
        images = new ImageMgr(comp);
        this.comp  = comp;
        initViewport(comp.getWidth(), comp.getHeight());
        ortho();
    }
    
    public AWTGraphics(AWTGraphics g, Graphics G, Component comp) {
        super(g.comp.getWidth(), g.comp.getHeight());
        this.g = G;
        this.r = g.r;
        r.setWindow(this);
        images = g.images;
        this.comp  = g.comp;
        initViewport(comp.getWidth(), comp.getHeight());
        ortho();
    }

    public final int getScreenWidth() {
        return this.getViewportWidth();
    }

    public final int getScreenHeight() {
        return this.getViewportHeight();
    }

    @Override
    public final void setColor(AColor color) {
        g.setColor(((AWTColor)color).color);
    }

    @Override
    public final AColor getColor() {
        return new AWTColor(g.getColor());
    }

    @Override
    public final  int getTextHeight() {
        return AWTUtils.getFontHeight(g);
    }

    @Override
    public final float getTextWidth(String string) {
        return AWTUtils.getStringWidth(g, string);
    }

    @Override
    public final  float drawStringLine(float x, float y, Justify hJust, String text) {
        AWTUtils.drawJustifiedString(g, Math.round(x), Math.round(y), hJust, Justify.TOP, text);
        return this.getTextWidth(text);
    }

    @Override
    public final  float setLineWidth(float newWidth) {
        if (newWidth >= 1) {
            float oldThickness = mLineThickness;
            mLineThickness = newWidth;
            return oldThickness;
        }
        error("Invalid parameter to setLinethickness " + newWidth + ".  value is ignored");
        return mLineThickness;
    }

    @Override
    public final  float setPointSize(float newSize) {
        if (newSize >= 1) {
            float oldSize = mPointSize;
            mPointSize = newSize;
            return oldSize;
        }
        error("Invalid parameter to setPointSize " + newSize + ".  value is ignored");
        return mPointSize;
    }

    @Override
    public final  void transform(float x, float y, float[] result) {
        r.transformXY(x, y, result);
    }
    
    public final void transform(MutableVector2D v) {
    	r.transformXY(v);
    }
    
    public final MutableVector2D transform(IVector2D v) {
    	return r.transformXY(v);
    }
    
    @Override
    public final Vector2D screenToViewport(int screenX, int screenY) {
    	return r.untransform(screenX, screenY);
    }

    @Override
    public final  void vertex(float x, float y) {
        r.addVertex(x, y);
    }

    @Override
    public final  void drawPoints() {
        r.drawPoints(g, Math.round(mPointSize));
    }

    @Override
    public final  void drawLines() {
        r.drawLines(g, Math.round(mLineThickness));
    }

    public final void drawLines(float thickness) {
    	r.drawLines(g, Math.round(thickness));
    }
    
    @Override
    public void drawLineStrip() {
        r.drawLineStrip(g, Math.round(mLineThickness));
    }

    public void drawLineStrip(float thickness) {
        r.drawLineStrip(g, Math.round(thickness));
    }

    @Override
    public final  void drawLineLoop() {
        r.drawLineLoop(g, Math.round(mLineThickness));
    }

    @Override
    public final  void drawTriangles() {
        r.fillTriangles(g);
    }

    @Override
    public final  void drawTriangleFan() {
        r.drawTriangleFan(g);
    }

    @Override
    public final  void drawTriangleStrip() {
        r.drawTriangleStrip(g);
    }
    
    @Override
    public final void drawQuadStrip() {
    	r.fillQuadStrip(g);
    }

    @Override
    public final  int loadImage(String assetPath, AColor transparent) {
        return images.loadImage(assetPath, transparent == null ? null : ((AWTColor)transparent).color);
    }
    
    @Override
    public final int[] loadImageCells(String assetPath, int w, int h, int numCellsX, int numCellsY, boolean bordered, AColor transparent) {
        return images.loadImageCells(assetPath, w, h, numCellsX, numCellsY, bordered, ((AWTColor)transparent).color);
    }

    @Override
    public final  AImage getImage(int id) {
        return new AWTImage(images.getImage(id), comp);
    }
    
    @Override
    public final  AImage getImage(int id, int width, int height) {
        return new AWTImage(images.getImage(id, width, height), comp);
    }

    @Override
    public final  int newRotatedImage(int id, int degrees) {
        return images.newRotatedImage(id, degrees);
    }

    public final  int newTransformedImage(int id, final IImageFilter filter) {
        return images.addImage(images.transform(images.getSourceImage(id), new RGBImageFilter() {

            @Override
            public int filterRGB(int x, int y, int rgb) {
                return filter.filterRGBA(x, y, rgb);
            }
            
        }));
    }
    
    @Override
    public final  int newSubImage(int id, int x, int y, int w, int h) {
        Image source = images.getSourceImage(id);
        return images.newSubImage(source, x, y, w, h);
    }

    @Override
    public final  void enableTexture(int id) {
    	throw new RuntimeException("Unsupported operation");
    }

    @Override
    public final  void disableTexture() {
    	throw new RuntimeException("Unsupported operation");
    }

    @Override
    public final  void texCoord(float s, float t) {
    	throw new RuntimeException("Unsupported operation");
    }

    @Override
    public final  void drawImage(int imageKey, int x, int y, int w, int h) {
        Vector2D v = r.transformXY(x, y);
        images.drawImage(g, imageKey, v.Xi(), v.Yi(), w, h);
    }

    @Override
    public final  void pushMatrix() {
        r.pushMatrix();
    }

    @Override
    public final  void popMatrix() {
        r.popMatrix();
    }

    @Override
    public final  void translate(float x, float y) {
        r.translate(x, y);
    }
    
    @Override
    public final  void rotate(float degrees) {
        r.rotate(degrees);
    }

    @Override
    public final  void scale(float x, float y) {
        r.scale(x, y);
    }

    @Override
    public final  void setIdentity() {
        r.makeIdentity();
    }

    @Override
    public final  AColor makeColor(float r, float g, float b, float a) {
        return new AWTColor(new Color(r, g, b, a));
    }

    @Override
    public final  void begin() {
        r.clearVerts();
    }

    @Override
    public final  void end() {
        r.clearVerts();
    }

    @Override
    public final  boolean isTextureEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public final  void clearScreen(AColor color) {
        Color c = g.getColor();
        g.setColor(((AWTColor)color).color);
        g.fillRect(0, 0, this.getViewportWidth(), this.getViewportHeight());
        g.setColor(c);
    }

    @Override
    public final  void ortho(float left, float right, float top, float bottom) {
        r.setOrtho(left, right, top, bottom);
    }

    public final  void setFont(Font font) {
        g.setFont(font);
    }

	@Override
	public final void clearMinMax() {
		r.clearBoundingRect();
	}

	@Override
	public final Vector2D getMinBoundingRect() {
		return r.getMin();
	}

	@Override
	public final Vector2D getMaxBoundingRect() {
		return r.getMax();
	}

	public void setColor(Color c) {
		g.setColor(c);
	}

	public Font getFont() {
		return g.getFont();
	}

	public void drawLineLoop(int thickness) {
		r.drawLineLoop(g, thickness);
	}

	public void fillPolygon() {
		r.fillPolygon(g);
	}

	public void setName(int index) {
		r.setName(index);
	}

	public int pickLines(int mouseX, int mouseY, int thickness) {
		return r.pickLines(mouseX, mouseY, thickness);
	}

	public int pickPoints(int mouseX, int mouseY, int size) {
		return r.pickPoints(mouseX, mouseY, size);
	}
	
}
