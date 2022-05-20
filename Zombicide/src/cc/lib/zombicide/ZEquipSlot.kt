package cc.lib.zombicide

import cc.lib.annotation.Keep

import cc.lib.ui.IButton
import cc.lib.utils.prettify

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
        return prettify(name)
    }

    companion object {
        @JvmStatic
        fun wearableValues(): Array<ZEquipSlot> {
            return arrayOf(LEFT_HAND, BODY, RIGHT_HAND)
        }
    }
}