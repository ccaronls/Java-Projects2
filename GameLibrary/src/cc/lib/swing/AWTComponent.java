package cc.lib.swing;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.filechooser.*;

import cc.lib.game.*;


/**
 * Created by chriscaron on 2/21/18.
 */

public abstract class AWTComponent extends JComponent implements Renderable, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private AWTGraphics G = null;
    private int mouseX = -1;
    private int mouseY = -1;
    private boolean focused = false;
    private int padding = 5;
    private int scrollAmount = -1;
    private int scrollStartY = 0;

    public AWTComponent() {
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
    public final synchronized void paint(Graphics g) {
        if (getWidth() > 0 && getHeight() > 0) {
            Color c = g.getColor();
            g.setColor(getBackground());//AWTUtils.toColor(GColor.TRANSPARENT));
            g.fillRect(0, 0, super.getWidth(), super.getHeight());
            g.setColor(c);
            //g.setClip(padding,padding, getWidth()+1, getHeight()+1);
            if (G == null) {
                if (g instanceof Graphics2D)
                    G = new AWTGraphics2(((Graphics2D)g), this);
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
                    if (scrollAmount < 0)
                        scrollAmount = G.getTextHeight();
                    if (scrollStartY != 0) {
                        G.pushMatrix();
                        G.translate(0, scrollStartY);
                        paint(G, mouseX, mouseY);
                        G.popMatrix();
                    } else {
                        paint(G, mouseX, mouseY);
                    }
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
                    G.drawFilledRect(x-tw/2, y, tw*progress, th);
                    g.setFont(f);
                    repaint();
                }
            }
            //g.setClip(0, 0, super.getWidth(), super.getHeight());
            if (focused) {
//                System.out.println("AWT " + toString() + " has focus!");
                g.setColor(Color.BLUE);
                g.drawRect(0, 0, super.getWidth()-2, super.getHeight()-2);
            }
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
            onDragStopped();
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

    protected void onMouseWheel(int rotation) {
        Dimension d = getMinimumSize();
        int maxScroll = getHeight() - d.height;
        if (maxScroll < 0) {
            scrollStartY = Utils.clamp(scrollStartY - rotation * scrollAmount, maxScroll, 0);
            repaint();
        }
    }

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
            onDragStarted(mouseX, mouseY);
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

    public void keyTyped(KeyEvent evt) {
    }

    public void keyPressed(KeyEvent evt) {
        int c = evt.getKeyChar();
    }

    public void keyReleased(KeyEvent evt) {
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    protected void onDragStarted(int x, int y) {}

    protected void onDragStopped() {}

    protected void onClick() {}

    public final void repaint() {
        super.repaint();
    }

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
        super.setMinimumSize(new Dimension(w, h));
    }

    public void setMinimumSize(GDimension dim) {
        setMinimumSize(Math.round(dim.width), Math.round(dim.height));
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

    public final void setBackground(GColor color) {
        super.setBackground(AWTUtils.toColor(color));
    }

    static FileFilter getExtensionFilter(final String ext, final boolean acceptDirectories) {

        return new FileFilter() {

            public boolean accept(File file) {
                if (file.isDirectory() && acceptDirectories)
                    return true;
                return file.getName().endsWith(ext);
            }

            public String getDescription() {
                return "SOC Board Files";
            }

        };
    }

    private File workingDir = new File(".");

    public File showFileOpenChooser(EZFrame frame, String title, String extension) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(workingDir);
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (extension != null) {
            if (!extension.startsWith("."))
                extension = "." + extension;
            chooser.setFileFilter(getExtensionFilter(extension, true));
        }
        int result = chooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            workingDir = file.getParentFile();
            String fileName = file.getAbsolutePath();
            if (extension != null && !fileName.endsWith(extension))
                fileName += extension;
            return new File(fileName);
        }
        return null;
    }

    public File showFileSaveChooser(EZFrame frame, String title, String extension, File selectedFile) {
        final JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(selectedFile);
        chooser.setCurrentDirectory(workingDir);
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (extension != null) {
            if (!extension.startsWith("."))
                extension = "." + extension;
            chooser.setFileFilter(getExtensionFilter(extension, true));
        }
        int result = chooser.showSaveDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            workingDir = file.getParentFile();
            String fileName = file.getAbsolutePath();
            if (extension != null && !fileName.endsWith(extension))
                fileName += extension;
            return new File(file.getParent(), fileName);
        }
        return null;
    }

    public void showMessageDialog(EZFrame frame, String title, String message) {
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.PLAIN_MESSAGE);
    }
}
