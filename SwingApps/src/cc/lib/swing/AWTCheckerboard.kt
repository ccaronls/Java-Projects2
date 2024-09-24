package cc.lib.swing

import cc.lib.checkerboard.AIPlayer
import cc.lib.checkerboard.CanadianDraughts
import cc.lib.checkerboard.Checkers
import cc.lib.checkerboard.Chess
import cc.lib.checkerboard.Color
import cc.lib.checkerboard.Columns
import cc.lib.checkerboard.Dama
import cc.lib.checkerboard.DragonChess
import cc.lib.checkerboard.Draughts
import cc.lib.checkerboard.Game
import cc.lib.checkerboard.KingsCourt
import cc.lib.checkerboard.PieceType
import cc.lib.checkerboard.Shashki
import cc.lib.checkerboard.Suicide
import cc.lib.checkerboard.UIGame
import cc.lib.checkerboard.UIPlayer
import cc.lib.checkerboard.Ugolki
import cc.lib.game.AGraphics
import cc.lib.game.Utils
import cc.lib.utils.EventQueue
import cc.lib.utils.FileUtils
import java.awt.event.KeyEvent
import java.io.File

class AWTCheckerboard internal constructor() : AWTComponent() {
	lateinit var frame: AWTFrame
	lateinit var game: UIGame
	lateinit var saveFile: File
	var difficulty = 2
	val eq = EventQueue()
	fun loadImage(g: AGraphics, path: String): Int {
		val id = g.loadImage(path)
		if (id < 0) throw RuntimeException("Failed to load image '$path'")
		return id
	}

	var numImagesLoaded = 0

	internal enum class Images(val color: Color?, vararg types: PieceType) {
		wood_checkerboard_8x8(null),
		kings_court_board_8x8(null),
		bk_bishop(Color.BLACK, PieceType.BISHOP),
		bk_king(Color.BLACK, PieceType.KING),
		bk_knight(Color.BLACK, PieceType.KNIGHT_R, PieceType.KNIGHT_L),
		bk_pawn(Color.BLACK, PieceType.PAWN),
		bk_queen(Color.BLACK, PieceType.QUEEN),
		bk_rook(Color.BLACK, PieceType.ROOK),
		bk_dragon(Color.BLACK, PieceType.DRAGON_L, PieceType.DRAGON_R, PieceType.DRAGON_IDLE_L, PieceType.DRAGON_IDLE_R),
		wt_bishop(Color.WHITE, PieceType.BISHOP),
		wt_king(Color.WHITE, PieceType.KING),
		wt_knight(Color.WHITE, PieceType.KNIGHT_R, PieceType.KNIGHT_L),
		wt_pawn(Color.WHITE, PieceType.PAWN),
		wt_queen(Color.WHITE, PieceType.QUEEN),
		wt_rook(Color.WHITE, PieceType.ROOK),
		wt_dragon(Color.WHITE, PieceType.DRAGON_L, PieceType.DRAGON_R, PieceType.DRAGON_IDLE_L, PieceType.DRAGON_IDLE_R),
		blk_checker(Color.BLACK, PieceType.CHECKER, PieceType.CHIP_4WAY),
		red_checker(Color.RED, PieceType.CHECKER, PieceType.CHIP_4WAY),
		wt_checker(Color.WHITE, PieceType.CHECKER);

		val pt = arrayOf(types)
	}

	var ids = IntArray(Images.values().size)
	override fun init(g: AWTGraphics) {
		object : Thread() {
			override fun run() {
				for (i in ids.indices) {
					ids[i] = loadImage(g, "images/" + Images.values()[i].name + ".png")
					numImagesLoaded++
					repaint()
				}
			}
		}.start()
	}

	override val initProgress: Float
		get() = numImagesLoaded.toFloat() / ids.size

	init {
		setMouseEnabled(true)
		setPadding(5)
		Thread(eq).start()
		frame = object : AWTFrame("Checkerboard") {
			override fun onMenuItemSelected(menu: String, subMenu: String) {
				when (menu) {
					"Load Game" -> {
						val file = frame.showFileOpenChooser("Load Game", ".save", "checkerboard games")
						if (file != null) {
							val tmp = Game()
							if (tmp.tryLoadFromFile(file)) {
								game.stopGameThread()
								game.tryLoadFromFile(file)
								//game.setPlayer(Game.NEAR, new UIPlayer(UIPlayer.Type.USER));
								//game.setPlayer(Game.FAR, new UIPlayer(UIPlayer.Type.USER));
								game.startGameThread()
							} else {
								System.err.println("Cannot load $file")
							}
						}
					}
					"New Game" -> {
						game.stopGameThread()
						when (subMenu) {
							"Checkers" -> game.setRules(Checkers())
							"Suicide" -> game.setRules(Suicide())
							"Draughts" -> game.setRules(Draughts())
							"Canadian Draughts" -> game.setRules(CanadianDraughts())
							"Dama" -> game.setRules(Dama())
							"Chess" -> game.setRules(Chess())
							"Dragon Chess" -> game.setRules(DragonChess())
							"Ugolki" -> game.setRules(Ugolki())
							"Columns" -> game.setRules(Columns())
							"Kings Court" -> game.setRules(KingsCourt())
							"Shashki" -> game.setRules(Shashki())
						}
						Thread(Runnable {
							val num = frame.showItemChooserDialog("PLAYERS", "Choose Number of Players", "ONE PLAYER", "TWO PLAYERS")
							when (num) {
								0 -> {
									game.setPlayer(Game.NEAR, UIPlayer(UIPlayer.Type.USER))
									game.setPlayer(Game.FAR, UIPlayer(UIPlayer.Type.AI, difficulty))
									game.newGame()
								}
								1 -> {
									game.setPlayer(Game.NEAR, UIPlayer(UIPlayer.Type.USER))
									game.setPlayer(Game.FAR, UIPlayer(UIPlayer.Type.USER, difficulty))
									game.newGame()
								}
							}
							game.trySaveToFile(saveFile)
							game.startGameThread()
						}).start()
					}
					"Game" -> when (subMenu) {
						"Stop Thinking" -> {
							AIPlayer.cancel()
						}
						"Resume" -> {
							game.startGameThread()
							redraw()
						}
						"Stop" -> {
							game.stopGameThread()
							redraw()
						}
						"One Player" -> {
							game.stopGameThread()
							(game.getPlayer(Game.FAR) as UIPlayer?)!!.type = UIPlayer.Type.AI
							redraw()
						}
						"Two Players" -> {
							game.stopGameThread()
							(game.getPlayer(Game.FAR) as UIPlayer?)!!.type = UIPlayer.Type.USER
							redraw()
						}
					}
					"Difficulty" -> {
						when (subMenu) {
							"Easy" -> {
								difficulty = 1
								(game.getPlayer(Game.FAR) as UIPlayer?)!!.maxSearchDepth = difficulty
							}
							"Medium" -> {
								difficulty = 2
								(game.getPlayer(Game.FAR) as UIPlayer?)!!.maxSearchDepth = difficulty
							}
							"Hard" -> {
								difficulty = 3
								(game.getPlayer(Game.FAR) as UIPlayer?)!!.maxSearchDepth = difficulty
							}
						}
						frame.setProperty("difficulty", difficulty)
					}
				}
			}
		}
		game = object : UIGame() {
			override fun repaint(delayMs: Long) {
				if (delayMs <= 0) this@AWTCheckerboard.repaint() else {
					eq.enqueue(delayMs, Runnable { this@AWTCheckerboard.repaint() })
				}
			}

			override fun getPieceImageId(p: PieceType, color: Color): Int {
				for (i in Images.values()) {
					if (i.color === color && Utils.linearSearch(i.pt, p) >= 0) return ids[i.ordinal]
				}
				return -1
			}

			override val checkerboardImageId: Int
				protected get() = ids[Images.wood_checkerboard_8x8.ordinal]
			override val kingsCourtBoardId: Int
				protected get() = ids[Images.kings_court_board_8x8.ordinal]
		}
		val settings = FileUtils.getOrCreateSettingsDirectory(javaClass)
		saveFile = File(settings, "game.save")
		frame.add(this)
		val items = arrayOf("Checkers", "Suicide", "Draughts", "Canadian Draughts", "Dama", "Chess", "Dragon Chess", "Ugolki", "Columns", "Kings Court", "Shashki")
		frame.addMenuBarMenu("New Game", *items)
		frame.addMenuBarMenu("Load Game", "From File")
		frame.addMenuBarMenu("Game", "Stop Thinking", "Resume", "Stop", "One Player", "Two Players")
		frame.addMenuBarMenu("Difficulty", "Easy", "Medium", "Hard")
		frame.setPropertiesFile(File(settings, "gui.properties"))
		if (!frame.restoreFromProperties()) frame.centerToScreen(640, 640)
		game.init(saveFile)
		difficulty = frame.getIntProperty("difficulty", difficulty)
	}

	override fun onClick() {
		game.doClick()
	}

	override fun onDragStarted(x: Int, y: Int) {
		game.startDrag()
	}

	override fun onDragStopped() {
		game.stopDrag()
	}

	override fun paint(g: AWTGraphics) {
		game.draw(g, mouseX, mouseY)
	}

	override fun onKeyReleased(e: KeyEvent) {
		when (e.keyCode) {
			KeyEvent.VK_U -> {
				game.stopGameThread()
				game.undoAndRefresh()
			}

			KeyEvent.VK_E -> {
				val m = game.moveHistory[0]
				val value = game.getRules().evaluate(game)
				println("EVALUATION [$value] for move:$m")
				(game.currentPlayer as UIPlayer?)!!.forceRebuildMovesList(game)
			}
			KeyEvent.VK_R -> {
				game.startGameThread()
			}
		}
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			AGraphics.DEBUG_ENABLED = true
			Utils.setDebugEnabled()
			AWTCheckerboard()
		}
	}
}