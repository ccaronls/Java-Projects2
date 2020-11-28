package cc.lib.game;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.lib.math.CMath;
import cc.lib.math.Matrix3x3;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.utils.GException;

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
    public abstract void setColor(GColor color);
    
    /**
     * 
     * @param argb
     */
	public abstract void setColorARGB(int argb);
	
	public abstract void setColorRGBA(int rgba);
	
	public abstract void setColor(int r, int g, int b, int a);
	
    /**
     * 
     * @return
     */
    public abstract GColor getColor();
    
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
     * @param x
     * @param y
     * @param text
     * @return the enclosing rect of the text
     */
    public final GDimension drawJustifiedString(float x, float y, String text) {
        return drawJustifiedString(x, y, Justify.LEFT, Justify.TOP, text);
    }

    /**
     *
     * @param pos
     * @param text
     * @return
     */
    public final GDimension drawJustifiedString(IVector2D pos, String text) {
        return drawJustifiedString(pos.getX(), pos.getY(), text);
    }

    /**
     * Convenience to draw with TOP Justification
     * @param x
     * @param y
     * @param hJust
     * @param text
     * @return the enclosing rect of the text
     */
    public final GDimension drawJustifiedString(float x, float y, Justify hJust, String text) {
        return drawJustifiedString(x, y, hJust, Justify.TOP, text);
    }

    /**
     *
     * @param pos
     * @param hJust
     * @param text
     * @return
     */
    public final GDimension drawJustifiedString(IVector2D pos, Justify hJust, String text) {
        return drawJustifiedString(pos.getX(), pos.getY(), hJust, text);
    }

    /**
     * Render test with LEFT/TOP Justification
     * @param text
     * @param x
     * @param y
     * @return the enclosing rect of the text
     */
    public final GDimension drawString(String text, float x, float y) {
        return drawJustifiedString(x, y, Justify.LEFT, Justify.TOP, text);
    }

    /**
     *
     * @param text
     * @param x
     * @param y
     * @return
     */
    public final GDimension drawAnnotatedString(String text, float x, float y) {
        return drawAnnotatedString(text, x,y, Justify.LEFT);
    }

   /**
    * Annotated string can have annotations in the string (ala html) to control color, underline etc.
    *
    * annotated color pattern:
    * [(a,)?r,g,b]
    *
    * @param text
    * @param x
    * @param y
    * @return the enclosing rect of the text
    */
    public final GDimension drawAnnotatedString(String text, float x, float y, Justify hJust) {

        Matcher m = ANNOTATION_PATTERN.matcher(text);

        if (m.find()) {
            MutableVector2D mv = new MutableVector2D(x, y);
            transform(mv);
            x = mv.getX();
            y = mv.getY();
            GColor saveColor = getColor();
            float width = 0;
            int start = 0;
            do {

                GColor nextColor = GColor.fromString(m.group());
                float w = drawStringLine(x, y, hJust, text.substring(start, m.start()));
                width += w;
                x += w;
                start = m.end();
                setColor(nextColor);

            } while (m.find());
            width += drawStringLine(x, y, hJust, text.substring(start));
            setColor(saveColor);
            return new GDimension(width, getTextHeight());
        }

        return drawString(text, x, y);
    }

    final static Pattern ANNOTATION_PATTERN = Pattern.compile("\\[([0-9]{1,3},)?[0-9]{1,3},[0-9]{1,3},[0-9]{1,3}\\]");

    /**
     * 
     * @return
     */
    public abstract int getTextHeight();

    /**
     *
     * @param height
     */
    public abstract void  setTextHeight(float height);

    /**
     * 
     * @param string
     * @return
     */
    public abstract float getTextWidth(String string);

    /**
     *
     * @param string
     * @return
     */
    public GDimension getTextDimension(String string, float maxWidth) {
        String [] lines = generateWrappedLines(string, maxWidth);
        float width = 0;
        int height = getTextHeight() * lines.length;
        for (String s : lines) {
            width = Math.max(width, getTextWidth(s));
        }
        return new GDimension((int)(width+0.9f), height);
    }

    /**
     * Return true if screen capture functionality is available. Default false
     * @return
     */
    public boolean isCaptureAvailable() {
        return false;
    }

    /**
     * Start a screen capture operation. the next call to captureScreen will end the operation
     */
    public void beginScreenCapture() {
        throw new GException("Not implemented");
    }

    /**
     * Capture a portion of the viewport
     *
     * @param x
     * @param y
     * @param w
     * @param h
     *
     * @return image id of the captured image
     */
    public int captureScreen(int x, int y, int w, int h) {
        throw new GException("Not implemented");
    }

    /**
     *
     */
    public enum TextStyle {
        NORMAL, BOLD, ITALIC, MONOSPACE, UNDERLINE
    }

    /**
     * Apply some combination of styles to the font
     *
     * @param styles
     */
    public abstract void setTextStyles(TextStyle ... styles);

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
     * @param x
     * @param y
     * @return
     */
    public final MutableVector2D transform(float x, float y) {
        MutableVector2D mv = new MutableVector2D(x, y);
        transform(mv);
        return mv;
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
     * @param x
     * @param y
     * @param hJust
     * @param vJust
     * @param text
     * @return the total height of the text. 
     */
    public GDimension drawJustifiedString(float x, float y, Justify hJust, Justify vJust, String text) {
        if (text==null || text.length() == 0)
            return GDimension.EMPTY;
        MutableVector2D mv = transform(x, y);
        String [] lines = text.split("\n");
        final float textHeight = (float)getTextHeight();
        switch (vJust) {
        case TOP: 
            break;
        case CENTER: 
            mv.subEq(0, 0.5f * (lines.length * (textHeight)));
            y -= 0.5f * (lines.length * (textHeight));
            break;
        case BOTTOM: 
            mv.subEq(0, lines.length * textHeight);
            y -= (lines.length * (textHeight));
            break;
        default:
            throw new GException("Unhandled case: " + vJust);
        }
        float maxWidth = 0;
        for (int i=0; i<lines.length; i++) {
            maxWidth = Math.max(drawStringLine(mv.X(), mv.Y(), hJust, lines[i]), maxWidth);
            mv.addEq(0, textHeight);
        }
        float maxHeight = textHeight * lines.length;
        begin();
        vertex(x, y);
        switch (hJust) {
            case RIGHT:
                vertex(x-maxWidth, y+maxHeight); break;
            case CENTER:
                vertex(x-maxWidth/2, y+maxHeight); break;
            case LEFT:
                vertex(x+maxWidth/2, y+maxHeight); break;
        }
        return new GDimension(maxWidth, maxHeight);
    }

    public GRectangle drawJustifiedStringR(float x, float y, Justify hJust, Justify vJust, String text) {
        if (text==null || text.length() == 0)
            return new GRectangle();
        MutableVector2D mv = transform(x, y);
        String [] lines = text.split("\n");
        final float textHeight = (float)getTextHeight();
        switch (vJust) {
            case TOP:
                break;
            case CENTER:
                mv.subEq(0, 0.5f * (lines.length * (textHeight)));
                y -= 0.5f * (lines.length * (textHeight));
                break;
            case BOTTOM:
                mv.subEq(0, lines.length * textHeight);
                y -= (lines.length * (textHeight));
                break;
            default:
                throw new GException("Unhandled case: " + vJust);
        }
        float top = mv.getY();
        float maxWidth = 0;
        for (int i=0; i<lines.length; i++) {
            maxWidth = Math.max(drawStringLine(mv.X(), mv.Y(), hJust, lines[i]), maxWidth);
            mv.addEq(0, textHeight);
        }
        float maxHeight = textHeight * lines.length;
        begin();
        vertex(x, y);
        float left = 0;
        switch (hJust) {
            case RIGHT:
                left = mv.getX()-maxWidth;
                vertex(x-maxWidth, y+maxHeight); break;
            case CENTER:
                left = mv.getX()-maxWidth/2;
                vertex(x-maxWidth/2, y+maxHeight); break;
            case LEFT:
                left = mv.getX();
                vertex(x+maxWidth/2, y+maxHeight); break;
        }
        return new GRectangle(left, top, maxWidth, maxHeight);
    }

    /**
     *
     * @param pos
     * @param hJust
     * @param vJust
     * @param text
     * @return
     */
    public final GDimension drawJustifiedString(IVector2D pos, Justify hJust, Justify vJust, String text) {
        return drawJustifiedString(pos.getX(), pos.getY(), hJust, vJust, text);
    }

    /**
     *
     * @param x
     * @param y
     * @param hJust
     * @param vJust
     * @param text
     * @param bkColor
     * @param border
     * @return
     */
    public GDimension drawJustifiedStringOnBackground(float x, float y, Justify hJust, Justify vJust, String text, GColor bkColor, float border, float cornerRadius) {
        GRectangle r = drawJustifiedStringR(x, y, hJust, vJust, text);
        pushMatrix();
        setIdentity();
        r.grow(border);
        GColor saveColor = getColor();
        setColor(bkColor);
        if (cornerRadius > 0)
            r.drawRounded(this, cornerRadius);
        else
            r.drawFilled(this);
        setColor(saveColor);
        popMatrix();
        drawJustifiedString(x, y, hJust, vJust, text);
        return r.getDimension();
    }

    /**
     *
     * @param pos
     * @param hJust
     * @param vJust
     * @param text
     * @param bkColor
     * @param border
     * @param cornerRadius
     * @return
     */
    public GDimension drawJustifiedStringOnBackground(IVector2D pos, Justify hJust, Justify vJust, String text, GColor bkColor, float border, float cornerRadius) {
        return drawJustifiedStringOnBackground(pos.getX(), pos.getY(), hJust, vJust, text, bkColor, border, cornerRadius);
    }

    /**
     *
     * @param x
     * @param y
     * @param hJust
     * @param vJust
     * @param text
     * @param bkColor
     * @param border
     * @return
     */
    public GDimension drawJustifiedStringOnBackground(float x, float y, Justify hJust, Justify vJust, String text, GColor bkColor, float border) {
        return drawJustifiedStringOnBackground(x, y, hJust, vJust, text, bkColor, border, 0);
    }

    
    /**
     * 
     * @param str
     * @param maxWidth
     * @return
     */
    public final String [] generateWrappedLines(String str, float maxWidth) {
        List<String> lines = new ArrayList<>(32);
        generateWrappedText(str, maxWidth, lines, null);
        return lines.toArray(new String[lines.size()]);
    }

    /**
     *
     * @param str
     * @param maxWidth
     * @param resultLines cannot be null
     * @param resultLineWidths optional, can be null
     * @return the maxWidth of any line
     */
    public final GDimension generateWrappedText(String str, float maxWidth, List<String> resultLines, List<Float> resultLineWidths) {
        String text = str.trim();
        if (text.isEmpty())
            return GDimension.EMPTY;
        float maxLineWidth = 0;
        //List<String> lines = new ArrayList<String>(32);
        while (text.length() > 0 && resultLines.size() < 256) {
            int endl = text.indexOf('\n');
            if (endl >= 0) {
                String t = text.substring(0, endl).trim();
                float width = getTextWidth(t);
                if (width <= maxWidth) {
                    resultLines.add(t);
                    if (resultLineWidths != null)
                        resultLineWidths.add(width);
                    maxLineWidth = Math.max(maxLineWidth, width);
                    text = text.substring(endl+1);
                    continue;
                }
            }
            
            // cant find an endl, see if text fits
            float width = getTextWidth(text);
            if (width <= maxWidth) {
                resultLines.add(text);
                if (resultLineWidths != null)
                    resultLineWidths.add(width);
                maxLineWidth = Math.max(maxLineWidth, width);
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
                resultLines.add(t);
                if (resultLineWidths != null)
                    resultLineWidths.add(width);
                maxLineWidth = Math.max(maxLineWidth, width);
                text = text.substring(spc+1).trim();
                continue;
            }
            
            // made it here means we have to wrap on a whole word!

            // try to split in half with a hyphen
            boolean gotHalf = false;
            if (text.length() > 3) {
                t = text.substring(0, text.length() / 2);
                String halfStr = t + "-";
                if (getTextWidth(halfStr) < maxWidth) {
                    resultLines.add(halfStr);
                    gotHalf = true;
                }
            }
            if (!gotHalf) {
                t = split(text, 0, text.length(), maxWidth);
                resultLines.add(t);
            }
            width = getTextWidth(t);
            if (resultLineWidths != null) {
                resultLineWidths.add(width);
            }
            maxLineWidth = Math.max(maxLineWidth, width);


            try {
            	text = text.substring(t.length()).trim();
            } catch (Exception e) {
            	e.printStackTrace();
            	break;
            }
        }

        return new GDimension(maxLineWidth, resultLines.size()*getTextHeight());
    }    
    
    /**
     * Use binary search to find substring of s that has max chars up to maxWidth pixels wide.
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
    		error("splitR is taking too many iterations!");
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
     * @return the enclosing rect of the text
     */
    public final GDimension drawWrapString(float x, float y, float maxWidth, String text) {
        MutableVector2D tv = transform(x, y);
        String [] lines = generateWrappedLines(text, maxWidth);
        float mw = 0;
        for (int i=0; i<lines.length; i++) {
            mw = Math.max(mw, drawStringLine(tv.X(), tv.Y(), Justify.LEFT, lines[i]));
            tv.addEq(0, getTextHeight());
        }
        return new GDimension(mw, lines.length*getTextHeight());
    }    
    
    /**
     * 
     * @param x
     * @param y
     * @param maxWidth
     * @param hJust
     * @param vJust
     * @param text
     * @return the enclosing rect of the text
     */
    public GDimension drawWrapString(float x, float y, float maxWidth, Justify hJust, Justify vJust, String text) {
        String [] lines = generateWrappedLines(text, maxWidth);
        switch (vJust) {
            case TOP: break;
            case BOTTOM: y -= lines.length * getTextHeight(); break;
            case CENTER: y -= lines.length * getTextHeight() / 2; break;
            default: 
                throw new GException("Unhandled case: " + vJust);
        }
        MutableVector2D tv = transform(x, y);
        float mw = 0;
        for (int i=0; i<lines.length; i++) {
            mw = Math.max(mw, drawStringLine(tv.X(), tv.Y(), hJust, lines[i]));
            tv.addEq(0, getTextHeight());
        }
        return new GDimension(mw, lines.length*getTextHeight());
    }

    /**
     * Draws the string with background and makes sure it is completely on the screen
     * @param x
     * @param y
     * @param maxWidth
     * @param text
     * @param bkColor
     * @param border
     * @return
     */
    public GDimension drawWrapStringOnBackground(float x, float y, float maxWidth, String text, GColor bkColor, float border) {
        List<String> lines = new ArrayList<>();
        GDimension dim = generateWrappedText(text, maxWidth, lines, null);
        MutableVector2D tv = transform(x, y);
        if (tv.getX() + dim.width + border > getViewportWidth()) {
            tv.setX(getViewportWidth() - dim.width - border);
        }
        if (tv.getY() + dim.height + border > getViewportHeight()) {
            tv.setY(getViewportHeight() - dim.height - border);
        }

        pushMatrix();
        setIdentity();
        GColor textColor = getColor();
        setColor(bkColor);
        GRectangle r = new GRectangle(tv, dim);
        r.grow(border);
        r.drawFilled(this);
        setColor(textColor);
        for (String s : lines) {
            drawStringLine(tv.X(), tv.Y(), Justify.LEFT, s);
            tv.addEq(0, getTextHeight());
        }
        popMatrix();
        return r.getDimension();
    }

    /**
     * Draw a single line of top justified text and return the width of the text
     * @param x position in screen coordinates
     * @param y position in screen coordinates
     * @param hJust
     * @param text
     * @return the width of the line in pixels
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
     * Return the old width
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
     * Add a vertex relative to previous vertex
     * @param x
     * @param y
     */
    public abstract void moveTo(float x, float y);

    /**
     * Add a vertex relative to previous vertex
     * @param dv
     */
    public final void moveTo(IVector2D dv) {
        moveTo(dv.getX(), dv.getY());
    }

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
    public final void vertexArray(IVector2D ... a) {
        for (IVector2D t : a)
            vertex(t);
    }

    /**
     *
     * @param verts
     */
    public final void vertexArray(float [][] verts) {
        for (float [] v : verts) {
            vertex(v[0], v[1]);
        }
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
     * Draw a series of triangle. Every 3 vertices is treated as a unique triangle
     */
    public abstract void drawTriangles();

    /**
     * draw a series of adjacent triangles that all share a common point, the first vertex in the list
     */
    public abstract void drawTriangleFan();
    
    /**
     * draw a series of adjacent triangles where the last 2 vertices of the current triangle are used as the first 2 point of the next triangle
     */
    public abstract void drawTriangleStrip();
    
    /**
     * draw a series of connected quads where the last 2 vertices of the current quad is used as the first 2 points of the next quad.
     * Note the points are not circular:
     *
     *  A -- C
     *  |    |
     *  B -- D
     */
    public abstract void drawQuadStrip();

    /**
     * Use each pair of points to render a rectangle
     */
    public abstract void drawRects();

    /**
     * Convenience
     * @param linethickness
     */
    public final void drawRects(float linethickness) {
        float saveT = setLineWidth(linethickness);
        drawRects();
        setLineWidth(saveT);
    }

    /**
     *
     */
    public abstract void drawFilledRects();

    /**
     * 
     * @param assetPath file location to load the image
     * @param transparent optional color to make the transparent color if the image does not have transparency built in
     * @return an id >= 0 if the image was loaded or -1 if not
     */
    public abstract int loadImage(String assetPath, GColor transparent);

    /**
     * 
     * @param assetPath
     * @param w
     * @param h
     * @param numCellsX
     * @param numCells
     * @param bordeered
     * @param transparent
     * @return an array of length numCells with ids to the sub images or null if asset path does not produce an image
     */
    public abstract int [] loadImageCells(String assetPath, int w, int h, int numCellsX, int numCells, boolean bordeered, GColor transparent);
    
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
     * @param m
     */
    public abstract void multMatrix(Matrix3x3 m);
    
    /**
     * 
     * @param x
     * @param y
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
     *
     * @param result
     */
    public abstract void getTransform(Matrix3x3 result);

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
     *
     * @param v0
     * @param v1
     */
    public final void drawLine(IVector2D v0, IVector2D v1) {
        drawLine(v0.getX(), v0.getY(), v1.getX(), v1.getY());
    }

    /**
     *
     * @param v0
     * @param v1
     * @param thickness
     */
    public final void drawLine(IVector2D v0, IVector2D v1, float thickness) {
        drawLine(v0.getX(), v0.getY(), v1.getX(), v1.getY(), thickness);
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
     * @param x_pts
     * @param y_pts
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
        begin();
        vertex(x, y);
        vertex(x+w, y+h);
        drawLine(x, y, x+w, y, thickness);
        drawLine(x+w, y, x+w, y+h, thickness);
        drawLine(x, y, x, y+h, thickness);
        drawLine(x, y+h, x+w, y+h, thickness);
    }

    /**
     *
     * @param dim
     * @param thickness
     */
    public final void drawRect(GDimension dim, float thickness) {
        drawRect(0, 0, dim.width, dim.height, thickness);
    }

    /**
     *
     * @param rect
     */
    public final void drawRect(GRectangle rect) {
        drawRect(rect.x, rect.y, rect.w, rect.h);
    }

    /**
     *
     * @param rect
     * @param thickness
     */
    public final void drawRect(GRectangle rect, float thickness) {
        drawRect(rect.x, rect.y, rect.w, rect.h, thickness);
    }

    /**
     *
     * @param v0
     * @param v1
     * @param thickness
     */
    public final void drawRect(IVector2D v0, IVector2D v1, float thickness) {
        float X = Math.min(v0.getX(), v1.getX());
        float Y = Math.min(v0.getY(), v1.getY());
        float W = Math.abs(v0.getX()-v1.getX());
        float H = Math.abs(v0.getY()-v1.getY());
        drawRect(X,Y,W,H,thickness);
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
    public void drawRoundedRect(float x, float y, float w, float h, float thickness, float radius) {
        if (radius < 1) {
            drawRect(x, y, w, h, thickness);
            return;
        }

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
     * @param rect
     * @param thickness
     * @param radius
     */
    public final void drawRoundedRect(GRectangle rect, float thickness, float radius) {
        drawRoundedRect(rect.x, rect.y, rect.w, rect.h, thickness, radius);
    }

    /**
     *
     * @param dim
     * @param thickness
     * @param radius
     */
    public final void drawRoundedRect(GDimension dim, float thickness, float radius) {
        drawRoundedRect(0, 0, dim.width, dim.height, thickness, radius);
    }

    /**
     * 
     * @param x
     * @param y
     * @param w
     * @param radius
     */
    public void drawFilledRoundedRect(float x, float y, float w, float h, float radius) {
        if (radius < 1) {
            drawFilledRect(x, y, w, h);
            return;
        }
    	float r2 = radius*2;
    	drawFilledWedge(x+radius, y+radius, radius, 180f, 90f, 6);
    	drawFilledRect(x+radius,y,w-r2,radius);
    	drawFilledWedge(x+w-radius,y+radius,radius,270f,90f, 6);
    	drawFilledRect(x,y+radius,w,h-r2);
    	drawFilledWedge(x+w-radius,y+h-radius,radius,0,90, 6);
    	drawFilledRect(x+radius,y+h-radius,w-r2,radius);
    	drawFilledWedge(x+radius,y+h-radius,radius,90f,90f, 6);
    }

    /**
     *
     * @param rect
     * @param radius
     */
    public void drawFilledRoundedRect(GRectangle rect, float radius) {
        drawFilledRoundedRect(rect.x, rect.y, rect.w, rect.h, radius);
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

    public final void drawFilledRect(float x, float y, float w, float h) {
        drawQuad(x, y, x+w, y+h);
    }

    public final void drawFilledRect(GRectangle rect) {
        drawQuad(rect.x, rect.y, rect.x+rect.w, rect.y+rect.h);
    }

    private void drawFilledRectP(float x, float y, float w, float h) {
        vertex(x, y);
        vertex(x+w, y+h);
        drawFilledRects();
    }

    // return whether points added for quads or lines (true == quads)
    private final void circlePoints(float radius, float startAngle, float sweep, int sections) {
        final float step = sweep / sections;
        float angle = startAngle;
        final float endAngle = startAngle + sweep;
        /*
    	float radius = (innerRadius + outerRadius)/2;
        //float sections = radius * sweep/180;
        //if (sections > 32)
        //    sections = 32;
        //else if (sections < 8)
        //    sections = 8;


        
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
        } else*/
         {
	        for (int i=0; i<sections; i++) {
	            float x = CMath.cosine(angle) * radius;
	            float y = CMath.sine(angle) * radius;
	            vertex(x, y);
	            angle += step;
	        }
	        vertex(CMath.cosine(endAngle) * radius, CMath.sine(endAngle) * radius);
	        //return false;
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
     * @param sections
     */
    public final void drawDisk(float x, float y, float radius, int sections) {
        pushMatrix();
        translate(x, y);
        begin();
        vertex(0, 0);
        circlePoints(radius, 0, 360, sections);
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
        circlePoints(radius, startDegrees, sweepDegrees, sections);
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
        circlePoints(radius, 0, 360, sections);
      	drawLineLoop();
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
        circlePoints(radius, startDegrees, sweepDegrees, sections);
       	drawLineStrip();
        popMatrix();
        setLineWidth(saveThickness);
    }

    /**
     *
     * @param x
     * @param y
     * @param radius
     * @param startDegrees
     * @param sweepDegrees
     * @param sections
     */
    public final void drawFilledArc(float x, float y, float radius, float startDegrees, float sweepDegrees, int sections) {
        pushMatrix();
        translate(x, y);
        begin();
        vertex(0, 0);
        circlePoints(radius, startDegrees, sweepDegrees, sections);
        drawTriangleFan();
        popMatrix();
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
     *
     * @param center
     * @param radius
     */
    public final void drawCircle(IVector2D center, float radius) {
        drawCircle(center.getX(), center.getY(), radius);
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
     * @param radius
     * @param thickness
     */
    public final void drawCircleWithThickness(float x, float y, float radius, int thickness) {
        int sections = Math.round(radius * 4);
        sections = Utils.clamp(sections,  8,  32);
        drawCircle(x, y, radius, thickness, sections);
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
        circlePoints(radius, 0, 360, sections);
       	drawLineLoop();
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
    public void drawFilledOval(float x, float y, float w, float h) {
        pushMatrix();
        MutableVector2D mv0 = transform(x, y);
        MutableVector2D mv2 = transform(x+w, y+h);
        mv2.subEq(mv0);
        float radius = Math.max(mv2.getX()/2, mv2.getY()/2);
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
     * @param center
     * @param radius
     */
	public final void drawFilledCircle(IVector2D center, float radius) {
	    drawFilledCircle(center.getX(), center.getY(), radius);
    }

    /**
     *
     * @param x
     * @param y
     * @param r
     */
    public final void drawFilledCircle(float x, float y, float r) {
        drawFilledOval(x-r, y-r, r*2, r*2);
    }

    /**
     *
     * @param pts_x
     * @param pts_y
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
     * Draw an image with pre-transformed rectangle. Not to be called directly.
     * @param imageKey
     * @param x
     * @param y
     * @param w
     * @param h
     */
    protected abstract void drawImage(int imageKey, int x, int y, int w, int h);

    /**
     *
     * @param imageKey
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public final void drawImage(int imageKey, float x, float y, float w, float h) {
        drawImage(imageKey, new Vector2D(x,y), new Vector2D(x+w, y+h));
    }

    /**
     *
     * @param imageKey
     * @param rect0
     * @param rect1
     */
    public final void drawImage(int imageKey, IVector2D rect0, IVector2D rect1) {

        MutableVector2D v0 = new MutableVector2D(rect0);
        MutableVector2D v1 = new MutableVector2D(rect1);

        transform(v0);
        transform(v1);

        Vector2D minV = v0.min(v1);
        Vector2D maxV = v0.max(v1);

        drawImage(imageKey, minV.Xi(), minV.Yi(), maxV.Xi()-minV.Xi(), maxV.Yi()-minV.Yi());
    }

    /**
     *
     * @param imageKey
     * @param rect
     */
    public final void drawImage(int imageKey, GRectangle rect) {
        drawImage(imageKey, rect.x, rect.y, rect.w, rect.h);
    }

    /**
     *
     * @param imageKey
     * @param center
     * @param dimension
     */
    public final void drawImage(int imageKey, IVector2D center, GDimension dimension) {
        drawImage(imageKey, center.getX()-dimension.width/2, center.getY()-dimension.height/2, dimension.width, dimension.height);
    }

    /**
     * Draw image centered at center using its natural dimension
     * @param imageKey
     * @param center
     */
    public final void drawImage(int imageKey, IVector2D center) {
        AImage img = getImage(imageKey);
        if (img != null) {
            drawImage(imageKey, center.getX()-img.getWidth()/2, center.getY()-img.getHeight()/2, img.getWidth(), img.getHeight());
        }
    }

    /**
     * 
     * @param color
     */
    public abstract void clearScreen(GColor color);
    
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

    /**
     * Reset min/max bounding rect
     */
	public abstract void clearMinMax();
	
	/**
	 * Get the min transformed point min of all verts since last call to clearMinMax
	 * @return
	 */
	public abstract Vector2D getMinBoundingRect();
	
	/**
	 * Get the max transformed point of all verts since last call to clearMinMax
	 * @return
	 */
	public abstract Vector2D getMaxBoundingRect();

    /**
     * Specify a clip rect in object coordinates
     * @param x
     * @param y
     * @param w
     * @param h
     */
	public abstract void setClipRect(float x, float y, float w, float h);

    /**
     * Convenience
     *
     * TODO: make push/pop clip is better
     *
     * @param rect
     */
	public final void setClipRect(GRectangle rect) {
	    setClipRect(rect.x, rect.y, rect.w, rect.y);
    }

    /**
     *
     * @param p0
     * @param p1
     */
    public final void setClipRect(IVector2D p0, IVector2D p1) {
	    setClipRect(new GRectangle(p0, p1));
    }

    /**
     * Clears out any clipping bounds applied
     */
    public abstract void clearClip();

    /**
     * Return the most recent clip or the whole screen if not set.
     *
     * @return
     */
    public abstract GRectangle getClipRect();
}
