package cc.lib.swing;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;

/**
 * Created by chriscaron on 3/15/18.
 */

public class AWTGraphics2 extends AWTGraphics {

    private BasicStroke stroke = new BasicStroke(2f);
    private BasicStroke dashedStroke = null;
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

    @Override
    public void setComposite(Composite comp) {
        if (old == null)
            old = G2.getComposite();
        G2.setComposite(comp);
    }

    @Override
    public void removeFilter() {
        super.removeFilter();
        if (old != null) {
            G2.setComposite(old);
            old = null;
        }
    }

    @Override
    protected void drawImage(int imageKey, int x, int y, int w, int h) {
        //super.drawImage(imageKey, x, y, w, h);
        Image img = images.getImage(imageKey);
        float xScale = (float)w/img.getWidth(comp);
        float yScale = (float)h/img.getHeight(comp);
        AffineTransform t = new AffineTransform();
        t.translate(x, y);
        t.scale(xScale, yScale);
//        t.translate(-w/2, -h/2);
        G2.drawImage(img, t, comp);
    }

    @Override
    public void drawImage(int imageKey) {
        double [][] M = R.getCurrentTransform().get();
        AffineTransform t = new AffineTransform(M[0][0], M[1][0]
                ,M[0][1], M[1][1]
                ,M[0][2], M[1][2]);
        Image img = images.getImage(imageKey);
        G2.drawImage(img, t, comp);
    }

    /*
    @Override
    public void setClipRect(float x, float y, float w, float h) {
        Rectangle rect = new Rectangle(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
        G2.setClip(rect);
        G2.clip(rect);
    }

    @Override
    public void clearClip() {
        G2.clip(null);
    }*/

    @Override
    public void drawDashedLine(float x0, float y0, float x1, float y1, float thickness, float dashLength) {
        if (dashLength < 1)
            throw new IllegalArgumentException("Invalid dashLength: " + dashLength);
        if (dashedStroke == null || dashedStroke.getDashArray()[0] != dashLength || dashedStroke.getLineWidth() != thickness) {
            dashedStroke = new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    0, new float[]{dashLength, dashLength*2}, 0);
        }
        Stroke prev = G2.getStroke();
        G2.setStroke(dashedStroke);
        try {
            float [] v = { 0,0 };
            transform(x0, y0, v);
            int xi0 = Math.round(v[0]);
            int yi0 = Math.round(v[1]);
            transform(x1, y1, v);
            int xi1 = Math.round(v[0]);
            int yi1 = Math.round(v[1]);
            G2.drawLine(xi0, yi0, xi1, yi1);
        } finally {
            G2.setStroke(prev);
        }
    }
}
