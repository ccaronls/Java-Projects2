package cc.lib.zombicide


import cc.lib.utils.Table
import cc.lib.utils.wrap
import java.util.*

class ZItem(override val type: ZItemType=ZItemType.APPLES) : ZEquipment<ZItemType>() {
    companion object {
        init {
            addAllFields(ZItem::class.java)
        }
    }

    override val slotType: ZEquipSlotType
        get() = type.slot
    override val isConsumable: Boolean
        get() = type.isActionType(ZActionType.CONSUME)

    override fun isEquippable(c: ZCharacter): Boolean {
        return type.slot.canEquip()
    }

    override val isMelee: Boolean
        get() = false
    override val isMagic: Boolean
        get() = false
    override val isRanged: Boolean
        get() = false

    override fun getCardInfo(c: ZCharacter, game: ZGame): Table {
        val card = Table().setNoBorder()
        card.addColumn(label, listOf(type.description.wrap(24)))
        return card
    }

    override fun getTooltipText(): String {
        return type.description
    }
}