package cc.lib.zombicide

import cc.lib.annotation.Keep
import cc.lib.game.Utils


@Keep
enum class ZArmorType(override val equipmentClass: ZEquipmentClass, val slotType: ZEquipSlotType, val dieRollToBlock: Int, val specialAbilityDescription: String?) : ZEquipmentType {
    LEATHER(ZEquipmentClass.ARMOR, ZEquipSlotType.BODY, 5, null),
    CHAIN_MAIL(ZEquipmentClass.ARMOR, ZEquipSlotType.BODY, 4, null),
    PLATE(ZEquipmentClass.ARMOR, ZEquipSlotType.BODY, 3, null),
    SHIELD(ZEquipmentClass.SHIELD, ZEquipSlotType.HAND, 4, null),
    DWARVEN_SHIELD(ZEquipmentClass.SHIELD, ZEquipSlotType.HAND, 4, "Allow armor rolls against Abominations"),
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

	override fun getTooltipText(): String {
		var txt = if (isShield) {
			return "If no armor equipped then get $dieRollToBlock+ armor rating. Otherwise adds an additional roll to block after existing armor rolls"
		} else {
			"Armor: $dieRollToBlock+"
		}
		specialAbilityDescription?.let {
			txt += "\n$it"
		}
		return Utils.wrapTextWithNewlines(txt, 32)
	}
}