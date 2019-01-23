package cc.game.roids.applet;

import java.awt.event.FocusEvent;

import javax.swing.Timer;

import cc.game.roids.core.Roids;
import cc.game.roids.core.Roids.DragMode;
import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTKeyboardAnimationApplet;
import cc.lib.utils.StopWatch;

public class RoidsApplet extends AWTKeyboardAnimationApplet {

    public static void main(String [] args) {
        Utils.DEBUG_ENABLED = true;
        Utils.setRandomSeed(0);
        AWTFrame frame = new AWTFrame("JavaRoids Debug Mode");
        AWTKeyboardAnimationApplet app = new RoidsApplet();
        frame.add(app);
        frame.centerToScreen(640, 480);
        app.init();
        app.start();
        app.setMillisecondsPerFrame(20);
    }   
    
    Roids roids = new Roids();
    
    @Override
    protected void doInitialization() {
    }

    StopWatch sw = null;// new StopWatch();
    
    //long startTime = -1;
    long lastTime = -1;
    int fps;
    int drawFps;
    long fpsTime = 0;
    
    @Override
    protected void drawFrame(AGraphics g) {
        if (roids != null) {
            if (sw == null) {
                sw = new StopWatch();
                sw.start();
            } else if (!roids.isPaused()){
                sw.unpause();
                sw.capture();
            } else {
                sw.pause();
            }
            
            roids.setPointer(getMouseX(), getMouseY(), getMouseDX(), getMouseDY(), this.getMouseButtonPressed(0));
            long curTime = sw.getTime();
            long deltaTime = sw.getDeltaTime();
            if (deltaTime > 1000)
                deltaTime = 1000;
            
            fps ++;

            roids.updateAll(curTime, deltaTime);

            roids.drawAll(g);

            int ty = 5;
            final int tx = getScreenWidth()-5;
            final int th = g.getTextHeight() + 3;
            if (drawFps > 0) {
                g.setColor(GColor.WHITE);
                g.drawJustifiedString(tx, ty, Justify.RIGHT, Justify.TOP, "FPS: " + drawFps);
                ty += th;
            }
            if (curTime - fpsTime > 1000) {
                fpsTime = curTime;
                drawFps = fps;
                //System.out.println("t=" + curTime + " dt=" + deltaTime + " FPS=" + fps);
                fps = 0;
            }
            
            g.drawJustifiedString(tx, ty, Justify.RIGHT, Justify.TOP, "dx=" + getMouseDX() + " dy=" + getMouseDY());
            ty += th;
            if (roids.pauseOnCollision) {
                g.drawJustifiedString(tx, ty, Justify.RIGHT, Justify.TOP, "Pause on collision");
                ty += th;
            }
            
            if(this.getKeyboard('1')) {
                roids.doTest1();
            }

            if(this.getKeyboard('2')) {
                roids.doTest2();
            }

            if(this.getKeyboard('3')) {
                roids.doTest3();
            }

            if(this.getKeyboard('4')) {
                roids.doTest4();
            }

            if (this.getKeyboardReset(' ')) {
                roids.addTestThingy();
            }
            
            if (this.getKeyboardReset('x')) {
                roids.toggleRotations();
            }
            
            if (this.getKeyboardReset('z')) {
                roids.clearThingys();
            }
            
            if (this.getKeyboardReset('p')) {
                roids.togglePause();
            }
            
            if (this.getKeyboardReset('P')) {
                roids.pauseOnCollision = !roids.pauseOnCollision;
            }
            
            if (this.getKeyboardReset('S')) {
                roids.saveStateToFile("roids.state");
            }
            
            if (this.getKeyboardReset('R')) {
                roids.restoreStateFromFile("roids.state");
            }
            
            if (this.getKeyboardReset(',')) {
                roids.paused = true;
                roids.historyStep(-1);
            } else if (this.getKeyboardReset('.')) {
                roids.paused = true;
                roids.historyStep(1);
            }
            
            if (this.getKeyboard('o')) {
                roids.dragMode = DragMode.ORIENTATION;
            } else if (this.getKeyboard('v')) {
                roids.dragMode = DragMode.VELOCITY;
            } else if (this.getKeyboard('a')) {
                roids.dragMode = DragMode.ANGVELOCITY;
            } else {
                roids.dragMode = DragMode.POSITION;
            }
            
            if (getKeyboardReset('d')) {
            	roids.deleteSelected();
            }
            
            if (getKeyboard('h')) {
            	drawHelp(g);
            }
        }
    }

    @Override
    protected void onDimensionsChanged(AGraphics g, int width, int height) {
        g.ortho();
        roids.setScreenDimension(width, height);
    }

    @Override
    public synchronized void onPauseChanged(boolean paused) {
        if (sw != null) {
        	if (paused) {
        		sw.pause();
        		roids.paused = true;
        	}
        	else
        		sw.unpause();
        }
    }

    void drawHelp(AGraphics g) {
    	String help = "1       - test 1\n" +
                	  "2       - test 2\n" + 
                	  "3       - test 3\n" +
                	  "4       - test 4\n" +
                	  "<SP>    - add thingy\n" +
                	  "x       - toggle rotations\n" +
                	  "z       - clear thingys\n" +
                	  "d       - delete selected\n" +
                	  "p       - pause\n" +
                	  "P       - pause on collision\n" +
                	  "S       - save state to file\n" +
                	  "R       - restore state from file\n" + 
                	  "<       - history back\n" +
                	  ">       - history forward\n" +
                	  "drag    - change position\n" +
                	  "o+drag  - change orientation\n" +
                	  "v+drag  - change velocity\n" + 
                	  "a+drag  - change ang velocity\n" +
                	  "";
                
    	g.setColor(GColor.WHITE);
    	g.drawJustifiedString(g.getViewportWidth()/3, g.getViewportHeight()/2, Justify.LEFT, Justify.CENTER, help);
    }
    
}
