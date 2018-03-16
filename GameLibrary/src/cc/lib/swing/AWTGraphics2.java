package cc.lib.swing;

import java.awt.*;

import cc.lib.game.GColor;

/**
 * Created by chriscaron on 3/15/18.
 */

public class AWTGraphics2 extends AWTGraphics {

    private BasicStroke stroke = new BasicStroke(2f);
    private Graphics2D G2;

    public AWTGraphics2(Graphics2D g, Component comp) {
        super(g, comp);
        G2 = g;
        G2.setStroke(stroke);
    }

    public void setGraphics(Graphics g) {
        super.setGraphics(g);
        this.G2 = (Graphics2D)g;
    }


    @Override
    public float setLineWidth(float newWidth) {
        float old = stroke.getLineWidth();
        stroke = new BasicStroke(newWidth);
        G2.setStroke(stroke);
        return old;
    }

    @Override
    protected void pdrawLine(float x0, float y0, float x1, float y1, float width) {
        BasicStroke s = new BasicStroke(width);
        G2.setStroke(s);
        getGraphics().drawLine(Math.round(x0), Math.round(y0), Math.round(x1), Math.round(y1));
        G2.setStroke(stroke);
    }

    @Override
    public void drawLineStrip() {
        int n = getPolyPts();
        G2.drawPolyline(x, y, n);
    }

    @Override
    public void drawLineLoop() {
        int n = getPolyPts();
        G2.drawPolygon(x, y, n);
    }
/*
    @Override
    public void setColor(GColor color) {
        Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        G2.setPaint(c);
        G2.setColor(c);
    }

/*    @Override
    public GColor getColor() {
        Color c = (Color)G2.getPaint();
        return new GColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }
*/
}
