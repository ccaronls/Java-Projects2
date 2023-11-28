package marcos.games.hexes.swing

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTKeyboardAnimationApplet
import cc.lib.utils.FileUtils
import marcos.games.hexes.core.Hexes
import marcos.games.hexes.core.Player
import marcos.games.hexes.core.Shape
import java.io.File
import java.util.*
import javax.swing.JOptionPane

class HexesApplet(frame: AWTFrame) : AWTKeyboardAnimationApplet() {
	val game: Hexes = object : Hexes() {
		override fun onGameOver(winner: Int) {
			stopGame()
			object : Thread() {
				override fun run() {
					val n = JOptionPane.showConfirmDialog(
						frame,
						"Player $winner wins\nPlay Again?",
						"Game Over",
						JOptionPane.YES_NO_OPTION)
					when (n) {
						JOptionPane.YES_OPTION -> Thread(Runnable { showMainMenu() }).start()
						JOptionPane.CLOSED_OPTION, JOptionPane.NO_OPTION -> System.exit(0)
					}
				}
			}.start()
		}

		override fun onPiecePlayed(pIndex: Int, pts: Int) {
			val p = board.getPiece(pIndex)
			addMessage("Player %s played a %s for %d points", p.player, p.type, pts)
		}
	}

	@JvmField
	val frame: AWTFrame
	val restoreFile: File

	@JvmField
	var highlightedPiece = -1
	val messages = LinkedList<String>()
	fun addMessage(msg: String?, vararg args: Any?) {
		synchronized(messages) {
			messages.add(String.format(msg!!, *args))
			while (messages.size > 3) messages.removeLast()
		}
	}

	private fun showHelpPopup() {
		Thread(Runnable {
			val welcome = """
	        	Welcome to Hexes!
	        	
	        	The Object of the game is to gain the most points by placing triangular pieces to form hexagons, triangles and diamonds.
	        	
	        	""".trimIndent()
			JOptionPane.showConfirmDialog(frame, welcome, "Welcome!", JOptionPane.INFORMATION_MESSAGE)
			showChooseColorMenu()
		}).start()
	}

	private fun showChooseColorMenu() {
		object : Thread() {
			override fun run() {
				val n = JOptionPane.showOptionDialog(frame,
					"Choose Your Color",
					"Choose",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.PLAIN_MESSAGE,
					null, arrayOf("RED", "BLUE"),
					null)
				if (n == 0) {
					game.initPlayers(SwingPlayer(), Player())
				} else if (n == 1) {
					game.initPlayers(Player(), SwingPlayer())
				}
				game.newGame()
				startGame()
			}
		}.start()
	}

	private fun showMainMenu() {
		object : Thread() {
			override fun run() {
				val n = JOptionPane.showOptionDialog(frame,
					"Choose Game",
					"Choose",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.PLAIN_MESSAGE,
					null, arrayOf("NEW", "RESTORE", "CUSTOM", "AUTO"),
					null)
				when (n) {
					0 -> showChooseColorMenu()
					1 -> try {
						val g = Hexes()
						g.loadFromFile(restoreFile)
						game.copyFrom(g)
						startGame()
					} catch (e: Exception) {
						e.printStackTrace()
						JOptionPane.showMessageDialog(frame, "Error loading restore file: " + e.message)
						restoreFile.delete()
					}
					2 -> {}
					3 -> {
						game.initPlayers(Player(), Player())
						game.newGame()
						startGame()
					}
				}
			}
		}.start()
	}

	fun stopGame() {
		running = false
	}

	var running = false

	init {
		instance = this
		this.frame = frame
		val settings = FileUtils.getOrCreateSettingsDirectory(javaClass)
		frame.loadFromFile(File(settings, "hexes.properties"))
		restoreFile = File(settings, "hexes.txt")
		if (restoreFile.exists() && game.tryLoadFromFile(restoreFile)) {
			startGame()
		} else {
			showHelpPopup()
		}
	}

	fun startGame() {
		if (!running) {
			grabFocus()
			synchronized(messages) { messages.clear() }
			running = true
			object : Thread() {
				override fun run() {
					try {
						Utils.println("Thread running")
						while (running && !game.isGameOver) {
							synchronized(game) {
								game.runGame()
								game.saveToFile(restoreFile)
							}
						}
					} catch (e: Exception) {
						e.printStackTrace()
					}
					Utils.println("Thread exiting running=" + running + " gameover=" + game.isGameOver)
					running = false
				}
			}.start()
		}
	}

	override fun doInitialization() {}
	override fun drawFrame(g: AGraphics) {
		g.clearScreen(GColor.LIGHT_GRAY)
		val b = game.board
		b.setHighlighted(mouseX, mouseY)
		highlightedPiece = b.draw(g)
		g.ortho()
		g.color = GColor.RED
		if (game.numPlayers > 1) {
			val redTxt = """
	        	Points     ${game.getPlayer(1).score}
	        	Hexagons   ${game.getPlayer(1).getShapeCount(Shape.HEXAGON)}
	        	Triangles  ${game.getPlayer(1).getShapeCount(Shape.TRIANGLE)}
	        	Diamonds   ${game.getPlayer(1).getShapeCount(Shape.DIAMOND)}
	        	""".trimIndent()
			g.drawJustifiedString(10f, 10f, Justify.LEFT, redTxt)
			g.color = GColor.BLUE
			val blueTxt = """
	        	Points     ${game.getPlayer(2).score}
	        	Hexagons   ${game.getPlayer(2).getShapeCount(Shape.HEXAGON)}
	        	Triangles  ${game.getPlayer(2).getShapeCount(Shape.TRIANGLE)}
	        	Diamonds   ${game.getPlayer(2).getShapeCount(Shape.DIAMOND)}
	        	""".trimIndent()
			g.drawJustifiedString((g.viewportWidth - 10).toFloat(), 10f, Justify.RIGHT, blueTxt)
		}
		// draw messages
		val tx = g.viewportWidth / 2
		var ty = 10
		g.color = GColor.BLACK
		synchronized(messages) {
			for (msg in messages) {
				g.drawJustifiedString(tx.toFloat(), ty.toFloat(), Justify.CENTER, msg)
				ty += g.textHeight.toInt()
			}
		}
	}

	override fun onDimensionsChanged(g: AGraphics, width: Int, height: Int) {}

	companion object {
		@JvmField
		var instance: HexesApplet? = null

		@JvmStatic
		fun main(args: Array<String>) {
			Utils.setDebugEnabled()
			Utils.setRandomSeed(0)
			val frame = AWTFrame("Hexes Debug Mode")
			val app: AWTKeyboardAnimationApplet = HexesApplet(frame)
			frame.add(app)
			frame.centerToScreen(640, 480)
			app.init()
			app.start()
			app.setMillisecondsPerFrame(200)
		}
	}
}