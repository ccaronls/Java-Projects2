package cc.lib.zombicide

import cc.lib.game.GDimension
import cc.lib.game.Utils
import cc.lib.reflector.RPrintWriter
import cc.lib.utils.Grid
import cc.lib.zombicide.ZGame.Companion.initDice
import cc.lib.zombicide.ZGame.MarksmanComparator
import cc.lib.zombicide.ZGame.RangedComparator
import cc.lib.zombicide.ZSkillLevel.Companion.getLevel
import cc.lib.zombicide.ZSpawnCard.Companion.drawSpawnCard
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.util.*

class ZombicideTest : TestCase() {
	@Throws(Exception::class)
	override fun setUp() {
		super.setUp()
		Utils.setDebugEnabled()
		for (z in ZZombieType.values()) {
			z.imageDims = arrayOf(
				GDimension.EMPTY
			)
			z.imageOptions = intArrayOf(0)
			z.imageOutlineOptions = intArrayOf(0)
		}
		for (i in ZIcon.values()) {
			i.imageIds = intArrayOf(0)
		}
	}

	fun testLoadBoards() {
		val game = ZGame()
		ZQuests.values().forEach { quest ->
			println(">>>>>>>>>>>>>>>>> Loading quest: $quest")
			game.loadQuest(quest)
			game.board.zones.forEach {
				assertTrue("Zone ${it.zoneIndex} of ${quest.name} has no cells", it.cells.isNotEmpty())
			}
		}
	}

	fun testRunGame() = runBlocking {
		val game = ZGame()
		game.addUser(ZTestUser())
		for (q in ZQuests.values()) {
			println("Testing Quest: $q")
			game.clearCharacters()
			game.loadQuest(q)
			for (pl in Utils.toList(ZPlayerName.Ariane, ZPlayerName.Clovis, ZPlayerName.Ann, ZPlayerName.Nelly)) game.addCharacter(pl!!)
			for (i in 0..999) {
				game.runGame()
				if (game.isGameOver) break
			}
		}
	}
/*
	@Throws(Exception::class)
	fun testRunGameWithGameDiffs() = runBlocking {

		/*
AVG Diff Size:              26011
AVG Compressed Diff Size:   13349
Total Diffs Size:           524722374
Total Compressed Size:      269292144
Compression Ratio:          1.9485246253600328
Total Diffs:                20173
Total Time:                 214900
Total Diff Time:            9346
Total Compression Time:     28952
Total Decompression Time:   58186

AVG Diff Size:              23955
AVG Compressed Diff Size:   11718
Total Diffs Size:           483249741
Total Compressed Size:      236402304
Compression Ratio:          2.0441837191231436
Total Diffs:                20173
Total Time:                 227535
Total Diff Time:            14509
Total Compression Time:     25639
Total Decompression Time:   49343

         */
		val COMPUTE_COUNTS = false
		val game: ZGame = HeadlessUIZombicide()
		game.addUser(ZTestUser())
		var totalDiffSizeBytes: Long = 0
		var totalCompressedDiffSizeBytes: Long = 0
		var totalDiffTimeMS: Long = 0
		var numDiffs: Long = 0
		var totalCompressionTimeMS: Long = 0
		var totalDecompressionTimeMS: Long = 0
		LoggerFactory.logLevel = LoggerFactory.LogLevel.SILENT
		//      int [] counts = {0,0,0,0,0,0,0,0,0,0,38723605,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,760928204,0,0,0,0,0,0,0,0,0,0,0,0,790973,24285073,0,7271806,4359237,1990584,1289033,1136347,1923453,1186936,605577,367121,868366,0,0,0,21309706,0,0,0,4868446,427663,2191494,426863,2368816,2411107,2059161,642876,1755727,11887,15305,6986105,460653,4912646,1835319,2319581,789813,2234146,3000699,2799966,254418,76858,3727594,0,5043,6467491,10,0,10,0,1413578,0,21068250,11527323,29288901,13948580,28916636,2989897,4918591,2169351,28742005,994152,1879472,38655539,8333837,19662857,19340855,6774298,4,12965080,12203750,11903970,15073054,3283530,4324690,2753174,3794159,5000722,7010098,0,7010098};
//      int [] counts = {0,0,0,0,0,0,0,0,0,0,38723605,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,760928204,1,1,1,1,1,1,1,1,1,1,1,1,790974,24285074,1,7271807,4359238,1990585,1289034,1136348,1923454,1186937,605578,367122,868367,1,1,1,21309707,1,1,1,4868447,427664,2191495,426864,2368817,2411108,2059162,642877,1755728,11888,15306,6986106,460654,4912647,1835320,2319582,789814,2234147,3000700,2799967,254419,76859,3727595,1,5044,6467492,11,1,11,1,1413579,1,21068251,11527324,29288902,13948581,28916637,2989898,4918592,2169352,28742006,994153,1879473,38655540,8333838,19662858,19340856,6774299,5,12965081,12203751,11903971,15073055,3283531,4324691,2753175,3794160,5000723,7010099,1,7010099,1};
//      int [] counts = {0,0,0,0,0,0,0,0,0,0,16083164,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,328200891,1,1,1,1,1,1,1,1,1,1,1,1,669045,9848700,1,1793781,1903988,898832,775395,703534,1552254,333574,363928,220394,381307,1,1,1,10373126,1,1,1,305679,73805,982953,8735,772464,385777,1635087,268748,928866,1115,14,1142828,59237,2669962,371534,1177625,667919,1501306,112755,2070630,214202,22,611035,1,5044,3227246,250095,1,250095,1,53870,1,6760076,4482197,13858764,6877520,13686098,1428841,2031316,1351126,12099834,42624,412846,10835467,3262179,5908641,9892008,3596512,5,6180675,4122276,6110429,6051791,51782,1338443,1333759,2145185,1301561,2874843,1,2874843,1};
//      int [] counts = {0,0,0,0,0,0,0,0,0,0,16081120,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,328187666,1,1,1,1,1,1,1,1,1,1,1,1,669045,9845694,1,1793774,1903980,898827,775391,703533,1552253,333574,363928,220394,381307,1,1,1,10373079,1,1,1,305679,73805,982934,8735,772464,385777,1635087,268748,928866,1115,14,1142821,59237,2669962,371534,1177625,667919,1501306,112755,2070630,214202,1,611035,1,5044,3225270,250095,1,250095,1,53870,1,6760034,4480221,13855779,6876532,13684021,1428841,2031316,1351126,12096842,42603,412846,10834413,3261191,5907606,9889983,3596512,5,6180654,4122248,6110368,6051770,51754,1338443,1333740,2145185,1300552,2873834,1,2873834,1};
		val counts = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16081334, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 328189620, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 669045, 1368451, 1, 1793796, 1903991, 898845, 775397, 703550, 1552274, 333576, 363929, 220400, 381312, 1, 1, 1, 10373126, 1, 1, 1, 305679, 73805, 982953, 8735, 772464, 385777, 946987, 268748, 928866, 1115, 14, 1142828, 59258, 2669962, 371534, 1177625, 667919, 1501306, 112755, 2070630, 214202, 22, 611035, 1, 5044, 3205150, 250095, 1, 250095, 1, 53870, 1, 5984270, 693295, 7500874, 4989636, 11774639, 1428841, 1360928, 1351126, 5701802, 1, 412846, 7545296, 1351642, 5907679, 8671090, 3596512, 3, 5512768, 3443670, 5389202, 5330564, 9159, 1338443, 1333759, 2145185, 81605, 2873928, 1, 2873928, 1)
		val enc = if (COMPUTE_COUNTS) HuffmanEncoding() else HuffmanEncoding(counts)
		if (COMPUTE_COUNTS) enc.initStandardCounts()
		val startTime = System.currentTimeMillis()
		val out = ByteArrayOutputStream()
		val eout = EncryptionOutputStream(out, enc)
		val dout = DataOutputStream(eout)
		for (q in ZQuests.values()) {
			println("Testing Quest: $q")
			game.clearCharacters()
			game.loadQuest(q)
			for (pl in Utils.toList(ZPlayerName.Ariane, ZPlayerName.Clovis, ZPlayerName.Ann, ZPlayerName.Nelly)) game.addCharacter(pl!!)
			val prev = game.deepCopy()
			assertEquals(game, prev)
			for (i in 0..999) {
				game.runGame()
				var t = System.currentTimeMillis()
				val diff = prev.diff(game)
				var dt = System.currentTimeMillis() - t
				totalDiffSizeBytes += diff.length.toLong()
				totalDiffTimeMS += dt
				numDiffs++
				if (COMPUTE_COUNTS) {
					enc.importCounts(diff)
				} else {
					t = System.currentTimeMillis()
					dout.writeBytes(diff)
					dout.flush()
					dt = System.currentTimeMillis() - t
					totalCompressedDiffSizeBytes += out.size().toLong()
					totalCompressionTimeMS += dt
					val reader = ByteArrayInputStream(out.toByteArray())
					val ein = EncryptionInputStream(reader, enc)
					val din = DataInputStream(ein)
					val data = ByteArray(din.available())
					t = System.currentTimeMillis()
					din.readFully(data)
					dt = System.currentTimeMillis() - t
					totalDecompressionTimeMS += dt
					val diff2 = String(data)
					out.reset()
					assertEquals(diff, diff2)
				}
				prev.merge(diff)
				//System.out.println("diff="+diff);
				assertEquals(game.toString(), prev.toString())
				assertEquals(game.checksum, prev.checksum)
				if (game.isGameOver) break
			}
		}
		val totalTimeMS = System.currentTimeMillis() - startTime
		if (COMPUTE_COUNTS) enc.printEncodingAsCode(System.out)
		println("AVG Diff Size:              " + totalDiffSizeBytes / numDiffs)
		println("AVG Compressed Diff Size:   " + totalCompressedDiffSizeBytes / numDiffs)
		println("Total Diffs Size:           $totalDiffSizeBytes")
		println("Total Compressed Size:      $totalCompressedDiffSizeBytes")
		println("Compression Ratio:          " + totalDiffSizeBytes.toDouble() / totalCompressedDiffSizeBytes)
		println("Total Diffs:                $numDiffs")
		println("Total Time:                 $totalTimeMS")
		println("Total Diff Time:            $totalDiffTimeMS")
		println("Total Compression Time:     $totalCompressionTimeMS")
		println("Total Decompression Time:   $totalDecompressionTimeMS")
	}

	@Throws(Exception::class)
	fun testQuests() {
		val game: UIZombicide = HeadlessUIZombicide()
		val g: APGraphics = TestGraphics()
		for (q in ZQuests.values()) {
			println("Testing Quest: $q")
			game.clearCharacters()
			game.loadQuest(q)
			val b = game.board
			for (zone in b.zones) {
				for (door in zone.doors) {
					var pos = door.cellPosStart
					var cell = b.getCell(pos)
					assertEquals(cell.zoneIndex, zone.zoneIndex)
					val otherSide = door.otherSide
					if (otherSide != null) {
						pos = otherSide.cellPosStart
						assertEquals(pos, door.cellPosEnd)
						assertEquals(otherSide.cellPosEnd, door.cellPosStart)
						cell = b.getCell(pos)
					}
				}
			}
			assertEquals(q, game.quest.quest)
			game.quest.getPercentComplete(game)
			game.quest.tiles
			for (ic in ZIcon.values()) {
				ic.imageIds = IntArray(8)
			}
			for (z in ZZombieType.values()) {
				z.imageOptions = IntArray(1)
			}
			game.boardRenderer.draw(g, 500, 300)
			for (zone in game.board.zones) {
				Assert.assertTrue("Zone: $zone is invalid", zone.zoneIndex >= 0)
			}
		}
	}*/

	@Throws(Exception::class)
	fun testEvilTwins() {
		val game = ZGame()
		game.loadQuest(ZQuests.The_Evil_Twins)
		val gvd1 = game.board.findDoor(Grid.Pos(0, 0), ZDir.DESCEND)
		assertNotNull(gvd1)
		val other = gvd1.getOtherSide(game.board)
		assertNotNull(other)
		assertEquals(Grid.Pos(9, 6), other.cellPosStart)
	}

	fun testLevels() {
		val rules = ZRules()
		rules.ultraRed = false
		for (i in 0 until ZColor.YELLOW.dangerPts)
			assertTrue(ZSkillLevel.getLevel(i, rules).color == ZColor.BLUE)
		for (i in ZColor.YELLOW.dangerPts until ZColor.ORANGE.dangerPts)
			assertTrue(ZSkillLevel.getLevel(i, rules).color == ZColor.YELLOW)
		for (i in ZColor.ORANGE.dangerPts until ZColor.RED.dangerPts)
			assertTrue(ZSkillLevel.getLevel(i, rules).color == ZColor.ORANGE)
		for (i in ZColor.RED.dangerPts until 1000)
			assertTrue(ZSkillLevel.getLevel(i, rules).color == ZColor.RED)

		rules.ultraRed = true
		val ultraRed = ZSkillLevel.getLevel(1000, rules)
		assertTrue(ultraRed.isUltra)

		val red = ZSkillLevel.getLevel(ZColor.RED.dangerPts, rules)
		val next = red.nextLevel(rules)
		val pts = next.pts - red.pts
		assertEquals(pts, red.getPtsToNextLevel(ZColor.RED.dangerPts, rules))

		for (i in 0..99) {
			println(
				"For exp $i level is ${getLevel(i, rules)} and next level in ${
					getLevel(
						i,
						rules
					).getPtsToNextLevel(i, rules)
				}"
			)
		}
	}

	fun testUltraRed() = runBlocking {
		val rules = ZRules()
		rules.ultraRed = false
		val game = ZGame()
		game.setUsers(ZTestUser())
		game.clearCharacters()
		game.loadQuest(ZQuests.The_Abomination)
		val ann = game.addCharacter(ZPlayerName.Ann)
		rules.ultraRed = true
		val state = game.state
		for (i in 0..999) {
			game.addExperience(ann, 1)
			while (game.state !== state) {
				game.runGame()
			}
		}
		for (i in 0 until ZSkillLevel.NUM_LEVELS) {
			assertEquals(0, ann.getRemainingSkillsForLevel(i).size)
		}
	}

	fun testUltraLevelStr() {
		val rules = ZRules()
		rules.ultraRed = true
		var i = 0
		while (i < 150) {
			println("Exp=" + i + "    -> " + getLevel(i, rules))
			i += 3
		}
	}

	fun testZombieSorting() {
		val zombies: MutableList<ZZombie> = ArrayList()
		for (i in 0..19) {
			val type = Utils.randItem(ZZombieType.values())
			zombies.add(ZZombie(type, 0))
		}
		Utils.shuffle(zombies)
		println("----------------------------------------------------")
		for (z in zombies) {
			println(
				String.format(
					"%-20s hits:%d  priority:%d",
					z.type,
					z.type.minDamageToDestroy,
					z.type.targetingPriority
				)
			)
		}
		println("----------------------------------------------------")
		for (i in 1..3) {
			val meleeList: List<ZZombie> = ArrayList(zombies)
			Collections.sort(meleeList, MarksmanComparator(1))
			println("MELEE SORTING $i")
			println("----------------------------------------------------")
			for (z in meleeList) {
				println(
					String.format(
						"%-20s hits:%d  priority:%d",
						z.type,
						z.type.minDamageToDestroy,
						z.type.targetingPriority
					)
				)
			}
		}
		println("----------------------------------------------------")
		for (i in 1..3) {
			val rangedList: List<ZZombie> = ArrayList(zombies)
			Collections.sort(rangedList, RangedComparator())
			println("RANGED SORTING $i")
			println("----------------------------------------------------")
			for (z in rangedList) {
				println(
					String.format(
						"%-20s hits:%d  priority:%d",
						z.type,
						z.type.minDamageToDestroy,
						z.type.targetingPriority
					)
				)
			}
		}
		for (i in 1..3) {
			val marksmanList: List<ZZombie> = ArrayList(zombies)
			Collections.sort(marksmanList, MarksmanComparator(1))
			println("MARKSMAN SORTING $i")
			println("----------------------------------------------------")
			for (z in marksmanList) {
				println(
					String.format(
						"%-20s hits:%d  priority:%d",
						z.type,
						z.type.minDamageToDestroy,
						z.type.targetingPriority
					)
				)
			}
		}
	}

	fun testInitDice() {
		for (d in ZDifficulty.values()) {
			val dice = initDice(d)
			for (n in dice) {
				assertTrue(n > 0 && n <= 6)
			}
		}
	}

	fun testWolfsberg() {
		val game = ZGame()
		game.setUsers(ZTestUser(ZPlayerName.Ann))
		game.loadQuest(ZQuests.Welcome_to_Wulfsberg)
		assertTrue(game.quest.quest.isWolfBurg)
		drawSpawnCard(game.quest.quest.isWolfBurg, true, ZDifficulty.HARD)
	}

	fun testUltraExp() {
		val rules = ZRules()
		rules.ultraRed = true
		var skill = ZSkillLevel()
		var pts = 0
		for (i in 0..9) {
			val nextLvl = skill.getPtsToNextLevel(pts, rules)
			assertTrue(nextLvl > 0)
			println("Cur level=$skill")
			println("Next Level=$nextLvl")
			pts += nextLvl
			skill = skill.nextLevel(rules)
		}
	}

	fun testWeaponComparision() = runBlocking {
		val game = ZGame()
		game.rules.ultraRed = true
		val c0 = ZPlayerName.Ann.create()
		c0.addExperience(game, ZColor.RED.dangerPts)
		c0.addAvailableSkill(ZSkill.Plus1_Damage_Melee)
		c0.addAvailableSkill(ZSkill.Plus1_die_Ranged)
		c0.addAvailableSkill(ZSkill.Plus1_max_Range)
		val c1 = ZPlayerName.Morrigan.create()
		c1.addAvailableSkill(ZSkill.Plus1_die_Melee)
		c1.addAvailableSkill(ZSkill.Zombie_link)
		val weapon = ZWeaponType.ORCISH_CROSSBOW.create()

		println(weapon.getComparisonInfo(game, c0, c1))

		println()

		println(weapon.getCardInfo(c0, game))
	}

	fun testBoardMerge() {

		val quest = ZQuests.Tutorial.load()
		val board = quest.loadBoard()
		val board2 = board.deepCopy()


		val player = ZPlayerName.Baldric.create()
		player.occupiedZone = board.getCell(0, 0).zoneIndex
		assertTrue(board.spawnActor(player))
		val copy = board.deepCopy()
		val position = player.position
		assertTrue(position.zone == 0)
		assertTrue(copy.getActor(position).type == ZPlayerName.Baldric)
		assertFalse(copy.getActor(position) === player)

		player.addAnimation(object : ZActorAnimation(player, 1000) {

		}.start())

		println("player position before: " + player.position)

		board.moveActor(player, 1)
		assertTrue(player.position.zone == 1)

		println("player position after: " + player.position)

		assertNull(board.getActorOrNull(position))
		val out = ByteArrayOutputStream()
		board.serializeDirty(RPrintWriter(out))
		copy.merge(String(out.toByteArray()))
		assertNotNull(copy.getActor(player.position))
		assertEquals(copy.getActor(player.position).position, player.position)
		val player2 = copy.getActor(player.position)
		println("player2 position after: " + player2.position)
//		assertTrue(player2.animation == null)
//		assertTrue(player.animation != null)


		assertNull(board.getActorOrNull(position))

		//println(board2.diff(copy))
	}

	fun testUserCharacterSorting() {

		val userSize = 3

		val map = listOf(
			Pair(ZPlayerName.Ann.create().also {
				it.actionsLeftThisTurn = 3
			}, 0),
			Pair(ZPlayerName.Baldric.create().also {
				it.actionsLeftThisTurn = 0
			}, 0),
			Pair(ZPlayerName.Ariane.create().also {
				it.actionsLeftThisTurn = 3
			}, 1),
			Pair(ZPlayerName.Clovis.create().also {
				it.actionsLeftThisTurn = 2
			}, 1),
			Pair(ZPlayerName.Nelly.create().also {
				it.actionsLeftThisTurn = 0
			}, 2),
			Pair(ZPlayerName.Baldric.create().also {
				it.actionsLeftThisTurn = 0
			}, 2)
		).filter { it.first.actionsLeftThisTurn > 0 }.shuffled()

		var currentUser = 1

		val sorted = map.sortedBy {
			(it.second - currentUser + userSize) % userSize
		}

		println("sorted: " + sorted.joinToString("\n") { "${it.second} -> ${it.first.type}" })

	}
}