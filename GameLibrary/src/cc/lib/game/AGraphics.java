package cc.lib.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.lib.math.Matrix3x3;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.utils.GException;

public abstract class AGraphics implements Utils.VertexList, Renderable {

    public static boolean DEBUG_ENABLED = false;

    private int mViewportWidth, mViewportHeight;
    private float[] textHeightStack = new float[32];
    private int textHeightIndex = 0;
    private GColor[] colorStack = new GColor[32];
    private int colorStackIndex = 0;

    /**
     *
     */
    protected AGraphics() {

    }

    /**
     * @param viewportWidth
     * @param viewportHeight
     */
    protected AGraphics(int viewportWidth, int viewportHeight) {
        this.mViewportWidth = viewportWidth;
        this.mViewportHeight = viewportHeight;
    }

    /**
     * @param color
     */
    public abstract void setColor(GColor color);

    /**
     * @param argb
     */
    public abstract void setColorARGB(int argb);

    /**
     * @param r
     * @param g
     * @param b
     * @param a
     */
    public abstract void setColor(int r, int g, int b, int a);

    /**
     * @return
     */
    public abstract GColor getColor();

    /**
     * @param color
     */
    public final void pushColor(GColor color) {
        colorStack[colorStackIndex++] = getColor();
        setColor(color);
    }

    /**
     *
     */
    public final void popColor() {
        setColor(colorStack[--colorStackIndex]);
    }

    /**
     * @param height
     */
    public final void pushTextHeight(float height, boolean pixels) {
        textHeightStack[textHeightIndex++] = getTextHeight();
        setTextHeight(height, pixels);
    }

    /**
     *
     */
    public final void popTextHeight() {
        setTextHeight(textHeightStack[--textHeightIndex], true);
    }

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
     * @return
     */
    public final GDimension getViewport() {
        return new GDimension(mViewportWidth, mViewportHeight);
    }

    /**
     * Convenience method to draw with LEFT/TOP Justification
     *
     * @param x
     * @param y
     * @param text
     * @return the enclosing rect of the text
     */
    public final GDimension drawJustifiedString(float x, float y, String text) {
        return drawJustifiedString(x, y, Justify.LEFT, Justify.TOP, text);
    }

    /**
     * @param pos
     * @param text
     * @return
     */
    public final GDimension drawJustifiedString(IVector2D pos, String text) {
        return drawJustifiedString(pos.getX(), pos.getY(), text);
    }

    /**
     * Convenience to draw with TOP Justification
     *
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
     *
     * @param text
     * @param x
     * @param y
     * @return the enclosing rect of the text
     */
    public final GDimension drawString(String text, float x, float y) {
        return drawJustifiedString(x, y, Justify.LEFT, Justify.TOP, text);
    }

    /**
     * @param text
     * @param pos
     * @return
     */
    public final GDimension drawString(String text, IVector2D pos) {
        return drawJustifiedString(pos.getX(), pos.getY(), Justify.LEFT, Justify.TOP, text);
    }

    /**
     * @param text
     * @param x
     * @param y
     * @return
     */
    public final GDimension drawAnnotatedString(String text, float x, float y) {
        return drawAnnotatedString(text, x, y, Justify.LEFT);
    }

    /**
     * @param text
     * @param pos
     * @return
     */
    public final GDimension drawAnnotatedString(String text, IVector2D pos) {
        return drawAnnotatedString(text, pos.getX(), pos.getY(), Justify.LEFT);
    }

    /**
     * Annotated string can have annotations in the string (ala html) to control color, underline etc.
     * <p>
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

    final static Pattern ANNOTATION_PATTERN = Pattern.compile("(ARGB)?\\[([0-9]{1,3},)?[0-9]{1,3},[0-9]{1,3},[0-9]{1,3}\\]");

    /**
     * @return
     */
    public abstract float getTextHeight();

    /**
     * Set the text height. If the value is a system type (like dp on android)
     * then 'pixels' param should be false
     *
     * @param height
     * @param pixels
     */
    public abstract float setTextHeight(float height, boolean pixels);

    /**
     * Width of text string in pixels
     *
     * @param string
     * @return
     */
    public abstract float getTextWidth(String string);

    /**
     * @param string
     * @return
     */
    public final GDimension getTextDimension(String string, float maxWidth) {
        List<String> lines = new ArrayList();
        return getTextDimension(string, maxWidth, lines);
    }

    public final GDimension getTextDimension(String string, float maxWidth, List<String> lines) {
        lines.clear();
        lines.addAll(generateWrappedLines(string, maxWidth));
        float width = 0;
        float height = getTextHeight() * lines.size();
        for (String s : lines) {
            width = Math.max(width, getTextWidth(s));
        }
        return new GDimension((int) (width + 0.9f), height);
    }

    /**
     * Gives the enclosing rectangle given the current transform
     *
     * @param text
     * @return
     */
    public final GRectangle getTextRectangle(String text) {
        GRectangle rect = new GRectangle(0, 0, getTextWidth(text), getTextHeight());
        screenToViewport(rect);
        return rect;
    }

    /**
     * Return true if screen capture functionality is available. Default false
     *
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
     * Apply all transforms to x, y
     * @param x
     * @param y
     * @param result
     */
    public abstract void transform(float x, float y, float [] result);
    
    /**
     * @param v
     */
    public final MutableVector2D transform(MutableVector2D v) {
        float[] result = new float[2];
        transform(v.getX(), v.getY(), result);
        v.assign(result[0], result[1]);
        return v;
    }

    /**
     *
     * @param x
     * @param y
     * @return
     */
    public final MutableVector2D transform(float x, float y) {
        float[] result = new float[2];
        transform(x, y, result);
        return new MutableVector2D(result[0], result[1]);
    }
    
    /**
     * Convert screen coordinates (like from a mouse) to view port coordinates
     * @param screenX
     * @param screenY
     * @return
     */
    public final Vector2D screenToViewport(int screenX, int screenY) {
        return untransform(screenX, screenY);
    }

    public final MutableVector2D screenToViewport(float screenX, float screenY) {
        return untransform(screenX, screenY);
    }

    public final MutableVector2D screenToViewport(IVector2D screen) {
        return untransform(screen.getX(), screen.getY());
    }

    public final void screenToViewport(MutableVector2D mouse) {
        mouse.assign(untransform(mouse.getX(), mouse.getY()));
    }

    public final void screenToViewport(GRectangle rect) {
        MutableVector2D tl = rect.getTopLeft();
        screenToViewport(tl);
        MutableVector2D br = rect.getBottomRight();
        screenToViewport(br);
        rect.set(tl, br);
    }

    public final void screenToViewport(GDimension dim) {
        GRectangle r = new GRectangle(dim);
        screenToViewport(r);
        dim.assign(r);
    }

    protected abstract MutableVector2D untransform(float x, float y);

    /**
     * Draw a justified block text.  '\n' is a delimiter for separate lines
     * @param x
     * @param y
     * @param hJust
     * @param vJust
     * @param text
     * @return the total height of the text. 
     */
    public final GDimension drawJustifiedString(float x, float y, Justify hJust, Justify vJust, String text) {
        if (text==null || text.length() == 0)
            return GDimension.EMPTY;
        MutableVector2D mv = transform(x, y);
        String [] lines = text.split("\n");
        final float textHeight = getTextHeight();
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
            maxWidth = Math.max(drawStringLine(mv.getX(), mv.getY(), hJust, lines[i]), maxWidth);
            mv.addEq(0, textHeight);
        }
        float maxHeight = textHeight * lines.length;
        begin();
        vertex(x, y);
        switch (hJust) {
            case RIGHT:
                vertex(x - maxWidth, y + maxHeight);
                break;
            case CENTER:
                vertex(x - maxWidth / 2, y + maxHeight);
                break;
            case LEFT:
                vertex(x + maxWidth / 2, y + maxHeight);
                break;
        }
        return new GDimension(maxWidth, maxHeight);
    }

    public final GDimension drawJustifiedString(int x, int y, Justify hJust, Justify vJust, String text) {
        return drawJustifiedString((float) x, (float) y, hJust, vJust, text);
    }

    public GRectangle drawJustifiedStringR(float x, float y, Justify hJust, Justify vJust, String text) {
        if (text == null || text.length() == 0)
            return new GRectangle();
        MutableVector2D mv = transform(x, y);
        String[] lines = text.split("\n");
        final float textHeight = (float) getTextHeight();
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
            maxWidth = Math.max(drawStringLine(mv.getX(), mv.getY(), hJust, lines[i]), maxWidth);
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

    public final GRectangle drawStringOnBackground(float x, float y, String text, GColor bkColor, float border, float cornerRadius) {
        return drawJustifiedStringOnBackground(x, y, Justify.LEFT, Justify.TOP, text, bkColor, border, cornerRadius);
    }

    public final GRectangle drawStringOnBackground(IVector2D v, String text, GColor bkColor, float border, float cornerRadius) {
        return drawStringOnBackground(v.getX(), v.getY(), text, bkColor, border, cornerRadius);
    }

    /**
     * @param x
     * @param y
     * @param hJust
     * @param vJust
     * @param text
     * @param bkColor
     * @param border
     * @return the rectangle enclosing the text
     */
    public final GRectangle drawJustifiedStringOnBackground(float x, float y, Justify hJust, Justify vJust, String text, GColor bkColor, float border, float cornerRadius) {
        GRectangle r = drawJustifiedStringR(x, y, hJust, vJust, text);
        pushMatrix();
        setIdentity();
        ortho();
        r.grow(border);
        GColor saveColor = getColor();
        setColor(bkColor);
        if (cornerRadius > 0)
            drawFilledRoundedRect(r, cornerRadius);
        else
            r.drawFilled(this);
        setColor(saveColor);
        popMatrix();
        drawJustifiedString(x, y, hJust, vJust, text);
        return r;
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
    public final GRectangle drawJustifiedStringOnBackground(IVector2D pos, Justify hJust, Justify vJust, String text, GColor bkColor, float border, float cornerRadius) {
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
     * @return the rectangle enclosing the text
     */
    public final GRectangle drawJustifiedStringOnBackground(float x, float y, Justify hJust, Justify vJust, String text, GColor bkColor, float border) {
        return drawJustifiedStringOnBackground(x, y, hJust, vJust, text, bkColor, border, 0);
    }

    public final static int BORDER_FLAG_NORTH = 1<<0;
    public final static int BORDER_FLAG_SOUTH = 1<<1;
    public final static int BORDER_FLAG_EAST  = 1<<2;
    public final static int BORDER_FLAG_WEST  = 1<<3;

    public final static class Border {
        final float thickness;
        final int flag;
        final Vector2D sizeAdjust;
        final Vector2D positionAdjust;

        public Border(int flag, float thickness, float dw, float dh, float dx, float dy) {
            this.flag = flag;
            this.thickness = thickness;
            this.sizeAdjust = new Vector2D(dw, dh);
            this.positionAdjust = new Vector2D(dx, dy);
        }
    }

    /**
     *
     * @param x
     * @param y
     * @param hJust
     * @param vJust
     * @param text
     * @param borders
     * @return
     */
    public GRectangle drawJustifiedStringBordered(float x, float y, Justify hJust, Justify vJust, String text, Border ... borders) {
        GRectangle r = drawJustifiedStringR(x, y, hJust, vJust, text);
        pushMatrix();
        setIdentity();
        ortho();
        for (Border b : borders) {
            MutableVector2D v0=null, v1=null;
            switch (b.flag) {
                case BORDER_FLAG_NORTH:
                    v0=r.getTopLeft();
                    v1=r.getTopRight();
                    break;
                case BORDER_FLAG_SOUTH:
                    v0=r.getBottomLeft();
                    v1=r.getBottomRight();
                    break;
                case BORDER_FLAG_EAST:
                    v0=r.getTopRight();
                    v1=r.getBottomRight();
                    break;
                case BORDER_FLAG_WEST:
                    v0=r.getTopLeft();
                    v1=r.getBottomLeft();
                    break;
                default:
                    throw new GException("Unhandled case: " + b.flag);
            }
            drawLine(v0.addEq(b.positionAdjust), v1.addEq(b.positionAdjust).addEq(b.sizeAdjust), b.thickness);
        }
        popMatrix();
        drawJustifiedString(x, y, hJust, vJust, text);
        return r;
    }

    /**
     *
     * @param v
     * @param hJust
     * @param vJust
     * @param text
     * @param border
     * @return
     */
    public GRectangle drawJustifiedStringBordered(IVector2D v, Justify hJust, Justify vJust, String text, Border ... border) {
        return drawJustifiedStringBordered(v.getX(), v.getY(), hJust, vJust, text, border);
    }

    /**
     * @param str
     * @param maxWidth
     * @return
     */
    public final List<String> generateWrappedLines(String str, float maxWidth) {
        List<String> lines = new ArrayList<>(32);
        generateWrappedText(str, maxWidth, lines, null);
        return lines;
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
        String text = Utils.trimSpaces(str);
        if (text.isEmpty())
            return GDimension.EMPTY;
        float maxLineWidth = 0;
        while (text.length() > 0 && resultLines.size() < 256) {
            int endl = text.indexOf('\n');
            if (endl >= 0) {
                String t = Utils.trimSpaces(text.substring(0, endl));
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
                    t = Utils.trimSpaces(t.substring(0, spc));
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
                text = Utils.trimSpaces(text.substring(spc+1));
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
            	text = Utils.trimSpaces(text.substring(t.length()));
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
        List<String> lines = generateWrappedLines(text, maxWidth);
        float mw = 0;
        for (String line : lines) {
            mw = Math.max(mw, drawStringLine(tv.getX(), tv.getY(), Justify.LEFT, line));
            tv.addEq(0, getTextHeight());
        }
        return new GDimension(mw, lines.size() * getTextHeight());
    }

    /**
     * @param pos
     * @param maxWidth
     * @param text
     * @return
     */
    public final GDimension drawWrapString(IVector2D pos, float maxWidth, String text) {
        return drawWrapString(pos.getX(), pos.getY(), maxWidth, text);
    }

    /**
     * @param x
     * @param y
     * @param maxWidth
     * @param hJust
     * @param vJust
     * @param text
     * @return the enclosing rect of the text
     */
    public final GDimension drawWrapString(float x, float y, float maxWidth, Justify hJust, Justify vJust, String text) {
        List<String> lines = generateWrappedLines(text, maxWidth);
        switch (vJust) {
            case TOP:
                break;
            case BOTTOM:
                y -= lines.size() * getTextHeight();
                break;
            case CENTER:
                y -= lines.size() * getTextHeight() / 2;
                break;
            default:
                throw new GException("Unhandled case: " + vJust);
        }
        MutableVector2D tv = transform(x, y);
        float mw = 0;
        for (int i = 0; i < lines.size(); i++) {
            mw = Math.max(mw, drawStringLine(tv.getX(), tv.getY(), hJust, lines.get(i)));
            tv.addEq(0, getTextHeight());
        }
        return new GDimension(mw, lines.size() * getTextHeight());
    }

    /**
     * @param pos
     * @param maxWidth
     * @param hJust
     * @param vJust
     * @param text
     * @return
     */
    public final GDimension drawWrapString(IVector2D pos, float maxWidth, Justify hJust, Justify vJust, String text) {
        return drawWrapString(pos.getX(), pos.getY(), maxWidth, hJust, vJust, text);
    }

    /**
     * Draws the string with background and makes sure it is completely on the screen
     *
     * @param x
     * @param y
     * @param maxWidth
     * @param text
     * @param bkColor
     * @param border
     * @return
     */
    public final GRectangle drawWrapStringOnBackground(float x, float y, float maxWidth, String text, GColor bkColor, float border) {
        List<String> lines = new ArrayList<>();
        GDimension dim = generateWrappedText(text, maxWidth, lines, null);
        MutableVector2D tv = transform(x, y);
        if (tv.getX() + dim.getWidth() + border > getViewportWidth()) {
            tv.setX(getViewportWidth() - dim.getWidth() - border);
        }
        if (tv.getY() + dim.getHeight() + border > getViewportHeight()) {
            tv.setY(getViewportHeight() - dim.getHeight() - border);
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
            drawStringLine(tv.getX(), tv.getY(), Justify.LEFT, s);
            tv.addEq(0, getTextHeight());
        }
        popMatrix();
        return r;
    }

    public final GRectangle drawWrapStringOnBackground(float x, float y, float maxWidth, Justify horz, Justify vert, String text, GColor bkColor, float border) {
        List<String> lines = new ArrayList<>();
        GDimension dim = generateWrappedText(text, maxWidth, lines, null);
        MutableVector2D tv = transform(x, y);

        switch (horz) {
            case CENTER:
                tv.addEq(-dim.getWidth() / 2, 0);
                break;
            case RIGHT:
                tv.addEq(-dim.getHeight(), 0);
        }

        switch (vert) {
            case CENTER:
                tv.addEq(0, -dim.getHeight() / 2);
                break;
            case BOTTOM:
                tv.addEq(0, dim.getHeight() / 2);
                break;
        }

        if (tv.getX() + dim.getWidth() + border > getViewportWidth()) {
            tv.setX(getViewportWidth() - dim.getWidth() - border);
        }
        if (tv.getY() + dim.getHeight() + border > getViewportHeight()) {
            tv.setY(getViewportHeight() - dim.getHeight() - border);
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
            drawStringLine(tv.getX(), tv.getY(), Justify.LEFT, s);
            tv.addEq(0, getTextHeight());
        }
        popMatrix();
        return r;
    }

    /**
     * Draw a single line of top justified text and return the width of the text
     *
     * @param x     position in screen coordinates
     * @param y     position in screen coordinates
     * @param hJust
     * @param text
     * @return the width of the line in pixels
     */
    protected abstract float drawStringLine(float x, float y, Justify hJust, String text);
    
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
     * @param bordered
     * @param transparent
     * @return an array of length numCells with ids to the sub images or null if asset path does not produce an image
     */
    public abstract int [] loadImageCells(String assetPath, int w, int h, int numCellsX, int numCells, boolean bordered, GColor transparent);

    /**
     *
     * @param source
     * @param cells
     * @return
     */
    public int [] loadImageCells(int source, int [][] cells) {
        int [] result = new int[cells.length];
        for (int i=0; i<result.length; i++) {
            int x = cells[i][0];
            int y = cells[i][1];
            int w = cells[i][2];
            int h = cells[i][3];
            try {
                result[i] = newSubImage(source, x, y, w, h);
            } catch (Throwable e) {
                throw new GException("Problem loading image cell " + i, e);
            }
        }
        return result;
    }

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

    public int getPushDepth() {
        return 0;
    }

    /**
     *
     */
    public abstract void popMatrix();

    public abstract void resetMatrices();

    /**
     *
     */
    public abstract void setIdentity();

    /**
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
     *
     * @param v0
     * @param v1
     * @param thickness
     * @param dashLength
     */
    public final void drawDashedLine(IVector2D v0, IVector2D v1, float thickness, float dashLength) {
        drawDashedLine(v0.getX(), v0.getY(), v1.getX(), v1.getY(), thickness, dashLength);
    }

    /**
     *
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param thickness
     * @param dashLength
     */
    public abstract void drawDashedLine(float x0, float y0, float x1, float y1, float thickness, float dashLength);

    /**
     * Convenience.  Thickness defaults to 1
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     */
    public void drawLine(float x0, float y0, float x1, float y1) {
        drawLine(x0, y0, x1, y1, 1);
    }
    
    /**
     *
     * @param x_pts
     * @param y_pts
     * @param thickness
     */
    public final void drawLineStrip(int[] x_pts, int[] y_pts, int thickness) {
        Utils.assertTrue(x_pts.length == y_pts.length);
        float oldWidth = setLineWidth(thickness);
        begin();
        for (int i = 0; i < x_pts.length - 1; i++) {
            vertex(x_pts[i], y_pts[i]);
            vertex(x_pts[i + 1], y_pts[i + 1]);
        }
        drawLines();
        setLineWidth(oldWidth);
    }

    /**
     * @param x_pts
     * @param y_pts
     * @param thickness
     */
    public final void drawLineStrip(float[] x_pts, float[] y_pts, float thickness) {
        Utils.assertTrue(x_pts.length == y_pts.length);
        float oldWidth = setLineWidth(thickness);
        begin();
        for (int i = 0; i < x_pts.length - 1; i++) {
            vertex(x_pts[i], y_pts[i]);
            vertex(x_pts[i + 1], y_pts[i + 1]);
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
    public final void drawRect(IDimension dim, float thickness) {
        drawRect(0, 0, dim.getWidth(), dim.getHeight(), thickness);
    }

    /**
     *
     * @param rect
     */
    public final void drawRect(IRectangle rect) {
        drawRect(rect.getLeft(), rect.getTop(), rect.getWidth(), rect.getHeight());
    }

    /**
     *
     * @param rect
     * @param thickness
     */
    public final void drawRect(IRectangle rect, float thickness) {
        drawRect(rect.getLeft(), rect.getTop(), rect.getWidth(), rect.getHeight(), thickness);
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
        float W = Math.abs(v0.getX() - v1.getX());
        float H = Math.abs(v0.getY() - v1.getY());
        drawRect(X, Y, W, H, thickness);
    }

    /**
     * @param v0
     * @param width
     * @param height
     * @param thickness
     */
    public final void drawRect(IVector2D v0, float width, float height, float thickness) {
        drawRect(v0.getX(), v0.getY(), width, height, thickness);
    }

    /**
     * @param v0
     * @param width
     * @param height
     */
    public final void drawRect(IVector2D v0, float width, float height) {
        drawRect(v0.getX(), v0.getY(), width, height);
    }

    /**
     * @param x
     * @param y
     * @param w
     * @param h
     * @param radius
     */
    public abstract void drawRoundedRect(float x, float y, float w, float h, float radius);

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
        float t = setLineWidth(thickness);
        drawRoundedRect(x, y, w, h, radius);
        setLineWidth(t);
    }

    /**
     *
     * @param rect
     * @param thickness
     * @param radius
     */
    public final void drawRoundedRect(IRectangle rect, float thickness, float radius) {
        drawRoundedRect(rect.getLeft(), rect.getTop(), rect.getWidth(), rect.getHeight(), thickness, radius);
    }

    /**
     *
     * @param dim
     * @param thickness
     * @param radius
     */
    public final void drawRoundedRect(IDimension dim, float thickness, float radius) {
        drawRoundedRect(0, 0, dim.getWidth(), dim.getHeight(), thickness, radius);
    }

    /**
     * 
     * @param x
     * @param y
     * @param w
     * @param radius
     */
    public abstract void drawFilledRoundedRect(float x, float y, float w, float h, float radius);

    /**
     *
     * @param rect
     * @param radius
     */
    public final void drawFilledRoundedRect(IRectangle rect, float radius) {
        drawFilledRoundedRect(rect.getLeft(), rect.getTop(), rect.getWidth(), rect.getHeight(), radius);
    }
    
    /**
     * Convenience.  Thickness defaults to 1.
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public void drawRect(float x, float y, float w, float h) {
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
     *
     */
    public void drawFilledRect(float x, float y, float w, float h) {
        drawQuad(x, y, x+w, y+h);
    }

    /**
     *
     * @param rect
     */
    public final void drawFilledRect(IRectangle rect) {
        drawFilledRect(rect.getLeft(), rect.getTop(), rect.getWidth(), rect.getHeight());
    }

    /**
     *
     * @param center
     * @param dim
     */
    public final void drawFilledRect(IVector2D center, IDimension dim) {
        drawFilledRect(center.getX() - dim.getWidth()/2, center.getY()-dim.getHeight()/2, dim.getWidth(), dim.getHeight());
    }

    /**
     * Draw a filled wedge
     *
     * @param cx
     * @param cy
     * @param radius
     * @param startDegrees
     * @param sweepDegrees
     */
    public abstract void drawWedge(float cx, float cy, float radius, float startDegrees, float sweepDegrees);
    
    /**
     * 
     * @param x
     * @param y
     * @param radius
     * @param startDegrees
     * @param sweepDegrees
     */
    public abstract void drawArc(float x, float y, float radius, float startDegrees, float sweepDegrees);

    /**
     *
     * @param v
     * @param radius
     * @param startDegrees
     * @param sweepDegrees
     */
    public void drawArc(IVector2D v, float radius, float startDegrees, float sweepDegrees) {
        drawArc(v.getX(), v.getY(), radius, startDegrees, sweepDegrees);
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
    public final void drawArc(float x, float y, float radius, float thickness, float startDegrees, float sweepDegrees) {
        float t = setLineWidth(thickness);
        drawArc(x, y, radius, startDegrees, sweepDegrees);
        setLineWidth(t);
    }

    /**
     * 
     * @param x
     * @param y
     * @param radius
     */
    public void drawCircle(float x, float y, float radius) {
        drawOval(x-radius, y-radius, radius*2, radius*2);
    }

    /**
     *
     * @param center
     * @param radius
     * @param thickness
     */
    public final void drawCircle(IVector2D center, float radius, float thickness) {
        float old = setLineWidth(thickness);
        drawCircle(center.getX(), center.getY(), radius);
        setLineWidth(old);
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
     *
     * @param x
     * @param y
     * @param radius
     * @param thickness
     */
    public final void drawCircle(float x, float y, float radius, float thickness) {
        float t = setLineWidth(thickness);
        drawCircle(x, y, radius);
        setLineWidth(t);
    }

    /**
     * 
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public final void drawOval(float x, float y, float w, float h, float thickness) {
        float saveThickness = setLineWidth(thickness);
        drawOval(x, y, w, h);
        setLineWidth(saveThickness);
    }

    /**
     *
     * @param r
     * @param thickness
     */
    public void drawOval(IRectangle r, float thickness) {
        drawOval(r.getLeft(), r.getTop(), r.getWidth(), r.getHeight(), thickness);
    }

    /**
     * 
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public abstract void drawOval(float x, float y, float w, float h);
    
    /**
     * 
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public abstract void drawFilledOval(float x, float y, float w, float h);

    /**
     *
     * @param rect
     */
    public final void drawFilledOval(GRectangle rect) {
        drawFilledOval(rect.getLeft(), rect.getTop(), rect.getWidth(), rect.getHeight());
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
    public void drawFilledCircle(float x, float y, float r) {
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
     * Applies Porter-Duff SRC_OVER to drawImage calls.
     * Result is image has incoming transparency applied.
     *
     * @param alpha
     */
    public abstract void setTransparencyFilter(float alpha);

    /**
     * Tint will replace incoming color (usually white) with the out going color on the next render
     *
     * @param inColor
     * @param outColor
     */
    public abstract void setTintFilter(GColor inColor, GColor outColor);

    /**
     *
     */
    public abstract void removeFilter();
    
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
        drawImage(imageKey, new Vector2D(x, y), new Vector2D(x + w, y + h));
    }

    /**
     * Draw image using the current transform
     *
     * @param imageKey
     */
    public abstract void drawImage(int imageKey);

    private final MutableVector2D working[] = {
            new MutableVector2D(),
            new MutableVector2D(),
            new MutableVector2D(),
            new MutableVector2D(),
    };


    /**
     * @param imageKey
     * @param rect0
     * @param rect1
     */
    public final void drawImage(int imageKey, IVector2D rect0, IVector2D rect1) {

        MutableVector2D v0 = transform(working[0].assign(rect0));
        MutableVector2D v1 = transform(working[1].assign(rect1));

        Vector2D minV = v0.min(v1, working[2]);
        Vector2D maxV = v0.max(v1, working[3]);

        drawImage(imageKey, minV.Xi(), minV.Yi(), maxV.Xi() - minV.Xi(), maxV.Yi() - minV.Yi());
    }

    /**
     *
     * @param imageKey
     * @param rect
     */
    public final void drawImage(int imageKey, IRectangle rect) {
        drawImage(imageKey, rect.getLeft(), rect.getTop(), rect.getWidth(), rect.getHeight());
    }

    /**
     *
     * @param imageKey
     * @param center
     * @param dimension
     */
    public final void drawImage(int imageKey, IVector2D center, GDimension dimension) {
        drawImage(imageKey, center.getX() - dimension.getWidth() / 2, center.getY() - dimension.getHeight() / 2, dimension.getWidth(), dimension.getHeight());
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
     * Draw image aligned to some point
     * @param imageKey
     * @param pos
     * @param hJust
     * @param vJust
     */
    public final void drawImage(int imageKey, IVector2D pos, Justify hJust, Justify vJust, float scale) {
        AImage img = getImage(imageKey);
        if (img != null) {
            float hgt = scale * img.getHeight();
            float wid = scale * img.getWidth();
            float x=pos.getX(),y=pos.getY();
            switch (hJust) {
                case CENTER:
                    x-=wid/2; break;
                case RIGHT:
                    x-=wid; break;
            }
            switch (vJust) {
                case CENTER:
                    y -= hgt / 2;
                    break;
                case BOTTOM:
                    y -= hgt;
                    break;
            }
            drawImage(imageKey, x, y, wid, hgt);
        }
    }

    /**
     * Draw image justified to pos
     *
     * @param imageKey
     * @param pos
     * @param width
     * @param height
     * @param hJust
     * @param vJust
     */
    public final void drawImage(int imageKey, IVector2D pos, float width, float height, Justify hJust, Justify vJust) {
        float x = pos.getX();
        float y = pos.getY();
        switch (hJust) {
            case CENTER:
                x -= width / 2;
                break;
            case RIGHT:
                x -= width;
                break;
        }
        switch (vJust) {
            case CENTER:
                y -= height / 2;
                break;
            case BOTTOM:
                y -= height;
                break;
        }
        drawImage(imageKey, x, y, width, height);
    }

    /**
     * Draw image centered at pos
     *
     * @param imageKey
     * @param pos
     * @param width
     * @param height
     */
    public final void drawImage(int imageKey, IVector2D pos, float width, float height) {
        drawImage(imageKey, pos.getX() - width / 2, pos.getY() - height / 2, width, height);
    }

    /**
     * @param color
     */
    public abstract void clearScreen(GColor color);

    /**
     *
     */
    public void clearScreen() {
        clearScreen(getBackgroundColor());
    }

    /**
     *
     * @return
     */
    public abstract GColor getBackgroundColor();

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
     *
     * @param rect
     */
    public final void ortho(IRectangle rect) {
        ortho(rect.getLeft(), rect.getLeft() + rect.getWidth(), rect.getTop(), rect.getTop() + rect.getHeight());
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
	public final void setClipRect(IRectangle rect) {
        setClipRect(rect.getLeft(), rect.getTop(), rect.getWidth(), rect.getTop());
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
