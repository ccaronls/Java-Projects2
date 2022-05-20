package cc.lib.zombicide

import cc.lib.annotation.Keep

@Keep
enum class ZEquipSlotType {
    HAND,
    BODY,
    BACKPACK;

    fun canEquip(): Boolean {
        return this != BACKPACK
    }
}