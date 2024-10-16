package cc.lib.zombicide

import cc.lib.annotation.Keep

import cc.lib.ui.IButton
import cc.lib.utils.prettify

/**
 * Created by Chris Caron on 8/28/21.
 */
@Keep
enum class ZEquipmentClass : IButton {
    AHHHH,
	THROWABLE,
	CONSUMABLE,
	DAGGER,
	SPEAR,
	SWORD,
	AXE,
	BOW,
	CROSSBOW,
	MAGIC,
	ENCHANTMENT,
	ARMOR,
	SHIELD,
	FAMILIAR;

	override fun getTooltipText(): String? {
		return null
	}

	override fun getLabel(): String {
		return name.prettify()
	}
}