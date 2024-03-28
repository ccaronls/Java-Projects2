package cc.lib.zombicide

import cc.lib.utils.Table

/**
 * Created by Chris Caron on 2/18/24.
 */
class ZFamiliarLink(override val type: ZFamiliarType) : ZEquipment<ZFamiliarType>() {

	companion object {
		init {
			addAllFields(ZFamiliarLink::class.java)
		}
	}

	override val slotType: ZEquipSlotType = ZEquipSlotType.BACKPACK

	override fun isEquippable(c: ZCharacter): Boolean {
		TODO("Not yet implemented")
	}

	override fun getCardInfo(c: ZCharacter, game: ZGame): Table {
		TODO("Not yet implemented")
	}

	var familiar: ZFamiliar? = null
}