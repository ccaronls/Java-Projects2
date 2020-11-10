package cc.game.superrobotron.swing;

import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import cc.game.superrobotron.ILocalization;
import cc.game.superrobotron.Robotron;
import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTKeyboardAnimationApplet;

public class RobotronApplet extends AWTKeyboardAnimationApplet implements ILocalization {

    public static void main(String [] args) {
        AGraphics.DEBUG_ENABLED = true;
        //Utils.DEBUG_ENABLED = true;
        //Golf.DEBUG_ENABLED = true;
        //PlayerBot.DEBUG_ENABLED = true;
        AWTFrame frame = new AWTFrame("Kaiser");
        AWTKeyboardAnimationApplet app = new RobotronApplet();
        frame.add(app);
        app.init();
        frame.centerToScreen(800, 600);
        app.start();
        app.setMillisecondsPerFrame(1000/20);
    }   

    @Override
    protected Font getDefaultFont() {
        return new Font("Arial", Font.BOLD, 12);

    }



    Robotron robotron;
    
    @Override
    protected void doInitialization() {
        Utils.DEBUG_ENABLED = true;
        robotron = new Robotron(this);
    }

    @Override
    protected void drawFrame(AGraphics g) {

        robotron.drawGame(g);
        
        if (showHelp) {
            g.setColor(GColor.YELLOW);
            String strHelp = "HELP:\n" 
                + "M   - add snake missle\n" 
                + "L   - add powerup\n" 
                + "H   - help screen\n" 
                + "R   - return to intro\n" 
                + "N   - goto next level [" + (robotron.getGameLevel()+1) + "]\n" 
                + "P   - goto previous level [" + (robotron.getGameLevel()-1) + "\n"
                + "V   - toggle visibility mode [" + (robotron.getGameVisibility()) + "]\n";
            
            g.drawJustifiedString(20, getScreenHeight()/2, Justify.LEFT, Justify.CENTER, strHelp);
        }
    }

    @Override
    protected void onDimensionsChanged(AGraphics g, int width, int height) {
        robotron.initGraphics(g);
        robotron.setDimension(width, height);
        g.ortho();
    }

    boolean showHelp = false;
    // bit flags too key_down_flag
    private final  int KEY_FLAG_LEFT     = 1;
    private final  int KEY_FLAG_RIGHT    = 2;
    private final  int KEY_FLAG_DOWN     = 4;
    private final  int KEY_FLAG_UP       = 8;
    
    private int key_down_flag = 0;
    private int playerDx = 0, playerDy=0;
    
    @Override
    public void keyPressed(KeyEvent evt) {
        switch (evt.getKeyCode()) {
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                key_down_flag |= KEY_FLAG_RIGHT;
                playerDx = 1;
                break;
                
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                key_down_flag |= KEY_FLAG_LEFT;
                playerDx = -1;
                break;
                
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                key_down_flag |= KEY_FLAG_DOWN;
                playerDy = 1;
                break;
                
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                key_down_flag |= KEY_FLAG_UP;
                playerDy = -1;
                break;
                
            case KeyEvent.VK_H:
                showHelp = true;
                break;
                
        }

        if (Utils.DEBUG_ENABLED) {
            int index = evt.getKeyChar() - '0';
            if (index >= 0 && index < Robotron.Debug.values().length) {
                boolean enabled = robotron.isDebugEnabled(Robotron.Debug.values()[index]);
                robotron.setDebugEnabled(Robotron.Debug.values()[index], !enabled);
            }
        }
        robotron.setPlayerMovement(playerDx, playerDy);
        evt.consume();
    }

    @Override
    public void keyReleased(KeyEvent evt) {
        switch (evt.getKeyCode()) {
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                key_down_flag &= ~KEY_FLAG_RIGHT;
                if ((key_down_flag & KEY_FLAG_LEFT) != 0) {
                    playerDx = -1;
                } else {
                    playerDx = 0;
                }
                break;
                
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                key_down_flag &= ~KEY_FLAG_LEFT;
                if ((key_down_flag & KEY_FLAG_RIGHT) != 0) {
                    playerDx = 1;
                } else {
                    playerDx = 0;
                }
                break;
                
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                key_down_flag &= ~KEY_FLAG_DOWN;
                if ((key_down_flag & KEY_FLAG_UP) != 0) {
                    playerDy = 1;
                } else {
                    playerDy = 0;
                }
                break;
                
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                key_down_flag &= ~KEY_FLAG_UP;
                if ((key_down_flag & KEY_FLAG_DOWN) != 0) {
                    playerDy = -1;
                } else {
                    playerDy = 0;
                }
                break;
                
            case KeyEvent.VK_H:
                showHelp = false;
                break;                
        }
        robotron.setPlayerMovement(playerDx, playerDy);
        evt.consume();
    }

    @Override
    protected void onMousePressed(MouseEvent ev) {
        robotron.setCursorPressed(true);
    }

    @Override
    public void mouseReleased(MouseEvent evt) {
        robotron.setCursorPressed(false);
    }

    @Override
    public void mouseClicked(MouseEvent evt) {
    }

    @Override
    public void mouseMoved(MouseEvent evt) {
        robotron.setCursor(evt.getX(), evt.getY());
    }

    @Override
    public void mouseDragged(MouseEvent evt) {
        robotron.setCursor(evt.getX(), evt.getY());
    }

    @Override
    public String getString(StringID id) {
        switch (id) {
            case GAME_OVER:
                return "G A M E    O V E R";
        }
        return "";
    }

}
