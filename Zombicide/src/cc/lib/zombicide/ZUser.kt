package cc.lib.zombicide

import cc.lib.game.GColor

abstract class ZUser(var name: String, vararg players: ZPlayerName) {
	var colorId = 0
		private set

	private val _players = mutableSetOf<ZPlayerName>().also {
		it.addAll(players)
	}
	val players: List<ZPlayerName>
		get() = _players.toList()

	fun clearCharacters() {
		_players.clear()
	}

	fun addCharacter(c: ZCharacter) {
		_players.add(c.type)
		c.color = getColor()
	}

	fun removeCharacter(character: ZCharacter) {
		_players.remove(character.type)
		character.color = GColor.TRANSLUSCENT_BLACK
	}

	fun setCharacters(chars: List<ZCharacter>) {
		_players.clear()
		for (nm in chars) {
			addCharacter(nm)
		}
	}

	fun setColor(board: ZBoard, id: Int, nm: String = USER_COLOR_NAMES[id]) {
		colorId = id
		name = nm
		_players.forEach {
			board.getCharacterOrNull(it)?.color = USER_COLORS[id]
		}
	}

	fun getColor(): GColor = USER_COLORS[colorId]

	fun hasPlayer(name: ZPlayerName): Boolean {
		return _players.contains(name)
	}

	abstract fun chooseCharacter(options: List<ZPlayerName>): ZPlayerName?
	abstract fun chooseMove(cur: ZPlayerName, options: List<ZMove>): ZMove?

	abstract fun chooseNewSkill(character: ZPlayerName, skillOptions: List<ZSkill>): ZSkill?
	abstract fun chooseSlotToOrganize(cur: ZPlayerName, slots: List<ZEquipSlot>): ZEquipSlot?
	abstract fun chooseEquipment(cur: ZPlayerName, equipOptions: List<ZEquipment<*>>): Int?
	fun chooseEquipmentInternal(cur: ZPlayerName, equipOptions: List<ZEquipment<*>>): ZEquipment<*>? {
		return chooseEquipment(cur, equipOptions)?.let { equipOptions[it] }
	}

    abstract fun chooseSlotForEquip(cur: ZPlayerName, equipableSlots: List<ZEquipSlot>): ZEquipSlot?
    abstract fun chooseZoneToWalk(cur: ZPlayerName, zones: List<Int>): Int?
    abstract fun chooseDoorToToggle(cur: ZPlayerName, doors: List<ZDoor>): Int?
    fun chooseDoorToToggleInternal(cur: ZPlayerName, doors: List<ZDoor>): ZDoor? {
        return chooseDoorToToggle(cur, doors)?.let { doors[it] }
    }

    abstract fun chooseWeaponSlot(c: ZPlayerName, weapons: List<ZWeapon>): Int?
    fun chooseWeaponSlotInternal(c: ZPlayerName, weapons: List<ZWeapon>): ZWeapon? {
        return chooseWeaponSlot(c, weapons)?.let { weapons[it] }
    }

    abstract fun chooseTradeCharacter(c: ZPlayerName, list: List<ZPlayerName>): ZPlayerName?
    abstract fun chooseZoneForAttack(c: ZPlayerName, zones: List<Int>): Int?
    abstract fun chooseItemToPickup(cur: ZPlayerName, list: List<ZEquipment<*>>): Int?
    fun chooseItemToPickupInternal(cur: ZPlayerName, list: List<ZEquipment<*>>): ZEquipment<*>? {
        return chooseItemToPickup(cur, list)?.let { list[it] }
    }

    abstract fun chooseItemToDrop(cur: ZPlayerName, list: List<ZEquipment<*>>): Int?
    fun chooseItemToDropInternal(cur: ZPlayerName, list: List<ZEquipment<*>>): ZEquipment<*>? {
        return chooseItemToDrop(cur, list)?.let { list[it] }
    }

    abstract fun chooseEquipmentToThrow(cur: ZPlayerName, slots: List<ZEquipment<*>>): Int?
    fun chooseEquipmentToThrowInternal(cur: ZPlayerName, slots: List<ZEquipment<*>>): ZEquipment<*>? {
        return chooseEquipmentToThrow(cur, slots)?.let { slots[it] }
    }

    abstract fun chooseZoneToThrowEquipment(cur: ZPlayerName, toThrow: ZEquipment<*>, zones: List<Int>): Int?
    abstract fun chooseZoneToShove(cur: ZPlayerName, list: List<Int>): Int?
	abstract fun chooseSpell(cur: ZPlayerName, spells: List<ZSpell>): ZSpell?
	abstract fun chooseCharacterForSpell(cur: ZPlayerName, spell: ZSpell, targets: List<ZPlayerName>): ZPlayerName?
	abstract fun chooseCharacterToBequeathMove(cur: ZPlayerName, list: List<ZPlayerName>): ZPlayerName?
	abstract fun chooseZoneForBloodlust(cur: ZPlayerName, list: List<Int>): Int?
	abstract fun chooseSpawnAreaToRemove(cur: ZPlayerName, list: List<ZSpawnArea>): Int?
	abstract fun chooseZoneToIgnite(playerName: ZPlayerName, ignitableZones: List<Int>): Int?
	abstract fun chooseEquipmentClass(playerName: ZPlayerName, classes: List<ZEquipmentClass>): ZEquipmentClass?
	abstract fun chooseStartingEquipment(playerName: ZPlayerName, list: List<ZEquipmentType>): ZEquipmentType?
	abstract fun chooseOrganize(playerName: ZPlayerName, list: List<ZMove>): ZMove?

	/**
	 * This may be called more than once. Need to handle if a dialog is already open
	 */
	open fun organizeStart(primary: ZPlayerName, secondary: ZPlayerName?) {
		throw NotImplementedError()
	}

	open fun organizeEnd() {
		throw NotImplementedError()
	}

	companion object {

		@JvmField
		val USER_COLORS = arrayOf(
			GColor.YELLOW,
			GColor.RED,
			GColor.GREEN,
			GColor.ORANGE,
			GColor.BLUE,
			GColor.MAGENTA
		)

		@JvmField
		val USER_COLOR_NAMES = arrayOf(
			"YELLOW", "RED", "GREEN", "ORANGE", "BLUE", "MAGENTA"
		)
	}
}