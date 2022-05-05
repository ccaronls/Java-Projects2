package cc.lib.zombicide

import cc.lib.game.Utils
import cc.lib.utils.Table

class ZSpell(override val type: ZSpellType=ZSpellType.HEALING) : ZEquipment<ZSpellType>() {
    companion object {
        init {
            addAllFields(ZSpell::class.java)
        }
    }

    override val slotType: ZEquipSlotType
        get() = ZEquipSlotType.HAND

    override fun isEquippable(c: ZCharacter): Boolean {
        return true
    }

    override val isEnchantment: Boolean
        get() = true

    override fun getCardInfo(c: ZCharacter, game: ZGame): Table {
        val t = Table(label).setNoBorder()
        return t.addRow(Utils.wrapTextWithNewlines(type.description, 20))
    }
}