package cc.game.zombicide.android

import android.animation.LayoutTransition
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.AsyncTask
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.Formatter
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cc.game.zombicide.android.ZButton.Companion.build
import cc.game.zombicide.android.databinding.ActivityZombicideBinding
import cc.game.zombicide.android.databinding.AssignDialogItemBinding
import cc.game.zombicide.android.databinding.TooltippopupLayoutBinding
import cc.lib.android.*
import cc.lib.game.GRectangle
import cc.lib.mp.android.P2PActivity
import cc.lib.ui.IButton
import cc.lib.utils.*
import cc.lib.zombicide.*
import cc.lib.zombicide.ZQuests.Companion.questsBlackPlague
import cc.lib.zombicide.ZQuests.Companion.questsWolfsburg
import cc.lib.zombicide.ui.*
import cc.lib.zombicide.ui.UIZombicide.Companion.instance
import cc.lib.zombicide.ui.UIZombicide.UIMode
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.Pair

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 *
 */
class ZombicideActivity : P2PActivity(), View.OnClickListener, OnItemClickListener, OnItemLongClickListener {
	lateinit var zb: ActivityZombicideBinding
	lateinit var vm: ActivityViewModel
	lateinit var gameFile: File
	lateinit var statsFile: File
	lateinit var savesMapFile: File
	lateinit var game: UIZombicide
	val thisUser: ZUser = UIZUser()
	var clientMgr: ZClientMgr? = null
	var serverMgr: ZServerMgr? = null
	lateinit var boardRenderer: UIZBoardRenderer
	lateinit var characterRenderer: UIZCharacterRenderer
	val stats = Stats()
	val isWolfburgUnlocked: Boolean
		get() = if (BuildConfig.DEBUG) true else stats.isQuestCompleted(ZQuests.Trial_by_Fire, ZDifficulty.MEDIUM)
	var organizeDialog : OrganizeDialog? = null

	val charLocks = arrayOf(
		CharLock(ZPlayerName.Ann, 0),
		CharLock(ZPlayerName.Baldric, 0),
		CharLock(ZPlayerName.Clovis, 0),
		CharLock(ZPlayerName.Samson, 0),
		CharLock(ZPlayerName.Nelly, 0),
		CharLock(ZPlayerName.Silas, 0),
		object : CharLock(ZPlayerName.Tucker, R.string.char_lock_tucker) {
			override val isUnlocked: Boolean
				get() = stats.isQuestCompleted(ZQuests.Big_Game_Hunting, ZDifficulty.MEDIUM)
		},
		object : CharLock(ZPlayerName.Jain, R.string.char_lock_jain) {
			override val isUnlocked: Boolean
				get() = stats.isQuestCompleted(ZQuests.The_Black_Book, ZDifficulty.HARD)
		},
		object : CharLock(ZPlayerName.Benson, R.string.char_lock_benson) {
			override val isUnlocked: Boolean
				get() = stats.isQuestCompleted(ZQuests.The_Evil_Temple, ZDifficulty.HARD)
		},
		object : CharLock(ZPlayerName.Karl, R.string.char_lock_wolfz) {
			override val isUnlocked: Boolean
				get() = isWolfburgUnlocked
		},
		object : CharLock(ZPlayerName.Morrigan, R.string.char_lock_wolfz) {
			override val isUnlocked: Boolean
				get() = isWolfburgUnlocked
		},
		object : CharLock(ZPlayerName.Ariane, R.string.char_lock_wolfz) {
			override val isUnlocked: Boolean
				get() = isWolfburgUnlocked
		},
		object : CharLock(ZPlayerName.Theo, R.string.char_lock_wolfz) {
			override val isUnlocked: Boolean
				get() = isWolfburgUnlocked
		})

	override fun getConnectPort(): Int {
		return ZMPCommon.CONNECT_PORT
	}

	override fun getVersion(): String {
		return ZMPCommon.VERSION
	}

	override fun getMaxConnections(): Int {
		return 2
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		ZGame.DEBUG = BuildConfig.DEBUG
		hideNavigationBar()
		vm = ViewModelProvider(this).get(ActivityViewModel::class.java)
		zb = ActivityZombicideBinding.inflate(layoutInflater)
		zb.viewModel = vm
		zb.lifecycleOwner = this
		setContentView(zb.root)
		zb.listMenu.onItemClickListener = this
		zb.listMenu.onItemLongClickListener = this
		zb.bZoom.setOnClickListener(this)
		zb.bUp.setOnClickListener(this)
		zb.bUseleft.setOnClickListener(this)
		zb.bUseright.setOnClickListener(this)
		zb.bCenter.setOnClickListener(this)
		zb.bVault.setOnClickListener(this)
		zb.bLeft.setOnClickListener(this)
		zb.bDown.setOnClickListener(this)
		zb.bRight.setOnClickListener(this)
		characterRenderer = UIZCharacterRenderer(zb.consoleView)
		boardRenderer = object : UIZBoardRenderer(zb.boardView) {
			override fun onLoaded() {
				zb.vgTop.layoutTransition = LayoutTransition()
				zb.listMenu.visibility = View.VISIBLE
				vm.loading.postValue(false)
			}

			override fun onLoading() {
				zb.vgTop.layoutTransition = null
				zb.listMenu.visibility = View.GONE
				vm.loading.postValue(true)
			}
		}
		boardRenderer.drawTiles = true
		boardRenderer.miniMapMode = prefs.getInt("miniMapMode", 1)

		val lock = Lock()

		game = object : UIZombicide(characterRenderer, boardRenderer) {
			override fun runGame(): Boolean {
				var changed = false
				try {
					// block here until the save game is completed
					lock.block()
					changed = super.runGame()
					log.debug("runGame changed=$changed")

					if (changed) {
						GlobalScope.launch {
							lock.acquire()
							pushGameState()
							lock.release()
						}
					}
					zb.boardView.postInvalidate()
					zb.consoleView.postInvalidate()
				} catch (e: Exception) {
					e.printStackTrace()
					stopGameThread()
					runOnUiThread {
						newDialogBuilder().setTitle("ERROR").setMessage(" ${e.javaClass.simpleName}:${e.message}").setNegativeButton("Ok", null).show()
					}
				}
				return changed
			}

			override val thisUser: ZUser
				get() = this@ZombicideActivity.thisUser

			override fun <T> waitForUser(expectedType: Class<T>): T? {
				zb.boardView.post { initMenu(uiMode, options) }
				return super.waitForUser(expectedType)
			}

			override fun onQuestComplete() {
				super.onQuestComplete()
				runOnUiThread {
					stopGame()
					completeQuest(quest.quest)
					initHomeMenu()
				}
			}

			override fun setResult(result: Any?) {
				Log.i(TAG, "setResult $result")
				game.boardRenderer.setOverlay(null)
				super.setResult(result)
			}

			override fun isGameRunning(): Boolean {
				return super.isGameRunning() || clientMgr != null
			}

			override fun onCurrentCharacterUpdated(priorPlayer: ZPlayerName?, player: ZPlayerName?) {
				super.onCurrentCharacterUpdated(priorPlayer, player)
				runOnUiThread { initGameMenu() }
			}

			override fun onCurrentUserUpdated(user: ZUser) {
				super.onCurrentUserUpdated(user)
				runOnUiThread { initGameMenu() }
			}

			override fun undo() {
				tryUndo()
			}

			override val isOrganizeEnabled: Boolean = true

			override fun onOrganizeStarted(primary : ZPlayerName, secondary : ZPlayerName?) {
				runOnUiThread {
					if (organizeDialog?.isShowing != true) {
						organizeDialog?.dismiss()
						organizeDialog = OrganizeDialog(this@ZombicideActivity).also {
							it.show()
						}
					}
					organizeDialog?.viewModel?.primaryCharacter?.value = primary.character
					organizeDialog?.viewModel?.secondaryCharacter?.value = secondary?.character
				}
			}

			override fun onOrganizeDone() {
				runOnUiThread {
					organizeDialog?.dismiss()
					organizeDialog = null
				}
			}

			override fun updateOrganize(character: ZCharacter, list: List<ZMove>): ZMove? {
				runOnUiThread {
					organizeDialog?.let {
						it.viewModel.allOptions.value = list
					}
				}
				return waitForUser(ZMove::class.java)
			}
		}
		val colorIdx = prefs.getInt("userColorIndex", 0)
		thisUser.setColor(colorIdx)
		game.addUser(thisUser)
	}

	fun loadCharacters(playersSet: Collection<String>) {
		game.clearCharacters()
		game.clearUsersCharacters()
		val players = playersSet.map { ZPlayerName.valueOf(it) }
		for (player in players) {
			game.addCharacter(player)
			thisUser.addCharacter(player)
		}
	}

	override fun onResume() {
		super.onResume()
		setKeepScreenOn(true)
	}

	override fun onPause() {
		super.onPause()
		setKeepScreenOn(false)
	}

	override fun onStart() {
		super.onStart()
		game.setDifficulty(savedDifficulty)
		gameFile = File(filesDir, "game.save")
		statsFile = File(filesDir, "stats.save")
		savesMapFile = File(filesDir, "saves.save")
		//if (!gameFile.exists() || !game.tryLoadFromFile(gameFile)) {
		//    showWelcomeDialog(true);
		//} else
		if (gameFile.exists() && game.tryLoadFromFile(gameFile)) {
			game.showSummaryOverlay()
		} else {
			game.loadQuest(ZQuests.Tutorial)
		}
		if (statsFile.exists()) {
			try {
				log.debug(FileUtils.fileToString(statsFile))
			} catch (e: IOException) {
				e.printStackTrace()
			}
			stats.tryLoadFromFile(statsFile)
		}
		initHomeMenu()
	}

	override fun onStop() {
		super.onStop()
		stopGame()
	}

	override fun newDialogBuilder(): AlertDialog.Builder {
		val b: AlertDialog.Builder = object : AlertDialog.Builder(ContextThemeWrapper(this, android.R.style.Theme_Holo_Dialog)) {
			override fun show(): AlertDialog {
				val d = super.show()
				d.setCanceledOnTouchOutside(false)
				return d
			}
		}
		if (!BuildConfig.DEBUG) b.setCancelable(false)
		return b
	}

	enum class MenuItem : UIZButton {
		RESUME,
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
		RULES,
		CHOOSE_COLOR,
		EMAIL_REPORT,
		MINIMAP_MODE;

		fun isHomeButton(instance: ZombicideActivity): Boolean {
			when (this) {
				LOAD, SAVE, ASSIGN, CLEAR, UNDO, DIFFICULTY, CHOOSE_COLOR -> return BuildConfig.DEBUG
				START, NEW_GAME, JOIN_GAME, SETUP_PLAYERS, SKILLS, LEGEND, EMAIL_REPORT, MINIMAP_MODE -> return true
				CONNECTIONS -> return instance.serverControl != null
				RESUME -> return instance.gameFile.exists()
			}
			return false
		}

		fun isGameButton(instance: ZombicideActivity): Boolean {
			when (this) {
				LOAD, SAVE, START, ASSIGN, RESUME, NEW_GAME, JOIN_GAME, SETUP_PLAYERS, CLEAR -> return false
				CONNECTIONS -> return instance.serverControl != null
				UNDO, SEARCHABLES, CHOOSE_COLOR -> return BuildConfig.DEBUG
			}
			return true
		}

		override fun getRect(): GRectangle {
			return GRectangle.EMPTY
		}

		override fun getTooltipText(): String? {
			return null
		}

		override fun getLabel(): String {
			return prettify(name)
		}

		fun isEnabled(z: ZombicideActivity): Boolean {
			return if (this == UNDO) {
				z.client != null || FileUtils.hasBackupFile(z.gameFile)
			} else true
		}
	}

	val savedDifficulty: ZDifficulty
		get() = ZDifficulty.valueOf(prefs.getString("difficulty", ZDifficulty.MEDIUM.name)!!)

	override fun onClick(v: View) {
		try {
			game.boardRenderer.setOverlay(null)
			when (v.id) {
				R.id.b_zoom -> {
					val curZoom = game.boardRenderer.zoomPercent
					if (curZoom < 1) {
						game.boardRenderer.animateZoomTo(curZoom + .5f)
					} else {
						game.boardRenderer.animateZoomTo(0f)
					}
					game.boardRenderer.redraw()
				}
				R.id.b_center -> {
					game.boardRenderer.clearDragOffset()
					if (v.tag != null) {
						game.setResult(v.tag)
						clearKeypad()
					}
				}
				R.id.b_useleft, R.id.b_useright, R.id.b_vault, R.id.b_up, R.id.b_left, R.id.b_down, R.id.b_right -> if (v.tag != null) {
					game.setResult(v.tag)
					clearKeypad()
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
		if (view.tag is MenuItem) {
			processMainMenuItem(view.tag as MenuItem)
		} else {
			instance.setResult(view.tag)
		}
	}

	override fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
		if (view.tag != null && view.tag is IButton) {
			val button = view.tag as IButton
			showToolTipTextPopup(parent, button)
			return true
		}
		return false
	}

	fun showToolTipTextPopup(view: View, button: IButton) {
		button.tooltipText?.takeIf { it.isNotEmpty() }?.let { ttText ->
			val d : Dialog = with (TooltippopupLayoutBinding.inflate(LayoutInflater.from(this))) {
				header.text = button.label
				text.text = ttText
				AlertDialog.Builder(this@ZombicideActivity, R.style.ZTooltipDialogTheme)
					.setView(root).create()
			}
			//val popup = View.inflate(this, R.layout.tooltippopup_layout, null)
			//(popup.findViewById<View>(R.id.header) as TextView).text = button.label
			//(popup.findViewById<View>(R.id.text) as TextView).text = text
			//val d: Dialog = AlertDialog.Builder(this, R.style.ZTooltipDialogTheme)
			//	.setView(popup).create()
			val window = d.window
			val outPos = intArrayOf(0, 0)
			view.getLocationOnScreen(outPos)
			if (window != null) {
				val lp = window.attributes
				lp.gravity = Gravity.TOP or Gravity.LEFT
				lp.x = outPos[0] + view.width
				lp.y = outPos[1]
				lp.width = DroidUtils.convertDipsToPixels(this, 200f)
			}
			d.show()
		}
	}

	fun startGame() {
		game.startGameThread()
		initGameMenu()
		game.refresh()
	}

	fun shutdownMP() {
		p2pShutdown()
		clientControl = null
		serverControl = null
		clientMgr = null
		serverMgr = null
	}

	fun stopGame() {
		game.stopGameThread()
		game.setResult(null)
		initHomeMenu()
	}

	val storedCharacters: Set<String>
		get() = prefs.getStringSet(PREF_PLAYERS, defaultPlayers)!!

	fun processMainMenuItem(item: MenuItem?) {
		when (item) {
			MenuItem.START -> if (game.roundNum > 0) {
				newDialogBuilder().setTitle(R.string.popup_title_confirm).setMessage(R.string.popup_message_confirm_restart)
					.setNegativeButton(R.string.popup_button_cancel, null)
					.setPositiveButton(R.string.popup_button_newgame) { dialogInterface: DialogInterface?, i: Int ->
						FileUtils.deleteFileAndBackups(gameFile)
						game.reload()
						loadCharacters(storedCharacters)
						startGame()
					}.show()
			} else {
				FileUtils.deleteFileAndBackups(gameFile)
				game.reload()
				startGame()
			}
			MenuItem.RESUME -> {
				thisUser.clearCharacters()
				for (pl in game.allCharacters) {
					if (thisUser.getColor() == pl.character.color) thisUser.addCharacter(pl) else pl.character.isInvisible = true
				}
				startGame()
			}
			MenuItem.QUIT -> if (client != null) {
				newDialogBuilder().setTitle(R.string.popup_title_confirm)
					.setMessage(R.string.popup_message_confirm_disconnect)
					.setNegativeButton(R.string.popup_button_cancel, null)
					.setPositiveButton(R.string.popup_button_disconnect) { dialog: DialogInterface?, which: Int ->
						object : SpinnerTask<Int>(this@ZombicideActivity) {
							@Throws(Exception::class)
							override fun doIt(vararg args: Int?) {
								client.disconnect("Quit Game")
							}

							override fun onCompleted() {
								shutdownMP()
								stopGame()
							}
						}.execute()
					}.show()
			} else if (server != null) {
				newDialogBuilder().setTitle(R.string.popup_title_confirm)
					.setMessage(R.string.popup_message_confirm_disconnect)
					.setNegativeButton(R.string.popup_button_cancel, null)
					.setPositiveButton(R.string.popup_button_disconnect) { dialog: DialogInterface?, which: Int ->
						object : SpinnerTask<Int>(this@ZombicideActivity) {
							@Throws(Exception::class)
							override fun doIt(vararg args: Int?) {
								server.stop()
							}

							override fun onCompleted() {
								stopGame()
								shutdownMP()
							}
						}.execute()
					}.show()
			} else {
				stopGame()
				game.setResult(null)
			}
			MenuItem.CANCEL -> if (game.isGameRunning()) {
				game.setResult(null)
			} else {
				initHomeMenu()
			}
			MenuItem.OBJECTIVES -> {
				game.showObjectivesOverlay()
			}
			MenuItem.SUMMARY -> {
				game.showSummaryOverlay()
			}
			MenuItem.NEW_GAME -> {
				shutdownMP()
				showNewGameDialog()
			}
			MenuItem.JOIN_GAME -> {
				p2pInit(P2PMode.CLIENT)
			}
			MenuItem.SETUP_PLAYERS -> showSetupPlayersDialog()
			MenuItem.CONNECTIONS -> serverControl!!.openConnections()
			MenuItem.CLEAR -> {
				prefs.edit().remove("completedQuests").apply()
				val byteDeleted = FileUtils.deleteFileAndBackups(gameFile)
				statsFile.delete()
				log.debug("deleted " + Formatter.formatFileSize(this, byteDeleted) + " of memory")
				initHomeMenu()
			}
			MenuItem.SEARCHABLES -> {
				val lv = ListView(this)
				val searchables = game.allSearchables.toMutableList()
				searchables.reverse()
				lv.adapter = object : BaseAdapter() {
					override fun getCount(): Int {
						return searchables.size
					}

					override fun getItem(position: Int): Any {
						return 0
					}

					override fun getItemId(position: Int): Long {
						return 0
					}

					override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
						val convertView = convertView?:TextView(this@ZombicideActivity)
						(convertView as TextView).text = searchables[position].label
						return convertView
					}
				}
				newDialogBuilder().setTitle(R.string.popup_title_searchables).setView(lv).setNegativeButton("Close", null).show()
			}
			MenuItem.LOAD -> newDialogBuilder().setTitle(R.string.popup_title_choose).setItems(resources.getStringArray(R.array.popup_message_choose_game_types)
			) { dialog: DialogInterface?, which: Int ->
				when (which) {
					0 -> showLoadQuestDialog()
					1 -> showLoadSavedGameDialog()
				}
			}.setNegativeButton(R.string.popup_button_cancel, null).show()
			MenuItem.SAVE -> showSaveGameDialog()
			MenuItem.ASSIGN -> showAssignDialog()
			MenuItem.DIFFICULTY -> {
				newDialogBuilder().setTitle(getString(R.string.popup_title_difficulty, savedDifficulty))
					.setItems(ZDifficulty.values().map { it.name }.toTypedArray()) { dialog: DialogInterface?, which: Int ->
						val difficulty = ZDifficulty.values()[which]
						game.setDifficulty(difficulty)
						prefs.edit().putString("difficulty", difficulty.name).apply()
					}.setNegativeButton(R.string.popup_button_cancel, null).show()
			}
			MenuItem.SKILLS -> {
				showSkillsDialog2()
			}
			MenuItem.UNDO -> {
				if (client != null) {
					object : CLSendCommandSpinnerTask(this, ZMPCommon.SVR_UPDATE_GAME) {
						override fun onSuccess() {
							zb.boardView.postInvalidate()
						}
					}.execute(clientMgr!!.newUndoPressed())
					//getClient().sendCommand(clientMgr.newUndoPressed());
					game.setResult(null)
				} else {
					tryUndo()
				}
			}
			MenuItem.LEGEND -> {
				showLegendDialog()
			}
			MenuItem.RULES -> {
				showWelcomeDialog(false)
			}
			MenuItem.EMAIL_REPORT -> {
				showEmailReportDialog()
			}
			MenuItem.CHOOSE_COLOR -> {
				showChooseColorDialog()
			}
			MenuItem.MINIMAP_MODE -> {
				prefs.edit().putInt("miniMapMode", boardRenderer.toggleDrawMinimap()).apply()
			}
		}
	}

	fun tryUndo() {
		val isRunning = game.isGameRunning()
		stopGame()
		if (FileUtils.restoreFile(gameFile)) {
			game.tryLoadFromFile(gameFile)
			game.refresh()
			serverMgr?.broadcastUpdateGame()
			organizeDialog?.viewModel?.onUndo()
		}
		if (isRunning) startGame()
	}

	fun updateCharacters(quest: ZQuests?) {
		serverMgr?.let { mgr ->
			server.broadcastCommand(mgr.newLoadQuest(quest!!))
			game.clearCharacters()
			for (user in game.getUsers()) {
				val newPlayers: MutableList<ZPlayerName> = ArrayList()
				for (pl in user.players) {
					game.addCharacter(pl)
					newPlayers.add(pl)
				}
				user.setCharacters(newPlayers)
			}
			mgr.broadcastUpdateGame()
		}?:run {
			loadCharacters(storedCharacters)
			game.trySaveToFile(gameFile)
		}
		startGame()
		zb.boardView.postInvalidate()
	}

	fun showLoadQuestDialog() {
		newDialogBuilder().setTitle(R.string.popup_title_load_quest)
			.setItems(ZQuests.values().map { prettify(it.name)}.toTypedArray()) { dialog: DialogInterface?, which: Int ->
				val q = ZQuests.values()[which]
				game.loadQuest(q)
				updateCharacters(q)
			}.setNegativeButton(R.string.popup_button_cancel, null).show()
	}

	fun showSaveGameDialog() {
		SaveGameDialog(this, MAX_SAVES)
	}

	fun showOrganizeDialog() {
		OrganizeDialog(this).show()
	}

	fun showLoadSavedGameDialog() {
		val saves: Map<String, String> = saves
		if (saves.isNotEmpty()) {
			val items = arrayOfNulls<String>(saves.size)
			newDialogBuilder().setTitle(R.string.popup_title_load_saved)
				.setItems(saves.keys.toList().toTypedArray()) { _: DialogInterface?, which: Int ->
					val fileName = saves[items[which]]
					val file = File(filesDir, fileName)
					if (game.tryLoadFromFile(file)) {
						updateCharacters(game.quest.quest)
					} else {
						newDialogBuilder().setTitle(R.string.popup_title_error)
							.setMessage(getString(R.string.popup_message_err_fileopen, fileName))
							.setNegativeButton(R.string.popup_button_ok, null).show()
					}
				}.setNegativeButton(R.string.popup_button_cancel, null).show()
		}
	}

	fun deleteSave(key: String) {
		val saves = saves
		saves[key]?.let { fileName ->
			val file = File(filesDir, fileName)
			file.delete()
		}
		saves.remove(key)
		try {
			Reflector.serializeToFile<Any>(saves, savesMapFile)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	val saves: MutableMap<String, String>
		get() {
			var saves: MutableMap<String, String>
			try {
				saves = Reflector.deserializeFromFile(savesMapFile)
			} catch (e: Exception) {
				e.printStackTrace()
				saves = LinkedHashMap()
			}
			return saves
		}

	fun pushGameState() {
		serverMgr?.broadcastUpdateGame()
		organizeDialog?.viewModel?.onGameSaved()
		log.debug("Backing up ... ")
		FileUtils.backupFile(gameFile, 32)
		game.trySaveToFile(gameFile)
	}

	fun saveGame() {
		val buf = StringBuffer()
		buf.append(game.quest.quest.displayName)
		buf.append(" ").append(game.getDifficulty().name)
		var delim = " "
		for (c in game.allCharacters) {
			buf.append(delim).append(c.name)
			delim = ","
		}
		val completePercent = game.quest.getPercentComplete(game)
		buf.append(String.format(" %d%% Completed", completePercent))
		buf.append(" ").append(SimpleDateFormat("MMM dd").format(Date()))
		var idx = 0
		var file: File? = null
		while (idx < 10) {
			val fileName = "savegame$idx"
			file = File(filesDir, fileName)
			if (!file.isFile) break
			idx++
		}
		try {
			game.saveToFile(file)
			val saves = saves
			saves[buf.toString()] = file!!.name
			Reflector.serializeToFile<Any>(saves, savesMapFile)
		} catch (e: Exception) {
			e.printStackTrace()
			Toast.makeText(this, "There was a problem saving the game.", Toast.LENGTH_LONG).show()
		}
	}

	fun showChooseColorDialog() {
		newDialogBuilder().setTitle(R.string.popup_title_choose_color)
			.setItems(ZUser.USER_COLOR_NAMES) { dialog: DialogInterface?, which: Int ->
				prefs.edit().putInt("userColorIndex", which).apply()
				thisUser.setColor(which)
				game.refresh()
			}.setNegativeButton(R.string.popup_button_cancel, null).show()
	}

	val displayName: String
		get() = prefs.getString(PREF_P2P_NAME, "Unnamed")!!

	override fun onP2PReady() {
		val p2pName = prefs.getString(PREF_P2P_NAME, null)
		if (p2pName == null) {
			showEditTextInputPopup("Set P2P Name", p2pName, "Display Name", 32) { txt: String? ->
				if (isEmpty(txt)) {
					newDialogBuilder().setMessage(R.string.popup_message_err_nonemptyname)
						.setNegativeButton(R.string.popup_button_cancel_mp) { dialog: DialogInterface?, which: Int ->
							hideKeyboard()
							p2pShutdown()
						}.setPositiveButton(R.string.popup_button_ok) { dialog: DialogInterface?, which: Int ->
							hideKeyboard()
							onP2PReady()
						}.show()
				} else {
					prefs.edit().putString(PREF_P2P_NAME, txt).apply()
					p2pStart()
				}
			}
		} else {
			p2pStart()
		}
	}

	override fun p2pStart() {
		game.clearCharacters()
		game.clearUsersCharacters()
		game.reload()
		game.refresh()
		super.p2pStart()
	}

	var clientControl: P2PClient? = null
	override fun onP2PClient(p2pClient: P2PClient) {
		clientControl = p2pClient
		clientMgr = ZClientMgr(this, game, clientControl!!.client, thisUser)
	}

	var serverControl: P2PServer? = null
	override fun onP2PServer(p2pServer: P2PServer) {
		serverControl = p2pServer
		serverMgr = ZServerMgr(this, game, 2, p2pServer.server)
		thisUser.name = prefs.getString(PREF_P2P_NAME, null)
	}

	override fun onP2PShutdown() {
		clientMgr = null
		clientControl = null
		serverMgr = null
		serverControl = null
		game.server = null
		game.client = null
		initHomeMenu()
	}

	fun showSetupPlayersDialog() {
		val saved = storedCharacters
		val assignments: MutableList<Assignee> = ArrayList()
		for (c in charLocks) {
			val a = Assignee(c)
			if (saved.contains(a.name.name)) {
				a.checked = true
				a.color = thisUser.colorId
				a.userName = thisUser.name!!
				a.isAssingedToMe = true
			}
			assignments.add(a)
		}
		object : CharacterChooserDialogMP(this@ZombicideActivity, assignments, 6) {
			override fun onAssigneeChecked(a: Assignee, checked: Boolean) {
				a.checked = checked
				if (a.checked) {
					a.color = thisUser.colorId
					a.userName = thisUser.name!!
					a.isAssingedToMe = true
				} else {
					a.color = -1
					a.userName = "??"
					a.isAssingedToMe = false
				}
			}

			override fun onStart() {
				game.clearCharacters()
				game.clearUsersCharacters()
				val players: MutableSet<String> = HashSet()
				for (a in assignments) {
					if (a.checked) {
						game.addCharacter(a.name)
						thisUser.addCharacter(a.name)
						players.add(a.name.name)
					}
				}
				prefs.edit().putStringSet(PREF_PLAYERS, players).apply()
				game.refresh()
			}
		}
	}

	fun showLegendDialog() {
		val legend = arrayOf(
			Pair(R.drawable.legend_chars, "CHARACTERS\nChoose your characters. Some are unlockable. Players can operate in any order but must execute all of their actions before switching to another player."),
			Pair(R.drawable.legend_gamepad, "GAMEPAD\nUse gamepad to perform common actions.\nLH / RH - Use Item in Left/Right hand.\nO - Toggle active player.\nZM - Zoom in/out."),
			Pair(R.drawable.legend_obj, "OBJECTIVES\nObjectives give player EXP, unlock doors, reveal special items and other things related to the Quest."),
			Pair(R.drawable.legend_vault, "VAULTS\nVaults are very handy. You can find special loot or drop loot to be pickup up later. You can also close zombies in or out of the vault. Also they can sometimes be shortcuts across the map. The only limitation is you cannot fire ranged weapons or magic into the vault from the outside or fire outside of vault from inside of it."),
			Pair(R.drawable.zwalker1, "WALKER\n${ZZombieType.Walker.description}".trimIndent()),
			Pair(R.drawable.zfatty1, "FATTY\n${ZZombieType.Fatty.description}".trimIndent()),
			Pair(R.drawable.zrunner1, "RUNNER\n${ZZombieType.Runner.description}".trimIndent()),
			Pair(R.drawable.znecro, "NECROMANCER\n${ZZombieType.Necromancer.description}".trimIndent()),
			Pair(R.drawable.zabomination, "ABOMINATION\n${ZZombieType.Abomination.description}".trimIndent()))
		val lv = ListView(this)
		lv.adapter = object : BaseAdapter() {
			override fun getCount(): Int {
				return legend.size
			}

			override fun getItem(position: Int): Any {
				return 0
			}

			override fun getItemId(position: Int): Long {
				return 0
			}

			override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
				var convertView = convertView?:View.inflate(this@ZombicideActivity, R.layout.legend_list_item, null)
				val iv_image = convertView.findViewById<ImageView>(R.id.iv_image)
				val tv_desc = convertView.findViewById<TextView>(R.id.tv_description)
				with (legend[position]) {
					iv_image.setImageResource(first)
					tv_desc.text = second
				}
				return convertView
			}
		}
		newDialogBuilder().setTitle(R.string.popup_title_legend).setView(lv).setNegativeButton(R.string.popup_button_close, null).show()
	}

	fun completeQuest(quest: ZQuests) {
		stats.completeQuest(quest, game.getDifficulty())
		stats.trySaveToFile(statsFile)
	}

	fun showWelcomeDialog(showNewGame: Boolean) {
		val b = newDialogBuilder().setTitle(R.string.popup_title_welcome).setMessage(R.string.welcome_msg)
		if (showNewGame) {
			b.setPositiveButton(R.string.popup_button_newgame) { dialog: DialogInterface?, which: Int -> showNewGameDialog() }
		} else {
			b.setPositiveButton(R.string.popup_button_close, null)
		}
		b.show()
	}

	fun showNewGameDialog() {
		newDialogBuilder().setTitle(R.string.popup_title_choose_version)
			.setItems(resources.getStringArray(if (BuildConfig.DEBUG) R.array.popup_message_choose_game_version_debug else R.array.popup_message_choose_game_version)) { dialog: DialogInterface?, which: Int ->
				when (which) {
					0 -> showNewGameChooseQuestDialog(questsBlackPlague(), stats.getCompletedQuests())
					1 -> if (isWolfburgUnlocked) showNewGameChooseQuestDialog(questsWolfsburg(), stats.getCompletedQuests()) else newDialogBuilder().setTitle(R.string.popup_title_wolflocked)
						.setMessage(R.string.popup_message_unlock_wolfburg)
						.setPositiveButton(R.string.popup_button_ok) { dialog1: DialogInterface?, which1: Int -> showNewGameDialog() }.show()
					2 -> showNewGameChooseQuestDialog(Arrays.asList(*ZQuests.values()), HashSet(Arrays.asList(*ZQuests.values())))
				}
			}.setNegativeButton(R.string.popup_button_cancel, null).show()
	}

	fun showNewGameChooseQuestDialog(allQuests: List<ZQuests>, playable: Set<ZQuests>) {
		NewGameChooseQuestDialog(this, allQuests, playable.toMutableSet())
	}

	fun showNewGameDialogChooseDifficulty(quest: ZQuests) {
		newDialogBuilder().setTitle(getString(R.string.popup_title_quest, quest.ordinal, quest.displayName))
			.setItems(ZDifficulty.values().map { it.name }.toTypedArray()) { dialog: DialogInterface?, which: Int ->
				val difficulty = ZDifficulty.values()[which]
				prefs.edit().putString("difficulty", difficulty.name).apply()
				game.setDifficulty(difficulty)
				showChooseGameModeDialog(quest)
			}.setNegativeButton(R.string.popup_button_back) { dialog: DialogInterface?, which: Int -> showNewGameDialog() }.show()
	}

	fun showChooseGameModeDialog(quest: ZQuests) {
		val modes = arrayOf(
			"Single Player",
			"Multi-player Host"
		)
		newDialogBuilder().setTitle("Choose Mode")
			.setItems(modes) { dialog: DialogInterface?, which: Int ->
				when (which) {
					0 -> showNewGameDialogChoosePlayers(quest)
					1 -> {
						game.loadQuest(quest)
						p2pInit(P2PMode.SERVER)
					}
				}
			}.setNegativeButton(R.string.popup_button_back) { dialog: DialogInterface?, which: Int -> showNewGameDialogChooseDifficulty(quest) }
			.show()
	}

	open class CharLock(val player: ZPlayerName, val unlockMessageId: Int) {
		open val isUnlocked = true
	}

	fun showNewGameDialogChoosePlayers(quest: ZQuests?) {
		object : CharacterChooserDialogSP(this@ZombicideActivity, quest!!) {
			override fun onStarted() {
				prefs.edit().putStringSet(PREF_PLAYERS, selectedPlayers).apply()
				game.loadQuest(quest!!)
				loadCharacters(storedCharacters)
				game.trySaveToFile(gameFile)
				startGame()
			}
		}
	}

	fun showSkillsDialog2() {
		SkillsDialog(this)
	}

	// TODO: Make this more organized. Should be able to order by character, danger level or all POSSIBLE
	fun showSkillsDialog() {
		val sorted = ZSkill.values().toMutableList().sortedWith { o1: ZSkill, o2: ZSkill -> o1.name.compareTo(o2.name) }
		newDialogBuilder().setTitle(R.string.popup_title_skills)
			.setItems(sorted.map { prettify(it.name) }.toTypedArray()) { dialog: DialogInterface?, which: Int ->
				val skill = sorted[which]
				newDialogBuilder().setTitle(skill.label)
					.setMessage(skill.description)
					.setNegativeButton(R.string.popup_button_cancel, null)
					.setPositiveButton(R.string.popup_button_back) { dialog1: DialogInterface?, which1: Int -> showSkillsDialog() }.show()
			}.setNegativeButton(R.string.popup_button_cancel, null).show()
	}

	fun initHomeMenu() {
		vm.playing.postValue(false)
		val buttons: MutableList<View> = ArrayList()
		MenuItem.values().filter { it.isHomeButton(this@ZombicideActivity) }.forEach { i ->
			buttons.add(build(this, i, i.isEnabled(this)))
		}
		initMenuItems(buttons)
	}

	fun initGameMenu() {
		vm.playing.postValue(true)
		initMenu(UIMode.NONE, null)
		game.refresh()
	}

	fun initKeypad(options: MutableList<Any>) {
		val it = options.iterator()
		while (it.hasNext()) {
			val o = it.next()
			if (o is ZMove) {
				val move = o
				when (move.type) {
					ZMoveType.WALK_DIR -> {
						when (ZDir.values()[move.integer!!]) {
							ZDir.NORTH -> {
								zb.bUp.tag = move
								zb.bUp.visibility = View.VISIBLE
							}
							ZDir.SOUTH -> {
								zb.bDown.tag = move
								zb.bDown.visibility = View.VISIBLE
							}
							ZDir.EAST -> {
								zb.bRight.tag = move
								zb.bRight.visibility = View.VISIBLE
							}
							ZDir.WEST -> {
								zb.bLeft.tag = move
								zb.bLeft.visibility = View.VISIBLE
							}
							ZDir.ASCEND,
							ZDir.DESCEND -> {
								zb.bVault.tag = move
								zb.bVault.visibility = View.VISIBLE
							}
						}
						it.remove()
					}
					ZMoveType.USE_LEFT_HAND -> {
						zb.bUseleft.tag = move
						zb.bUseleft.visibility = View.VISIBLE
						it.remove()
					}
					ZMoveType.USE_RIGHT_HAND -> {
						zb.bUseright.tag = move
						zb.bUseright.visibility = View.VISIBLE
						it.remove()
					}
					ZMoveType.SWITCH_ACTIVE_CHARACTER -> {
						zb.bCenter.tag = move
						zb.bCenter.visibility = View.VISIBLE
						it.remove()
					}
				}
			}
		}
	}

	fun clearKeypad() {
		zb.bUseleft.visibility = View.INVISIBLE
		zb.bUseright.visibility = View.INVISIBLE
		zb.bUp.visibility = View.INVISIBLE
		zb.bDown.visibility = View.INVISIBLE
		zb.bLeft.visibility = View.INVISIBLE
		zb.bRight.visibility = View.INVISIBLE
		zb.bVault.visibility = View.INVISIBLE
		zb.bCenter.tag = null
	}

	fun initMenu(mode: UIMode, options: List<Any>?) {
		val buttons: MutableList<View> = ArrayList()
		clearKeypad()
		options?.toMutableList()?.also { options ->
			initKeypad(options)
			when (mode) {
				UIMode.PICK_CHARACTER -> {
					zb.bCenter.tag = options[0]
					for (e:IButton in options as List<IButton>) {
						buttons.add(build(this, e, e.isEnabled))
					}
					buttons.add(ListSeparator(this))
				}
				UIMode.PICK_MENU -> {
					for (e:IButton in options as List<IButton>) {
						buttons.add(build(this, e, e.isEnabled))
					}
					buttons.add(ListSeparator(this))
				}
			}
		}
		MenuItem.values().filter { it.isGameButton(this@ZombicideActivity) }.forEach { i->
			buttons.add(build(this, i, i.isEnabled(this)))
		}
		initMenuItems(buttons)
	}

	fun initMenuItems(buttons: List<View>) {
		vm.listAdapter.update(buttons)
	}

	val defaultPlayers: Set<String>
		get() {
			val players = HashSet<String>()
			players.add(ZPlayerName.Baldric.name)
			players.add(ZPlayerName.Clovis.name)
			players.add(ZPlayerName.Silas.name)
			return players
		}

	/*
        Set<String> getDefaultUnlockedPlayers() {
            HashSet<String> players = new HashSet<>();
            players.add(ZPlayerName.Baldric.name());
            players.add(ZPlayerName.Clovis.name());
            players.add(ZPlayerName.Silas.name());
            players.add(ZPlayerName.Ann.name());
            players.add(ZPlayerName.Nelly.name());
            players.add(ZPlayerName.Samson.name());
            return players;
        }
    */
	fun showAssignDialog() {
		val selectedPlayers = storedCharacters.toMutableSet()
		val recyclerView = RecyclerView(this)
		recyclerView.layoutManager = GridLayoutManager(this, 2, LinearLayoutManager.VERTICAL, false)
		recyclerView.adapter = object : RecyclerView.Adapter<Holder>() {
			override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
				val ab = AssignDialogItemBinding.inflate(layoutInflater, parent, false)
				return Holder(ab)
			}

			override fun onBindViewHolder(holder: Holder, position: Int) {
				val player = ZPlayerName.values()[position]
				holder.ab.image.setImageResource(player.cardImageId)
				holder.ab.image.tag = player.name
				holder.ab.checkbox.isChecked = selectedPlayers.contains(player.name)
				holder.ab.image.setOnClickListener { v: View? ->
					if (selectedPlayers.contains(player.name)) {
						selectedPlayers.remove(player.name)
						holder.ab.checkbox.isChecked = false
					} else {
						selectedPlayers.add(player.name)
						holder.ab.checkbox.isChecked = true
					}
				}
				holder.ab.checkbox.isClickable = false
			}

			override fun getItemCount(): Int {
				return ZPlayerName.values().size
			}
		}
		newDialogBuilder().setTitle(R.string.popup_title_assign).setView(recyclerView).setNegativeButton(R.string.popup_button_cancel, null)
			.setPositiveButton(R.string.popup_button_ok) { dialog: DialogInterface?, which: Int ->
				Log.d(TAG, "Selected players: $selectedPlayers")
				prefs.edit().putStringSet(PREF_PLAYERS, selectedPlayers).apply()
				loadCharacters(selectedPlayers)
				game.reload()
			}.show()
	}

	class Holder(val ab: AssignDialogItemBinding) : RecyclerView.ViewHolder(ab.root)

	fun showEmailReportDialog() {
		val message = EditText(this)
		message.minLines = 5
		newDialogBuilder().setTitle(R.string.popup_title_email)
			.setView(message)
			.setNegativeButton(R.string.popup_button_cancel, null)
			.setPositiveButton(R.string.popup_button_send) { dialog: DialogInterface?, which: Int ->
				//char [] pw = { 'z', '0', 'm', 'b', '1', '3', '$', '4', 'e', 'v', 'a' };
				EmailTask(this@ZombicideActivity,
					message.editableText.toString() //, new String(pw)
				).execute(gameFile)
			}.show()
	}

	internal class EmailTask(private val context: CCActivityBase, private val message: String) : AsyncTask<File, Void, Exception>() {
		private var progress: Dialog? = null
		override fun onPreExecute() {
			progress = ProgressDialog.show(context, null, context.getString(R.string.popup_message_sending_report))
		}

		override fun doInBackground(vararg inFile: File): Exception? {
			return try {
				val date = DateFormat.format("MMddyyyy", Date()).toString()
				val zipFile = File(context.filesDir, "zh_$date.zip")
				val files = FileUtils.getFileAndBackups(inFile[0])
				FileUtils.zipFiles(zipFile, files)
				val fileSize = DroidUtils.getHumanReadableFileSize(context, zipFile)
				Log.d(TAG, "Zipped file size: $fileSize")
				EmailHelper.sendEmail(context, zipFile, "ccaronsoftware@gmail.com", "Zombies Hide Report", message)
				null
			} catch (e: Exception) {
				e.printStackTrace()
				e
			}
		}

		override fun onPostExecute(e: Exception?) {
			progress!!.dismiss()
			if (e != null) {
				context.newDialogBuilder().setTitle(R.string.popup_title_error)
					.setMessage("""An error occurred trying to send report: ${e.javaClass.simpleName} ${e.message}""")
					.setNegativeButton(R.string.popup_button_ok, null).show()
			}
		}

		companion object {
			const val TAG = "EmailTask"
		}
	}

	companion object {
		private val TAG = ZombicideActivity::class.java.simpleName
		const val MAX_PLAYERS = 6 // max number of characters on screen at one time
		const val MAX_SAVES = 4
		const val PREF_P2P_NAME = "p2pname"
		const val PREF_PLAYERS = "players"
	}
}