package cc.lib.game;

import java.util.*;

import cc.lib.math.CMath;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;

public abstract class AGraphics implements Utils.VertexList, Renderable {
    
    public static boolean DEBUG_ENABLED = false;
    
    private int mViewportWidth, mViewportHeight;

    /**
     * 
     */
    protected AGraphics() {
        
    }
    
    /**
     * 
     * @param viewportWidth
     * @param viewportHeight
     */
    protected AGraphics(int viewportWidth, int viewportHeight) {
        this.mViewportWidth = viewportWidth;
        this.mViewportHeight = viewportHeight;
    }
    
    /**
     * 
     * @param color
     */
    public abstract void setColor(AColor color);
    
    /**
     * 
     * @param outlineColorARGB
     */
	public abstract void setColorARGB(int argb);
	
	public abstract void setColorRGBA(int rgba);
	
	public abstract void setColor(int r, int g, int b, int a);
	
    /**
     * 
     * @return
     */
    public abstract AColor getColor();
    
    /**
     * 
     */
    public final int getViewportWidth() {
        return mViewportWidth;
    }
    
    /**
     * 
     */
    public final int getViewportHeight() {
        return mViewportHeight;
    }

    /**
     * Convenience method to draw with LEFT/TOP Justification
     * @param g
     * @param x
     * @param y
     * @param text
     */
    public final float drawJustifiedString(float x, float y, String text) {
        return drawJustifiedString(x, y, Justify.LEFT, Justify.TOP, text);
    }

    /**
     * Convenience to draw with TOP Justification
     * @param g
     * @param x
     * @param y
     * @param hJust
     * @param text
     */
    public final float drawJustifiedString(float x, float y, Justify hJust, String text) {
        return drawJustifiedString(x, y, hJust, Justify.TOP, text);
    }
    
    /**
     * Render test with LEFT/TOP Justification
     * @param text
     * @param x
     * @param y
     */
    public final float drawString(String text, float x, float y) {
        return drawJustifiedString(x, y, Justify.LEFT, Justify.TOP, text);
    }
    
    /**
     * 
     * @return
     */
    public abstract int getTextHeight();
    
    /**
     * 
     * @param string
     * @return
     */
    public abstract float getTextWidth(String string);

    /**
     * 
     * @param x
     * @param y
     * @param result
     */
    public abstract void transform(float x, float y, float [] result);
    
    /**
     * 
     * @param v
     */
    public final void transform(MutableVector2D v) {
    	v.set(transform((IVector2D)v));
    }
    
    /**
     * 
     * @param v
     * @return
     */
	public final MutableVector2D transform(IVector2D v) {
		float [] result = new float[2];
		transform(v.getX(), v.getY(), result);
		return new MutableVector2D(result[0], result[1]);
	}

    
    /**
     * Convert screen coordinates (like from a mouse) to view port coordinates
     * @param screenX
     * @param screenY
     * @return
     */
    public abstract Vector2D screenToViewport(int screenX, int screenY);
    
    /**
     * Draw a justified block text.  '\n' is a delimiter for seperate lines
     * @param g
     * @param x
     * @param y
     * @param hJust
     * @param vJust
     * @param text
     * @return the total height of the text. 
     */
    public float drawJustifiedString(float x, float y, Justify hJust, Justify vJust, String text) {
        if (text==null || text.length() == 0)
            return 0;
        float [] r= new float[2];
        transform(x, y, r);
        x = r[0];
        y = r[1];
        String [] lines = text.split("\n");
        final float textHeight = (float)getTextHeight();
        switch (vJust) {
        case TOP: 
            break;
        case CENTER: 
            y -= (lines.length * (textHeight)) / 2; 
            break;
        case BOTTOM: 
            y -= lines.length * textHeight; 
            break;
        default:
            throw new RuntimeException("Unhandled case: " + vJust);
        }
        for (int i=0; i<lines.length; i++) {
            //priv_drawJustifiedString(sGl, x, y, hJust, lines[i]);
            drawStringLine(x, y, hJust, lines[i]);
            y += textHeight;
        }
        return textHeight * lines.length;
    }
    
    /**
     * 
     * @param g
     * @param str
     * @param maxWidth
     * @return
     */
    public final String [] generateWrappedLines(String str, float maxWidth) {
        String text = str.trim();
        List<String> lines = new ArrayList<String>(32);
        while (text.length() > 0 && lines.size() < 256) {
            int endl = text.indexOf('\n');
            if (endl >= 0) {
                String t = text.substring(0, endl).trim();
                float width = getTextWidth(t);
                if (width <= maxWidth) {
                    lines.add(t);
                    text = text.substring(endl+1);
                    continue;
                }
            }
            
            // cant find an endl, see if text fits
            float width = getTextWidth(text);
            if (width <= maxWidth) {
                lines.add(text);
                break;
            }
            
            // try to find a space to break on
            int spc = -1;
            String t = new String(text);
            while (width > maxWidth) {
                spc = t.lastIndexOf(' ');
                if (spc >= 0) {
                    t = t.substring(0, spc).trim();
                    width = getTextWidth(t);
                } else {
                    spc = -1;
                    break;
                }
            }
            
            if (spc >= 0) {
                // found a space!
                lines.add(t);
                text = text.substring(spc+1).trim();
                continue;
            }
            
            // made it here means we have to wrap on a whole word!
            t = split(text, 0, text.length(), maxWidth);
            lines.add(t);
            try {
            	text = text.substring(t.length()).trim();
            } catch (Exception e) {
            	e.printStackTrace();
            	break;
            }
        }

        return lines.toArray(new String[lines.size()]);
    }    
    
    /**
     * Use binary search to find substring of s that has max chars up to maxWidth pixels wide.
     * @param g
     * @param s
     * @param start
     * @param end
     * @param maxWidth
     * @return
     */
    public final String split(String s, int start, int end, float maxWidth) {
    	return splitR(s, start, end, maxWidth, 0);
    }
    
    private final String splitR(String s, int start, int end, float maxWidth, int i) {
    	if (i > 20) {
    		System.out.print("");
    	}
        if (end - start <= 1)
            return "";
        int mid = (start+end)/2;
        String t = s.substring(start, mid);
        float wid = getTextWidth(t);
        if (wid < maxWidth)
            return t + splitR(s, mid, end, maxWidth - wid, ++i);
        else
            return splitR(s, start, mid, maxWidth, ++i);
    }    
    
    /**
     * 
     * @param x
     * @param y
     * @param maxWidth
     * @param text
     * @return
     */
    public final float drawWrapString(float x, float y, int maxWidth, String text) {
        String [] lines = generateWrappedLines(text, maxWidth);
        for (int i=0; i<lines.length; i++) {
            drawStringLine(x, y, Justify.LEFT, lines[i]);
            //g.drawString(lines[i], x, y);
            y += getTextHeight();
        }
        return y;
    }    
    
    /**
     * 
     * @param x
     * @param y
     * @param maxWidth
     * @param hJust
     * @param vJust
     * @param text
     * @return
     */
    public float drawWrapString(float x, float y, float maxWidth, Justify hJust, Justify vJust, String text) {
        String [] lines = generateWrappedLines(text, maxWidth);
        switch (vJust) {
            case TOP: break;
            case BOTTOM: y -= lines.length * getTextHeight(); break;
            case CENTER: y -= lines.length * getTextHeight() / 2; break;
            default: 
                throw new RuntimeException("Unhandled case: " + vJust);
        }
        for (int i=0; i<lines.length; i++) {
            drawStringLine(x, y, hJust, lines[i]);
            //g.drawString(lines[i], x, y);
            y += getTextHeight();
        }
        return y;
    }
    
    /**
     * Draw a single line of justified text and return the width of the text
     * @param x
     * @param y
     * @param hJust
     * @param text
     * @return
     */
    public abstract float drawStringLine(float x, float y, Justify hJust, String text);
    
    /**
     * 
     * @param viewportWidth
     * @param viewportHeight
     */
    public void initViewport(int viewportWidth, int viewportHeight) {
        mViewportWidth = viewportWidth;
        mViewportHeight = viewportHeight;
    }
    
    /**
     * 
     * @param newWidth
     * @return
     */
    public abstract float setLineWidth(float newWidth);

    /**
     * 
     * @param newSize
     * @return
     */
    public abstract float setPointSize(float newSize);
    
    /**
     * 
     * @param x
     * @param y
     */
    public abstract void vertex(float x, float y);
    
    /**
     * Convenience
     * 
     * @param v
     */
    public final void vertex(IVector2D v) {
        vertex(v.getX(), v.getY());
    }
    
    /**
     * 
     * @param l
     */
    public final void vertexList(Collection<IVector2D> l) {
        for (IVector2D t : l)
            vertex(t);
    }
    
    /**
     * 
     * @param a
     */
    public final void vertexArray(IVector2D [] a) {
        for (IVector2D t : a)
            vertex(t);
    }
    
    /**
     * 
     */
    public abstract void drawPoints();
    
    /**
     * 	
     * @param pointSize
     */
    public final void drawPoints(float pointSize) {
    	float old = setPointSize(pointSize);
    	drawPoints();
    	setPointSize(old);
    }

    /**
     * 
     */
    public abstract void drawLines();
    
    /**
     * 
     * @param thickness
     */
    public final void drawLines(float thickness) {
    	float old = setLineWidth(thickness);
    	drawLines();
    	setLineWidth(old);
    }
    
    /**
     * 
     */
    public abstract void drawLineStrip();

    /**
     * 
     * @param thickness
     */
    public final void drawLineStrip(float thickness) {
    	float old = setLineWidth(thickness);
    	drawLineStrip();
    	setLineWidth(old);
    }

    /**
     * 
     */
    public abstract void drawLineLoop();
    
    /**
     * 
     * @param thickness
     */
    public final void drawLineLoop(float thickness) {
    	float old = setLineWidth(thickness);
    	drawLineLoop();
    	setLineWidth(old);
    }

    /**
     * 
     */
    public abstract void drawTriangles();

    /**
     * 
     */
    public abstract void drawTriangleFan();
    
    /**
     * 
     */
    public abstract void drawTriangleStrip();
    
    /**
     * 
     */
    public abstract void drawQuadStrip();
    
    /**
     * 
     * @param assetPath
     * @param transparent
     * @return
     */
    public abstract int loadImage(String assetPath, AColor transparent);

    /**
     * 
     * @param assetPath
     * @param w
     * @param h
     * @param rows
     * @param cols
     * @param bordeered
     * @param transparent
     * @return
     */
    public abstract int [] loadImageCells(String assetPath, int w, int h, int numCellsX, int numCellsY, boolean bordeered, AColor transparent);
    
    /**
     * 
     * @param id
     * @return
     */
    public abstract AImage getImage(int id);

    /**
     * 
     * @param id
     * @param width
     * @param height
     * @return
     */
    public abstract AImage getImage(int id, int width, int height);
    
    /**
     * 
     * @param assetPath
     * @return
     */
    public final int loadImage(String assetPath) {
        return loadImage(assetPath, null);
    }
    
    /**
     * 
     * @param id
     */
    public abstract void deleteImage(int id);

    /**
     * 
     * @param id
     * @param x
     * @param y
     * @param w
     * @param h
     * @return
     */
    public abstract int newSubImage(int id, int x, int y, int w, int h);
    
    /**
     * 
     * @param id
     * @param degrees
     * @return
     */
    public abstract int newRotatedImage(int id, int degrees);
    
    /**
     * 
     * @param id
     * @param filter
     * @return
     */
    public abstract int newTransformedImage(int id, IImageFilter filter);
    
    /**
     * 
     * @param id
     */
    public abstract void enableTexture(int id);

    /**
     * 
     */
    public abstract void disableTexture();

    /**
     * 
     * @param s
     * @param t
     */
    public abstract void texCoord(float s, float t);
    
    /**
     * 
     */
    public abstract void pushMatrix();
    
    /**
     * 
     */
    public abstract void popMatrix();

    /**
     * 
     */
    public abstract void setIdentity();
    
    /**
     * 
     * @param x
     * @param y
     * @param z
     */
    public abstract void translate(float x, float y);

    /**
     * Convenience
     * 
     * @param v
     */
    public final void translate(IVector2D v) {
        translate(v.getX(), v.getY());
    }

    /**
     * 
     * @param degrees
     */
    public abstract void rotate(float degrees);

    /**
     * Call before calls to vertex and draw methods.
     */
    public void begin() {}
    
    /**
     * Call after render methods to reset the vertex list
     */
    public void end() {}
    
    /**
     * 
     * @param x
     * @param y
     * @param z
     */
    public abstract void scale(float x, float y);
    
    /**
     * Convenience to scale x,y by single scalar
     * @param s
     */
    public final void scale(float s) {
    	scale(s, s);
    }
    
    /**
     * 
     * @param g
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param thickness
     */
    public final void drawLine(float x0, float y0, float x1, float y1, float thickness) {
        begin();
        float oldWidth = setLineWidth(thickness);
        vertex(x0, y0);
        vertex(x1, y1);
        drawLines();
        setLineWidth(oldWidth);
    }

    /**
     * Convenience.  Thickness defaults to 1
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     */
    public final void drawLine(float x0, float y0, float x1, float y1) {
        drawLine(x0, y0, x1, y1, 1);
    }
    
    /**
     * 
     * @param g
     * @param points
     * @param thickness
     */
    public final void drawLineStrip(int [] x_pts, int [] y_pts, int thickness) {
        assert(x_pts.length == y_pts.length);
        float oldWidth = setLineWidth(thickness);
        begin();
        for (int i=0; i<x_pts.length-1; i++) {
            vertex(x_pts[i], y_pts[i]);
            vertex(x_pts[i+1], y_pts[i+1]);
        }
        drawLines();
        setLineWidth(oldWidth);
    }
    
    /**
     * draw an empty rectangle 
     */
    public final void drawRect(float x, float y, float w, float h, float thickness) {
        float oldWidth = setLineWidth(thickness);
        begin();
        vertex(x, y);
        vertex(x+w, y);
        vertex(x+w, y+h);
        vertex(x, y+h);
        drawLineLoop();
        setLineWidth(oldWidth);
    }
    
    /**
     * 
     * @param x
     * @param y
     * @param w
     * @param h
     * @param thickness
     * @param radius
     */
    public final void drawRoundedRect(float x, float y, float w, float h, float thickness, float radius) {
    	drawArc(x+radius, y+radius, radius, thickness, 180f, 90f, 6);
    	drawLine(x+radius, y, x+w-radius, y, thickness);
    	drawArc(x+w-radius,y+radius,radius,thickness,270f,90f, 6);
    	drawLine(x+w,y+radius,x+w,y+h-radius,thickness);
    	drawArc(x+w-radius,y+h-radius,radius,thickness,0f,90f, 6);
    	drawLine(x+radius,y+h,x+w-radius,y+h,thickness);
    	drawArc(x+radius,y+h-radius,radius,thickness,90f,90f, 6);
    	drawLine(x,y+radius,x,y+h-radius,thickness);
    }

    /**
     * 
     * @param x
     * @param y
     * @param w
     * @param radius
     */
    public final void drawFilledRoundedRect(float x, float y, float w, float h, float radius) {
    	float r2 = radius*2;
    	drawFilledWedge(x+radius, y+radius, radius, 180f, 90f, 6);
    	drawFilledRectf(x+radius,y,w-r2,radius);
    	drawFilledWedge(x+w-radius,y+radius,radius,270f,90f, 6);
    	drawFilledRectf(x,y+radius,w,h-r2);
    	drawFilledWedge(x+w-radius,y+h-radius,radius,0,90, 6);
    	drawFilledRectf(x+radius,y+h-radius,w-r2,radius);
    	drawFilledWedge(x+radius,y+h-radius,radius,90f,90f, 6);
    }
    
    /**
     * Convenience.  Thickness defaults to 1.
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public final void drawRect(float x, float y, float w, float h) {
        drawRect(x, y, w, h, 1);
    }
    
    /**
     * 
     * @return
     */
    public abstract boolean isTextureEnabled();
    
    /**
     * Draw a quad
     */
    public final void drawQuad(float x0, float y0, float x1, float y1) {
        begin();
        if (isTextureEnabled()) {
            texCoord(0,0);
            texCoord(1,0);
            texCoord(0,1);
            texCoord(1,1);
        } 
        vertex(x0, y0);
        vertex(x1, y0);
        vertex(x0, y1);
        vertex(x1, y1);
        drawTriangleStrip();
    }
    
    /**
     * alias for drawQuad.  Here for compatibility with AWT Graphics
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public final void drawFilledRect(int x, int y, int w, int h) {
        drawQuad(x, y, x+w, y+h);
    }

    public final void drawFilledRectf(float x, float y, float w, float h) {
        drawQuad(x, y, x+w, y+h);
    }

    // return whether points added for quads or lines (true == quads)
    private final boolean circlePoints(float innerRadius, float outerRadius, float startAngle, float sweep, int sections) {
    	float radius = (innerRadius + outerRadius)/2;
        //float sections = radius * sweep/180;
        //if (sections > 32)
        //    sections = 32;
        //else if (sections < 8)
        //    sections = 8;
        float step = sweep / sections;
        float angle = startAngle;
        float endAngle = startAngle + sweep;
        
        if (outerRadius-innerRadius>1) {
	        for (int i=0; i<sections && angle < endAngle; i++) {
	            float x = CMath.cosine(angle);
	            float y = CMath.sine(angle);
	            vertex(x*outerRadius, y*outerRadius);
	            vertex(x*innerRadius, y*innerRadius);
	            angle += step;
	        }
            float x = CMath.cosine(endAngle);
            float y = CMath.sine(endAngle);
            vertex(x*outerRadius, y*outerRadius);
            vertex(x*innerRadius, y*outerRadius);
        	return true;
        } else {
	        for (int i=0; i<sections; i++) {
	            float x = CMath.cosine(angle) * radius;
	            float y = CMath.sine(angle) * radius;
	            vertex(x, y);
	            angle += step;
	        }
	        vertex(CMath.cosine(endAngle) * radius, CMath.sine(endAngle) * radius);
	        return false;
        }
    }
    
    /**
     * 
     * @param x
     * @param y
     * @param radius
     */
    public final void drawDisk(float x, float y, float radius) {
    	int sections = Utils.clamp(Math.round(radius * 4), 8, 32);
        drawDisk(x, y, radius, sections);
    }
    
    /**
     * 
     * @param x
     * @param y
     * @param radius
     * @param int sections
     */
    public final void drawDisk(float x, float y, float radius, int sections) {
        pushMatrix();
        translate(x, y);
        begin();
        vertex(0, 0);
        circlePoints(radius, radius, 0, 360, sections);
        drawTriangleFan();
        popMatrix();
    }

    /**
     * 
     * @param cx
     * @param cy
     * @param radius
     * @param startDegrees
     * @param sweepDegrees
     */
    public final void drawFilledWedge(float cx, float cy, float radius, float startDegrees, float sweepDegrees, int sections) {
        pushMatrix();
        translate(cx, cy);
        begin();
        vertex(0, 0);
        circlePoints(radius, radius, startDegrees, sweepDegrees, sections);
        drawTriangleFan();
        popMatrix();
    }
    
    /**
     * 
     * @param x
     * @param y
     * @param radius
     */
    public final void drawCircle(float x, float y, float radius, float thickness, int sections) {
        float saveThickness = setLineWidth(thickness);
        pushMatrix();
        translate(x, y);
        begin();
        if (circlePoints(radius-thickness/2, radius+thickness/2, 0, 360, sections)) {
        	drawQuadStrip();
        } else {
        	drawLineLoop();
        }
        popMatrix();
        setLineWidth(saveThickness);
    }

    /**
     * 
     * @param x
     * @param y
     * @param radius
     * @param thickness
     * @param startDegrees
     * @param sweepDegrees
     */
    public final void drawArc(float x, float y, float radius, float thickness, float startDegrees, float sweepDegrees, int sections) {
        float saveThickness = setLineWidth(thickness);
        pushMatrix();
        translate(x, y);
        begin();
        if (circlePoints(radius-thickness/2, radius+thickness/2, startDegrees, sweepDegrees, sections)) {
        	drawQuadStrip();
        } else {
        	drawLineStrip();
        }
        popMatrix();
        setLineWidth(saveThickness);
    }

    /**
     * 
     * @param x
     * @param y
     * @param radius
     */
    public final void drawCircle(float x, float y, float radius) {
    	int sections = Math.round(radius * 4);
    	sections = Utils.clamp(sections,  8,  32);
    	drawCircle(x, y, radius, sections);
    }
    /**
     * Convenience.  Thickness defaults to 1
     * @param x
     * @param y
     * @param radius
     * @param sections
     */
    public final void drawCircle(float x, float y, float radius, int sections) {
        drawCircle(x, y, radius, 1, sections);
    }

    /**
     * 
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public final void drawOval(float x, float y, float w, float h, float thickness, int sections) {
        float saveThickness = setLineWidth(thickness);
        pushMatrix();
        float X = x + w/2;
        float Y = y + h/2;
        float radius = w;
        translate(X, Y);
        if (w < h) {
            scale((h-w)/w, 1);
            radius = h;
        } else if (w > h) {
            scale(1, (w-h)/h);
            radius = w;
        }
        begin();
        if (circlePoints(radius-thickness/2, radius+thickness/2, 0, 360, sections)) {
        	drawQuadStrip();
        } else {
        	drawLineLoop();
        }
        popMatrix();
        setLineWidth(saveThickness);
    }

    /**
     * 
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public final void drawOval(float x, float y, float w, float h) {
    	float radius = Math.max(w/2, h/2);
        int sections = Math.round(radius * 2);
        if (sections > 32)
            sections = 32;
        else if (sections < 8)
            sections = 8;
    	drawOval(x, y, w, h, sections);
    }
    
    /**
     * Convenience.  thickness default to 1
     * @param x
     * @param y
     * @param w
     * @param h
     * @param sections
     */
    public final void drawOval(float x, float y, float w, float h, int sections) {
        drawOval(x, y, w, h, 1, sections);
    }
    
    /**
     * 
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public final void drawFilledOval(float x, float y, float w, float h) {
        pushMatrix();
        float radius = Math.max(w/2, h/2);
        float sections = radius * 2;
        if (sections > 32)
            sections = 32;
        else if (sections < 8)
            sections = 8;
        float step = 360 / sections;
        float angle = 0;
        float endAngle = 360;
        begin();
        translate(x+w/2,y+h/2);
        vertex(0, 0);
        for (int i=0; i<sections; i++) {
            float x0 = CMath.cosine(angle) * w/2;
            float y0 = CMath.sine(angle) * h/2;
            vertex(x0, y0);
            angle += step;
        }
        vertex(CMath.cosine(endAngle) * w/2, CMath.sine(endAngle) * h/2);
        drawTriangleFan();
        popMatrix();
    }

    /**
     * 
     * @param x
     * @param y
     * @param r
     */
	public final void drawFilledCircle(int x, int y, int r) {
		drawFilledOval(x-r, y-r, r*2, r*2);
	}

    /**
     * 
     * @param brain_pts_x
     * @param brain_pts_y
     * @param length
     */
    public final void drawFilledPolygon(int[] pts_x, int[] pts_y, int length) {
        if (length < 3)
            return;
        begin();
        for (int i=0; i<length; i++) {
            vertex(pts_x[i], pts_y[i]);
        }
        drawTriangleFan();
    }
    
    /**
     * 
     * @param ctrl_x0
     * @param ctrl_y0
     * @param ctrl_x1
     * @param ctrl_y1
     * @param ctrl_x2
     * @param ctrl_y2
     * @param ctrl_x3
     * @param ctrl_y3
     * @param iterations
     */
    public final void drawBeizerCurve(float ctrl_x0, float ctrl_y0, float ctrl_x1, float ctrl_y1, float ctrl_x2, float ctrl_y2, float ctrl_x3, float ctrl_y3, int iterations) {
        float step = 1.0f/iterations;
        for (float t=0; t<1.0f; t+=step) {
            float fW = 1 - t; 
            float fA = fW * fW * fW;
            float fB = 3 * t * fW * fW; 
            float fC = 3 * t * t * fW;
            float fD = t * t * t;
            float fX = fA * ctrl_x0 + fB * ctrl_x1 + fC * ctrl_x2 + fD * ctrl_x3; 
            float fY = fA * ctrl_y0 + fB * ctrl_y1 + fC * ctrl_y2 + fD * ctrl_y3;
            vertex(fX, fY);
        }
        vertex(ctrl_x3, ctrl_y3);
        drawLineStrip();
    }
    
    /**
     * 
     * @param controlPts
     * @param iterations
     */
    public final void drawBeizerCurve(IVector2D [] controlPts, int iterations) {
        if (controlPts.length < 4) {
            System.err.println("Must be four control points");
            return;
        }
        drawBeizerCurve(controlPts[0].getX(), controlPts[0].getY(), 
                        controlPts[1].getX(), controlPts[1].getY(), 
                        controlPts[2].getX(), controlPts[2].getY(), 
                        controlPts[3].getX(), controlPts[3].getY(), iterations);
    }
    
    /**
     * 
     * @param imageKey
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public void drawImage(int imageKey, int x, int y, int w, int h) {
        enableTexture(imageKey);
        drawFilledRect(x, y, w, h);
        disableTexture();
    }
    
    /**
     * 
     * @param color
     */
    public abstract void clearScreen(AColor color);
    
    /**
     * 
     * @param left
     * @param right
     * @param top
     * @param bottom
     */
    public abstract void ortho(float left, float right, float top, float bottom);
    
    /**
     * Convenience to set screen to standard ortho mode where 0,0 is at upper left
     * hand corner and right bottom corner is the viewport width/height
     */
    public final void ortho() {
        ortho(0, mViewportWidth, 0, mViewportHeight);
    }

    /**
     * Used internally to report errors.  default prints to stderr.
     * @param message
     */
    protected void error(String message) {
        System.err.println(message);
    }

    public final AColor BLACK       = makeColor(0,0,0,1);
    public final AColor WHITE       = makeColor(1,1,1,1);
    public final AColor RED         = makeColor(1,0,0,1);
    public final AColor BLUE        = makeColor(0,0,1,1);
    public final AColor GREEN       = makeColor(0,1,0,1);
    public final AColor CYAN        = makeColor(0,1,1,1);
    public final AColor MAGENTA     = makeColor(1,0,1,1);
    public final AColor YELLOW      = makeColor(1,1,0,1);
    public final AColor ORANGE      = makeColor(1,0.4f,0,1);
    public final AColor GRAY      	= makeColor(0.6f, 0.6f, 0.6f, 1);
    public final AColor LIGHT_GRAY 	= makeColor(0.8f, 0.8f, 0.8f, 1);
    public final AColor DARK_GRAY 	= makeColor(0.4f, 0.4f, 0.4f, 1);
    public final AColor TRANSPARENT = makeColor(0,0,0,0);
    
    /**
     * 
     * @param r
     * @param g
     * @param b
     * @param a
     * @return
     */
    public abstract AColor makeColor(float r, float g, float b, float a);

    /**
     * 
     * @param r
     * @param g
     * @param b
     * @return
     */
    public AColor makeColori(int r, int g, int b) {
        return makeColor((float)r/ 255, (float)g/255, (float)b/255, 1);
    }

    /**
     * 
     * @param r
     * @param g
     * @param b
     * @param a
     * @return
     */
    public AColor makeColori(int r, int g, int b, int a) {
        return makeColor((float)r/ 255, (float)g/255, (float)b/255, (float)a/255);
    }
    
    public AColor makeColorRGBA(int rgba) {
    	int r = (rgba >>> 24) & 0xff;
        int g = (rgba >> 16) & 0xff;
        int b = (rgba >> 8) & 0xff;
    	int a = (rgba >> 0) & 0xff;
    	return makeColor(r, g, b, a);
    }

    public AColor makeColorARGB(int argb) {
    	int a = (argb >>> 24) & 0xff;
    	int r = (argb >> 16) & 0xff;
        int g = (argb >> 8) & 0xff;
        int b = (argb >> 0) & 0xff;
    	return makeColor(r, g, b, a);
    }

    /**
     * Reset min/max bounding rect
     */
	public abstract void clearMinMax();
	
	/**
	 * Get the min bounding rect of all verts since last call to clearMinMax
	 * @return
	 */
	public abstract Vector2D getMinBoundingRect();
	
	/**
	 * Get the max bounding rect of all verts since last call to clearMinMax
	 * @return
	 */
	public abstract Vector2D getMaxBoundingRect();

}
