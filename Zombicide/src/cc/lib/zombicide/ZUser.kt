package cc.lib.zombicide

import cc.lib.game.GColor

abstract class ZUser(var name: String, vararg players: ZPlayerName) {

	var colorId = 0
		private set

	protected open fun setColorId(id: Int) {
		colorId = id
	}

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
		c.isInvisible = false
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

	fun setColor(board: ZBoard, id: Int, nm: String? = null) {
		setColorId(id)
		name = nm ?: USER_COLOR_NAMES[id]
		_players.forEach {
			board.getCharacterOrNull(it)?.color = USER_COLORS[id]
		}
	}

	fun getColor(): GColor = USER_COLORS[colorId]

	fun hasPlayer(name: ZPlayerName): Boolean {
		return _players.contains(name)
	}

	abstract suspend fun chooseCharacter(options: List<ZPlayerName>): ZPlayerName?
	abstract suspend fun chooseMove(cur: ZPlayerName, options: List<ZMove>): ZMove?

	abstract suspend fun chooseNewSkill(character: ZPlayerName, skillOptions: List<ZSkill>): ZSkill?
	abstract suspend fun chooseSlotToOrganize(cur: ZPlayerName, slots: List<ZEquipSlot>): ZEquipSlot?
	abstract suspend fun chooseEquipment(cur: ZPlayerName, equipOptions: List<ZEquipment<*>>): Int?
	suspend fun chooseEquipmentInternal(cur: ZPlayerName, equipOptions: List<ZEquipment<*>>): ZEquipment<*>? {
		return chooseEquipment(cur, equipOptions)?.let { equipOptions[it] }
	}

	abstract suspend fun chooseSlotForEquip(cur: ZPlayerName, equipableSlots: List<ZEquipSlot>): ZEquipSlot?
	abstract suspend fun chooseZoneToWalk(cur: ZPlayerName, zones: List<Int>): Int?
	abstract suspend fun chooseDoorToToggle(cur: ZPlayerName, doors: List<ZDoor>): Int?
	suspend fun chooseDoorToToggleInternal(cur: ZPlayerName, doors: List<ZDoor>): ZDoor? {
		return chooseDoorToToggle(cur, doors)?.let { doors[it] }
	}

	abstract suspend fun chooseWeaponSlot(c: ZPlayerName, weapons: List<ZWeapon>): Int?
	suspend fun chooseWeaponSlotInternal(c: ZPlayerName, weapons: List<ZWeapon>): ZWeapon? {
		return chooseWeaponSlot(c, weapons)?.let { weapons[it] }
	}

	abstract suspend fun chooseTradeCharacter(c: ZPlayerName, list: List<ZPlayerName>): ZPlayerName?
	abstract suspend fun chooseZoneForAttack(c: ZPlayerName, zones: List<Int>): Int?
	abstract suspend fun chooseItemToPickup(cur: ZPlayerName, list: List<ZEquipment<*>>): Int?
	suspend fun chooseItemToPickupInternal(cur: ZPlayerName, list: List<ZEquipment<*>>): ZEquipment<*>? {
		return chooseItemToPickup(cur, list)?.let { list[it] }
	}

	abstract suspend fun chooseItemToDrop(cur: ZPlayerName, list: List<ZEquipment<*>>): Int?
	suspend fun chooseItemToDropInternal(cur: ZPlayerName, list: List<ZEquipment<*>>): ZEquipment<*>? {
		return chooseItemToDrop(cur, list)?.let { list[it] }
	}

	abstract suspend fun chooseEquipmentToThrow(cur: ZPlayerName, slots: List<ZEquipment<*>>): Int?
	suspend fun chooseEquipmentToThrowInternal(cur: ZPlayerName, slots: List<ZEquipment<*>>): ZEquipment<*>? {
		return chooseEquipmentToThrow(cur, slots)?.let { slots[it] }
	}

	abstract suspend fun chooseZoneToThrowEquipment(cur: ZPlayerName, toThrow: ZEquipment<*>, zones: List<Int>): Int?
	abstract suspend fun chooseZoneToShove(cur: ZPlayerName, list: List<Int>): Int?
	abstract suspend fun chooseSpell(cur: ZPlayerName, spells: List<ZSpell>): ZSpell?
	abstract suspend fun chooseCharacterForSpell(
		cur: ZPlayerName,
		spell: ZSpell,
		targets: List<ZPlayerName>
	): ZPlayerName?

	abstract suspend fun chooseCharacterToBequeathMove(
		cur: ZPlayerName,
		list: List<ZPlayerName>
	): ZPlayerName?

	abstract suspend fun chooseZoneForBloodlust(cur: ZPlayerName, list: List<Int>): Int?
	abstract suspend fun chooseSpawnAreaToRemove(cur: ZPlayerName, list: List<ZSpawnArea>): Int?
	abstract suspend fun chooseZoneToIgnite(
		playerName: ZPlayerName,
		ignitableZones: List<Int>
	): Int?

	abstract suspend fun chooseEquipmentClass(
		playerName: ZPlayerName,
		classes: List<ZEquipmentClass>
	): ZEquipmentClass?

	abstract suspend fun chooseStartingEquipment(
		playerName: ZPlayerName,
		list: List<ZEquipmentType>
	): ZEquipmentType?

	abstract suspend fun chooseFamiliar(
		playerName: ZPlayerName,
		list: List<ZFamiliarType>
	): ZFamiliarType?

	abstract suspend fun chooseOrganize(playerName: ZPlayerName, list: List<ZMove>): ZMove?

	abstract suspend fun chooseZoneForCatapult(
		playerName: ZPlayerName,
		ammoType: ZWeaponType,
		zones: List<Int>
	): Int?

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