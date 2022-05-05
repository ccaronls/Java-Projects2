package cc.lib.zombicide

import cc.lib.annotation.Keep
import cc.lib.game.Utils

@Keep
enum class ZArmorType(override val equipmentClass: ZEquipmentClass, val slotType: ZEquipSlotType, val dieRollToBlock: Int, val specialAbilityDescription: String) : ZEquipmentType {
    LEATHER(ZEquipmentClass.ARMOR, ZEquipSlotType.BODY, 5, ""),
    CHAINMAIL(ZEquipmentClass.ARMOR, ZEquipSlotType.BODY, 4, ""),
    PLATE(ZEquipmentClass.ARMOR, ZEquipSlotType.BODY, 3, ""),
    SHIELD(ZEquipmentClass.SHIELD, ZEquipSlotType.HAND, 4, ""),
    DWARVEN_SHIELD(ZEquipmentClass.SHIELD, ZEquipSlotType.HAND, 4, "TODO: Protects against abomination"),
    SHIELD_OF_AGES(ZEquipmentClass.SHIELD, ZEquipSlotType.HAND, 4, "TODO: Gain the shove skill") {
        override val skillsWhileEquipped: List<ZSkill>
            get() = Utils.toList(ZSkill.Shove)
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