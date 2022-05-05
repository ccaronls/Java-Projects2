package cc.lib.zombicide

import cc.lib.utils.Reflector

data class ZWeaponStat(var actionType: ZActionType = ZActionType.MOVE,
                       var attackType: ZAttackType = ZAttackType.BLADE,
                       @JvmField var dieRollToOpenDoor: Int = 0,
                       @JvmField var minRange: Int = 0,
                       @JvmField var maxRange: Int = 0,
                       @JvmField var numDice: Int = 0,
                       @JvmField var dieRollToHit: Int = 0,
                       @JvmField var damagePerHit: Int = 0) : Reflector<ZWeaponStat>() {

    companion object {
        init {
            addAllFields(ZWeaponStat::class.java)
        }
    }

    fun copy(): ZWeaponStat {
        return ZWeaponStat(actionType, attackType, dieRollToOpenDoor, minRange, maxRange, numDice, dieRollToHit, damagePerHit)
    }
}