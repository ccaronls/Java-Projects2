package cc.lib.zombicide.quests

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.utils.Grid
import cc.lib.utils.Table
import cc.lib.zombicide.ZArmorType
import cc.lib.zombicide.ZBoard
import cc.lib.zombicide.ZCell
import cc.lib.zombicide.ZCellType
import cc.lib.zombicide.ZCharacter
import cc.lib.zombicide.ZColor
import cc.lib.zombicide.ZDir
import cc.lib.zombicide.ZDoor
import cc.lib.zombicide.ZEquipmentType
import cc.lib.zombicide.ZGame
import cc.lib.zombicide.ZIcon
import cc.lib.zombicide.ZQuest
import cc.lib.zombicide.ZQuests
import cc.lib.zombicide.ZSpawnArea
import cc.lib.zombicide.ZTile
import cc.lib.zombicide.ZWeaponType
import cc.lib.zombicide.ZZombieType
import cc.lib.zombicide.ZZone
import kotlin.math.roundToInt

@Suppress("SpellCheckingInspection")
class ZQuestThorsHammer : ZQuest(ZQuests.Thors_Hammer) {

	companion object {
		init {
			addAllFields(ZQuestThorsHammer::class.java)
		}
	}

	var greenKeyZone = -1
	var mjolnirZone = -1
	var greenDoors = mutableListOf<ZDoor>()
	var numStartSpawns = 0
	val abomZones = mutableMapOf<ZDir, Int>()

	override fun loadBoard() = load(arrayOf(
		arrayOf("z0:start",       "z2","z3",                      "z4","z5:spn","z6",                         "z7", "z8",                             "z10"),
		arrayOf("z22",            "z24:i:red:wn:ww","z24:i:wn",   "z25:i:wn:ww","z25:i:dn","z25:i:wn",        "z26:i:dw:wn","z26:i:wn:red:we",        "z28"),
		arrayOf("z29",            "z24:i:ww:ws","z24:i:ds:de",    "z25:i:ws","z25:i:greends","z25:i:ws",      "z26:i:ww:ds","z26:i:we:ws",            "z32"),
		arrayOf("z33",            "z36:i:ww","z36:i:we",          "z37","z38:abomds","z39",                   "z40:i:ww","z40:i:we",                  "z42"),
		arrayOf("z43:spw",        "z36:i:dw","z36:i:greende",     "z45:abomde","z46:i:mjolnir","z47:abomdw",  "z40:i:greendw","z40:i:de",             "z49:spe"),
		arrayOf("z50",            "z36:i:ww:ws","z36:i:ds:we",    "z52","z53:abomdn","z54",                   "z40:i:ww:ws","z40:i:ds:we",            "z56"),
		arrayOf("z57",            "z59:i:ww","z59:i:de",          "z60:i:wn","z60:i:greendn","z60:i:wn:de",   "z61:i:wn","z61:i:we",                  "z63"),
		arrayOf("z64",            "z59:i:red:ww:ws","z59:i:ws",   "z60:i:ww:ws","z60:i:ds","z60:i:ws:we",     "z61:i:ww:ws","z61:i:red:ws:we",        "z67"),
		arrayOf("z79",            "z81", "z82",                   "z83", "z84:sps", "z85",                    "z86", "z87",                           "z89:start"),
	))

	override fun init(game: ZGame) {
		greenDoors.forEach {
			game.board.setDoorLocked(it)
		}
		greenKeyZone = redObjectives.random()
		numStartSpawns = game.board.getSpawnZones().size
		require(numStartSpawns > 0)
	}

	override val allVaultOptions: List<ZEquipmentType>
		get() = listOf(ZWeaponType.INFERNO, ZWeaponType.ORCISH_CROSSBOW, ZWeaponType.DEFLECTING_DAGGER, ZWeaponType.FLAMING_GREAT_SWORD, ZWeaponType.AXE_OF_CARNAGE, ZArmorType.DWARVEN_SHIELD, ZArmorType.SHIELD_OF_AGES)

	override suspend fun processObjective(game: ZGame, c: ZCharacter) {
		if (c.occupiedZone == mjolnirZone) {
			game.addLogMessage("MJOLNIR ACQUIRED!!")
			game.giftEquipment(c, ZWeaponType.MJOLNIR.create())
			mjolnirZone = -1
		} else {
			super.processObjective(game, c)
			if (c.occupiedZone == greenKeyZone) {
				greenDoors.forEach {
					game.addLogMessage(c.name() + " has unlocked the Green Doors.")
					game.unlockDoor(it)
				}
				greenKeyZone = -1
			}
			game.giftRandomVaultArtifact(c)
		}
	}

	override suspend fun onDoorOpened(game: ZGame, door: ZDoor, c: ZCharacter) {
		greenDoors.firstOrNull { it == door }?.let {
			// when a character open a green door, spawn a abomination right in front of them!!
			abomZones[door.moveDirection]?.let {
				game.spawnZombies(1, ZZombieType.Abomination, it)
			}
			abomZones.clear()
		}
	}

	override fun drawBlackObjective(board: ZBoard, g: AGraphics, cell: ZCell, zone: ZZone) {
		g.drawImage(ZIcon.MJOLNIR.imageIds[ZDir.SOUTH.ordinal], GRectangle(cell).scaledBy(.5f))
	}

	override fun loadCmd(grid: Grid<ZCell>, pos: Grid.Pos, cmd: String) {
		val cell = grid[pos]
		when (cmd) {
			"greendn" -> greenDoors.add(ZDoor(pos, ZDir.NORTH, GColor.GREEN))
			"greends" -> greenDoors.add(ZDoor(pos, ZDir.SOUTH, GColor.GREEN))
			"greende" -> greenDoors.add(ZDoor(pos, ZDir.EAST, GColor.GREEN))
			"greendw" -> greenDoors.add(ZDoor(pos, ZDir.WEST, GColor.GREEN))
			"abomdn" -> abomZones[ZDir.NORTH] = cell.zoneIndex
			"abomds" -> abomZones[ZDir.SOUTH] = cell.zoneIndex
			"abomde" -> abomZones[ZDir.EAST] = cell.zoneIndex
			"abomdw" -> abomZones[ZDir.WEST] = cell.zoneIndex
			"mjolnir" -> {
				mjolnirZone = grid[pos].zoneIndex
				cell.setCellType(ZCellType.OBJECTIVE_BLACK, true)
				addObjective(ZCellType.OBJECTIVE_BLACK, cell.zoneIndex)
			}
			else -> super.loadCmd(grid, pos, cmd)
		}
	}

	override suspend fun canCharacterTakeObjective(game: ZGame, cur: ZCharacter, zone: Int): Boolean {
		if (zone == mjolnirZone) {
			if (cur.skillLevel.difficultyColor >= ZColor.RED) {
				game.addLogMessage("${cur.name()} IS WORTHY!")
				return true
			} else {
				game.addLogMessage("${cur.name()} is not worthy")
				return false
			}
		}
		return super.canCharacterTakeObjective(game, cur, zone)
	}

	override fun createSpawnAreas(pos: Grid.Pos, dir: ZDir): ZSpawnArea {
		return ZSpawnArea(cellPos = pos, icon = ZIcon.SPAWN_BLUE, dir = dir, isCanBeRemovedFromBoard = true, isCanSpawnNecromancers = false, isEscapableForNecromancers = false)
	}

	fun isMjolnirFound(game: ZGame) : Boolean =
		game.allCharacters.firstOrNull { it.isInPossession(ZWeaponType.MJOLNIR) } != null

	override fun getPercentComplete(game: ZGame): Int {
		val maxSkillPlayer = game.allLivingCharacters.maxByOrNull { it.skillLevel }
		val greenKeyFound = greenKeyZone < 0 // 15%
		val mjolnirFound = isMjolnirFound(game)
		val numSpawnsClosed = numStartSpawns - game.board.getSpawnZones().size // 0-20%
		val numZombies = if (game.getNumKills(ZZombieType.Walker) == 0) 100 else game.board.getAllZombies().filter { it.isAlive }.size // 5%
		val skill = maxSkillPlayer?.let {
			it.skillLevel.difficultyColor.ordinal
		}?:0

		return skill * 15 + // 0-45%
			(if (greenKeyFound) 15 else 0) + // 15%
			(if (mjolnirFound) 15 else 0) + // 15%
			(numSpawnsClosed.toFloat() * 20 / numStartSpawns).roundToInt() + // 0-20%
			(5 - numZombies.coerceIn(0..5)) // 5%
	}

	override val tiles : Array<ZTile>
		get ()= arrayOf(
			ZTile("6R", 0, ZTile.getQuadrant(3, 3)),
			ZTile("1V", 0, ZTile.getQuadrant(3, 0)),
			ZTile("1V", 90, ZTile.getQuadrant(0, 3)),
			ZTile("1V", 180, ZTile.getQuadrant(3, 6)),
			ZTile("1V", 270, ZTile.getQuadrant(6, 3)),
			ZTile("1C", 0, ZTile.getQuadrant(0, 0)),
			ZTile("1C", 90, ZTile.getQuadrant(0, 6)),
			ZTile("1C", 180, ZTile.getQuadrant(6, 6)),
			ZTile("1C", 270, ZTile.getQuadrant(6, 0)),
		)

	override fun getObjectivesOverlay(game: ZGame): Table {

		val maxSkill = game.allLivingCharacters.maxByOrNull { it.skillLevel }?.skillLevel?.difficultyColor
			?: ZColor.BLUE
		val greenKeyFound = greenKeyZone < 0 // 15%
		val mjolnirFound = isMjolnirFound(game)
		val numSpawns = game.board.getSpawnZones().size.coerceIn(0,4) // 0-20%
		val numZombies = game.board.getAllZombies().size // 5%

		return Table(name)
			.addRow(Table().setNoBorder()
				.addRow("1. Obtain RED Danger Level to wield Mjolnir", maxSkill, maxSkill == ZColor.RED)
				.addRow("2. Find the Green Key Hidden among the RED Objectives", "", greenKeyFound)
				.addRow("3. Obtain Mjolnir Locked behind the GREEN door", "", mjolnirFound)
				.addRow("4. Destroy all spawn zones using Mjolnir's special ability", "${numStartSpawns-numSpawns} of $numStartSpawns", numSpawns == 0)
				.addRow("5. Destroy all Zombies", "", numZombies == 0)
			)
	}

}