package cc.game.zombicide.android

import android.util.Log
import cc.game.zombicide.android.ZMPCommon.CL
import cc.lib.logger.LoggerFactory
import cc.lib.net.GameClient
import cc.lib.net.GameCommand
import cc.lib.zombicide.ZGame
import cc.lib.zombicide.ZQuests
import cc.lib.zombicide.ZUser
import cc.lib.zombicide.p2p.ZGameMP
import cc.lib.zombicide.p2p.ZUserMP
import cc.lib.zombicide.ui.UIZombicide
import java.util.*

/**
 * Created by Chris Caron on 7/28/21.
 */
class ZClientMgr(activity: ZombicideActivity, game: UIZombicide, val client: GameClient, val user: ZUser) : ZMPCommon(activity, game), GameClient.Listener, CL {
	var playerChooser: CharacterChooserDialogMP? = null
	fun shutdown() {
		client.removeListener(this)
	}

	override fun onCommand(cmd: GameCommand) {
		parseSVRCommand(cmd)
	}

	override fun onLoadQuest(quest: ZQuests) {
		game.loadQuest(quest)
		game.boardRenderer.redraw()
	}

	override fun onInit(color: Int, maxCharacters: Int, playerAssignments: List<Assignee>) {
		user.setColor(color)
		game.clearCharacters()
		game.clearUsersCharacters()
		val assignees: MutableList<Assignee> = ArrayList()
		for (c in activity.charLocks) {
			val a = Assignee(c)
			val idx = playerAssignments.indexOf(a)
			if (idx >= 0) {
				val aa = playerAssignments[idx]
				a.copyFrom(aa)
				if (color == a.color) a.isAssingedToMe = true
			}
			assignees.add(a)
			if (a.checked) {
				game.addCharacter(a.name)
				if (a.isAssingedToMe) {
					user.addCharacter(a.name)
				}
			}
		}
		activity.runOnUiThread {
			playerChooser = object : CharacterChooserDialogMP(activity, assignees, maxCharacters) {
				override fun onAssigneeChecked(assignee: Assignee, checked: Boolean) {
					Log.d(TAG, "onAssigneeChecked: $assignee")
					val cmd = activity.clientMgr!!.newAssignCharacter(assignee.name, checked)
					object : CLSendCommandSpinnerTask(activity, SVR_ASSIGN_PLAYER) {
						override fun onSuccess() {
							assignee.checked = checked
							postNotifyUpdateAssignee(assignee)
						}
					}.execute(cmd)
				}

				override fun onStart() {
					game.client = client
					client.sendCommand(newStartPressed())
					activity.initGameMenu()
					activity.game.showQuestTitleOverlay()
				}
			}
			game.boardRenderer.redraw()
		}
	}

	override fun onAssignPlayer(assignee: Assignee) {
		Log.d("ZClientMgr", "onAssignPlayer: $assignee")
		if (user.colorId == assignee.color) assignee.isAssingedToMe = true
		if (assignee.checked) {
			game.addCharacter(assignee.name)
			if (assignee.isAssingedToMe) user.addCharacter(assignee.name)
		} else {
			game.removeCharacter(assignee.name)
			user.removeCharacter(assignee.name)
		}
		game.boardRenderer.redraw()
		playerChooser?.postNotifyUpdateAssignee(assignee)
	}

	override fun onError(e: Exception) {
		log.error(e)
		game.addPlayerComponentMessage("Error: ${e.javaClass.simpleName} :  + ${e.message}")
	}

	override val gameForUpdate: ZGame
		get() = game

	override fun onGameUpdated(game: ZGame) {
		this.game.boardRenderer.redraw()
		this.game.characterRenderer.redraw()
	}

	override fun onMessage(msg: String) {
		game.addPlayerComponentMessage(msg)
	}

	override fun onDisconnected(reason: String, serverInitiated: Boolean) {
		if (serverInitiated) {
			activity.game.setResult(null)
			playerChooser?.dialog?.dismiss()
			activity.runOnUiThread {
				activity.newDialogBuilder().setTitle("Disconnected from Server")
					.setMessage("Do you want to try and Reconnect?")
					.setNegativeButton(R.string.popup_button_no) { dialog, which -> activity.p2pShutdown() }.setPositiveButton(R.string.popup_button_yes) { dialog, which -> client.reconnectAsync() }.show()
			}
		}
	}

	override fun onConnected() {
		client.register(ZUserMP.USER_ID, user)
		client.register(ZGameMP.GAME_ID, game)
		client.displayName = activity.displayName
	}

	companion object {
		var log = LoggerFactory.getLogger(ZClientMgr::class.java)
	}

	init {
		client.addListener(this)
	}
}