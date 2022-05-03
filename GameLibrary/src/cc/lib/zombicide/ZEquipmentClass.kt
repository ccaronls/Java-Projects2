package cc.lib.zombicide

import cc.lib.annotation.Keep
import cc.lib.game.Utils
import cc.lib.ui.IButton

/**
 * Created by Chris Caron on 8/28/21.
 */
@Keep
enum class ZEquipmentClass : IButton {
    THROWABLE,
    CONSUMABLE,
    DAGGER,
    SWORD,
    AXE,
    BOW,
    CROSSBOW,
    MAGIC,
    ENCHANTMENT,
    ARMOR,
    SHIELD;

    override fun getTooltipText(): String? {
        return null
    }

    override fun getLabel(): String {
        return Utils.toPrettyString(name)
    }
}