package cc.app.soap;

import java.awt.Color;
import java.awt.Graphics;

import cc.lib.game.AGraphics;
import cc.lib.game.Utils;

public class Snake {

    private final int MAX_SECTIONS = 100;
    private final float LEN    = 5;
    
    private float sx, sy;
    private float [] ldx;
    private float [] ldy;
    private float headX, headY;
    
    private float health = 0;
    private SnakeFood target = null;
    private int numSections = 20;
    private State state = State.EATING;
    
    enum State {
        EATING,
        SHEDDING,
        DEAD,
        COILING,
        STRIKING,
    }
    
    Snake(float sx, float sy) {
        this.sx = headX = sx;
        this.sy = headY = sy;
        ldx = new float[MAX_SECTIONS];
        ldy = new float[MAX_SECTIONS];
        int ang = Utils.rand() % 360;
        for (int i=0; i<numSections; i++) {
            ldx[i] = Utils.cosine(ang) * LEN;
            ldy[i] = Utils.sine(ang) * LEN;
            headX += ldx[i];
            headY += ldy[i];
            ang += Utils.rand() % 10 + 10;
        }
        health = Utils.randFloat(1);
    }
    
    void move(float dx, float dy) {
        headX += dx;
        headY += dy;
        for (int i=numSections - 1; i>=0; --i) {
            ldx[i] += dx;
            ldy[i] += dy;
            float l = (float)Math.sqrt(ldx[i]*ldx[i] + ldy[i]*ldy[i]);
            float dl = l - LEN;
            float nx = ldx[i] / l;
            float ny = ldy[i] / l;
            ldx[i] = nx * LEN;
            ldy[i] = ny * LEN;
            dx = nx * dl;
            dy = ny * dl;
        }
        sx += dx;
        sy += dy;
    }
    
    private void drawSections(AGraphics g, float x, float y) {
        float thickness = 1;
        float maxThickness = 10.0f*health;
        maxThickness = Utils.clamp(maxThickness, 3, 10);
        for (int i=0; i<numSections; i++) {
            float x2 = x+ldx[i];
            float y2 = y+ldy[i];
            g.drawLine(x, y, x2, y2, Math.round(thickness));
            x = x2;
            y = y2;
            if (thickness < maxThickness)
                thickness += 0.3f;
            else if (i>numSections-10)
                thickness -= 0.2;
        }
        // draw the head
        g.drawDisk(x, y, maxThickness+2);
    }
    
    void draw(AGraphics g) {
    
        // draw the shadow
        g.setColor(g.BLACK);
        drawSections(g, sx+5, sy+5);
        // draw the actual snake
        int green = Math.round(255.0f * health);
        green = Utils.clamp(green, 0, 255);
        g.setColor(g.makeColori(0, green, 0));
        drawSections(g, sx, sy);        
        
        // randomly draw a 'tounge'
        if (Utils.rand() % 100 == 0) {
            g.setColor(g.RED);
            
        }
    }
    
    float getDistanceTo(float x, float y) {
        float dx = x - headX;
        float dy = y - headY;
        return (float)Math.sqrt(dx*dx + dy*dy);
    }
    
    SnakeFood getTarget() {
        return target;
    }
    
    void setTarget(SnakeFood target) {
        this.target = target;
    }
    
    void move() {
        if (target != null) {
            if (target.eaten) {
                target = null;
            } else {
                float dx = target.x - headX;
                float dy = target.y - headY;
                float mag = (float)Math.sqrt(dx*dx + dy*dy);
                if (mag < 5) {
                    // eat the food
                    target.eaten = true;
                    health += target.health;
                    target = null;
                    if (health >= 1.0f) {
                        if (numSections < MAX_SECTIONS) {
                            // add a section
                            ldx[numSections] = ldx[numSections-1];
                            ldy[numSections] = ldy[numSections-1];
                            numSections ++;
                            health -= 0.3f;
                        } else {
                            health = 0;
                        }                        
                    }
                } else {
                    float speed = health / mag;
                    dx *= speed;
                    dy *= speed;
                    move(dx, dy);
                }
            }
        } 
    }
    
}
