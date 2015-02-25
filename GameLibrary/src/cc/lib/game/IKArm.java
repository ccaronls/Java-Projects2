package cc.lib.game;

import java.util.ArrayList;
import java.util.List;

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
		addField(Section.class, "v");
		addField(Section.class, "pmag");
		addField(Section.class, "nmag");
		addField(Section.class, "fixed");
	}
	
    public static class Section extends Reflector<Section> {
        MutableVector2D v = new MutableVector2D(); 
        float pmag, nmag; // cache magnitudes to the next and previous sections
        boolean fixed;
        float angle = 0;
    }
    
    private List<Section> sections = new ArrayList<Section>(); 
    
    public IKArm() {}
    
    public void addSection(float x, float y, boolean fixed) {
        Section s = new Section();
        s.v.set(x, y);
        s.nmag = s.pmag = 0;
        s.fixed = fixed;
        if (sections.size() > 0) {
            Section p = sections.get(sections.size()-1);
            float dx = s.v.X() - p.v.X();
            float dy = s.v.Y() - p.v.Y();
            s.nmag = (float)Math.sqrt(dx*dx + dy*dy);
            sections.get(sections.size()-1).pmag = s.nmag;
        }
        sections.add(s);
    }
    
    public int getNumSections() {
        return sections.size();
    }
    
    public void debug(String msg) {
        System.out.println(msg);
    }
    
    public void moveSection(int index, float dx, float dy) {
        //debug("moveSection " + index + " delta = " + dx + ", " + dy);
        if (index == 0) {
            // start from root
            moveSectionR(index, dx, dy, 1);
        } else if (index == sections.size()-1) {
            // start from end of tail
            moveSectionR(index, dx, dy, -1);
        } else {
            // starting somewhere in the middle
            moveSectionR(index, dx, dy, 1);
            Section s = sections.get(index);
            s.v.subEq(dx, dy);
            moveSectionR(index, dx, dy, -1);
        }
    }
    
    public void moveSectionTo(int index, float x, float y) {
    	float dx = x - getX(index);
    	float dy = y - getY(index);
    	moveSection(index, dx, dy);
    }
    
    // recursive method 
    private void moveSectionR(int index, float dx, float dy, int inc) {
        Section s = sections.get(index);
        if (index + inc < 0 || index + inc >= sections.size()) {
            s.v.addEq(dx, dy);
            return;
        }
        // get the cached magnitude of the 
        float len = getMag(index, inc);
        // compute the target point
        float tx = s.v.X() + dx;
        float ty = s.v.Y() + dy;
        
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
        s.angle = 0;
        moveSectionR(index+inc, vx, vy,inc);
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
    	Section s = sections.get(section);
    	if (s.angle == 0) {
    		float dx = getX(section) - getX(section-1);
    		float dy = getY(section) - getY(section-1);
    		s.angle = Utils.RAD_TO_DEG * (float)Math.atan2(dy, dx);
    	}
    	return s.angle;
    }

    public float getMag(int section, int inc) {
        if (inc < 0)
            return sections.get(section).nmag;
        return sections.get(section).pmag;
    }
}
