package cc.lib.swing;

import java.awt.*;
import java.awt.event.*;

import javax.swing.JComponent;

import cc.lib.game.*;
import cc.lib.swing.*;



/**
 * Created by chriscaron on 2/21/18.
 */

public abstract class AWTComponent extends JComponent implements Renderable, MouseListener, MouseMotionListener {

    AWTGraphics G = null;
    int mouseX, mouseY;

    public AWTComponent(boolean focusable) {
        addMouseListener(this);
        addMouseMotionListener(this);
        setFocusable(focusable);
    }

    @Override
    public final synchronized void paint(Graphics g) {
        if (G == null) {
            G = new AWTGraphics(g, this);
            G.setIdentity();
        } else {
            G.setGraphics(g);
            G.initViewport(getWidth(), getHeight());
        }
        paint(G, mouseX, mouseY);
    }

    protected abstract void paint(AWTGraphics g, int mouseX, int mouseY);

    @Override
    public final void mouseClicked(MouseEvent e) {
        Utils.println("mouseClicked");
        onClick();
    }

    @Override
    public final synchronized void mousePressed(MouseEvent e) {
        Utils.println("mousePressed");
        grabFocus();
        mouseX = e.getX();
        mouseY = e.getY();
        repaint();
    }

    @Override
    public final void mouseReleased(MouseEvent e) {
        Utils.println("mouseReleased");
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
    }

    @Override
    public final void mouseExited(MouseEvent e) {
    }

    boolean dragging = false;

    @Override
    public final void mouseDragged(MouseEvent e) {
        if (!dragging) {
            startDrag();
            dragging = true;
        }
        Utils.println("mouseDragged");
        mouseX = e.getX();
        mouseY = e.getY();
        repaint();
    }

    @Override
    public final void mouseMoved(MouseEvent e) {
        Utils.println("mouseMoved");
        mouseX = e.getX();
        mouseY = e.getY();
        repaint();
    }

    protected void startDrag() {}

    protected void stopDrag() {}

    protected void onClick() {}

    public final void repaint() {
        super.repaint();
    }

    public final int getWidth() {
        return super.getWidth();
    }

    public final int getHeight() {
        return super.getHeight();
    }

    @Override
    public Dimension getMinimumSize() {
        GRectangle rect = getMinRect();
        return new Dimension(rect.width, rect.height);
    }

    protected GRectangle getMinRect() {
        return new GRectangle(32, 32);
    }

    @Override
    public int getViewportWidth() {
        return getWidth();
    }

    @Override
    public int getViewportHeight() {
        return getHeight();
    }
}
