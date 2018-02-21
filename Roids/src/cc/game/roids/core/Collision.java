package cc.game.roids.core;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.GColor;
import cc.lib.game.AGraphics;
import cc.lib.math.*;

/**
 * Contains information related to a collision between 2 objects
 * 
 * @author ccaron
 *
 */
public class Collision {

    Thingy source, target;
    //MutableVector2D sourcePos = new MutableVector2D();
    //MutableVector2D targetPos = new MutableVector2D();
    MutableVector2D collisionPoint = new MutableVector2D(); // collision point
    MutableVector2D planeNormal = new MutableVector2D(); // unit length normal to plane of collision

    // Collision response vars for debugging
    MutableVector2D R1 = new MutableVector2D();
    MutableVector2D R2 = new MutableVector2D();
    MutableVector2D Vp1 = new MutableVector2D();
    MutableVector2D Vp2 = new MutableVector2D();
    MutableVector2D Jr = new MutableVector2D();
    
    
    // for debugging
    class Segment {
    	final Vector2D A, B;
    	final int index;
		public Segment(Vector2D a, Vector2D b, int index) {
			super();
			A = a;
			B = b;
			this.index = index;
		}
    	
    }
    List<Segment> segments = new ArrayList<>();
    
    public void draw(AGraphics g) {
    	
    	GColor [] segColors = {
                GColor.YELLOW,
                GColor.MAGENTA,
                GColor.DARK_GRAY
    	};
    	
        for (Segment segment: segments) {
        	g.setColor(segColors[segment.index % segColors.length]);
            g.drawLine(segment.A.X(), segment.A.Y(), segment.B.X(), segment.B.Y(), 2);
        }
        g.setColor(GColor.WHITE);
        g.drawCircle(collisionPoint.getX(), collisionPoint.getY(), 2);
        g.drawCircle(collisionPoint.getX(), collisionPoint.getY(), 5);
        g.setColor(GColor.ORANGE);
        final float arrowLen = 10;
        Vector2D pt = collisionPoint;
        g.drawLine(pt.getX(), pt.getY(), pt.getX()+planeNormal.getX()*arrowLen, pt.getY()+planeNormal.getY()*arrowLen, 2);
        //g.drawLine(source.getX(), source.getY(), source.getX()+sourceBounce.getX()*arrowLen, source.getY()+sourceBounce.getY()*arrowLen);
        //g.drawLine(target.getX(), target.getY(), target.getX()+targetBounce.getX()*arrowLen, target.getY()+targetBounce.getY()*arrowLen);
        g.drawString("SRC", source.getX(), source.getY());
        g.drawString("TGT", target.getX(), target.getY());

        g.setColor(GColor.CYAN);
        g.begin();
        g.vertex(source.getPosition());
        g.vertex(source.getPosition().add(R1, Vector2D.newTemp()));
        g.vertex(source.getPosition());
        g.vertex(source.getPosition().add(Vp1, Vector2D.newTemp()));
        g.drawLines();
        g.end();
        
        g.setColor(GColor.GREEN);
        g.begin();
        g.vertex(target.getPosition());
        g.vertex(target.getPosition().add(R2, Vector2D.newTemp()));
        g.vertex(target.getPosition());
        g.vertex(target.getPosition().add(Vp2, Vector2D.newTemp()));
        g.drawLines();
        g.end();
    }

    void addSegment(Vector2D v0, Vector2D v1, int colorIndex) {
        segments.add(new Segment(v0, v1, colorIndex));
    }
    
    public String toString() {
        return "P=" + collisionPoint + " N=" + planeNormal;
    }
}
