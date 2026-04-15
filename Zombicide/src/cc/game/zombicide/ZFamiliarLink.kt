package cc.game.zombicide

import cc.lib.utils.Table
import cc.lib.utils.TableImage
import cc.lib.utils.prettify

/**
 * Created by Chris Caron on 2/18/24.
 */
class ZFamiliarLink(override val type: ZFamiliarType = ZFamiliarType.GOG) : ZEquipment<ZFamiliarType>() {

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
		return Table().setNoBorder().addColumn(
			getLabel() + " (${type.weaponType.prettify()})",
			TableImage(type.imageId, 128),
			*(type.skills.map { it.prettify() }.toTypedArray()))
	}

}