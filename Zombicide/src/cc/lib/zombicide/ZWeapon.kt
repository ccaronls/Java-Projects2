package cc.lib.zombicide


import cc.lib.game.GColor
import cc.lib.utils.Table
import cc.lib.utils.toInt
import cc.lib.utils.wrap

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
	    if (type.needsReload)
		    isEmpty = true
	    if (type === ZWeaponType.DAGGER) {
		    cur.removeEquipment(this)
		    game.putBackInSearchables(this)
	    }
    }

    override val isDualWieldCapable: Boolean
        get() = type.canTwoHand
    override val isOpenDoorsNoisy: Boolean
        get() = type.openDoorsIsNoisy

    fun reload(): Boolean {
        if (isEmpty) {
            isEmpty = false
            return true
        }
        return false
    }

	fun getComparisonInfo(game : ZGame, c0 : ZCharacter, c1 : ZCharacter) : Table {

		// build a table where stats are followed by one of '^,-,v' for 'better, same, worse'
		// for stats for each character

		/*
		ORCISH CROSSBOW
		---------------------------------
		ANN                           | MORRIGAN
		---------------------------------
		        | MELEE    | RANGED   | MELEE    | RANGED
		Doors   | 50% -    | no -     | 50% -    | no -
		Damage  |  1  v    | 2 ^      | 2 ^      | 1 v

		 */

		val c0StatsTab = Table().setNoBorder()
		val c1StatsTab = Table().setNoBorder()
		val labels = Table().setNoBorder().addColumnNoHeader(listOf(
			"Damage",
			"Hit %",
			"Max Range",
			"Doors",
			"Equippable"))
		c0StatsTab.addColumnNoHeader(listOf("", labels))

		fun getComparisonArray(stat0 : Int, stat1 : Int, formatter : (Int) -> String) : Array<String> {
			val f0 = formatter(stat0)
			val f1 = formatter(stat1)
			return when (stat0.compareTo(stat1)) {
				-1   -> arrayOf("$f0", "v", "$f1", "^")
				1    -> arrayOf("$f0", "^", "$f1", "v")
				else -> arrayOf("$f0", "-", "$f1", "-")
			}
		}

		for (at in ZActionType.values()) {
			listOfNotNull(
				c0.getWeaponStat(this, at, game, -1),
				c1.getWeaponStat(this, at, game, -1)).takeIf { it.size > 1 }?.let {
				val c0Stats = it[0]
				val c1Stats = it[1]
				val damage = getComparisonArray(c0Stats.damagePerHit, c1Stats.damagePerHit) { "$it" }
				val hitPercent = getComparisonArray(c0Stats.numDice, c1Stats.numDice) { "${c0Stats.dieRollToHitPercent}% x $it" }
				val range = getComparisonArray(c0Stats.maxRange, c1Stats.maxRange) { "$it" }
				val dieRollToOpenDoor = getComparisonArray(c0Stats.dieRollToOpenDoorPercent, c1Stats.dieRollToOpenDoorPercent) { if (it == 0) "no" else "$it%" }
				val equippable = getComparisonArray(isEquippable(c0).toInt(), isEquippable(c1).toInt()) { if (it == 0) "no" else "yes" }

				c0StatsTab.addColumnNoHeader(listOf(
					at.label, Table().setPadding(0).setNoBorder().addColumnNoHeader(listOf(damage[0], hitPercent[0], range[0], dieRollToOpenDoor[0], equippable[0]))
					.addColumnNoHeader(listOf(damage[1], hitPercent[1], range[1], dieRollToOpenDoor[1], equippable[1]))

				))
				c1StatsTab.addColumnNoHeader(listOf(
					at.label, Table().setPadding(0).setNoBorder().addColumnNoHeader(listOf(damage[2], hitPercent[2], range[2], dieRollToOpenDoor[2], equippable[2]))
					.addColumnNoHeader(listOf(damage[3], hitPercent[3], range[3], dieRollToOpenDoor[3], equippable[3]))
				))

			}
		}

		return Table(c0.getLabel(), c1.getLabel()).setNoBorder().addRow(c0StatsTab, c1StatsTab)
	}

    override fun getCardInfo(c: ZCharacter, game: ZGame): Table {

        /*

        ORCISH CROSSBOW (DW)
        --------------------
        Doors |  no/quiet/noisy
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
        cardLower.addColumnNoHeader(listOf(
                "Type",
                "Damage",
                "Hit %",
                "Range",
                "Doors",
                "Reloads"))
        for (at in ZActionType.values()) {
            c.getWeaponStat(this, at, game, -1)?.let { stats ->
	            val doorInfo: String = if (stats.dieRollToOpenDoor > 0) {
		            String.format(
			            "%s %d%%",
			            if (type.openDoorsIsNoisy) "noisy" else "quiet",
			            stats.dieRollToOpenDoorPercent
		            )
	            } else {
		            "no"
	            }
	            cardLower.addColumnNoHeader(
		            listOf(
			            at.label,
			            String.format(
				            "%d %s",
				            stats.damagePerHit,
				            if (type.attackIsNoisy) " loud" else " quiet"
			            ),
			            String.format(
				            "%d%% x %d",
				            (7 - stats.dieRollToHit) * 100 / 6,
				            stats.numDice
			            ),
			            if (stats.minRange == stats.maxRange) stats.minRange.toString() else String.format(
				            "%d-%d",
				            stats.minRange,
				            stats.maxRange
			            ),
			            doorInfo,
			            if (type.needsReload) String.format(
				            "yes (%s)",
				            if (isEmpty) "empty" else "loaded"
			            ) else "no"
		            )
	            )
            }
        }
	    val card = Table(
		    String.format(
			    "%s%s%s %s",
			    if (c.isDualWielding(this)) "2X " else "",
			    type.getLabel(),
			    if (type.canTwoHand) " (DW)" else "",
			    type.minColorToEquip
		    )
	    )
		    .addRow(cardLower).setNoBorder()
	    if (type.specialInfo != null) {
		    card.addRow("*${type.specialInfo}".wrap(32))
	    } else {
		    val skills = type.skillsWhileEquipped
		    if (skills.isNotEmpty()) {
			    card.addRow("Equipped", skills)
		    }
	    }
	    return card
    }

	override fun getColor(): GColor = type.minColorToEquip.color

	override fun getTooltipText(): String = type.getStatsTable(isEmpty).toString()

	override suspend fun onEndOfRound(game: ZGame) {
		when (type) {
			ZWeaponType.HAND_CROSSBOW -> if (!isLoaded) {
				game.addLogMessage("${getLabel()} auto reloaded")
				reload()
			}

			else -> Unit
		}
	}
}