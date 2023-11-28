package cc.game.zombicide.android

import cc.game.zombicide.android.ZombicideActivity.CharLock
import cc.lib.utils.Reflector
import cc.lib.zombicide.ZPlayerName

class Assignee(val name: ZPlayerName = ZPlayerName.Nelly, var userName: String = "??", var color: Int = -1, var checked: Boolean = false) : Reflector<Assignee>(), Comparable<Assignee> {
	companion object {
		init {
			addAllFields(Assignee::class.java)
		}
	}

	@JvmField
	@Omit
	var isAssingedToMe = false

	@JvmField
    @Omit
	var lock: CharLock?=null

	constructor(cl: CharLock) : this(cl.player) {
		lock = cl
	}

	override fun compareTo(o: Assignee): Int {
		return name.compareTo(o.name)
	}

	val isUnlocked: Boolean
		get() = (color < 0 || isAssingedToMe) && lock?.isUnlocked == true

	override fun equals(o: Any?): Boolean {
		if (this === o) return true
		if (o == null || javaClass != o.javaClass) return false
		val assignee = o as Assignee
		return name === assignee.name
	}

	override fun hashCode(): Int {
		return name.hashCode()
	}
}