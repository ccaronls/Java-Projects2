package cc.game.zombicide.p2p

interface IZClient {
	val connected: Boolean

	val numSpawn: Int

	val numLoot: Int

	val hoardSize: Int
}
