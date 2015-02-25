package cc.lib.game;

import cc.lib.utils.Reflector;

/**
 * A 2D Figure 
 * @author chriscaron
 *
 */
public class Figure extends Reflector<Figure> {

	public interface Render {

		void drawUpperLeg(AGraphics g, float upperLegLength,
				float upperLegThickness, boolean far);

		void drawLowerLeg(AGraphics g, float lowerLegLength,
				float lowerLegThickness, boolean far);

		void drawFoot(AGraphics g, float footLength, float footThickness,
				boolean far);

		void drawUpperArm(AGraphics g, float upperArmLength,
				float armThickness, boolean far);

		void drawLowerArm(AGraphics g, float lowerArmLength,
				float armThickness, boolean far);

		void drawHand(AGraphics g, float handRadius, boolean far);

		void drawBody(AGraphics g, float bodyLength, float bodyThickness);

		void drawNeck(AGraphics g, float neckRadius);

		void drawHead(AGraphics g, float headRadius);
		
	}
	
	static {
		addAllFields(Figure.class);
	}
	
	public Figure() {}
	public Figure(String name) {
		this.name = name;
	}
	
	String name = "unknown";
	
	// these are values no affected 
    float bodyThickness = 3;
    float bodyLength = 6;
    float upperArmLength = 3;
    float lowerArmLength = 2;
    float armThickness = 1;
    float upperLegLength = 4;
    float upperLegThickness = 1.5f;
    float lowerLegLength = 3;
    float lowerLegThickness = 1;
    float footLength = 2;
    float footThickness = 1;
    float handRadius = 0.5f;
    float neckRadius = 0.5f;
    float headRadius = 1.5f;
    
    float bodyAngle = 0;
    float headAngle = 10;
    float [] legsOffset = { 3, 3 };
    float [] armsOffset = { -3, -3 };
    float [] upperLegsAngle = {-45, 0};
    float [] lowerLegsAngle = {0,0};
    float [] footAngle = {-90,-90};
    float [] upperArmsAngle = {20,-40};
    float [] lowerArmsAngle = {-10,-10};
    
    public void transitionTo(Figure to, float t, Figure result) {
    	result.bodyThickness = (1-t)*bodyThickness + t*to.bodyThickness; 
        result.bodyLength =(1-t)*bodyLength + t*to.bodyLength;
        result.upperArmLength = (1-t)*upperArmLength + t*to.upperArmLength;
        result.lowerArmLength = (1-t)*lowerArmLength + t*to.lowerArmLength;
        result.armThickness = (1-t)*armThickness + t*to.armThickness;
        result.upperLegLength = (1-t)*upperLegLength + t*to.upperLegLength;
        result.upperLegThickness = (1-t)*upperLegThickness + t*to.upperLegThickness;
        result.lowerLegThickness= (1-t)*lowerLegThickness + t*to.lowerLegThickness;
        result.lowerLegLength = (1-t)*lowerLegLength + t*to.lowerLegLength;
        result.footLength = (1-t)*footLength + t*to.footLength;
        result.footThickness = (1-t)*footThickness + t*to.footThickness;
        result.handRadius = (1-t)*handRadius + t*to.handRadius;
        result.neckRadius = (1-t)*neckRadius + t*to.neckRadius;
        result.headRadius = (1-t)*headRadius + t*to.headRadius;
        
        result.bodyAngle = (1-t)*bodyAngle + t*to.bodyAngle;
        result.headAngle = (1-t)*headAngle + t*to.headAngle;
        for (int i=0; i<2; i++) {
	        result.legsOffset[i] = (1-t)*legsOffset[i] + t*to.legsOffset[i];
	        result.armsOffset[i] = (1-t)*armsOffset[i] + t*to.armsOffset[i];
	        result.upperLegsAngle[i] = (1-t)*upperLegsAngle[i] + t*to.upperLegsAngle[i];
	        result.lowerLegsAngle[i] = (1-t)*lowerLegsAngle[i] + t*to.lowerLegsAngle[i];
	        result.footAngle[i] = (1-t)*footAngle[i] + t*to.footAngle[i];
	        result.upperArmsAngle[i] = (1-t)*upperArmsAngle[i] + t*to.upperArmsAngle[i];
	        result.lowerArmsAngle[i] = (1-t)*lowerArmsAngle[i] + t*to.lowerArmsAngle[i];
        }
    }
    
    
    public void draw(AGraphics g, Render render) {
    	g.pushMatrix();
	    	g.rotate(bodyAngle);
	    	boolean far = true;
	    	for (int i=0; i<2; i++) {
		    	// draw legs
	    		g.pushMatrix();
		    		g.translate(0, legsOffset[i]);
		    		g.rotate(upperLegsAngle[i]);
		    		render.drawUpperLeg(g, upperLegLength, upperLegThickness, far);
		    		g.translate(0, upperLegLength);
		    		g.rotate(lowerLegsAngle[i]);
		    		render.drawLowerLeg(g, lowerLegLength, lowerLegThickness, far);
		    		g.translate(-lowerLegThickness/2, lowerLegLength);
		    		g.rotate(footAngle[i]);
		    		render.drawFoot(g, footLength, footThickness, far);
	    		g.popMatrix();
		    	// draw arms
	    		g.pushMatrix();
		    		g.translate(0, armsOffset[i]);
		    		g.rotate(upperArmsAngle[i]);
		    		render.drawUpperArm(g, upperArmLength, armThickness, far);
		    		g.translate(0, upperArmLength);
		    		g.rotate(lowerArmsAngle[i]);
		    		render.drawLowerArm(g, lowerArmLength, armThickness, far);
		    		g.translate(0, lowerArmLength + handRadius);
		    		render.drawHand(g, handRadius, far);
	    		g.popMatrix();
	    		far = false;
	    		if (i == 0)
	    			// draw body between inner and outer legs
	    			render.drawBody(g, bodyLength, bodyThickness);
	    	}
	    	// neck/head
	    	g.translate(0, -(bodyLength/2 + neckRadius));
	    	render.drawNeck(g, neckRadius);
	    	g.rotate(headAngle);
	    	g.translate(0, -(neckRadius + headRadius));
	    	render.drawHead(g, headRadius);
    	g.popMatrix();
    }
    
	public final String getName() {
		return name;
	}
	public final void setName(String name) {
		this.name = name;
	}
	public final float getBodyThickness() {
		return bodyThickness;
	}
	public final void setBodyThickness(float bodyThickness) {
		this.bodyThickness = bodyThickness;
	}
	public final float getBodyLength() {
		return bodyLength;
	}
	public final void setBodyLength(float bodyLength) {
		this.bodyLength = bodyLength;
	}
	public final float getUpperArmLength() {
		return upperArmLength;
	}
	public final void setUpperArmLength(float upperArmLength) {
		this.upperArmLength = upperArmLength;
	}
	public final float getLowerArmLength() {
		return lowerArmLength;
	}
	public final void setLowerArmLength(float lowerArmLength) {
		this.lowerArmLength = lowerArmLength;
	}
	public final float getArmThickness() {
		return armThickness;
	}
	public final void setArmThickness(float armThickness) {
		this.armThickness = armThickness;
	}
	public final float getUpperLegLength() {
		return upperLegLength;
	}
	public final void setUpperLegLength(float upperLegLength) {
		this.upperLegLength = upperLegLength;
	}
	public final float getUpperLegThickness() {
		return upperLegThickness;
	}
	public final void setUpperLegThickness(float upperLegThickness) {
		this.upperLegThickness = upperLegThickness;
	}
	public final float getLowerLegLength() {
		return lowerLegLength;
	}
	public final void setLowerLegLength(float lowerLegLength) {
		this.lowerLegLength = lowerLegLength;
	}
	public final float getLowerLegThickness() {
		return lowerLegThickness;
	}
	public final void setLowerLegThickness(float lowerLegThickness) {
		this.lowerLegThickness = lowerLegThickness;
	}
	public final float getFootLength() {
		return footLength;
	}
	public final void setFootLength(float footLength) {
		this.footLength = footLength;
	}
	public final float getFootThickness() {
		return footThickness;
	}
	public final void setFootThickness(float footThickness) {
		this.footThickness = footThickness;
	}
	public final float getHandRadius() {
		return handRadius;
	}
	public final void setHandRadius(float handRadius) {
		this.handRadius = handRadius;
	}
	public final float getNeckRadius() {
		return neckRadius;
	}
	public final void setNeckRadius(float neckRadius) {
		this.neckRadius = neckRadius;
	}
	public final float getHeadRadius() {
		return headRadius;
	}
	public final void setHeadRadius(float headRadius) {
		this.headRadius = headRadius;
	}
	public final float getBodyAngle() {
		return bodyAngle;
	}
	public final void setBodyAngle(float bodyAngle) {
		this.bodyAngle = bodyAngle;
	}
	public final float getHeadAngle() {
		return headAngle;
	}
	public final void setHeadAngle(float headAngle) {
		this.headAngle = headAngle;
	}
	public final float[] getLegsOffset() {
		return legsOffset;
	}
	public final void setLegsOffset(float[] legsOffset) {
		this.legsOffset = legsOffset;
	}
	public final float[] getArmsOffset() {
		return armsOffset;
	}
	public final void setArmsOffset(float[] armsOffset) {
		this.armsOffset = armsOffset;
	}
	public final float[] getUpperLegsAngle() {
		return upperLegsAngle;
	}
	public final void setUpperLegsAngle(float[] upperLegsAngle) {
		this.upperLegsAngle = upperLegsAngle;
	}
	public final float[] getLowerLegsAngle() {
		return lowerLegsAngle;
	}
	public final void setLowerLegsAngle(float[] lowerLegsAngle) {
		this.lowerLegsAngle = lowerLegsAngle;
	}
	public final float[] getFootAngle() {
		return footAngle;
	}
	public final void setFootAngle(float[] footAngle) {
		this.footAngle = footAngle;
	}
	public final float[] getUpperArmsAngle() {
		return upperArmsAngle;
	}
	public final void setUpperArmsAngle(float[] upperArmsAngle) {
		this.upperArmsAngle = upperArmsAngle;
	}
	public final float[] getLowerArmsAngle() {
		return lowerArmsAngle;
	}
	public final void setLowerArmsAngle(float[] lowerArmsAngle) {
		this.lowerArmsAngle = lowerArmsAngle;
	}
    
    
}
