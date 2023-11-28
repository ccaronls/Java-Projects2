package cc.lib.zombicide.ui

import cc.lib.utils.Table
import cc.lib.utils.prettify
import cc.lib.zombicide.*

class UIZUser(name: String) : ZUser(name) {
	fun <T> indexOrNull(item: T?, options: List<T>): Int? {
		return if (item == null) null else options.indexOf(item)
	}

	override fun chooseCharacter(characters: List<ZPlayerName>): ZPlayerName? {
		return UIZombicide.instance.pickCharacter("Pick character to play", characters)
	}

	override fun chooseMove(cur: ZPlayerName, moves: List<ZMove>): ZMove? {
		return UIZombicide.instance.pickMenu(cur, cur.name + " Choose Move", ZMove::class.java, moves)
	}

	override fun chooseNewSkill(cur: ZPlayerName, list: List<ZSkill>): ZSkill? {
		val table = Table(object : Table.Model {
			override fun getMaxCharsPerLine(): Int {
				return 32
			}
		})
		for (t in list) {
			table.addColumn(prettify(t.name), t.description)
		}
		UIZombicide.instance.boardRenderer.setOverlay(table)
		return UIZombicide.instance.pickMenu(cur, cur.name + " Choose New Skill", ZSkill::class.java, list)
	}

	override fun chooseSlotToOrganize(cur: ZPlayerName, slots: List<ZEquipSlot>): ZEquipSlot? {
		return UIZombicide.instance.pickMenu(cur, cur.name + " Choose Slot to Organize", ZEquipSlot::class.java, slots)
	}

	override fun chooseEquipment(cur: ZPlayerName, list: List<ZEquipment<*>>): Int? {
		UIZombicide.instance.showEquipmentOverlay(cur, list)
		return indexOrNull(UIZombicide.instance.pickMenu(cur, cur.name + " Choose Equipment to Organize", ZEquipment::class.java, list), list as List<*>)
	}

	override fun chooseSlotForEquip(cur: ZPlayerName, equipableSlots: List<ZEquipSlot>): ZEquipSlot? {
		return UIZombicide.instance.pickMenu(cur, cur.name + " Choose Slot to Equip Item", ZEquipSlot::class.java, equipableSlots)
	}

	override fun chooseZoneToWalk(cur: ZPlayerName, zones: List<Int>): Int? {
		return UIZombicide.instance.pickZone(cur.name + " Choose zone to Walk", zones)
	}

	override fun chooseDoorToToggle(cur: ZPlayerName, doors: List<ZDoor>): Int? {
		return indexOrNull(UIZombicide.instance.pickDoor(cur.name + " Choose door to open or close", doors), doors)
	}

	override fun chooseWeaponSlot(cur: ZPlayerName, weapons: List<ZWeapon>): Int? {
		return indexOrNull(UIZombicide.instance.pickMenu(cur, cur.name + " Choose weapon from slot", ZWeapon::class.java, weapons), weapons)
	}

	override fun chooseTradeCharacter(cur: ZPlayerName, list: List<ZPlayerName>): ZPlayerName? {
		return UIZombicide.instance.pickCharacter(cur.name + " Choose Character for Trade", list as List<ZPlayerName>)
	}

	override fun chooseZoneForAttack(c: ZPlayerName, zones: List<Int>): Int? {
		return UIZombicide.instance.pickZone("Choose Zone to Attack", zones)
	}

	override fun chooseItemToPickup(cur: ZPlayerName, list: List<ZEquipment<*>>): Int? {
		UIZombicide.instance.showEquipmentOverlay(cur, list)
		return indexOrNull(UIZombicide.instance.pickMenu(cur, "Choose Item to Pickup", ZEquipment::class.java, list), list as List<*>)
	}

	override fun chooseItemToDrop(cur: ZPlayerName, list: List<ZEquipment<*>>): Int? {
		UIZombicide.instance.showEquipmentOverlay(cur, list)
		return indexOrNull(UIZombicide.instance.pickMenu(cur, "Choose Item to Drop", ZEquipment::class.java, list), list as List<*>)
	}

	override fun chooseEquipmentToThrow(cur: ZPlayerName, list: List<ZEquipment<*>>): Int? {
		UIZombicide.instance.showEquipmentOverlay(cur, list)
		return indexOrNull(UIZombicide.instance.pickMenu(cur, "Choose Item to Throw", ZEquipment::class.java, list), list as List<*>)
	}

	override fun chooseZoneToThrowEquipment(cur: ZPlayerName, toThrow: ZEquipment<*>, zones: List<Int>): Int? {
		UIZombicide.instance.showEquipmentOverlay(cur, listOf(toThrow))
		return UIZombicide.instance.pickZone("Choose Zone to throw the $toThrow", zones)
	}

	override fun chooseZoneToShove(cur: ZPlayerName, zones: List<Int>): Int? {
		return UIZombicide.instance.pickZone("Choose Zone to shove zombies into", zones)
	}

	override fun chooseSpell(cur: ZPlayerName, spells: List<ZSpell>): ZSpell? {
		UIZombicide.instance.showEquipmentOverlay(cur, spells)
		return UIZombicide.instance.pickMenu(cur, "Choose Spell", ZSpell::class.java, spells)
	}

	override fun chooseCharacterForSpell(cur: ZPlayerName, spell: ZSpell, targets: List<ZPlayerName>): ZPlayerName? {
		UIZombicide.instance.showEquipmentOverlay(cur, listOf(spell))
		return UIZombicide.instance.pickCharacter("Choose character to enchant with " + spell.type, targets)
	}

	override fun chooseCharacterToBequeathMove(cur: ZPlayerName, targets: List<ZPlayerName>): ZPlayerName? {
		return UIZombicide.instance.pickCharacter("Choose character to bequeath an extra action", targets)
	}

	override fun chooseZoneForBloodlust(cur: ZPlayerName, list: List<Int>): Int? {
		return UIZombicide.instance.pickZone("Choose Zone for Bloodlust", list)
	}

	override fun chooseSpawnAreaToRemove(cur: ZPlayerName, list: List<ZSpawnArea>): Int? {
		return UIZombicide.instance.pickSpawn("Choose SPAWN Area to Remove", list)
	}

	override fun chooseZoneToIgnite(playerName: ZPlayerName, ignitableZones: List<Int>): Int? {
		return UIZombicide.instance.pickZone("Choose Zone to Ignite", ignitableZones)
	}

	override fun chooseEquipmentClass(playerName: ZPlayerName, classes: List<ZEquipmentClass>): ZEquipmentClass? {
		return UIZombicide.instance.pickMenu(playerName, "Choose Equipment Class", ZEquipmentClass::class.java, classes)
	}

	override fun chooseStartingEquipment(playerName: ZPlayerName, list: List<ZEquipmentType>): ZEquipmentType? {
		val table = Table().setNoBorder()
		for (t in list) {
			table.addColumnNoHeaderVarArg(t.create().getCardInfo(UIZombicide.instance.board.getCharacter(playerName), UIZombicide.instance))
		}
		UIZombicide.instance.boardRenderer.setOverlay(Table().addColumn("Choose Starting Equipment", table))
		return UIZombicide.instance.pickMenu(playerName, "Choose Starting Equipment", ZEquipmentType::class.java, list)
	}

	override fun chooseOrganize(playerName: ZPlayerName, list: List<ZMove>): ZMove? {
		return UIZombicide.instance.updateOrganize(UIZombicide.instance.board.getCharacter(playerName), list)
	}

	override fun organizeStart(primary: ZPlayerName, secondary: ZPlayerName?) {
		UIZombicide.instance.showOrganizeDialog(primary, secondary)
	}

	override fun organizeEnd() {
		UIZombicide.instance.closeOrganizeDialog()
	}

}