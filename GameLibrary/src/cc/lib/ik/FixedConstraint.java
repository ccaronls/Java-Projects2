package cc.lib.ik;

import cc.lib.math.MutableVector2D;

public class FixedConstraint extends IKConstraint {
    @Override
    boolean move(IKArm arm, int index, MutableVector2D dv) {
        if (dv.magSquared() > 0.001) {
            return true;
        }
        return false;
    }
}