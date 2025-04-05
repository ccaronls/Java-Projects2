package cc.lib.ik

import cc.lib.game.Utils
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.reflector.Reflector

/**
 * Inverse Kinematic Arm
 *
 * @author ccaron
 */
class IKArm : Reflector<IKArm>() {
	private val sections: MutableList<IKHinge> = ArrayList()
	fun addHinge(x: Float, y: Float, vararg constraints: IKConstraint) {
		val s = IKHinge(*constraints)
		s.v.assign(x, y)
		s.prevMag = 0f
		s.nextMag = s.prevMag
		if (sections.size > 0) {
			val p = sections[sections.size - 1]
			val mag = p.v.sub(s).mag()
			s.prevMag = mag
			p.nextMag = s.prevMag
		}
		sections.add(s)
	}

	val numHinges: Int
		get() = sections.size

	fun debug(msg: String?) {
		println(msg)
	}

	private fun checkConstraints(index: Int, dv: MutableVector2D): Boolean {
		val s = getHinge(index)
		var result = false
		for (c in s.constraints) {
			if (c.move(this, index, dv)) {
				result = true
			}
		}
		return result
	}

	fun moveHinge(index: Int, dx: Float, dy: Float) {
		val depth = 5
		val dv = MutableVector2D(dx, dy)
		if (checkConstraints(index, dv)) return
		//debug("moveIKHinge " + index + " delta = " + dx + ", " + dy);
		if (index == 0) {
			// start from root
			moveIKHingeR(index, dv, 1, depth)
		} else if (index == sections.size - 1) {
			// start from end of tail
			moveIKHingeR(index, dv, -1, depth)
		} else {
			// starting somewhere in the middle
			if (!moveIKHingeR(index, dv, 1, depth)) {
				val s = sections[index]
				s.v.addEq(dx, dy)
				moveIKHingeR(index - 1, dv, -1, depth)
			}
		}
	}

	fun moveHingeTo(index: Int, x: Float, y: Float) {
		val dx = x - getX(index)
		val dy = y - getY(index)
		moveHinge(index, dx, dy)
	}

	// recursive method 
	private fun moveIKHingeR(index: Int, dv: MutableVector2D, inc: Int, depth: Int): Boolean {
		if (index < 0 || index >= sections.size || depth < 0) return false
		if (checkConstraints(index, dv)) {
			if (index == 0) {
				return moveIKHingeR(1, dv.scaleEq(-1), 1, depth - 1)
			}
			if (index == sections.size - 1) {
				return moveIKHingeR(index - 1, dv.scaleEq(-1), -1, depth - 1)
			}
		}
		val s = getHinge(index)
		// check for endpoints
		if (index + inc < 0 || index + inc >= sections.size) {
			s.v.addEq(dv)
			return false
		}
		// get the cached magnitude of the
		val len = getMag(index, inc)
		// compute the target point
		val tx = s.v.x + dv.x
		val ty = s.v.y + dv.y

		// compute new arm delta to next section
		var vx = tx - getX(index + inc)
		var vy = ty - getY(index + inc)
		var vm = Math.sqrt((vx * vx + vy * vy).toDouble()).toFloat()
		// normalize
		// compute mag of next delta
		val m = vm - len
		vm = m / vm
		vx *= vm
		vy *= vm
		s.v.assign(tx, ty)
		//s.angle = Float.POSITIVE_INFINITY;
		dv.assign(vx, vy)
		return moveIKHingeR(index + inc, dv, inc, depth - 1)
	}

	fun getV(section: Int): Vector2D {
		return sections[section].v
	}

	fun getX(section: Int): Float {
		return sections[section].v.x
	}

	fun getY(section: Int): Float {
		return sections[section].v.y
	}

	fun getAngle(section: Int): Float {
		return if (section == 0) 0f else getV(section).sub(getV(section - 1)).angleOf()
	}

	fun getMag(section: Int, inc: Int): Float {
		return if (inc < 0) sections[section].prevMag else sections[section].nextMag
	}

	fun getHinge(index: Int): IKHinge {
		return sections[index]
	}

	val hinges: Iterable<IKHinge>
		get() = sections

	fun clear() {
		sections.clear()
	}

	fun findHinge(x: Float, y: Float, radius: Float): Int {
		for (i in sections.indices) {
			val s = sections[i]
			if (Utils.distSqPointPoint(s.v.x, s.v.y, x, y) < radius * radius) {
				return i
			}
		}
		return -1
	}

	companion object {
		init {
			addAllFields(IKArm::class.java)
		}
	}
}
