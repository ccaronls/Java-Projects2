package cc.game.zombicide.ui

import cc.game.zombicide.ZDoor
import cc.game.zombicide.ZEquipSlot
import cc.game.zombicide.ZEquipment
import cc.game.zombicide.ZEquipmentClass
import cc.game.zombicide.ZEquipmentType
import cc.game.zombicide.ZFamiliarType
import cc.game.zombicide.ZMove
import cc.game.zombicide.ZPlayerName
import cc.game.zombicide.ZSkill
import cc.game.zombicide.ZSpawnArea
import cc.game.zombicide.ZSpell
import cc.game.zombicide.ZUserRemote
import cc.game.zombicide.ZWeapon
import cc.game.zombicide.ZWeaponType
import cc.lib.ui.ButtonHandler
import cc.lib.utils.Table
import cc.lib.utils.prettify

open class UIZUser(name: String?, colorId: Int) : ZUserRemote(name, colorId) {
	fun <T> indexOrNull(item: T?, options: List<T>): Int? {
		return if (item == null) null else options.indexOf(item)
	}

	override suspend fun chooseCharacter(characters: List<ZPlayerName>): ZPlayerName? {
		return UIZombicide.instance.pickCharacter(null, "Pick character to play", characters)
	}

	override suspend fun chooseMove(cur: ZPlayerName, moves: List<ZMove>): ZMove? {
		return UIZombicide.instance.pickMenu(
			cur,
			cur.name + " Choose Move",
			ZMove::class.java,
			moves
		)
	}

	override suspend fun chooseNewSkill(cur: ZPlayerName, list: List<ZSkill>): ZSkill? {
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

	override suspend fun chooseSlotToOrganize(cur: ZPlayerName, slots: List<ZEquipSlot>): ZEquipSlot? {
		return UIZombicide.instance.pickMenu(
			cur,
			cur.name + " Choose Slot to Organize",
			ZEquipSlot::class.java,
			slots
		)
	}

	override suspend fun chooseEquipment(cur: ZPlayerName, list: List<ZEquipment<*>>): Int? {
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

	override suspend fun chooseSlotForEquip(
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

	override suspend fun chooseZoneToWalk(cur: ZPlayerName, zones: List<Int>): Int? {
		return UIZombicide.instance.pickZone(cur, cur.name + " Choose zone to Walk", zones)
	}

	override suspend fun chooseDoorToToggle(cur: ZPlayerName, _doors: List<ZDoor>): Int? {
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

	override suspend fun chooseWeaponSlot(cur: ZPlayerName, weapons: List<ZWeapon>): Int? {
		return indexOrNull(
			UIZombicide.instance.pickMenu(
				cur,
				cur.name + " Choose weapon from slot",
				ZWeapon::class.java,
				weapons
			), weapons
		)
	}

	override suspend fun chooseTradeCharacter(cur: ZPlayerName, list: List<ZPlayerName>): ZPlayerName? {
		return UIZombicide.instance.pickCharacter(
			cur,
			cur.name + " Choose Character for Trade",
			list as List<ZPlayerName>
		)
	}

	override suspend fun chooseZoneForAttack(c: ZPlayerName, zones: List<Int>): Int? {
		return UIZombicide.instance.pickZone(c, "Choose Zone to Attack", zones)
	}

	override suspend fun chooseItemToPickup(cur: ZPlayerName, list: List<ZEquipment<*>>): Int? {
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

	override suspend fun chooseItemToDrop(cur: ZPlayerName, list: List<ZEquipment<*>>): Int? {
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

	override suspend fun chooseEquipmentToThrow(cur: ZPlayerName, list: List<ZEquipment<*>>): Int? {
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

	override suspend fun chooseZoneToThrowEquipment(
		cur: ZPlayerName,
		toThrow: ZEquipment<*>,
		zones: List<Int>
	): Int? {
		UIZombicide.instance.showEquipmentOverlay(cur, listOf(toThrow))
		return UIZombicide.instance.pickZone(cur, "Choose Zone to throw the $toThrow", zones)
	}

	override suspend fun chooseZoneToShove(cur: ZPlayerName, zones: List<Int>): Int? {
		return UIZombicide.instance.pickZone(cur, "Choose Zone to shove zombies into", zones)
	}

	override suspend fun chooseSpell(cur: ZPlayerName, spells: List<ZSpell>): ZSpell? {
		UIZombicide.instance.showEquipmentOverlay(cur, spells)
		return UIZombicide.instance.pickMenu(cur, "Choose Spell", ZSpell::class.java, spells)
	}

	override suspend fun chooseCharacterForSpell(
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

	override suspend fun chooseCharacterToBequeathMove(
		cur: ZPlayerName,
		targets: List<ZPlayerName>
	): ZPlayerName? {
		return UIZombicide.instance.pickCharacter(
			cur,
			"Choose character to bequeath an extra action",
			targets
		)
	}

	override suspend fun chooseZoneForBloodlust(cur: ZPlayerName, list: List<Int>): Int? {
		return UIZombicide.instance.pickZone(cur, "Choose Zone for Bloodlust", list)
	}

	override suspend fun chooseSpawnAreaToRemove(cur: ZPlayerName, list: List<ZSpawnArea>): Int? {
		return UIZombicide.instance.pickSpawn(cur, "Choose SPAWN Area to Remove", list)
	}

	override suspend fun chooseZoneToIgnite(playerName: ZPlayerName, ignitableZones: List<Int>): Int? {
		return UIZombicide.instance.pickZone(playerName, "Choose Zone to Ignite", ignitableZones)
	}

	override suspend fun chooseEquipmentClass(
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

	override suspend fun chooseStartingEquipment(
		playerName: ZPlayerName,
		list: List<ZEquipmentType>
	): ZEquipmentType? {
		val table = createOptionsTable(playerName, list)
		UIZombicide.instance.boardRenderer.setOverlay(Table().addColumn("Choose Starting Equipment", table))
		return UIZombicide.instance.pickMenu(playerName, "Choose Starting Equipment", ZEquipmentType::class.java, list)
	}

	override suspend fun chooseOrganize(playerName: ZPlayerName, list: List<ZMove>, undos: Int): ZMove? {
		return UIZombicide.instance.updateOrganize(
			UIZombicide.instance.board.getCharacter(
				playerName
			), list, undos
		)
	}

	override suspend fun organizeStart(primary: ZPlayerName, secondary: ZPlayerName?, undos: Int) {
		UIZombicide.instance.showOrganizeDialog(primary, secondary, undos)
	}

	override suspend fun organizeEnd() {
		UIZombicide.instance.closeOrganizeDialog()
	}

	override suspend fun chooseZoneForCatapult(
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

	override suspend fun chooseFamiliar(
		playerName: ZPlayerName,
		list: List<ZFamiliarType>
	): ZFamiliarType? {
		val table = createOptionsTable(playerName, list)
		UIZombicide.instance.boardRenderer.setOverlay(Table().addColumn("Choose Starting Familiar", table))
		return UIZombicide.instance.pickMenu(
			playerName,
			"Choose Familiar",
			ZFamiliarType::class.java,
			list
		)
	}

	private fun createOptionsTable(
		playerName: ZPlayerName,
		list: List<ZEquipmentType>
	): Table {
		if (list.size > 3) {
			val table = Table().setNoBorder()
			if (list.size == 4) {
				table.addRow(createOptionsRow(playerName, list.take(2)))
				table.addRow(createOptionsRow(playerName, list.takeLast(2)))
			} else {
				table.addRow(createOptionsRow(playerName, list.take(3)))
				table.addRow(createOptionsRow(playerName, list.subList(3, list.lastIndex + 1)))
			}
			return table
		}
		return createOptionsRow(playerName, list)
	}

	private fun createOptionsRow(
		playerName: ZPlayerName,
		list: List<ZEquipmentType>
	): Table {
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
							UIZombicide.instance.boardRenderer.removeButton(it)
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
		return table
	}
}