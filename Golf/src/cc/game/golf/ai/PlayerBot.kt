package cc.game.golf.ai

import cc.game.golf.core.Card
import cc.game.golf.core.DrawType
import cc.game.golf.core.Golf
import cc.game.golf.core.Player
import cc.game.golf.core.Rules
import cc.lib.utils.random
import java.util.LinkedList

/**
 * Simplistic AI robot.  Always chooses play that results in lowest points.
 * @author ccaron
 */
class PlayerBot : Player {
	constructor() : super()
	constructor(name: String?) : super(name ?: "")

	class Hand(var cards: Array<Array<Card?>>) {
		var replaced: Card? = null
		var pts = 0
		var ptsScore = 0.0
		var setPotentialScore = 0.0
		var faceDownScore = 0.0
		var totalScore = 0.0
		var label = ""
	}

	private val hands = LinkedList<Hand>()
	private fun debugWriteHands() {
		debugWriteHands(hands, numRows, numCols)
	}

	private fun generateHands(golf: Golf, drawCard: Card) {
		if (DEBUG_ENABLED) {
			println("""-----------------------------------------------------
Player $playerNum generating hands from card: ${drawCard.toPrettyString()}""")
		}
		hands.clear()
		val cols = numCols
		val numRows = numRows

		for (i in 0 until numRows * cols) {
			val hand: Array<Array<Card?>> = Array(numRows) {
				getRow(it).toTypedArray()
			}
			hands.add(Hand(hand))
		}
		for (i in hands.indices) {
			val row = i / cols
			val col = i % cols
			val hand = hands[i]
			hand.replaced = hand.cards[row][col]
			hand.cards[row][col] = drawCard
			computeHandScore(golf, hand)
		}
	}

	private val bestHand: Hand
		get() = hands.minBy { it.totalScore }.also {
			it.label += " (B)"
		}

	/**
	 * Return a double value that ranks the hand.
	 * Should be value between 0-1 where 0 is the worst and 1 is the best
	 * @param golf
	 * @param cards
	 * @return
	 */
	protected fun computeHandScore(golf: Golf, hand: Hand) {
		internalComputeHandScore(golf.rules, hand, random(1000).toDouble())
	}

	override suspend fun chooseDrawPile(golf: Golf): DrawType? {
		// see if we have an empty slot matching drawCard
		if (golf.topOfDiscardPile == null)
			return DrawType.DTStack
		generateHands(golf, golf.topOfDiscardPile!!)
		val hand = Hand(getCards())
		hand.label += "Current"
		computeHandScore(golf, hand)
		hands.addFirst(hand)
		val best = bestHand
		if (DEBUG_ENABLED) {
			debugWriteHands()
		}
		return if (best.replaced == null) DrawType.DTStack else DrawType.DTDiscardPile
	}

	override suspend fun chooseDiscardOrPlay(golf: Golf, drawCard: Card): Card? {
		generateHands(golf, drawCard)
		val hand = Hand(getCards())
		hand.label = "Current"
		hand.replaced = drawCard
		computeHandScore(golf, hand)
		hands.addFirst(hand)
		val best = bestHand
		if (DEBUG_ENABLED) {
			debugWriteHands()
		}
		return best.replaced
	}

	override suspend fun chooseCardToSwap(golf: Golf, drawCard: Card): Card? {
		assert(drawCard != null)
		if (hands.size == 0) {
			generateHands(golf, drawCard)
		}
		val swapped = bestHand.replaced
		if (swapped == null) {
			debugWriteHands()
		}
		assert(swapped != null)
		return swapped
	}

	val message: String
		/**
		 * for debugging load/save.  Just any ole single line statement (no newline)
		 * @return
		 */
		get() = "I am a robot"

	override suspend fun turnOverCard(golf: Golf, row: Int): Int {
		return random(golf.rules.gameType.cols)
	}

	companion object {
		var DEBUG_ENABLED = false
		private var maxPoints = 100
		private var maxSetPotential = 1.0
		fun debugWriteHands(hands: List<Hand>, rows: Int, cols: Int) {
			var hand: String? = ""
			val handSpacing = "  "
			for (i in 0 until rows) {
				for (iii in hands.indices) {
					for (ii in 0 until cols) {
						hand += "+--+"
					}
					hand += handSpacing
				}
				hand += "\n"
				for (iii in hands.indices) {
					for (ii in 0 until cols) {
						val c = hands[iii].cards[i][ii]
						hand += if (c?.isShowing == true) String.format("|%c%c|", c.rank.rankString[0], c.suit.suitChar) else "|  |"
					}
					hand += handSpacing
				}
				hand += "\n"
				for (iii in hands.indices) {
					for (ii in 0 until cols) {
						hand += "|  |"
					}
					hand += handSpacing
				}
				hand += "\n"
				for (iii in hands.indices) {
					for (ii in 0 until cols) {
						hand += "+--+"
					}
					hand += handSpacing
				}
				hand += "\n"
			}
			for (i in hands.indices) {
				hand += String.format("Pts   :%5d  ", hands[i].pts)
			}
			hand += "\n"
			for (i in hands.indices) {
				hand += String.format("Pscore:%1.3f  ", hands[i].ptsScore)
			}
			hand += "\n"
			for (i in hands.indices) {
				hand += String.format("Fscore:%1.3f  ", hands[i].faceDownScore)
			}
			hand += "\n"
			for (i in hands.indices) {
				hand += String.format("Sscore:%1.3f  ", hands[i].setPotentialScore)
			}
			hand += "\n"
			for (i in hands.indices) {
				hand += String.format("Total :%1.3f  ", hands[i].totalScore)
			}
			hand += "\n"
			for (i in hands.indices) {
				val c = hands[i].replaced
				hand += if (c != null) String.format("Replaced:%c%c   ", c.rank.rankString[0], c.suit.suitChar) else String.format("%-14s", "")
			}
			hand += "\n"
			for (i in hands.indices) {
				hand += String.format("%-14s", hands[i].label)
			}
			hand += "\n"
			//String msg = "Score:" + score + "\n" + hand + "pts=" + pts + " ptsValue=" + ptsValue + " faceDown=" + facedownValue + " setPotential=" + setPotentialValue + "\n";
			println(hand)
		}

		/*
     * Package access for unit tests
     */
		fun internalComputeHandScore(rules: Rules, hand: Hand, randFactor: Double) {
			// get the actual showing score of this hand
			val cards = hand.cards
			val pts = getHandPoints(rules, cards)
			if (Math.abs(pts) > maxPoints) {
				maxPoints = Math.abs(pts)
			}
			// get the number of face down cards and return an value whose graph is 2^x 
			val facedownValue = 1.0 - Math.pow(2.0, countFaceDownCards(cards).toDouble()) / 100
			var setPotentialValue = 0.0
			if (rules.gameType == Rules.GameType.NineCard) {
				// visit each cards and see check all adjacent cards to see if we make a set
				// potential sets are +1.  Max possible is 99 if all squares are same rank
				val xStep = intArrayOf(-1, 0, 1, 0, -2, 0, 2, 0)
				val yStep = intArrayOf(0, 1, 0, -1, 0, -2, 0, 2)
				assert(xStep.size == yStep.size)
				for (i in cards.indices) {
					for (ii in cards[i].indices) {
						if (cards[i][ii]?.isShowing == false) continue
						for (iii in xStep.indices) {
							val r = i + xStep[iii]
							val c = ii + yStep[iii]
							if (r >= 0 && r < cards.size && (c >= 0) and (c < cards[r].size)) {
								if (cards[r][c]?.isShowing == true && isSet(rules, cards[i][ii], cards[r][c])) {
									setPotentialValue += 1.0
								}
							}
						}
					}
				}
				if (setPotentialValue > maxSetPotential) maxSetPotential = setPotentialValue
				setPotentialValue /= maxSetPotential
			}
			val randomFactor = 0.01
			val ptsFactor = 0.45
			val faceDownFactor = 0.2
			val setPotentialFactor = 0.34
			val ptsValue = (maxPoints - pts).toDouble() / (maxPoints * 2)
			val randValue = randFactor / 1000 //(double)(golf.rand() % 1000) / 1000;
			val ptsScore = ptsFactor * ptsValue
			val fdScore = faceDownFactor * facedownValue
			val spScore = setPotentialFactor * setPotentialValue
			val randScore = randValue * randomFactor
			val score = ptsScore + fdScore + spScore + randScore
			hand.pts = pts
			hand.ptsScore = ptsScore
			hand.faceDownScore = fdScore
			hand.setPotentialScore = spScore
			hand.totalScore = score
			if (score < 0 || score > 1) System.err.println("Score should be between [0-1] but is: $score")

			//return score;
		}

		fun countFaceDownCards(cards: Array<Array<Card?>>): Int {
			var numDown = 0
			for (i in cards.indices) {
				for (ii in cards[i].indices) {
					if (cards[i][ii]?.isShowing == false) numDown++
				}
			}
			return numDown
		}
	}
}
