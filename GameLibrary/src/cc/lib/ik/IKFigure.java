package cc.lib.ik;

import cc.lib.game.AGraphics;
import cc.lib.utils.Reflector;

public class IKFigure extends Reflector<IKFigure> {

	static {
		addAllFields(IKFigure.class);
	}
	
	IKArm rightArm = new IKArm();
	IKArm leftArm  = new IKArm();
	IKArm rightLeg = new IKArm();
	IKArm leftLeg = new IKArm();
	
	float bodyAngle = 0;
	float headAngle = 0;
	float bodyLength = 6;
	float bodyThickness = 2.5f;
	float neckRadius=0.5f;
	float headRadius = 1.5f;
	float handRadius = 0.5f;
	
	float upperArmLength = 2.5f;
	float lowerArmLength = 3;
	float upperLegLength = 3;
	float lowerLegLength = 4;
	
	float upperArmThickness = 1;
	float lowerArmThickness = 1;
	float upperLegThickness = 1.5f;
	float lowerLegThickness = 1;
	
	float footLength = 2;
	float footThickness = 0.6f;
	
	public void build() {
		rightArm.addHinge(0, 0, new FixedConstraint());
		rightArm.addHinge(0, -bodyThickness/2);
		rightArm.addHinge(upperArmLength, 0);
		rightArm.addHinge(lowerArmLength, 0);

		leftArm.addHinge(0, 0, new FixedConstraint());
		leftArm.addHinge(0, -bodyThickness/2);
		leftArm.addHinge(upperArmLength, 0);
		leftArm.addHinge(lowerArmLength, 0);

		rightLeg.addHinge(0, 0, new FixedConstraint());
		rightLeg.addHinge(0, bodyThickness/2);
		rightLeg.addHinge(0, upperLegLength);
		rightLeg.addHinge(0, lowerLegLength);
		rightLeg.addHinge(footLength,  0);

		leftLeg.addHinge(0, 0, new FixedConstraint());
		leftLeg.addHinge(0, bodyThickness/2);
		leftLeg.addHinge(0, upperLegLength);
		leftLeg.addHinge(0, lowerLegLength);
		leftLeg.addHinge(footLength,  0);

	}
	
	
	
	public void pick(AGraphics g) {
		
	}
	
	void debugDrawArm(AGraphics g, IKArm arm) {
		g.setColor(g.WHITE);
		g.setLineWidth(3);
		g.begin();
		for (int i=0; i<arm.getNumHinges(); i++) {
			g.vertex(arm.getV(i));
		}
		g.drawLineStrip();
	}

	/**
	 * Left arm/leg are the 'far' limbs and are drawn first, then body, then right arm/leg then head
	 * @param g
	 */
	public void draw(AGraphics g) {
		g.pushMatrix();
    	g.rotate(bodyAngle);
    	// draw legs
    	
    	g.pushMatrix();
    	for (int i=0; i<leftArm.getNumHinges(); i++) {
    		g.translate(leftArm.getV(i));
    		g.rotate(leftArm.getAngle(i));
    		if (i == 2) {
    			drawUpperArm(g, upperArmLength, upperArmThickness, true);
    		} else if (i == 3) {
    			drawLowerArm(g, lowerArmLength, lowerArmThickness, true);
    		}
    	}
    	g.popMatrix();
    	debugDrawArm(g, leftArm);
    	g.pushMatrix();
    	for (int i=0; i<leftLeg.getNumHinges(); i++) {
    		g.translate(leftLeg.getV(i));
    		g.rotate(leftLeg.getAngle(i));
    		if (i == 2) {
    			drawUpperLeg(g, upperLegLength, upperLegThickness, true);
    		} else if (i == 3) {
    			drawLowerLeg(g, lowerLegLength, lowerLegThickness, true);
    		} else if (i == 4) {
    			drawFoot(g, footLength, footThickness, true);
    		}
    	}
    	g.translate(0, handRadius);
    	drawHand(g, handRadius, true);
    	g.popMatrix();
    	drawBody(g, bodyLength, bodyThickness);
    	g.pushMatrix();
    	for (int i=0; i<rightArm.getNumHinges(); i++) {
    		g.translate(rightArm.getV(i));
    		g.rotate(rightArm.getAngle(i));
    		if (i == 2) {
    			drawUpperArm(g, upperArmLength, upperArmThickness, false);
    		} else if (i == 3) {
    			drawLowerArm(g, lowerArmLength, lowerArmThickness, false);
    		}
    	}
    	g.popMatrix();
    	g.pushMatrix();
    	for (int i=0; i<rightLeg.getNumHinges(); i++) {
    		g.translate(rightLeg.getV(i));
    		g.rotate(rightLeg.getAngle(i));
    		if (i == 2) {
    			drawUpperLeg(g, upperLegLength, upperLegThickness, false);
    		} else if (i == 3) {
    			drawLowerLeg(g, lowerLegLength, lowerLegThickness, false);
    		}
    	}
    	g.translate(0, handRadius);
    	drawHand(g, handRadius, false);
    	g.popMatrix();
    	debugDrawArm(g, leftArm);
    	//debugDrawArm(g, rightArm);
    	//debugDrawArm(g, leftLeg);
    	//debugDrawArm(g, rightLeg);
    	g.translate(0, -(bodyLength/2 + neckRadius));
    	drawNeck(g, neckRadius);
    	g.rotate(headAngle);
    	g.translate(0, -(neckRadius + headRadius));
    	drawHead(g, headRadius);
    	g.popMatrix();
	}
	
	/**
	 * Return a figure whose position is (1-t)*this + t*next
	 * @param next figure to transition too
	 * @param t value between 0-1
	 * @return
	 */
	public IKFigure transitionTo(IKFigure next, float t, IKFigure result) {
		moveSection(rightArm, next.rightArm, t, result.rightArm);
		moveSection(leftArm, next.leftArm, t, result.leftArm);
		moveSection(rightLeg, next.rightLeg, t, result.rightLeg);
		moveSection(leftLeg, next.leftLeg, t, result.leftLeg);
		result.bodyAngle = (1-t)*bodyAngle * t*next.bodyAngle;
		result.headAngle = (1-t)*headAngle * t*next.headAngle;
		return result;
	}
	
	private void moveSection(IKArm from, IKArm to, float t, IKArm result) {
		float omt = (1.0f - t);
		int num = rightArm.getNumHinges();
		float x0 = from.getX(num-1);
		float y0 = from.getY(num-1);
		float x1 = to.getX(num-1);
		float y1 = to.getY(num-1);
		float x = omt*x0 + t*x1;
		float y = omt*y0 + t*y1;
		result.moveHingeTo(num-1, x, y);
		
	}
	
	protected void drawUpperLeg(AGraphics g, float length, float thickness, boolean far) {
    	if (far)
    		g.setColor(g.LIGHT_GRAY.darkened(0.5f));
    	else
    		g.setColor(g.LIGHT_GRAY);
    	g.begin();
    	g.vertex(-thickness/2, 0);
    	g.vertex(-thickness/2,length);
    	g.vertex(thickness/2, length);
    	g.vertex(thickness/2, 0);
    	g.drawTriangleFan();
    }

    protected void drawLowerLeg(AGraphics g, float length, float thickness, boolean far) {
    	if (far)
    		g.setColor(g.DARK_GRAY.darkened(0.5f));
    	else
    		g.setColor(g.DARK_GRAY);
    	g.begin();
    	g.vertex(-thickness/2, 0);
    	g.vertex(-thickness/2,length);
    	g.vertex(thickness/2, length);
    	g.vertex(thickness/2, 0);
    	g.drawTriangleFan();
    }

    protected void drawUpperArm(AGraphics g, float length, float thickness, boolean far) {
    	if (far)
    		g.setColor(g.MAGENTA.darkened(0.5f));
    	else
    		g.setColor(g.MAGENTA);
    	g.begin();
    	g.vertex(-thickness/2, 0);
    	g.vertex(-thickness/2,length);
    	g.vertex(thickness/2, length);
    	g.vertex(thickness/2, 0);
    	g.drawTriangleFan();
    }

    protected void drawLowerArm(AGraphics g, float length, float thickness, boolean far) {
    	if (far)
    		g.setColor(g.GREEN.darkened(0.5f));
    	else
    		g.setColor(g.GREEN);
    	g.begin();
    	g.vertex(-thickness/2, 0);
    	g.vertex(-thickness/2,length);
    	g.vertex(thickness/2, length);
    	g.vertex(thickness/2, 0);
    	g.drawTriangleFan();
    }

    protected void drawFoot(AGraphics g, float length, float thickness, boolean far) {
    	if (far)
    		g.setColor(g.CYAN.darkened(0.5f));
    	else
    		g.setColor(g.CYAN);
    	g.begin();
    	g.vertex(-thickness/2, 0);
    	g.vertex(-thickness/2,length);
    	g.vertex(thickness/2, length);
    	g.vertex(thickness/2, 0);
    	g.drawTriangleFan();
    }

    protected void drawHand(AGraphics g, float radius, boolean far) {
    	if (far)
    		g.setColor(g.YELLOW.darkened(0.5f));
    	else
    		g.setColor(g.YELLOW);
    	g.drawDisk(0,0,radius);
    }

    protected void drawNeck(AGraphics g, float radius) {
    	g.setColor(g.YELLOW);
    	g.drawDisk(0, 0, radius);
    }

    protected void drawHead(AGraphics g, float radius) {
    	g.setColor(g.RED);
    	g.drawDisk(0, 0, radius);
    }

    protected void drawBody(AGraphics g, float length, float thickness) {
    	g.setColor(g.BLUE);
    	g.begin();
    	g.vertex(-thickness/2, -length/2);
    	g.vertex(thickness/2, -length/2);
    	g.vertex(thickness/2, length/2);
    	g.vertex(-thickness/2, length/2);
    	g.drawTriangleFan();
    }
	
}
