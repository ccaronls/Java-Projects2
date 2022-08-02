package cc.game.kaiser.core

import cc.game.kaiser.ai.PlayerBot
import cc.lib.game.Utils
import junit.framework.TestCase
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

class KaiserTest : TestCase() {
	public override fun setUp() {
		Kaiser.DEBUG_ENABLED = true
		println("Start Test: $name")
	}

	@Throws(Exception::class)
	fun testSerializeTeam() {
		val team = Team()
		team.bid = NO_BID
		team.name = "scrubs"
		team.roundPoints = 10000
		team.totalPoints = 98320982

		//PrintWriter out = new PrintWriter(System.out);
		val out = StringWriter()
		team.serialize(PrintWriter(out))
		println(out.toString())
		val t2 = Team()
		t2.deserialize(out.toString())
		assertEquals(t2.totalPoints, team.totalPoints)
		assertEquals(t2.roundPoints, team.roundPoints)
		assertEquals(t2.name, team.name)
		assertEquals(t2, team)
	}

	@Throws(Exception::class)
	fun testKaiser() {
		var saved = false
		val k = Kaiser()
		k.setPlayer(0, PlayerBot("Joe Blow"))
		k.setPlayer(1, PlayerBot("Pat Robertson"))
		k.setPlayer(2, PlayerBot("Bobbing for Apples"))
		k.setPlayer(3, PlayerBot("Sam crow"))
		while (k.state !== State.GAME_OVER) {
			if (k.state === State.NEW_ROUND && !saved && Math.random() * 10 < 2) {
				k.saveToFile(File("resources/kaisertest.txt"))
				saved = true
			}
			k.runGame()
		}
	}

	@Throws(Exception::class)
	fun testRestoreGame() {
		Kaiser.DEBUG_ENABLED = true
		Utils.setDebugEnabled()
		var k = Kaiser()
		k.setPlayer(0, PlayerBot("Joe Blow"))
		k.setPlayer(1, PlayerBot("Pat Robertson"))
		k.setPlayer(2, PlayerBot("Bobbing for Apples"))
		k.setPlayer(3, PlayerBot("Sam crow"))
		while (k.state !== State.GAME_OVER) {
			k.saveToFile(File("resources/test.txt"))
			k = Kaiser()
			k.loadFromFile(File("resources/test.txt"))
			k.runGame()
			println("State after run: " + k.state)
		}
	}

	@Throws(Exception::class)
	fun testRestoreGame2() {
		val k = Kaiser()
		k.loadFromFile(File("resources/savegame.txt"))
		val p = k.getPlayer(0)
		assertEquals("Chris", p.name)
		assertEquals(7, p.numCards)
		assertEquals(1, k.getTeam(1).roundPoints)
		assertEquals(10, k.getTeam(1).roundPoints)
		var cnt = 1000
		while (cnt-- > 0 && k.state !== State.GAME_OVER) {
			k.runGame()
		}
		assert(k.state === State.GAME_OVER)
	}

	@Throws(Exception::class)
	fun testSerialize() {
		val k = Kaiser()
		k.loadFromFile(File("resources/kaisertest.txt"))
		while (k.state !== State.GAME_OVER) {
			k.runGame()
		}
	}

	fun testGetTrickWinnerIndex() {
		val aceClubs = Card(Rank.ACE, Suit.CLUBS)
		val kngDmnds = Card(Rank.KING, Suit.DIAMONDS)
		val qunHrts = Card(Rank.QUEEN, Suit.HEARTS)
		val jckSpds = Card(Rank.JACK, Suit.SPADES)
		val jckHrts = Card(Rank.JACK, Suit.HEARTS)
		val jckClubs = Card(Rank.JACK, Suit.CLUBS)
		val jckDmnds = Card(Rank.JACK, Suit.DIAMONDS)
		val qunDmnds = Card(Rank.QUEEN, Suit.DIAMONDS)
		val tenDmnds = Card(Rank.TEN, Suit.DIAMONDS)

		// test when all the same, no trump
		doTestTrick(arrayOf(jckSpds, jckHrts, jckClubs, jckDmnds), Suit.NOTRUMP, 0, jckSpds)
		doTestTrick(arrayOf(jckSpds, jckHrts, jckClubs, jckDmnds), Suit.NOTRUMP, 1, jckHrts)
		doTestTrick(arrayOf(jckSpds, jckHrts, jckClubs, jckDmnds), Suit.NOTRUMP, 2, jckClubs)
		doTestTrick(arrayOf(jckSpds, jckHrts, jckClubs, jckDmnds), Suit.NOTRUMP, 3, jckDmnds)

		// test when all the same, has trump
		for (i in 0..3) {
			doTestTrick(arrayOf(jckSpds, jckHrts, jckClubs, jckDmnds), Suit.SPADES, i, jckSpds)
			doTestTrick(arrayOf(jckSpds, jckHrts, jckClubs, jckDmnds), Suit.HEARTS, i, jckHrts)
			doTestTrick(arrayOf(jckSpds, jckHrts, jckClubs, jckDmnds), Suit.CLUBS, i, jckClubs)
			doTestTrick(arrayOf(jckSpds, jckHrts, jckClubs, jckDmnds), Suit.DIAMONDS, i, jckDmnds)
		}

		// test when all different, no trump
		doTestTrick(arrayOf(aceClubs, kngDmnds, qunHrts, jckSpds), Suit.NOTRUMP, 0, aceClubs)
		doTestTrick(arrayOf(aceClubs, kngDmnds, qunHrts, jckSpds), Suit.NOTRUMP, 1, kngDmnds)
		doTestTrick(arrayOf(aceClubs, kngDmnds, qunHrts, jckSpds), Suit.NOTRUMP, 2, qunHrts)
		doTestTrick(arrayOf(aceClubs, kngDmnds, qunHrts, jckSpds), Suit.NOTRUMP, 3, jckSpds)
		doTestTrick(arrayOf(aceClubs, qunHrts, jckSpds, jckHrts), Suit.NOTRUMP, 0, aceClubs)
		doTestTrick(arrayOf(aceClubs, qunHrts, jckSpds, jckHrts), Suit.NOTRUMP, 1, qunHrts)
		doTestTrick(arrayOf(aceClubs, qunHrts, jckSpds, jckHrts), Suit.NOTRUMP, 2, jckSpds)
		doTestTrick(arrayOf(aceClubs, qunHrts, jckSpds, jckHrts), Suit.NOTRUMP, 3, qunHrts)

		// test when all different, has trump
		val suits = arrayOf(Suit.CLUBS, Suit.DIAMONDS, Suit.HEARTS, Suit.SPADES)
		val cards = arrayOf(aceClubs, kngDmnds, qunHrts, jckSpds)
		var index = 0
		for (trump in suits) {
			doTestTrick(cards, trump, 0, cards[index])
			doTestTrick(cards, trump, 1, cards[index])
			doTestTrick(cards, trump, 2, cards[index])
			doTestTrick(cards, trump, 3, cards[index])
			index++
		}

		// test all the same suit, no trump
		for (i in 0..3) doTestTrick(arrayOf(kngDmnds, qunDmnds, jckDmnds, tenDmnds), Suit.NOTRUMP, i, kngDmnds)
		for (i in 0..3) doTestTrick(arrayOf(kngDmnds, qunDmnds, jckDmnds, tenDmnds), Suit.DIAMONDS, i, kngDmnds)
		for (i in 0..3) doTestTrick(arrayOf(kngDmnds, qunDmnds, jckDmnds, tenDmnds), Suit.SPADES, i, kngDmnds)
	}

	fun doTestTrick(trick: Array<Card>, trump: Suit?, startIndex: Int, expected: Card?) {
//		val index: Int = Kaiser.trgetrickWinnerIndex
//		assertEquals(expected, trick[index])
	}

	/*
    public void testParseNextLineElement() {
        String line = "PLAYER:0 cc.game.kaiser.swing.SwingPlayerUser \"Chris\"";

        assertEquals("PLAYER", Kaiser.parseNextLineElement(line, 0));
        assertEquals("0", Kaiser.parseNextLineElement(line, 1));
        assertEquals("cc.game.kaiser.swing.SwingPlayerUser", Kaiser.parseNextLineElement(line, 2));
        assertEquals("Chris", Kaiser.parseNextLineElement(line, 3));

    }
   */
	fun test_getBestTrumpOptions() {
		val k = Kaiser()
		for (i in 0..3) {
			val hand = listOf(k.getCard(Rank.ACE, Suit.values()[i]))
			val suits = Kaiser.getBestTrumpOptions(hand)
			println(Arrays.asList(*suits))
			assertEquals(Suit.values()[i], suits[0])
		}
		run {
			val hand = listOf(
				Card(Rank.EIGHT, Suit.DIAMONDS),
				Card(Rank.NINE, Suit.DIAMONDS),
				Card(Rank.TEN, Suit.DIAMONDS),
				Card(Rank.EIGHT, Suit.HEARTS),
				Card(Rank.EIGHT, Suit.HEARTS),
				Card(Rank.EIGHT, Suit.CLUBS)
			)
			val suits = Kaiser.getBestTrumpOptions(hand)
			assertEquals(Suit.DIAMONDS, suits[0])
			assertEquals(Suit.HEARTS, suits[1])
			assertEquals(Suit.CLUBS, suits[2])
		}
	}

	fun test_computeBidOptions() {
		val hand = listOf(
			Card(Rank.EIGHT, Suit.DIAMONDS),
			Card(Rank.NINE, Suit.DIAMONDS),
			Card(Rank.TEN, Suit.CLUBS),
			Card(Rank.EIGHT, Suit.HEARTS),
			Card(Rank.ACE, Suit.CLUBS),
			Card(Rank.QUEEN, Suit.DIAMONDS),
			Card(Rank.TEN, Suit.SPADES),
			Card(Rank.FIVE, Suit.HEARTS))
		var options: Array<Bid>
		//Bid [] options = Kaiser.computeBidOptions(null, false, hand);
		//System.out.println(Arrays.asList(options));
		//options = Kaiser.computeBidOptions(null, true, hand);
		//System.out.println(Arrays.asList(options));
		options = Kaiser.computeBidOptions(Bid(10, Suit.HEARTS), false, hand)
		println(Arrays.asList(*options))
		options = Kaiser.computeBidOptions(Bid(10, Suit.HEARTS), true, hand)
		println(Arrays.asList(*options))
		options = Kaiser.computeBidOptions(Bid(10, Suit.NOTRUMP), false, hand)
		println(Arrays.asList(*options))
		options = Kaiser.computeBidOptions(Bid(10, Suit.NOTRUMP), true, hand)
		println(Arrays.asList(*options))
	}
}