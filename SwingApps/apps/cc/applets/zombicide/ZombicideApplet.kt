package cc.applets.zombicide

import cc.game.zombicide.ZActor
import cc.game.zombicide.ZCharacter
import cc.game.zombicide.ZDifficulty
import cc.game.zombicide.ZGame
import cc.game.zombicide.ZPlayerName
import cc.game.zombicide.ZQuests
import cc.game.zombicide.ZUser
import cc.game.zombicide.anims.OverlayTextAnimation
import cc.game.zombicide.p2p.CommAssign
import cc.game.zombicide.p2p.CommAssignImpl
import cc.game.zombicide.p2p.IZClient
import cc.game.zombicide.p2p.IZServer
import cc.game.zombicide.p2p.impl.ZClient
import cc.game.zombicide.p2p.impl.ZServer
import cc.game.zombicide.toName
import cc.game.zombicide.ui.UIZBoardRenderer
import cc.game.zombicide.ui.UIZCharacterRenderer
import cc.game.zombicide.ui.UIZUser
import cc.game.zombicide.ui.UIZombicide
import cc.game.zombicide.ui.UIZombicide.UIMode
import cc.game.zombicide.ui.ZSound
import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Utils
import cc.lib.logger.Logger
import cc.lib.logger.LoggerFactory
import cc.lib.swing.AWTApplet
import cc.lib.swing.AWTButton
import cc.lib.swing.AWTDialog
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTLabel
import cc.lib.swing.AWTNumberPicker
import cc.lib.swing.AWTPanel
import cc.lib.swing.AWTRulesPopup
import cc.lib.swing.AWTScope
import cc.lib.swing.AWTSoundMgr
import cc.lib.swing.AWTStringPicker
import cc.lib.swing.AWTToggleButton
import cc.lib.swing.AWTWrapLabel
import cc.lib.timer.DebugTimer
import cc.lib.timer.GlobalTimer
import cc.lib.ui.IButton
import cc.lib.utils.KFileUtils.backupFile
import cc.lib.utils.KFileUtils.getOrCreateSettingsDirectory
import cc.lib.utils.KFileUtils.restore
import cc.lib.utils.doIf
import cc.lib.utils.launchIn
import cc.lib.utils.takeIfInstance
import cc.lib.utils.toInetAddress
import cc.lib.utils.toString
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.FlowLayout
import java.awt.GridBagLayout
import java.awt.Rectangle
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.net.InetAddress
import java.net.MalformedURLException
import java.net.URL
import java.util.EnumMap
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JScrollPane
import javax.swing.JSeparator
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager

open class ZombicideApplet(val id: Int, val appletName: String) : AWTApplet(), ActionListener {
	@Throws(MalformedURLException::class)
	override fun getAbsoluteURL(imagePath: String): URL {
		return URL("http://mac-book.local/~chriscaron/Zombicide/$imagePath")
	}

	val uiUser: ZUser by lazy {
		UIZUser(System.getenv("USER") ?: "User", getIntProperty("COLOR", 1))
	}

	lateinit var game: UIZombicide

//	val p2pClient : IZClient by lazy {
//		ZClient(game, uiUser, CoroutineScope(SupervisorJob() + Dispatchers.Swing))
//	}

//	val p2pServer : IZServer by lazy {
//		TODO()
//	}

	var menu: AWTPanel = object : AWTPanel() {
		override fun add(comp: Component): Component {
			comp.minimumSize = Dimension(140, 40)
			comp.maximumSize = Dimension(140, 400)
			(comp as JComponent).alignmentX = LEFT_ALIGNMENT
			return super.add(comp)
		}
	}

	val settings by lazy {
		ZombicideApplet::class.java.getOrCreateSettingsDirectory()
	}
	val gameFile by lazy {
		File(settings, "savegame$id.txt")
	}
	val rulesFile by lazy {
		File(settings, "rules$id.txt")
	}

	var menuContainer = AWTPanel()
	private lateinit var boardComp: BoardComponent
	private lateinit var charComp: CharacterComponent

	init {
		instance = this
		AWTSoundMgr.addSearchPath("zombicideandroid/src/main/res/raw")
		ZSound.SWORD_SLASH.id = AWTSoundMgr.loadAudio("sword_swing1.wav")
	}

	fun onAllImagesLoaded() {
		val boardRenderer: UIZBoardRenderer = object : UIZBoardRenderer(boardComp) {
			override fun drawActor(g: AGraphics, actor: ZActor<*>, outline: GColor?) {
				if (actor.isAlive && actor.outlineImageId > 0) {
					// for AWT to need to render the outline in white fist otherwise the tinting looks messed up
					g.drawImage(actor.outlineImageId, actor.getRect())
				}
				super.drawActor(g, actor, outline)
			}
		}
		val charRenderer = object : UIZCharacterRenderer(charComp) {
			override fun scrollToTop() {
				charComp.scrollRectToVisible(Rectangle(0, 0, 1, 1))
			}
		}
		game = object : UIZombicide(charRenderer, boardRenderer) {
			override suspend fun runGame(): Boolean {
				var changed = false
				try {
					changed = super.runGame()
					charComp.repaint()
					boardComp.repaint()
				} catch (e: Exception) {
					e.printStackTrace()
					stopGameThread()
					initHomeMenu()
					boardRenderer.setOverlay("Error: " + e.message)
				}
				if (isGameOver) {
					stopGameThread()
					initHomeMenu()
				}
				return changed
			}

			override suspend fun <T> waitForUser(expectedType: Class<T>): T? {
				SwingUtilities.invokeLater {
					initMenu(uiMode, options)
					boardComp.requestFocus()
				}
				return super.waitForUser(expectedType)
			}

			override fun setResult(result: Any?) {
				if (result != null && isGameRunning()) {
					gameFile.takeIf { it.exists() }?.backupFile(100)
					game.trySaveToFile(gameFile)
				}
				super.setResult(result)
				boardComp.requestFocus()
			}

			override val thisUser: ZUser
				get() = uiUser

			override fun focusOnMainMenu() {
				menuContainer.grabFocus()
			}

			override fun focusOnBoard() {
				boardComp.grabFocus()
			}

			override fun undo() {
				val running = isGameRunning()
				if (running) stopGameThread()
				synchronized(synchronizeLock) {
					tryLoadFromFile(gameFile)
				}
				refresh()
				if (running)
					startGameThread()
				else
					initHomeMenu()
				super.undo()
			}

			override fun clOpenAssignmentsDialog(numCharacters: Int, colorId: Int, assignments: List<CommAssign>) {
				SwingUtilities.invokeLater {
					showMPAssignMenu(numCharacters, colorId, assignments)
				}
			}

			override fun onDisconnected(reason: String) {
				stopGameThread()
				setResult(null)
				initHomeMenu()
			}

			override fun playSound(sound: ZSound, times: Int) {
				AWTSoundMgr.playSound(sound.id, times - 1)
			}
		}
		game.rules.tryLoadFromFile(rulesFile)
		initIntro()
	}

	@OptIn(InternalCoroutinesApi::class)
	fun initIntro() {
		with(boardComp.renderer) {
			val job: Job = CoroutineScope(Dispatchers.Main).async {
				addOverlay(OverlayTextAnimation("Z O M B I C I D E", 1))
				delay(2000)
				addOverlay(OverlayTextAnimation("B L A C K   P L A G U E", 2))
				delay(4000)
			}
			val listener = object : MouseAdapter() {
				override fun mouseClicked(p0: MouseEvent) {
					job.cancel()
					boardComp.removeMouseListener(this)
				}
			}
			boardComp.addMouseListener(listener)
			job.invokeOnCompletion(true) {
				boardComp.renderer.stopAnimations()
				boardComp.removeMouseListener(listener)
				initHome()
			}
		}

	}

	fun initHome() {
		try {
			game.loadQuest(ZQuests.valueOf(getStringProperty("quest", ZQuests.Tutorial.name)))
		} catch (e: Exception) {
			e.printStackTrace()
			game.loadQuest(ZQuests.Tutorial)
		}
		//uiUser.setColor(game.board, frame.getIntProperty("COLOR", 0))
		val players = getEnumListProperty(
			"players",
			ZPlayerName::class.java,
			Utils.toList(ZPlayerName.Baldric, ZPlayerName.Clovis)
		)
		for (pl in players) {
			game.addCharacter(pl).let {
				uiUser.addCharacter(it)
			}
		}
		game.setUsers(uiUser)
		game.setDifficulty(ZDifficulty.valueOf(getStringProperty("difficulty", ZDifficulty.MEDIUM.name)))
		initHomeMenu()
	}


	enum class MenuItem {
		START,
		HOST,
		JOIN,
		RESUME,
		QUIT,
		CANCEL,
		LOAD,
		ASSIGN,
		SUMMARY,
		COLOR,
		DIFFICULTY,
		UNDO,
		OBJECTIVES,
		RULES,
		DISCONNECT;

		fun isHomeButton(instance: ZombicideApplet): Boolean = when (this) {
			LOAD, START, HOST, JOIN, COLOR, ASSIGN, DIFFICULTY, UNDO, RULES -> true
			RESUME -> instance.gameFile.exists()

			else -> false
		}

		fun isClientButton(instance: ZombicideApplet): Boolean = when (this) {
			COLOR, UNDO, RULES, SUMMARY, OBJECTIVES, DISCONNECT -> true
			else -> false
		}

		fun isHostButton(instance: ZombicideApplet): Boolean = when (this) {
			COLOR, UNDO, RULES, SUMMARY, OBJECTIVES, DISCONNECT -> true
			else -> false
		}

		fun isSPGameButton(): Boolean = when (this) {
			CANCEL, SUMMARY, OBJECTIVES, DIFFICULTY, UNDO, QUIT -> true
			else -> false
		}
	}

	fun initHomeMenu() {
		val items = MenuItem.values().filter { it.isHomeButton(this) }
		setMenuItems(items)
		if (game.questInitialized)
			frame.title = game.quest.name
	}

	@Synchronized
	fun setMenuItems(items: List<MenuItem>) {
		if (SwingUtilities.isEventDispatchThread()) {
			menu.removeAll()
			for (i in items) {
				menu.add(AWTButton(i.name, this))
			}
			menuContainer.revalidate()
		} else {
			EventQueue.invokeLater { setMenuItems(items) }
		}
	}

	override fun actionPerformed(e: ActionEvent) {
		val item = MenuItem.valueOf(e.actionCommand)
		game.boardRenderer.setOverlay(null)
		when (item) {
			MenuItem.START -> {
				game.reload()
				game.startGameThread()
			}

			MenuItem.HOST -> {
				launchIn {
					getDisplayName().await()?.let { displayName ->
						game.currentUserName = displayName
						val numPlayersPicker =
							AWTNumberPicker.Builder()
								.setLabel("PLAYERS")
								.setMin(2).setMax(4)
								.setValue(frame.getIntProperty("hostNumPlayers", 3))
								.build() { newValue ->
									frame.setProperty("hostNumPlayers", newValue)
								}
						val numCharsPicker =
							AWTNumberPicker.Builder()
								.setLabel("CHARACTERS EACH")
								.setMin(1).setMax(3)
								.setValue(frame.getIntProperty("hostNumChars", 2))
								.build() { newValue ->
									frame.setProperty("hostNumChars", newValue)
								}
						val colorsPicker =
							AWTStringPicker.Builder(
								ZUser.getAvailableColorNames()
							).setLabel("COLOR").setValueIndex(game.thisUser.colorId).build()
						AWTDialog(frame, "HOST GAME").also { popup ->
							popup.setBody(AWTPanel(2, 2).also {
								it.add(numPlayersPicker)
								it.add(numCharsPicker)
								it.add(colorsPicker)
							})
							popup.setFooter(AWTPanel(FlowLayout()).also {
								it.add(AWTButton("START") {
									launch {
										game.server =
											ZServer(game, displayName, numPlayersPicker.value - 1, numCharsPicker.value).also { server ->
												server.start()
												popup.closePopup()
												SwingUtilities.invokeLater {
													showMPAssignMenu(numCharsPicker.value, game.thisUser.colorId, emptyList())
												}
											}
									}
								})
								it.add(AWTButton("CANCEL") {
									popup.closePopup()
								})
							})
						}.showPopup()
					}
				}
			}

			MenuItem.JOIN -> {
				launchIn {
					getDisplayName().await()?.let { displayName ->
						game.currentUserName = displayName
						game.client = ZClient(game, uiUser).also { client ->
							client.startDiscovery()
							HostChooser(client).open().await()?.let {
								try {
									client.connect(it)
									launchIn(Dispatchers.Swing) {
										frame.showSpinnerDialog("STAND BY", true).await().doIf(true) {
											game.disconnect("User Cancelled")
										}
									}
								} catch (e: Throwable) {
									frame.showError(e)
								}
							} ?: game.disconnect("User Cancelled")
						}
					}
				}
			}

			MenuItem.DISCONNECT -> {
				game.disconnect("User Disconnected")
			}

			MenuItem.RESUME -> {
				launchIn {
					if (game.tryLoadFromFile(gameFile)) {
						uiUser.setCharacters(game.board.getAllCharacters())
						game.startGameThread()
						game.refresh()
					}
				}
			}

			MenuItem.QUIT -> {
				game.stopGameThread()
				game.setResult(null)
				initHomeMenu()
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
			MenuItem.LOAD -> {
				menu.removeAll()
				for (q in ZQuests.values()) {
					menu.add(AWTButton(q) {
						game.loadQuest(q)
						setStringProperty("quest", q.name)
						boardComp.repaint()
						initHomeMenu()
					})
				}
				menu.add(AWTButton(MenuItem.CANCEL.name, this))
				menuContainer.revalidate()
			}
			MenuItem.ASSIGN -> {
				showSPAssignMenu()
			}

			MenuItem.DIFFICULTY -> {
				JOptionPane.showInputDialog(
					this, "Set Difficulty", "DIFFICULTY", JOptionPane.PLAIN_MESSAGE, null,
					ZDifficulty.values(), game.getDifficulty()
				)?.takeIfInstance<ZDifficulty>()?.let { difficulty ->
					game.setDifficulty(difficulty)
					setStringProperty("difficulty", difficulty.name)
				}
			}

			MenuItem.UNDO -> if (gameFile.restore()) {
				menu.removeAll()
				game.undo()
			}

			MenuItem.COLOR -> {
				val choiceIndex = frame.showItemChooserDialog(
					"Choose Color", null,
					ZUser.getColorName(frame.getIntProperty("COLOR", 0)),
					*ZUser.getAvailableColorNames().toTypedArray()
				)
				if (choiceIndex >= 0) {
					val color = ZUser.getAvailableColorIds()[choiceIndex]
					frame.setProperty("COLOR", color)
					game.setUserColorId(uiUser, color)
				}
			}

			MenuItem.RULES -> {
				AWTRulesPopup(frame, game.rules, rulesFile).show("EDIT RULES", true);
			}
			//else -> log.error("Unhandled action: " + e.actionCommand)
		}
	}

	inner class HostChooser(client: ZClient) : AWTDialog(frame, "HOSTS") {

		private val hostPanel = AWTPanel(3, 1)
		private val completed = CompletableDeferred<InetAddress?>()

		init {
			preferredSize = Dimension(400, 100)
			dialogScope.launch {
				client.discoveredHosts.onEach { hosts ->
					hostPanel.removeAll()
					val colors = arrayOf(Color.LIGHT_GRAY, Color.DARK_GRAY)
					var color = 0
					hosts.values.filter { it.discoverable }.forEach { h ->
						hostPanel.add(AWTButton("${h.hostName} : ${h.serverName} \n ${h.description}") {
							completed.complete(h.hostAddress.toInetAddress())
							closePopup()
						}.also {
							it.background = colors[(color++) % 2]
						})
					}
					hostPanel.revalidate()
				}.collect()
			}
			minimumSize = Dimension(800, 200)
			menu.layout = BoxLayout(menu, BoxLayout.Y_AXIS)
			AWTPanel(BorderLayout()).also { root ->
				setBody(root)
				root.addCenter(AWTPanel(GridBagLayout()).also {
					it.add(hostPanel)
				})
				root.addBottom(AWTPanel(FlowLayout()).also {
					it.add(AWTButton("DISCONNECT") {
						closePopup()
					})
				})
			}
		}

		fun open(): CompletableDeferred<InetAddress?> {
			super.showPopup()
			return completed
		}

		override fun onWindowClosing() {
			completed.complete(null)
		}
	}

	fun getDisplayName(): CompletableDeferred<String?> {
		val savedName = frame.getStringProperty("displayName", "")
		if (savedName.isNotBlank()) {
			return CompletableDeferred(savedName)
		}
		val completedName = CompletableDeferred<String?>()
		JOptionPane.showInputDialog(this, "Provide Display Name", "")?.let { displayName ->
			if (displayName.isNotBlank()) {
				frame.setProperty("displayName", displayName)
				completedName.complete(displayName)
			} else {
				completedName.complete(null)
			}
		}
		return completedName
	}

	inner class ConnectionsDialog(server: ZServer, val numConnections: Int) : AWTDialog(frame, "CONNECTIONS") {

		private val connectionsPanel = AWTPanel(numConnections, 1)
		private val started = CompletableDeferred<Boolean>()
		private val startButton = AWTButton("START") {
			started.complete(true)
			closePopup()
		}

		init {
			setBody(connectionsPanel)
			setFooter(AWTPanel(FlowLayout()).also {
				it.add(startButton)
				it.add(AWTButton("DISCONNECT") {
					game.disconnect("User Disconnected")
					closePopup()
					started.complete(false)
				})
			})
			dialogScope.launch {
				server.connectionsFlow.onEach { connections ->
					connectionsPanel.removeAll()
					val colors = arrayOf(Color.DARK_GRAY, Color.LIGHT_GRAY)
					var colorIdx = 0
					connections.forEach {
						connectionsPanel.add(AWTLabel(it.displayName).also {
							it.background = colors[(colorIdx++ % 2)]
						})
					}
					startButton.isEnabled = server.connections.size >= numConnections
					connectionsPanel.revalidate()
				}.collect()
			}
		}

		fun open(): CompletableDeferred<Boolean> {
			showPopup()
			return started
		}
	}

	fun showSPAssignMenu() {
		val currentAssignments = getEnumListProperty("players", ZPlayerName::class.java, listOf()).map {
			it to game.thisUser.colorId
		}.toList()
		val buttons = showAssignMenu(currentAssignments, 8, game.thisUser.colorId)
		menu.add(AWTButton("KEEP") { _: ActionEvent ->
			game.clearCharacters()
			for ((key, value) in buttons) {
				if (value.isSelected) {
					game.addCharacter(key).also {
						uiUser.addCharacter(it)
					}
				}
			}
			game.reload()
			setEnumListProperty("players", buttons.keys.filter { buttons[it]?.isSelected == true })//Utils.filter(buttons.keys, Utils.Filter { `object`: ZPlayerName -> buttons[`object`]?.isSelected == true }))
			initHomeMenu()
			boardComp.repaint()
		})
		menu.add(AWTButton(MenuItem.CANCEL.name, this))
		menuContainer.revalidate()
	}

	fun showMPAssignMenu(numChars: Int, colorId: Int, assignments: List<CommAssign>) {
		frame.closeSpinner()
		val buttons = showAssignMenu(emptyList(), numChars, colorId)
		assignments.forEach {
			buttons[it.name]?.let { button ->
				button.colorId = it.colorId
				button.isSelected = it.selected
			}
		}

		val started = MutableStateFlow(false)

		fun start() {
			game.client?.userStarted(colorId) ?: game.server?.userStarted(colorId) ?: run {
				game.disconnect("No Connection")
				return
			}

			started.value = true
//				frame.showSpinnerDialog("Waiting for everyone", true).await().doIf(true) {
//					game.disconnect("User got bored")
//				}

		}

		val startButton = AWTButton("START") { _: ActionEvent ->
			launchIn {
				start()
			}
		}


		val startButtonScope = AWTScope(startButton)

		started.onEach {
			if (it) {
				startButton.isEnabled = false
				buttons.values.forEach {
					it.isEnabled = false
				}
				boardComp.renderer.setOverlay("Waiting for other players")
			}
		}.produceIn(startButtonScope)

		choicesMade.onEach {
			startButton.isEnabled = it == numChars
		}.produceIn(startButtonScope)

		menu.add(startButton)
		menu.add(AWTButton(MenuItem.DISCONNECT.name, this))
		menuContainer.revalidate()
	}

	open inner class ZCharacterToggleButton(
		val player: ZPlayerName,
		var colorId: Int,
		selected: Boolean
	) : AWTToggleButton(
		player.name
	), IZClient.Listener, IZServer.Listener {
		init {
			background = Color.LIGHT_GRAY
			game.client?.addWeakListener(this)
			game.server?.addWeakListener(this)
			isSelected = selected
			addMouseListener(object : MouseAdapter() {
				override fun mouseEntered(e: MouseEvent) {
					if (::game.isInitialized)
						game.boardRenderer.setOverlay(player)
				}

				override fun mouseExited(e: MouseEvent) {
					if (::game.isInitialized)
						game.boardRenderer.setOverlay(null)
				}
			})
		}

		override fun onToggle(on: Boolean) {
			isSelected = on
			game.server?.assign(CommAssignImpl(player, game.thisUser.name, colorId, on))
			game.client?.sendTCP(CommAssignImpl(player, game.thisUser.name, colorId, on))
		}

		@Synchronized
		override fun setSelected(selected: Boolean) {
			if (selected) {
				foreground = Color(ZUser.getUserColor(colorId).toRGB())
				isOpaque = true
			} else {
				foreground = null
				isOpaque = false
			}
			super.setSelected(selected)
		}

		override fun onAssignment(assign: CommAssign) {
			SwingUtilities.invokeLater {
				charComp.renderer.addMessage("User %s has %s %s", assign.userName, assign.selected.toString("selected", "unselected"), assign.name.name)
				if (assign.name == player) {
					log.debug("onAssignment: ${player}, ${assign.colorId.toName()}, ${assign.selected}")
					colorId = assign.colorId
					isSelected = assign.selected

					if (isSelected) {
						game.addCharacter(player).let {
							it.colorId = colorId
						}
					} else {
						game.removeCharacter(player)
					}
					game.refresh()
				}
			}
		}
	}

	val choicesMade = MutableStateFlow(0)

	fun showAssignMenu(currentSelections: List<Pair<ZPlayerName, Int>>, maxChoices: Int, colorId: Int): Map<ZPlayerName, ZCharacterToggleButton> {
		log.debug("showAssignMenu ${colorId.toName()}, currentSelections: ${currentSelections.joinToString()}")

		menu.removeAll()
		val buttons: MutableMap<ZPlayerName, ZCharacterToggleButton> = EnumMap(ZPlayerName::class.java)
		fun updateEnabled() {
			// cases:
			// buttons with colors not our own are disabled
			// button with colors that are our own are enabled
			// unselected buttons disabled if maxChoices it hit
			val count = buttons.count { it.value.isSelected && it.value.colorId == colorId }
			val maxxed = count >= maxChoices
			choicesMade.value = count

			buttons.values.forEach {
				if (it.isSelected) {
					it.isEnabled = it.colorId == colorId
				} else {
					it.isEnabled = !maxxed
				}
			}
		}

		for (player in ZPlayerName.entries) {
			val (color, selected) = currentSelections.firstOrNull { it.first == player }?.let {
				Pair(it.second, true)
			} ?: Pair(colorId, false)
			buttons[player] = object : ZCharacterToggleButton(player, color, selected) {
				override fun onToggle(on: Boolean) {
					if (on)
						this.colorId = colorId
					super.onToggle(on)
				}

				override fun setSelected(selected: Boolean) {
					super.setSelected(selected)
					log.debug("setSelected $player, ${this.colorId.toName()}, $selected")
					updateEnabled()
				}
			}.also { btn ->
				menu.add(btn)
			}
		}
		updateEnabled()
		return buttons
	}

	override fun initApp() {
		ToolTipManager.sharedInstance().dismissDelay = 30 * 1000
		ToolTipManager.sharedInstance().initialDelay = 0
		// For applets:all fonts are: [Arial, Dialog, DialogInput, Monospaced, SansSerif, Serif]
		layout = BorderLayout()
		val charScrollContainer = JScrollPane()
		val menuScrollContainer = JScrollPane()
		charScrollContainer.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
		menuScrollContainer.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
		charScrollContainer.viewport.add(CharacterComponent().also {
			charComp = it
		})
		charScrollContainer.preferredSize = Dimension(400, 200)
		charScrollContainer.maximumSize = Dimension(10000, 200)
		add(charScrollContainer, BorderLayout.SOUTH)
		menu.layout = BoxLayout(menu, BoxLayout.Y_AXIS)
		menuContainer.layout = GridBagLayout()
		menuScrollContainer.preferredSize = Dimension(150, 400)
		menu.alignmentX = LEFT_ALIGNMENT
		menuContainer.addMouseListener(object : MouseAdapter() {
			override fun mouseEntered(e: MouseEvent) {
				if (::game.isInitialized) {
					game.boardRenderer.setHighlightActor(null)
					game.characterRenderer.redraw()
				}
			}
		})
		menuContainer.minimumSize = Dimension(150, 400)
		menuContainer.add(menu)
		menuScrollContainer.viewport.add(menuContainer)
		add(menuScrollContainer, BorderLayout.LINE_START)
		add(BoardComponent().also { boardComp = it }, BorderLayout.CENTER)
		frame.addWindowListener(boardComp)
	}

	internal inner class ZButton(obj: IButton) : AWTButton(obj) {
		var obj: Any

		init {
			this.isFocusable = false
			this.obj = obj
			//            log.debug("created button for type " + obj.getClass());
			if (obj is ZCharacter) {
				addMouseListener(object : MouseAdapter() {
					override fun mouseEntered(e: MouseEvent) {
						launchIn {
							boardComp.renderer.setHighlightActor(obj)
							charComp.renderer.actorInfo = obj
						}
					}
				})
			} else {
				addMouseListener(object : MouseAdapter() {
					override fun mouseEntered(p0: MouseEvent?) {
						game.currentCharacter?.let {
							charComp.renderer.actorInfo = it
						}
					}
				})
			}
		}

		override fun onAction() {
			SwingUtilities.invokeLater {
				game.setResult(obj)
			}
		}
	}

	fun initMenu(mode: UIMode, _options: List<*>) {

		menu.removeAll()
		val options = _options.toMutableList()
		boardComp.initKeysPresses(options)
		when (mode) {
			UIMode.NONE -> {}
			UIMode.PICK_MENU,
			UIMode.PICK_CHARACTER -> { // }, UIMode.PICK_SUBMENU -> {
				for (o in options) {
					menu.add(ZButton(o as IButton))
				}
			}
			UIMode.PICK_ZONE, UIMode.PICK_SPAWN,
			UIMode.PICK_ZOMBIE, UIMode.PICK_DOOR -> menu.add(AWTWrapLabel("Pick an element on the board"))
		}
		val sep: JComponent = JSeparator()
		//sep.setMaximumSize(new Dimension(140, 32));
		//Dimension d = sep.getPreferredSize();
		//d.height = 32;
		//sep.setPreferredSize(d);
		menu.add(sep, null)
		if (game.client?.connected == true) {
			MenuItem.values().filter { it.isClientButton(instance) }.forEach {
				menu.add(AWTButton(it.name, this))
			}
		} else if (game.server != null) {
			MenuItem.values().filter { it.isHostButton(instance) }.forEach {
				menu.add(AWTButton(it.name, this))
			}

		} else {
			MenuItem.values().filter { it.isSPGameButton() }.forEach {
				menu.add(AWTButton(it.name, this))
			}
		}
		menuContainer.revalidate()
	}

	companion object {
		val log: Logger = LoggerFactory.getLogger(ZombicideApplet::class.java)
		lateinit var instance: ZombicideApplet

		@JvmStatic
		fun main(args: Array<String>) {
			Utils.setDebugEnabled()
			GlobalTimer = DebugTimer(GlobalTimer)
			val id = args.firstOrNull()?.toIntOrNull() ?: 0
			val name = args.getOrNull(1) ?: "Zombicide Applet $id"
			ZGame.DEBUG = true
			frame = object : AWTFrame() {
				override fun onWindowClosing() {
					instance.game.disconnect("Window Closing")
				}
			}
			instance = object : ZombicideApplet(id, name) {
				override fun <T : Enum<T>> getEnumListProperty(property: String, clazz: Class<T>, defaultList: List<T>): List<T> {
					return frame.getEnumListProperty(property, clazz, defaultList)
				}

				override fun getStringProperty(property: String, defaultValue: String): String {
					return frame.getStringProperty(property, defaultValue)
				}

				override fun setStringProperty(s: String, v: String) {
					frame.setProperty(s, v)
				}

				override fun <T : Enum<T>> setEnumListProperty(s: String, l: Collection<T>) {
					frame.setEnumListProperty(s, l)
				}

				override fun setIntProperty(s: String, value: Int) {
					frame.setProperty(s, value.toString())
				}

				override fun getIntProperty(s: String, defaultValue: Int): Int {
					return frame.getIntProperty(s, defaultValue)
				}

				override fun getFloatProperty(s: String, defaultValue: Float): Float {
					return frame.getFloatProperty(s, defaultValue)
				}

				override fun setFloatProperty(s: String, value: Float) {
					frame.setProperty(s, value)
				}
			}
			frame.setPropertiesFile(File(instance.settings, "application$id.properties"))
			frame.add(instance)
			instance.initApp()
			instance.start()
			if (!frame.restoreFromProperties())
				frame.centerToScreen(800, 600)
		}

		lateinit var frame: AWTFrame
	}
}