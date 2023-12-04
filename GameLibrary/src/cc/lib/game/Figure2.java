package cc.lib.game;

import cc.lib.math.MutableVector2D;
import cc.lib.reflector.Reflector;

/**
 * A 2D Figure 
 * @author chriscaron
 *
 */
public class Figure2 extends Reflector<Figure2> {

	public interface Render {
		public void drawPart(AGraphics g, Part part, float length, float thickness, float radius, boolean farSide);
	}
	
	public Figure2() {
		this("unknown");
	}
	public Figure2(String name) {
		this.name = name;
		value = new float[NUM_PARTS];
		for (Value v : Value.values()) {
			value[v.ordinal()] = v.defaultValue;
		}
	}
	
	public final static int NUM_PARTS = Value.values().length;
	
	private boolean rightFacing = true;	

    private final float [] value;
	private String name;

	static {
		addField(Figure2.class, "name");
		addField(Figure2.class, "value");
		addField(Figure2.class, "rightFacing");
	}

	public enum Part {
		UPPER_ARM,
		LOWER_ARM,
		UPPER_LEG,
		LOWER_LEG,
		FOOT,
		HEAD,
		NECK,
		HAND,
		BODY,
	};
	
	public enum Value {
		BODY_ANGLE(0),
		BODY_THICKNESS(3),
		BODY_LENGTH(6),

		HEAD_ANGLE(0),
		HEAD_RADIUS(1.5f),

		NECK_ANGLE(0),
		NECK_LENGTH(1),
		NECK_RADIUS(0.5f),

		ARM_OFFSET(3),
		RU_ARM_ANGLE(0),
		LU_ARM_ANGLE(0),
		U_ARM_LENGTH(3),
		RL_ARM_ANGLE(0),
		LL_ARM_ANGLE(0),
		L_ARM_LENGTH(2),
		ARM_THICKNESS(1),

		R_HAND_ANGLE(0),
		L_HAND_ANGLE(0),
		HAND_RADIUS(0.5f),

		RU_LEG_ANGLE(0),
		LU_LEG_ANGLE(0),
		U_LEG_LENGTH(4),
		U_LEG_THICKNESS(1.5f),

		RL_LEG_ANGLE(0),
		LL_LEG_ANGLE(0),
		L_LEG_LENGTH(3),
		L_LEG_THICKNESS(1),
		
		R_FOOT_ANGLE(-90),
		L_FOOT_ANGLE(-90),
		FOOT_LENGTH(2),
		FOOT_THICKNESS(1),
		
		;
		
		Value(float defaultValue) {
			this.defaultValue = defaultValue;
		}
		
		final float defaultValue;
	}
	
    public void transitionTo(Figure2 to, float t, Figure2 result) {
        for (int i=0; i<NUM_PARTS; i++) {
        	result.value[i] = (1-t)*value[i] + t*to.value[i];
        }
    }
    
    private void drawLeftArms(AGraphics g, Render render, boolean farSide) {
    	g.pushMatrix();
    	g.translate(0, -getValue(Value.ARM_OFFSET));
    	g.rotate(getValue(Value.LU_ARM_ANGLE));
    	render.drawPart(g, Part.UPPER_ARM, getValue(Value.U_ARM_LENGTH), getValue(Value.ARM_THICKNESS), 0, farSide);
    	g.translate(0, getValue(Value.U_ARM_LENGTH));
    	g.rotate(getValue(Value.LL_ARM_ANGLE));
    	render.drawPart(g, Part.LOWER_ARM, getValue(Value.L_ARM_LENGTH), getValue(Value.ARM_THICKNESS), 0, farSide);
    	g.translate(0, getValue(Value.L_ARM_LENGTH));
    	g.rotate(getValue(Value.L_HAND_ANGLE));
    	g.translate(0, getValue(Value.HAND_RADIUS));
    	render.drawPart(g, Part.HAND, 0, 0, getValue(Value.HAND_RADIUS), farSide);
    	g.popMatrix();
    }
    
    private void drawRightArms(AGraphics g, Render render, boolean farSide) {
    	g.pushMatrix();
    	g.translate(0, -getValue(Value.ARM_OFFSET));
    	g.rotate(getValue(Value.RU_ARM_ANGLE));
    	render.drawPart(g, Part.UPPER_ARM, getValue(Value.U_ARM_LENGTH), getValue(Value.ARM_THICKNESS), 0, farSide);
    	g.translate(0, getValue(Value.U_ARM_LENGTH));
    	g.rotate(getValue(Value.RL_ARM_ANGLE));
    	render.drawPart(g, Part.LOWER_ARM, getValue(Value.L_ARM_LENGTH), getValue(Value.ARM_THICKNESS), 0, farSide);
    	g.translate(0, getValue(Value.L_ARM_LENGTH));
    	g.rotate(getValue(Value.R_HAND_ANGLE));
    	g.translate(0, getValue(Value.HAND_RADIUS));
    	render.drawPart(g, Part.HAND, 0, 0, getValue(Value.HAND_RADIUS), farSide);
    	g.popMatrix();
    }
    
    private void drawLeftLegs(AGraphics g, Render render, boolean farSide) {
    	g.pushMatrix();
    	g.translate(0, getValue(Value.BODY_LENGTH)/2);
    	g.rotate(getValue(Value.LU_LEG_ANGLE));
    	render.drawPart(g, Part.UPPER_LEG, getValue(Value.U_LEG_LENGTH), getValue(Value.U_LEG_THICKNESS), 0, farSide);
    	g.translate(0, getValue(Value.U_LEG_LENGTH));
    	g.rotate(getValue(Value.LL_LEG_ANGLE));
    	render.drawPart(g, Part.LOWER_LEG, getValue(Value.L_LEG_LENGTH), getValue(Value.L_LEG_THICKNESS), 0, farSide);
    	g.translate(-getValue(Value.L_LEG_THICKNESS)/2, getValue(Value.L_LEG_LENGTH));
    	g.rotate(getValue(Value.L_FOOT_ANGLE));
    	render.drawPart(g, Part.FOOT, getValue(Value.FOOT_LENGTH), getValue(Value.FOOT_THICKNESS), 0, farSide);
    	g.popMatrix();
    }
    
    private void drawRightLegs(AGraphics g, Render render, boolean farSide) {
    	g.pushMatrix();
    	g.translate(0, getValue(Value.BODY_LENGTH)/2);
    	g.rotate(getValue(Value.RU_LEG_ANGLE));
    	render.drawPart(g, Part.UPPER_LEG, getValue(Value.U_LEG_LENGTH), getValue(Value.U_LEG_THICKNESS), 0, farSide);
    	g.translate(0, getValue(Value.U_LEG_LENGTH));
    	g.rotate(getValue(Value.RL_LEG_ANGLE));
    	render.drawPart(g, Part.LOWER_LEG, getValue(Value.L_LEG_LENGTH), getValue(Value.L_LEG_THICKNESS), 0, farSide);
    	g.translate(-getValue(Value.L_LEG_THICKNESS)/2, getValue(Value.L_LEG_LENGTH));
    	g.rotate(getValue(Value.R_FOOT_ANGLE));
    	render.drawPart(g, Part.FOOT, getValue(Value.FOOT_LENGTH), getValue(Value.FOOT_THICKNESS), 0, farSide);
    	g.popMatrix();
    }
    
    public void draw(AGraphics g, Render render) {
    	g.pushMatrix();
    	g.rotate(getValue(Value.BODY_ANGLE));
    	if (rightFacing) {
    		drawLeftArms(g, render, true);
    		drawLeftLegs(g, render, true);
    	} else {
    		drawRightArms(g, render, true);
    		drawRightLegs(g, render, true);
    	}
    	render.drawPart(g, Part.BODY, getValue(Value.BODY_LENGTH), getValue(Value.BODY_THICKNESS), 0, false);
    	g.pushMatrix();
    	g.translate(0, -getValue(Value.BODY_LENGTH)/2);
    	g.rotate(getValue(Value.NECK_ANGLE));
    	g.translate(0, -getValue(Value.NECK_LENGTH));
    	render.drawPart(g, Part.NECK, getValue(Value.NECK_LENGTH), getValue(Value.NECK_RADIUS), getValue(Value.NECK_RADIUS), false);
    	g.rotate(getValue(Value.HEAD_ANGLE));
    	g.translate(0, -getValue(Value.HEAD_RADIUS));
    	render.drawPart(g, Part.HEAD, 0, 0, getValue(Value.HEAD_RADIUS), false);
    	g.popMatrix();
    	if (rightFacing) {
    		drawRightArms(g, render, false);
    		drawRightLegs(g, render, false);
    	} else {
    		drawLeftArms(g, render, false);
    		drawLeftLegs(g, render, false);
    	}
    	g.popMatrix();
    }
    
    public void getBoundingRect(MutableVector2D min, MutableVector2D max) {
    	
    }

    public float getValue(Value part) {
    	return this.value[part.ordinal()];
    }
    
    public void setValue(Value part, float value) {
    	this.value[part.ordinal()] = value;
    }
    
	public final String getName() {
		return name;
	}

	public final void setName(String name) {
		this.name = name;
	}
	public final boolean isRightFacing() {
		return rightFacing;
	}
	public final void setRightFacing(boolean rightFacing) {
		this.rightFacing = rightFacing;
	}
	
	
}
