package cc.applets.soap;

import java.awt.Graphics;

public interface ISnakeTarget {

    void move();
    void draw(Graphics g);
    boolean canEat();
    
}
