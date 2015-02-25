package cc.lib.main;

import java.awt.Graphics;

import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.EZFrame;
import cc.lib.swing.KeyboardAnimationApplet;

public class CollisionTest extends KeyboardAnimationApplet {

    public static void main(String[] args) {
        Utils.DEBUG_ENABLED = true;
        EZFrame frame = new EZFrame("Collision Test");
        KeyboardAnimationApplet app = new CollisionTest();
        frame.add(app);
        frame.centerToScreen(600, 400);
        app.init();
        app.start();
        app.focusGained(null);
    }
    
	private final int SHAPE_TYPE_CIRCLE = 0;
	private final int SHAPE_TYPE_RECT = 1;
	private final int SHAPE_TYPE_POLY = 2;
	private final int SHAPE_TYPE_LINE = 3;
	
	private int shapeMode = SHAPE_TYPE_CIRCLE;
	
	private class Shape {
		int [] xpts; // polygon
		int [] ypts; // polygon
		int numPts = 0; // polygon
		int radius;  // circle
		int cx, cy;  // circle
		int x0, y0, x1, y1; // rect, line
		int type;
		
		Shape() {
			type = shapeMode;
			switch (type) {
			case SHAPE_TYPE_CIRCLE:
				cx = getMouseX();
				cy = getMouseY();
				radius = 2;
				break;
				
			case SHAPE_TYPE_RECT:				
				x0 = x1 = getMouseX();
				y0 = y1 = getMouseY();
				break;
				
			case SHAPE_TYPE_POLY:
				xpts = new int[32];
				ypts = new int[32];
				numPts = 1;
				xpts[0] = getMouseX();
				ypts[0] = getMouseY();
				break;
				
			default:
				Utils.unhandledCase(type);
				
			}
		}
		
	}
	
	private Shape [] shapes = new Shape[32];
	private int numShapes = 0;	
	
	private void addShape() {
		shapes[numShapes] = new Shape();
	}
	
	protected void onDimensionsChanged(AGraphics g, int width, int height) {}
	
	@Override
	protected void drawFrame(AWTGraphics g) {
		for (int i=0; i<numShapes; i++) {
			Shape shape = shapes[i];
			switch (shape.type) {
			case SHAPE_TYPE_CIRCLE:
				
			}
		}
	}
	
	private boolean collisionTest(int shapeIndex) {
		Shape shape = shapes[shapeIndex];
		for (int i=0; i<numShapes; i++) {
			if (i == shapeIndex)
				continue;

			Shape shape2 = shapes[i];
			
		}
		
		return false;
	}
	
	private boolean collisionTest(Shape s0, Shape s1) {
		switch (s0.type) {
		case SHAPE_TYPE_CIRCLE:
			switch (s1.type) {
			case SHAPE_TYPE_CIRCLE:
				return Utils.isCirclesOverlapping(s0.cx, s0.cy, s0.radius, s1.cx, s1.cy, s1.radius);
			case SHAPE_TYPE_RECT:
				return Utils.isPointInsideRect(s0.cx, s0.cy, s1.x0, s1.y0, s1.x1-s1.x0, s1.y1-s1.y0);
			case SHAPE_TYPE_POLY:
				return Utils.isPointInsidePolygon(s0.cx, s0.cy, s1.xpts, s1.ypts, s1.numPts);
			case SHAPE_TYPE_LINE:
			}
			break;
			
		case SHAPE_TYPE_RECT:
			switch (s1.type) {
			case SHAPE_TYPE_CIRCLE:
			case SHAPE_TYPE_RECT:
			case SHAPE_TYPE_POLY:
			case SHAPE_TYPE_LINE:
			}
			break;

		case SHAPE_TYPE_POLY:
			switch (s1.type) {
			case SHAPE_TYPE_CIRCLE:
			case SHAPE_TYPE_RECT:
			case SHAPE_TYPE_POLY:
			case SHAPE_TYPE_LINE:;
			}
			break;

		case SHAPE_TYPE_LINE:
			switch (s1.type) {
			case SHAPE_TYPE_CIRCLE:;
			case SHAPE_TYPE_RECT:
			case SHAPE_TYPE_POLY:
			case SHAPE_TYPE_LINE:
			}
			break;

		}
		
		return false;
	}
	
	protected void doInitialization() {
		
	}
	
	
}
