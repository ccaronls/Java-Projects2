package cc.lib.ik

import cc.lib.math.CMath
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.reflector.Omit

/**
 * Constrain angle from a section to its previous hinge.
 * all unit in (0,360) degrees
 */
class AngleConstraint(private var startAngle: Float = 0f, var sweep: Float = 0f) : IKConstraint() {

	@Omit
	var lastStartAngle = 0f

	constructor(sweep: Float) : this(-1f, sweep)

	public override fun move(arm: IKArm, index: Int, dv: MutableVector2D): Boolean {
		if (index >= arm.numHinges - 1) return false
		val i = arm.getHinge(index) // this hinge
		val n = arm.getHinge(index + 1) // next hinge
		val a: Vector2D = n.v.sub(i.v.add(dv)) // a is the vector that will result once applying dv
		val angle = a.angleOf()
		if (startAngle < 0) {
			startAngle = angle - sweep / 2
			if (startAngle < 0) startAngle += 360f
		}
		var st = startAngle

		// adjust for the previous point if it exists, otherwise this is a hard angle
		if (index > 0) {
			//float r2 = arm.getAngle(index-1);
			val p = arm.getHinge(index - 1)
			val pAng = i.v.sub(p).angleOf()
			st += pAng
			if (st > 360) st -= 360f
		}
		lastStartAngle = st
		var maxAngle = st + sweep
		if (maxAngle > 360) {
			maxAngle -= 360f
			if (angle >= st || angle <= maxAngle) return false
		} else {
			if (angle >= st && angle <= maxAngle) return false
		}
		var dMax = Math.abs(maxAngle - angle)
		if (dMax > 180) dMax = Math.abs(dMax - 360)
		var dSt = Math.abs(st - angle)
		if (dSt > 180) dSt = Math.abs(dSt - 360)
		val ang = if (dSt < dMax) st else maxAngle
		val rads = CMath.DEG_TO_RAD * ang
		println("AngleConstraint Hit idx($index) dv($dv)")
		val cosx = Math.cos(rads.toDouble()).toFloat()
		val sinx = Math.sin(rads.toDouble()).toFloat()
		val a2 = Vector2D(cosx * i.nextMag, sinx * i.nextMag)
		// add too dv the difference between a and a2
		//dv.addEq(a2.sub(a));
		i.v.assign(n.v.sub(a2))
		return true

		/*
        dv.set(nv.sub(s0));
        System.out.println("                               nextDv(" + dv + ")");
        i.v.set(nv);
        return true;*/
	}

	companion object {
		init {
			addAllFields(AngleConstraint::class.java)
		}
	}
}
