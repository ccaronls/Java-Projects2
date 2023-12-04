package cc.applets.zombicide

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Utils
import cc.lib.logger.Logger
import cc.lib.logger.LoggerFactory
import cc.lib.swing.*
import cc.lib.ui.IButton
import cc.lib.utils.FileUtils
import cc.lib.utils.takeIfInstance
import cc.lib.zombicide.*
import cc.lib.zombicide.ui.UIZBoardRenderer
import cc.lib.zombicide.ui.UIZCharacterRenderer
import cc.lib.zombicide.ui.UIZUser
import cc.lib.zombicide.ui.UIZombicide
import cc.lib.zombicide.ui.UIZombicide.UIMode
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import javax.swing.*
import javax.swing.Timer

open class ZombicideApplet : AWTApplet(), ActionListener {
	@Throws(MalformedURLException::class)
	override fun getAbsoluteURL(imagePath: String): URL {
		return URL("http://mac-book.local/~chriscaron/Zombicide/$imagePath")
	}

	val uiUser: ZUser by lazy {
		UIZUser(System.getenv("USER") ?: "User")
	}

	lateinit var game: UIZombicide
	var menu: AWTPanel = object : AWTPanel() {
		override fun add(comp: Component): Component {
			comp.minimumSize = Dimension(140, 40)
			comp.maximumSize = Dimension(140, 400)
			(comp as JComponent).alignmentX = LEFT_ALIGNMENT
			return super.add(comp)
		}
	}

	var menuContainer = AWTPanel()
	private lateinit var boardComp: BoardComponent
	private lateinit var charComp: CharacterComponent
	var gameFile: File? = null

	init {
		instance = this
	}

	fun onAllImagesLoaded() {
		val renderer: UIZBoardRenderer = object : UIZBoardRenderer(boardComp) {
			override fun drawActor(g: AGraphics, actor: ZActor, outline: GColor?) {
				if (actor.isAlive && actor.outlineImageId > 0) {
					// for AWT to need to render the outline in white fist otherwise the tinting looks messed up
					g.drawImage(actor.outlineImageId, actor.getRect())
				}
				super.drawActor(g, actor, outline)
			}
		}
		game = object : UIZombicide(UIZCharacterRenderer(charComp), renderer) {
			override fun runGame(): Boolean {
				var changed = false
				try {
					changed = super.runGame()
					charComp.repaint()
					boardComp.repaint()
					if (isGameRunning() && changed && gameFile != null) {
						FileUtils.backupFile(gameFile, 100)
						game.trySaveToFile(gameFile)
					}
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

			override fun <T> waitForUser(expectedType: Class<T>): T? {
				initMenu(uiMode, options)
				return super.waitForUser(expectedType)
			}

			override fun setResult(result: Any?) {
				super.setResult(result)
				boardComp.requestFocus()
			}

			override val thisUser: ZUser
				get() = uiUser
		}
		try {
			game.loadQuest(ZQuests.valueOf(getStringProperty("quest", ZQuests.Tutorial.name)))
		} catch (e: Exception) {
			e.printStackTrace()
			game.loadQuest(ZQuests.Tutorial)
		}
		uiUser.setColor(game.board, frame.getIntProperty("COLOR", 0))
		val players = getEnumListProperty("players", ZPlayerName::class.java, Utils.toList(ZPlayerName.Baldric, ZPlayerName.Clovis))
		for (pl in players) {
			game.addCharacter(pl)
			uiUser.addCharacter(game.addCharacter(pl))
		}
		game.setUsers(uiUser)
		game.setDifficulty(ZDifficulty.valueOf(getStringProperty("difficulty", ZDifficulty.MEDIUM.name)))
		initHomeMenu()
	}

	enum class MenuItem {
		START,
		RESUME,
		QUIT,
		CANCEL,
		LOAD,
		ASSIGN,
		SUMMARY,
		COLOR,
		DIFFICULTY,
		UNDO,
		OBJECTIVES;

		fun isHomeButton(instance: ZombicideApplet): Boolean = when (this) {
			LOAD, START, COLOR, ASSIGN, DIFFICULTY, UNDO -> true
			RESUME -> instance.gameFile?.exists() == true
			else -> false
		}
	}

	fun initHomeMenu() {
		val items = MenuItem.values().filter { it.isHomeButton(this) }
		setMenuItems(items)
		frame.title = "Zombicide: " + game.quest.name
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
			MenuItem.RESUME -> if (game.tryLoadFromFile(gameFile)) {
				uiUser.setCharacters(game.board.getAllCharacters())
				game.startGameThread()
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
				menu.removeAll()
				val buttons: MutableMap<ZPlayerName, AWTToggleButton> = EnumMap(ZPlayerName::class.java)
				for (player in ZPlayerName.values()) {
					val btn: AWTToggleButton = object : AWTToggleButton(player.name) {
						override fun actionPerformed(e: ActionEvent) {
							// override this since parent class has method that causes our layout to resize badly
							onToggle(isSelected)
						}
					}
					buttons[player] = btn
					menu.add(btn)
					btn.addMouseListener(object : MouseListener {
						override fun mouseClicked(e: MouseEvent) {}
						override fun mousePressed(e: MouseEvent) {}
						override fun mouseReleased(e: MouseEvent) {}
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
			MenuItem.DIFFICULTY -> {
				val difficulty = JOptionPane.showInputDialog(this, "Set Difficulty", "DIFFICULTY", JOptionPane.PLAIN_MESSAGE, null,
					ZDifficulty.values(), game.getDifficulty()) as ZDifficulty
				game.setDifficulty(difficulty)
				setStringProperty("difficulty", difficulty.name)
			}
			MenuItem.UNDO -> if (FileUtils.restoreFile(gameFile)) {
				val running = game.isGameRunning()
				game.stopGameThread()
				game.tryLoadFromFile(gameFile)
				game.refresh()
				if (running) game.startGameThread()
			}
			MenuItem.COLOR -> {
				val color = frame.showItemChooserDialog("Choose Color", null, ZUser.USER_COLOR_NAMES[frame.getIntProperty("COLOR", 0)],
					*ZUser.USER_COLOR_NAMES
				)
				if (color >= 0) {
					frame.setProperty("COLOR", color)
					uiUser.setColor(game.board, color)
				}
			}
			//else -> log.error("Unhandled action: " + e.actionCommand)
		}
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
		charScrollContainer.viewport.add(CharacterComponent().also { charComp = it })
		charScrollContainer.preferredSize = Dimension(400, 200)
		charScrollContainer.maximumSize = Dimension(10000, 200)
		add(charScrollContainer, BorderLayout.SOUTH)
		menu.layout = BoxLayout(menu, BoxLayout.Y_AXIS)
		menuContainer.layout = GridBagLayout()
		menuScrollContainer.preferredSize = Dimension(150, 400)
		menu.alignmentX = LEFT_ALIGNMENT
		menuContainer.addMouseListener(object : MouseListener {
			override fun mouseClicked(e: MouseEvent) {}
			override fun mousePressed(e: MouseEvent) {}
			override fun mouseReleased(e: MouseEvent) {}
			override fun mouseEntered(e: MouseEvent) {
				if (::game.isInitialized) {
					game.boardRenderer.setHighlightActor(null)
					game.characterRenderer.redraw()
				}
			}

			override fun mouseExited(e: MouseEvent) {}
		})
		menuContainer.minimumSize = Dimension(150, 400)
		menuContainer.add(menu)
		menuScrollContainer.viewport.add(menuContainer)
		add(menuScrollContainer, BorderLayout.LINE_START)
		add(BoardComponent().also { boardComp = it }, BorderLayout.CENTER)
		frame.addWindowListener(boardComp)
		charComp.addMouseListener(object : MouseListener {
			override fun mouseClicked(e: MouseEvent) {}
			override fun mousePressed(e: MouseEvent) {}
			override fun mouseReleased(e: MouseEvent) {}
			override fun mouseEntered(e: MouseEvent) {
				game.currentCharacter?.let {
					setCharacterSkillsOverlay(it)
				} ?: boardComp.renderer.highlightedActor?.takeIfInstance<ZCharacter>()?.let {
					setCharacterSkillsOverlay(it)
				}
			}

			override fun mouseExited(e: MouseEvent) {
				boardComp.renderer.setOverlay(null)
			}
		})
	}

	fun setCharacterSkillsOverlay(c: ZCharacter) {
		boardComp.renderer.setOverlay(c.getSkillsTable())
	}

	internal inner class ZButton(obj: IButton) : AWTButton(obj) {
		var obj: Any

		init {
			this.obj = obj
			//            log.debug("created button for type " + obj.getClass());
			if (obj is ZCharacter) {
				addMouseListener(object : MouseListener {
					var t = Timer(1, null)
					override fun mouseClicked(e: MouseEvent) {}
					override fun mousePressed(e: MouseEvent) {}
					override fun mouseReleased(e: MouseEvent) {}
					override fun mouseEntered(e: MouseEvent) {
						boardComp.renderer.setHighlightActor(obj)
						charComp.renderer.actorInfo = obj
						//t.stop()
						//t = AWTUtils.postDelayed(4000, false) {
						//	boardComp.renderer.setOverlay(obj.getAllSkillsTableExpanded())
						//}
						//charComp.redraw()
					}

					override fun mouseExited(e: MouseEvent) {
						t.stop()
						boardComp.renderer.setOverlay(null)
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
			UIMode.PICK_ZONE, UIMode.PICK_SPAWN, UIMode.PICK_ZOMBIE, UIMode.PICK_DOOR -> menu.add(AWTWrapLabel("Pick an element on the board"))
		}
		val sep: JComponent = JSeparator()
		//sep.setMaximumSize(new Dimension(140, 32));
		//Dimension d = sep.getPreferredSize();
		//d.height = 32;
		//sep.setPreferredSize(d);
		menu.add(sep, null)
		menu.add(AWTButton(MenuItem.CANCEL.name, this))
		menu.add(AWTButton(MenuItem.SUMMARY.name, this))
		menu.add(AWTButton(MenuItem.OBJECTIVES.name, this))
		menu.add(AWTButton(MenuItem.DIFFICULTY.name, this))
		menu.add(AWTButton(MenuItem.UNDO.name, this))
		menu.add(AWTButton(MenuItem.QUIT.name, this))
		menuContainer.revalidate()
	}

	companion object {
		val log: Logger = LoggerFactory.getLogger(ZombicideApplet::class.java)
		lateinit var instance: ZombicideApplet

		@JvmStatic
		fun main(args: Array<String>) {
			Utils.setDebugEnabled()
			ZGame.DEBUG = true
			frame = AWTFrame("Zombicide")
			instance = object : ZombicideApplet() {
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
			}
			val settings = FileUtils.getOrCreateSettingsDirectory(ZombicideApplet::class.java)
			frame.setPropertiesFile(File(settings, "application.properties"))
			instance.gameFile = File(settings, "savegame.txt")
			frame.add(instance)
			instance.initApp()
			instance.start()
			if (!frame.restoreFromProperties()) frame.centerToScreen(800, 600)
		}

		lateinit var frame: AWTFrame
	}
}