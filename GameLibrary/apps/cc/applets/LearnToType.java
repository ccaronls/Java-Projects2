package cc.applets;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTKeyboardAnimationApplet;
import cc.lib.utils.FileUtils;
import cc.lib.utils.Reflector;

public class LearnToType extends AWTKeyboardAnimationApplet {

    public static void main(String [] args) {
        //Utils.DEBUG_ENABLED = true;
        //Golf.DEBUG_ENABLED = true;
        //PlayerBot.DEBUG_ENABLED = true;
        //mode = 0;
        AWTFrame frame = new AWTFrame("Learn to Type");
        AWTKeyboardAnimationApplet app = new LearnToType();
        frame.add(app);
        app.init();
        frame.centerToScreen(800, 600);
        app.start();
        app.setMillisecondsPerFrame(20);
        File settings = FileUtils.getOrCreateSettingsDirectory(LearnToType.class);
        frame.setPropertiesFile(new File(settings, "app.properties"));
        frame.restoreFromProperties();
    }

    File keysFile = new File("keys.txt");

    @Override
    protected void doInitialization() {

        try {
            keys = Reflector.deserializeFromFile(keysFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    int keyboardId = -1;
    Character key = 'a';
    boolean shiftPressed = false;

    static int mode = 1;

    void drawKeyboard(AGraphics g) {
        if (keyboardId < 0)
            keyboardId = g.loadImage("small_keyboard.jpg");

        AImage img = g.getImage(keyboardId);
        float aspect = img.getAspect();

        int w = g.getViewportWidth();
        int h = Math.round((float)w / aspect);

        int x = 0;
        int y = 0;

        g.setTextHeight(28);
        g.drawImage(keyboardId, x, y, w, h);

    }

    Character typeThis = null;

    @Override
    protected void drawFrame(AGraphics g) {
        g.clearScreen(GColor.GREEN);

        if (mode == 0) {
            drawKeyboard(g);
            if (key != null) {
                g.setColor(GColor.BLACK);
                g.drawJustifiedString(10, getViewportHeight() - 10, Justify.LEFT, Justify.BOTTOM, key.toString());
            }

            for (Map.Entry<Character, GRectangle> k : keys.entrySet()) {
                g.setColor(GColor.YELLOW);
                g.drawRect(k.getValue());
                g.drawJustifiedString(k.getValue().getCenter(), Justify.CENTER, Justify.CENTER, k.getKey().toString());
            }

            if (dragging) {
                g.setColor(GColor.GREEN);
                g.drawRect(rect);
            }
        }

        if (mode == 1) {
            drawKeyboardKeys(g);

            String txt = "Thanks for trying Learn to Type!\nPlace fingers on the F and J keys and press them simultaneously to begin.";
            g.drawWrapString(getViewportWidth()/2, getViewportHeight()*3/4, getViewportWidth()*2/3, Justify.CENTER, Justify.CENTER, txt);
        }

        if (mode == 2) {
            drawKeyboardKeys(g);
            if (pressed != null) {
                GRectangle r = keys.get(pressed);
                if (r != null) {
                    g.setColor(GColor.MAGENTA);
                    g.drawRoundedRect(r, 1, 10);
                }
            }


            g.setTextHeight(56);
            if (typeThis == null) {
                String chars = "abcdefghijklmnopqrstuvwxyz";
                typeThis = chars.charAt(Utils.rand()%chars.length());
            }

            g.setColor(GColor.BLUE);
            g.drawWrapString(getViewportWidth()/2, getViewportHeight()*3/4, getViewportWidth(), Justify.CENTER, Justify.CENTER, "TYPE THIS\n" + typeThis.toString());
            GRectangle rect = keys.get(typeThis);
            g.drawRect(rect);
        }
    }

    void drawKeyboardKeys(AGraphics g) {
        g.setTextHeight(20);
//        g.setTextStyles(AGraphics.TextStyle.BOLD);
        for (Map.Entry<Character, GRectangle> e : keys.entrySet()) {
            g.setColor(GColor.WHITE);
            g.drawFilledRoundedRect(e.getValue(), 10);
            g.setColor(GColor.BLACK);
            g.drawJustifiedString(e.getValue().getCenter(), Justify.CENTER, Justify.CENTER, e.getKey().toString().toUpperCase());
        }

        g.setColor(GColor.RED);
        g.drawRoundedRect(keys.get('f'), 1, 10);
        g.drawRoundedRect(keys.get('j'), 1, 10);

    }

    @Override
    public void keyTyped(KeyEvent evt) {

    }

    boolean dragging = false;
    GRectangle rect = new GRectangle();

    @Override
    public void mouseDragged(MouseEvent ev) {
        if (mode == 0) {
            Vector2D v = new Vector2D(ev.getX(), ev.getY());
            rect.setEnd(v);
            dragging = true;
        }
    }

    @Override
    protected void onMousePressed(MouseEvent ev) {
        if (mode == 0) {
            rect = new GRectangle();
            Vector2D v = new Vector2D(ev.getX(), ev.getY());
            rect.set(v, v);
        }
    }

    @Override
    public synchronized void mouseReleased(MouseEvent evt) {
        if (mode == 0) {
            if (dragging) {
                keys.put(key, rect);
                dragging = false;
                try {
                    Reflector.serializeToFile(keys, keysFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    Character pressed = null;

    @Override
    public void keyPressed(KeyEvent evt) {
        if (mode == 0) {
            switch (evt.getKeyCode()) {
                case KeyEvent.VK_SHIFT:
                    shiftPressed = true;
                    break;

            }
        }

        if (mode == 1) {
            super.keyPressed(evt);
            if (getKeyboard('f') && getKeyboard('j')) {
                mode = 2;
            }
        }

        if (mode == 2) {
            pressed = evt.getKeyChar();
        }
    }

    @Override
    public void keyReleased(KeyEvent evt) {
        if (mode == 0) {
            int code = evt.getKeyCode();
            if (code >= KeyEvent.VK_0 && code <= KeyEvent.VK_Z) {
                key = evt.getKeyChar();
            }
            switch (evt.getKeyCode()) {
                case KeyEvent.VK_SHIFT:
                    shiftPressed = false;
                    break;

            }
        }

        if (mode == 1) {
            super.keyReleased(evt);
        }

        if (mode == 2 && evt.getKeyChar() == typeThis) {
            typeThis = null;
            pressed = null;
        }

    }

    Map<Character, GRectangle> keys = new HashMap<>();

    @Override
    protected void onDimensionsChanged(AGraphics g, int width, int height) {

    }


}
