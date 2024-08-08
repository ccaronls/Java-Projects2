package cc.game.zombicide.android

import android.util.Log
import cc.game.zombicide.android.ZMPCommon.CL
import cc.lib.logger.LoggerFactory
import cc.lib.net.AGameClient
import cc.lib.net.GameCommand
import cc.lib.zombicide.ZGame
import cc.lib.zombicide.ZQuests
import cc.lib.zombicide.ZUser
import cc.lib.zombicide.p2p.ZGameMP
import cc.lib.zombicide.p2p.ZUserMP
import cc.lib.zombicide.ui.ConnectedUser
import cc.lib.zombicide.ui.UIZombicide
import java.util.Collections

/**
 * Created by Chris Caron on 7/28/21.
 */
class ZClientMgr(activity: ZombicideActivity, game: UIZombicide, override val client: AGameClient, val user: ZUser) :
	CL(activity, game), AGameClient.Listener, ZMPCommon.CLListener {

	var playerChooser: CharacterChooserDialogMP? = null
	val connectedPlayers = Collections.synchronizedList(mutableListOf<ConnectedUser>())

	init {
		addListener(this)
		client.setProperty("name", user.name)
		client.setProperty("color", user.colorId)
	}

	fun shutdown() {
		removeListener(this)
		client.removeListener(this)
	}

	override fun onCommand(cmd: GameCommand) {
		parseSVRCommand(cmd)
	}

	override fun onLoadQuest(quest: ZQuests) {
		game.loadQuest(quest)
		game.boardRenderer.redraw()
	}

	override fun onInit(color: Int, quest: ZQuests) {
		game.client = client
		game.setUserColorId(user, color)
		game.loadQuest(quest)
		activity.runOnUiThread {
			activity.initGameMenu()
		}
	}

	override fun onOpenAssignments(maxChars: Int, assignments: List<Assignee>) {
		showPlayerChooserDialog(maxChars, assignments)
	}

	override fun onMaxCharactersPerPlayerUpdated(max: Int) {
		activity.runOnUiThread {
			playerChooser?.updateMaxPlayers(max)
		}
	}

	fun showPlayerChooserDialog(maxCharacters: Int, playerAssignments: List<Assignee>) {
		activity.runOnUiThread {
			val assignees: MutableList<Assignee> = ArrayList()
			playerAssignments.forEach { assignee ->
				activity.charLocks.firstOrNull { assignee.name == it.player }?.let {
					assignee.lock = it
					assignees.add(assignee)
					if (assignee.isAssingedToMe)
						user.addCharacter(game.addCharacter(assignee.name))
				} ?: run {
					if (assignee.checked)
						game.addCharacter(assignee.name)
				}
			}
			playerChooser =
				object : CharacterChooserDialogMP(activity, assignees, false, maxCharacters) {
					override fun onAssigneeChecked(assignee: Assignee, checked: Boolean) {
						Log.d(TAG, "onAssigneeChecked: $assignee")
						val cmd = activity.clientMgr!!.newAssignCharacter(assignee.name, checked)
						object : CLSendCommandSpinnerTask(activity) {

							override fun onAssignPlayer(_assignee: Assignee) {
								if (assignee.name == _assignee.name)
									release()
							}

						override fun onSuccess() {
							assignee.checked = checked
							postNotifyUpdateAssignee(assignee)
						}
					}.execute(cmd)
				}

				override fun onStart() {
					dialog.dismiss()
					object : CLSendCommandSpinnerTask(activity) {

						override val timeout: Long = 30000L

						init {
							progressMessage = "Starting Game ...."
						}

						override fun onPlayerPressedStart(userName: String, numStarted: Int, numTotal: Int) {
							if (numStarted >= numTotal) {
								release()
								activity.runOnUiThread {
									activity.initGameMenu()
									activity.game.showQuestTitleOverlay()
								}
							} else
								publishProgress(arrayOf(numStarted, numTotal))
						}

						override fun onProgressUpdate(values: Array<Int>) {
							progressMessage = "${values[0]} of ${values[1]} have started"
						}

					}.execute(newStartPressed())
				}
			}
			game.boardRenderer.redraw()
		}
	}

	override fun onAssignPlayer(assignee: Assignee) {
		Log.d("ZClientMgr", "user.colorId: ${user.colorId} onAssignPlayer: $assignee")
		assignee.isAssingedToMe = user.colorId == assignee.color
		if (assignee.checked) {
			val c = game.addCharacter(assignee.name)
			if (assignee.isAssingedToMe) user.addCharacter(c)
		} else {
			game.removeCharacter(assignee.name)?.let {
				user.removeCharacter(it)
			}
		}
		game.boardRenderer.redraw()
		playerChooser?.postNotifyUpdateAssignee(assignee)
	}

	override fun onNetError(e: Exception) {
		log.error(e)
		game.addPlayerComponentMessage("Error: ${e.javaClass.simpleName} :  + ${e.message}")
	}

	override fun onGameUpdated(game: ZGame) {
		this.game.boardRenderer.redraw()
		this.game.characterRenderer.redraw()
	}

	override fun onMessage(msg: String) {
		game.addPlayerComponentMessage(msg)
	}

	override fun onDisconnected(reason: String, serverInitiated: Boolean) {
		activity.game.setResult(null)
		playerChooser?.dialog?.dismiss()
		activity.runOnUiThread {
			if (serverInitiated) {
				activity.p2pShutdown()
				activity.newDialogBuilder().setTitle("Disconnected from Server")
					.setMessage("Server Stopped: $reason")
					.setNegativeButton(R.string.popup_button_ok, null).show()
			} else activity.newDialogBuilder().setTitle("Disconnected from Server")
				.setMessage("Do you want to try and Reconnect?")
				.setNegativeButton(R.string.popup_button_no) { dialog, which -> activity.p2pShutdown() }
				.setPositiveButton(R.string.popup_button_yes) { dialog, which -> client.reconnectAsync() }
				.show()
		}
	}

	override fun onConnected() {
		client.register(ZUserMP.USER_ID, user)
		client.register(ZGameMP.GAME_ID, game)
		client.displayName = activity.displayName
	}

	override fun onConnectionsInfo(connections: List<ConnectedUser>) {
		connectedPlayers.clear()
		connectedPlayers.addAll(connections)
		activity.game.boardRenderer.redraw()
	}

	fun setColorId(id: Int) {
		client.setProperty("color", id)
	}

	companion object {
		var log = LoggerFactory.getLogger(ZClientMgr::class.java)
	}

	init {
		client.addListener(this)
	}
}