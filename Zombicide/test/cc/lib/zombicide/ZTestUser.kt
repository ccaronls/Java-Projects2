package cc.lib.zombicide

import cc.lib.utils.randomIndex

class ZTestUser(vararg names: ZPlayerName?) : ZUser("test", 0) {

	override suspend fun chooseCharacter(options: List<ZPlayerName>) = options.random()

	override suspend fun chooseMove(cur: ZPlayerName, options: List<ZMove>) = options.random()

	override suspend fun chooseNewSkill(character: ZPlayerName, skillOptions: List<ZSkill>) = skillOptions.random()

	override suspend fun chooseSlotToOrganize(cur: ZPlayerName, slots: List<ZEquipSlot>) = slots.random()

	override suspend fun chooseEquipment(cur: ZPlayerName, equipOptions: List<ZEquipment<*>>) = equipOptions.randomIndex()

	override suspend fun chooseSlotForEquip(cur: ZPlayerName, equipableSlots: List<ZEquipSlot>) = equipableSlots.random()
	override suspend fun chooseZoneToWalk(cur: ZPlayerName, zones: List<Int>) = zones.random()

	override suspend fun chooseDoorToToggle(cur: ZPlayerName, doors: List<ZDoor>) = doors.randomIndex()

	override suspend fun chooseWeaponSlot(c: ZPlayerName, weapons: List<ZWeapon>) = weapons.randomIndex()

	override suspend fun chooseTradeCharacter(c: ZPlayerName, list: List<ZPlayerName>) = list.random()

	override suspend fun chooseZoneForAttack(c: ZPlayerName, zones: List<Int>) = zones.random()

	override suspend fun chooseItemToPickup(cur: ZPlayerName, list: List<ZEquipment<*>>) = list.randomIndex()
	override suspend fun chooseItemToDrop(cur: ZPlayerName, list: List<ZEquipment<*>>) = list.randomIndex()

	override suspend fun chooseEquipmentToThrow(cur: ZPlayerName, slots: List<ZEquipment<*>>) = slots.randomIndex()

	override suspend fun chooseZoneToThrowEquipment(cur: ZPlayerName, toThrow: ZEquipment<*>, zones: List<Int>) = zones.random()

	override suspend fun chooseZoneToShove(cur: ZPlayerName, list: List<Int>) = list.random()

	override suspend fun chooseSpell(cur: ZPlayerName, spells: List<ZSpell>) = spells.random()

	override suspend fun chooseCharacterForSpell(cur: ZPlayerName, spell: ZSpell, targets: List<ZPlayerName>) = targets.random()

	override suspend fun chooseCharacterToBequeathMove(cur: ZPlayerName, list: List<ZPlayerName>) = list.random()

	override suspend fun chooseZoneForBloodlust(cur: ZPlayerName, list: List<Int>) = list.random()

	override suspend fun chooseSpawnAreaToRemove(cur: ZPlayerName, list: List<ZSpawnArea>) = list.randomIndex()

	override suspend fun chooseZoneToIgnite(playerName: ZPlayerName, ignitableZones: List<Int>) = ignitableZones.random()

	override suspend fun chooseEquipmentClass(playerName: ZPlayerName, classes: List<ZEquipmentClass>) = classes.random()

	override suspend fun chooseStartingEquipment(playerName: ZPlayerName, list: List<ZEquipmentType>) = list.random()

	override suspend fun chooseFamiliar(playerName: ZPlayerName, list: List<ZFamiliarType>) = list.random()

	override suspend fun chooseOrganize(playerName: ZPlayerName, list: List<ZMove>, undos: Int) = list.random()

	override suspend fun chooseZoneForCatapult(playerName: ZPlayerName, ammoType: ZWeaponType, zones: List<Int>) =
		zones.random()
}
