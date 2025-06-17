package cc.lib.zombicide

import cc.lib.game.GColor
import cc.lib.ksp.remote.IRemoteSuspend
import cc.lib.ksp.remote.Remote
import cc.lib.ksp.remote.RemoteFunction

@Remote
abstract class ZUser(_name: String?, var colorId: Int) : IRemoteSuspend {

	init {
		if (colorId !in USER_COLORS.indices)
			throw IllegalArgumentException("colorId out of bounds")
	}

	var name = _name ?: USER_COLOR_NAMES[colorId]

	private val _players = mutableSetOf<ZPlayerName>()

	val players: List<ZPlayerName>
		get() = _players.toList()

	fun clearCharacters() {
		_players.clear()
	}

	fun addCharacter(c: ZCharacter) {
		_players.add(c.type)
		c.colorId = colorId
		c.isInvisible = false
	}

	fun removeCharacter(character: ZCharacter) {
		_players.remove(character.type)
		character.colorId = -1
	}

	fun setCharacters(chars: List<ZCharacter>) {
		_players.clear()
		for (nm in chars) {
			addCharacter(nm)
		}
	}

	fun getColor(): GColor = USER_COLORS[colorId]

	fun getColorName(): String = USER_COLOR_NAMES[colorId]

	fun hasPlayer(name: ZPlayerName): Boolean {
		return _players.contains(name)
	}

	@RemoteFunction
	abstract suspend fun chooseCharacter(options: List<ZPlayerName>): ZPlayerName?

	@RemoteFunction
	abstract suspend fun chooseMove(cur: ZPlayerName, options: List<ZMove>): ZMove?

	@RemoteFunction
	abstract suspend fun chooseNewSkill(character: ZPlayerName, skillOptions: List<ZSkill>): ZSkill?

	@RemoteFunction
	abstract suspend fun chooseSlotToOrganize(cur: ZPlayerName, slots: List<ZEquipSlot>): ZEquipSlot?

	@RemoteFunction
	abstract suspend fun chooseEquipment(cur: ZPlayerName, equipOptions: List<ZEquipment<*>>): Int?
	suspend fun chooseEquipmentInternal(cur: ZPlayerName, equipOptions: List<ZEquipment<*>>): ZEquipment<*>? {
		return chooseEquipment(cur, equipOptions)?.let { equipOptions[it] }
	}

	@RemoteFunction
	abstract suspend fun chooseSlotForEquip(cur: ZPlayerName, equipableSlots: List<ZEquipSlot>): ZEquipSlot?

	@RemoteFunction
	abstract suspend fun chooseZoneToWalk(cur: ZPlayerName, zones: List<Int>): Int?

	@RemoteFunction
	abstract suspend fun chooseDoorToToggle(cur: ZPlayerName, doors: List<ZDoor>): Int?

	suspend fun chooseDoorToToggleInternal(cur: ZPlayerName, doors: List<ZDoor>): ZDoor? {
		return chooseDoorToToggle(cur, doors)?.let { doors[it] }
	}

	@RemoteFunction
	abstract suspend fun chooseWeaponSlot(c: ZPlayerName, weapons: List<ZWeapon>): Int?
	suspend fun chooseWeaponSlotInternal(c: ZPlayerName, weapons: List<ZWeapon>): ZWeapon? {
		return chooseWeaponSlot(c, weapons)?.let { weapons[it] }
	}

	@RemoteFunction
	abstract suspend fun chooseTradeCharacter(c: ZPlayerName, list: List<ZPlayerName>): ZPlayerName?

	@RemoteFunction
	abstract suspend fun chooseZoneForAttack(c: ZPlayerName, zones: List<Int>): Int?

	@RemoteFunction
	abstract suspend fun chooseItemToPickup(cur: ZPlayerName, list: List<ZEquipment<*>>): Int?
	suspend fun chooseItemToPickupInternal(cur: ZPlayerName, list: List<ZEquipment<*>>): ZEquipment<*>? {
		return chooseItemToPickup(cur, list)?.let { list[it] }
	}

	@RemoteFunction
	abstract suspend fun chooseItemToDrop(cur: ZPlayerName, list: List<ZEquipment<*>>): Int?

	suspend fun chooseItemToDropInternal(cur: ZPlayerName, list: List<ZEquipment<*>>): ZEquipment<*>? {
		return chooseItemToDrop(cur, list)?.let { list[it] }
	}

	@RemoteFunction
	abstract suspend fun chooseEquipmentToThrow(cur: ZPlayerName, slots: List<ZEquipment<*>>): Int?
	suspend fun chooseEquipmentToThrowInternal(
		cur: ZPlayerName,
		slots: List<ZEquipment<*>>
	): ZEquipment<*>? {
		return chooseEquipmentToThrow(cur, slots)?.let { slots[it] }
	}

	@RemoteFunction
	abstract suspend fun chooseZoneToThrowEquipment(
		cur: ZPlayerName,
		toThrow: ZEquipment<*>,
		zones: List<Int>
	): Int?

	@RemoteFunction
	abstract suspend fun chooseZoneToShove(cur: ZPlayerName, list: List<Int>): Int?

	@RemoteFunction
	abstract suspend fun chooseSpell(cur: ZPlayerName, spells: List<ZSpell>): ZSpell?

	@RemoteFunction
	abstract suspend fun chooseCharacterForSpell(
		cur: ZPlayerName,
		spell: ZSpell,
		targets: List<ZPlayerName>
	): ZPlayerName?

	@RemoteFunction
	abstract suspend fun chooseCharacterToBequeathMove(
		cur: ZPlayerName,
		list: List<ZPlayerName>
	): ZPlayerName?

	@RemoteFunction
	abstract suspend fun chooseZoneForBloodlust(cur: ZPlayerName, list: List<Int>): Int?

	@RemoteFunction
	abstract suspend fun chooseSpawnAreaToRemove(cur: ZPlayerName, list: List<ZSpawnArea>): Int?

	@RemoteFunction
	abstract suspend fun chooseZoneToIgnite(
		playerName: ZPlayerName,
		ignitableZones: List<Int>
	): Int?

	@RemoteFunction
	abstract suspend fun chooseEquipmentClass(
		playerName: ZPlayerName,
		classes: List<ZEquipmentClass>
	): ZEquipmentClass?

	@RemoteFunction
	abstract suspend fun chooseStartingEquipment(
		playerName: ZPlayerName,
		list: List<ZEquipmentType>
	): ZEquipmentType?

	@RemoteFunction
	abstract suspend fun chooseFamiliar(
		playerName: ZPlayerName,
		list: List<ZFamiliarType>
	): ZFamiliarType?

	@RemoteFunction
	abstract suspend fun chooseOrganize(playerName: ZPlayerName, list: List<ZMove>, undos: Int): ZMove?

	@RemoteFunction
	abstract suspend fun chooseZoneForCatapult(
		playerName: ZPlayerName,
		ammoType: ZWeaponType,
		zones: List<Int>
	): Int?

	/**
	 * This may be called more than once. Need to handle if a dialog is already open
	 */
	@RemoteFunction
	open suspend fun organizeStart(primary: ZPlayerName, secondary: ZPlayerName?, undos: Int) {
		throw NotImplementedError()
	}

	@RemoteFunction
	open suspend fun organizeEnd() {
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

		fun getColorName(id: Int): String = if (id in USER_COLORS.indices) {
			USER_COLOR_NAMES[id]
		} else "None($id)"

		fun getColorName(color: GColor): String = USER_COLORS.indexOfFirst { it == color }.takeIf { it >= 0 }?.let {
			USER_COLOR_NAMES[it]
		} ?: "Unknown($color)"
	}
}