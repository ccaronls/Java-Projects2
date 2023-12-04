package cc.lib.ik;

import cc.lib.game.IVector2D;
import cc.lib.math.MutableVector2D;
import cc.lib.reflector.Reflector;

/**
 * Created by chriscaron on 10/20/17.
 */

public class IKHinge extends Reflector<IKHinge> implements IVector2D {

    static {
        addAllFields(IKHinge.class);
    }

    public final MutableVector2D v = new MutableVector2D();
    float pmag, nmag; // cache magnitudes to the next and previous sections
    public final IKConstraint [] constraints;

    public IKHinge() {
        this(new IKConstraint[0]);
    }

    public IKHinge(IKConstraint...cons) {
        this.constraints = cons;
    }

    @Override
    public float getX() {
        return v.getX();
    }

    @Override
    public float getY() {
        return v.getY();
    }

    /**
     * Cached distance to the previous hinge
     * @return
     */
    public final float getPrevMag() {
        return pmag;
    }

    /**
     * Cached distance to the next hinge
     * @return
     */
    public final float getNextMag() {
        return nmag;
    }

}