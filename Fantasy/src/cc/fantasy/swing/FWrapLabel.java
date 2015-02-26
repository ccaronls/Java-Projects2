package cc.fantasy.swing;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.SwingConstants;

public class FWrapLabel extends FLabel {

    String [] lines = new String[0];
    
    FWrapLabel(String txt) {
        this(txt, Justify.LEFT, Justify.TOP); 
    }
    
    FWrapLabel(String txt, Justify hJust, Justify vJust) {
        super(txt);
        this.setHorizontalTextPosition(hJust.getJustify());
        this.setVerticalTextPosition(vJust.getJustify());
        lines = txt.split("[\n]");
    }
    
    public FWrapLabel setJustify(Justify hJust, Justify vJust) {
        this.setHorizontalTextPosition(hJust.getJustify());
        this.setVerticalTextPosition(vJust.getJustify());
        return this;
    }

    public void setText(String txt) {
        super.setText(txt);
        lines = txt.split("[\n]");
    }
    
    public Dimension getPreferredDimension() {
        int width = 0;
        FontMetrics metrics = getFontMetrics(getFont());
        final int height = metrics.getHeight() * lines.length;
        for (int i=0; i<lines.length; i++) {
            int w = metrics.stringWidth(lines[i]);
            if (w > width)
                width = w;
        }
        return new Dimension(width, height);
    }
    
    public void paint(Graphics g) {
        Rectangle rect = g.getClipBounds();
        FontMetrics metrics = getFontMetrics(getFont());
        int hJust = this.getHorizontalTextPosition();
        int vJust = this.getVerticalTextPosition();
        int x = (int)Math.round(rect.getX());
        int y = (int)Math.round(rect.getY());
        int w = (int)Math.round(rect.getWidth());
        int h = (int)Math.round(rect.getHeight());
        int textHeight = metrics.getHeight(); 
        switch (vJust) {
        case SwingConstants.TOP: 
            break;
        case SwingConstants.CENTER: 
            y = y + h/2 - (lines.length * (textHeight+textHeight/3)) / 2; 
            break;
        case SwingConstants.BOTTOM: 
            y = y+h-lines.length * textHeight; 
            break;
        }
        for (int i=0; i<lines.length; i++) {
            drawJustifiedString(g, metrics, x, y, w, hJust, lines[i]);
            y += textHeight;
        }
    }
    
    private void drawJustifiedString(Graphics g, FontMetrics metrics, int x, int y, int w, int hJust, String text) {
        int x0 = x;
        final int textWidth = metrics.stringWidth(text);
        switch (hJust) {
        case SwingConstants.LEFT: 
            break;
        case SwingConstants.CENTER: 
            x0 = x0 + w/2 - textWidth/2; 
            break;
        case SwingConstants.RIGHT: 
            x0 = x0 + w - textWidth; 
            break;
        }
        g.drawString(text, x0, y);
    }
}
