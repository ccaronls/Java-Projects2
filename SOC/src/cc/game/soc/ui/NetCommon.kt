package cc.game.soc.ui

import cc.lib.crypt.Cypher
import cc.lib.crypt.HuffmanEncoding
import cc.lib.net.GameCommandType

/**
 * Created by chriscaron on 3/12/18.
 */
object NetCommon {
	const val USER_ID = "PlayerUser"
	const val SOC_ID = "SOC"
	const val PORT = 15551
	const val VERSION = "SOC1.0"
	const val CLIENT_READ_TIMEOUT = 20000
	const val MAX_CONNECTIONS = 8
	const val DNS_SERVICE_ID = "_soc._tcp.local."
	@JvmStatic
    val cypher: Cypher
		get() = try {
			val counts = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 275883, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1257558, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 94, 339046, 0, 812, 9178, 10282, 9494, 8439, 7550, 9458, 13, 9, 13, 0, 0, 0, 21056, 0, 0, 0, 7718, 1142, 48994, 1684, 11074, 57, 500, 144, 80896, 0, 57, 7969, 71, 8567, 35989, 12425, 0, 13645, 80100, 5183, 3768, 3282, 174, 1793, 4170, 0, 0, 0, 0, 0, 8671, 0, 292194, 108, 241162, 19716, 258754, 13, 147110, 14, 12327, 56905, 42303, 72978, 91145, 109648, 104923, 3889, 0, 128677, 59648, 163926, 16152, 56921, 0, 1536, 10810, 0, 105802, 0, 105802)
			val enc = HuffmanEncoding(counts)
			enc.keepAllOccurances()
			enc.generate()
			enc
		} catch (e: Exception) {
			throw RuntimeException(e)
		}
	@JvmField
    val SVR_TO_CL_INIT = GameCommandType("SVR_TO_CL_INIT")
	@JvmField
    val SVR_TO_CL_UPDATE = GameCommandType("SVR_TO_CL_UPDATE")
}