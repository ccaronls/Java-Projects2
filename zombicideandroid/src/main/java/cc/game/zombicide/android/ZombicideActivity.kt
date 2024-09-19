package cc.game.zombicide.android

import android.animation.LayoutTransition
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.media.AudioManager
import android.media.SoundPool
import android.os.AsyncTask
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.Formatter
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cc.game.zombicide.android.ZButton.Companion.build
import cc.game.zombicide.android.databinding.ActivityZombicideBinding
import cc.game.zombicide.android.databinding.AssignDialogItemBinding
import cc.game.zombicide.android.databinding.TooltippopupLayoutBinding
import cc.lib.android.CCActivityBase
import cc.lib.android.ConfigDialogBuilder
import cc.lib.android.DroidUtils
import cc.lib.android.EmailHelper
import cc.lib.android.SpinnerTask
import cc.lib.android.getEnum
import cc.lib.mp.android.P2PActivity
import cc.lib.reflector.Reflector
import cc.lib.ui.IButton
import cc.lib.utils.FileUtils
import cc.lib.utils.KLock
import cc.lib.utils.enumValueOfOrNull
import cc.lib.utils.isEmpty
import cc.lib.utils.launchIn
import cc.lib.utils.prettify
import cc.lib.zombicide.ZCharacter
import cc.lib.zombicide.ZDifficulty
import cc.lib.zombicide.ZDir
import cc.lib.zombicide.ZEquipSlot
import cc.lib.zombicide.ZGame
import cc.lib.zombicide.ZMove
import cc.lib.zombicide.ZMoveType
import cc.lib.zombicide.ZPlayerName
import cc.lib.zombicide.ZQuests
import cc.lib.zombicide.ZQuests.Companion.questsBlackPlague
import cc.lib.zombicide.ZQuests.Companion.questsWolfsburg
import cc.lib.zombicide.ZSkill
import cc.lib.zombicide.ZUser
import cc.lib.zombicide.ZZombieType
import cc.lib.zombicide.ui.ConnectedUser
import cc.lib.zombicide.ui.UIZBoardRenderer
import cc.lib.zombicide.ui.UIZCharacterRenderer
import cc.lib.zombicide.ui.UIZUser
import cc.lib.zombicide.ui.UIZombicide
import cc.lib.zombicide.ui.UIZombicide.Companion.instance
import cc.lib.zombicide.ui.UIZombicide.UIMode
import cc.lib.zombicide.ui.ZSound
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Stack

inline fun isTV(): Boolean = BuildConfig.FLAVOR == "tv"

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
	val thisUser: ZUser by lazy {
		UIZUser(displayName, colorId)
	}
	var clientMgr: ZClientMgr? = null
	var serverMgr: ZServerMgr? = null
	lateinit var boardRenderer: UIZBoardRenderer
	lateinit var characterRenderer: UIZCharacterRenderer
	val stats = Stats()
	val isWolfburgUnlocked: Boolean
		get() = if (BuildConfig.DEBUG) true else stats.isQuestCompleted(ZQuests.Trial_by_Fire, ZDifficulty.MEDIUM)
	var organizeDialog: OrganizeDialog? = null

	val charLocks = arrayOf(
		CharLock(ZPlayerName.Ann, R.string.char_lock_empty_string),
		CharLock(ZPlayerName.Baldric, R.string.char_lock_empty_string),
		CharLock(ZPlayerName.Clovis, R.string.char_lock_empty_string),
		CharLock(ZPlayerName.Samson, R.string.char_lock_empty_string),
		CharLock(ZPlayerName.Nelly, R.string.char_lock_empty_string),
		CharLock(ZPlayerName.Silas, R.string.char_lock_empty_string),
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

	override val connectPort: Int = ZMPCommon.CONNECT_PORT
	override val version: String = ZMPCommon.VERSION
	override val maxConnections: Int = 2

	val soundPool: SoundPool by lazy {
		SoundPool(8, AudioManager.STREAM_MUSIC, 0)
	}

	private fun loadSound(pool: SoundPool, sound: ZSound, resId: Int) {
		sound.id = pool.load(this, resId, 1)
		Log.d("SOUND_POOL", "Loaded $sound = ${sound.id}")
	}

	private fun loadSounds() {
		loadSound(soundPool, ZSound.SWORD_SLASH, R.raw.sword_swing1)
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
		zb.listMenu.itemsCanFocus = false
		zb.bZoom.setOnClickListener(this)
		zb.bUp.setOnClickListener(this)
		zb.bUseleft.setOnClickListener(this)
		zb.bUseright.setOnClickListener(this)
		zb.bCenter.setOnClickListener(this)
		zb.bVault.setOnClickListener(this)
		zb.bLeft.setOnClickListener(this)
		zb.bDown.setOnClickListener(this)
		zb.bRight.setOnClickListener(this)
		zb.bUndo.setOnClickListener {
			tryUndo()
		}
		zb.bRepeat.setOnClickListener(this)
		zb.boardView.enablePinchZoom()
		zb.consoleView.setOnClickListener {
			game.showCharacterExpandedOverlay()
		}
		zb.bGameMenu.setOnClickListener {
			showGameMenuDialog()
		}
		characterRenderer = object : UIZCharacterRenderer(zb.consoleView) {
			override fun scrollToTop() {
				zb.svConsole.smoothScrollTo(0, 0)
			}
		}
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

			override val touchOrMouseSupported = !isTV()
		}
		boardRenderer.drawTiles = true
		boardRenderer.miniMapMode = prefs.getEnum(PREF_MINIMAP_MODE_STRING, boardRenderer.miniMapMode)

		val lock = KLock()
		loadSounds()

		game = object : UIZombicide(characterRenderer, boardRenderer) {
			override suspend fun runGame(): Boolean {
				var changed = false
				try {
					// block here until the save game is completed
					lock.block()
					changed = super.runGame()
					log.debug("runGame changed=$changed")
					pushGameState(changed)
					zb.boardView.postInvalidate()
					zb.consoleView.postInvalidate()
					characterRenderer.redraw()
				} catch (e: Exception) {
					e.printStackTrace()
					stopGameThread()
					runOnUiThread {
						newDialogBuilder().setTitle("ERROR")
							.setMessage(" ${e.javaClass.simpleName}:${e.message}")
							.setNegativeButton("Ok", null).show()
					}
				}
				return changed
			}

			override val thisUser: ZUser
				get() = this@ZombicideActivity.thisUser

			override val connectedUsersInfo: List<ConnectedUser>
				get() = serverMgr?.connectionInfo ?: clientMgr?.connectedPlayers ?: emptyList()


			override suspend fun <T> waitForUser(expectedType: Class<T>): T? {
				vm.processingMove.postValue(false)
				zb.boardView.post { initMenu(uiMode, options) }
				organizeDialog?.refresh()
				return super.waitForUser(expectedType).also {
					zb.boardView.post {
						if (isGameRunning())
							initMenu(UIMode.NONE, null)
					}
				}
			}

			override suspend fun onQuestComplete() {
				super.onQuestComplete()
				runOnUiThread {
					stopGame()
					completeQuest(quest.quest)
					initHomeMenu()
				}
			}

			override fun setResult(result: Any?) {
				Log.i(TAG, "setResult $result")
				vm.processingMove.postValue(true)
				super.setResult(result)
			}

			override fun isGameRunning(): Boolean {
				return super.isGameRunning() || clientMgr != null
			}

			override suspend fun onCurrentCharacterUpdated(
				priorPlayer: ZPlayerName?,
				character: ZCharacter?
			) {
				super.onCurrentCharacterUpdated(priorPlayer, character)
				runOnUiThread { initGameMenu() }
			}

			override suspend fun onCurrentUserUpdated(userName: String, colorId: Int) {
				super.onCurrentUserUpdated(userName, colorId)
				runOnUiThread { initGameMenu() }
				serverMgr?.updateConnectionStatus()
			}

			override suspend fun onZombieStageMoveDone() {
				super.onZombieStageMoveDone()
				serverMgr?.broadcastUpdateGame()
			}

			override val isOrganizeEnabled: Boolean = true

			override suspend fun showOrganizeDialog(primary: ZPlayerName, secondary: ZPlayerName?, undos: Int) {
				Log.d(TAG, "showOrganizeDialog undos: $undos")
				runOnUiThread {
					if (organizeDialog?.isShowing != true) {
						organizeDialog?.dismiss()
						organizeDialog = OrganizeDialog(this@ZombicideActivity).also {
							it.show()
						}
					}
					organizeDialog?.viewModel?.undoPushes?.value = undos
					organizeDialog?.viewModel?.primaryCharacter?.value = board.getActor(primary.name) as ZCharacter?
					organizeDialog?.viewModel?.secondaryCharacter?.value = board.getActor(secondary?.name) as ZCharacter?
				}
			}

			override suspend fun closeOrganizeDialog() {
				runOnUiThread {
					organizeDialog?.dismiss()
					organizeDialog = null
				}
			}

			override suspend fun updateOrganize(character: ZCharacter, list: List<ZMove>, undos: Int): ZMove? {
				Log.d(TAG, "updateOrganize moves: ${list.joinToString(separator = "\n")}")
				runOnUiThread {
					organizeDialog?.viewModel?.allOptions?.value = list
				}
				return waitForUser(ZMove::class.java)
			}

			override fun playSound(sound: ZSound) {
				if (sound.id >= 0)
					soundPool.play(sound.id, 1f, 1f, 1, 0, 1f)
			}

			override fun focusOnMainMenu() {
				runOnUiThread {
					zb.listMenu.requestFocus()
				}
			}

			override fun focusOnBoard() {
				runOnUiThread {
					zb.boardView.requestFocus()
				}
			}

			override fun undo() {
				tryUndo()
			}
		}
		game.addUser(thisUser)
	}

	fun loadCharacters(playersSet: Collection<String>) {
		game.clearCharacters()
		val players = playersSet.map { ZPlayerName.valueOf(it) }
		for (player in players) {
			thisUser.addCharacter(game.addCharacter(player))
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
			thisUser.setCharacters(game.allCharacters)
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

	override fun onBackPressed() {
		if (game.isGameRunning() || clientMgr?.client?.isConnected == true) {
			newDialogBuilder().setTitle(R.string.popup_title_confirm)
				.setMessage(R.string.popup_message_confirm_exit)
				.setNegativeButton(R.string.popup_button_cancel, null)
				.setPositiveButton(R.string.popup_button_quit_game) { _, _ ->
					super.onBackPressed()
				}
				.show()
		}
	}

	var currentDialog: AlertDialog? = null
	val dialogStack = Stack<Dialog>()

	override fun newDialogBuilder(): AlertDialog.Builder {
		val b: AlertDialog.Builder = object :
			AlertDialog.Builder(ContextThemeWrapper(this, android.R.style.Theme_Holo_Dialog)) {
			override fun show(): AlertDialog {
				with(super.show()) {
					currentDialog = this
					setCanceledOnTouchOutside(false)
					return this
				}
			}
		}
		b.setOnCancelListener {
			currentDialog = null
			dialogStack.clear()
		}
		if (!BuildConfig.DEBUG) b.setCancelable(false)
		return b
	}

	fun pushDialog(): AlertDialog.Builder {
		currentDialog?.let {
			dialogStack.push(it)
		}
		return newDialogBuilder().setNeutralButton(R.string.popup_button_back) { _, _ ->
			dialogStack.takeIf { it.isNotEmpty() }?.pop()?.show()
		}
	}

	val savedDifficulty: ZDifficulty
		get() = ZDifficulty.valueOf(
			prefs.getString(
				PREF_DIFFICULTY_STRING,
				ZDifficulty.MEDIUM.name
			)!!
		)

	override fun onClick(v: View) {
		try {
			game.boardRenderer.setOverlay(null)
			when (v.id) {
				R.id.b_zoom -> {
					game.boardRenderer.toggleZoomType()
				}

				R.id.b_center -> {
					if (v.tag != null) {
						game.setResult(v.tag)
						clearKeypad()
					}
				}

				else -> {
					if (v.tag is ZMove) {
						game.setResult(v.tag)
						clearKeypad()
					}
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
		button.getTooltipText()?.takeIf { it.isNotEmpty() }?.let { ttText ->
			val d: Dialog = with(TooltippopupLayoutBinding.inflate(LayoutInflater.from(this))) {
				header.text = button.getLabel()
				text.text = ttText
				AlertDialog.Builder(this@ZombicideActivity, R.style.ZTooltipDialogTheme)
					.setView(root).create()
			}
			//val popup = View.inflate(this, R.layout.tooltippopup_layout, null)
			//(popup.findViewById<View>(R.id.header) as TextView).text = button.getLabel()
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
		get() = prefs.getStringSet(PREF_PLAYERS_STRING_SET, defaultPlayers)!!

	fun processMainMenuItem(item: MenuItem) {
		prefs.edit().putString("lastMenuItem", item.name).apply()
		when (item) {
			MenuItem.UNDO -> tryUndo()
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
				if (game.tryLoadFromFile(gameFile)) {
					boardRenderer.board = game.board
					log.debug(game.allCharacters.joinToString("\n") {
						it.name() + " " + ZUser.getColorName(it.colorId) + " invisible:" + it.isInvisible
					})
					if (game.allCharacters.map { it.colorId }.toSet().size > 1) {
						newDialogBuilder().setMessage("Resume in Multiplayer mode?")
							.setNegativeButton(R.string.popup_button_no) { _, _ ->
								thisUser.setCharacters(game.allCharacters)
								startGame()
							}.setPositiveButton(R.string.popup_button_yes) { _, _ ->
								log.debug("this user color: ${thisUser.getColorName()}")
								game.allCharacters.filter { it.colorId == thisUser.colorId }
									.apply {
										log.debug("Assigning ${joinToString { it.name() }} to host user")
										thisUser.setCharacters(this)
									}
								// make remaining characters invisible to show they are waiting to join
								game.allCharacters.filter { it.colorId != thisUser.colorId }
									.forEach {
										it.isInvisible = true
										it.isReady = false
									}
								// game thread will wait for all users to be ready
								startGame()
								p2pInit(P2PMode.SERVER)
							}.show()
					} else {
						thisUser.setCharacters(game.allCharacters)
						startGame()
					}
				}
			}
			MenuItem.QUIT -> if (client != null) {
				newDialogBuilder().setTitle(R.string.popup_title_confirm)
					.setMessage(R.string.popup_message_confirm_client_disconnect)
					.setNegativeButton(R.string.popup_button_cancel, null)
					.setPositiveButton(R.string.popup_button_disconnect) { _, _ ->
						object : SpinnerTask<Int>(this@ZombicideActivity) {
							override suspend fun doIt(args: Int?) {
								client?.disconnect("Quit Game")
							}

							override fun onCompleted() {
								shutdownMP()
								stopGame()
							}
						}.execute()
					}.show()
			} else if (server != null) {
				newDialogBuilder().setTitle(R.string.popup_title_confirm)
					.setMessage(R.string.popup_message_confirm_server_disconnect)
					.setNegativeButton(R.string.popup_button_cancel, null)
					.setNeutralButton(R.string.popup_button_stop) { _, _ ->
						stopGame()
					}
					.setPositiveButton(R.string.popup_button_disconnect) { _, _ ->
						object : SpinnerTask<Int>(this@ZombicideActivity) {
							override suspend fun doIt(args: Int?) {
								server?.stop()
							}

							override fun onCompleted() {
								stopGame()
								shutdownMP()
							}
						}.execute()
					}.show()
			} else {
				stopGame()
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
				//shutdownMP()
				showNewGameDialog()
			}
			MenuItem.JOIN_GAME -> {
				p2pInit(P2PMode.CLIENT)
			}

			MenuItem.SETUP_PLAYERS -> showNewGameDialogChoosePlayers(null)
			MenuItem.CONNECTIONS -> serverControl?.openConnections()
			MenuItem.CLEAR -> {
//				prefs.edit().remove("completedQuests").apply()
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
					override fun getCount(): Int = searchables.size

					override fun getItem(position: Int): Any = 0

					override fun getItemId(position: Int): Long = 0

					override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
						val convertView = convertView?:TextView(this@ZombicideActivity)
						(convertView as TextView).text = searchables[position].getLabel()
						return convertView
					}
				}
				newDialogBuilder().setTitle(R.string.popup_title_searchables).setView(lv).setNegativeButton("Close", null).show()
			}
			MenuItem.LOAD -> newDialogBuilder().setTitle(R.string.popup_title_choose).setItems(resources.getStringArray(R.array.popup_message_choose_game_types)
			) { _, which: Int ->
				when (which) {
					0 -> showLoadQuestDialog()
					1 -> showLoadSavedGameDialog()
				}
			}.setNegativeButton(R.string.popup_button_cancel, null).show()
			MenuItem.SAVE -> showSaveGameDialog()
			MenuItem.ASSIGN -> showAssignDialog()
			MenuItem.DIFFICULTY -> {
				newDialogBuilder().setTitle(
					getString(
						R.string.popup_title_difficulty,
						savedDifficulty
					)
				)
					.setItems(ZDifficulty.entries.map { it.name }
						.toTypedArray()) { dialog: DialogInterface?, which: Int ->
						val difficulty = ZDifficulty.entries[which]
						game.setDifficulty(difficulty)
						prefs.edit().putString(PREF_DIFFICULTY_STRING, difficulty.name).apply()
					}.setNegativeButton(R.string.popup_button_cancel, null).show()
			}
			MenuItem.SKILLS -> {
				showSkillsDialog2()
			}

			MenuItem.LEGEND -> {
				showLegendDialog()
			}

			MenuItem.ABOUT -> {
				showWelcomeDialog(false)
			}

			MenuItem.RULES -> {
				showRulesDialog()
			}

			MenuItem.EMAIL_REPORT -> {
				showEmailReportDialog()
			}

			MenuItem.CHOOSE_COLOR -> {
				showChooseColorDialog()
			}

			MenuItem.MINIMAP_MODE -> {
				prefs.edit()
					.putString(PREF_MINIMAP_MODE_STRING, boardRenderer.toggleDrawMinimap().name)
					.apply()
			}

			MenuItem.DEBUG_MENU -> {
				showDebugDialog()
			}

			MenuItem.DISCONNECT -> {
				p2pShutdown()
			}

			MenuItem.CHANGE_NAME -> {
				val et = EditText(this)
				et.hint = displayName
				newDialogBuilder()
					.setTitle("Change name")
					.setView(et)
					.setNegativeButton(R.string.popup_button_cancel, null)
					.setPositiveButton(R.string.popup_button_ok) { _, _ ->
						updateDisplayName(et.toString())
					}.show()
			}
		}
	}

	fun updateDisplayName(name: String) {
		if (name.isNotEmpty()) {
			prefs.edit().putString(PREF_P2P_NAME_STRING, name).commit()
			clientMgr?.client?.setProperty("name", name)
			thisUser.name = name
		}
	}

	fun tryUndo() {
		val isRunning = game.isGameRunning()
		stopGame()
		if (FileUtils.restoreFile(gameFile)) {
			synchronized(game.synchronizeLock) {
				game.tryLoadFromFile(gameFile)
			}
			game.refresh()
			serverMgr?.broadcastUpdateGame()
		}
		if (isRunning)
			startGame()
	}

	fun updateCharacters(quest: ZQuests) {
		loadCharacters(storedCharacters)
		game.trySaveToFile(gameFile)
		startGame()
	}

	fun showLoadQuestDialog() {
		newDialogBuilder().setTitle(R.string.popup_title_load_quest)
			.setItems(ZQuests.entries.map { it.name.prettify() }
				.toTypedArray()) { dialog: DialogInterface?, which: Int ->
				val q = ZQuests.entries[which]
				game.loadQuest(q)
				setupLoadedGame(q)
			}.setNegativeButton(R.string.popup_button_cancel, null).show()
	}

	fun setupLoadedGame(q: ZQuests) {
		serverMgr?.takeIf { it.server.isConnected }?.let { mgr ->
			mgr.getConnectedUsers().forEach { (conn, user) ->
				conn.sendCommand(mgr.newLoadQuest(q))
			}
			mgr.showChooser()
		} ?: updateCharacters(q)
	}

	fun showSaveGameDialog() {
		SaveGameDialog(this, MAX_SAVES)
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
						setupLoadedGame(game.quest.quest)
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
			return try {
				Reflector.deserializeFromFile(savesMapFile)
			} catch (e: Exception) {
				e.printStackTrace()
				LinkedHashMap()
			}
		}

	fun pushGameState(file: Boolean) {
		serverMgr?.broadcastUpdateGame()
		organizeDialog?.refresh()
		if (file) {
			log.debug("Backing up ... ")
			FileUtils.backupFile(gameFile, 32)
			game.trySaveToFile(gameFile)
		}
	}

	fun saveGame() {
		val buf = StringBuffer()
		buf.append(game.quest.quest.displayName)
		buf.append(" ").append(game.getDifficulty().name).append(" ")
		buf.append(game.allCharacters.joinToString(",") { it.name() })
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
		if (clientMgr?.client?.isConnected == true) {
			object : CLSendCommandSpinnerTask(this@ZombicideActivity) {

				override fun onColorOptions(colorIdOptions: List<Int>) {
					runOnUiThread {
						showColorChooser(colorIdOptions) {
							clientMgr?.setColorId(it)
						}
					}
					release()
				}

				override fun onSuccess() {
				}
			}.execute(clientMgr!!.newColorPickerPressed())
		} else {
			showColorChooser(IntArray(ZUser.USER_COLORS.size) { it }.toList()) { id ->
				prefs.edit().putInt(PREF_COLOR_ID_INT, id).apply()
				game.setUserColorId(thisUser, id)
				clientMgr?.setColorId(id)
				game.refresh()
			}
		}
	}

	fun showColorChooser(options: List<Int>, callback: (Int) -> Unit) {
		val listView = ListView(this)
		var colorId = thisUser.colorId
		listView.adapter = object : BaseAdapter() {
			override fun getCount(): Int = options.size

			override fun getItem(position: Int): Any = options[position]

			override fun getItemId(position: Int): Long = options[position].toLong()

			override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
				val view = (convertView as CheckBox?) ?: CheckBox(this@ZombicideActivity)
				view.setOnCheckedChangeListener(null)
				val id = options[position]
				view.isChecked = colorId == id
				view.isEnabled = !view.isChecked
				view.text = ZUser.getColorName(id)
				view.setTextColor(ZUser.USER_COLORS[id].toARGB())
				view.setOnCheckedChangeListener { _, isChecked ->
					if (isChecked) {
						colorId = id
						notifyDataSetChanged()
					}
				}
				return view
			}
		}
		newDialogBuilder()
			.setTitle("Choose Color")
			.setView(listView)
			.setNegativeButton(R.string.popup_button_cancel, null)
			.setPositiveButton(R.string.popup_button_ok) { _, _ ->
				callback(colorId)
			}.show()
	}

	fun showDebugDialog() {

		class Option(val text: String, val checked: Boolean, val toggleCb: () -> Unit)

		val options = arrayOf(
			Option("TILES", boardRenderer.drawTiles) {
				boardRenderer.toggleDrawTiles()
			},
			Option("TEXT", boardRenderer.drawDebugText) {
				boardRenderer.toggleDrawDebugText()
			},
			Option("TOWERS", boardRenderer.drawTowersHighlighted) {
				boardRenderer.toggleDrawTowersHighlighted()
			},
			Option("CENTER", boardRenderer.drawScreenCenter) {
				boardRenderer.toggleDrawScreenCenter()
			},
			Option("CLICKABLES", boardRenderer.drawClickable) {
				boardRenderer.toggleDrawClickables()
			},
			Option("ZOMBIE PATHS", boardRenderer.drawZombiePaths) {
				boardRenderer.toggleDrawZoombiePaths()
			},
			Option("RANGE", boardRenderer.drawRangedAccessibility) {
				boardRenderer.toggleDrawRangedAccessibility()
			}
		)
		val listView = ListView(this)
		listView.adapter = object : BaseAdapter() {
			override fun getCount(): Int = options.size
			override fun getItem(position: Int): Any = options[position]
			override fun getItemId(position: Int): Long = 0
			override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
				val button: ToggleButton = (convertView as ToggleButton?)
					?: ToggleButton(this@ZombicideActivity)
				with(options[position]) {
					button.textOn = "$text On"
					button.textOff = "$text Off"
					button.isChecked = checked
					button.setOnCheckedChangeListener { _, _ -> toggleCb() }
				}
				return button
			}
		}
		newDialogBuilder()
			.setTitle("DEBUG OPTIONS")
			.setView(listView)
			.setNegativeButton("Done") { _, _ ->
				boardRenderer.redraw()
			}.show()
	}

	val displayName: String
		get() = prefs.getString(PREF_P2P_NAME_STRING, null) ?: ZUser.USER_COLOR_NAMES[colorId]

	val colorId: Int
		get() = prefs.getInt(PREF_COLOR_ID_INT, 0)

	override fun onP2PReady() {
		val p2pName = prefs.getString(PREF_P2P_NAME_STRING, null)
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
					prefs.edit().putString(PREF_P2P_NAME_STRING, txt).apply()
					p2pStart()
				}
			}
		} else {
			p2pStart()
		}
	}

	override fun p2pStart() {
		if (!game.isGameRunning()) {
			game.clearCharacters()
			game.reload()
			game.refresh()
		}
		super.p2pStart()
	}

	var clientControl: P2PClient? = null
	override fun onP2PClient(p2pClient: P2PClient) {
		clientControl = p2pClient
		clientMgr = ZClientMgr(this, game, p2pClient.getClient(), thisUser)
	}

	var serverControl: P2PServer? = null
	override fun onP2PServer(p2pServer: P2PServer) {
		serverControl = p2pServer
		serverMgr = ZServerMgr(this, game, 2, p2pServer.getServer()).also {
			if (!game.isGameRunning())
				it.showChooser()
		}
//		thisUser.name = prefs.getString(PREF_P2P_NAME, p2pServer.server.name)!!
	}

	override fun onP2PShutdown() {
		clientMgr?.shutdown()
		serverMgr?.shutdown()
		clientMgr = null
		clientControl = null
		serverMgr = null
		serverControl = null
		game.server = null
		game.client = null
		initHomeMenu()
	}
/*
	fun showSetupPlayersDialog() {
		val saved = storedCharacters
		val assignments: MutableList<Assignee> = ArrayList()
		for (c in charLocks) {
			val a = Assignee(c)
			if (saved.contains(a.name.name)) {
				a.checked = true
				a.color = thisUser.colorId
				a.userName = thisUser.name
				a.isAssingedToMe = true
			}
			assignments.add(a)
		}
		object : CharacterChooserDialogSP(this@ZombicideActivity, assignments, 6) {
			override fun onAssigneeChecked(a: Assignee, checked: Boolean) {
				a.checked = checked
				if (a.checked) {
					a.color = thisUser.colorId
					a.userName = thisUser.name
					a.isAssingedToMe = true
				} else {
					a.color = -1
					a.userName = "??"
					a.isAssingedToMe = false
				}
			}

			override fun onStart() {
				game.clearCharacters()
				val players: MutableSet<String> = HashSet()
				for (a in assignments) {
					if (a.checked) {
						val c = game.addCharacter(a.name)
						thisUser.addCharacter(c)
						players.add(a.name.name)
					}
				}
				prefs.edit().putStringSet(PREF_PLAYERS_STRING_SET, players).apply()
				game.refresh()
			}
		}
	}*/

	fun showGameMenuDialog() {
		val options = MenuItem.entries.filter { it.isMenuButton() }
		newDialogBuilder()
			.setTitle("Main Menu")
			.setNegativeButton(R.string.popup_button_cancel, null)
			.setItems(options.map { it.name.prettify() }.toTypedArray()) { d, i ->
				processMainMenuItem(options[i])
			}.show()
	}

	fun showLegendDialog() {
		val legend = arrayOf(
			Pair(
				R.drawable.legend_chars,
				"CHARACTERS\nChoose your characters. Some are unlockable. Players can operate in any order but must execute all of their actions before switching to another player."
			),
			Pair(
				R.drawable.legend_gamepad,
				"GAMEPAD\nUse gamepad to perform common actions.\nLH / RH - Use Item in Left/Right hand.\nO - Toggle active player.\nZM - Zoom in/out."
			),
			Pair(
				R.drawable.legend_obj,
				"OBJECTIVES\nObjectives give player EXP, unlock doors, reveal special items and other things related to the Quest."
			),
			Pair(
				R.drawable.legend_vault,
				"VAULTS\nVaults are very handy. You can find special loot or drop loot to be pickup up later. You can also close zombies in or out of the vault. Also they can sometimes be shortcuts across the map. The only limitation is you cannot fire ranged weapons or magic into the vault from the outside or fire outside of vault from inside of it."
			),
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
				val convertView = convertView
					?: View.inflate(this@ZombicideActivity, R.layout.legend_list_item, null)
				val ivImage = convertView.findViewById<ImageView>(R.id.iv_image)
				val tvDesc = convertView.findViewById<TextView>(R.id.tv_description)
				with(legend[position]) {
					ivImage.setImageResource(first)
					tvDesc.text = second
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
			.setItems(resources.getStringArray(if (BuildConfig.DEBUG) R.array.popup_message_choose_game_version_debug else R.array.popup_message_choose_game_version)) { _, which ->
				when (which) {
					0 -> showNewGameChooseQuestDialog(
						questsBlackPlague(),
						stats.getCompletedQuests()
					)

					1 -> if (isWolfburgUnlocked) showNewGameChooseQuestDialog(
						questsWolfsburg(),
						stats.getCompletedQuests()
					) else newDialogBuilder().setTitle(R.string.popup_title_wolflocked)
						.setMessage(R.string.popup_message_unlock_wolfburg)
						.setPositiveButton(R.string.popup_button_ok) { _, _ -> showNewGameDialog() }
						.show()

					2 -> showNewGameChooseQuestDialog(ZQuests.entries, HashSet(ZQuests.entries))
				}
			}.setNegativeButton(R.string.popup_button_cancel, null).show()
	}

	fun showNewGameChooseQuestDialog(allQuests: List<ZQuests>, playable: Set<ZQuests>) {
		NewGameChooseQuestDialog(this, allQuests, playable.toMutableSet())
	}

	fun showNewGameDialogChooseDifficulty(quest: ZQuests) {
		pushDialog().setTitle(
			getString(
				R.string.popup_title_quest,
				quest.ordinal,
				quest.displayName
			)
		)
			.setItems(ZDifficulty.entries.map { it.name }.toTypedArray()) { _, which: Int ->
				val difficulty = ZDifficulty.entries[which]
				prefs.edit().putString(PREF_DIFFICULTY_STRING, difficulty.name).apply()
				game.setDifficulty(difficulty)
				showChooseGameModeDialog(quest)
			}.setNegativeButton(R.string.popup_button_cancel, null).show()
	}

	fun showChooseGameModeDialog(quest: ZQuests) {
		val modes = arrayOf(
			"Single Player",
			"Multi-player Host"
		)
		pushDialog().setTitle("Choose Mode")
			.setItems(modes) { _, which: Int ->
				when (which) {
					0 -> showNewGameDialogChoosePlayers(quest)
					1 -> {
						game.loadQuest(quest)
						p2pInit(P2PMode.SERVER)
					}
				}
			}.setNegativeButton(R.string.popup_button_cancel, null).show()
	}

	open class CharLock(val player: ZPlayerName, val unlockMessageId: Int) {
		open val isUnlocked = true
	}

	fun showNewGameDialogChoosePlayers(quest: ZQuests?) {
		object : CharacterChooserDialogSP(this@ZombicideActivity, quest) {
			override fun onStarted() {
				prefs.edit().putStringSet(PREF_PLAYERS_STRING_SET, selectedPlayers).apply()
				quest?.let {
					game.loadQuest(it)
					loadCharacters(storedCharacters)
					game.trySaveToFile(gameFile)
					startGame()
				}
			}
		}
	}

	fun showSkillsDialog2() {
		SkillsDialog(this)
	}

	// TODO: Make this more organized. Should be able to order by character, danger level or all POSSIBLE
	fun showSkillsDialog() {
		val sorted = ZSkill.entries.toMutableList()
			.sortedWith { o1: ZSkill, o2: ZSkill -> o1.name.compareTo(o2.name) }
		newDialogBuilder().setTitle(R.string.popup_title_skills)
			.setItems(sorted.map { it.name.prettify() }.toTypedArray()) { dialog: DialogInterface?, which: Int ->
				val skill = sorted[which]
				newDialogBuilder().setTitle(skill.getLabel())
					.setMessage(skill.description)
					.setNegativeButton(R.string.popup_button_cancel, null)
					.setPositiveButton(R.string.popup_button_back) { _, _ -> showSkillsDialog() }
					.show()
			}.setNegativeButton(R.string.popup_button_cancel, null).show()
	}

	fun initHomeMenu() {
		runOnUiThread {
			vm.playing.postValue(false)
			val buttons: MutableList<View> = ArrayList()
			val focussed = enumValueOfOrNull<MenuItem>(prefs.getString("lastMenuItem", MenuItem.RESUME.name))
			var selectedPosition = 0
			MenuItem.entries.filter { it.isHomeButton(this@ZombicideActivity) }.forEachIndexed { index, menuItem ->
				buttons.add(build(this, menuItem, menuItem.isEnabled(this)).also {
					if (menuItem == focussed)
						selectedPosition = index
				})
			}
			initMenuItems(buttons, selectedPosition)
		}
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
						when (ZDir.entries[move.integer!!]) {
							ZDir.NORTH -> {
								zb.bUp.tag = move
								zb.bUp.isEnabled = true
							}

							ZDir.SOUTH -> {
								zb.bDown.tag = move
								zb.bDown.isEnabled = true
							}

							ZDir.EAST -> {
								zb.bRight.tag = move
								zb.bRight.isEnabled = true
							}
							ZDir.WEST -> {
								zb.bLeft.tag = move
								zb.bLeft.isEnabled = true
							}
							ZDir.ASCEND,
							ZDir.DESCEND -> {
								zb.bVault.tag = move
								zb.bVault.isEnabled = true
							}
							else -> Unit
						}
						it.remove()
					}
					ZMoveType.USE_SLOT -> {
						when (move.fromSlot) {
							ZEquipSlot.LEFT_HAND -> {
								zb.bUseleft.tag = move
								zb.bUseleft.isEnabled = true
								it.remove()
							}

							ZEquipSlot.RIGHT_HAND -> {
								zb.bUseright.tag = move
								zb.bUseright.isEnabled = true
								it.remove()
							}

							else -> Unit
						}
					}

					ZMoveType.SWITCH_ACTIVE_CHARACTER -> {
						zb.bCenter.tag = move
						zb.bCenter.isEnabled = true
						it.remove()
					}

					else -> Unit
				}
			}
		}
		game.repeatableMove?.takeIf { game.repeatableMovePlayer != null && game.repeatableMovePlayer == game.currentCharacter?.type }
			?.let {
				zb.bRepeat.visibility = View.VISIBLE
				zb.bRepeat.text = it.action?.label ?: "Repeat"
				zb.bRepeat.tag = it
			}
	}

	fun clearKeypad() {
		zb.bUseleft.isEnabled = false
		zb.bUseright.isEnabled = false
		zb.bUp.isEnabled = false
		zb.bDown.isEnabled = false
		zb.bLeft.isEnabled = false
		zb.bRight.isEnabled = false
		zb.bVault.isEnabled = false
		zb.bCenter.tag = null
		zb.bUndo.isEnabled = false
		zb.bRepeat.visibility = View.GONE
	}

	fun initMenu(mode: UIMode, options: List<Any>?) {
		log.debug("current colorId: ${game.currentUserColorId} thisUserColorId: ${thisUser.colorId}")
		val buttons: MutableList<View> = ArrayList()
		clearKeypad()
		options?.toMutableList()?.also {
			initKeypad(it)
			when (mode) {
				UIMode.PICK_CHARACTER -> {
					zb.bCenter.tag = it[0]
					for (e: IButton in it as List<IButton>) {
						buttons.add(build(this, e, e.isEnabled()))
					}
					buttons.add(ListSeparator(this))
				}

				UIMode.PICK_MENU -> {
					for (e: IButton in it as List<IButton>) {
						buttons.add(build(this, e, e.isEnabled()))
					}
					buttons.add(ListSeparator(this))
				}

				else -> Unit
			}
		}
		MenuItem.entries.filter { it.isGameButton(this@ZombicideActivity) }.forEach { i ->
			buttons.add(build(this, i, i.isEnabled(this)))
		}
		zb.bUndo.isEnabled = canUndo()
		initMenuItems(buttons)
	}

	fun canUndo(): Boolean = FileUtils.hasBackupFile(gameFile)

	fun initMenuItems(buttons: List<View>, selectedPosition: Int = 0) {
		launchIn(Dispatchers.Main) {
			vm.listAdapter.update(buttons)
			zb.listMenu.setSelection(selectedPosition)
		}
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
				val player = ZPlayerName.entries[position]
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
				return ZPlayerName.entries.size
			}
		}
		newDialogBuilder().setTitle(R.string.popup_title_assign).setView(recyclerView)
			.setNegativeButton(R.string.popup_button_cancel, null)
			.setPositiveButton(R.string.popup_button_ok) { dialog: DialogInterface?, which: Int ->
				Log.d(TAG, "Selected players: $selectedPlayers")
				prefs.edit().putStringSet(PREF_PLAYERS_STRING_SET, selectedPlayers).apply()
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
				EmailTask(
					this@ZombicideActivity,
					message.editableText.toString() //, new String(pw)
				).execute(gameFile)
			}.show()
	}

	fun showRulesDialog() {
		val rules = game.rules.deepCopy()
		object : ConfigDialogBuilder(this) {
			override fun onApplyRules() {
				game.rules.copyFrom(rules)
			}
		}.show(rules, true)
	}

	internal class EmailTask(private val context: CCActivityBase, private val message: String) :
		AsyncTask<File, Void, Exception>() {
		private lateinit var progress: Dialog
		override fun onPreExecute() {
			progress = ProgressDialog.show(
				context,
				null,
				context.getString(R.string.popup_message_sending_report)
			)
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
			progress.dismiss()
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
		const val PREF_P2P_NAME_STRING = "p2pname"
		const val PREF_PLAYERS_STRING_SET = "players"
		const val PREF_COLOR_ID_INT = "colorId"
		const val PREF_COMPLETED_QUESTS = "completedQuests"
		const val PREF_DIFFICULTY_STRING = "difficulty"
		const val PREF_MINIMAP_MODE_STRING = "miniMapMode"
	}
}