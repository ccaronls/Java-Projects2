package cc.lib.swing;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;

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

    public Graphics2D getGraphics2D() {
        return G2;
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
    public void drawLineStrip() {
        int n = getPolyPts();
        G2.drawPolyline(x, y, n);
    }

    @Override
    public void drawLineLoop() {
        int n = getPolyPts();
        G2.drawPolygon(x, y, n);
    }

    Composite old = null;

    public void setTransparentcyFilter(float alpha) {
        old = G2.getComposite();
        Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
        G2.setComposite(comp);
    }

    public void removeComposite() {
        if (old != null) {
            G2.setComposite(old);
            old = null;
        }
    }
}
