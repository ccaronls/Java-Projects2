package cc.applets.robotron

import cc.game.superrobotron.IRoboClientListener
import cc.game.superrobotron.PLAYER_STATE_SPECTATOR
import cc.game.superrobotron.POWERUP_NUM_TYPES
import cc.game.superrobotron.Robotron
import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.logger.LoggerFactory.LogLevel
import cc.lib.math.Vector2D
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTGraphics
import cc.lib.swing.AWTKeyboardAnimationApplet
import cc.lib.utils.FileUtils
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

class RobotronApplet : AWTKeyboardAnimationApplet(), IRoboClientListener {
	override fun getDefaultFont(): Font {
		return Font("Arial", Font.BOLD, 12)
	}

	lateinit var robotron: Robotron
	override fun doInitialization() {
		Utils.setDebugEnabled()
		LoggerFactory.logLevel = LogLevel.DEBUG
		setRandomSeed(0)
		robotron = object : Robotron() {
			override val imageKey: Int by lazy {
				G.loadImage("key.png", GColor.BLACK)
			}
			override val imageLogo: Int by lazy {
				G.loadImage("logo.png", GColor.BLACK)
			}
			override val animJaws: IntArray by lazy {
				G.loadImageCells("jaws.png", 32, 32, 8, 9, true, GColor.BLACK)
			}
			override val animLava: IntArray by lazy {
				G.loadImageCells("lavapit.png", 32, 32, 8, 25, true, GColor.BLACK)
			}
			override val animPeople: Array<IntArray> by lazy {
				arrayOf(
					G.loadImageCells("people.png", 32, 32, 4, 16, true, GColor.BLACK),
					G.loadImageCells("people2.png", 32, 32, 4, 16, true, GColor.BLACK),
					G.loadImageCells("people3.png", 32, 32, 4, 16, true, GColor.BLACK)
				)
			}

			override fun initGraphics(g: AGraphics) {
				super.initGraphics(g)
				with(g as AWTGraphics) {
					addSearchPath("SuperRobotronAndroid/assets/pngs")
					addSearchPath("SuperRobotronAndroid/src/main/res/drawable")
				}
			}

			override val clock: Long
				get() = System.currentTimeMillis()

		}
	}

	val isConnected: Boolean
		get() = robotron.server?.roboConnections?.any {
			it.connected
		} ?: robotron.client?.connected ?: false

	fun rootFrame(container: Container = parent): AWTFrame? {
		return (container as? AWTFrame) ?: rootFrame(container.parent)
	}

	val requireRootFrame: AWTFrame
		get() = requireNotNull(rootFrame())

	fun showDisplayNameDialog(onDoneCb: () -> Unit) {
		robotron.player.displayName = requireRootFrame.getStringProperty("displayName", "")
		if (robotron.player.displayName.isNotBlank()) {
			onDoneCb()
			return
		}
		JOptionPane.showInputDialog(this, "Confirm Display Name", robotron.player.displayName)?.let { displayName ->
			robotron.player.displayName = displayName
			requireRootFrame.setProperty("displayName", displayName)
			onDoneCb()
		}
	}

	fun initHost() = showDisplayNameDialog {
		robotron.server = bindToHost(robotron)
	}

	fun joinHost() = showDisplayNameDialog {
		robotron.client = connectToHost(robotron)
	}

	override fun onDropped() {
		showMsg = "Dropped"
		robotron.setGameStateIntro()
	}

	override fun onConnected() {
		grabFocus()
		showMsg = "Connected"
	}

	fun disconnect() {
		robotron.server?.disconnect()
		robotron.client?.disconnect()
		robotron.server = null
		robotron.client = null
		robotron.setGameStateIntro()
		robotron.players.clear()
		robotron.players.add()
		robotron.this_player = 0
	}

	@Synchronized
	override fun drawFrame(g: AGraphics) {
		robotron.drawGame(g, millisecondsPerFrame)
		g.color = showMsgColor
		g.drawJustifiedString(viewportWidth / 2, 5, Justify.CENTER, Justify.TOP, showMsg)
		showMsgColor = showMsgColor.darkened(.05f)
		g.color = GColor.YELLOW
		if (showHelp) {
			val str = "HELP\n" + helpMap.values.joinToString("\n") {
				"${it.first}    - ${it.second()}"
			}
			g.drawJustifiedStringOnBackground(20f, (screenHeight / 2).toFloat(), Justify.LEFT, Justify.CENTER, str, GColor.TRANSLUSCENT_BLACK, 5f)
		}

		if (robotron.players.size > 1) {
			var str = ""
			robotron.players.forEachIndexed { idx, pl ->
				if (idx == robotron.this_player)
					str += "-> "
				str += pl.displayName + "\n"
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

	override fun onDimensionsChanged(g: AGraphics, width: Int, height: Int) {
		robotron.initGraphics(g)
		robotron.setDimension(width, height)
	}

	var showHelp = false
	var showMsgColor = GColor.WHITE
	var showMsg: String = ""
		set(value) {
			field = value
			showMsgColor = GColor.WHITE
		}

	// bit flags too key_down_flag
	private val KEY_FLAG_LEFT = 1
	private val KEY_FLAG_RIGHT = 2
	private val KEY_FLAG_DOWN = 4
	private val KEY_FLAG_UP = 8
	private var key_down_flag = 0
	private var playerDx = 0
	private var playerDy = 0

	val helpMap = noDupesMapOf(
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
		KeyEvent.VK_LESS to Triple('<', { "Next Level" }) { robotron.nextLevel() },
		KeyEvent.VK_GREATER to Triple('>', { "Previous Level" }) { robotron.prevLevel() },
		KeyEvent.VK_V to Triple('V', { "Toggle Visibility ${Robotron.GAME_VISIBILITY.toOnOffStr()}" }) {
			Robotron.GAME_VISIBILITY = !Robotron.GAME_VISIBILITY
		},
		//KeyEvent.VK_G to Triple('G', { "Game Over" }) { robotron.gameOver() },
		KeyEvent.VK_B to Triple('B', { "Add Player" }) {
			robotron.players.addOrNull()?.let {
				robotron.initNewPlayer(it)
			}
		},
		KeyEvent.VK_G to Triple('G', { "Add Remote Player" }) {
			spawn()
		},
		KeyEvent.VK_N to Triple('N', { "Toggle current player ${robotron.this_player}" }) {
			robotron.this_player = (robotron.this_player + 1) % robotron.players.size
		},
		KeyEvent.VK_M to Triple('M', { "Enter Spectator Mode" }) { robotron.player.state = PLAYER_STATE_SPECTATOR },
		KeyEvent.VK_H to Triple('H', { "Host" }) { initHost() },
		KeyEvent.VK_J to Triple('J', { "Join" }) { joinHost() },
		KeyEvent.VK_L to Triple('L', { "Disconnect" }) { disconnect() },
	)

	override fun reportKeyRepeats(): Boolean = false

	override fun onKeyPressed(evt: KeyEvent) {
		log.debug("key pressed: $evt")
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
						showMsg = it.second()
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
		//println("keyPressed $playerDx, $playerDy")
		if (Utils.isDebugEnabled()) {
			val index = evt.keyChar - '1'
			if (index >= 0 && index < Robotron.Debug.values().size) {
				val enabled = robotron.isDebugEnabled(Robotron.Debug.values()[index])
				robotron.setDebugEnabled(Robotron.Debug.values()[index], !enabled)
			}
		}
		robotron.setPlayerMovement(playerDx, playerDy)
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

			else -> showHelp = false
		}
		robotron.setPlayerMovement(playerDx, playerDy)
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

		val log = LoggerFactory.getLogger(RobotronApplet::class.java)

		val settingsDir by lazy {
			FileUtils.getOrCreateSettingsDirectory(RobotronApplet::class.java)
		}

		var spawns = 0

		@JvmStatic
		fun main(args: Array<String>) {
//			AGraphics.DEBUG_ENABLED = false
//			LoggerFactory.logLevel = LoggerFactory.LogLevel.SILENT
			setRandomSeed(0L)
			spawn()
		}

		fun spawn() {
			if (spawns == 4)
				return
			val frame = AWTFrame("Robotron $spawns")
			val app: AWTKeyboardAnimationApplet = RobotronApplet()
			frame.add(app)
			app.init()
			if (!frame.loadFromFile(File(settingsDir, "robo$spawns.properties")))
				frame.centerToScreen(800, 600)
			app.start()
			app.setMillisecondsPerFrame(1000 / 20)
			spawns++
		}
	}

	override fun canPauseOnLostFocus(): Boolean {
		return false //!isConnected
	}
}