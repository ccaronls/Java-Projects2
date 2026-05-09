package cc.game.zombicide.android

import cc.game.zombicide.android.ZombicideActivity.CharLock
import cc.game.zombicide.p2p.CommAssign
import cc.lib.reflector.Omit
import cc.lib.reflector.Reflector

class Assignee(val assignee: CommAssign) : Reflector<Assignee>(), Comparable<Assignee> {
	companion object {
		init {
			addAllFields(Assignee::class.java)
		}
	}

	@Omit
	var isAssingedToMe = false

	@Omit
	var lock: CharLock = CharLock(assignee.name, 0)

	val isClickable: Boolean
		get() = isAssingedToMe || assignee.colorId == 0

	override fun compareTo(o: Assignee): Int {
		return assignee.name.compareTo(o.assignee.name)
	}

	val isUnlocked: Boolean
		get() = (assignee.colorId == 0 || isAssingedToMe) && lock.isUnlocked

	override fun equals(o: Any?): Boolean {
		if (this === o) return true
		if (o == null || javaClass != o.javaClass) return false
		val assignee = o as Assignee
		return assignee.assignee.name === assignee.assignee.name
	}

	override fun hashCode(): Int {
		return assignee.name.hashCode()
	}
}