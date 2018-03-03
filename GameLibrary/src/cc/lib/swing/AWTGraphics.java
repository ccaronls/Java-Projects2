package cc.lib.swing;

import java.awt.*;
import java.awt.image.RGBImageFilter;
import java.util.Arrays;

import cc.lib.game.*;
import cc.lib.math.Vector2D;

public final class AWTGraphics extends APGraphics {

    private Graphics g;
    private final static ImageMgr images = new ImageMgr();
    private final Component comp;
    //private int textureId = -1;
    private float mLineThickness = 1;
    private float mPointSize = 1;

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
        r.setWindow(this);
        this.comp  = g.comp;
        initViewport(comp.getWidth(), comp.getHeight());
        ortho();
    }

    public final Graphics getGraphics() {
        return g;
    }

    public final void setGraphics(Graphics g) {
        g.setFont(this.g.getFont());
        this.g = g;
    }

    @Override
    public final void setColor(GColor color) {
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()));
    }

    @Override
    public final GColor getColor() {
        Color c = g.getColor();
        return new GColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    @Override
    public final  int getTextHeight() {
        return AWTUtils.getFontHeight(g);
    }

    @Override
    public final void setTextHeight(float height) {
        g.setFont(g.getFont().deriveFont(height));
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
        AWTUtils.drawJustifiedString(g, Math.round(x), Math.round(y)-3, hJust, Justify.TOP, text);
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
        if (newSize < 1) {
            error("Invalid parameter to setPointSize " + newSize + ". Clamped to 1");
            newSize = 1;
        }
        float oldSize = mPointSize;
        mPointSize = newSize;
        return oldSize;
    }

    private int getNumVerts() {
        return r.getNumVerts();
    }

    private float getX(int index) {
        return r.getX(index);
    }

    private float getY(int index) {
        return r.getY(index);
    }

    private Vector2D getVertex(int index) {
        return r.getVertex(index);
    }

    @Override
    public final  void drawPoints() {
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
    public final  void drawLines() {
        //r.drawLines(g, Math.round(mLineThickness));
        for (int i=0; i<getNumVerts(); i+=2) {
            if (i+1 < getNumVerts())
                AWTUtils.drawLinef(g, getX(i), getY(i), getX(i+1), getY(i+1), Math.round(mLineThickness));
        }
    }

    @Override
    public void drawLineStrip() {
        //r.drawLineStrip(g, Math.round(mLineThickness));
        for (int i=0; i<getNumVerts()-1; i++) {
            AWTUtils.drawLinef(g, getX(i), getY(i), getX(i+1), getY(i+1), Math.round(mLineThickness));
        }
    }

    @Override
    public final  void drawLineLoop() {
        //r.drawLineLoop(g, Math.round(mLineThickness));
        int thickness = Math.round(mLineThickness);
        if (thickness <= 1) {
            if (getNumVerts() > 1) {
                for (int i=0; i<getNumVerts()-1; i++) {
                    AWTUtils.drawLinef(g, getX(i), getY(i), getX(i+1), getY(i+1), 1);
                }
                int lastIndex = getNumVerts()-1;
                AWTUtils.drawLinef(g, getX(lastIndex), getY(lastIndex), getX(0), getY(0), 1);
            }
            return;
        }
        if (getNumVerts() > 1) {
            for (int i=1; i<getNumVerts(); i++) {
                float x0 = getX(i-1);
                float y0 = getY(i-1);
                float x1 = getX(i);
                float y1 = getY(i);
                AWTUtils.drawLinef(g, x0, y0, x1, y1, thickness);
            }
            if (getNumVerts()>2) {
                float x0 = getX(getNumVerts()-1);
                float y0 = getY(getNumVerts()-1);
                float x1 = getX(0);
                float y1 = getY(0);
                AWTUtils.drawLinef(g, x0, y0, x1, y1, thickness);
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
        images.addSearchPath(path);
    }

    @Override
    public final  int loadImage(String assetPath, GColor transparent) {
        return images.loadImage(assetPath, transparent == null ? null : AWTUtils.toColor(transparent));
    }
    
    @Override
    public final int[] loadImageCells(String assetPath, int w, int h, int numCellsX, int numCells, boolean bordered, GColor transparent) {
        return images.loadImageCells(assetPath, w, h, numCellsX, numCells, bordered, AWTUtils.toColor(transparent));
    }

    @Override
	public void deleteImage(int id) {
    	images.deleteImage(id);
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
    protected final void drawImage(int imageKey, int x, int y, int w, int h) {
        images.drawImage(g, comp, imageKey, x, y, w, h);
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

	public void drawLineLoop(int thickness) {
		float saveThickness = setLineWidth(thickness);
        drawLineLoop();
        setLineWidth(saveThickness);
	}

	public void fillPolygon() {
		//r.fillPolygon(g);
        drawTriangleFan();
	}

}
