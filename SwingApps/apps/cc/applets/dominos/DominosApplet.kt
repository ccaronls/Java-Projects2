package cc.applets.dominos

import cc.game.dominos.core.Dominos
import cc.lib.game.AGraphics
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.net.AClientConnection
import cc.lib.net.AGameServer
import cc.lib.swing.*
import cc.lib.utils.FileUtils
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.ActionListener
import java.io.File
import javax.swing.ButtonGroup
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton

class DominosApplet internal constructor() : AWTComponent(), AGameServer.Listener {
	val frame: AWTFrame
	var numPlayersChoice: Int
	var numPipsChoice: Int
	var maxScoreChoice: Int
	var difficultyChoice: Int
	fun saveSettings() {
		frame.setProperty("numPlayersChoice", numPlayersChoice)
		frame.setProperty("numPipsChoice", numPipsChoice)
		frame.setProperty("maxScoreChoice", maxScoreChoice)
		frame.setProperty("difficultyChoice", difficultyChoice)
	}

	interface OnChoiceListener {
		fun choiceMade(choice: Int)
	}

	fun makeRadioGroup(label: String?, choice: Int, listener: OnChoiceListener, vararg buttons: Int): JPanel {
		val panel = AWTPanel(GridLayout(1, 0))
		panel.add(JLabel(label))
		val al = ActionListener { e -> listener.choiceMade(e.actionCommand.toInt()) }
		val group = ButtonGroup()
		for (i in buttons) {
			val s = i.toString()
			val b = JRadioButton(s)
			if (i == choice) b.isSelected = true
			b.addActionListener(al)
			b.actionCommand = s
			group.add(b)
			panel.add(b)
		}
		return panel
	}

	fun makeRadioGroup(choice: Int, listener: OnChoiceListener, vararg buttons: String?): JPanel {
		val panel = AWTPanel(GridLayout(1, 0))
		val al = ActionListener { e -> listener.choiceMade(e.actionCommand.toInt()) }
		val group = ButtonGroup()
		for ((i, s) in buttons.withIndex()) {
			val b = JRadioButton(s)
			b.isSelected = i == choice
			b.addActionListener(al)
			b.actionCommand = i.toString()
			group.add(b)
			panel.add(b)
		}
		return panel
	}

	fun showNewGamePopup() {
		val numPlayers = makeRadioGroup("Num Players:", numPlayersChoice, object : OnChoiceListener {
			override fun choiceMade(choice: Int) {
				numPlayersChoice = choice
			}
		}, 2, 3, 4)
		val numPips = makeRadioGroup("Num Pips:", numPipsChoice, object : OnChoiceListener {
			override fun choiceMade(choice: Int) {
				numPipsChoice = choice
			}
		}, 6, 9, 12)
		val maxPts = makeRadioGroup("Max Score:", maxScoreChoice, object : OnChoiceListener {
			override fun choiceMade(choice: Int) {
				maxScoreChoice = choice
			}
		}, 150, 200, 250)
		val difficulty = makeRadioGroup(difficultyChoice, object : OnChoiceListener {
			override fun choiceMade(choice: Int) {
				difficultyChoice = choice
			}
		}, "Easy", "Medium", "Hard")
		val popup = AWTFrame()
		val panel = AWTPanel(GridLayout(0, 1),
			numPlayers,
			numPips,
			maxPts,
			difficulty
		)
		val buttons = AWTPanel(FlowLayout(),
			AWTButton("Cancel", ActionListener { popup.closePopup() }),
			AWTButton("Start", ActionListener {
				dominos.stopGameThread()
				dominos.initGame(numPipsChoice, maxScoreChoice, difficultyChoice)
				dominos.setNumPlayers(numPlayersChoice)
				dominos.startNewGame()
				dominos.startGameThread()
				popup.closePopup()
				saveSettings()
			}),
			AWTButton("Resume", ActionListener {
				dominos.stopGameThread()
				if (!dominos.tryLoadFromFile(saveFile) || !dominos.isInitialized()) {
					dominos.clear()
					isEnabled = false
				} else {
					dominos.startGameThread()
					popup.closePopup()
				}
			})
		)
		val root = AWTPanel(BorderLayout())
		root.add(JLabel("Game Setup"), BorderLayout.NORTH)
		root.add(panel, BorderLayout.CENTER)
		root.add(buttons, BorderLayout.SOUTH)
		popup.contentPane = root
		popup.showAsPopup(frame)
	}

	lateinit var saveFile: File
	val dominos: Dominos = object : Dominos() {
		override fun redraw() {
			repaint()
		}

		override fun onMenuClicked() {
			showNewGamePopup()
		}
	}

	init {
		setMouseEnabled(true)
		setPadding(5)
		frame = object : AWTFrame("Dominos") {
			override fun onWindowClosing() {
				if (dominos.isGameRunning) dominos.trySaveToFile(saveFile)
			}

			override fun onMenuItemSelected(menu: String, subMenu: String) {
				when (subMenu) {
					"New Game" -> showNewGamePopup()
				}
			}
		}
		frame.addMenuBarMenu("File", "New Game")
		frame.add(this)
		val settings = FileUtils.getOrCreateSettingsDirectory(javaClass)
		if (!frame.loadFromFile(File(settings, "dominos.properties"))) frame.centerToScreen(800, 600)
		saveFile = File(settings, "dominos.save")
		numPlayersChoice = frame.getIntProperty("numPlayersChoice", 3)
		numPipsChoice = frame.getIntProperty("numPipsChoice", 6)
		maxScoreChoice = frame.getIntProperty("maxScoreChoice", 150)
		difficultyChoice = frame.getIntProperty("difficultyChoice", 0)
		log.debug("loaded from properties:")
		log.debug("  numPlayersChoice:$numPlayersChoice")
		log.debug("  numPipsChoice:$numPipsChoice")
		log.debug("  maxScoreChoice:$maxScoreChoice")
		log.debug("  difficultyChoice:$difficultyChoice")

//        dominos.startGameThread();
		//dominos.initGame(9, 150, 0);
		//dominos.startIntroAnim();
		//dominos.getBoard().setBoardImageId(AWTGraphics.getImages().loadImage("assets/jamaica_dominos_table.png"));
	}

	override fun init(g: AWTGraphics) {
		dominos.init(g)
		dominos.startDominosIntroAnimation(null)
	}

	override fun paint(g: AWTGraphics, mouseX: Int, mouseY: Int) {
		dominos.draw(g, mouseX, mouseY)
	}

	override fun onDragStarted(x: Int, y: Int) {
		dominos.startDrag()
	}

	override fun onDragStopped() {
		dominos.stopDrag()
	}

	override fun onClick() {
		dominos.onClick()
	}

	override fun onConnected(conn: AClientConnection) {
		log.info("New Client connection: " + conn.name)
	}

	companion object {
		private val log = LoggerFactory.getLogger(DominosApplet::class.java)

		@JvmStatic
		fun main(args: Array<String>) {
			AGraphics.DEBUG_ENABLED = true
			Utils.setDebugEnabled()
			DominosApplet()
		}
	}
}