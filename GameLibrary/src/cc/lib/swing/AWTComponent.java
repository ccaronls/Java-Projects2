package cc.lib.swing;

import java.awt.*;
import java.awt.event.*;

import javax.swing.JComponent;

import cc.lib.game.*;
import cc.lib.swing.*;



/**
 * Created by chriscaron on 2/21/18.
 */

public abstract class AWTComponent extends JComponent implements Renderable, MouseListener, MouseMotionListener, MouseWheelListener {

    private AWTGraphics G = null;
    private int mouseX = -1;
    private int mouseY = -1;
    private boolean focused = false;
    private int padding = 5;

    public AWTComponent() {
    }

    public void setMouseEnabled(boolean enabled) {
        if (enabled) {
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
            setFocusable(true);
        } else {
            removeMouseListener(this);
            removeMouseMotionListener(this);
            removeMouseWheelListener(this);
            setFocusable(false);
            mouseX = mouseY = -1;
        }
    }

    @Override
    public final synchronized void paint(Graphics g) {
        if (getWidth() > 0 && getHeight() > 0) {
            g.translate(padding,padding);
            g.clearRect(0, 0, getWidth(), getHeight());
            g.setClip(0, 0, getWidth(), getHeight());
            if (G == null) {
                G = new AWTGraphics(g, this);
                init(G);
                repaint();
            } else {
                float progress = getInitProgress();
                G.setGraphics(g);
                G.initViewport(getWidth(), getHeight());
                G.ortho();
                if (progress >= 1) {
                    paint(G, mouseX, mouseY);
                } else {
                    Font f = g.getFont();
                    G.clearScreen(GColor.CYAN);
                    G.setColor(GColor.WHITE);
                    G.setTextHeight(getHeight()/10);
                    float x = getWidth()/2;
                    float y = getHeight()/3;
                    String txt = "INITIALIZING";
                    float tw = G.getTextWidth(txt);
                    float th = G.getTextHeight();
                    while (tw > getWidth() && G.getTextHeight() > 8) {
                        G.setTextHeight(G.getTextHeight()-2);
                    }
                    G.drawJustifiedString(x-tw/2, y,txt);
                    y += th;
                    G.drawFilledRectf(x-tw/2, y, tw*progress, th);
                    g.setFont(f);
                    repaint();
                }
            }
            if (focused) {
                System.out.println("AWT " + toString() + " has focus!");
                g.setColor(Color.BLUE);
                g.drawRect(0, 0, getWidth()-1, getHeight()-1);
            }
            g.translate(-padding,-padding);
            g.setClip(0, 0, getWidth()+2*padding, getHeight()+2*padding);
        } else {
            repaint();
        }
    }

    protected abstract void paint(AWTGraphics g, int mouseX, int mouseY);

    protected void init(AWTGraphics g) {
    }

    /**
     * Return value between 0-1 that is the progress of init flow
     * @return
     */
    protected float getInitProgress() {
        return 1;
    }

    @Override
    public final void mouseClicked(MouseEvent e) {
        //Utils.println("mouseClicked");
        onClick();
    }

    @Override
    public final synchronized void mousePressed(MouseEvent e) {
        //Utils.println("mousePressed");
        grabFocus();
        mouseX = e.getX();
        mouseY = e.getY();
        repaint();
    }

    @Override
    public final void mouseReleased(MouseEvent e) {
        //Utils.println("mouseReleased");
        if (dragging) {
            stopDrag();
            dragging = false;
        }
        mouseX = e.getX();
        mouseY = e.getY();
        repaint();
    }

    @Override
    public final void mouseEntered(MouseEvent e) {
//        grabFocus();
        focused = true;
        repaint();
    }

    @Override
    public final void mouseExited(MouseEvent e) {
        focused = false;
        repaint();
    }

    boolean dragging = false;

    @Override
    public final void mouseWheelMoved(MouseWheelEvent e) {
        onMouseWheel(e.getWheelRotation());
    }

    protected void onMouseWheel(int rotation) {}

    @Override
    public boolean lostFocus(java.awt.Event ev, Object obj) {
        repaint();
        return super.lostFocus(ev, obj);
    }

    @Override
    public final void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        if (!dragging) {
            startDrag(mouseX, mouseY);
            dragging = true;
        }
        //Utils.println("mouseDragged");
        repaint();
    }

    @Override
    public final void mouseMoved(MouseEvent e) {
        //Utils.println("mouseMoved");
        mouseX = e.getX();
        mouseY = e.getY();
        repaint();
    }

    protected void startDrag(int x, int y) {}

    protected void stopDrag() {}

    protected void onClick() {}

    public final void repaint() {
        super.repaint();
    }

    public final int getWidth() {
        return super.getWidth()-padding*2;
    }

    public final int getHeight() {
        return super.getHeight()-padding*2;
    }

    @Override
    public int getViewportWidth() {
        return getWidth();
    }

    @Override
    public int getViewportHeight() {
        return getHeight();
    }

    public void setMinimumSize(int width, int height) {
        super.setMinimumSize(new Dimension(width, height));
    }

    public void setPreferredSize(int w, int h) {
        super.setPreferredSize(new Dimension(w, h));
    }

    public final APGraphics getAPGraphics() {
        return G;
    }

    public final void setPadding(int padding) {
        this.padding = padding;
    }

}
