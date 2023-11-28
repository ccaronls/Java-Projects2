package cc.applets.swing

import cc.game.othello.ai.AiOthelloPlayer
import cc.game.othello.core.Othello
import cc.game.othello.core.OthelloBoard
import cc.lib.game.AAnimation
import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Utils
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTKeyboardAnimationApplet
import cc.lib.utils.FileUtils
import cc.lib.utils.Lock
import java.awt.BorderLayout
import java.awt.Container
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.io.File
import java.util.*
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class OthelloApplet internal constructor(val frame: Container) : AWTKeyboardAnimationApplet(), ActionListener {
	inner class FlipAnimation(val row: Int, val col: Int, val fromColor: Int, val toColor: Int) : AAnimation<AGraphics>(1000, 0) {
		override fun draw(g: AGraphics, position: Float, dt: Float) {
			var scale = 0f
			if (position < 0.5f) {
				g.color = if (fromColor == OthelloBoard.CELL_WHITE) GColor.WHITE else GColor.BLACK
				scale = 1f - position * 2
			} else {
				g.color = if (toColor == OthelloBoard.CELL_WHITE) GColor.WHITE else GColor.BLACK
				scale = (position - 0.5f) * 2
			}
			g.pushMatrix()
			//			g.translate(p/2, 0);
			g.scale(scale, 1f)
			g.drawFilledCircle(0f, 0f, 1f)
			g.popMatrix()
		}
	}

	val buttons = JPanel()
	val game: Othello = object : Othello() {
		override fun onCellChanged(row: Int, col: Int, oldColor: Int, newColor: Int) {
			val anim = FlipAnimation(row, col, oldColor, newColor)
			anim.start<AAnimation<AGraphics>>((anims.size * 500).toLong())
			anims.add(anim)
		}
	}
	val gameFile = File(FileUtils.getOrCreateSettingsDirectory(javaClass), "othello.txt")
	val anims: MutableList<FlipAnimation> = LinkedList()
	val lock = Lock()

	internal enum class Cmd {
		NEW_GAME,
		RESTORE,
		CHOOSE_WHITE,
		CHOOSE_BLACK,
		SHOW_MAIN_MENU
	}

	private fun newButton(txt: String, actionCmd: Cmd, enabled: Boolean = true): JButton {
		val b = JButton(txt)
		b.actionCommand = actionCmd.name
		b.addActionListener(this)
		b.isEnabled = enabled
		return b
	}

	private fun showMainMenu() {
		buttons.removeAll()
		buttons.add(newButton("New Game", Cmd.NEW_GAME))
		buttons.add(newButton("Restore", Cmd.RESTORE, gameFile.exists()))
	}

	protected fun showChooseColorMenu() {
		buttons.removeAll()
		buttons.add(newButton("White", Cmd.CHOOSE_WHITE))
		buttons.add(newButton("Black", Cmd.CHOOSE_BLACK))
		buttons.add(newButton("Back", Cmd.SHOW_MAIN_MENU))
	}

	private fun showGameMenu() {
		buttons.removeAll()
		buttons.add(newButton("Quit", Cmd.SHOW_MAIN_MENU))
		buttons.add(JLabel("White: " + game.board.getCellCount(OthelloBoard.CELL_WHITE)))
		buttons.add(JLabel("Black: " + game.board.getCellCount(OthelloBoard.CELL_BLACK)))
		frame.validate()
		frame.repaint()
	}

	var running = false
	fun startGame() {
		if (!running) {
			running = true
			Thread(Runnable {
				try {
					Utils.println("Thread running")
					while (running && !game.isGameOver) {
						if (anims.size == 0) {
							synchronized(game) {
								game.runGame()
								game.saveToFile(gameFile)
							}
							showGameMenu()
						}
					}
				} catch (e: Exception) {
					e.printStackTrace()
				}
				running = false
				Utils.println("Thread exiting")
			}).start()
		}
	}

	override fun doInitialization() {}
	override fun drawFrame(g: AGraphics) {
		val w = g.viewportWidth.toFloat()
		val h = g.viewportHeight.toFloat()
		val radius = 10f
		val padding = 10f
		val thickness = 5f
		g.color = GColor.GREEN
		g.drawFilledRoundedRect(padding, padding, w - 2 * padding, h - padding * 2, radius)
		g.color = GColor.WHITE
		g.drawRoundedRect(padding, padding, w - 2 * padding, h - padding * 2, thickness, radius)
		val b = game.board
		val bw = g.viewportWidth - padding * 2 - radius * 2
		val bh = g.viewportHeight - padding * 2 - radius * 2
		val cw = bw / b.numCols
		val ch = bh / b.numRows
		val x0 = radius + padding
		val y0 = radius + padding
		pickedCol = -1
		pickedRow = pickedCol
		for (r in 0 until b.numRows) {
			for (c in 0 until b.numCols) {
				val cx = x0 + cw / 2 + cw * c
				val cy = y0 + ch / 2 + ch * r
				val cell = b[r, c]
				val cx0 = cx - cw / 2 + 1
				val cy0 = cy - ch / 2 + 1
				val pcRad = Math.min(cw, ch) / 2 - 3
				g.color = GColor.WHITE
				when (cell) {
					OthelloBoard.CELL_AVAILABLE -> {
						if (Utils.isPointInsideRect(mouseX.toFloat(), mouseY.toFloat(), cx0, cy0, cw - 2, ch - 2)) {
							g.color = GColor.RED
							pickedRow = r
							pickedCol = c
						}
						g.drawRect(cx - cw / 2 + 1, cy - ch / 2 + 1, cw - 2, ch - 2, 2f)
						continue
					}
					OthelloBoard.CELL_UNUSED -> {
						g.drawRect(cx - cw / 2 + 1, cy - ch / 2 + 1, cw - 2, ch - 2, 2f)
						continue
					}
				}
				g.color = GColor.WHITE
				g.drawRect(cx - cw / 2 + 1, cy - ch / 2 + 1, cw - 2, ch - 2, 2f)
				val f = getAnimationAt(r, c)
				if (f != null) {
					g.pushMatrix()
					g.translate(cx, cy)
					g.scale(pcRad, pcRad)
					f.update(g)
					if (f.isDone) anims.remove(f)
					g.popMatrix()
				} else {
					g.color = if (b[r, c] == OthelloBoard.CELL_BLACK) GColor.BLACK else GColor.WHITE
					g.drawFilledCircle(cx, cy, pcRad)
				}
			}
		}
	}

	fun getAnimationAt(r: Int, c: Int): FlipAnimation? {
		for (f in anims) {
			if (f.row == r && f.col == c) return f
		}
		return null
	}

	var pickedRow = 0
	var pickedCol = 0

	init {
		instance = this
		frame.add(buttons, BorderLayout.SOUTH)
		buttons.layout = GridLayout(1, 0)
		showMainMenu()
	}

	override fun onDimensionsChanged(g: AGraphics, width: Int, height: Int) {
		// TODO Auto-generated method stub
	}

	override fun actionPerformed(ev: ActionEvent) {
		try {
			when (Cmd.valueOf(ev.actionCommand)) {
				Cmd.CHOOSE_BLACK -> {
					game.intiPlayers(AiOthelloPlayer(), SwingOthelloPlayer())
					game.newGame()
					startGame()
				}
				Cmd.CHOOSE_WHITE -> {
					game.intiPlayers(SwingOthelloPlayer(), AiOthelloPlayer())
					game.newGame()
					startGame()
				}
				Cmd.NEW_GAME -> showChooseColorMenu()
				Cmd.RESTORE -> {
					game.loadFromFile(gameFile)
					startGame()
				}
				Cmd.SHOW_MAIN_MENU -> {
					run {
						pickedCol = -1
						pickedRow = pickedCol
					}
					running = false
					lock.release()
					showMainMenu()
				}
				else -> {}
			}
			frame.validate()
			frame.repaint()
			getRootPane().grabFocus()
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	fun pickCell(rowColCell: IntArray): Boolean {
		try {
			lock.acquireAndBlock()
			rowColCell[0] = pickedRow
			rowColCell[1] = pickedCol
			return true
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return false
	}

	override fun mouseClicked(evt: MouseEvent) {
		super.mouseClicked(evt)
		lock.release()
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			Utils.setDebugEnabled()
			Utils.setRandomSeed(0)
			val frame = AWTFrame("JavaRoids Debug Mode")
			instance = OthelloApplet(frame)
			frame.add(instance)
			val settings = FileUtils.getOrCreateSettingsDirectory(OthelloApplet::class.java)
			if (!frame.loadFromFile(File(settings, "gui.properties"))) frame.centerToScreen(640, 480)
			instance.init()
			instance.start()
			//        app.setMillisecondsPerFrame(20);
		}

		lateinit var instance: OthelloApplet
	}
}