package cc.lib.zombicide.quests

import cc.lib.utils.Grid
import cc.lib.utils.Table
import cc.lib.zombicide.ZBoard
import cc.lib.zombicide.ZCell
import cc.lib.zombicide.ZCharacter
import cc.lib.zombicide.ZDifficulty
import cc.lib.zombicide.ZDir
import cc.lib.zombicide.ZGame
import cc.lib.zombicide.ZIcon
import cc.lib.zombicide.ZQuest
import cc.lib.zombicide.ZQuests
import cc.lib.zombicide.ZSkill
import cc.lib.zombicide.ZSpawnArea
import cc.lib.zombicide.ZTile
import cc.lib.zombicide.ZZombie
import cc.lib.zombicide.ZZombieType

/**
 * Created by Chris Caron on 9/1/21.
 */
class WolfTheZombieArmy : ZQuest(ZQuests.The_Zombie_Army) {
	companion object {
		const val MAX_SPAWN_ZONES = 6

		init {
			addAllFields(WolfTheZombieArmy::class.java)
		}
	}

	var blueObjZone = -1
	var greenObjZone = -1
	var startDeckSize = 0

	override fun loadBoard(): ZBoard {
		val map = arrayOf(
			arrayOf("z0", "z1", "z2:st",                            "z10:i:ww", "z10:i:xspn", "z10:i"),
			arrayOf("z3:xspw", "z4:i:wn:ws:we:ww", "z5",            "z10:i:dw:ws", "z10:i:ws", "z10:i:ws"),
			arrayOf("z6", "z7", "z8",                               "z11", "z12", "z13"),
			arrayOf("z20:i:odn:ws:xspw", "z20:i:wn:we:ws", "z21",   "z30:t1:rn", "z31:t2:rn:re", "z32:t3:rn:red"),
			arrayOf("z22", "z23", "z24",                            "z33:t3:rw:rn:red", "z34:t3:red", "z35:t3:red"),
			arrayOf("z25:i:wn:xspw", "z25:i:dn:we", "z26",          "z36:t3:rw:red", "z37:t3:rw:red:odn:odw:ode", "z38:t3:red"))
		return load(map)
	}

	override fun loadCmd(grid: Grid<ZCell>, pos: Grid.Pos, cmd: String) {
		when (cmd) {
			"xspn" -> setSpawnArea(grid[pos], ZSpawnArea(pos, ZIcon.SPAWN_BLUE, ZDir.NORTH, true, true, false))
			"xsps" -> setSpawnArea(grid[pos], ZSpawnArea(pos, ZIcon.SPAWN_BLUE, ZDir.SOUTH, true, true, false))
			"xspe" -> setSpawnArea(grid[pos], ZSpawnArea(pos, ZIcon.SPAWN_BLUE, ZDir.EAST, true, true, false))
			"xspw" -> setSpawnArea(grid[pos], ZSpawnArea(pos, ZIcon.SPAWN_BLUE, ZDir.WEST, true, true, false))
			else   -> super.loadCmd(grid, pos, cmd)
		}
	}

	override val tiles: Array<ZTile>
		get() = arrayOf(
			ZTile("6R", 0, ZTile.getQuadrant(0, 0)),
			ZTile("1V", 270, ZTile.getQuadrant(0, 3)),
			ZTile("9V", 180, ZTile.getQuadrant(3, 0)),
			ZTile("10R", 90, ZTile.getQuadrant(3, 3))
		)

	override fun init(game: ZGame) {
		require(redObjectives.size > 1)
		while (blueObjZone == greenObjZone) {
			blueObjZone = redObjectives.random()
			greenObjZone = redObjectives.random()
		}
		startDeckSize = when (game.getDifficulty()) {
			ZDifficulty.EASY -> 20
			ZDifficulty.MEDIUM -> 30
			ZDifficulty.HARD -> 40
		}
//		for (i in 0 until startDeckSize) spawnDeck.add(ZSpawnCard.drawSpawnCard(true, true, game.getDifficulty()))
	}

//	override fun drawSpawnCard(game: ZGame, targetZone: Int, dangerLevel: ZSkillLevel?): ZSpawnCard? {
//		return if (spawnDeck.isEmpty()) null else spawnDeck.removeAt(spawnDeck.size - 1)
//	}

	override fun getPercentComplete(game: ZGame): Int {
		val numZombies = game.board.getAllZombies().size
		val total = 4 * startDeckSize + numZombies
		val completed = 4 * (startDeckSize - game.spawnDeckSize)
		return completed * 100 / total
	}

	override fun processObjective(game: ZGame, c: ZCharacter) {
		super.processObjective(game, c)
		if (c.occupiedZone == greenObjZone) {
			greenObjZone = -1
			game.chooseVaultItem()
		} else if (c.occupiedZone == blueObjZone) {
			blueObjZone = -1
			game.chooseVaultItem()
		} else {
			game.chooseEquipmentFromSearchables()
			c.addAvailableSkill(ZSkill.Inventory)
		}
	}

	fun getNumSpawnZones(game: ZGame): Int {
		return game.board.getCells().sumBy { it.spawnAreas.size }
	}

	override fun getObjectivesOverlay(game: ZGame): Table {
		return Table(name)
			.addRow(
				Table().setNoBorder()
					.addRow(
						"1.",
						"Destroy the Zombie Army. All Spawn cards must be depleted and all zombies destroyed in order to complete the quest",
						String.format(
							"%d of %d cards left",
							startDeckSize - game.spawnDeckSize,
							startDeckSize
						)
					)
				.addRow("2.", "Find the GREEN objective hidden among the RED objetives. Choose a Vault item of your choice", greenObjZone < 0)
				.addRow("3.", "Find the BLUE objective hidden among the RED objetives. Choose a Vault item of your choice", blueObjZone < 0)
				.addRow("4.", "Taking a RED objectives allows survivor to take and item from deck and reorganize their inventory for free")
				.addRow("5.", "Spawn areas cannot be removed after killing a Necromancer, even those created by Necromancers. Max of $MAX_SPAWN_ZONES spawn areas on the board", String.format("%d of %d", getNumSpawnZones(game), MAX_SPAWN_ZONES))
			)
	}

	override fun onZombieSpawned(game: ZGame, zombie: ZZombie, zone: Int) {
		when (zombie.type) {
			ZZombieType.Necromancer -> {
				if (getNumSpawnZones(game) < MAX_SPAWN_ZONES) {
					game.board.setSpawnZone(zone, ZIcon.SPAWN_GREEN, false, false, false)
					game.spawnZombies(zone)
				}
			}
			else -> Unit
		}
	}
}