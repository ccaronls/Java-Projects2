package cc.lib.zombicide


import cc.lib.ui.IButton
import cc.lib.utils.GException
import cc.lib.utils.prettify

interface ZEquipmentType : IButton {

    open fun create(): ZEquipment<*>

    val name: String
    override fun getLabel(): String {
        return prettify(name)
    }

    override fun getTooltipText(): String? {
        return null
    }

    /**
     * Additional skills processed when item equipped in hand or body
     * @return
     */
    val skillsWhileEquipped: List<ZSkill>
        get() = emptyList()

    /**
     * Additional skills processed when the item used
     *
     * @return
     */
    val skillsWhenUsed: List<ZSkill>
        get() = emptyList()

    /**
     *
     * @return
     */
    val equipmentClass: ZEquipmentClass

    /**
     * Items have can potentially support multiple actions
     *
     * @param type
     * @return
     */
    fun isActionType(type: ZActionType): Boolean
    fun onThrown(game: ZGame, thrower: ZCharacter, targetZoneIdx: Int) {
        throw GException("Not a throwable item '$this'")
    }

    val throwMinRange: Int
        get() = 0
    val throwMaxRange: Int
        get() = 1

    fun getDieRollToBlock(type: ZZombieType): Int {
        return 0
    }

    val isShield: Boolean
        get() = false
}