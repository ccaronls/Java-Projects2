package cc.lib.swing;

import java.io.File;
import java.util.List;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.util.*;

import cc.lib.game.GColor;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class AWTUtils {
	
	public final static Color TRANSPARENT = new Color(0,0,0,0);
	
	public final static Color TRANSLUSCENT_BLACK = setAlpha(Color.BLACK, 128);
	
    /**
     * 
     * @param r0
     * @param r1
     * @return
     */
    public static boolean isBoxesOverlapping(Rectangle r0, Rectangle r1) {
        return Utils.isBoxesOverlapping(r0.x, r0.y, r0.width, r0.height, r1.x, r1.y, r1.width, r1.height);
    }
    
    /**
     * Return a Rectangle with the correct x,y,w,h params given the input points
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @return
     */
    public static Rectangle getBoundingRect(int x0, int y0, int x1, int y1) {
        Rectangle result = new Rectangle();
        getBoundingRect(x0, y0, x1, y1, result);
        return result;
    }
    
    /**
     * Version takes Rectangle as a param too avoid new operation
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param result
     */
    public static void getBoundingRect(int x0, int y0, int x1, int y1, Rectangle result) {
        result.x = Math.min(x0,x1);
        result.y = Math.min(y0,y1);
        result.width = Math.abs(x0-x1);
        result.height = Math.abs(y0-y1);
    }

    /**
     * 
     * @param g
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     */
    public static void drawTriangle(Graphics g, int x0, int y0, int x1, int y1, int x2, int y2) {
        int [] x_pts = { x0, x1, x2 };
        int [] y_pts = { y0, y1, y2 }; 
        g.drawPolygon(x_pts, y_pts, 3);
    }

    /**
     * 
     * @param g
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     */
    public static void drawTrianglef(Graphics g, float x0, float y0, float x1, float y1, float x2, float y2) {
        int [] x_pts = { Math.round(x0), Math.round(x1), Math.round(x2) };
        int [] y_pts = { Math.round(y0), Math.round(y1), Math.round(y2) }; 
        g.drawPolygon(x_pts, y_pts, 3);
    }

    /**
     * 
     * @param g
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     */
    public static void fillTriangle(Graphics g, int x0, int y0, int x1, int y1, int x2, int y2) {
        int [] x_pts = { x0, x1, x2 };
        int [] y_pts = { y0, y1, y2 }; 
        g.fillPolygon(x_pts, y_pts, 3);
    }

    /**
     * 
     * @param g
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     */
    public static void fillTrianglef(Graphics g, float  x0, float  y0, float  x1, float  y1, float  x2, float  y2) {
        int [] x_pts = { Math.round(x0), Math.round(x1), Math.round(x2) };
        int [] y_pts = { Math.round(y0), Math.round(y1), Math.round(y2) }; 
        g.fillPolygon(x_pts, y_pts, 3);
    }   
    
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
    public static int drawJustifiedOutlinedString(Graphics g, int x, int y, Justify hJust, Justify vJust, String text, Color outlineColor, int outlineThickness) {
        if (text.length() == 0)
            return 0;
        String [] lines = text.split("\n");
        final int textHeight = g.getFontMetrics().getHeight();
        switch (vJust) {
        case TOP: 
            break;
        case CENTER: 
            y -= (lines.length * (textHeight+textHeight/3)) / 2;
            break;
        case BOTTOM: 
            y -= (lines.length+1) * textHeight; 
            break;
        default:
            Utils.unhandledCase(vJust.ordinal());
            break;
        }
        for (int i=0; i<lines.length; i++) {
            y += textHeight;
            priv_drawJustifiedString(g, x, y, hJust, lines[i], outlineColor, outlineThickness);
        }
        return lines.length * textHeight;
    }
    
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
    public static Rectangle drawJustifiedString(Graphics g, int x, int y, Justify hJust, Justify vJust, String text) {
        if (text.length() == 0)
            return new Rectangle(0,0);
        String [] lines = text.split("\n");
        return drawJustifiedStringLines(g, x, y, hJust, vJust, lines);
    }
    
    /**
     * 
     * @param g
     * @param x
     * @param y
     * @param hJust
     * @param vJust
     * @param lines
     * @return the bounding rectangle of the text
     */
    public static Rectangle drawJustifiedStringLines(Graphics g, int x, int y, Justify hJust, Justify vJust, String [] lines) {
        final int textHeight = g.getFontMetrics().getHeight();
        // adjust for natural bottom alignment of text
        //y += textHeight; <-- TODO: Figure out why this line shift text down on SOC but is correct for FractalViewer
        int width = 0;
        switch (vJust) {
        case TOP:
        	//y += textHeight; <-- this looks wrong on SOC
            break;
        case CENTER: 
            y -= (lines.length * (textHeight+textHeight/3)) / 2;
            break;
        case BOTTOM: 
            y -= (lines.length * textHeight); 
            break;
        default:
            Utils.unhandledCase(vJust.ordinal());
            break;
        }
        for (int i=0; i<lines.length; i++) {
            y += textHeight;
            width = Math.max(width, priv_drawJustifiedString(g, x, y, hJust, lines[i], null, 0));
        }
        return new Rectangle(width, lines.length * textHeight);
    }
    
    /*
     * Draw a hJust/TOP justified line of text (no wrapping)
     * @param g
     * @param x
     * @param y
     * @param hJust
     * @param text
     * @return width of the rendered string
     */
    private static int priv_drawJustifiedString(Graphics g, int x, int y, Justify hJust, String text, Color outlineColor, int outlineThickness) {
        int x0 = x;
        final int textWidth = getStringWidth(g, text);
        switch (hJust) {
        case LEFT: 
            break;
        case CENTER: 
            x0 = x - textWidth/2; 
            break;
        case RIGHT: 
            x0 = x - textWidth; 
            break;
        default:
            Utils.unhandledCase(hJust.ordinal());
            break;
        }
        
        if (outlineColor != null && outlineThickness > 0) {
        	Color save = g.getColor();
        	g.setColor(outlineColor);
        	drawOutlinedText(g, x0-1, y-1, text, outlineThickness+1);
        	g.setColor(save);
        } 
        g.drawString(text, x0, y);
        return textWidth;
    }
    
    /**
     * This will draw just the outlined text in the current color.
     * 
     * @param g
     * @param x
     * @param y
     * @param text
     * @param outlineThickness
     */
    public static void drawOutlinedText(Graphics g, int x, int y, String text, int outlineThickness) {
    	Graphics2D g2d = ((Graphics2D)g);
    	GlyphVector gv = g.getFont().createGlyphVector(g2d.getFontRenderContext(), text);
    	Shape shape = gv.getOutline();
    	g2d.setStroke(new BasicStroke(outlineThickness));
    	g2d.translate(x, y);
    	g2d.draw(shape);
    	g2d.translate(-x,  -y);
    }
    
    /**
     * 
     * @param g
     * @param x
     * @param y
     * @param maxWidth
     * @param text
     * @return
     *
    public static int drawWrapString(Graphics g, int x, int y, int maxWidth, String text) {
        int endl = text.indexOf('\n');
        if (endl >= 0) {
            String t = text.substring(0, endl);
            int width = getStringWidth(g, t);
            if (width <= maxWidth) {
                g.drawString(t, x, y);
                return t.length();
            }
        }

        // cant find an endl, see if text fits
        int width = getStringWidth(g, text);
        if (width <= maxWidth) {
            g.drawString(text, x, y);
            return text.length();
        }

        // try to find a space to break on
        String t = text;
        int spc = -1;
        while (width > maxWidth) {
            spc = text.lastIndexOf(' ');
            if (spc >= 0) {
                t = text.substring(0, spc).trim();
                width = getStringWidth(g, t);
            } else {
                spc = -1;
                break;
            }
        }
        
        if (spc >= 0) {
            // found a space!
            g.drawString(t, x, y);
            return t.length();
        }
        
        // made it here means we have to wrap on a whole word!
        t = split(g, text, 0, text.length(), maxWidth);
        g.drawString(t, x, y);
        return t.length();
    }
    
    /**
     * Draw top/left justified wrapped lines with each line no longer than maxWidth pixels.  
     * Lines are wrapped on a newline first, then a space, then hyphenated as a last resort.
     * @param g
     * @param x
     * @param y
     * @param maxWidth
     * @param text
     * @return enclosing rectangle of text (x/y/w/h)
     */
    public static Rectangle drawWrapString(Graphics g, int x, int y, int maxWidth, String text) {
    	return drawWrapJustifiedString(g, x, y, maxWidth, Justify.LEFT, text);
    }

    /**
     * draw and wrap a string with both h/v justification.  Return enclosing rectangle.
     * @param g
     * @param x
     * @param y
     * @param maxWidth
     * @param hJust
     * @param vJust
     * @param text
     * @return
     */
    public static Rectangle drawWrapJustifiedString(Graphics g, int x, int y, int maxWidth, Justify hJust, Justify vJust, String text) {
    	
    	if (maxWidth < 0) {
    		return drawJustifiedString(g, x, y, hJust, vJust, text);
    	}
    	
        String [] lines = generateWrappedLines(g, text, maxWidth);
    	switch (vJust) {
    		case TOP:
    			return priv_drawWrapJustifiedString(g, x, y, maxWidth, hJust, lines);
    		case CENTER:
    			return priv_drawWrapJustifiedString(g, x, y-(lines.length*getFontHeight(g))/2, maxWidth, hJust, lines);
    		case BOTTOM:
    			return priv_drawWrapJustifiedString(g, x, y-(lines.length*getFontHeight(g)), maxWidth, hJust, lines);
    		default:
    			throw new AssertionError("Invalid vJust parameter '" + vJust + "'");
    	}
    }
    
    /**
     * Draw top justified with user provide horizontal justified string no wider than maxWidth pixels wide.
     * @param g
     * @param x
     * @param y
     * @param maxWidth
     * @param hJust
     * @param text
     * @return
     */
    public static Rectangle drawWrapJustifiedString(Graphics g, int x, int y, int maxWidth, Justify hJust, String text) {
    	String [] lines = generateWrappedLines(g, text, maxWidth);
    	return priv_drawWrapJustifiedString(g, x, y, maxWidth, hJust, lines);
    }
    
    private static Rectangle priv_drawWrapJustifiedString(Graphics g, int x, int y, int maxWidth, Justify hJust, String [] lines) {
        int tw = 0;
        final int sy = y;
        int th = 0 ;
        final int fh = getFontHeight(g);
        for (int i=0; i<lines.length; i++) {
        	int width = priv_drawJustifiedString(g, x, y+fh, hJust, lines[i], null, 0);
        	if (width > tw)
        		tw = width;
        	y += fh;
        	th += fh;
        }

        y = y+fh-g.getFontMetrics().getMaxAscent();
        th += g.getFontMetrics().getMaxDescent();
        
        Rectangle r = null;
        switch (hJust) {
			case CENTER:
				r = new Rectangle(x-tw/2, sy, tw, th);
				break;
			case LEFT:
				r = new Rectangle(x, sy, tw, th);
				break;
			case RIGHT:
				r = new Rectangle(x-tw, sy, tw, th);
				break;
			default:
				throw new AssertionError("Invalid jJust parameter '" + hJust + "'");
        	
        }

        // TODO: remove debug rect
        //if (Utils.DEBUG_ENABLED)
        //	g.drawRect(r.x, r.y, r.width, r.height);
        
        return r;
    }
    
    /**
     * 
     * @param g
     * @param str
     * @param maxWidth
     * @return
     */
    public static String [] generateWrappedLines(Graphics g, String str, int maxWidth) {
    	List<String> lines = new ArrayList<String>();
    	while (str.length() > 0) {
    		int endl = str.indexOf('\n');
    		if (endl >= 0) {
    			wrapString(g, str.substring(0, endl), lines, maxWidth);
    			if (endl >= str.length())
    				break;
    			str = str.substring(endl+1, str.length());
    		} else {
    			wrapString(g, str, lines, maxWidth);
    			break;
    		}
    	}
    	return lines.toArray(new String[lines.size()]);
    }
    
    private static void wrapString(Graphics g, String str, List<String> target, int maxWidth) {
    	
		int width = getStringWidth(g, str);
		while (width > maxWidth) {
			int spc = findSpaceToWrapString(g, str, maxWidth);
			if (spc < 0) {
    			String s = split(g, str, 0, str.length(), maxWidth);
    			target.add(s);
    			str = str.substring(s.length());
			} else {
				target.add(str.substring(0, spc));
				if (spc >= str.length()-1)
					return;
				str = str.substring(spc+1);
			}
    		width = getStringWidth(g, str);
		}
		if (str.length() > 0)
			target.add(str);
    }
    
    private static int findSpaceToWrapString(Graphics g, String str, int maxWidth) {
    	int spc = str.indexOf(' ');
    	if (spc < 0)
    		return -1;
    	int width = getStringWidth(g, str.substring(0, spc));
    	if (width > maxWidth)
    		return -1;
    	while (true) {
    		int spc2 = str.indexOf(' ', spc+1);
    		if (spc2 < 0)
    			return spc;
    		width = getStringWidth(g, str.substring(0, spc2));
    		if (width < maxWidth)
    			spc = spc2;
    		else
    			break;
    	}
    	return spc;
    }
    
    /*
        String text = str.trim();
        List<String> lines = new ArrayList<String>(32);
        while (text.length() > 0) {
            int endl = text.indexOf('\n');
            if (endl >= 0) {
                String t = text.substring(0, endl).trim();
                int width = getStringWidth(g, t);
                if (width <= maxWidth) {
                    lines.add(t);
                    text = text.substring(endl+1);
                    continue;
                }
            }
            
            // cant find an endl, see if text fits
            int width = getStringWidth(g, text);
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
                    width = getStringWidth(g, t);
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
            if (text.length() <= 2) {
            	lines.add(t);
            	break;
            }
            try {
            	t = splitR(g, text, 0, text.length(), maxWidth, 0);
                lines.add(t);
                text = text.substring(t.length()).trim();
            } catch (Exception e) {
            	System.err.println("Problem splitting line '" + text + "'");
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
    public static String split(Graphics g, String s, int start, int end, int maxWidth) {
    	return splitR(g, s, start, end, maxWidth, 0);
    }
    
    
    private static String splitR(Graphics g, String s, int start, int end, int maxWidth, int depth) {
    	if (depth > 128) {
    		System.err.println("Unable to split string '" + s + "'");
    		return s;
    	}
    	if (start >= end-1)
    		return s;
    	if (maxWidth <= 2)
    		return "";
    	int mid = (start+end)/2;
    	String r = s.substring(start, mid);
    	if (r.length() <= 1)
    		return r;
    	int wid = getStringWidth(g, r);
    	if (wid > maxWidth) {
    		return splitR(g, r, 0, mid, maxWidth, depth+1);
    	}
    	return r + splitR(g, s.substring(mid, end), 0, end-mid, maxWidth-wid, depth+1);
    }
    
    /**
     * 
     * @param g
     * @param text
     * @return
     */
    public static int getStringWidth(Graphics g, String text) {
    	if (g == null)
    		return text.length();
        return g.getFontMetrics().stringWidth(text) + 2; // https://docs.oracle.com/javase/tutorial/2d/text/measuringtext.html
        //(int)(0.5 + g.getFontMetrics().getStringBounds(text.toCharArray(), 0, text.length(), g).getWidth());
        		//.stringWidth(text);
    }
    
    /**
     * 
     * @param g
     * @return
     */
    public static int getFontHeight(Graphics g) {
        return g.getFontMetrics().getHeight();
    }
    
    /**
     * 
     * @return
     */
    public static Font [] getAllFonts() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
    }
    
    /**
     * 
     * @return
     */
    public static String [] getAllFontFamilyNames() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    }
    
    /**
     * 
     * @param g
     * @param name
     * @param height
     * @param width
     * @param bold
     * @param italic
     * @param plain
     * @return
     */
    public static Font deriveFont(Graphics g, String name, float height, float width, boolean bold, boolean italic, boolean plain) {
        Font [] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        if (fonts == null || fonts.length == 0)
            return null;
        Font best = null;
        
        for (int i=0; i<fonts.length; i++) {
            Font t = fonts[i];
            if (t.getName().toLowerCase().startsWith(name.toLowerCase())) {
                if (best == null) {
                    best = t;
                }
                if (t.isItalic()) {
                    if (!italic)
                        continue;
                } else {
                    if (italic)
                        continue;
                }
                // test for bold
                if (t.isBold()) {
                    if (!bold)
                        continue;               
                } else {
                    if (bold)
                        continue;
                }
                // test for underlined
                if (t.isPlain()) {
                    if (!plain)
                        continue;
                } else {
                    if (plain)
                        continue;
                }
                best = t;
            }
        }
        
        return best;
    }
    
    /**
     * Detemine the minimum rectangle to hold the given text.
     * \n is a delim for each line.
     * @param g
     * @param txt
     * @return
     */
    public static Dimension computeTextDimension(Graphics g, String txt) {
        String [] lines = txt.split("\n");
        int width = 0;
        final int height = g.getFontMetrics().getHeight() * lines.length;
        for (int i=0; i<lines.length; i++) {
            int w = getStringWidth(g, lines[i]);
            if (w > width)
                width = w;
        }
        return new Dimension(width, height);
    }
    
    /**
     * 
     * @param g
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param x3
     * @param y3
     */
    public static void fillQuad(Graphics g, int x0, int y0, int x1, int y1, int x2, int y2, int x3, int y3) {
        int [] x = { x0, x1, x2, x3 };
        int [] y = { y0, y1, y2, y3 };
        g.fillPolygon(x,y,4);
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
    public static void drawLine(Graphics g, int x0, int y0, int x1, int y1, int thickness) {
        if (thickness < 2) {
            g.drawLine(x0, y0, x1, y1);
        } else {
            drawThickLine(g, x0, y0, x1, y1, thickness);
        }       
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
    public static void drawLine(Graphics g, float x0, float y0, float x1, float y1, int thickness) {
        int ix0 = Math.round(x0);
        int iy0 = Math.round(y0);
        int ix1 = Math.round(x1);
        int iy1 = Math.round(y1);
        
        drawLine(g, ix0, iy0, ix1, iy1, thickness);
    }
    
    /**
     * 
     * @param g
     * @param cx
     * @param cy
     * @param radius
     */
    public static void drawDisk(Graphics g, float cx, float cy, float radius) {
        int x0 = Math.round(cx-radius);
        int y0 = Math.round(cy-radius);
        int r =  Math.round(radius*2);
        g.fillOval(x0,y0,r,r);
    }
    
    private static void drawThickLine(Graphics g, int x1, int y1, int x2, int y2, int thickness) {
        int dX = x2 - x1;
        int dY = y2 - y1;
        // line length
        double lineLength = Math.sqrt(dX * dX + dY * dY);

        double scale = (double)(thickness) / (2 * lineLength);

        // The x,y increments from an endpoint needed to create a rectangle...
        double ddx = -scale * (double)dY;
        double ddy = scale * (double)dX;
        ddx += (ddx > 0) ? 0.5 : -0.5;
        ddy += (ddy > 0) ? 0.5 : -0.5;
        int dx = (int)Math.round(ddx);
        int dy = (int)Math.round(ddy);

        // Now we can compute the corner points...
        int xPoints[] = new int[4];
        int yPoints[] = new int[4];

        xPoints[0] = x1 + dx; yPoints[0] = y1 + dy;
        xPoints[1] = x1 - dx; yPoints[1] = y1 - dy;
        xPoints[2] = x2 - dx; yPoints[2] = y2 - dy;
        xPoints[3] = x2 + dx; yPoints[3] = y2 + dy;

        g.fillPolygon(xPoints, yPoints, 4);     
    }

    /**
     * 
     * @param g
     * @param points
     * @param thickness
     */
    public static void drawLineStrip(Graphics g, int [] x_pts, int [] y_pts, int thickness) {
        assert(x_pts.length == y_pts.length);
        for (int i=0; i<x_pts.length-1; i++) {
            drawLine(g, x_pts[i], y_pts[i], x_pts[i+1], y_pts[i+1], thickness);
        }
    }
    
    /**
     * 
     * @param g
     * @param x
     * @param y
     * @param r
     */
    public static void fillCircle(Graphics g, int x, int y, int r) {
        int r2 = r>>1;
        g.fillOval(x-r2, y-r2, r, r);
    }

    /**
     * 
     * @param g
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param thickness
     * @param borderColor
     */
    public static void drawBorderedLine(Graphics g, int x0, int y0, int x1, int y1, int thickness, Color borderColor) {
        Color curColor = g.getColor();
        g.setColor(borderColor);
        drawLine(g,x0,y0,x1,y1,thickness);
        g.setColor(curColor);
        drawLine(g,x0,y0,x1,y1,thickness-1);
    }

    /**
     * 
     * @param g
     * @param x
     * @param y
     * @param w
     * @param h
     * @param thickness
     */
    public static void drawQuad(Graphics g, int x, int y, int w, int h, int thickness) {
        drawLine(g, x, y, x+w, y, thickness);
        drawLine(g, x+w, y, x+w, y+h, thickness);
        drawLine(g, x, y+h, x+w, y+h, thickness);
        drawLine(g, x, y, x, y+h, thickness);
    }
    
    /**
     * 
     * @param g
     * @param x
     * @param y
     * @param w
     * @param h
     * @param thickness
     */
    public static void drawRect(Graphics g, int x, int y, int w, int h, int innerThickness, int outerThickness) {
    	final int thickness = innerThickness + outerThickness;
        if (thickness > 1) {
        	g.fillRect(x-outerThickness, y-outerThickness, w+thickness, thickness); // top
        	g.fillRect(x-outerThickness, y-outerThickness, thickness, h+thickness); // left
        	g.fillRect(x+w-innerThickness, y-outerThickness, thickness, h+thickness); // right
        	g.fillRect(x-outerThickness, y+h-innerThickness, w+thickness, thickness); // bottom
            //drawLine(g, x+0, y+0, x+w, y+0, thickness);
            //drawLine(g, x+0, y+0, x+0, y+h, thickness);
            //drawLine(g, x+w, y+0, x+w, y+h, thickness);
            //drawLine(g, x+0, y+h, x+w, y+h, thickness);
        } else {
            g.drawRect(x, y, w, h);
        }
    }
    
    /**
     * convenience takes rectangle argument 
     * @param g
     * @param r
     * @param innerThickness
     * @param outerThickness
     */
    public static void drawRect(Graphics g, Rectangle r, int innerThickness, int outerThickness) {
    	drawRect(g, r.x, r.y, r.width, r.height, innerThickness, outerThickness);
    }

    /**
     * 
     * @param color
     * @return
     */
    public static int colorToInt(Color color) {
        int a = color.getAlpha();
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int d = (a << 24) | (r << 16) | (g << 8) | (b << 0);
        return d;
    }
    
    /**
     * 
     * @param c
     * @return
     */
    public static Color intToColor(int c) {
        int a = (c&0xff000000)>>24;
        int r = (c&0x00ff0000)>>16;
        int g = (c&0x0000ff00)>>8;
        int b = (c&0x000000ff)>>0;
        return new Color(r,g,b,a);
    }

    /**
     * 
     * @param color
     * @param i
     * @return
     */
    public static Color setAlpha(Color color, int i) {
    	if (color.getAlpha() == i)
    		return color;
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), i);
    }

    /**
     * 
     * @param color
     * @param amount
     * @return
     */
    public static Color darken(Color color, float amount) {
        int R = Math.round(amount * color.getRed());
        int G = Math.round(amount * color.getGreen());
        int B = Math.round(amount * color.getBlue());
        R = (int)Utils.clamp(color.getRed() - R, 0, 255);
        G = (int)Utils.clamp(color.getGreen() - G, 0, 255);
        B = (int)Utils.clamp(color.getBlue() - B, 0, 255);
        return new Color(R,G,B,color.getAlpha());       
    }

    /**
     * 
     * @param color
     * @param amount
     * @return
     */
    public static Color lighten(Color color, float amount) {
        int R = Math.round(amount * color.getRed());
        int G = Math.round(amount * color.getGreen());
        int B = Math.round(amount * color.getBlue());
        R = Utils.clamp(color.getRed() + R, 0, 255);
        G = Utils.clamp(color.getGreen() + G, 0, 255);
        B = Utils.clamp(color.getBlue() + B, 0, 255);
        return new Color(R,G,B,color.getAlpha());       
    }
    
    /**
     * Return a when aWeight == 1.0
     * Return board when aWeight == 0.0
     * @param a
     * @param b
     * @param aWeight
     * @return
     */
    public static Color interpolate(Color a, Color b, float aWeight) {
        if (aWeight > 0.99)
            return a;
        if (aWeight < 0.01)
            return b;
        float bWeight = 1.0f - aWeight;
        int newAlpha = Math.round(aWeight * a.getAlpha() + bWeight * b.getAlpha());
        int newRed   = Math.round(aWeight * a.getRed() + bWeight * b.getRed());
        int newGreen = Math.round(aWeight * a.getGreen() + bWeight * b.getGreen());
        int newBlue  = Math.round(aWeight * a.getBlue() + bWeight * b.getBlue());
        return new Color(newRed,  newGreen, newBlue, newAlpha);
    }    
    
    /**
     * 
     * @param line
     * @return
     */
    public static Color stringToColor(String line) {
		String [] parts = line.split(",");
		if (parts.length == 3)
			return new Color(Integer.parseInt(parts[0]),
						 Integer.parseInt(parts[1]), 
						 Integer.parseInt(parts[2]));
		else 
			return new Color(Integer.parseInt(parts[0]),
					 Integer.parseInt(parts[1]), 
					 Integer.parseInt(parts[2]),
					 Integer.parseInt(parts[3]));
	}

    /**
     * 
     * @param color
     * @return
     */
	public static String colorToString(Color color) {
	    return String.valueOf(color.getRed()) + "," + String.valueOf(color.getGreen()) + "," + String.valueOf(color.getBlue());
	}
	
	/**
	 * 
	 */
	public static final Reflector.AArchiver COLOR_ARCHIVER = new Reflector.AArchiver() {
		
		@Override
		public Object parse(String value) throws Exception {
			return stringToColor(value);
		}
		
		@Override
		public String getStringValue(Object obj) {
			return colorToString((Color)obj);
		}
	};

	/**
	 * 
	 * @param g
	 * @param rect
	 * @param border
	 */
	public static void fillRect(Graphics g, Rectangle rect, int border) {
		g.fillRect(rect.x-border, rect.y-border, rect.width+border*2, rect.height+border*2);
	}

	/**
	 * 
	 * @param x
	 * @param y
	 * @param r
	 * @return
	 */
	public static boolean isInsideRect(int x, int y, Rectangle r) {
		return x > r.x && x < r.x+r.width && y > r.y && y < r.y+r.height;
	}
	
	public static void drawWrapJustifiedStringOnBackground(Graphics g, int x, int y, int maxWidth, int padding, Justify hJust, Justify vJust, String txt, Color bkColor) {
		Color cur = g.getColor();
		g.setColor(AWTUtils.TRANSPARENT);
		Rectangle rect = drawWrapJustifiedString(g, x, y, maxWidth, hJust, vJust, txt);
		g.setColor(bkColor);
		AWTUtils.fillRect(g, rect, padding);
		g.setColor(cur);
		AWTUtils.drawWrapJustifiedString(g, x, y, maxWidth, hJust, vJust, txt);
	}
	
	public static Color addColors(Color a, Color b) {
		return new Color(Math.min(255, a.getRed() + b.getRed()),
				Math.min(255, a.getGreen() + b.getGreen()),
				Math.min(255, a.getBlue() + b.getBlue()),
				Math.min(255, a.getAlpha() + b.getAlpha()));
	}

    public static Color toColor(GColor c) {
	    return new Color(c.red(), c.green(), c.blue(), c.alpha());
    }

    public static File getOrCreateSettingsDirectory(Class<?> clazz) {
        File homeDir = new File(System.getProperty("user.home"));
        if (!homeDir.isDirectory()) {
            System.err.println("Failed to find users home dir: '" + homeDir);
            homeDir = new File(".");
        }
        String pkg = clazz.getCanonicalName().replace('.', '/');
        File settingsDir = new File(homeDir, "settings/" + pkg);
        if (!settingsDir.isDirectory()) {
            if (!settingsDir.mkdirs())
                throw new RuntimeException("Failed to create settings directory: " + settingsDir.getAbsolutePath());
            else
                System.out.println("Created settings directory: " + settingsDir.getAbsolutePath());
        }
        return settingsDir;
    }
}
