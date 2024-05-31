package cc.game.zombicide.android

import cc.lib.game.GRectangle
import cc.lib.utils.FileUtils
import cc.lib.utils.prettify
import cc.lib.zombicide.ui.UIZButton
import cc.lib.zombicide.ui.UIZombicide

/**
 * Created by Chris Caron on 4/10/24.
 */

enum class MenuItem : UIZButton {
	RESUME {
		override fun getTooltipText(): String? {
			return with(UIZombicide.instance) {
				"""${quest.name}
					round: $roundNum
					players: ${allCharacters.map { it.colorId }.distinct().size}
					Completed %${quest.getPercentComplete(this)}
					""".trimMargin()
			}
		}
	},
	CANCEL,
	LOAD,
	SAVE,
	NEW_GAME,
	JOIN_GAME,
	SETUP_PLAYERS,
	CONNECTIONS,
	START,
	ASSIGN,
	SUMMARY,
	UNDO,
	DIFFICULTY,
	OBJECTIVES,
	SKILLS,
	LEGEND,
	QUIT,
	CLEAR,
	SEARCHABLES,
	DISCONNECT,
	ABOUT,
	RULES,
	CHOOSE_COLOR,
	EMAIL_REPORT,
	MINIMAP_MODE,
	DEBUG_MENU;

	fun isHomeButton(instance: ZombicideActivity): Boolean = when (this) {
		NEW_GAME, LOAD -> true
		JOIN_GAME -> instance.server?.isRunning != true
		CONNECTIONS -> instance.server?.isConnected == true
		DISCONNECT -> instance.server?.isRunning == true || instance.client != null
		RESUME -> instance.gameFile.exists()
		UNDO -> BuildConfig.DEBUG

		//LOAD, SAVE, ASSIGN, CLEAR, UNDO, DIFFICULTY, CHOOSE_COLOR, DEBUG_MENU -> BuildConfig.DEBUG
		//START, NEW_GAME, JOIN_GAME, SETUP_PLAYERS, SKILLS, LEGEND, EMAIL_REPORT, MINIMAP_MODE -> true
		//CONNECTIONS -> instance.serverControl != null
		//RESUME -> instance.gameFile.exists()
		else -> false
	}

	fun isGameButton(instance: ZombicideActivity): Boolean = when (this) {
		//LOAD, SAVE, START, ASSIGN, RESUME, NEW_GAME, JOIN_GAME, SETUP_PLAYERS, CLEAR -> false
		//CONNECTIONS -> instance.serverControl != null
		//SEARCHABLES -> BuildConfig.DEBUG
		UNDO, CANCEL -> instance.game.currentUserColorId == instance.thisUser.colorId
		//CHOOSE_COLOR -> instance.clientMgr == null
		SUMMARY, OBJECTIVES, SKILLS, MINIMAP_MODE -> true
		else -> false
	}

	fun isDebugButton(): Boolean = when (this) {
		SEARCHABLES,
		MINIMAP_MODE,
		CLEAR -> true

		else -> false
	}

	fun isMenuButton(): Boolean = when (this) {
		//RESUME,
		//CANCEL,
		//LOAD,
		//SAVE,
		//NEW_GAME,
		//JOIN_GAME,
		//SETUP_PLAYERS,
		//CONNECTIONS -> instance.server?.isConnected == true
		//START,
		//ASSIGN,
		//UNDO,
		DIFFICULTY,
		SAVE,
		CHOOSE_COLOR,
		LEGEND,
			//QUIT,
			//SEARCHABLES,
		ABOUT,
			//RULES,
		QUIT,
		EMAIL_REPORT -> true

		DEBUG_MENU -> BuildConfig.DEBUG
		else -> false
	}

	override fun getRect(): GRectangle {
		return GRectangle.EMPTY
	}

	override fun getTooltipText(): String? {
		return null
	}

	override fun getLabel(): String {
		return name.prettify()
	}

	fun isEnabled(z: ZombicideActivity): Boolean {
		return if (this == UNDO) {
			z.client != null || FileUtils.hasBackupFile(z.gameFile)
		} else true
	}
}
