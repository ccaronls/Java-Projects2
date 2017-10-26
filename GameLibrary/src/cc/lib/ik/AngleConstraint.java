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
    boolean move(IKArm arm, int index, MutableVector2D dv) {
        if (index >= arm.getNumHinges()-1)
            return false;
        IKHinge i = arm.getHinge(index); // this hinge
        IKHinge n  = arm.getHinge(index+1); // next hinge
        Vector2D a = n.v.sub(i.v.add(dv)); // a is the vector that will result once applying dv

        float angle = a.angleOf();

        if (startAngle < 0) {
            startAngle = angle - sweep/2;
            if (startAngle < 0)
                startAngle += 360;
        }

        float st = startAngle;

        // adjust for the previous point if it exists, otherwise this is a hard angle
        if (index > 0) {
            //float r2 = arm.getAngle(index-1);
            IKHinge p = arm.getHinge(index-1);
            float pAng = i.v.sub(p).angleOf();
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

        float dMax= Math.abs(maxAngle-angle);
        if (dMax > 180)
            dMax=Math.abs(dMax - 360);

        float dSt = Math.abs(st-angle);
        if (dSt > 180)
            dSt=Math.abs(dSt-360);

        float ang = dSt < dMax ? st : maxAngle;
        float rads = CMath.DEG_TO_RAD * ang;

        System.out.println("AngleConstraint Hit idx("+index+") dv(" + dv + ")");

        float cosx = (float) Math.cos(rads);
        float sinx = (float) Math.sin(rads);
        Vector2D a2 = new Vector2D(cosx*i.nmag, sinx*i.nmag);
        // add too dv the difference between a and a2
        //dv.addEq(a2.sub(a));
        i.v.set(n.v.sub(a2));
        return true;

        /*
        dv.set(nv.sub(s0));
        System.out.println("                               nextDv(" + dv + ")");
        i.v.set(nv);
        return true;*/
    }

    public final float getSweep() {
        return sweep;
    }

    public void setSweep(float sweep) {
        this.sweep = sweep;
    }
}
