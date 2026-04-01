package cc.game.dominos.core

/**
 * Created by Chris Caron on 3/31/26.
 */
interface IDominosClient {

}

interface IDominosServer {

}

interface IDominosConnection {
	fun chooseMove(moves: List<Move>): Move?

}