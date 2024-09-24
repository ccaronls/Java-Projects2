package cc.lib.swing

import cc.lib.checkers.ACheckboardGame
import cc.lib.checkers.ACheckboardGame.BoardType
import cc.lib.checkers.Checkers
import cc.lib.checkers.Dama
import cc.lib.checkers.Draughts
import cc.lib.checkers.Move
import cc.lib.checkers.MoveType
import cc.lib.checkers.Piece
import cc.lib.checkers.PieceType
import cc.lib.checkers.Robot
import cc.lib.game.AGraphics
import cc.lib.game.DescisionTree
import cc.lib.game.GColor
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.reflector.Reflector
import cc.lib.utils.FileUtils
import cc.lib.utils.GException
import java.awt.event.KeyEvent
import java.io.File
import java.io.PrintWriter

class AWTCheckers internal constructor() : AWTComponent() {
	lateinit var game: ACheckboardGame
	val frame: AWTFrame
	lateinit var SAVE_FILE: File
	var robot: Robot? = null

	internal inner class AWTRobot(difficulty: Int) : Robot(difficulty) {
		override fun onNewMove(m: Move) {
			super.onNewMove(m)
			repaint()
		}
	}

	internal enum class GameType {
		Checkers {
			override fun create(): ACheckboardGame {
				return Checkers()
			}
		},  /*
        Chess {
            @Override
            ACheckboardGame create() {
                return new Chess();
            }
        },*/
		Draughts {
			override fun create(): ACheckboardGame {
				return Draughts()
			}
		},
		Dama {
			override fun create(): ACheckboardGame {
				return Dama()
			}
		};

		abstract fun create(): ACheckboardGame
	}

	val isRobotTurn: Boolean
		get() = robot != null && game.turn == 1

	@Synchronized
	fun onFileMenu(item: String?) {
		when (item) {
			"New Game" -> {
				val index = frame.showItemChooserDialog("New Game", "Choose Game Type", null, *Utils.toStringArray(GameType.values(), true))
				if (index >= 0) {
					val num = frame.showItemChooserDialog("New Game", "Choose Single or Multipllayer", "Single", "Multi")
					if (num < 0)
						return
					robot = null
					when (num) {
						0 ->                             // single
							robot = AWTRobot(1)
						1 -> {}
					}
					game = GameType.values()[index].create()
					game.newGame()
					game.singlePlayerDifficulty = -1
					if (robot != null) game.singlePlayerDifficulty = robot!!.difficulty.ordinal
				}
			}
			"Load Game", "Save as" -> {}
		}
	}

	var selectedRank = -1
	var selectedCol = -1
	var highlightedRank = 0
	var highlightedCol = 0
	var selectedMove: Move? = null
	val CHECKER_HEIGHT = 0.2f
	private fun drawCheckerKing(g: AGraphics, outlineColor: GColor?) {
		if (outlineColor != null) {
			val c = g.color
			g.color = outlineColor
			g.pushMatrix()
			drawChecker(g, outlineColor != null)
			g.translate(0f, -CHECKER_HEIGHT)
			drawChecker(g, outlineColor != null)
			g.popMatrix()
			g.color = c
		}
		g.pushMatrix()
		drawChecker(g, false)
		g.translate(0f, -CHECKER_HEIGHT)
		drawChecker(g, false)
		g.popMatrix()
	}

	private fun drawChecker(g: AGraphics, outlined: Boolean) {
		val vr = 2 - CHECKER_HEIGHT
		val c = g.color
		if (!outlined) g.color = c.darkened(0.5f)
		g.drawFilledOval(-1f, 1f - vr, 2f, vr)
		g.drawFilledRect(-1f, -CHECKER_HEIGHT / 2, 2f, CHECKER_HEIGHT)
		g.color = c
		g.drawFilledOval(-1f, -1f, 2f, vr)
	}

	/*
        float hgt = CHECKER_HEIGHT;
        GColor c = g.getColor();
        if (!outlined)
            g.setColor(c.darkened(0.5f));
        g.drawFilledOval(-1f, -hgt/2, 2, 2-hgt);
        g.drawFilledRect(-1f, -hgt/2, 2, hgt);
        g.setColor(c);
        g.drawFilledOval(-1f, -1f, 2, 2-hgt);
    }*/
	var cellWidth = 1f
	var cellHeight = 1f

	init {
		frame = object : AWTFrame() {
			override fun onMenuItemSelected(menu: String, subMenu: String) {
				when (menu) {
					"File" -> onFileMenu(subMenu)
					else -> super.onMenuItemSelected(menu, subMenu)
				}
			}

			override fun onWindowClosing() {
				if (game != null) {
					try {
						Reflector.serializeToFile<Any>(game, SAVE_FILE)
					} catch (e: Exception) {
						e.printStackTrace()
					}
				}
			}
		}
		setMouseEnabled(true)
		setPadding(10)
		frame.addMenuBarMenu("File", "New Game", "Load Game", "Save as")
		frame.add(this)
		val settings = FileUtils.getOrCreateSettingsDirectory(javaClass)
		if (!frame.loadFromFile(File(settings, "checkers.properties"))) frame.centerToScreen(640, 640)
		SAVE_FILE = File(settings, "checkers.save")
		try {
			if (SAVE_FILE.exists()) {
				game = Reflector.deserializeFromFile(SAVE_FILE)
				if (game.singlePlayerDifficulty >= 0) {
					robot = AWTRobot(game.singlePlayerDifficulty)
					checkRobotTurn()
				} else {
					robot = null
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		if (game == null) {
			game = Checkers()
			game.newGame()
			game.singlePlayerDifficulty = 1
			game.turn = 0
			robot = AWTRobot(1)
		}
	}

	@Synchronized
	override fun paint(g: AWTGraphics) {
		//Utils.println("RENDER");
		var mouseX = mouseX
		var mouseY = mouseY
		g.pushMatrix()
		if (game != null) try {
			val dim = Math.min(width, height)
			val sx = width / 2 - dim / 2
			val sy = height / 2 - dim / 2
			g.translate(sx.toFloat(), sy.toFloat())
			cellWidth = (dim / game.COLUMNS).toFloat()
			cellHeight = (dim / game.RANKS).toFloat()
			when (game.boardType) {
				BoardType.CHECKERS -> {
					val colors = arrayOf(
						GColor.LIGHT_GRAY,
						GColor.BLACK
					)
					var cIndex = 0
					var i = 0
					while (i < game.COLUMNS) {
						var ii = 0
						while (ii < game.RANKS) {
							g.color = colors[cIndex]
							g.drawFilledRect(i * cellWidth, ii * cellHeight, cellWidth, cellHeight)
							cIndex = (cIndex + 1) % colors.size
							ii++
						}
						cIndex = (cIndex + 1) % colors.size
						i++
					}
				}
				BoardType.DAMA -> {
					g.color = DAMA_BOARD_COLOR
					g.drawFilledRect(0f, 0f, dim.toFloat(), dim.toFloat())
					g.color = GColor.BLACK
					run {
						var i = 0
						while (i <= game.COLUMNS) {
							g.drawLine(i * cellWidth, 0f, i * cellWidth, dim.toFloat(), 5f)
							i++
						}
					}
					var i = 0
					while (i <= game.RANKS) {
						g.drawLine(0f, i * cellHeight, dim.toFloat(), i * cellHeight, 5f)
						i++
					}
				}
			}

			// draw the pieces and figure out what rect the mouse is over
			highlightedCol = -1
			highlightedRank = highlightedCol
			if (isRobotTurn) {
				return
			}
			mouseX -= sx
			mouseY -= sy
			var highlightedPiece: Piece? = null
			for (r in 0 until game.RANKS) {
				for (c in 0 until game.COLUMNS) {
					val x = c * cellWidth
					val y = r * cellHeight
					val p = game.getPiece(r, c)
					if (p.playerNum >= 0) drawPiece(g, p, x, y, null)
					if (Utils.isPointInsideRect(mouseX.toFloat(), mouseY.toFloat(), x, y, cellWidth, cellHeight)) {
						highlightedCol = c
						highlightedRank = r
						if (p.playerNum >= 0) highlightedPiece = p
						g.color = GColor.GREEN
						g.drawRect(x, y, cellWidth, cellHeight, 5f)
					}
				}
			}
			selectedMove = null
			if (selectedCol >= 0 && selectedRank >= 0) {
				val p = game.getPiece(selectedRank, selectedCol)
				if (p.playerNum == game.turn) {
					for (m in p.moves) {
						if (m.hasEnd()) {
							g.color = GColor.GREEN
							g.drawCircle(m.end[1] * cellWidth + cellWidth / 2, m.end[0] * cellHeight + cellHeight / 2, Math.min(cellWidth, cellHeight) * 1 / 3 + 2, 5f)
							if (highlightedRank == m.end[0] && highlightedCol == m.end[1]) selectedMove = m
							println("selected move = $m")
						}
					}
				}
			} else {
				// draw the movable pieces
				g.color = GColor.CYAN
				for (m in game.getMoves()) {
					val pos = m.start
					if (pos != null) {
						val y = pos[0] * cellHeight
						val x = pos[1] * cellWidth
						//g.drawCircleWithThickness(x + cellWidth / 2, y + cellHeight / 2, Math.min(cellWidth, cellHeight) * 1 / 3 + 2, 5);
						val p = game.getPiece(pos[0], pos[1])
						if (p.a != null) {
							p.a.update(g)
							if (p.a.isDone) p.a = null
						} else {
							drawPiece(g, p, x, y, GColor.CYAN)
							drawPiece(g, p, x, y, null)
						}
					}
				}
			}
			if (highlightedPiece != null) {
				val x = cellWidth * highlightedCol
				val y = cellHeight * highlightedRank
				drawPiece(g, highlightedPiece, x, y, null)
			}
			/*
            for (int r=0; r<game.RANKS; r++) {
                for (int c=0; c<game.COLUMNS; c++) {
                    float x = c*cellWidth;
                    float y = r*cellHeight;

                    Piece p = game.getPiece(r, c);
                    if (p.getPlayerNum() >= 0)
                        drawPiece(g, p, x, y, false);

                    if (Utils.isPointInsideRect(mouseX, mouseY, x, y, cellWidth, cellHeight)) {
                        highlightedCol = c;
                        highlightedRank = r;
                        g.setColor(GColor.GREEN);
                        g.drawRect(x, y, cellWidth, cellHeight, 5);
                    }

                }
            }*/for (m in game.getMoves()) {
				if (m.moveType == MoveType.END) {
					g.color = GColor.YELLOW
					g.drawJustifiedString((g.viewportWidth / 2).toFloat(), (g.viewportHeight / 2).toFloat(), Justify.CENTER, Justify.CENTER, "Press 'e' to end turn")
					break
				}
			}
		} finally {
			g.popMatrix()
			if (isRobotTurn) {
				g.color = GColor.GREEN
				g.drawJustifiedString((g.viewportWidth / 2).toFloat(), (g.viewportHeight / 2).toFloat(), Justify.CENTER, Justify.CENTER, "THINKING")
			}
		}
	}

	fun getColor(playerNum: Int): GColor {
		when (playerNum) {
			0 -> return GColor.RED
			1 -> return GColor.BLUE
		}
		throw GException("Unhandled Case")
	}

	fun drawPiece(g: AGraphics, p: Piece, x: Float, y: Float, outlineColor: GColor?) {
		if (outlineColor == null && p.isCaptured) {
			drawPiece(g, p, x, y, GColor.PINK)
		}
		g.pushMatrix()
		val w = cellWidth
		val h = cellHeight
		g.translate(x + w / 2, y + h / 2)
		g.scale(Math.min(w, h) * 1 / 3)
		g.color = getColor(p.playerNum)
		if (outlineColor != null) {
			g.color = outlineColor
			g.scale(1.2f)
		}
		when (p.type) {
			PieceType.EMPTY -> {}
			PieceType.PAWN, PieceType.PAWN_IDLE, PieceType.PAWN_ENPASSANT, PieceType.PAWN_TOSWAP -> g.drawJustifiedString(0f, 0f, Justify.CENTER, Justify.CENTER, "PAWN")
			PieceType.BISHOP -> g.drawJustifiedString(0f, 0f, Justify.CENTER, Justify.CENTER, "BSHP")
			PieceType.KNIGHT -> g.drawJustifiedString(0f, 0f, Justify.CENTER, Justify.CENTER, "KNGT")
			PieceType.ROOK, PieceType.ROOK_IDLE -> g.drawJustifiedString(0f, 0f, Justify.CENTER, Justify.CENTER, "ROOK")
			PieceType.QUEEN -> g.drawJustifiedString(0f, 0f, Justify.CENTER, Justify.CENTER, "QUEN")
			PieceType.CHECKED_KING, PieceType.CHECKED_KING_IDLE, PieceType.UNCHECKED_KING, PieceType.UNCHECKED_KING_IDLE -> g.drawJustifiedString(0f, 0f, Justify.CENTER, Justify.CENTER, "KING")
			PieceType.KING, PieceType.FLYING_KING, PieceType.DAMA_KING -> drawCheckerKing(g, outlineColor)
			PieceType.CHECKER, PieceType.DAMA_MAN -> drawChecker(g, outlineColor != null)
			PieceType.UNAVAILABLE -> {}
		}
		g.popMatrix()
	}

	fun checkRobotTurn() {
		if (isRobotTurn) {
			object : Thread() {
				override fun run() {
					val g = game.deepCopy()
					val root = DescisionTree(0)
					robot!!.doRobot(g, root)
					root.dumpTreeXML(PrintWriter(System.out))
					/*
                    for (Move m : root.getPath()) {
                        animateMove(m);
                        game.executeMove(m);
                    }*/repaint()
					Utils.print("Thread " + currentThread().name + " DONE")
				}
			}.start()
		}
	}

	fun animateMove(m: Move) {
		/*
		val a : AAnimation<AGraphics> = when (m.moveType) {
			MoveType.SLIDE -> {
				object : AAnimation<AGraphics>(1000L) {
					override fun draw(g: AGraphics, position: Float, dt: Float) {
						val start = m.start
						val end = m.end
						val p = game.getPiece(start)
						val sx = start[1].toFloat()
						val sy = start[0].toFloat()
						val ex = end[1].toFloat()
						val ey = end[0].toFloat()
						val x = (ex - sx) * position
						val y = (ey - sy) * position
						g.pushMatrix()
						g.translate(x, y)
						drawPiece(g, p, x, y, null)
					}

					override fun onDone() {
						synchronized(this) { notify() }
					}
				}
				
			}
			
		}?.let {
			
		}
		if (a != null) {
			a.start<AAnimation<AGraphics>>()
			repaint()
			Utils.waitNoThrow(a, 2000)
		}*/
	}

	override fun onClick() {
		if (selectedMove != null && highlightedRank == selectedMove!!.end[0] && highlightedCol == selectedMove!!.end[1]) {
			game.executeMove(selectedMove)
			highlightedCol = -1
			highlightedRank = highlightedCol
			selectedRank = highlightedRank
			selectedCol = selectedRank
			checkRobotTurn()
		} else if (selectedRank >= 0 && selectedCol >= 0) {
			selectedCol = -1
			selectedRank = selectedCol
		} else {
			val p = game.getPiece(highlightedRank, highlightedCol)
			if (p != null && p.playerNum == game.turn) {
				selectedRank = highlightedRank
				selectedCol = highlightedCol
			}
		}
		repaint()
	}

	override fun onKeyPressed(e: KeyEvent) {
		if (game.turn == 1 && robot != null) return
		when (e.keyCode) {
			KeyEvent.VK_B -> if (game.canUndo()) game.undo()
			KeyEvent.VK_E -> {
				for (m in game.getMoves()) {
					if (m.moveType == MoveType.END) {
						game.executeMove(m)
						checkRobotTurn()
						break
					}
				}
			}
			KeyEvent.VK_UP -> if (highlightedRank < game.RANKS - 1) highlightedRank++
			KeyEvent.VK_DOWN -> if (highlightedRank > 0) highlightedRank--
			KeyEvent.VK_LEFT -> if (highlightedCol > 0) highlightedCol--
			KeyEvent.VK_RIGHT -> if (highlightedCol < game.COLUMNS - 1) highlightedCol++
			KeyEvent.VK_ENTER -> onClick()
		}
		repaint()
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			Utils.setDebugEnabled()
			AWTCheckers()
		}

		val DAMA_BOARD_COLOR = GColor(-0x21657)
	}
}