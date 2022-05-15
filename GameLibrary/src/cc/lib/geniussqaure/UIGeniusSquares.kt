package cc.lib.geniussqaure

import cc.lib.game.*
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.utils.Grid

abstract class UIGeniusSquares : GeniusSquares() {
	var WIDTH = 0
	var HEIGHT = 0
	var DIM = 0
	var CELL_DIM = 0
	var BOARD_DIM = 0
	var highlighted: Piece? = null
	var pickedCell: IntArray? = null
	var dragging = false
	var isAutoFitPieces = true
	abstract fun repaint()
	@Synchronized
	fun paint(g: APGraphics, mx: Int, my: Int) {
		val PREFIX = "paint($mx,$my)"
		//log.info("paint x=" + mx + " y=" + my);
		WIDTH = g.viewportWidth
		HEIGHT = g.viewportHeight
		DIM = Math.min(WIDTH, HEIGHT)
		CELL_DIM = DIM / (BOARD_DIM_CELLS + 2)
		BOARD_DIM = CELL_DIM * BOARD_DIM_CELLS
		if (WIDTH > HEIGHT) {
			g.pushMatrix()
			g.translate((WIDTH - DIM + CELL_DIM).toFloat(), CELL_DIM.toFloat())
			g.scale(CELL_DIM.toFloat())
			if (pickedCell != null && !dragging) {
				highlighted?.let { highlighted ->
					liftPiece(highlighted)
					val drop = findDropForPiece2(highlighted, pickedCell!![0], pickedCell!![1])
					if (drop != null) {
						dropPiece(highlighted, drop[1], drop[2])
					}
				}
			}
			pickedCell = drawBoard(g, mx, my)
			//log.info(PREFIX + "pickedCell: " + Arrays.toString(pickedCell));
			g.popMatrix()
			g.pushMatrix()
			g.scale(CELL_DIM.toFloat())
			if (highlighted == null || !dragging) {
				highlighted = pickPieces(g, mx, my)
			}
			drawPiecesBeveled(g)
			if (highlighted == null) pickedCell?.let { pickedCell ->
				val color = board.get(pickedCell[1], pickedCell[0])
				//System.out.println("color = " + color);
				if (color > PieceType.PIECE_0.ordinal && color < PieceType.PIECE_CHIT.ordinal) {
					highlighted = pieces[color - 1]
				}
			}
			highlighted?.let { highlighted ->
				log.info(PREFIX + "highlighted = " + highlighted)
				g.pushMatrix()
				g.color = GColor.WHITE
				val useFind = true
				if (dragging) {
					liftPiece(highlighted)
					val pt = g.screenToViewport(mx, my)
					highlighted.center = pt
					pickedCell?.let { pickedCell ->
						run { // sumtin to break out of
							if (useFind) {
								findDropForPiece2(highlighted, pickedCell[0], pickedCell[1])?.let { result ->
									log.info(PREFIX + "Droppable at: " + result[1] + "," + result[2] + " index:" + result[0])
									highlighted.setIndex(result[0])
									dropPiece(highlighted, result[1], result[2])
									return@run
								}
							}

							if (!useFind && canDropPiece(highlighted, pickedCell[0], pickedCell[1])) {
								dropPiece(highlighted, pickedCell[0], pickedCell[1])
							} else {
								g.color = GColor.RED
							}
						}
					}
				}
				if (!highlighted.dropped) {
					g.pushMatrix()
					g.translate(highlighted.center)
					g.scale(1.03f)
					g.translate(-highlighted.width / 2, -highlighted.height / 2)
					renderPiece(g, highlighted)
					g.drawFilledRects()
					g.popMatrix()
					g.end()
					drawPieceBeveled(g, highlighted)
				}
				g.popMatrix()
			}
			g.popMatrix()
			g.textHeight = (CELL_DIM / 2).toFloat()
			timer.capture()
			var timeSecs = (timer.time / 1000).toInt()
			val timeMins = timeSecs / 60
			timeSecs -= timeMins * 60
			var bestTimeStr = ""
			if (bestTime > 0) {
				var bestTimeSecs = (bestTime / 1000).toInt()
				val bestTimeMins = bestTimeSecs / 60
				bestTimeSecs -= bestTimeMins * 60
				bestTimeStr = String.format("   %sBEST %02d:%02d", GColor.WHITE.toString(), bestTimeMins, bestTimeSecs)
			}
			var timeColor = GColor.GREEN
			if (bestTime > 0 && timer.time > bestTime) timeColor = GColor.RED
			val curTimeStr = String.format("%sTIME %02d:%02d", timeColor.toString(), timeMins, timeSecs)
			g.drawAnnotatedString(curTimeStr + bestTimeStr, (WIDTH - BOARD_DIM - CELL_DIM).toFloat(), (CELL_DIM / 5).toFloat())
			pickedCell?.takeIf { false }?.let { pickedCell ->
				g.color = GColor.WHITE
				var hl = ""
				highlighted?.let { highlighted ->
					hl = highlighted.pieceType.name + " " + (if (highlighted.dropped) "v" else "^") + " "
				}
				g.drawJustifiedString((WIDTH - 10).toFloat(), 10f, Justify.RIGHT, Justify.TOP, hl + "pickedCell: " + pickedCell[0] + "," + pickedCell[1])
			}
		} else {
			g.drawJustifiedString((WIDTH / 2).toFloat(), (HEIGHT / 2).toFloat(), Justify.CENTER, Justify.CENTER, "Portrait not supported")
		}
		if (isCompleted) {
			if (!endgameAnim.isStarted)
				endgameAnim.start<AAnimation<AGraphics>>()
		} else {
			endgameAnim.kill()
		}
		if (endgameAnim.isStarted) {
			endgameAnim.update(g)
			repaint()
		}
	}

	var endgameAnim: AAnimation<AGraphics> = object : AAnimation<AGraphics>(2000, -1, true) {
		override fun draw(g: AGraphics, position: Float, dt: Float) {
			val hgt = g.viewport.height/5
			g.textHeight = hgt + position * hgt
			g.color = GColor.MAGENTA
			g.drawJustifiedString((WIDTH / 2).toFloat(), (HEIGHT / 2).toFloat(), Justify.CENTER, Justify.CENTER, "COMPLETED")
		}
	}

	@Synchronized
	override fun newGame() {
		super.newGame()
		endgameAnim.kill()
	}

	// return row/col for mx, my
	private fun drawBoard(g: APGraphics, mx: Int, my: Int): IntArray? {
		var picked: IntArray? = null
		g.color = GColor.BLACK
		g.drawFilledRect(0f, 0f, BOARD_DIM_CELLS.toFloat(), BOARD_DIM_CELLS.toFloat())
		g.color = GColor.WHITE
		g.setLineWidth(3f)
		for (i in 0..BOARD_DIM_CELLS) {
			g.drawLine(0f, i.toFloat(), BOARD_DIM_CELLS.toFloat(), i.toFloat())
			g.drawLine(i.toFloat(), 0f, i.toFloat(), BOARD_DIM_CELLS.toFloat())
		}
		for (y in 0 until BOARD_DIM_CELLS) {
			for (x in 0 until BOARD_DIM_CELLS) {
				g.pushMatrix()
				if (picked == null) {
					g.begin()
					g.setName(1)
					g.vertex(x.toFloat(), y.toFloat())
					g.vertex((x + 1).toFloat(), (y + 1).toFloat())
					if (1 == g.pickRects(mx, my)) {
						picked = intArrayOf(x, y)
					}
				}
				val pt = PieceType.values()[board.get(y,x)]
				when (pt) {
					PieceType.PIECE_0 -> {
					}
					PieceType.PIECE_CHIT -> {
						g.color = pt.color
						g.drawFilledCircle(x + 0.5f, y + 0.5f, 0.4f)
					}
					else                 -> {
						drawCellBeveled(g, board, Vector2D(0f, 0f), x, y)
					}
				}
				g.popMatrix()
			}
		}
		return picked
	}

	private fun pickPieces(g: APGraphics, mx: Int, my: Int): Piece? {
		var picked: Piece? = null
		for (p in pieces) {
			g.color = p.pieceType.color
			g.pushMatrix()
			g.translate(p.topLeft)
			g.begin()
			g.setName(1)
			renderPiece(g, p)
			if (g.pickRects(mx, my) == 1) {
				picked = p
			}
			g.end()
			g.popMatrix()
		}
		return picked
	}

	private fun renderPiece(g: AGraphics, p: Piece) {
		val w = p.width / 2
		val h = p.height / 2
		g.pushMatrix()
		//g.translate(-w, -h);
		val cells = p.shape
		for (cr in cells.indices) {
			for (cc in cells[0].indices) {
				if (cells[cr][cc] == 0) continue
				g.vertex(cc.toFloat(), cr.toFloat())
				g.vertex((cc + 1).toFloat(), (cr + 1).toFloat())
			}
		}
		g.popMatrix()
	}

	val BEVEL_INSET = 0.25f
	val BEVEL_PADDING = 0.05f
	fun drawPieceBeveled(g: AGraphics, p: Piece) { //int [][] shape, Vector2D _topLeft, GColor color) {
		val shape = p.shape
		for (y in shape.indices) {
			for (x in shape[y].indices) {
				if (shape[y][x] == 0)
					continue
				g.pushMatrix()
				drawCellBeveled(g, Grid(shape), p.getTopLeft(), x, y)
				g.popMatrix()
			}
		}
	}

	// wow programmatic beveling harder than I expected
	fun drawCellBeveled(g: AGraphics, matrix: Grid<Int>, _topLeft: Vector2D, x: Int, y: Int) {
		val cell = matrix.get(y, x)
		val topLeft = MutableVector2D(_topLeft).addEq(x.toFloat(), y.toFloat())
		val topRight = MutableVector2D(topLeft).addEq(1f, 0f)
		val bottomLeft = MutableVector2D(topLeft).addEq(0f, 1f)
		val bottomRight = MutableVector2D(topLeft).addEq(1f, 1f)
		val topLeftBevel = MutableVector2D(_topLeft).add(x.toFloat(), y.toFloat())
		val topRightBevel = MutableVector2D(topLeftBevel).addEq(1f, 0f)
		val bottomLeftBevel = MutableVector2D(topLeftBevel).addEq(0f, 1f)
		val bottomRightBevel = MutableVector2D(topLeftBevel).addEq(1f, 1f)
		var bevel = 0
		if (getCellAt(y - 1, x, matrix) != cell) {
			// draw 'top' for this cell with beveled
			topLeft.addEq(0f, BEVEL_PADDING)
			topRight.addEq(0f, BEVEL_PADDING)
			topLeftBevel.addEq(0f, BEVEL_INSET)
			topRightBevel.addEq(0f, BEVEL_INSET)
		} else {
			bevel = bevel or BEVEL_TOP
		}
		if (getCellAt(y + 1, x, matrix) != cell) {
			// draw 'bottom' for this cell with beveled
			bottomLeft.subEq(0f, BEVEL_PADDING)
			bottomRight.subEq(0f, BEVEL_PADDING)
			bottomLeftBevel.subEq(0f, BEVEL_INSET)
			bottomRightBevel.subEq(0f, BEVEL_INSET)
		} else {
			bevel = bevel or BEVEL_BOTTOM
		}
		if (getCellAt(y, x - 1, matrix) != cell) {
			// draw 'left' for this cell with beveled
			topLeft.addEq(BEVEL_PADDING, 0f)
			bottomLeft.addEq(BEVEL_PADDING, 0f)
			topLeftBevel.addEq(BEVEL_INSET, 0f)
			bottomLeftBevel.addEq(BEVEL_INSET, 0f)
		} else {
			bevel = bevel or BEVEL_LEFT
		}
		if (getCellAt(y, x + 1, matrix) != cell) {
			// draw 'right' for this cell with beveled
			topRight.subEq(BEVEL_PADDING, 0f)
			bottomRight.subEq(BEVEL_PADDING, 0f)
			topRightBevel.subEq(BEVEL_INSET, 0f)
			bottomRightBevel.subEq(BEVEL_INSET, 0f)
		} else {
			bevel = bevel or BEVEL_RIGHT
		}
		val topColor = PieceType.values()[cell].color.lightened(.3f)
		val cntrColor = topColor.darkened(.1f)
		val leftColor = topColor.darkened(.2f)
		val rightColor = topColor.darkened(.3f)
		val bottomColor = topColor.darkened(.4f)
		// top
		g.begin()
		g.color = topColor
		g.vertex(topLeft)
		g.vertex(topRight)
		g.vertex(topLeftBevel)
		g.vertex(topRightBevel)
		g.drawQuadStrip()
		// right
		g.begin()
		g.color = rightColor
		g.vertex(topRight)
		g.vertex(bottomRight)
		g.vertex(topRightBevel)
		g.vertex(bottomRightBevel)
		g.drawQuadStrip()
		// bottom
		g.begin()
		g.color = bottomColor
		g.vertex(bottomRight)
		g.vertex(bottomLeft)
		g.vertex(bottomRightBevel)
		g.vertex(bottomLeftBevel)
		g.drawQuadStrip()
		// left
		g.begin()
		g.color = leftColor
		g.vertex(bottomLeft)
		g.vertex(topLeft)
		g.vertex(bottomLeftBevel)
		g.vertex(topLeftBevel)
		g.drawQuadStrip()
		val bevelTopLeft = bevel and BEVEL_LEFT_TOP == BEVEL_LEFT_TOP && getCellAt(y - 1, x - 1, matrix) != cell
		val bevelTopRight = bevel and BEVEL_RIGHT_TOP == BEVEL_RIGHT_TOP && getCellAt(y - 1, x + 1, matrix) != cell
		val bevelBottomLeft = bevel and BEVEL_LEFT_BOTTOM == BEVEL_LEFT_BOTTOM && getCellAt(y + 1, x - 1, matrix) != cell
		val bevelBottomRight = bevel and BEVEL_RIGHT_BOTTOM == BEVEL_RIGHT_BOTTOM && getCellAt(y + 1, x + 1, matrix) != cell
		g.begin()
		if (bevelTopLeft) {
			topLeftBevel.addEq(BEVEL_INSET, BEVEL_INSET)
		}
		if (bevelTopRight) {
			topRightBevel.addEq(-BEVEL_INSET, BEVEL_INSET)
		}
		if (bevelBottomRight) {
			bottomRightBevel.addEq(-BEVEL_INSET, -BEVEL_INSET)
		}
		if (bevelBottomLeft) {
			bottomLeftBevel.addEq(BEVEL_INSET, -BEVEL_INSET)
		}
		g.begin()
		g.color = cntrColor
		if (bevelTopLeft || bevelTopRight) {
			// top
			g.vertex(topLeftBevel.min(topRightBevel).setY(topLeft.y))
			g.vertex(topLeftBevel.max(topRightBevel))
		}
		if (bevelTopLeft || bevelBottomLeft) {
			// left
			g.vertex(topLeftBevel.min(bottomLeftBevel).setX(topLeft.x))
			g.vertex(topLeftBevel.max(bottomLeftBevel))
		}
		if (bevelTopRight || bevelBottomRight) {
			// right
			g.vertex(topRightBevel.min(bottomRightBevel))
			g.vertex(topRightBevel.max(bottomRightBevel).setX(bottomRight.x))
		}
		if (bevelBottomLeft || bevelBottomRight) {
			// bottom
			g.vertex(bottomLeftBevel.min(bottomRightBevel))
			g.vertex(bottomLeftBevel.max(bottomRightBevel).setY(bottomRight.y))
		}
		g.drawFilledRects()
		g.begin()
		// center
		g.vertex(topLeftBevel)
		g.vertex(topRightBevel)
		g.vertex(bottomLeftBevel)
		g.vertex(bottomRightBevel)
		g.drawQuadStrip()

		// check the corners. This draws the little 'L' in the corner(s)
		// top/left corner
		if (bevelTopLeft) {
			g.begin()
			g.color = topColor
			g.vertex(topLeft.x, topLeft.y + BEVEL_PADDING)
			g.vertex(topLeft.x, topLeft.y + BEVEL_INSET)
			g.vertex(topLeft.x + BEVEL_PADDING, topLeft.y + BEVEL_PADDING)
			g.vertex(topLeft.x + BEVEL_INSET, topLeft.y + BEVEL_INSET)
			g.drawQuadStrip()
			g.begin()
			g.color = leftColor
			g.vertex(topLeft.x + BEVEL_PADDING, topLeft.y)
			g.vertex(topLeft.x + BEVEL_INSET, topLeft.y)
			g.vertex(topLeft.x + BEVEL_PADDING, topLeft.y + BEVEL_PADDING)
			g.vertex(topLeft.x + BEVEL_INSET, topLeft.y + BEVEL_INSET)
			g.drawQuadStrip()
		}

		// top/right corner
		if (bevelTopRight) {
			g.begin()
			g.color = topColor
			g.vertex(topRight.x, topRight.y + BEVEL_PADDING)
			g.vertex(topRight.x, topRight.y + BEVEL_INSET)
			g.vertex(topRight.x - BEVEL_PADDING, topRight.y + BEVEL_PADDING)
			g.vertex(topRight.x - BEVEL_INSET, topRight.y + BEVEL_INSET)
			g.drawQuadStrip()
			g.begin()
			g.color = rightColor
			g.vertex(topRight.x - BEVEL_PADDING, topRight.y)
			g.vertex(topRight.x - BEVEL_INSET, topRight.y)
			g.vertex(topRight.x - BEVEL_PADDING, topRight.y + BEVEL_PADDING)
			g.vertex(topRight.x - BEVEL_INSET, topRight.y + BEVEL_INSET)
			g.drawQuadStrip()
		}
		// bottom/right corner
		if (bevelBottomRight) {
			g.begin()
			g.color = bottomColor
			g.vertex(bottomRight.x, bottomRight.y - BEVEL_PADDING)
			g.vertex(bottomRight.x, bottomRight.y - BEVEL_INSET)
			g.vertex(bottomRight.x - BEVEL_PADDING, bottomRight.y - BEVEL_PADDING)
			g.vertex(bottomRight.x - BEVEL_INSET, bottomRight.y - BEVEL_INSET)
			g.drawQuadStrip()
			g.begin()
			g.color = rightColor
			g.vertex(bottomRight.x - BEVEL_PADDING, bottomRight.y)
			g.vertex(bottomRight.x - BEVEL_INSET, bottomRight.y)
			g.vertex(bottomRight.x - BEVEL_PADDING, bottomRight.y - BEVEL_PADDING)
			g.vertex(bottomRight.x - BEVEL_INSET, bottomRight.y - BEVEL_INSET)
			g.drawQuadStrip()
		}
		// bottom/left corner
		if (bevelBottomLeft) {
			g.begin()
			g.color = bottomColor
			g.vertex(bottomLeft.x, bottomLeft.y - BEVEL_PADDING)
			g.vertex(bottomLeft.x, bottomLeft.y - BEVEL_INSET)
			g.vertex(bottomLeft.x + BEVEL_PADDING, bottomLeft.y - BEVEL_PADDING)
			g.vertex(bottomLeft.x + BEVEL_INSET, bottomLeft.y - BEVEL_INSET)
			g.drawQuadStrip()
			g.begin()
			g.color = leftColor
			g.vertex(bottomLeft.x + BEVEL_PADDING, bottomLeft.y)
			g.vertex(bottomLeft.x + BEVEL_INSET, bottomLeft.y)
			g.vertex(bottomLeft.x + BEVEL_PADDING, bottomLeft.y - BEVEL_PADDING)
			g.vertex(bottomLeft.x + BEVEL_INSET, bottomLeft.y - BEVEL_INSET)
			g.drawQuadStrip()
		}
		g.end()
	}

	fun drawPiecesBeveled(g: AGraphics) {
		for (p in pieces) {
			if (p.dropped || p === highlighted) continue
			drawPieceBeveled(g, p)
		}
	}

	@Synchronized
	fun doClick() {
		log.info("doClick highlighted=$highlighted")
		highlighted?.let {
			it.increment(1)
			log.info("doClick: incremented:$it")
			repaint()
		}
	}

	@Synchronized
	fun startDrag() {
		log.info("Stop Drag")
		highlighted?.let {
			dragging = true
			repaint()
		}
	}

	@Synchronized
	fun stopDrag() {
		log.info("Stop Drag")
		dragging = false
		repaint()
	}

	companion object {
		/**
		 * Return -1 for off board or the cell assignment at row, col
		 * @param row
		 * @param col
		 * @param rowMajorMatrix
		 * @return
		 */
		fun getCellAt(row: Int, col: Int, rowMajorMatrix: Grid<Int>): Int {
			with (Grid.Pos(row, col)) {
				if (rowMajorMatrix.isOnGrid(this))
					return rowMajorMatrix.get(this)
			}
			return -1
		}

		private const val BEVEL_TOP = 1
		private const val BEVEL_RIGHT = 1 shl 1
		private const val BEVEL_BOTTOM = 1 shl 2
		private const val BEVEL_LEFT = 1 shl 3
		private const val BEVEL_LEFT_TOP = BEVEL_LEFT or BEVEL_TOP
		private const val BEVEL_RIGHT_TOP = BEVEL_RIGHT or BEVEL_TOP
		private const val BEVEL_RIGHT_BOTTOM = BEVEL_RIGHT or BEVEL_BOTTOM
		private const val BEVEL_LEFT_BOTTOM = BEVEL_BOTTOM or BEVEL_LEFT
	}
}