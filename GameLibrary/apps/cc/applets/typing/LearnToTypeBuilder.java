package cc.applets.typing;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JApplet;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.Justify;
import cc.lib.math.Vector2D;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTGraphics;
import cc.lib.utils.FileUtils;
import cc.lib.utils.Reflector;

public class LearnToTypeBuilder extends JApplet
        implements MouseListener,
        MouseMotionListener,
        KeyListener,
        FocusListener {

    private Graphics G = null;
    private AGraphics AG = null;

    public static void main(String [] args) {
        final LearnToTypeBuilder app = new LearnToTypeBuilder();
        AWTFrame frame = new AWTFrame("Learn to Type") {
            @Override
            protected void onWindowClosing() {
                try {
                    File resources = new File("GameLibrary/resources");
                    if (!resources.isDirectory())
                        throw new Exception("resources folder:\n '" + resources + "'\n not found in working directory:\n " + new File(".").getCanonicalPath());
                    Reflector.serializeToFile(app.keyboard, new File(resources, keyBoardFile.getName()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        app.setCursor( app.getToolkit().createCustomCursor(
                new BufferedImage( 1, 1, BufferedImage.TYPE_INT_ARGB ),
                new Point(),
                null ) );
        frame.add(app);
        File settings = FileUtils.getOrCreateSettingsDirectory(LearnToTypeBuilder.class);
        frame.setPropertiesFile(new File(settings, "app.properties"));
        if (!frame.restoreFromProperties())
            frame.centerToScreen(800, 600);
        app.init();
        app.start();
    }

    Map<Integer, GRectangle> keyboard = new HashMap<>();
    static File keyBoardFile = new File("keyboard.txt");

    @Override
    public void init() {
        addFocusListener(this);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        try {
            Reflector.deserializeFromFile(keyBoardFile);
        } catch (FileNotFoundException e) {
            // ignore
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public final void paint(Graphics g) {

        if (g != G) {
            G = g;
            AG = new AWTGraphics(g, this);
            setup(AG);
        }

        draw(AG);
    }

    int keybdId = -1;

    protected void setup(AGraphics g) {
        if (keybdId < 0)
            keybdId = g.loadImage("small_keyboard.png");
    }

    KeyEvent pressed = null;
    GRectangle rect = null;
    boolean dragging = false;
    int mouseX=0, mouseY=0;

    protected void draw(AGraphics g) {

        g.clearScreen(GColor.GREEN);
        if (keybdId >= 0) {
            AImage img = g.getImage(keybdId);
            g.drawImage(keybdId, 0, 0, g.getViewportWidth(), (float)g.getViewportWidth()/img.getAspect());
        }

        for (Map.Entry<Integer, GRectangle> e : keyboard.entrySet()) {
            g.setColor(GColor.WHITE);
            g.drawFilledRoundedRect(e.getValue(), 10);
            g.setColor(GColor.BLACK);
            String key = KeyEvent.getKeyText(e.getKey());
            g.drawJustifiedString(e.getValue().getCenter(), Justify.CENTER, Justify.CENTER, key);
        }

        g.setColor(GColor.YELLOW);
        g.setLineWidth(3);
        if (dragging) {
            g.drawRect(rect);
        }

        if (pressed != null) {
            g.setTextHeight(32);
            g.setColor(GColor.BLUE);
            g.setTextStyles(AGraphics.TextStyle.BOLD);
            g.drawJustifiedString(g.getViewportWidth()/2, g.getViewportHeight()*3/4, KeyEvent.getKeyText(pressed.getKeyCode()));
        }

        // draw crosshais
        g.setColor(GColor.LIGHT_GRAY);
        final int SIZE = 10;
        g.drawLine(mouseX-SIZE, mouseY, mouseX+SIZE, mouseY);
        g.drawLine(mouseX, mouseY-SIZE, mouseX, mouseY+SIZE);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        pressed = e;
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        pressed = e;
        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressed = e;
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }



    @Override
    public void mousePressed(MouseEvent e) {
        if (pressed != null) {
            Vector2D v = new Vector2D(mouseX, mouseY);
            rect = new GRectangle(v, v);
            repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (dragging) {
            keyboard.put(pressed.getKeyCode(), rect);
            repaint();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
        if (pressed != null) {
            dragging = true;
            Vector2D v = new Vector2D(mouseX, mouseY);
            rect.setEnd(v);
        }
    }

    final int GRID = 5;

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = (e.getX()/GRID) * GRID;
        mouseY = (e.getY()/GRID) * GRID;
        repaint();
    }

    @Override
    public void focusGained(FocusEvent e) {

    }

    @Override
    public void focusLost(FocusEvent e) {

    }
}
