package cc.lib.zombicide

import cc.lib.game.Utils
import cc.lib.utils.Table

class ZArmor(override val type: ZArmorType=ZArmorType.CHAINMAIL) : ZEquipment<ZArmorType>() {
    companion object {
        init {
            addAllFields(ZArmor::class.java)
        }
    }

    override val slotType: ZEquipSlotType
        get() = type.slotType

    override fun isEquippable(c: ZCharacter): Boolean {
        return true
    }

    override val isArmor: Boolean
        get() = true

    override fun getCardInfo(c: ZCharacter, game: ZGame): Table {

        /*

        SHIELD
        specialInfo    |
        ---------------| How armor works text
        RATING         |
        Walker      | 0|
        Fatty       | 5|
        Runner      | 5|
        Necromancer | 5|
        Abomination | 5|
        Special        |
         */
        val ratings = Table().setNoBorder()
        for (type in Utils.asList(ZZombieType.Walker, ZZombieType.Fatty, ZZombieType.Runner, ZZombieType.Necromancer, ZZombieType.Abomination)) {
            ratings.addRow(type, this.type.getDieRollToBlock(type))
        }
        val main = Table(label).setNoBorder().addRow(ratings)
        if (type.specialAbilityDescription != null) {
            main.addRow(Utils.wrapTextWithNewlines(type.specialAbilityDescription, 24))
        }
        return main
    }

    override fun getTooltipText(): String {
        val ratings = Table().setNoBorder()
        for (type in Utils.asList(ZZombieType.Walker, ZZombieType.Fatty, ZZombieType.Runner, ZZombieType.Necromancer, ZZombieType.Abomination)) {
            ratings.addRow(type, this.type.getDieRollToBlock(type))
        }
        if (type.specialAbilityDescription != null) {
            ratings.addRow(Utils.wrapTextWithNewlines(type.specialAbilityDescription, 24))
        }
        return ratings.toString()
    }
}