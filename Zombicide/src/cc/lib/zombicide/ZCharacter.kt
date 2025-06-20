package cc.lib.zombicide

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.GDimension
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.reflector.Omit
import cc.lib.reflector.RBufferedReader
import cc.lib.ui.IButton
import cc.lib.utils.Table
import cc.lib.utils.increment
import cc.lib.utils.prettify
import cc.lib.zombicide.ZEquipSlot.Companion.wearableValues
import java.util.Arrays
import kotlin.math.max

class ZCharacter(
	override val type: ZPlayerName = ZPlayerName.Ann,
	skillz: Array<Array<ZSkill>> = emptyArray()
) : ZSurvivor(-1), Table.Model, IButton {
	companion object {
		val log = LoggerFactory.getLogger(ZCharacter::class.java)
		const val MAX_BACKPACK_SIZE = 5
		const val MAX_WOUNDS = 4
		const val MOVE_SPEED_MILLIS = 750
		const val PRIORITY = 100

		init {
			addAllFields(ZCharacter::class.java)
		}
    }

    var woundBar = 0
        private set
    var exp = 0
	    private set

    private val actionsDoneThisTurn: MutableList<ZActionType> = ArrayList()
    private val allSkills: MutableList<ZSkill> = ArrayList() // all skills based on the characters level and choices
    private val availableSkills: MutableSet<ZSkill> = HashSet() // skills from all skills minus ones used that are once per turn

    private val skillsRemaining: Array<MutableList<ZSkill>> = skillz.map { it.toMutableList() }.toTypedArray()
    private val backpack: MutableList<ZEquipment<*>> = ArrayList()
	var leftHand: ZEquipment<*>? = null
		private set
	var rightHand: ZEquipment<*>? = null
		private set
	var body: ZEquipment<*>? = null
		private set
	private val kills = IntArray(ZZombieType.values().size)
	private val favoriteWeapons: MutableMap<ZEquipmentType, Int> = HashMap()
	private var fallen = false
	private var forceInvisible = false
	var colorId = -1

	val color: GColor
		get() = ZUser.USER_COLORS.getOrNull(colorId) ?: GColor.WHITE

	var zonesMoved = 0
		private set
	var isStartingWeaponChosen = false
		private set
	var isStartingFamiliarChosen = false
		private set
	val usedSpells = mutableMapOf<ZSpellType, Int>()
	override var skillLevel = ZSkillLevel()
		private set
	var hasMovedThisTurn = false
		private set

	@Omit
	private var savedWounds = 0

	@Omit
	var isReady = true
		get() = field || !isInvisible

	var deathType: ZAttackType? = null
		private set

	fun saveWounds() {
		savedWounds = woundBar
	}

	fun restoreWounds() {
		woundBar = savedWounds
	}

	init {
		val blueSkills = getRemainingSkillsForLevel(0)
		allSkills.addAll(blueSkills)
		blueSkills.clear()
	}

	override fun parseUnknownField(name: String, value: String?, input: RBufferedReader) {
		when (name) {
			"color" -> colorId = ZUser.USER_COLORS.indexOf(parse(GColor::class.java, input))
			else -> super.parseUnknownField(name, value, input)
		}
	}

	@Synchronized
	fun clear() {
		Arrays.fill(kills, 0)
		body = null
		rightHand = body
		leftHand = rightHand
		backpack.clear()
		allSkills.clear()
		availableSkills.clear()
		exp = 0
		woundBar = 0
		deathType = null
		isStartingWeaponChosen = false
		isStartingFamiliarChosen = false
		favoriteWeapons.clear()
	}

	fun addFamiliar(type: ZFamiliarType) {
		isStartingFamiliarChosen = true
		backpack.add(type.create())
	}

	override val playerType: ZPlayerName
		get() = type

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is ZCharacter) return false
		return other.type === type
	}

	override fun hashCode(): Int {
		return type.hashCode()
	}

	override val imageId: Int
		get() = type.imageId
	override val outlineImageId: Int
		get() = type.outlineImageId

	override val dimension: GDimension
		get() = type.imageDim

	override fun onBeginRound(game: ZGame) {
		actionsDoneThisTurn.clear()
		availableSkills.clear()
		availableSkills.addAll(allSkills)
		zonesMoved = 0
		fallen = isDead
		usedSpells.clear()
		hasMovedThisTurn = false
		super.onBeginRound(game)
    }

	override fun getMaxCharsPerLine(): Int {
		return 256
	}

	override fun makeId(): String = type.name

	val killsTable: Table
		get() {
			val tab = Table(this).setNoBorder().setPadding(0)
			var added = false
			for (nm in ZZombieType.values()) {
				if (kills.size > nm.ordinal && kills[nm.ordinal] > 0) {
					tab.addRow(nm.toString() + " x " + kills[nm.ordinal])
					added = true
				}
            }
            if (!added) {
                tab.addRow("None")
            }
            return tab
        }
    val favoriteWeaponsTable: Table
        get() {
            val tab = Table(this).setNoBorder().setPadding(0)
            val list = favoriteWeapons.toList().sortedByDescending { it.second }
            for (p in list) {
                tab.addRow(p.first.getLabel() + " x " + p.second)
            }
            return tab
        }

    override fun name(): String {
	    return type.getLabel()
    }

	override suspend fun onKilledZombie(game: ZGame, zombie: ZZombie, type: ZEquipmentType?) {
		kills[zombie.type.ordinal]++
		type?.let {
			favoriteWeapons.increment(it, 1)
		}
	}

	override suspend fun performAction(action: ZActionType, game: ZGame) {
		hasMovedThisTurn = true
		if (action === ZActionType.MOVE) {
			zonesMoved++
		}
		for (skill in getAvailableSkills()) {
			when (skill.modifyActionsRemaining(this, action, game)) {
				1 -> {
					removeAvailableSkill(skill)
					game.addLogMessage(name() + " used " + skill.getLabel())
					return
                }
                -1 -> {
	                game.addLogMessage(name() + " used " + skill.getLabel())
	                return
                }
            }
        }
        when (action) {
	        ZActionType.INVENTORY,
	        ZActionType.CONSUME -> if (!hasAvailableSkill(ZSkill.Inventory)) {
		        addAvailableSkill(ZSkill.Inventory)
	        }

	        else -> Unit
        }
	    if (action.oncePerTurn) {
		    actionsDoneThisTurn.add(action)
	    }
	    if (isInvisible && action.breaksInvisibility) {
		    removeAvailableSkill(ZSkill.Invisible)
	    }
        super.performAction(action, game)
    }

	suspend fun tryOpenDoor(game: ZGame): Boolean {
		val weapons = weapons
		// order the weapons so that the best choice for door is in the front
		weapons.sortedWith { o1: ZWeapon, o2: ZWeapon ->
			// first order by % to open
			val v1 = o1.openDoorValue
			val v2 = o2.openDoorValue
			v2.compareTo(v1)
		}
		for (w in weapons) {
			val openDoorStat = w.openDoorStat
            if (openDoorStat != null) {
                if (w.type.openDoorsIsNoisy) game.addNoise(occupiedZone, 1)
                if (openDoorStat.dieRollToOpenDoor > 1) {
                    val die = game.rollDice(1)
                    if (die[0] < openDoorStat.dieRollToOpenDoor) {
                        game.addLogMessage(name() + " Failed to open the door with their " + w)
                        return false
                    }
                }
                game.addLogMessage(name() + " Used their " + w + " to break open the door")
                return true
            }
        }
        return false
    }

    public override val actionsPerTurn = 3

    fun canUnjamDoor(): Boolean {
        for (w in weapons) {
            if (w.isOpenDoorCapable) return true
        }
        return false
    }

    fun canBarricadeDoors(): Boolean {
        return isInPossession(ZItemType.BARRICADE)
    }

    private fun getEquipped(): List<ZEquipment<*>> = listOf(leftHand, rightHand, body).filterNotNull()
    
    /**
     *
     * @return
     */
    val weapons: List<ZWeapon>
        get() = getEquipped().filterIsInstance<ZWeapon>()

    /**
     * Return any armor and / or shileds player has
     * @return
     */
    val armor: List<ZArmor>
        get() = getEquipped().filterIsInstance<ZArmor>()
    
    val spells: List<ZSpell>
        get() {
	        val spells = getEquipped().filterIsInstance<ZSpell>().filter {
		        usedSpells.getOrDefault(it.type, 0) < it.type.usesPerTurn
	        }.toMutableList()
	        if (getAvailableSkills().contains(ZSkill.Spellbook)) {
		        spells.addAll(backpack.filterIsInstance<ZSpell>())
	        }
	        return spells
        }

	/**
	 *
	 * @return
	 */
	val isDualWielding: Boolean
		get() = weapons.firstOrNull { isDualWielding(it) } != null

	fun isDualWielding(weapon: ZWeapon): Boolean = canDualWield(weapon)
		&& (arrayOf(leftHand, body, rightHand).count { it?.type == weapon.type } >= 2)

	fun canDualWield(weapon: ZWeapon): Boolean {
		if (weapon.isDualWieldCapable) return true
		for (skill in getAvailableSkills()) {
			if (skill.canTwoHand(weapon)) return true
		}
		return false
	}

	val numBackpackItems: Int
	    get() = backpack.size

	fun getBackpackItem(index: Int): ZEquipment<*> {
		return backpack[index]
	}

	fun getBackpack(): List<ZEquipment<*>> {
		return backpack
	}

	fun getBackpackTable(game: ZGame): Table = Table().apply {
		backpack.forEach {
			addRow(it.type, it.type.getTooltipText() ?: it.getCardInfo(this@ZCharacter, game))
		}
	}


	fun getEmptyEquipSlotsFor(e: ZEquipment<*>): List<ZEquipSlot> = mutableListOf<ZEquipSlot>().also { list ->
		if (e.isEquippable(this)) {
			if (body == null && canEquipBody(e)) {
				list.add(ZEquipSlot.BODY)
			}
			when (e.slotType) {
				ZEquipSlotType.HAND -> {
					if (leftHand == null) {
						list.add(ZEquipSlot.LEFT_HAND)
                    }
                    if (rightHand == null) {
                        list.add(ZEquipSlot.RIGHT_HAND)
                    }
                }
	            ZEquipSlotType.BODY -> if (body == null) {
		            list.add(ZEquipSlot.BODY)
	            }
	            else -> Unit
            }
        }
	    if (!isBackpackFull)
	    	list.add(ZEquipSlot.BACKPACK)
    }

    /**
     * If user can equip the item the result slot equipped too. null otherwise
     * @param e
     * @return
     */
    fun tryEquip(e: ZEquipment<*>): ZEquipSlot? {
        if (e.isEquippable(this)) {
            if (body == null && canEquipBody(e)) {
                body = e
                e.slot = ZEquipSlot.BODY
                return ZEquipSlot.BODY
            }
            when (e.slotType) {
                ZEquipSlotType.HAND -> {
                    if (leftHand == null) {
                        leftHand = e
                        e.slot = ZEquipSlot.LEFT_HAND
                        return e.slot
                    }
                    if (rightHand == null) {
                        rightHand = e
                        e.slot = ZEquipSlot.RIGHT_HAND
                        return e.slot
                    }
                }
	            ZEquipSlotType.BODY -> if (body == null) {
		            body = e
		            e.slot = ZEquipSlot.BODY
		            return e.slot
	            }
	            else -> Unit
            }
        }
        if (backpack.size < MAX_BACKPACK_SIZE) {
            backpack.add(e)
            e.slot = ZEquipSlot.BACKPACK
            return e.slot
        }
        return null
    }

    fun removeEquipment(type: Any, slot: ZEquipSlot): ZEquipment<*>? {
        var removed: ZEquipment<*>? = null
        when (slot) {
            ZEquipSlot.BODY -> {
                removed = body
                body = null
            }
            ZEquipSlot.BACKPACK -> {
                val idx = backpack.indexOf(type)
                require(idx >= 0)
                removed = backpack.removeAt(idx)
            }
            ZEquipSlot.LEFT_HAND -> {
                removed = leftHand
                leftHand = null
            }
            ZEquipSlot.RIGHT_HAND -> {
                removed = rightHand
                rightHand = null
            }
        }
        removed?.slot = null
        return removed
    }

    @JvmOverloads
    fun attachEquipment(equipment: ZEquipment<*>, _slot: ZEquipSlot? = null): ZEquipment<*>? {
	    val slot = _slot?:getEmptyEquipSlotsFor(equipment)[0]
        equipment.slot = slot
        var prev: ZEquipment<*>? = null
        when (slot) {
            ZEquipSlot.RIGHT_HAND -> {
                prev = rightHand
                rightHand = equipment
            }
            ZEquipSlot.LEFT_HAND -> {
                prev = leftHand
                leftHand = equipment
            }
            ZEquipSlot.BODY -> {
                prev = body
                body = equipment
            }
	        ZEquipSlot.BACKPACK -> backpack.add(equipment)
        }
	    return prev
    }

	fun getStatsTable(rules: ZRules): Table = Table(this).also { stats ->
		var armorRating = StringBuilder("none")
		val ratings = getArmorRatings(ZZombieType.Walker)
		if (ratings.isNotEmpty()) {
			armorRating = StringBuilder("" + ratings[0] + "+")
			for (i in 1 until ratings.size) {
				armorRating.append("/").append(ratings[i]).append("+")
			}
		}
		stats.addRow("Moves", String.format("%d of %d", (actionsPerTurn - actionsLeftThisTurn), actionsPerTurn))
		stats.addRow("Wounds", String.format("%d of %d", woundBar, MAX_WOUNDS))
		stats.addRow("Armor Rolls", armorRating.toString())
		val ptsToNxt = skillLevel.getPtsToNextLevel(exp, rules)
		stats.addRow("Skill", skillLevel)
		stats.addRow("Exp/Next", "$exp / $ptsToNxt")
		stats.addRow("Dual\nWielding", isDualWielding)
	}

	fun getEquippedTable(game: ZGame): Table {
		val info = Table(this)
		if (isStartingWeaponChosen) {
			arrayOf(ZEquipSlot.LEFT_HAND, ZEquipSlot.BODY, ZEquipSlot.RIGHT_HAND).groupBy {
				getSlot(it)?.type
			}.forEach { (type, list) ->
				info.addColumn(
					list.joinToString(separator = "/") { it.getLabel() },
					getSlotInfo(list[0], game)
				)
			}
		} else {
			val options = Table(this)
			type.startingEquipment.forEach {
				options.addColumn(it.getLabel(), it.getInfoTable())
			}
			info.addColumn("Starting Weapon Options", options)
		}

		/*
				if (isDualWielding) {
					info.addColumn("Each Hand(DW)", getSlotInfo(ZEquipSlot.LEFT_HAND, game))
					info.addColumn(ZEquipSlot.BODY.getLabel(), getSlotInfo(ZEquipSlot.BODY, game))
				} else {
					info.addColumn(ZEquipSlot.LEFT_HAND.getLabel(), getSlotInfo(ZEquipSlot.LEFT_HAND, game))
					info.addColumn(ZEquipSlot.BODY.getLabel(), getSlotInfo(ZEquipSlot.BODY, game))
					info.addColumn(ZEquipSlot.RIGHT_HAND.getLabel(), getSlotInfo(ZEquipSlot.RIGHT_HAND, game))
				}*/
		return info
	}

	fun getInfoTable(game: ZGame): Table {

		/*

		Left Hand    |   Body    |    Right Hand    |    Stats    |     Skills
		-----------------------------------------------------------------------

		<left hand card? | <body card> | <right hand card> | <stats card> | <skills[0]>


		 */
		val info = getEquippedTable(game).setNoBorder().setPadding(0)
		val slotInfo = getSlotInfo(ZEquipSlot.BACKPACK, game)
		info.addColumn(ZEquipSlot.BACKPACK.getLabel() + if (isBackpackFull) " (full)" else "", slotInfo)
		val stats = getStatsTable(game.rules).setNoBorder().setPadding(0)
		info.addColumn("Stats", stats)
	    if (getAvailableSkills().isNotEmpty()) {
		    val skills = Table(this).setNoBorder().addColumnNoHeader(getAvailableSkills().map {
			    it.prettify()
		    })
		    info.addColumn("Skills", skills)
	    }
	    val main = Table(this).setNoBorder()
	    if (isDead) {
            main.addRow(String.format("%s (%s) Killed in Action",
	            type.getLabel(), type.characterClass))
        } else {
            main.addRow(String.format("%s (%s) Body:%s",
	            type.getLabel(), type.characterClass, type.alternateBodySlot
            ))
        }
        main.addRow(info)
        return main
    }

	fun getSkillsTable() : Table = Table().also { table ->
		setOf(*allSkills.toTypedArray(), *getAvailableSkills().toTypedArray()).forEachIndexed { index, it ->
			if (index > 0)
				table.addRow("", "")
			table.addRow(it.getLabel(), it.description)
		}
	}

    val isBackpackFull: Boolean
        get() = backpack.size == MAX_BACKPACK_SIZE

    fun canTrade(): Boolean {
        return allEquipment.size > 0
    }

    fun canSearch(zone: ZZone): Boolean {
        if (actionsDoneThisTurn.contains(ZActionType.SEARCH)) return false
        return if (getAvailableSkills().contains(ZSkill.Scavenger)) true else zone.isSearchable
    }

    fun canTake(e: ZEquipment<*>): Boolean {
        if (!isBackpackFull) return true
        if (!e.isEquippable(this)) return false
	    when (e.slotType) {
		    ZEquipSlotType.BODY -> return body == null
		    ZEquipSlotType.HAND -> {
			    return if (canEquipBody(e) && body == null) true else leftHand == null || rightHand == null
		    }
		    else -> Unit
	    }
	    return false
    }


	fun getEquippableSlots(equip: ZEquipment<*>, includeBackpack: Boolean): List<ZEquipSlot> {
		val options: MutableList<ZEquipSlot> = ArrayList()
		val canEquip = !includeBackpack || !isBackpackFull || null != backpack.firstOrNull { e: ZEquipment<*>? -> e === equip }
		val canWield = equip.isEquippable(this)
		when (equip.slotType) {
			ZEquipSlotType.BODY -> if (canWield && (body == null || canEquip)) options.add(ZEquipSlot.BODY)
			ZEquipSlotType.HAND -> if (canWield) {
				if (leftHand == null || canEquip) options.add(ZEquipSlot.LEFT_HAND)
				if (rightHand == null || canEquip) options.add(ZEquipSlot.RIGHT_HAND)
				if (body == null || canEquip) {
					if (canEquipBody(equip)) {
						options.add(ZEquipSlot.BODY)
					}
				}
			}

			ZEquipSlotType.BACKPACK -> if (!isBackpackFull) {
				options.add(ZEquipSlot.BACKPACK)
			}
		}
		if (includeBackpack && !options.contains(ZEquipSlot.BACKPACK) && !isBackpackFull) {
			options.add(ZEquipSlot.BACKPACK)
		}
		return options
	}

	fun canEquipBody(equip: ZEquipment<*>): Boolean {
        return type.alternateBodySlot == equip.type.equipmentClass
    }

	fun canEquip(_slot : ZEquipSlot?, equip : ZEquipment<*>) : Boolean {
		val slot = _slot ?: getEmptyEquipSlotsFor(equip).firstOrNull() ?: return false
		return when (equip.slotType) {
			ZEquipSlotType.HAND     -> ((slot == ZEquipSlot.LEFT_HAND && leftHand == null)
				|| (slot == ZEquipSlot.RIGHT_HAND && rightHand == null))
			ZEquipSlotType.BACKPACK -> slot == ZEquipSlot.BACKPACK && numBackpackItems < MAX_BACKPACK_SIZE
			ZEquipSlotType.BODY     -> slot == ZEquipSlot.BODY && body == null
		}
	}
    val meleeWeapons: List<ZWeapon>
        get() = (if (isDualWielding) listOfNotNull(leftHand, body) else listOfNotNull(leftHand, rightHand, body))
			.filterIsInstance<ZWeapon>().filter { it.isMelee }

    val rangedWeapons: List<ZWeapon>
        get() = (if (isDualWielding) listOfNotNull(leftHand, body) else listOfNotNull(leftHand, rightHand, body))
	        .filterIsInstance<ZWeapon>().filter { it.isRanged }

	val magicWeapons: List<ZWeapon>
		get() = (if (isDualWielding) listOfNotNull(leftHand, body) else listOfNotNull(
			leftHand,
			rightHand,
			body
		))
			.filterIsInstance<ZWeapon>().filter { it.isMagic }.toMutableList().also {
				if (getAvailableSkills().contains(ZSkill.Spellbook)) {
					it.addAll(backpack.filterIsInstance<ZWeapon>().filter { it.isMagic })
				}
			}

	val familiars: List<ZFamiliarLink>
		get() = backpack.filterIsInstance<ZFamiliarLink>()

	val throwableEquipment: List<ZEquipment<*>>
		get() = listOfNotNull(leftHand, rightHand, body).filter { it.isThrowable }

	fun getWeaponStat(
		weapon: ZWeapon,
		actionType: ZActionType,
		game: ZGame,
		targetZone: Int
	): ZWeaponStat? {
		return weapon.getStatForAction(actionType)?.let { _stat ->
			val stat = _stat.copy()
			for (skill in Utils.mergeLists(getAvailableSkills(), weapon.type.skillsWhenUsed)) {
				skill.modifyStat(stat, actionType, this, game, targetZone)
			}
			if (isDualWielding(weapon)) {
				stat.numDice *= 2
			}
			stat
		}
    }

	fun getSlot(slot: ZEquipSlot): ZEquipment<*>? = when (slot) {
		ZEquipSlot.BODY -> body
		ZEquipSlot.RIGHT_HAND -> rightHand
		ZEquipSlot.LEFT_HAND -> leftHand
		else -> null
	}

	fun getArmorRatings(type: ZZombieType): List<Int> {
		val ratings: MutableList<Int> = ArrayList()
		var skillRating = getAvailableSkills().map {
			it.getArmorRating(this, type)
		}.filter { it > 0 }.takeIf { it.isNotEmpty() }?.min() ?: 0
		for (slot in wearableValues()) {
			getSlot(slot)?.let { e ->
				var rating = e.type.getDieRollToBlock(type)
				if (rating > 0) {
					if (e.type.isShield) {
						ratings.add(rating)
					} else {
						if (skillRating > 0) rating -= 1
						ratings.add(rating)
						skillRating = 0
					}
				}
            }
        }
		if (!type.ignoresArmor && skillRating > 0) {
			ratings.add(skillRating)
		}
        return ratings
    }

    /**
     *
     * @param type
     * @return
     */
    fun getArmorRating(type: ZZombieType): Int {
        var rating = 0
        for (i in getArmorRatings(type)) {
            rating += i
        }
        return rating
    }

    /**
     *
     * @return
     */
    val isDead: Boolean
        get() = woundBar >= MAX_WOUNDS

    /**
     *
     * @return
     */
    override val isAlive: Boolean
	    get() = woundBar < MAX_WOUNDS

	override val noise: Int
		get() = if (isInvisible) 0 else 1
	override var isInvisible: Boolean
		get() = forceInvisible || getAvailableSkills().contains(ZSkill.Invisible)
		set(enable) {
			forceInvisible = enable
		}

	val isVisible: Boolean
		get() = !isInvisible && isAlive

	fun getSlotInfo(slot: ZEquipSlot, game: ZGame): Table? {
		when (slot) {
			ZEquipSlot.RIGHT_HAND -> {
				return rightHand?.getCardInfo(this, game)
			}

			ZEquipSlot.LEFT_HAND -> {
				return leftHand?.getCardInfo(this, game)
			}

			ZEquipSlot.BODY -> {
				return body?.getCardInfo(this, game)
			}
            ZEquipSlot.BACKPACK -> {
                if (backpack.size == 0) return null
                val table = Table().setNoBorder()
                for (e in backpack) {
                    table.addRow(e)
                }
	            return table
            }
        }
    }

	val allEquipment: List<ZEquipment<*>>
		get() = backpack.toMutableList().also {
			it.addAll(listOfNotNull(leftHand, body, rightHand))
		}

	fun removeEquipment(equip: ZEquipment<*>): ZEquipSlot? {
		val slot = equip.slot
		when (equip.slot) {
			ZEquipSlot.BACKPACK -> {
				val success = backpack.remove(equip)
				require(success)
			}
			ZEquipSlot.LEFT_HAND -> {
				require(leftHand === equip)
				leftHand = null
			}
			ZEquipSlot.RIGHT_HAND -> {
				require(rightHand === equip)
				rightHand = null
			}
			ZEquipSlot.BODY -> {
				require(body === equip)
				body = null
			}
			else -> Unit
		}
		equip.slot = null
		return slot
	}

	/**
	 * Return true if a equip type is in any of the users slots or backpack
	 * @param type
	 * @return
	 */
	fun isInPossession(type: ZEquipmentType): Boolean = allEquipment.firstOrNull { it.type == type } != null

	fun getEquipmentOfType(type: ZEquipmentType): ZEquipment<*>? {
		return allEquipment.firstOrNull { it.type == type }
    }

    fun isEquipped(type: ZEquipmentType): Boolean {
        return listOfNotNull(leftHand, rightHand, body).firstOrNull { it.type == type } != null
    }

	override fun canReroll(game: ZGame, action: ZAttackType): Boolean {
		return if (getAvailableSkills().contains(ZSkill.Lucky))
			true
		else when (action) {
			ZAttackType.RANGED_ARROWS -> isInPossession(ZItemType.PLENTY_OF_ARROWS)
			ZAttackType.RANGED_BOLTS -> isInPossession(ZItemType.PLENTY_OF_BOLTS)
			else -> false
		}
	}

	suspend fun onEndOfRound(game: ZGame) {
		for (skill in getAvailableSkills()) {
			skill.onEndOfTurn(game, this)
		}
		availableSkills.clear()
		listOfNotNull(leftHand, rightHand, body).forEach { it.onEndOfRound(game) }
		availableSkills.addAll(allSkills)
	}

    override val moveSpeed: Long
    get() = MOVE_SPEED_MILLIS.toLong()
    override val priority: Int
    get() = PRIORITY

    fun canFriendlyFire(): Boolean {
        for (s in getAvailableSkills()) {
	        if (s.avoidsInflictingFriendlyFire()) return false
        }
        return true
    }

    fun canReceiveFriendlyFire(): Boolean {
        if (isDead) return false
        for (s in getAvailableSkills()) {
	        if (s.avoidsReceivingFriendlyFire()) return false
        }
        return true
    }

    override fun draw(g: AGraphics) {
        if (fallen) {
	        g.drawImage(ZIcon.GRAVESTONE.imageIds[0], getRect().fit(g.getImage(ZIcon.GRAVESTONE.imageIds[0])))
        } else {
	        drawPedestal(g)
	        super.draw(g)
        }
    }

    protected fun drawPedestal(g: AGraphics) {
	    val hgt = 0.04f
	    val rect = getRect()
	    g.color = color.darkened(.5f)
	    val x = rect.left + 0.02f
	    val w = rect.width - 0.04f
	    g.drawFilledRect(x, rect.top + rect.height - hgt / 4, w, hgt)
	    g.drawFilledOval(x, rect.top + rect.height + hgt / 4, w, hgt)
	    g.color = color
	    g.drawFilledOval(x, rect.top + rect.height - hgt / 2, w, hgt)
    }

	override fun getAvailableSkills(): List<ZSkill> {
		return availableSkills.toMutableList().also { skills ->
			listOfNotNull(leftHand, rightHand, body).forEach {
				skills.addAll(it.type.skillsWhileEquipped)
				skills.addAll(it.type.skillsWhenUsed)
			}
			familiars.filter { it.familiar?.occupiedZone == occupiedZone }.forEach {
				skills.addAll(it.type.skillsWhileEquipped)
			}
	    }
    }

	override fun hasAvailableSkill(skill: ZSkill): Boolean {
		return getAvailableSkills().contains(skill)
	}

    fun useMarksmanForSorting(zoneIdx: Int): Boolean {
        for (skill in getAvailableSkills()) {
	        if (skill.useMarksmanForSorting(occupiedZone, zoneIdx)) {
		        return true
	        }
        }
	    return false
    }

	override fun hasSkill(skill: ZSkill): Boolean {
		return allSkills.contains(skill)
	}

	/**
	 * adds a permanent skill
	 */
	fun addSkill(skill: ZSkill) {
		allSkills.add(skill)
		availableSkills.add(skill)
	}

	/**
	 * 	Adds a temporary skill
	 */
	override fun addAvailableSkill(skill: ZSkill) {
		availableSkills.add(skill)
	}

	fun removeAvailableSkill(skill: ZSkill) {
		availableSkills.remove(skill)
	}

	fun getRemainingSkillsForLevel(level: Int): MutableList<ZSkill> {
        if (level < skillsRemaining.size)
	        return skillsRemaining[level]
        return mutableListOf()
    }

    fun setFallen(fallen: Boolean) {
        this.fallen = fallen
    }

	override suspend fun heal(game: ZGame, amt: Int): Boolean {
		if (woundBar > 0) {
			game.onCharacterHealed(type, amt)
			game.addLogMessage(String.format("%s has %d wounds healed.", name(), amt))
			woundBar = max(0, woundBar - amt)
			return true
		}
		return false
	}

	fun wound(amt: Int, type: ZAttackType) {
		woundBar += amt
		if (isDead) {
			deathType = type
		}
	}

    fun getKills(type: ZZombieType): Int {
        return kills[type.ordinal]
    }

	override suspend fun addExperience(game: ZGame, pts: Int) {
		exp += pts
		skillLevel = ZSkillLevel.getLevel(exp, game.rules)
	}

    fun setStartingEquipment(type: ZEquipmentType) {
        attachEquipment(type.create())
        isStartingWeaponChosen = true
    }

	fun canDoAction(action : ZActionType) : Boolean {
		when (action) {
			ZActionType.INVENTORY -> if (availableSkills.contains(ZSkill.Inventory))
				return true
			else -> Unit
		}
		return actionsLeftThisTurn > 0
	}

	override val isNoisy: Boolean
		get() = isAlive && !isInvisible
	override val scale: Float
		get() = when (type) {
			ZPlayerName.Nelly -> 1.4f
			ZPlayerName.Baldric -> 1.2f
			else -> super.scale
		}
	val isWounded: Boolean
		get() = woundBar > 0

	fun getAllSkillsTable(rules: ZRules): Table {
		return Table().also { table ->
			ZColor.entries.forEach { color ->
				var colorTxt = color.name
				if (skillLevel.difficultyColor == color)
					colorTxt = ">$colorTxt<"
				else
					colorTxt = " $colorTxt "
				val options = type.getSkillOptions(color).map {
					if (hasSkill(it)) {
						">${it.prettify()}<"
					} else {
						" ${it.prettify()} "
					}
				}.joinToString("\n")
				table.addRow(colorTxt, options)
			}
		}
	}

	fun getAllSkillsTableExpanded() : Table {
		return Table().also { table ->
			table.addRow(getLabel())
			ZColor.values().forEach { color ->
				table.addRow(color.name)
				type.getSkillOptions(color).forEach {
					table.addRow(hasSkill(it), it.getLabel(), it.description)
				}
			}
		}
	}

	fun useSpell(type: ZSpellType) {
		usedSpells.increment(type, 1)
	}

	override val isRendered: Boolean = true
}