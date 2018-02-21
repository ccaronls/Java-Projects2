package cc.game.roids.core;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.math.*;

public class TestPolygon extends PolygonThingy {

    boolean highlighted = false;
    boolean circleHighlighed = false;
    
    /*
    TestPolygon(int type) {

        float [] xVerts, yVerts;
        if (type > 4) {
            int numVerts = 11;
    
            xVerts = new float[]{
                    3, 4, 6, 6, 4, 4, 2, 2, 0, 0, 2
            };
            
            yVerts = new float[]{
                    0, 2, 2, 4, 4, 6, 6, 4, 4, 2, 2
            };
        }

        else if (type < 3) {
            // a box
            
            xVerts = new float[]{ 0, 1, 1, 0 };
            yVerts = new float[]{ 0, 0, 1, 1 };
        } else {
            // random
            int numVerts = Utils.randRange(5, 10);
            
            xVerts = new float[numVerts];
            yVerts = new float[numVerts];

            float radius = 20;
            for (int i=0; i<numVerts; i++) {
                float deg = (360f / numVerts) * i + Utils.randRange(-10, 10);
                float l = Utils.randFloat(radius/2) + radius/2; 
                xVerts[i] = Utils.cosine(deg) * l;
                yVerts[i] = Utils.sine(deg) * l;
            }
        }
        /*
        int numVerts = Utils.randRange(5, 10);
        
        float [] xVerts = new float[numVerts];
        float [] yVerts = new float[numVerts];

        float radius = 20;
        for (int i=0; i<numVerts; i++) {
            float deg = (360f / numVerts) * i + Utils.randRange(-10, 10);
            float l = Utils.randFloat(radius/2) + radius/2; 
            xVerts[i] = Utils.cosine(deg) * l;
            yVerts[i] = Utils.sine(deg) * l;
        }
        this.setVertices(xVerts, yVerts);
        this.setRadius(Utils.randRange(10, 25));
        this.setAngVelocity(Utils.randFloatX(10));
        this.velocity[0] = Utils.randFloatX(2.0f);
        this.velocity[1] = Utils.randFloatX(2.0f);
    }*/
    
    @Override
    void update(Roids roids, float curTimeSeconds, float deltaTimeSeconds) {

        if (roids.pressed && highlighted) {
            switch (roids.dragMode) {
                case POSITION:
                    getPosition().set(roids.getPointer()); 
                    roids.somethingMoved = true;
                    break;
                case VELOCITY:
                    roids.getPointer().sub(position, velocity); 
                    break;
                case ORIENTATION: {
                	MutableVector2D delta = roids.getPointer().sub(position, Vector2D.newTemp());
                    setOrientation(delta.angleOf()); 
                    roids.somethingMoved = true;
                    break;
                }
                case ANGVELOCITY: {
                    Vector2D delta = roids.getPointer().sub(position, Vector2D.newTemp());
                    this.setAngVelocity(180.0f - delta.angleOf()); 
                    break;
                }
                    
            }
        } else if (roids.numHighlighed == 0) {
            circleHighlighed = Utils.distSqPointPoint(getX(), getY(), roids.getPointerX(), roids.getPointerY()) < getRadius() * getRadius();
            highlighted = this.isPointInsidePolygon(roids.getPointerX(), roids.getPointerY());
            if (highlighted) {
                roids.numHighlighed ++;
            }
        }
    }

    @Override
    void draw(AGraphics g) {
        g.setColor(GColor.GREEN);
        super.draw(g);
        g.pushMatrix();
	        g.translate(position);
	        g.rotate(getOrientation());
	        g.scale(1.1f, 1.1f);
	        if (this.circleHighlighed) {
	            g.setColor(GColor.RED);
	        } else {
	            g.setColor(GColor.YELLOW);
	        }
	        g.drawCircle(0, 0, getRadius());
	        //g.setLineWidth(2);
	        if (this.highlighted) {
	            g.setColor(GColor.RED);
	        } else {
	            g.setColor(GColor.BLUE);
	        }
	        renderBoundingVerts(g);
	        g.setColor(GColor.WHITE);
	    g.popMatrix();
        g.pushMatrix();
	        g.translate(position);
	        g.begin();
	        g.vertex(0,0);
	        g.vertex(velocity);
	        g.drawLines();
	        g.end();
	        //g.setLineWidth(2);
	        //g.setColor(g.WHITE);
	        //g.begin();
	        //g.vertex(0,0);
	        //g.vertex(getXVert(getPrimaryVert()), getYVert(getPrimaryVert()));
	        //g.drawLines();
	        //g.end();
	        //g.setLineWidth(1);
	        //*
	        g.setColor(GColor.WHITE);
	        g.pushMatrix();
		        g.rotate(getOrientation());
		        for (int i=0; i<getNumBoundingVerts(); i++) {
		            g.drawString("" + i, getXBoundingVert(i), getYBoundingVert(i));
		        }
	        //	*/
	        g.popMatrix();
	    g.popMatrix();
        

        if (highlighted) {
            String txt = "\n\n\nP=" + getPosition() + "\n"
                       + "V=" + getVelocity() + "\n"
                       + "O=" + getOrientation() + "\n"
                       + "Av=" + getAngVelocity() + "\n"
                       + "R=" + getRadius() + "\n"
                       + "M=" + getMass() + "\n"
                       + "I=" + getInertia() + "\n"
                       + "A=" + getSurfaceArea();
            g.drawJustifiedString(getX(), getY(), Justify.LEFT, Justify.TOP, txt);
        }
                       
    }

    public void toggleRotations() {
        if (this.getAngVelocity()!= 0) {
            this.setAngVelocity(0);
        } else {
            this.setAngVelocity(Utils.randFloatX(10));
        }
            
    }
    
    
}
