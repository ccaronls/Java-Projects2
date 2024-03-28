package cc.lib.zombicide.quests

import cc.lib.utils.Grid
import cc.lib.utils.Table
import cc.lib.zombicide.ZCell
import cc.lib.zombicide.ZCharacter
import cc.lib.zombicide.ZDoor
import cc.lib.zombicide.ZEquipment
import cc.lib.zombicide.ZGame
import cc.lib.zombicide.ZItemType
import cc.lib.zombicide.ZQuest
import cc.lib.zombicide.ZQuests
import cc.lib.zombicide.ZTile
import cc.lib.zombicide.ZTile.Companion.getQuadrant
import cc.lib.zombicide.ZZombieType

class ZQuestTheEvilTemple : ZQuest(ZQuests.The_Evil_Temple) {
	companion object {
		init {
			addAllFields(ZQuestTheEvilTemple::class.java)
		}
	}

	var blueObjZone = -1
	var greenObjZone = -1
	var violetVaultZone = -1
	var goldVaultZone = -1
	lateinit var goldVaultDoor: ZDoor
	lateinit var violetVaultDoor: ZDoor
	override val tiles
		get() = arrayOf(
			ZTile("8R", 180, getQuadrant(0, 0)),
			ZTile("9V", 180, getQuadrant(0, 3)),
			ZTile("1V", 270, getQuadrant(0, 6)),
			ZTile("5R", 180, getQuadrant(3, 0)),
			ZTile("4R", 90, getQuadrant(3, 3)),
			ZTile("2R", 90, getQuadrant(3, 6))
		)

	override fun loadBoard() = load(arrayOf(
arrayOf("z0:i:ds", "z0:i:ws:ode", "z1:i:red:ws:ode",            "z2:i:ws", "z2:i:ws:de", "z3:spn",                      "z4:i:dw", "z4:i:lvd2", "z4:i"),
arrayOf("z5:spw", "z6", "z7",                                   "z8", "z9", "z10",                                      "z4:i:ww:ws", "z4:i:abom:ws", "z4:i:ws"),
arrayOf("z11:i:dn:ods:lgvd1:we", "z12", "z13:i:wn:ww:ods:ode",  "z14:i:wn:ws", "z14:i:wn:we:ws", "z15",                 "z16", "z17", "z18:st"),
arrayOf("z19:i:ww:ws:we", "z44", "z45:i:ww:ws:ode",             "z20:i:red:ws", "z20:i:ws:ode", "z21:i:dn:ws:ode",      "z22:i:dn:ws:ode", "z23:i:wn:ws:we", "z24"),
arrayOf("z25", "z26", "z27",                                    "z28", "z29", "z30",                                    "z31", "z32", "z33"),
arrayOf("z34:i:wn:ws:ode", "z35:i:dn:ws", "z35:i:wn:ws:ode",    "z36:i:wn:ws:red:ode", "z37:i:wn:ws", "z37:i:wn:ws:de", "z38:sps:ws", "z39:i:ww:wn:ws:red:ode", "z40:i:dn:ws"),
arrayOf("", "", "",                                             "", "", "z41:v:ww:gvd1",                                "z41:v:we", "z42:v:vd2", "z42:v")))

	override fun loadCmd(grid: Grid<ZCell>, pos: Grid.Pos, cmd: String) {
		when (cmd) {
			"lvd2" -> {
				violetVaultZone = grid[pos].zoneIndex
				//                violetVaultDoor = new ZDoor(pos, ZDir.DESCEND, GColor.MAGENTA);
				super.loadCmd(grid, pos, "vd2")
			}
			"lgvd1" -> {
				goldVaultZone = grid[pos].zoneIndex
				//                goldVaultDoor = new ZDoor(pos, ZDir.DESCEND, GColor.GOLD);
				super.loadCmd(grid, pos, "gvd1")
			}
			else    -> super.loadCmd(grid, pos, cmd)
		}
	}

	override fun init(game: ZGame) {
		require(redObjectives.size > 1)
		violetVaultDoor = game.board.findVault(2)
		goldVaultDoor = game.board.findVault(1)
		while (blueObjZone == greenObjZone) {
			blueObjZone = redObjectives.random()
			greenObjZone = redObjectives.random()
		}
		game.lockDoor(goldVaultDoor)
		game.lockDoor(violetVaultDoor)
	}

	override fun getPercentComplete(game: ZGame): Int {
		return if (game.getNumKills(ZZombieType.Abomination) > 0) 100 else 0
	}

	override fun processObjective(game: ZGame, c: ZCharacter) {
		super.processObjective(game, c)
		if (c.occupiedZone == greenObjZone) {
			game.addLogMessage(c.name() + " has found the GREEN objective and opened the GOLD vault")
			game.unlockDoor(goldVaultDoor)
			greenObjZone = -1
		} else if (c.occupiedZone == blueObjZone) {
			game.addLogMessage(c.name() + " has found the BLUE objective and opened the VIOLET vault")
			game.unlockDoor(violetVaultDoor)
			blueObjZone = -1
		}
	}

	public override fun getInitVaultItems(vaultZone: Int): List<ZEquipment<*>> {
		return when (vaultZone) {
			goldVaultZone   -> listOf(ZItemType.TORCH.create(), ZItemType.DRAGON_BILE.create())
			violetVaultZone -> vaultItemsRemaining.toMutableList().also {
				it.shuffle()
				while (it.size > 2)
					it.removeFirst()
			}
			else            -> super.getInitVaultItems(vaultZone)
		}
	}

	override fun getObjectivesOverlay(game: ZGame): Table {
		return Table(name)
			.addRow(Table().setNoBorder()
				.addRow("1.", "Collect all objectives.", String.format("%d of %d", numFoundObjectives, numStartObjectives))
				.addRow("2.", "Unlock GOLD vault. Key hidden among RED objectives.", greenObjZone == -1)
				.addRow("3.", "Unlock VIOLET vault. Key hidden among RED objectives.", blueObjZone == -1)
				.addRow("4.", "Kill the Abomination.", game.getNumKills(ZZombieType.Abomination) > 0)
			)
	}
}