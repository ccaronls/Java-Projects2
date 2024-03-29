package cc.lib.zombicide.ui

import cc.lib.ui.ButtonHandler
import cc.lib.utils.Table
import cc.lib.utils.prettify
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

open class UIZUser(name: String) : ZUser(name) {
	fun <T> indexOrNull(item: T?, options: List<T>): Int? {
		return if (item == null) null else options.indexOf(item)
	}

	override fun chooseCharacter(characters: List<ZPlayerName>): ZPlayerName? {
		return UIZombicide.instance.pickCharacter(null, "Pick character to play", characters)
	}

	override fun chooseMove(cur: ZPlayerName, moves: List<ZMove>): ZMove? {
		return UIZombicide.instance.pickMenu(
			cur,
			cur.name + " Choose Move",
			ZMove::class.java,
			moves
		)
	}

	override fun chooseNewSkill(cur: ZPlayerName, list: List<ZSkill>): ZSkill? {
		val table = Table(object : Table.Model {
			override fun getMaxCharsPerLine(): Int {
				return 32
			}
		})
		for (t in list) {
			table.addColumn(t.name.prettify(), t.description)
		}
		UIZombicide.instance.boardRenderer.setOverlay(table)
		return UIZombicide.instance.pickMenu(
			cur,
			cur.name + " Choose New Skill",
			ZSkill::class.java,
			list
		)
	}

	override fun chooseSlotToOrganize(cur: ZPlayerName, slots: List<ZEquipSlot>): ZEquipSlot? {
		return UIZombicide.instance.pickMenu(
			cur,
			cur.name + " Choose Slot to Organize",
			ZEquipSlot::class.java,
			slots
		)
	}

	override fun chooseEquipment(cur: ZPlayerName, list: List<ZEquipment<*>>): Int? {
		UIZombicide.instance.showEquipmentOverlay(cur, list)
		return indexOrNull(
			UIZombicide.instance.pickMenu(
				cur,
				cur.name + " Choose Equipment to Organize",
				ZEquipment::class.java,
				list
			), list as List<*>
		)
	}

	override fun chooseSlotForEquip(
		cur: ZPlayerName,
		equipableSlots: List<ZEquipSlot>
	): ZEquipSlot? {
		return UIZombicide.instance.pickMenu(
			cur,
			cur.name + " Choose Slot to Equip Item",
			ZEquipSlot::class.java,
			equipableSlots
		)
	}

	override fun chooseZoneToWalk(cur: ZPlayerName, zones: List<Int>): Int? {
		return UIZombicide.instance.pickZone(cur, cur.name + " Choose zone to Walk", zones)
	}

	override fun chooseDoorToToggle(cur: ZPlayerName, _doors: List<ZDoor>): Int? {
		val doors =
			_doors.map { UIZombicide.instance.board.findDoor(it.cellPosStart, it.moveDirection) }
		return indexOrNull(
			UIZombicide.instance.pickDoor(
				cur,
				cur.name + " Choose door to open or close",
				doors
			), doors
		)
	}

	override fun chooseWeaponSlot(cur: ZPlayerName, weapons: List<ZWeapon>): Int? {
		return indexOrNull(
			UIZombicide.instance.pickMenu(
				cur,
				cur.name + " Choose weapon from slot",
				ZWeapon::class.java,
				weapons
			), weapons
		)
	}

	override fun chooseTradeCharacter(cur: ZPlayerName, list: List<ZPlayerName>): ZPlayerName? {
		return UIZombicide.instance.pickCharacter(
			cur,
			cur.name + " Choose Character for Trade",
			list as List<ZPlayerName>
		)
	}

	override fun chooseZoneForAttack(c: ZPlayerName, zones: List<Int>): Int? {
		return UIZombicide.instance.pickZone(c, "Choose Zone to Attack", zones)
	}

	override fun chooseItemToPickup(cur: ZPlayerName, list: List<ZEquipment<*>>): Int? {
		UIZombicide.instance.showEquipmentOverlay(cur, list)
		return indexOrNull(
			UIZombicide.instance.pickMenu(
				cur,
				"Choose Item to Pickup",
				ZEquipment::class.java,
				list
			), list as List<*>
		)
	}

	override fun chooseItemToDrop(cur: ZPlayerName, list: List<ZEquipment<*>>): Int? {
		UIZombicide.instance.showEquipmentOverlay(cur, list)
		return indexOrNull(
			UIZombicide.instance.pickMenu(
				cur,
				"Choose Item to Drop",
				ZEquipment::class.java,
				list
			), list as List<*>
		)
	}

	override fun chooseEquipmentToThrow(cur: ZPlayerName, list: List<ZEquipment<*>>): Int? {
		UIZombicide.instance.showEquipmentOverlay(cur, list)
		return indexOrNull(
			UIZombicide.instance.pickMenu(
				cur,
				"Choose Item to Throw",
				ZEquipment::class.java,
				list
			), list as List<*>
		)
	}

	override fun chooseZoneToThrowEquipment(
		cur: ZPlayerName,
		toThrow: ZEquipment<*>,
		zones: List<Int>
	): Int? {
		UIZombicide.instance.showEquipmentOverlay(cur, listOf(toThrow))
		return UIZombicide.instance.pickZone(cur, "Choose Zone to throw the $toThrow", zones)
	}

	override fun chooseZoneToShove(cur: ZPlayerName, zones: List<Int>): Int? {
		return UIZombicide.instance.pickZone(cur, "Choose Zone to shove zombies into", zones)
	}

	override fun chooseSpell(cur: ZPlayerName, spells: List<ZSpell>): ZSpell? {
		UIZombicide.instance.showEquipmentOverlay(cur, spells)
		return UIZombicide.instance.pickMenu(cur, "Choose Spell", ZSpell::class.java, spells)
	}

	override fun chooseCharacterForSpell(
		cur: ZPlayerName,
		spell: ZSpell,
		targets: List<ZPlayerName>
	): ZPlayerName? {
		UIZombicide.instance.showEquipmentOverlay(cur, listOf(spell))
		return UIZombicide.instance.pickCharacter(
			cur,
			"Choose character to enchant with " + spell.type,
			targets
		)
	}

	override fun chooseCharacterToBequeathMove(
		cur: ZPlayerName,
		targets: List<ZPlayerName>
	): ZPlayerName? {
		return UIZombicide.instance.pickCharacter(
			cur,
			"Choose character to bequeath an extra action",
			targets
		)
	}

	override fun chooseZoneForBloodlust(cur: ZPlayerName, list: List<Int>): Int? {
		return UIZombicide.instance.pickZone(cur, "Choose Zone for Bloodlust", list)
	}

	override fun chooseSpawnAreaToRemove(cur: ZPlayerName, list: List<ZSpawnArea>): Int? {
		return UIZombicide.instance.pickSpawn(cur, "Choose SPAWN Area to Remove", list)
	}

	override fun chooseZoneToIgnite(playerName: ZPlayerName, ignitableZones: List<Int>): Int? {
		return UIZombicide.instance.pickZone(playerName, "Choose Zone to Ignite", ignitableZones)
	}

	override fun chooseEquipmentClass(
		playerName: ZPlayerName,
		classes: List<ZEquipmentClass>
	): ZEquipmentClass? {
		return UIZombicide.instance.pickMenu(
			playerName,
			"Choose Equipment Class",
			ZEquipmentClass::class.java,
			classes
		)
	}

	override fun chooseStartingEquipment(
		playerName: ZPlayerName,
		list: List<ZEquipmentType>
	): ZEquipmentType? {
		val table = Table().setNoBorder()
		for (t in list) {
			table.addColumnNoHeaderVarArg(
				t.create().getCardInfo(
					UIZombicide.instance.board.getCharacter(playerName),
					UIZombicide.instance
				).also {
					UIZombicide.instance.boardRenderer.addButton(it, object : ButtonHandler() {
						override fun onClick(): Boolean {
							UIZombicide.instance.setResult(t)
							return true
						}

						override fun onHoverEnter() {
							it.highlighted = true
						}

						override fun onHoverExit() {
							it.highlighted = false
						}
					})
				}

			)
		}
		UIZombicide.instance.boardRenderer.setOverlay(Table().addColumn("Choose Starting Equipment", table))
		return UIZombicide.instance.pickMenu(playerName, "Choose Starting Equipment", ZEquipmentType::class.java, list)
	}

	override fun chooseOrganize(playerName: ZPlayerName, list: List<ZMove>): ZMove? {
		return UIZombicide.instance.updateOrganize(
			UIZombicide.instance.board.getCharacter(
				playerName
			), list
		)
	}

	override fun organizeStart(primary: ZPlayerName, secondary: ZPlayerName?) {
		UIZombicide.instance.showOrganizeDialog(primary, secondary)
	}

	override fun organizeEnd() {
		UIZombicide.instance.closeOrganizeDialog()
	}

	override fun chooseZoneForCatapult(
		playerName: ZPlayerName,
		ammoType: ZWeaponType,
		zones: List<Int>
	): Int? {
		return UIZombicide.instance.pickZone(
			playerName,
			"Pick Zone for ${ammoType.prettify()}",
			zones
		)
	}

	override fun chooseFamiliar(
		playerName: ZPlayerName,
		list: List<ZFamiliarType>
	): ZFamiliarType? {
		return UIZombicide.instance.pickMenu(
			playerName,
			"Choose Familiar",
			ZFamiliarType::class.java,
			list
		)
	}
}