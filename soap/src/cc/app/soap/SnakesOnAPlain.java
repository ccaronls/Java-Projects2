package cc.app.soap;

import java.util.ArrayList;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTKeyboardAnimationApplet;

/**
 * Snakes writhing on the desert plain (not plane!)
 * @author ccaron
 *
 */
public class SnakesOnAPlain extends AWTKeyboardAnimationApplet {

    public static void main(String [] args) {
        Utils.setDebugEnabled();
        AWTFrame frame = new AWTFrame("Snakes on a Plain?");
        AWTKeyboardAnimationApplet app = new SnakesOnAPlain();
        frame.add(app);
        app.init();
        frame.centerToScreen(500, 500);
        app.start();
        app.setMillisecondsPerFrame(20);
    }       
    
    @Override
    protected void doInitialization() {
        // TODO Auto-generated method stub
    }

    @Override
    protected void drawFrame(AGraphics g) {
        this.clearScreen(GColor.WHITE);
        
        // TODO Auto-generated method stub
        if (this.getMouseButtonClicked(0)) {
            SnakeFood food = new SnakeFood(getMouseX(), getMouseY());
            snakeFood.add(food);
        }
        
        if (this.getMouseButtonClicked(1)) {
            addSnake(getMouseX(), getMouseY());
        }
        
        g.setColor(GColor.GRAY);
        for (int i=0; i<snakeFood.size(); ) {
            SnakeFood food = snakeFood.get(i);
            if (food.eaten) {
                snakeFood.remove(i);
                continue;
            }
            i++;
            g.drawDisk(food.x, food.y, 3);
        }
        
        for (int i=0; i<snakes.size(); i++) {
            Snake s = snakes.get(i);
            SnakeFood target = s.getTarget();
            if (true) { //target == null) {
                float minD = Float.MAX_VALUE;
                for (int f=0; f<snakeFood.size(); f++) {
                    SnakeFood food = snakeFood.get(f);
                    float d = s.getDistanceTo(food.x, food.y);
                    if (d < minD) {
                        target = food;
                        minD = d;
                    }
                }
                s.setTarget(target);
            }
            
            s.move();
            s.draw(g);
        }
    }

    @Override
    protected void onDimensionsChanged(AGraphics g, int width, int height) {
        g.ortho(0, width, 0, height);        
    }

    void addSnake(float x, float y) {
        Snake snake = new Snake(x, y);
        snakes.add(snake);
    }
    
    private ArrayList<Snake> snakes = new ArrayList();
    private ArrayList<SnakeFood>  snakeFood = new ArrayList();
    
}
