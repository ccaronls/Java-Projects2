package cc.lib.zombicide


import cc.lib.reflector.Reflector
import cc.lib.ui.IButton
import cc.lib.utils.Table
import cc.lib.utils.prettify

abstract class ZEquipment<T : ZEquipmentType> : Reflector<ZEquipment<T>>(), IButton, Comparable<ZEquipment<*>> {
    companion object {
        init {
            addAllFields(ZEquipment::class.java)
        }
    }

    @JvmField
    var vaultItem = false
    @JvmField
    var slot: ZEquipSlot? = null
    abstract val slotType: ZEquipSlotType
    open val isOpenDoorCapable: Boolean
        get() = false
    open val isConsumable: Boolean
        get() = false

    abstract fun isEquippable(c: ZCharacter): Boolean
    open val isMelee: Boolean
        get() = false
    open val isMagic: Boolean
        get() = false
    open val isRanged: Boolean
        get() = false
    open val isEnchantment: Boolean
        get() = false
    open val isArmor: Boolean
        get() = false
    val isThrowable: Boolean
        get() = type.isActionType(ZActionType.THROW_ITEM)
    open val isDualWieldCapable: Boolean
        get() = false
    open val isOpenDoorsNoisy: Boolean
        get() = false
    abstract val type: T
    override fun equals(o: Any?): Boolean {
        if (this === o || type === o) return true
        if (o == null) return false
        return if (o !is ZEquipment<*>) {
            false
        } else type == o.type
    }

    override fun compareTo(o: ZEquipment<*>): Int {
        // Consider not using this. Causes a problem when multiple of same equipment for user to choose from
        val comp = getLabel().compareTo(o.getLabel())
        return if (comp != 0) {
            comp
        } else compareValues(slot, o.slot)
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }

	abstract fun getCardInfo(c: ZCharacter, game: ZGame): Table
	override fun getTooltipText(): String? = type.getTooltipText()

	override fun toString(): String = type.name.prettify()

	override fun getLabel(): String = type.name.prettify()

    open fun onEndOfRound(game: ZGame) {}
    override fun isImmutable(): Boolean = true
}