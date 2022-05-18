package cc.lib.swing;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.RGBImageFilter;
import java.io.File;
import java.util.Arrays;

import cc.lib.game.AImage;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.IImageFilter;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.utils.GException;

public class AWTGraphics extends APGraphics {

    private Graphics g;
    protected final static AWTImageMgr images = new AWTImageMgr();
    protected final Component comp;
    //private int textureId = -1;
    private float mLineThickness = 1;
    private float mPointSize = 1;
    protected int [] x = new int[32];
    protected int [] y = new int[32];

    public static AWTImageMgr getImages() {
        return images;
    }

    private int getCurrentFontHeight() {
        return g.getFontMetrics().getFont().getSize();
    }

    int getPolyPts() {
        int n = getNumVerts();
        if (x.length < n) {
            x = new int[n*2];
            y = new int[n*2];
        }
        for (int i=0; i<n; i++) {
            x[i] = Math.round(getX(i));
            y[i] = Math.round(getY(i));
        }
        return n;
    }

    public AWTGraphics(Graphics g, Component comp) {
        super(comp.getWidth(), comp.getHeight());
        this.g = g;
        this.comp  = comp;
        initViewport(comp.getWidth(), comp.getHeight());
        ortho();
        setIdentity();
    }
    
    public AWTGraphics(AWTGraphics g, Graphics G, Component comp) {
        super(g.comp.getWidth(), g.comp.getHeight());
        this.g = G;
        R.setWindow(this);
        this.comp  = g.comp;
        initViewport(comp.getWidth(), comp.getHeight());
        ortho();
    }

    public final Graphics getGraphics() {
        return g;
    }

    public void setXorMode(GColor color) {
        if (color == null)
            g.setPaintMode();
        else
            g.setXORMode(new Color(color.toARGB(), true));
    }

    public void setGraphics(Graphics g) {
        g.setFont(this.g.getFont());
        this.g = g;
    }

    public final AWTImageMgr getImageMgr() {
        return images;
    }

    @Override
    public void setColor(GColor color) {
        g.setColor(new Color(color.toARGB(), true));
    }

    @Override
    public GColor getColor() {
        Color c = g.getColor();
        return new GColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    @Override
    public GColor getBackgroundColor() {
        Color c = comp.getBackground();
        return new GColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    public void setBackgroundColor(GColor color) {
        comp.setBackground(new Color(color.toARGB(), true));
    }

    @Override
    public final float getTextHeight() {
        return getCurrentFontHeight();
    }

    @Override
    public final float setTextHeight(float height) {
        int oldHeight = getCurrentFontHeight();
        Font newFont = g.getFont().deriveFont((float)(int)height);
        g.setFont(newFont);
        return oldHeight;
    }

    private TextStyle [] existingStyle = new TextStyle[0];

    @Override
    public void setTextStyles(TextStyle ... styles) {

        if (Arrays.deepEquals(existingStyle, styles))
            return;

        for (TextStyle style : styles) {
            switch (style) {

                case NORMAL:
                    g.setFont(g.getFont().deriveFont(Font.PLAIN));
                    break;
                case BOLD:
                    g.setFont(g.getFont().deriveFont(Font.BOLD));
                    break;
                case ITALIC:
                    g.setFont(g.getFont().deriveFont(Font.ITALIC));
                    break;
                case MONOSPACE: {
                    Font f = Font.decode(Font.MONOSPACED);
                    Font x = g.getFont();
                    g.setFont(f.deriveFont(x.getStyle(), x.getSize2D()));
                    break;
                }

                case UNDERLINE:
                    //g.setFont(g.getFont().deriveFont(Font.ITALIC));

                    //break;
                default:
                    error("Ignoring unsupported text style: " + style);
            }
        }

        existingStyle = styles;
    }

    @Override
    public final float getTextWidth(String string) {
        return AWTUtils.getStringWidth(g, string);
    }

    @Override
    public final float drawStringLine(float x, float y, Justify hJust, String text) {
        int leading = g.getFontMetrics().getLeading();
        int ascent = g.getFontMetrics().getAscent();
        int descent = g.getFontMetrics().getDescent();
        AWTUtils.drawJustifiedString(g, Math.round(x), Math.round(y)-descent, hJust, Justify.TOP, text);
        return this.getTextWidth(text);
    }

    @Override
    public float setLineWidth(float newWidth) {
        if (newWidth >= 1) {
            float oldThickness = mLineThickness;
            mLineThickness = newWidth;
            return oldThickness;
        }
        error("Invalid parameter to setLinethickness " + newWidth + ".  value is ignored");
        return mLineThickness;
    }

    @Override
    public final float setPointSize(float newSize) {
        float oldSize = mPointSize;
        mPointSize = Math.max(1, newSize);
        return oldSize;
    }

    protected int getNumVerts() {
        return R.getNumVerts();
    }

    protected float getX(int index) {
        return R.getX(index);
    }

    protected float getY(int index) {
        return R.getY(index);
    }

    protected Vector2D getVertex(int index) {
        return R.getVertex(index);
    }

    @Override
    public void drawPoints() {
        //r.drawPoints(g, Math.round(mPointSize));
        int size = Math.round(mPointSize);
        if (size <= 1) {
            for (int i=0; i<getNumVerts(); i++) {
                g.fillRect(Math.round(getX(i)), Math.round(getY(i)), 1, 1);
            }
        } else {
            for (int i=0; i<getNumVerts(); i++) {
                g.fillOval(Math.round(getX(i)-size/2), Math.round(getY(i)-size/2), size, size);
            }
        }
    }

    @Override
    public void drawLines() {
        //r.drawLines(g, Math.round(mLineThickness));
        for (int i=0; i<getNumVerts(); i+=2) {
            if (i+1 < getNumVerts())
                AWTUtils.drawLine(g, getX(i), getY(i), getX(i+1), getY(i+1), Math.round(mLineThickness));
        }
    }

    @Override
    public void drawLineStrip() {
        //r.drawLineStrip(g, Math.round(mLineThickness));
        for (int i=0; i<getNumVerts()-1; i++) {
            AWTUtils.drawLine(g, getX(i), getY(i), getX(i+1), getY(i+1), Math.round(mLineThickness));
        }
    }

    @Override
    public void drawLineLoop() {
        //r.drawLineLoop(g, Math.round(mLineThickness));
        int thickness = Math.round(mLineThickness);
        if (thickness <= 1) {
            if (getNumVerts() > 1) {
                for (int i=0; i<getNumVerts()-1; i++) {
                    AWTUtils.drawLine(g, getX(i), getY(i), getX(i+1), getY(i+1), 1);
                }
                int lastIndex = getNumVerts()-1;
                AWTUtils.drawLine(g, getX(lastIndex), getY(lastIndex), getX(0), getY(0), 1);
            }
            return;
        }
        if (getNumVerts() > 1) {
            for (int i=1; i<getNumVerts(); i++) {
                float x0 = getX(i-1);
                float y0 = getY(i-1);
                float x1 = getX(i);
                float y1 = getY(i);
                AWTUtils.drawLine(g, x0, y0, x1, y1, thickness);
            }
            if (getNumVerts()>2) {
                float x0 = getX(getNumVerts()-1);
                float y0 = getY(getNumVerts()-1);
                float x1 = getX(0);
                float y1 = getY(0);
                AWTUtils.drawLine(g, x0, y0, x1, y1, thickness);
            }
        }
    }

    @Override
    public final  void drawTriangles() {
        //r.fillTriangles(g);
        for (int i=0; i<=getNumVerts()-3; i+=3) {
            AWTUtils.fillTrianglef(g, getX(i), getY(i), getX(i+1), getY(i+1), getX(i+2), getY(i+2));
        }
    }

    @Override
    public final  void drawTriangleFan() {
        ///r.drawTriangleFan(g);
        for (int i=1; i<getNumVerts()-1; i+=1) {
            //AWTUtils.drawTriangle(g, getX(i), getY(i), getX(i+1), getY(i+1), getX(i+2), getY(i+2));
            AWTUtils.fillTrianglef(g, getX(0), getY(0), getX(i), getY(i), getX(i+1), getY(i+1));
        }
    }

    @Override
    public final  void drawTriangleStrip() {
        //r.drawTriangleStrip(g);
        for (int i=0; i<=getNumVerts()-3; i+=1) {
            AWTUtils.fillTrianglef(g, getX(i), getY(i), getX(i+1), getY(i+1), getX(i+2), getY(i+2));
        }
    }
    
    @Override
    public final void drawQuadStrip() {
    	//r.fillQuadStrip(g);
        for (int i=0; i<=getNumVerts()-4; i+=2) {
            AWTUtils.fillTrianglef(g, getX(i), getY(i), getX(i+1), getY(i+1), getX(i+2), getY(i+2));
            AWTUtils.fillTrianglef(g, getX(i+1), getY(i+1), getX(i+2), getY(i+2), getX(i+3), getY(i+3));
        }
    }

    @Override
    public void drawRects() {
        for (int i=0; i<=getNumVerts()-1; i+=2) {
            Vector2D v0 = getVertex(i);
            Vector2D v1 = getVertex(i+1);
            int x = Math.min(v0.Xi(), v1.Xi());
            int y = Math.min(v0.Yi(), v1.Yi());
            int w = Math.abs(v0.Xi()-v1.Xi());
            int h = Math.abs(v0.Yi()-v1.Yi());
            g.drawRect(x, y, w, h);
        }
    }

    @Override
    public void drawFilledRects() {
        for (int i=0; i<=getNumVerts()-1; i+=2) {
            Vector2D v0 = getVertex(i);
            Vector2D v1 = getVertex(i+1);
            int x = Math.min(v0.Xi(), v1.Xi());
            int y = Math.min(v0.Yi(), v1.Yi());
            int w = Math.abs(v0.Xi()-v1.Xi());
            int h = Math.abs(v0.Yi()-v1.Yi());
            g.fillRect(x, y, w, h);
        }
    }

    public final void addSearchPath(String path) {
        try {
            File fullPath = new File(path).getCanonicalFile();
            if (!fullPath.isDirectory())
                throw new GException("Not a path " + path + " on root path: '" + new File(".").getCanonicalPath() + "'");
        } catch (GException e) {
            throw e;
        } catch (Exception e) {
            throw new GException(e);
        }
        images.addSearchPath(path);
    }

    public final int loadImage(String assetPath, int degrees) {
        int id = images.loadImage(assetPath);
        if (id < 0)
            return id;
        return images.newRotatedImage(id, degrees, comp);
    }

    public final int addImage(Image img) {
        return images.addImage(img);
    }

    public final int loadImage(String assetPath, GColor transparent, int maxCopies) {
        return images.loadImage(assetPath, transparent == null ? null : AWTUtils.toColor(transparent), maxCopies);
    }

    @Override
    public final  int loadImage(String assetPath, GColor transparent) {
        return images.loadImage(assetPath, transparent == null ? null : AWTUtils.toColor(transparent));
    }
    
    @Override
    public final int[] loadImageCells(String assetPath, int w, int h, int numCellsX, int numCells, boolean bordered, GColor transparent) {
        return images.loadImageCells(assetPath, w, h, numCellsX, numCells, bordered, AWTUtils.toColor(transparent));
    }

    public final int [] loadImageCells(String assetPath, int [][] cells) {
        return images.loadImageCells(assetPath, cells);
    }

    @Override
	public void deleteImage(int id) {
    	images.deleteImage(id);
	}

	@Override
    public final AImage getImage(int id) {
        if (id < 0)
            return null;
        return new AWTImage(images.getImage(id), comp);
    }
    
    @Override
    public final  AImage getImage(int id, int width, int height) {
        return new AWTImage(images.getImage(id, width, height, comp), comp);
    }

    @Override
    public final  int newRotatedImage(int id, int degrees) {
        return images.newRotatedImage(id, degrees, comp);
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
    protected void drawImage(int imageKey, int x, int y, int w, int h) {
        images.drawImage(g, comp, imageKey, x, y, w, h);
    }

    @Override
    public void drawImage(int imageKey) {
        throw new AssertionError("Not Implemented");
    }

    @Override
    public final void setTransparencyFilter(float alpha) {
        Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Utils.clamp(alpha, 0, 1));
        setComposite(comp);
    }

    GColor tintSave = null;

    @Override
    public void setTintFilter(GColor inColor, GColor outColor) {
        tintSave = getColor();
        setColor(inColor);
        g.setXORMode(new Color(outColor.toRGB()));
    }

    public void setComposite(Composite comp) {
        throw new RuntimeException("Not implemented");
    }

    public void setAlphaComposite(float alpha, int mode) {
        setComposite(AlphaComposite.getInstance(mode, alpha));
    }

    @Override
    public void removeFilter() {
        if (tintSave != null) {
            setColor(tintSave);
            g.setPaintMode();
            tintSave = null;
        }
    }

    public void drawImage(AWTImage image, int x, int y) {
        g.drawImage(image.image, x, y, image.comp);
    }

    @Override
    public final  boolean isTextureEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public final  void clearScreen(GColor color) {
        Color c = g.getColor();
        g.setColor(AWTUtils.toColor(color));
        g.fillRect(0, 0, this.getViewportWidth(), this.getViewportHeight());
        g.setColor(c);
    }

    @Override
    public final void clearScreen() {
        if (comp != null) {
            g.setColor(comp.getBackground());
            g.fillRect(0, 0, getViewportWidth(), getViewportHeight());
        }
    }

    public final  void setFont(Font font) {
        g.setFont(font);
    }

	public void setColor(Color c) {
		g.setColor(c);
	}
	
	public void setColorARGB(int argb) {
		int a = (argb >> 24) & 0xff;
		int r = (argb >> 16) & 0xff;
		int g = (argb >>  8) & 0xff;
		int b = (argb >>  0) & 0xff;
		this.g.setColor(new Color(r, g, b, a));
	}
	
	public void setColorRGBA(int rgba) {
		int r = (rgba >> 24) & 0xff;
		int g = (rgba >> 16) & 0xff;
		int b = (rgba >>  8) & 0xff;
		int a = (rgba >>  0) & 0xff;
		this.g.setColor(new Color(r, g, b, a));
	}
	
	public void setColor(int r, int g, int b, int a) {
		this.g.setColor(new Color(r, g, b, a));
	}
	

	public Font getFont() {
		return g.getFont();
	}

	public void fillPolygon() {
		//r.fillPolygon(g);
        drawTriangleFan();
	}

    @Override
    public void setClipRect(float x, float y, float w, float h) {
        Vector2D v0 = transform(x, y);
        Vector2D v1 = transform(x+w, y+h);
        GRectangle r = new GRectangle(v0, v1);
        g.setClip(Math.round(r.x), Math.round(r.y), Math.round(r.w), Math.round(r.h));
    }

    @Override
    public GRectangle getClipRect() {
        Rectangle r = g.getClipBounds();
        if (r == null) {
            Vector2D v0 = screenToViewport(0, 0);
            Vector2D v1 = screenToViewport(getViewportWidth(), getViewportHeight());
            return new GRectangle(v0, v1);
        }
        Vector2D v0 = screenToViewport(r.x, r.y);
        Vector2D v1 = screenToViewport(r.x+r.width, r.y+r.height);
        return new GRectangle(v0, v1);
    }

    /**
     *
     * @param id
     * @param degrees
     * @return
     */
    public int createRotatedImage(int id, int degrees) {
        return images.newRotatedImage(id, degrees, comp);
    }

    @Override
    public void clearClip() {
        g.setClip(null);
    }

    @Override
    public boolean isCaptureAvailable() {
        return super.isCaptureAvailable();
    }

    @Override
    public void beginScreenCapture() {


        super.beginScreenCapture();
    }

    @Override
    public int captureScreen(int x, int y, int w, int h) {
        return super.captureScreen(x, y, w, h);
    }

    public int getMatrixStackSize() {
        return R.getStackSize();
    }

    @Override
    public void drawRoundedRect(float x, float y, float w, float h, float radius) {
        MutableVector2D tl = new MutableVector2D(x, y);
        MutableVector2D br = new MutableVector2D(x+w, y+h);
        float W = br.Xi() - tl.Xi();
        transform(tl);
        transform(br);
        int iRad = Math.round(radius * W / w);
        g.drawRoundRect(tl.Xi(), tl.Yi(), br.Xi()-tl.Xi(), br.Yi()-tl.Yi(), iRad, iRad);
    }

    @Override
    public void drawFilledRoundedRect(float x, float y, float w, float h, float radius) {
        MutableVector2D tl = new MutableVector2D(x, y);
        MutableVector2D br = new MutableVector2D(x+w, y+h);
        float W = br.Xi() - tl.Xi();
        transform(tl);
        transform(br);
        int iRad = Math.round(radius * W / w);
        g.fillRoundRect(tl.Xi(), tl.Yi(), br.Xi()-tl.Xi(), br.Yi()-tl.Yi(), iRad, iRad);
    }

    @Override
    public void drawWedge(float cx, float cy, float radius, float startDegrees, float sweepDegrees) {
        MutableVector2D tl = new MutableVector2D(cx-radius, cy-radius);
        MutableVector2D br = new MutableVector2D(cx+radius, cy+radius);
        transform(tl);
        transform(br);
        g.fillArc(tl.Xi(), tl.Yi(), br.Xi()-tl.Xi(), br.Yi()-tl.Yi(), Math.round(startDegrees), Math.round(startDegrees+sweepDegrees));
    }

    @Override
    public void drawArc(float cx, float cy, float radius, float startDegrees, float sweepDegrees) {
        MutableVector2D tl = new MutableVector2D(cx-radius, cy-radius);
        MutableVector2D br = new MutableVector2D(cx+radius, cy+radius);
        transform(tl);
        transform(br);
        g.drawArc(tl.Xi(), tl.Yi(), br.Xi()-tl.Xi(), br.Yi()-tl.Yi(), 360-Math.round(startDegrees), Math.round(sweepDegrees));
    }

    @Override
    public void drawCircle(float cx, float cy, float radius) {
        MutableVector2D tl = new MutableVector2D(cx-radius, cy-radius);
        MutableVector2D br = new MutableVector2D(cx+radius, cy+radius);
        transform(tl);
        transform(br);
        g.drawOval(tl.Xi(), tl.Yi(), br.Xi()-tl.Xi(), br.Yi()-tl.Yi());
    }

    @Override
    public void drawOval(float x, float y, float w, float h) {
        MutableVector2D tl = new MutableVector2D(x, y);
        MutableVector2D br = new MutableVector2D(x+w, y+h);
        transform(tl);
        transform(br);
        g.drawOval(tl.Xi(), tl.Yi(), br.Xi()-tl.Xi(), br.Yi()-tl.Yi());
    }

    @Override
    public void drawFilledOval(float x, float y, float w, float h) {
        MutableVector2D tl = new MutableVector2D(x, y);
        MutableVector2D br = new MutableVector2D(x+w, y+h);
        transform(tl);
        transform(br);
        g.fillOval(tl.Xi(), tl.Yi(), br.Xi()-tl.Xi(), br.Yi()-tl.Yi());
    }

    @Override
    public void drawDashedLine(float x0, float y0, float x1, float y1, float thickness, float dashLength) {
        throw new RuntimeException("Not implemented");
    }
}
