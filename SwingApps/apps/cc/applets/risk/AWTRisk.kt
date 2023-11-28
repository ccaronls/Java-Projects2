package cc.applets.risk

import cc.lib.game.AGraphics
import cc.lib.game.Dice
import cc.lib.game.GColor
import cc.lib.logger.LoggerFactory
import cc.lib.risk.*
import cc.lib.swing.*
import cc.lib.swing.AWTFrame.OnListItemChoosen
import cc.lib.utils.FileUtils
import cc.lib.utils.Table
import cc.lib.utils.prettify
import java.awt.BorderLayout
import java.awt.GridLayout
import java.io.File
import java.io.FileInputStream
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Created by Chris Caron on 5/8/23.
 */


class AWTRisk internal constructor(): AWTComponent() {

	var bId = 0
	val frame = AWTFrame("R I S K")
	val board by lazy {
		RiskBoard().also {
			it.deserialize(FileInputStream(File("RiskAndroid/assets/risk.board")))
		}
	}
	val settingsDir by lazy {
		FileUtils.getOrCreateSettingsDirectory(AWTRisk::class.java)
	}

	val menu = JPanel(AWTButtonLayout())
	val menuContainer = JPanel(GridLayout()).also {
		it.add(menu)
		setPreferredSize(200, 1000)
	}

	val game = object : UIRisk(board) {
		override fun showDiceDialog(attacker: Army, attackingDice: IntArray, defender: Army, defendingDice: IntArray, result: BooleanArray) {
			val table = Table().addColumn(attacker.name, attackingDice.map {
				Dice(numPips = it, dieColor = GColor.RED, pipColor = GColor.WHITE)
			}).addColumn("", result.map {
				if (it) " <-- " else " --> "
			}).addColumn(defender.name, defendingDice.map {
				Dice(it)
			})
			val content = AWTPanel(BorderLayout())
			val panel = object : AWTComponent() {

				override fun paint(g: AWTGraphics, mouseX: Int, mouseY: Int) {
					with (table.measure(g)) {
						g.pushMatrix()
						g.translate(g.viewportWidth/2 - width/2, g.viewportHeight/2 - height/2)
						table.draw(g)
						g.popMatrix()
					}
				}
			}
			panel.setPreferredSize(300, 300)
			content.add(panel)
			val popup = AWTFrame(content)
			content.add(
				AWTWrapLabel("$attacker is attacking\n$defender with ${attackingDice.size} armies"),
				BorderLayout.NORTH)
			content.add(AWTButton("Okay") {
				setGameResult(null)
				popup.closePopup()
			}, BorderLayout.SOUTH)
			popup.showAsPopup(frame)
		}

		override fun initMenu(buttons: List<*>) {
			if (SwingUtilities.isEventDispatchThread())
				initMenuInternal(buttons)
			else SwingUtilities.invokeAndWait {
				initMenuInternal(buttons)
			}
		}

		private fun initMenuInternal(buttons: List<*>) {
			menu.removeAll()
			buttons.forEachIndexed { index, any ->
				menu.add(AWTButton(prettify(any.toString())) {
					setGameResult(buttons[index])
				})
			}
			menu.add(AWTButton("Quit") {
				stopGameThread()
			}.also {
				it.setTooltip("Quit the Game", 32)
			})
			menu.revalidate()
		}

		override val boardImageId: Int
			get() = bId

		override fun redraw() {
			this@AWTRisk.redraw()
		}

		override val saveGame: File by lazy {
			File(settingsDir, "save.game")
		}

		override fun onGameThreadStopped() {
			SwingUtilities.invokeLater {
				showHomeMenu()
			}
		}
	}

	init {
		setMouseEnabled(true)
		setPadding(5)
		//frame.addMenuBarMenu("File", "New Game", "Resume", "Rules")
		frame.add(this)
		frame.add(menuContainer, BorderLayout.WEST)
		showHomeMenu()

		if (!frame.loadFromFile(File(settingsDir, "risk.properties")))
			frame.centerToScreen(800, 600)
	}

	fun showHomeMenu() {
		menu.removeAll()
		menu.add(AWTButton("New Game") {
			frame.showListChooserDialog(object : OnListItemChoosen {
				override fun itemChoose(index: Int) {
					frame.showListChooserDialog(object : OnListItemChoosen {
						override fun itemChoose(numPlayersChoice: Int) {
							val color = Army.choices()[index]
							val colors = Army.choices().toMutableList().also { list ->
								list.removeIf { it == color }
								list.shuffle()
							}
							game.stopGameThread()
							game.reset()
							game.addPlayer(UIRiskPlayer(color))
							for (i in 1 until numPlayersChoice + 2) {
								game.addPlayer(RiskPlayer(colors.removeFirst()))
							}
							game.startGameThread()
						}
					}, "Choose Army", *Army.choices().prettify())
				}
			}, "Choose Number Of Players", "2", "3", "4")
		})
		menu.add(AWTButton("Resume") {
			game.stopGameThread()
			val temp = RiskGame(board)
			if (temp.tryLoadFromFile(game.saveGame)) {
				game.copyFrom(temp)
				game.startGameThread()
			} else {
				log.error("Cannot load save game")
			}
		})
		menu.add(AWTButton("Rules"))
	}

	var progress : Float = 0f

	override fun init(g: AWTGraphics) {
		g.addSearchPath("RiskAndroid/src/main/res/drawable")
		bId = g.loadImage("risk_board.png")
		with(g.loadImage("blowup_anim.png")) {
			game.loadExplodAnim(g, this)
		}
		progress = 1f
	}

	override val initProgress: Float
		get() = progress

	override fun paint(g: AWTGraphics, mouseX: Int, mouseY: Int) {
		game.onDraw(g)
	}

	override fun onDragStarted(x: Int, y: Int) {
		game.onDragStart(x.toFloat(), y.toFloat())
	}

	override fun onDrag(x: Int, y: Int, dx: Int, dy: Int) {
		game.onDrag(x.toFloat(), y.toFloat())
	}

	override fun onClick() {
		game.onTap(mouseX.toFloat(), mouseY.toFloat())
	}

	override fun onMouseMoved(mouseX: Int, mouseY: Int) {
		game.onMouse(mouseX.toFloat(), mouseY.toFloat())
		game.redraw()
	}

	companion object {
		private val log = LoggerFactory.getLogger(AWTRisk::class.java)

		@JvmStatic
		fun main(args: Array<String>) {
			AGraphics.DEBUG_ENABLED = true
			//Utils.setDebugEnabled()
			AWTRisk()
		}
	}
}