package cc.lib.zombicide.p2p

import cc.lib.net.AClientConnection
import cc.lib.zombicide.ZDoor
import cc.lib.zombicide.ZEquipSlot
import cc.lib.zombicide.ZEquipment
import cc.lib.zombicide.ZEquipmentClass
import cc.lib.zombicide.ZEquipmentType
import cc.lib.zombicide.ZFamiliarType
import cc.lib.zombicide.ZMove
import cc.lib.zombicide.ZPlayerName
import cc.lib.zombicide.ZSkill
import cc.lib.zombicide.ZSpawnArea
import cc.lib.zombicide.ZSpell
import cc.lib.zombicide.ZUser
import cc.lib.zombicide.ZWeapon
import cc.lib.zombicide.ZWeaponType
import cc.lib.zombicide.ui.UIZombicide

/**
 * Created by Chris Caron on 7/17/21.
 */
class ZUserMP(val connection: AClientConnection) : ZUser(connection.displayName), AClientConnection.Listener {

	init {
		connection.addListener(this)
		setColor(UIZombicide.instance.board, connection.getAttribute("color") as Int, name)
	}

	override fun onPropertyChanged(c: AClientConnection) {
		setColor(UIZombicide.instance.board, c.getAttribute("color") as Int, c.name)
	}

	override suspend fun chooseCharacter(options: List<ZPlayerName>): ZPlayerName? {
		return connection.executeDerivedOnRemote(USER_ID, true, options)
	}

	override suspend fun chooseMove(cur: ZPlayerName, options: List<ZMove>): ZMove? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, options)
	}

	override suspend fun chooseNewSkill(character: ZPlayerName, skillOptions: List<ZSkill>): ZSkill? {
		return connection.executeDerivedOnRemote(USER_ID, true, character, skillOptions)
	}

	override suspend fun chooseSlotToOrganize(cur: ZPlayerName, slots: List<ZEquipSlot>): ZEquipSlot? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, slots)
	}

	override suspend fun chooseEquipment(cur: ZPlayerName, equipOptions: List<ZEquipment<*>>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, equipOptions)
	}

	override suspend fun chooseSlotForEquip(cur: ZPlayerName, equipableSlots: List<ZEquipSlot>): ZEquipSlot? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, equipableSlots)
	}

	override suspend fun chooseZoneToWalk(cur: ZPlayerName, zones: List<Int>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, zones)
	}

	override suspend fun chooseDoorToToggle(cur: ZPlayerName, doors: List<ZDoor>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, doors)
	}

	override suspend fun chooseWeaponSlot(c: ZPlayerName, weapons: List<ZWeapon>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, c, weapons)
	}

	override suspend fun chooseTradeCharacter(c: ZPlayerName, list: List<ZPlayerName>): ZPlayerName? {
		return connection.executeDerivedOnRemote(USER_ID, true, c, list)
	}

	override suspend fun chooseZoneForAttack(c: ZPlayerName, zones: List<Int>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, c, zones)
	}

	override suspend fun chooseItemToPickup(cur: ZPlayerName, list: List<ZEquipment<*>>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, list)
	}

	override suspend fun chooseItemToDrop(cur: ZPlayerName, list: List<ZEquipment<*>>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, list)
	}

	override suspend fun chooseEquipmentToThrow(cur: ZPlayerName, slots: List<ZEquipment<*>>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, slots)
	}

	override suspend fun chooseZoneToThrowEquipment(cur: ZPlayerName, toThrow: ZEquipment<*>, zones: List<Int>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, toThrow, zones)
	}

	override suspend fun chooseZoneToShove(cur: ZPlayerName, list: List<Int>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, list)
	}

	override suspend fun chooseSpell(cur: ZPlayerName, spells: List<ZSpell>): ZSpell? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, spells)
	}

	override suspend fun chooseCharacterForSpell(cur: ZPlayerName, spell: ZSpell, targets: List<ZPlayerName>): ZPlayerName? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, spell, targets)
	}

	override suspend fun chooseCharacterToBequeathMove(cur: ZPlayerName, list: List<ZPlayerName>): ZPlayerName? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, list)
	}

	override suspend fun chooseZoneForBloodlust(cur: ZPlayerName, list: List<Int>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, list)
	}

	override suspend fun chooseSpawnAreaToRemove(cur: ZPlayerName, list: List<ZSpawnArea>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, list)
	}

	override suspend fun chooseZoneToIgnite(playerName: ZPlayerName, ignitableZones: List<Int>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, playerName, ignitableZones)
	}

	override suspend fun chooseEquipmentClass(playerName: ZPlayerName, classes: List<ZEquipmentClass>): ZEquipmentClass? {
		return connection.executeDerivedOnRemote(USER_ID, true, playerName, classes)
	}

	override suspend fun chooseStartingEquipment(playerName: ZPlayerName, list: List<ZEquipmentType>): ZEquipmentType? {
		return connection.executeDerivedOnRemote(USER_ID, true, playerName, list)
	}

	override suspend fun chooseOrganize(playerName: ZPlayerName, list: List<ZMove>): ZMove? {
		return connection.executeDerivedOnRemote(USER_ID, true, playerName, list)
	}

	override fun organizeStart(primary: ZPlayerName, secondary: ZPlayerName?) {
		connection.executeDerivedOnRemote<Void>(USER_ID, false, primary, secondary)
	}

	override fun organizeEnd() {
		connection.executeDerivedOnRemote<Void>(USER_ID, false)
	}

	override suspend fun chooseZoneForCatapult(
		playerName: ZPlayerName,
		ammoType: ZWeaponType,
		zones: List<Int>
	): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, playerName, ammoType, zones)
	}

	override suspend fun chooseFamiliar(
		playerName: ZPlayerName,
		list: List<ZFamiliarType>
	): ZFamiliarType? {
		return connection.executeDerivedOnRemote(USER_ID, true, playerName, list)
	}

	companion object {
		const val USER_ID = "ZUser"
	}
}