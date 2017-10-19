package cc.lib.game;

import java.util.ArrayList;
import java.util.List;

import cc.lib.math.CMath;
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

	public static abstract class AConstraint extends Reflector<AConstraint> {

        /**
         * if dv can be applied without violation then return false.
         * Otherwise adjust arm.section(index) as much as possible without violating constraint
         * and dv will be modified with remainder. retun true in this case to cease recursion
         *
         * @param arm
         * @param index
         * @param dv
         * @return
         */
        abstract boolean constrain(IKArm arm, int index, MutableVector2D dv);
    };

    public static class FixedConstraint extends AConstraint {
        @Override
        boolean constrain(IKArm arm, int index, MutableVector2D dv) {
            if (dv.magSquared() > 0.001) {
                return true;
            }
            return false;
        }
    }

    /**
     * Constrain angle from a section to its previous hinge
     */
    public static class AngleConstraint extends AConstraint {

        static {
            addAllFields(AngleConstraint.class);
        }

        public AngleConstraint() {
            this(0,0);
        }

        final float minAngle, maxAngle;

        @Omit
        public float lastMinAngle=0;
        @Omit
        public float lastMaxAngle=0;

        /**
         *
         * @param minAngle [-180, +180]
         * @param maxAngle [-180, +180]
         */
        public AngleConstraint(float minAngle, float maxAngle) {
            this.minAngle = minAngle;
            this.maxAngle = maxAngle;
        }

        @Override
        boolean constrain(IKArm arm, int index, MutableVector2D dv) {
            if (index <= 0)
                return false;
            Section s0 = arm.getSection(index);
            Section p0  = arm.getSection(index-1);
            Vector2D tv = s0.v.sub(p0.v);
            float angle = tv.angleOf();

            float min =minAngle;
            float max =maxAngle;

            if (index > 1) {
                float r2 = arm.getAngle(index-2);
                min += r2;
                max += r2;
            }

            lastMinAngle = min;
            lastMaxAngle = max;

            if (angle < min || angle > max) {
                float rads = angle < min ? min * CMath.DEG_TO_RAD : max * CMath.DEG_TO_RAD;
                float cosx = (float) Math.cos(rads);
                float sinx = (float) Math.sin(rads);
                s0.v.set(p0.v.X()+cosx*p0.nmag, p0.v.Y()+sinx*p0.nmag);
                dv.subEq(s0.v);
                return true;
            }
            return false;
        }
    }

    public static class Section extends Reflector<Section> {
        public MutableVector2D v = new MutableVector2D();
        public float pmag, nmag; // cache magnitudes to the next and previous sections
        public float angle = Float.POSITIVE_INFINITY;
        public AConstraint [] constraints;
    }

    private List<Section> sections = new ArrayList<Section>(); 
    
    public IKArm() {}
    
    public void addSection(float x, float y, AConstraint...constraints) {
        Section s = new Section();
        s.v.set(x, y);
        s.nmag = s.pmag = 0;
        s.constraints=constraints;
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

    private boolean checkConstraints(int index, MutableVector2D dv) {
        Section s = getSection(index);
        for (AConstraint c : s.constraints) {
            if (c.constrain(this, index, dv)) {
                return true;
            }
        }
        return false;
    }

    public void moveSection(int index, float dx, float dy) {
        MutableVector2D dv = new MutableVector2D(dx, dy);
        if (checkConstraints(index, dv))
            return;
        //debug("moveSection " + index + " delta = " + dx + ", " + dy);
        if (index == 0) {
            // start from root
            moveSectionR(index, dv, 1);
        } else if (index == sections.size()-1) {
            // start from end of tail
            moveSectionR(index, dv, -1);
        } else {
            // starting somewhere in the middle
            if (!moveSectionR(index, dv, 1)) {
                Section s = sections.get(index);
                s.v.subEq(dx, dy);
                moveSectionR(index, dv, -1);
            }
        }
    }
    
    public void moveSectionTo(int index, float x, float y) {
    	float dx = x - getX(index);
    	float dy = y - getY(index);
    	moveSection(index, dx, dy);
    }
    
    // recursive method 
    private boolean moveSectionR(int index, MutableVector2D dv, int inc) {
        if (index < 0 || index >= sections.size())
            return false;
        if (checkConstraints(index, dv)) {
            if (index > 0) {
                moveSectionR(index-1, dv.scaleEq(-1), -1);
            }
            if (index < sections.size()-1) {
                moveSectionR(index+1, dv.scaleEq(-1), 1);
            }
            return true;
        }
        Section s = getSection(index);
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
        s.angle = Float.POSITIVE_INFINITY;
        dv.set(vx, vy);
        return moveSectionR(index+inc, dv,inc);
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
    	if (s.angle == Float.POSITIVE_INFINITY) {
            s.angle = getV(section).sub(getV(section-1)).angleOf();
    	}
    	return s.angle;
    }

    public float getMag(int section, int inc) {
        if (inc < 0)
            return sections.get(section).nmag;
        return sections.get(section).pmag;
    }

    public final Section getSection(int index) {
        return sections.get(index);
    }

    public final Iterable<Section> getSections() {
        return sections;
    }
}
