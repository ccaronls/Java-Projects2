package cc.game.kaiser.core

import cc.game.kaiser.core.Bid.Companion.parseBid
import cc.lib.utils.Reflector
import java.io.BufferedReader
import java.io.EOFException

/**
 * Keeps track of those variable relevant to a single team.
 * A team has 2 players, a customizable name, a current score and a bid.
 * @author ccaron
 */
class Team internal constructor(var name: String="") : Reflector<Team>() {
	companion object {
		init {
			addAllFields(Team::class.java)
		}
	}

	@JvmField
    var players = IntArray(2)
	@JvmField
    var bid: Bid = Bid.NO_BID
	@JvmField
    var totalPoints = 0
	@JvmField
    var roundPoints = 0

	override fun toString(): String {
		return ("Team " + name + " (" + players[0] + "/" + players[1] + ") "
			+ (if (bid != null) "Bid: $bid" else "")
			+ " pts: $totalPoints rnd: $roundPoints")
	}

	val playerA: Int
		get() = players[0]
	val playerB: Int
		get() = players[1]

	@Throws(Exception::class)
	fun parseTeamInfo(input: BufferedReader) {
		while (true) {
			val line = input.readLine() ?: throw EOFException()
			if (line.trim { it <= ' ' }.startsWith("}")) break
			val colon = line.indexOf(':')
			if (colon < 0) throw Exception("Invalid line.  Not of format: <NAME>:<VALUE>")
			val name = line.substring(0, colon).trim { it <= ' ' }
			val value = line.substring(colon + 1).trim { it <= ' ' }
			if (name.equals("BID", ignoreCase = true)) {
				bid = parseBid(value)
			} else if (name.equals("ROUND_PTS", ignoreCase = true)) {
				roundPoints = value.toInt()
			} else if (name.equals("TOTAL_PTS", ignoreCase = true)) {
				totalPoints = line.substring(colon + 1).toInt()
			} else {
				throw Exception("Unknown key '$name' while parsing Team")
			}
		}
	}
}