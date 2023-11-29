package cc.lib.zombicide.p2p

import cc.lib.net.AClientConnection
import cc.lib.zombicide.*
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

	override fun chooseCharacter(options: List<ZPlayerName>): ZPlayerName? {
		return connection.executeDerivedOnRemote(USER_ID, true, options)
	}

	override fun chooseMove(cur: ZPlayerName, options: List<ZMove>): ZMove? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, options)
	}

	override fun chooseNewSkill(character: ZPlayerName, skillOptions: List<ZSkill>): ZSkill? {
		return connection.executeDerivedOnRemote(USER_ID, true, character, skillOptions)
	}

	override fun chooseSlotToOrganize(cur: ZPlayerName, slots: List<ZEquipSlot>): ZEquipSlot? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, slots)
	}

	override fun chooseEquipment(cur: ZPlayerName, equipOptions: List<ZEquipment<*>>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, equipOptions)
	}

	override fun chooseSlotForEquip(cur: ZPlayerName, equipableSlots: List<ZEquipSlot>): ZEquipSlot? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, equipableSlots)
	}

	override fun chooseZoneToWalk(cur: ZPlayerName, zones: List<Int>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, zones)
	}

	override fun chooseDoorToToggle(cur: ZPlayerName, doors: List<ZDoor>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, doors)
	}

	override fun chooseWeaponSlot(c: ZPlayerName, weapons: List<ZWeapon>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, c, weapons)
	}

	override fun chooseTradeCharacter(c: ZPlayerName, list: List<ZPlayerName>): ZPlayerName? {
		return connection.executeDerivedOnRemote(USER_ID, true, c, list)
	}

	override fun chooseZoneForAttack(c: ZPlayerName, zones: List<Int>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, c, zones)
	}

	override fun chooseItemToPickup(cur: ZPlayerName, list: List<ZEquipment<*>>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, list)
	}

	override fun chooseItemToDrop(cur: ZPlayerName, list: List<ZEquipment<*>>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, list)
	}

	override fun chooseEquipmentToThrow(cur: ZPlayerName, slots: List<ZEquipment<*>>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, slots)
	}

	override fun chooseZoneToThrowEquipment(cur: ZPlayerName, toThrow: ZEquipment<*>, zones: List<Int>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, toThrow, zones)
	}

	override fun chooseZoneToShove(cur: ZPlayerName, list: List<Int>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, list)
	}

	override fun chooseSpell(cur: ZPlayerName, spells: List<ZSpell>): ZSpell? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, spells)
	}

	override fun chooseCharacterForSpell(cur: ZPlayerName, spell: ZSpell, targets: List<ZPlayerName>): ZPlayerName? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, spell, targets)
	}

	override fun chooseCharacterToBequeathMove(cur: ZPlayerName, list: List<ZPlayerName>): ZPlayerName? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, list)
	}

	override fun chooseZoneForBloodlust(cur: ZPlayerName, list: List<Int>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, list)
	}

	override fun chooseSpawnAreaToRemove(cur: ZPlayerName, list: List<ZSpawnArea>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, cur, list)
	}

	override fun chooseZoneToIgnite(playerName: ZPlayerName, ignitableZones: List<Int>): Int? {
		return connection.executeDerivedOnRemote(USER_ID, true, playerName, ignitableZones)
	}

	override fun chooseEquipmentClass(playerName: ZPlayerName, classes: List<ZEquipmentClass>): ZEquipmentClass? {
		return connection.executeDerivedOnRemote(USER_ID, true, playerName, classes)
	}

	override fun chooseStartingEquipment(playerName: ZPlayerName, list: List<ZEquipmentType>): ZEquipmentType? {
		return connection.executeDerivedOnRemote(USER_ID, true, playerName, list)
	}

	override fun chooseOrganize(playerName: ZPlayerName, list: List<ZMove>): ZMove? {
		return connection.executeDerivedOnRemote(USER_ID, true, playerName, list)
	}

	override fun organizeStart(primary: ZPlayerName, secondary: ZPlayerName?) {
		connection.executeDerivedOnRemote<Void>(USER_ID, false, primary, secondary)
	}

	override fun organizeEnd() {
		connection.executeDerivedOnRemote<Void>(USER_ID, false)
	}

	companion object {
		const val USER_ID = "ZUser"
	}
}