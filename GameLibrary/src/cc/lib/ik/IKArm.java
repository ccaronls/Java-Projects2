package cc.lib.ik;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.utils.Reflector;

/**
 * Inverse Kinematic Arm
 * 
 * @author ccaron
 *
 */
public final class IKArm extends Reflector<IKArm> {

	static {
		addAllFields(IKArm.class);
	}

    private List<IKHinge> sections = new ArrayList<IKHinge>(); 
    
    public IKArm() {}
    
    public void addHinge(float x, float y, IKConstraint...constraints) {
        IKHinge s = new IKHinge(constraints);
        s.v.set(x, y);
        s.nmag = s.pmag = 0;
        if (sections.size() > 0) {
            IKHinge p = sections.get(sections.size()-1);
            float mag = p.v.sub(s).mag();
            s.pmag = mag;
            p.nmag = s.pmag;
        }
        sections.add(s);
    }
    
    public int getNumHinges() {
        return sections.size();
    }
    
    public void debug(String msg) {
        System.out.println(msg);
    }

    private boolean checkConstraints(int index, MutableVector2D dv) {
        IKHinge s = getHinge(index);
        boolean result = false;
        for (IKConstraint c : s.constraints) {
            if (c.move(this, index, dv)) {
                result = true;
            }
        }
        return result;
    }

    public void moveHinge(int index, float dx, float dy) {
        int depth = 5;
        MutableVector2D dv = new MutableVector2D(dx, dy);
        if (checkConstraints(index, dv))
            return;
        //debug("moveIKHinge " + index + " delta = " + dx + ", " + dy);
        if (index == 0) {
            // start from root
            moveIKHingeR(index, dv, 1, depth);
        } else if (index == sections.size()-1) {
            // start from end of tail
            moveIKHingeR(index, dv, -1, depth);
        } else {
            // starting somewhere in the middle
            if (!moveIKHingeR(index, dv, 1, depth)) {
                IKHinge s = sections.get(index);
                s.v.addEq(dx, dy);
                moveIKHingeR(index-1, dv, -1, depth);
            }
        }
    }
    
    public void moveHingeTo(int index, float x, float y) {
    	float dx = x - getX(index);
    	float dy = y - getY(index);
    	moveHinge(index, dx, dy);
    }
    
    // recursive method 
    private boolean moveIKHingeR(int index, MutableVector2D dv, int inc, int depth) {
        if (index < 0 || index >= sections.size() || depth<0)
            return false;
        if (checkConstraints(index, dv)) {
            if (index == 0) {
                return moveIKHingeR(1, dv.scaleEq(-1), 1, depth-1);
            }
            if (index == sections.size()-1) {
                return moveIKHingeR(index-1, dv.scaleEq(-1), -1, depth-1);
            }
        }
        IKHinge s = getHinge(index);
        // check for endpoints
        if (index + inc < 0 || index + inc >= sections.size()) {
            s.v.addEq(dv);
            return false;
        }
        // get the cached magnitude of the
        float len = getMag(index, inc);
        // compute the target point
        float tx = s.v.X() + dv.X();
        float ty = s.v.Y() + dv.Y();
        
        // compute new arm delta to next section
        float vx = tx - getX(index+inc);
        float vy = ty - getY(index+inc);
        float vm = (float)Math.sqrt(vx*vx + vy*vy);
        // normalize
        // compute mag of next delta
        final float m = vm - len;
        vm = m / vm;
        vx *= vm;
        vy *= vm;
        s.v.set(tx, ty);
        //s.angle = Float.POSITIVE_INFINITY;
        dv.set(vx, vy);
        return moveIKHingeR(index+inc, dv,inc, depth-1);
    }
    
    public Vector2D getV(int section) {
    	return sections.get(section).v;
    }
    
    public float getX(int section) {
        return sections.get(section).v.X();
    }

    public float getY(int section) {
        return sections.get(section).v.Y();
    }
    
    public float getAngle(int section) {
    	if (section == 0)
    		return 0;
        return getV(section).sub(getV(section-1)).angleOf();
    }

    public float getMag(int section, int inc) {
        if (inc < 0)
            return sections.get(section).pmag;
        return sections.get(section).nmag;
    }

    public final IKHinge getHinge(int index) {
        return sections.get(index);
    }

    public final Iterable<IKHinge> getHinges() {
        return sections;
    }

    public final void clear() {
        sections.clear();
    }

    public final int findHinge(float x, float y, float radius) {
        for (int i=0; i<sections.size(); i++) {
            IKHinge s = sections.get(i);
            if (Utils.distSqPointPoint(s.v.X(), s.v.Y(), x, y) < radius*radius) {
                return i;
            }
        }
        return -1;
    }
}
