package cc.jroids.swing;

import java.awt.event.KeyEvent;

import cc.jroids.JavaRoids;
import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTKeyboardAnimationApplet;

/**
 * Created by chriscaron on 10/30/17.
 */

public class JavaRoidsApplet extends AWTKeyboardAnimationApplet {

    JavaRoids jr = new JavaRoids() {
        @Override
        public int getFrameNumber() {
            return 0;
        }

        @Override
        public int getScreenWidth() {
            return 0;
        }

        @Override
        public int getScreenHeight() {
            return 0;
        }
    };

    public static void main(String [] args) {
        Utils.DEBUG_ENABLED = true;
        AWTFrame frame = new AWTFrame("JavaRoids Debug Mode");
        AWTKeyboardAnimationApplet app = new JavaRoidsApplet();
        frame.add(app);
        app.init();
        //frame.centerToScreen(620, 620);
        frame.finalizeToBounds(2244, 301, 620, 620);
        app.start();
        app.setMillisecondsPerFrame(33);
    }

    @Override
    protected void onDimensionsChanged(AGraphics g, int width, int height) {
        jr.initGraphics(g);
        //g.ortho();
        g.ortho(-width/2, width/2, height/2, -height/2);
    }

    //---------------------------------------------------------------
    public void keyTyped(KeyEvent e) {
        int key=e.getKeyCode();
        switch (key){
            case KeyEvent.VK_SPACE:
                //hyperspace();
                break;

            case KeyEvent.VK_S: // stop the player dead
                jr.player_v.set(0,0);
                break;
        }
    }

    //---------------------------------------------------------------
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        switch (key) {
            case KeyEvent.VK_LEFT:
                jr.pressButton(JavaRoids.PLAYER_BUTTON_LEFT);
                break;

            case KeyEvent.VK_RIGHT:
                jr.pressButton(JavaRoids.PLAYER_BUTTON_RIGHT);
                break;

            case KeyEvent.VK_UP:
                jr.pressButton(JavaRoids.PLAYER_BUTTON_THRUST);
                break;

            case KeyEvent.VK_DOWN:
                break;

            case KeyEvent.VK_SHIFT:
                jr.pressButton(JavaRoids.PLAYER_BUTTON_SHIELD);
                break;

            case KeyEvent.VK_Z:
            case KeyEvent.VK_SPACE:
                jr.pressButton(JavaRoids.PLAYER_BUTTON_SHOOT);
                break;

            case KeyEvent.VK_ALT:
                break;

            case KeyEvent.VK_S:
                jr.player_v.zero();
        }
    }

    @Override
    protected void doInitialization() {
        jr.doInitialization();
    }

    @Override
    protected void drawFrame(AGraphics g) {
        jr.drawFrame(g);
    }

    //---------------------------------------------------------------
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        switch (key) {
            case KeyEvent.VK_LEFT:
                jr.releaseButton(JavaRoids.PLAYER_BUTTON_LEFT);
                break;

            case KeyEvent.VK_RIGHT:
                jr.releaseButton(JavaRoids.PLAYER_BUTTON_RIGHT);
                break;

            case KeyEvent.VK_UP:
                jr.releaseButton(JavaRoids.PLAYER_BUTTON_THRUST);
                break;

            case KeyEvent.VK_DOWN:
                break;

            case KeyEvent.VK_SHIFT:
                jr.releaseButton(JavaRoids.PLAYER_BUTTON_SHIELD);
                break;

            case KeyEvent.VK_Z:
            case KeyEvent.VK_SPACE:
                jr.releaseButton(JavaRoids.PLAYER_BUTTON_SHOOT);
                break;

            case KeyEvent.VK_ALT:
                break;
        }
    }
/*
    //---------------------------------------------------------------
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        mouse_dx = mouse_x - x;
        mouse_dy = mouse_y - y;
        mouse_x = x;
        mouse_y = y;
    }

    //---------------------------------------------------------------
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    //---------------------------------------------------------------
    public void onMousePressed(MouseEvent e) {
        int button=e.getButton();
        switch (button) {
            case MouseEvent.BUTTON1:
                switch (game_state) {
                    case STATE_INTRO:
                        break;

                    case STATE_PLAY:
                        break;
                }
                break;

            case MouseEvent.BUTTON2:
                break;

            case MouseEvent.BUTTON3:
                if (game_state == STATE_PLAY)
                {

                }
                break;
        }
    }

    //---------------------------------------------------------------
    public void mouseReleased(MouseEvent e) {
        if (game_state != STATE_PLAY)
            return;

        int button=e.getButton();
        switch (button) {
            case MouseEvent.BUTTON1:
                break;

            case MouseEvent.BUTTON2:
                break;

            case MouseEvent.BUTTON3:
                break;
        }
    }*/
}
