package cc.applets.robotron

import cc.game.superrobotron.MAX_PLAYERS
import cc.game.superrobotron.POWERUP_NUM_TYPES
import cc.game.superrobotron.RoboClient
import cc.game.superrobotron.RoboConnectionStatus
import cc.game.superrobotron.RoboServer
import cc.game.superrobotron.Robotron
import cc.game.superrobotron.RobotronRemote
import cc.game.superrobotron.TARGET_FRAMES_PER_SEC
import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.logger.LoggerFactory.LogLevel
import cc.lib.math.Vector2D
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTKeyboardAnimationApplet
import cc.lib.utils.KFileUtils.getOrCreateSettingsDirectory
import cc.lib.utils.noDupesMapOf
import cc.lib.utils.random
import cc.lib.utils.setRandomSeed
import cc.lib.utils.toOnOffStr
import java.awt.Container
import java.awt.Font
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.JOptionPane
import javax.swing.SwingWorker


class RobotronApplet(frameId: Int, serverName: String) : AWTKeyboardAnimationApplet() {

	var frame: AWTFrame

	init {
		frame = object : AWTFrame("Robotron $frameId") {
			override fun onWindowClosing() {
				disconnect()
			}
		}
		frame.add(this)
		if (!frame.loadFromFile(File(settingsDir, "robo$frameId.properties")))
			frame.centerToScreen(800, 600)
		init()
		start()
		setTargetFPS(TARGET_FRAMES_PER_SEC)
		grabFocus()
	}

	/**
	 * When true, will use non-socketed fake client / server from LocalHost in same process
	 * When false will use GameClient/Server socket connection is separate process(es)
	 */
	val USE_LOCAL_NETWORK = false

	val log = LoggerFactory.getLogger("$frameId", RobotronApplet::class.java)

	override fun getDefaultFont(): Font {
		return Font("Arial", Font.BOLD, 12)
	}

	private lateinit var robotron: RobotronRemote

	private var loadingMax = 0
	private var loadingProgress = 0


	override fun doInitialization() {
		Utils.setDebugEnabled()
		LoggerFactory.logLevel = LogLevel.DEBUG
		setRandomSeed(0)
		robotron = object : RobotronRemote() {
			override val imageKey: Int by lazy {
				G.loadImage("robotron/key.gif", GColor.BLACK)
			}
			override val imageLogo: Int by lazy {
				G.loadImage("robotron/logo.gif", GColor.BLACK)
			}
			override val animJaws: IntArray by lazy {
				G.loadImageCells("robotron/jaws.gif", 32, 32, 8, 9, true, GColor.BLACK)
			}
			override val animLava: IntArray by lazy {
				G.loadImageCells("robotron/lavapit.gif", 32, 32, 8, 25, true, GColor.BLACK)
			}
			override val animPeople: Array<IntArray> by lazy {
				arrayOf(
					G.loadImageCells("robotron/people.gif", 32, 32, 4, 16, true, GColor.BLACK),
					G.loadImageCells("robotron/people2.gif", 32, 32, 4, 16, true, GColor.BLACK),
					G.loadImageCells("robotron/people3.gif", 32, 32, 4, 16, true, GColor.BLACK)
				)
			}

			override fun initGraphics(g: AGraphics) {
				super.initGraphics(g)
				setDimension(g.viewportWidth, g.viewportHeight)
			}

			override val clock: Long
				get() = System.currentTimeMillis()

			override var high_score: Int = 0
				set(value) {
					field = value
					client ?: run {
						if (field != value) {
							frame.setProperty("highScore", value)
						}
					}
				}
		}

		loadingMax = 5 // number of lazy initializations needed
		robotron.high_score = frame.getIntProperty("highScore", 0)
	}

	override fun graphicsCreated(g: AGraphics) {
		object : SwingWorker<Int, Int>() {
			override fun doInBackground(): Int {
				robotron.imageKey
				loadingProgress++
				robotron.imageLogo
				loadingProgress++
				robotron.animJaws
				loadingProgress++
				robotron.animLava
				loadingProgress++
				robotron.animPeople
				loadingProgress = loadingMax
				return 0
			}
		}.execute()
	}

	fun rootFrame(container: Container = parent): AWTFrame? {
		return (container as? AWTFrame) ?: rootFrame(container.parent)
	}

	val requireRootFrame: AWTFrame
		get() = requireNotNull(rootFrame())

	fun showDisplayNameDialog(onDoneCb: (String) -> Unit) {
		robotron.player.displayName = requireRootFrame.getStringProperty("displayName", "")
		if (robotron.player.displayName.isNotBlank()) {
			onDoneCb(robotron.player.displayName)
			return
		}
		JOptionPane.showInputDialog(this, "Confirm Display Name", robotron.player.displayName)?.let { displayName ->
			if (displayName.isNotBlank()) {
				robotron.player.displayName = displayName
				requireRootFrame.setProperty("displayName", displayName)
				onDoneCb(robotron.player.displayName)
			}
		}
	}

	fun showGetServerDialog(onDoneCb: (serverName: String) -> Unit) {
		val server = requireRootFrame.getStringProperty("server", "127.0.0.1")
		JOptionPane.showInputDialog(this, "Server Address", server)?.let { name ->
			requireRootFrame.setProperty("server", name)
			onDoneCb(name)
		}
	}

	fun initHost() = showDisplayNameDialog { displayName ->
		if (USE_LOCAL_NETWORK) {
			TODO()
		} else robotron.server = RoboServer(robotron, displayName).also {
			robotron.player.status = RoboConnectionStatus.HOST
			robotron.player.displayName = displayName
			it.start("Robo Applet")
		}
	}

	private var savedId = 0

	fun joinHost() = showDisplayNameDialog { displayName ->
		try {
			if (USE_LOCAL_NETWORK) {
				TODO()
			} else showGetServerDialog { server ->
				robotron.client = RoboClient(robotron, displayName, savedId).also {
					it.connectBlocking(server)
					// connected!
					savedId = it.id
				}
			}

		} catch (e: Exception) {
			robotron.player.displayName = ""
			throw e
		}

	}

	fun changeDisplayName() =
		JOptionPane.showInputDialog(this, "Confirm Display Name", robotron.player.displayName)?.let { displayName ->
			robotron.player.displayName = displayName
			requireRootFrame.setProperty("displayName", displayName)
		}

	fun disconnect() {
		robotron.disconnect()
	}

	fun drawLoading(g: AGraphics) {
		g.clearScreen(GColor.BLACK)
		g.color = GColor.RED
		val dim = g.viewport.scaleBy(.5, .1)
		val rect = GRectangle(dim).setCenter(g.viewport.scaleBy(.5).toVector())
		g.drawRect(rect)
		rect.scaleDimension(loadingProgress.toFloat() / loadingMax.toFloat(), 1)
		g.drawFilledRect(rect)
	}

	@Synchronized
	override fun drawFrame(g: AGraphics) {
		if (loadingProgress < loadingMax && loadingMax > 0) {
			drawLoading(g)
			return
		}
		robotron.drawGame(g)
		g.color = GColor.YELLOW
		if (showHelp) {
			val str = "HELP\n" + helpMap.values.joinToString("\n") {
				"${it.first}    - ${it.second()}"
			}
			g.drawJustifiedStringOnBackground(20f, screenHeight / 2, Justify.LEFT, Justify.CENTER, str, GColor.TRANSLUSCENT_BLACK, 5f)
		}

		if (robotron.players.size > 1 || robotron.server != null) {
			var str = ""
			robotron.players.forEachIndexed { idx, pl ->
				if (idx == robotron.this_player)
					str += "-> "
				str += "${pl.displayName}:${pl.status.code}\n"
			}
			g.drawJustifiedString(screenWidth - 10, screenHeight / 2, Justify.RIGHT, Justify.CENTER, str)
		}


		/*
		robotron.server?.let { svr ->
			val str = "CONNECTIONS:\n" +
				svr.roboConnections.joinToString("\n") { it -> "${it.clientId} : ${it.connected}" }
			g.drawJustifiedString(screenWidth - 20, (screenHeight / 2), Justify.RIGHT, Justify.CENTER, str)
		}
		robotron.client?.let { clnt ->
			val str = "CONNECTED: ${clnt.connected}\n" + robotron.players.joinToString("\n") { "${it.displayName}" }
			g.drawJustifiedString(screenWidth - 20, (screenHeight / 2), Justify.RIGHT, Justify.CENTER, str)
		}*/
	}

	override fun onError(t: Throwable) {
		super.onError(t)
		disconnect()
	}

	override fun onDimensionsChanged(g: AGraphics, width: Int, height: Int) {
		robotron.initGraphics(g)
		robotron.setDimension(width, height)
	}

	var showHelp = false

	// bit flags too key_down_flag
	private val KEY_FLAG_LEFT = 1
	private val KEY_FLAG_RIGHT = 2
	private val KEY_FLAG_DOWN = 4
	private val KEY_FLAG_UP = 8
	private var key_down_flag = 0
	private var playerDx = 0
	private var playerDy = 0

	val helpMap by lazy {
		noDupesMapOf(
			KeyEvent.VK_K to Triple('K', { "Add Snake Missle" }) {
				with(robotron) {
					addSnakeMissile(Vector2D.random(screen_x..screen_x + screen_width, screen_y..screen_y + screen_height))
				}
			},
			KeyEvent.VK_P to Triple('P', { "Add powerup" }) {
				with(robotron) {
					addPowerup(
						Vector2D.random(screen_x..screen_x + screen_width, screen_y..screen_y + screen_height),
						random(0 until POWERUP_NUM_TYPES)
					)
				}
			},
			KeyEvent.VK_Q to Triple('Q', { "Quit to Home" }) { robotron.setGameStateIntro() },
			KeyEvent.VK_COMMA to Triple('<', { "Previous Level" }) { robotron.prevLevel() },
			KeyEvent.VK_PERIOD to Triple('>', { "Next Level" }) { robotron.nextLevel() },
			KeyEvent.VK_V to Triple('V', { "Toggle Visibility ${Robotron.GAME_VISIBILITY.toOnOffStr()}" }) {
				Robotron.GAME_VISIBILITY = !Robotron.GAME_VISIBILITY
			},
			//KeyEvent.VK_G to Triple('G', { "Game Over" }) { robotron.gameOver() },
			/*
			KeyEvent.VK_B to Triple('B', { "Add Player" }) {
				robotron.client ?: run {
					robotron.players.addOrNull()?.let {
						robotron.initNewPlayer(it)
					}
				}
			}*/
			/*,
			KeyEvent.VK_G to Triple('G', { "Add Remote Player" }) {
				if (USE_LOCAL_NETWORK) {
					spawn(frameId + 1)
				} else {
					try {
						val javaHome = System.getProperty("java.home")
						val javaBin = "$javaHome/bin/java"
						val classpath = System.getProperty("java.class.path")
						val className: String = RobotronApplet::class.java.name
						val builder = ProcessBuilder(javaBin, "-cp", classpath, className, "${frameId + 1}")
						val process = builder.start()
						println("Spawned process with PID: " + process.pid())
					} catch (e: IOException) {
						e.printStackTrace()
					}
				}
			}*/
			KeyEvent.VK_N to Triple('N', { "Toggle current player ${robotron.this_player}" }) {
				robotron.client ?: run {
					robotron.this_player = (robotron.this_player + 1) % robotron.players.size
				}
			},
			KeyEvent.VK_H to Triple('H', { "Host" }) { initHost() },
			KeyEvent.VK_J to Triple('J', { "Join" }) { joinHost() },
			KeyEvent.VK_L to Triple('L', { "Disconnect" }) { disconnect() },
			KeyEvent.VK_O to Triple('O', { "Display Name" }) { changeDisplayName() },
			KeyEvent.VK_B to Triple('B', { "Barrier Version ${robotron.drawPlayerBarrierElectircWallVersion}" }) { robotron.toggleDrawPlayerBarrierElectircWall() }
		)
	}

	override fun reportKeyRepeats(): Boolean = false

	override fun onKeyPressed(evt: KeyEvent) {
		when (evt.keyCode) {
			KeyEvent.VK_RIGHT, KeyEvent.VK_D -> {
				key_down_flag = key_down_flag or KEY_FLAG_RIGHT
				playerDx = 1
			}

			KeyEvent.VK_LEFT, KeyEvent.VK_A -> {
				key_down_flag = key_down_flag or KEY_FLAG_LEFT
				playerDx = -1
			}

			KeyEvent.VK_DOWN, KeyEvent.VK_S -> {
				key_down_flag = key_down_flag or KEY_FLAG_DOWN
				playerDy = 1
			}

			KeyEvent.VK_UP, KeyEvent.VK_W -> {
				key_down_flag = key_down_flag or KEY_FLAG_UP
				playerDy = -1
			}

			else -> {
				try {
					helpMap[evt.keyCode]?.let {
						robotron.setToastMsg(it.second())
						it.third()
					} ?: run {
						showHelp = true
					}
				} catch (e: Exception) {
					e.printStackTrace()
					requireRootFrame.showMessageDialog("ERROR", e.message
						?: e.javaClass.simpleName, AWTFrame.MessageIconType.ERROR)
				}
			}
		}

		if (Utils.isDebugEnabled()) {
			val index = evt.keyChar - '1'
			if (index >= 0 && index < Robotron.Debug.values().size) {
				val enabled = robotron.isDebugEnabled(Robotron.Debug.values()[index])
				robotron.setDebugEnabled(Robotron.Debug.values()[index], !enabled)
			}
		}
		robotron.setPlayerMovement(Vector2D(playerDx, playerDy))
		evt.consume()
	}

	override fun onKeyReleased(evt: KeyEvent) {
		when (evt.keyCode) {
			KeyEvent.VK_RIGHT, KeyEvent.VK_D -> {
				key_down_flag = key_down_flag and KEY_FLAG_RIGHT.inv()
				playerDx = if (key_down_flag and KEY_FLAG_LEFT != 0) {
					-1
				} else {
					0
				}
			}

			KeyEvent.VK_LEFT, KeyEvent.VK_A -> {
				key_down_flag = key_down_flag and KEY_FLAG_LEFT.inv()
				playerDx = if (key_down_flag and KEY_FLAG_RIGHT != 0) {
					1
				} else {
					0
				}
			}

			KeyEvent.VK_DOWN, KeyEvent.VK_S -> {
				key_down_flag = key_down_flag and KEY_FLAG_DOWN.inv()
				playerDy = if (key_down_flag and KEY_FLAG_UP != 0) {
					1
				} else {
					0
				}
			}

			KeyEvent.VK_UP, KeyEvent.VK_W -> {
				key_down_flag = key_down_flag and KEY_FLAG_UP.inv()
				playerDy = if (key_down_flag and KEY_FLAG_DOWN != 0) {
					-1
				} else {
					0
				}
			}
		}
		showHelp = false
		robotron.setPlayerMovement(Vector2D(playerDx, playerDy))
		evt.consume()
	}

	override fun onMousePressed(ev: MouseEvent) {
		robotron.setCursorPressed(true)
	}

	override fun mouseReleased(evt: MouseEvent) {
		robotron.setCursorPressed(false)
	}

	override fun mouseClicked(evt: MouseEvent) {}
	override fun mouseMoved(evt: MouseEvent) {
		robotron.setCursor(evt.x, evt.y)
	}

	override fun mouseDragged(evt: MouseEvent) {
		robotron.setCursor(evt.x, evt.y)
	}

	companion object {

		val settingsDir: File by lazy {
			RobotronApplet::class.java.getOrCreateSettingsDirectory()
		}

		@JvmStatic
		fun main(args: Array<String>) {
			setRandomSeed(0L)
			val id = args.firstOrNull()?.toIntOrNull() ?: 0
			val name = args.getOrNull(1) ?: "Robo Applet $id"
			spawn(id, name)
		}

		fun spawn(id: Int, name: String) {
			if (id >= MAX_PLAYERS)
				return
			RobotronApplet(id, name)
		}
	}

	override fun canPauseOnLostFocus(): Boolean {
		return false
	}
}