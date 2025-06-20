package cc.lib.zombicide

import cc.lib.reflector.Reflector

data class ZWeaponStat(var actionType: ZActionType = ZActionType.MOVE,
                       var attackType: ZAttackType = ZAttackType.BLADE,
                       var dieRollToOpenDoor: Int = 0,
                       var minRange: Int = 0,
                       var maxRange: Int = 0,
                       var numDice: Int = 0,
                       var dieRollToHit: Int = 0,
                       var damagePerHit: Int = 0) : Reflector<ZWeaponStat>() {

    companion object {
        init {
            addAllFields(ZWeaponStat::class.java)
        }
    }

	fun copy(): ZWeaponStat {
		return ZWeaponStat(
			actionType,
			attackType,
			dieRollToOpenDoor,
			minRange,
			maxRange,
			numDice,
			dieRollToHit,
			damagePerHit
		)
	}

	val dieRollToOpenDoorPercent: Int
		get() = if (dieRollToOpenDoor == 0) 0 else (7 - dieRollToOpenDoor) * 100 / 6

	val dieRollToHitPercent: Int
		get() = (7 - dieRollToHit) * 100 / 6

	val rangeString: String = when (maxRange) {
		minRange -> "$minRange"
		Int.MAX_VALUE -> "$minRange+"
		else -> "$minRange-$maxRange"
	}
}