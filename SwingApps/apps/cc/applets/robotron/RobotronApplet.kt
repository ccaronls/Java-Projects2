package cc.applets.robotron

import cc.game.superrobotron.POWERUP_NUM_TYPES
import cc.game.superrobotron.Robotron
import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTGraphics
import cc.lib.swing.AWTKeyboardAnimationApplet
import cc.lib.utils.random
import java.awt.Font
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent

class RobotronApplet : AWTKeyboardAnimationApplet() {
	override fun getDefaultFont(): Font {
		return Font("Arial", Font.BOLD, 12)
	}

	lateinit var robotron: Robotron
	override fun doInitialization() {
		Utils.setDebugEnabled()
		robotron = object : Robotron() {
			override val imageKey: Int by lazy {
				G.loadImage("key.gif", GColor.BLACK)
			}
			override val imageLogo: Int by lazy {
				G.loadImage("logo.gif", GColor.BLACK)
			}
			override val animJaws: IntArray by lazy {
				G.loadImageCells("jaws.gif", 32, 32, 8, 9, true, GColor.BLACK)
			}
			override val animLava: IntArray by lazy {
				G.loadImageCells("lavapit.gif", 32, 32, 8, 25, true, GColor.BLACK)
			}
			override val animPeople: Array<IntArray> by lazy {
				arrayOf(
					G.loadImageCells("people.gif", 32, 32, 4, 16, true, GColor.BLACK),
					G.loadImageCells("people2.gif", 32, 32, 4, 16, true, GColor.BLACK),
					G.loadImageCells("people3.gif", 32, 32, 4, 16, true, GColor.BLACK)
				)
			}

			override fun initGraphics(g: AGraphics) {
				super.initGraphics(g)
				with(g as AWTGraphics) {
					//addSearchPath()
				}
			}

			override val clock: Long
				get() = System.currentTimeMillis()
		}
	}

	override fun drawFrame(g: AGraphics) {
		robotron.drawGame(g)
		if (showHelp) {
			g.color = GColor.YELLOW
			val strHelp = """
	        	HELP:
	        	M   - add snake missle
	        	L   - add powerup
	        	H   - help screen
	        	R   - return to intro
	        	N   - goto next level [${robotron.gameLevel + 1}]
	        	P   - goto previous level [${robotron.gameLevel - 1}
	        	V   - toggle visibility mode [${Robotron.GAME_VISIBILITY}]

	        	""".trimIndent()
			g.drawJustifiedString(20f, (screenHeight / 2).toFloat(), Justify.LEFT, Justify.CENTER, strHelp)
		}
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

	override fun keyPressed(evt: KeyEvent) {
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

			KeyEvent.VK_M -> with(robotron) {
				addSnakeMissle(random(screen_x..screen_x + screen_width), random(screen_y..screen_y + screen_height))
			}

			KeyEvent.VK_L -> with(robotron) {
				addPowerup(
					random(screen_x..screen_x + screen_width),
					random(screen_y..screen_y + screen_height),
					random(0 until POWERUP_NUM_TYPES)
				)
			}

			KeyEvent.VK_H -> showHelp = true
			KeyEvent.VK_R -> robotron.setGameStateIntro()
			KeyEvent.VK_N -> robotron.nextLevel()
			KeyEvent.VK_P -> robotron.prevLevel()
			KeyEvent.VK_V -> Robotron.GAME_VISIBILITY = !Robotron.GAME_VISIBILITY
			/*
			val strHelp = """
	        	HELP:
	        	M   - add snake missle
	        	L   - add powerup
	        	H   - help screen
	        	R   - return to intro
	        	N   - goto next level [${robotron.gameLevel + 1}]
	        	P   - goto previous level [${robotron.gameLevel - 1}
	        	V   - toggle visibility mode [${Robotron.GAME_VISIBILITY}]
			 */
			/*
			M   - add snake missle
	        	L   - add powerup
	        	H   - help screen
	        	R   - return to intro
	        	N   - goto next level [${robotron.gameLevel + 1}]
	        	P   - goto previous level [${robotron.gameLevel - 1}
	        	V   - toggle visibility mode [${Robotron.GAME_VISIBILITY}]
			 */
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

	override fun keyReleased(evt: KeyEvent) {
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
			KeyEvent.VK_H -> showHelp = false
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
		@JvmStatic
		fun main(args: Array<String>) {
			AGraphics.DEBUG_ENABLED = false
			LoggerFactory.logLevel = LoggerFactory.LogLevel.SILENT
			//Utils.DEBUG_ENABLED = true;
			//Golf.DEBUG_ENABLED = true;
			//PlayerBot.DEBUG_ENABLED = true;
			val frame = AWTFrame("Robotron")
			val app: AWTKeyboardAnimationApplet = RobotronApplet()
			frame.add(app)
			app.init()
			frame.centerToScreen(800, 600)
			app.start()
			app.setMillisecondsPerFrame(1000 / 20)
		}
	}
}