package cc.lib.zombicide


import cc.lib.utils.Table
import cc.lib.utils.appendedWith
import cc.lib.utils.prettify
import cc.lib.utils.wrap
import java.util.*

class ZWeapon(override val type: ZWeaponType=ZWeaponType.AXE) : ZEquipment<ZWeaponType>() {
    companion object {
        init {
            addAllFields(ZWeapon::class.java)
        }
    }

    var isEmpty = false

    val openDoorValue: Int
        get() {
            val doorStat = openDoorStat ?: return 0
            return 7 - doorStat.dieRollToOpenDoor + if (type.openDoorsIsNoisy) 0 else 1
        }
    override val isOpenDoorCapable: Boolean
        get() = openDoorStat != null
    val openDoorStat: ZWeaponStat?
        get() {
            for (stat in type.weaponStats) {
                if (stat.dieRollToOpenDoor > 0) return stat
            }
            return null
        }

    fun getStatForAction(actionType: ZActionType): ZWeaponStat? {
        return type.stats.firstOrNull { it.actionType === actionType }
    }

    override val slotType: ZEquipSlotType
        get() = ZEquipSlotType.HAND
    override val isMelee: Boolean
        get() = countStatsType(ZActionType.MELEE) > 0
    override val isRanged: Boolean
        get() = countStatsType(ZActionType.RANGED) > 0
    override val isMagic: Boolean
        get() = countStatsType(ZActionType.MAGIC) > 0

    private fun countStatsType(actionType: ZActionType): Int {
        return type.stats.count { stat -> stat.actionType === actionType }
    }

    override fun isEquippable(c: ZCharacter): Boolean {
        return c.skillLevel.difficultyColor.ordinal >= type.minColorToEquip.ordinal
    }

    val isLoaded: Boolean
        get() = !isEmpty

    fun fireWeapon(game: ZGame, cur: ZCharacter, stat: ZWeaponStat) {
        if (stat.attackType.needsReload()) isEmpty = true
        if (type === ZWeaponType.DAGGER) {
            cur.removeEquipment(this)
            game.putBackInSearchables(this)
        }
    }

    override val isDualWieldCapable: Boolean
        get() = type.canTwoHand
    override val isAttackNoisy: Boolean
        get() = type.attackIsNoisy
    override val isOpenDoorsNoisy: Boolean
        get() = type.openDoorsIsNoisy

    fun reload(): Boolean {
        if (isEmpty) {
            isEmpty = false
            return true
        }
        return false
    }

    override fun getCardInfo(c: ZCharacter, game: ZGame): Table {

        /*

        ORCISH CROSSBOW (DW)
        --------------------
        Doors |  no/quietly/noisy
        Open % |  1=6/6 2 = 5/6 3 = 4/6 4 = 3/6 2 = 5/6 6 = 1/6 ---> (7-n)*100/6 %
               | Melee      | Ranged
        Damage | 2 (loud)  | s (quiet)
        Hit %  | 50% x dice | 50% x dice
        Range  | 0      | 0-1 |
        Reload | no | yes |
         */

        /*
        Table cardUpper = new Table(new Object[][] {
                { "Doors", !canOpenDoor() ? "no" : (type.openDoorsIsNoisy ? "noisy" : "quietly") },
                { "Open %", type.meleeStats == null ? "(/)" : String.format("%d%%", (7-type.meleeStats.dieRollToOpenDoor)*100/6) }
        }, Table.NO_BORDER);
*/
        val cardLower = Table().setNoBorder()
        cardLower.addColumnNoHeader(Arrays.asList(
                "Type",
                "Damage",
                "Hit %",
                "Range",
                "Doors",
                "Reloads"))
        for (at in ZActionType.values()) {
            val stats = c.getWeaponStat(this, at, game, -1)
            if (stats != null) {
                var doorInfo: String
                doorInfo = if (stats.dieRollToOpenDoor > 0) {
                    String.format("%s %d%%", if (type.openDoorsIsNoisy) "noisy" else "quiet", (7 - stats.dieRollToOpenDoor) * 100 / 6)
                } else {
                    "no"
                }
                cardLower.addColumnNoHeader(Arrays.asList(at.label, String.format("%d %s", stats.damagePerHit, if (type.attackIsNoisy) " loud" else " quiet"), String.format("%d%% x %d", (7 - stats.dieRollToHit) * 100 / 6, stats.numDice),
                        if (stats.minRange == stats.maxRange) stats.minRange.toString() else String.format("%d-%d", stats.minRange, stats.maxRange),
                        doorInfo,
                        if (stats.attackType.needsReload()) String.format("yes (%s)", if (isEmpty) "empty" else "loaded") else "no"
                ))
            }
        }
        val card = Table(String.format("%s%s %s", type.label, if (type.canTwoHand) " (DW)" else "", type.minColorToEquip))
                .addRow(cardLower).setNoBorder()
        if (type.specialInfo != null) {
            card.addRow("*${type.specialInfo}".wrap( 32))
        } else {
            val skills = type.skillsWhileEquipped
            if (skills.isNotEmpty()) {
                card.addRow("Equipped", skills)
            }
        }
        return card
    }

    override fun getTooltipText(): String? {
        val cardLower = Table().setNoBorder()
        cardLower.addColumnNoHeader(Arrays.asList(
                "Attack Type",
                "Dual Wield",
                "Damage",
                "Hit %",
                "Range",
                "Doors",
                "Reloads"))
        for (stats in type.stats) {
            var doorInfo: String
            doorInfo = if (stats.dieRollToOpenDoor > 0) {
                String.format("%s %d%%", if (type.openDoorsIsNoisy) "noisy" else "quiet", (7 - stats.dieRollToOpenDoor) * 100 / 6)
            } else {
                "no"
            }
            cardLower.addColumnNoHeader(Arrays.asList(
                    prettify(stats.attackType.name),
                    if (type.canTwoHand) "yes" else "no", String.format("%d %s", stats.damagePerHit, if (type.attackIsNoisy) " loud" else " quiet"), String.format("%d%% x %d", (7 - stats.dieRollToHit) * 100 / 6, stats.numDice),
                    if (stats.minRange == stats.maxRange) stats.minRange.toString() else String.format("%d-%d", stats.minRange, stats.maxRange),
                    doorInfo,
                    if (stats.attackType.needsReload()) String.format("yes (%s)", if (isEmpty) "empty" else "loaded") else "no"
            ))
        }
        if (type.minColorToEquip.ordinal > 0) {
            cardLower.addRow(type.minColorToEquip.toString() + " Required")
        }
        val skills = type.skillsWhileEquipped.appendedWith(type.skillsWhenUsed)
        for (skill in skills) {
            cardLower.addRow(skill.label)
        }
        return cardLower.toString()
    }

    override fun onEndOfRound(game: ZGame) {
        when (type) {
            ZWeaponType.HAND_CROSSBOW -> if (!isLoaded) {
                game.addLogMessage("$label auto reloaded")
                reload()
            }
        }
    }
}