package cc.lib.ik;

import cc.lib.math.MutableVector2D;
import cc.lib.reflector.Reflector;

/**
 * Created by chriscaron on 10/20/17.
 */

public abstract class IKConstraint extends Reflector<IKConstraint> {

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
    abstract boolean move(IKArm arm, int index, MutableVector2D dv);
}
