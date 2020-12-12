package cc.misslecommand.swing;

import java.awt.event.KeyEvent;

import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTKeyboardAnimationApplet;
import cc.misslecommand.MissleCommand;

/**
 * Created by chriscaron on 10/26/17.
 */

public class MCApplet extends AWTKeyboardAnimationApplet {

    MissleCommand mc = new MissleCommand() {
        @Override
        public int getScreenWidth() {
            return MCApplet.this.getScreenWidth();
        }

        @Override
        public int getScreenHeight() {
            return MCApplet.this.getScreenHeight();
        }

        @Override
        public int getPointerX() {
            return MCApplet.this.getMouseX();
        }

        @Override
        public int getPointerY() {
            return MCApplet.this.getMouseY();
        }

        @Override
        public int getFrameNumber() {
            return MCApplet.this.getFrameNumber();
        }

        @Override
        public void checkPlayerInput() {
            MCApplet.this.checkPlayerInput();
        }

        @Override
        public void setFrameNumber(int frameNum) {
            MCApplet.this.setFrameNumber(frameNum);
        }
    };

    @Override
    protected void doInitialization() {
        mc.doInitialization();
    }

    @Override
    protected void drawFrame(AGraphics g) {
        mc.drawFrame(g);
    }

    @Override
    protected void onDimensionsChanged(AGraphics g, int width, int height) {
        mc.onDimensionsChanged(g, width, height);
    }

    // --------------------------------------------------------------
    // MAIN - DEBUGGING ENABLED
    // In Applet mode, debugging is off by default
    // --------------------------------------------------------------

    public static void main(String [] args) {
        Utils.setDebugEnabled();
        AWTFrame frame = new AWTFrame("Missle Command DEBUG MODE");
        MCApplet applet = new MCApplet();
        frame.add(applet);
        frame.centerToScreen(820, 620);
        applet.init();
        applet.start();
    }

    private boolean qPressed = false;

    @Override
    public void keyPressed(KeyEvent ev) {
        // make so 2 consecutive Q presses quit the game
        if (ev.getKeyCode() == KeyEvent.VK_Q) {
            if (qPressed)
                System.exit(0);
            else {
                qPressed = true;
                return;
            }
        }
        qPressed = false;

        // look for:
        // R restart level
        // L launch wave
        // A,S,D fire missle
        if (ev.getKeyCode() == KeyEvent.VK_R) {}
        //this.initGameStateGetReady(g, getScreenWidth(), getScreenHeight());
        else if (ev.getKeyCode() == KeyEvent.VK_L)
            mc.startMissleWave();

        int [] cityKeys = { KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D };

        for (int i=0; i<cityKeys.length; i++) {
            if (ev.getKeyCode() == cityKeys[i] && mc.getMissleCity(i).getNumMissles() > 0) {
                mc.startPlayerMissle(mc.getMissleCity(i));
            }
        }

    }

    void checkPlayerInput() {
        MissleCommand.City missle = null;
        for (int i=0; i<3; i++) {
            if (getMouseButtonClicked(i) && mc.getMissleCity(i).getNumMissles() > 0) {
                missle = mc.getMissleCity(i);
            }
        }
        if (missle != null) {
            mc.startPlayerMissle(missle);
        }
    }
}
