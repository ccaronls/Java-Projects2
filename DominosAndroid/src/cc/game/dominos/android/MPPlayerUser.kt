package cc.game.dominos.android

import android.widget.Toast
import cc.game.dominos.core.Dominos
import cc.game.dominos.core.MPConstants
import cc.game.dominos.core.Move
import cc.game.dominos.core.Player
import cc.game.dominos.core.PlayerUser
import cc.lib.annotation.Keep
import cc.lib.net.AGameClient
import cc.lib.net.GameClient
import cc.lib.net.GameCommand
import cc.lib.utils.GException

/**
 * Created by chriscaron on 3/14/18.
 *
 * This is a user that can be connected as a client to server or just a SP normal user
 */
class MPPlayerUser internal constructor(
	private val client: GameClient,
	private val activity: DominosActivity,
	private val dominoes: Dominos
) : PlayerUser(), AGameClient.Listener {
	init {
		client.register(MPConstants.USER_ID, this)
		client.register(MPConstants.DOMINOS_ID, dominoes)
		client.addListener(this)
	}

	override fun getName(): String {
		return "P" + (playerNum + 1) + " " + client.displayName
	}

	@Keep
	fun chooseMove(options: List<Move>): Move? {
		// TODO: we should be able to apply the move our self to prevent the visual glitch that happens
		//    while waiting for the server to process our move
		return chooseMove(dominoes, options)
	}

	override fun onCommand(cmd: GameCommand) {
		if (cmd.type == MPConstants.SVR_TO_CL_INIT_GAME) {
			val numPlayers = cmd.getInt("numPlayers", -1)
			if (numPlayers < 2 || numPlayers > 4) throw GException("invalid numPlayers: $numPlayers")
			var playerNum = cmd.getInt("playerNum", -1)
			if (playerNum < 0 || playerNum >= numPlayers) throw GException("invalid playerNum: $playerNum")
			val players = Array(numPlayers) { idx ->
				if (idx == playerNum) {
					this
				} else Player(idx)
			}
			dominoes.clear()
			dominoes.setPlayers(*players)
		} else if (cmd.type == MPConstants.SVR_TO_CL_INIT_ROUND) {
			try {
				dominoes.reset()
				cmd.getReflector("dominos", dominoes)
				activity.dismissCurrentDialog()
				if (dominoes.board.root == null) {
					dominoes.startShuffleAnimation()
				}
			} catch (e: Exception) {
				client.sendError(e)
				client.disconnect("Error: " + e.message)
			}
		} else {
			client.sendError("Dont know how to handle cmd: '$cmd'")
		}
	}

	override fun onMessage(message: String) {
		activity.runOnUiThread { Toast.makeText(activity, message, Toast.LENGTH_LONG).show() }
	}

	override fun onDisconnected(reason: String, serverInitiated: Boolean) {
		client.removeListener(this)
		client.unregister(MPConstants.USER_ID)
		client.unregister(MPConstants.DOMINOS_ID)
		activity.runOnUiThread {
			activity.newDialogBuilder().setTitle("Disconnected")
				.setMessage("You have been disconnected from the server.")
				.setNegativeButton("Ok") { dialog, which -> activity.showNewGameDialog() }.setCancelable(false).show()
		}
		activity.killGame()
	}

	override fun onConnected() {
//		synchronized(this) { notify() }
		activity.runOnUiThread { Toast.makeText(activity, "Connected", Toast.LENGTH_LONG).show() }
	}
}
