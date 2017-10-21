package cc.lib.ik;

import cc.lib.math.CMath;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;

/**
 * Constrain angle from a section to its previous hinge.
 * all unit in (0,360) degrees
 */
public class AngleConstraint extends IKConstraint {

    static {
        addAllFields(AngleConstraint.class);
    }

    public AngleConstraint() {
        this(0,0);
    }

    private float startAngle, sweep;

    @Omit
    public float lastStartAngle=0;

    /**
     *
     * @param startAngle
     * @param sweep
     */
    public AngleConstraint(float startAngle, float sweep) {
        this.startAngle = startAngle;
        this.sweep = sweep;
    }

    public AngleConstraint(float sweep) {
        this(-1, sweep);
    }

    @Override
    boolean constrain(IKArm arm, int index, MutableVector2D dv) {
        if (index >= arm.getNumHinges()-1)
            return false;
        IKHinge s0 = arm.getHinge(index);
        IKHinge n0  = arm.getHinge(index+1);
        Vector2D tv = n0.v.sub(s0.v);
        float angle = tv.angleOf();

        if (startAngle < 0) {
            startAngle = angle - sweep/2;
            if (startAngle < 0)
                startAngle += 360;
        }

        float st = startAngle;

        if (index > 0) {
            //float r2 = arm.getAngle(index-1);
            IKHinge p0 = arm.getHinge(index-1);
            float pAng = s0.v.sub(p0).angleOf();
            st += pAng;
            if (st > 360)
                st -= 360;
        }

        lastStartAngle = st;

        float maxAngle = st+sweep;
        if (maxAngle > 360) {
            maxAngle -= 360;
            if (angle >= st || angle <= maxAngle)
                return false;
        } else {
            if (angle >= st && angle <= maxAngle)
                return false;
        }

        System.out.println("AngleConstraint idx("+index+") dv(" + dv + ")");

        float rads = angle < st ? st * CMath.DEG_TO_RAD : maxAngle * CMath.DEG_TO_RAD;
        float cosx = (float) Math.cos(rads);
        float sinx = (float) Math.sin(rads);
        Vector2D nv = new Vector2D(n0.v.X()-cosx*s0.nmag, n0.v.Y()-sinx*s0.nmag);
        dv.set(nv.sub(s0));
        System.out.println("                               nextDv(" + dv + ")");
        s0.v.set(nv);
        return true;
    }

    public final float getSweep() {
        return sweep;
    }

    public void setSweep(float sweep) {
        this.sweep = sweep;
    }
}
