package cc.lib.zombicide

import cc.lib.annotation.Keep


@Keep
enum class ZArmorType(override val equipmentClass: ZEquipmentClass, val slotType: ZEquipSlotType, val dieRollToBlock: Int, val specialAbilityDescription: String?) : ZEquipmentType {
    LEATHER(ZEquipmentClass.ARMOR, ZEquipSlotType.BODY, 5, null),
    CHAINMAIL(ZEquipmentClass.ARMOR, ZEquipSlotType.BODY, 4, null),
    PLATE(ZEquipmentClass.ARMOR, ZEquipSlotType.BODY, 3, null),
    SHIELD(ZEquipmentClass.SHIELD, ZEquipSlotType.HAND, 4, null),
    DWARVEN_SHIELD(ZEquipmentClass.SHIELD, ZEquipSlotType.HAND, 4, "Protects against abomination"),
    SHIELD_OF_AGES(ZEquipmentClass.SHIELD, ZEquipSlotType.HAND, 4, "Gain the shove skill") {
        override val skillsWhileEquipped: List<ZSkill>
            get() = listOf(ZSkill.Shove)
    };

    override fun getDieRollToBlock(type: ZZombieType): Int {
        when (this) {
            DWARVEN_SHIELD -> {
            }
            else           -> if (type.ignoresArmor) return 0
        }
        return dieRollToBlock
    }

    override fun create(): ZArmor {
        return ZArmor(this)
    }

    override fun isActionType(type: ZActionType): Boolean {
        return false
    }

    override val isShield: Boolean
        get() = equipmentClass === ZEquipmentClass.SHIELD
}