package cc.lib.zombicide

import cc.lib.annotation.Keep
import cc.lib.game.Utils
import cc.lib.ui.IButton

@Keep
enum class ZEquipSlot(val shorthand: String) : IButton {
    LEFT_HAND("LH"),
    BODY("Bo"),
    RIGHT_HAND("RH"),
    BACKPACK("BP");

    override fun getTooltipText(): String? {
        return null
    }

    override fun getLabel(): String {
        return Utils.toPrettyString(name)
    }

    companion object {
        @JvmStatic
        fun wearableValues(): Array<ZEquipSlot> {
            return Utils.toArray(LEFT_HAND, BODY, RIGHT_HAND)
        }
    }
}