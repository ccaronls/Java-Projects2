package cc.lib.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JComponent;

import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.IVector2D;
import cc.lib.game.Renderable;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.Vector2D;
import cc.lib.utils.GException;


/**
 * Created by chriscaron on 2/21/18.
 */

public abstract class AWTComponent extends JComponent implements Renderable, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private AWTGraphics G = null;
    private int mouseX = -1;
    private int mouseY = -1;
    private boolean focused = false;
    private int padding = 5;
    private int scrollAmount = -1;
    private int scrollStartY = 0;

    public AWTComponent() {
    }

    public AWTComponent(int width , int height) {
        setPreferredSize(width, height);
    }

    public void setMouseEnabled(boolean enabled) {
        if (enabled) {
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
            setFocusable(true);
            addKeyListener(this);
        } else {
            removeMouseListener(this);
            removeMouseMotionListener(this);
            removeMouseWheelListener(this);
            setFocusable(false);
            removeKeyListener(this);
            mouseX = mouseY = -1;
        }
    }

    @Override
    public synchronized void paint(Graphics g) {
        try {
            if (getWidth() > 0 && getHeight() > 0) {
                Color c = g.getColor();
                g.setColor(getBackground());//AWTUtils.toColor(GColor.TRANSPARENT));
                g.fillRect(0, 0, super.getWidth(), super.getHeight());
                g.setColor(c);
                //g.setClip(padding,padding, getWidth()+1, getHeight()+1);
                if (G == null) {
                    if (g instanceof Graphics2D)
                        G = new AWTGraphics2(((Graphics2D) g), this);
                    else
                        G = new AWTGraphics(g, this);
                    init(G);
                    repaint();
                } else {
                    float progress = getInitProgress();
                    G.setGraphics(g);
                    G.initViewport(getWidth(), getHeight());
                    G.ortho();
                    if (progress >= 1) {
                        int matStack = G.getMatrixStackSize();
                        if (scrollAmount < 0)
                            scrollAmount = (int)G.getTextHeight();
                        if (scrollStartY != 0) {
                            G.pushMatrix();
                            G.translate(0, scrollStartY);
                            paint(G, mouseX, mouseY);
                            G.popMatrix();
                        } else {
                            try {
                                paint(G, mouseX, mouseY);
                            } catch (Exception e) {
                                log.error("Error: %s", e);
                                e.printStackTrace();
                                throw new GException(e);
                            }
                        }
                        if (G.getMatrixStackSize() != matStack) {
                            throw new cc.lib.utils.GException("Matrix stack not zero");
                        }
                    } else {
                        Font f = g.getFont();
                        G.clearScreen(GColor.CYAN);
                        G.setColor(GColor.WHITE);
                        G.setTextHeight(getHeight() / 10);
                        float x = getWidth() / 2;
                        float y = getHeight() / 3;
                        String txt = "INITIALIZING";
                        float tw = G.getTextWidth(txt);
                        float th = G.getTextHeight();
                        while (tw > getWidth() && G.getTextHeight() > 8) {
                            G.setTextHeight(G.getTextHeight() - 2);
                        }
                        G.drawJustifiedString(x - tw / 2, y, txt);
                        y += th;
                        G.drawFilledRect(x - tw / 2, y, tw * progress, th);
                        G.setFont(f);
                        repaint();
                    }
                }
                //g.setClip(0, 0, super.getWidth(), super.getHeight());
                if (focused) {
                    //                System.out.println("AWT " + toString() + " has focus!");
                    g.setColor(Color.BLUE);
                    g.drawRect(0, 0, super.getWidth() - 2, super.getHeight() - 2);
                }
            } else {
                repaint();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw new GException(e);
        }
    }

    protected abstract void paint(AWTGraphics g, int mouseX, int mouseY);

    /**
     * Called on first call from paint
     * @param g
     */
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
    public final synchronized void mouseClicked(MouseEvent e) {
        //Utils.println("mouseClicked");
        onClick();
    }

    @Override
    public final synchronized void mousePressed(MouseEvent e) {
        //Utils.println("mousePressed");
        grabFocus();
        mouseX = e.getX()-padding;
        mouseY = e.getY()-padding;
        repaint();
        onMousePressed(mouseX, mouseY);
    }

    protected void onMousePressed(int mouseX, int mouseY) {}

    @Override
    public final synchronized void mouseReleased(MouseEvent e) {
        //Utils.println("mouseReleased");
        if (dragging) {
            onDragStopped();
            dragging = false;
        }
        mouseX = e.getX()-padding;
        mouseY = e.getY()-padding;
        repaint();
    }

    @Override
    public final synchronized void mouseEntered(MouseEvent e) {
//        grabFocus();
        focused = true;
        repaint();
        onFocusGained();
    }

    protected void onFocusGained() {}
    protected void onFocusLost() {}

    @Override
    public final synchronized void mouseExited(MouseEvent e) {
        focused = false;
        repaint();
        onFocusLost();
    }

    boolean dragging = false;

    @Override
    public final synchronized void mouseWheelMoved(MouseWheelEvent e) {
        onMouseWheel(e.getWheelRotation());
    }

    protected void onMouseWheel(int rotation) {
        Dimension d = getMinimumSize();
        int maxScroll = getHeight() - d.height;
        if (maxScroll < 0) {
            scrollStartY = Utils.clamp(scrollStartY - rotation * scrollAmount, maxScroll, 0);
            repaint();
        }
    }

    @Override
    public synchronized boolean lostFocus(java.awt.Event ev, Object obj) {
        repaint();
        return super.lostFocus(ev, obj);
    }

    @Override
    public final synchronized void mouseDragged(MouseEvent e) {
        mouseX = e.getX()-padding;
        mouseY = e.getY()-padding;
        if (!dragging) {
            onDragStarted(mouseX, mouseY);
            dragging = true;
        } else {
            onDrag(mouseX, mouseY);
        }
        //Utils.println("mouseDragged");
        repaint();
    }

    @Override
    public final synchronized void mouseMoved(MouseEvent e) {
        //log.info("mouse %d,%d", e.getX(), e.getY());
        //Utils.println("mouseMoved");
        mouseX = e.getX()-padding;
        mouseY = e.getY()-padding;
        onMouseMoved(mouseX, mouseY);
        repaint();
    }

    protected void onMouseMoved(int mouseX, int mouseY) {

    }

    @Override
    public synchronized void keyTyped(KeyEvent evt) {
    }

    @Override
    public synchronized void keyPressed(KeyEvent evt) {
    }

    @Override
    public synchronized void keyReleased(KeyEvent evt) {
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    protected void onDragStarted(int x, int y) {}

    protected void onDragStopped() {}

    protected void onDrag(int x, int y) {}

    protected void onClick() {}

    public final int getX() {
        return super.getX() + padding;
    }

    public final int getY() {
        return super.getY() + padding;
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

    public void setMinimumSize(int w, int h) {
        log.debug("set min size: %d x %d", w, h);
        super.setMinimumSize(new Dimension(w, h));
    }

    public void setMinimumSize(GDimension dim) {
        setMinimumSize(Math.round(dim.width), Math.round(dim.height));
    }

    public void setPreferredSize(int w, int h) {
        //log.debug("set pref size: %d x %d", w, h);
        super.setPreferredSize(new Dimension(w, h));
    }

    public void setMaximumSize(int w, int h) {
        log.debug("set max size: %d x %d", w, h);
        super.setMaximumSize(new Dimension(w, h));
    }

    public final APGraphics getAPGraphics() {
        return G;
    }

    public final void setPadding(int padding) {
        this.padding = padding;
    }

    public final void setBackground(GColor color) {
        super.setBackground(AWTUtils.toColor(color));
    }

    /**
     * Get the position in viewport coords
     * @return
     */
    protected IVector2D getMousePos() {
        return getMousePos(getMouseX(), getMouseY());
    }

    protected IVector2D getMousePos(int mx, int my) {
        return getAPGraphics().screenToViewport(mx, my);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        Rectangle rect = getBounds();
        super.setBounds(x, y, width, height);
        if (G != null && (rect.width != width || rect.height != height)) {
            log.info("Dimension changed to %d x %d", width, height);
            onDimensionChanged(G, width, height);
        }
    }

    protected void onDimensionChanged(AWTGraphics g, int width, int height) {
        log.info("Dimension changed to %d x %d", width, height);
    }

    public void redraw() {
        repaint();
    }

    public Vector2D getViewportLocation() {
        Point pt = super.getLocationOnScreen();
        return new Vector2D(pt.x, pt.y);
    }

    public boolean isFocused() {
        return focused;
    }
}