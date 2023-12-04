package cc.game.dominos.core

import cc.lib.game.*
import cc.lib.logger.LoggerFactory
import cc.lib.math.Matrix3x3
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.reflector.Omit
import cc.lib.reflector.Reflector
import java.util.*

/**
 * Created by chriscaron on 2/1/18.
 *
 * Representation of a Dominos board.
 *
 */
class Board : Reflector<Board?>() {
	@Omit
	private val log = LoggerFactory.getLogger(javaClass)

	companion object {
		const val EP_LEFT = 0
		const val EP_RIGHT = 1
		const val EP_UP = 2
		const val EP_DOWN = 3
		const val PLACMENT_FWD = 0
		const val PLACMENT_FWD_RIGHT = 1
		const val PLACMENT_FWD_LEFT = 2
		const val PLACMENT_RIGHT = 3
		const val PLACMENT_LEFT = 4
		const val PLACEMENT_COUNT = 5

		/**
		 *
		 * @param g
		 * @param pips1
		 * @param pips2
		 * @param alpha [0-1] inclusive
		 */
        @JvmStatic
        fun drawTile(g: AGraphics, pips1: Int, pips2: Int, alpha: Float) {
			g.pushMatrix()
			g.color = GColor.WHITE.withAlpha(alpha)
			// draw solid white round rect behind black to achieve outline effect
			// to get around android scaling the line width, ug.
			g.pushMatrix()
			g.translate(1f, 0.5f)
			g.scale(1.03f, 1.06f)
			g.translate(-1f, -0.5f)
			g.drawFilledRoundedRect(0f, 0f, 2f, 1f, 0.25f)
			g.popMatrix()
			g.color = GColor.BLACK.withAlpha(alpha)
			g.drawFilledRoundedRect(0f, 0f, 2f, 1f, 0.25f)
			g.color = GColor.WHITE
			g.drawLine(1f, 0f, 1f, 1f, 2f)
			g.color = GColor.WHITE
			drawDie(g, 0f, 0f, pips1)
			drawDie(g, 1f, 0f, pips2)
			if (false && AGraphics.DEBUG_ENABLED) {
				g.color = GColor.RED
				g.drawFilledCircle(0, 0, 4)
			}
			g.popMatrix()
		}

		@JvmStatic
        fun drawDie(g: AGraphics, x: Float, y: Float, numDots: Int) {
			val dd2 = 0.5f
			val dd4 = 0.25f
			val dd34 = 0.75f
			val dd5 = 0.2f
			val dd25 = 0.4f
			val dd35 = 0.6f
			val dd45 = 0.8f
			g.begin()
			when (numDots) {
				0 -> {
				}
				1 -> g.vertex(x + dd2, y + dd2)
				2 -> {
					g.vertex(x + dd4, y + dd4)
					g.vertex(x + dd34, y + dd34)
				}
				3 -> {
					g.vertex(x + dd4, y + dd4)
					g.vertex(x + dd2, y + dd2)
					g.vertex(x + dd34, y + dd34)
				}
				4 -> {
					g.vertex(x + dd4, y + dd4)
					g.vertex(x + dd34, y + dd34)
					g.vertex(x + dd4, y + dd34)
					g.vertex(x + dd34, y + dd4)
				}
				5 -> {
					g.vertex(x + dd4, y + dd4)
					g.vertex(x + dd34, y + dd34)
					g.vertex(x + dd4, y + dd34)
					g.vertex(x + dd34, y + dd4)
					g.vertex(x + dd2, y + dd2)
				}
				6 -> {
					g.vertex(x + dd4, y + dd4)
					g.vertex(x + dd34, y + dd34)
					g.vertex(x + dd4, y + dd34)
					g.vertex(x + dd34, y + dd4)
					g.vertex(x + dd4, y + dd2)
					g.vertex(x + dd34, y + dd2)
				}
				7 -> {
					g.vertex(x + dd2, y + dd2)
					g.vertex(x + dd4, y + dd4)
					g.vertex(x + dd34, y + dd34)
					g.vertex(x + dd4, y + dd34)
					g.vertex(x + dd34, y + dd4)
					g.vertex(x + dd4, y + dd2)
					g.vertex(x + dd34, y + dd2)
				}
				8 -> {
					g.vertex(x + dd2, y + dd4)
					g.vertex(x + dd2, y + dd34)
					g.vertex(x + dd4, y + dd4)
					g.vertex(x + dd34, y + dd34)
					g.vertex(x + dd4, y + dd34)
					g.vertex(x + dd34, y + dd4)
					g.vertex(x + dd4, y + dd2)
					g.vertex(x + dd34, y + dd2)
				}
				9 -> {
					g.vertex(x + dd2, y + dd2)
					g.vertex(x + dd2, y + dd4)
					g.vertex(x + dd2, y + dd34)
					g.vertex(x + dd4, y + dd4)
					g.vertex(x + dd34, y + dd34)
					g.vertex(x + dd4, y + dd34)
					g.vertex(x + dd34, y + dd4)
					g.vertex(x + dd4, y + dd2)
					g.vertex(x + dd34, y + dd2)
				}
				10 -> {
					g.vertex(x + dd4, y + dd5)
					g.vertex(x + dd4, y + dd25)
					g.vertex(x + dd4, y + dd35)
					g.vertex(x + dd4, y + dd45)
					g.vertex(x + dd2, y + dd5)
					g.vertex(x + dd2, y + dd45)
					g.vertex(x + dd34, y + dd5)
					g.vertex(x + dd34, y + dd25)
					g.vertex(x + dd34, y + dd35)
					g.vertex(x + dd34, y + dd45)
				}
				11 -> {
					g.vertex(x + dd4, y + dd5)
					g.vertex(x + dd4, y + dd25)
					g.vertex(x + dd4, y + dd35)
					g.vertex(x + dd4, y + dd45)
					g.vertex(x + dd2, y + dd5)
					g.vertex(x + dd2, y + dd2)
					g.vertex(x + dd2, y + dd45)
					g.vertex(x + dd34, y + dd5)
					g.vertex(x + dd34, y + dd25)
					g.vertex(x + dd34, y + dd35)
					g.vertex(x + dd34, y + dd45)
				}
				12 -> {
					g.vertex(x + dd4, y + dd5)
					g.vertex(x + dd4, y + dd25)
					g.vertex(x + dd4, y + dd35)
					g.vertex(x + dd4, y + dd45)
					g.vertex(x + dd2, y + dd5)
					g.vertex(x + dd2, y + dd25)
					g.vertex(x + dd2, y + dd35)
					g.vertex(x + dd2, y + dd45)
					g.vertex(x + dd34, y + dd5)
					g.vertex(x + dd34, y + dd25)
					g.vertex(x + dd34, y + dd35)
					g.vertex(x + dd34, y + dd45)
				}
			}
			g.drawPoints()
			g.end()
		}

		init {
			addAllFields(Board::class.java)
		}
	}

	var root: Tile? = null
		private set

	private val endpoints: Array<LinkedList<Tile>>
	private val endpointTransforms: Array<Matrix3x3>
	@Omit
	private val highlightedMoves: Array<MutableList<Move>>

	@JvmField
    @Omit
	var selectedMove: Move? = null

	@Omit
	private var boardImageId = -1
	private val rects: MutableList<Array<Vector2D>> = ArrayList()
	private val saveMinV = MutableVector2D()
	private val saveMaxV = MutableVector2D()

	private inner class PlaceTileAnim internal constructor(val tile: Tile, startPlayerPosition: Int, endPoint: Int) : AAnimation<AGraphics>(2000) {
		var start: IVector2D
		var end: IVector2D
		override fun draw(g: AGraphics, position: Float, dt: Float) {}

		init {
			start = when (startPlayerPosition) {
				EP_DOWN -> Vector2D(0.5f, 1f)
				EP_LEFT -> Vector2D(0f, 0.5f)
				EP_RIGHT -> Vector2D(1f, 0.5f)
				EP_UP -> Vector2D(0.5f, 0f)
				else     -> Vector2D.ZERO
			}
			end = when (endPoint) {
				EP_DOWN -> Vector2D(0f, 0.5f + 2 * endpoints[EP_DOWN].size)
				EP_LEFT -> Vector2D(1f + 2 * endpoints[EP_DOWN].size, 0f)
				EP_RIGHT -> Vector2D(1f + 2 * endpoints[EP_DOWN].size, 0f)
				EP_UP -> Vector2D(0f, -0.5f - 2 * endpoints[EP_DOWN].size)
				else     -> Vector2D.ZERO
			}
		}
	}

	fun setBoardImageId(id: Int) {
		boardImageId = id
	}

	@JvmField
    @Omit
	val animations = Collections.synchronizedList(ArrayList<AAnimation<AGraphics>>())
	fun addAnimation(a: AAnimation<AGraphics>) {
		synchronized(animations) { animations.add(a) }
	}

	@Synchronized
	fun clear() {
		root = null
		for (l in endpoints) {
			l.clear()
		}
		clearSelection()
		rects.clear()
		for (i in 0..3) {
			endpointTransforms[i].identityEq()
			transformEndpoint(endpointTransforms[i], i)
		}
		saveMaxV.zero()
		saveMinV.zero()
		animations.clear()
	}

	fun clearSelection() {
		for (s in highlightedMoves) s.clear()
		selectedMove = null
	}

	@Synchronized
	fun collectPieces(): List<Tile> {
		val pieces: MutableList<Tile> = ArrayList()
		if (root != null) {
			pieces.add(root!!)
			root = null
		}
		for (i in 0..3) {
			pieces.addAll(endpoints[i])
			endpoints[i].clear()
		}
		return pieces
	}

	@Synchronized
	fun placeRootPiece(pc: Tile?) {
		root = pc
		rects.add(arrayOf(
			Vector2D(-1f, -0.5f),
			Vector2D(1f,0.5f)
		))
	}

	fun findMovesForPiece(p: Tile): List<Move> {
		val moves: MutableList<Move> = ArrayList()
		for (i in 0..3) {
			if (endpoints[i].size == 0) {
				if (canPieceTouch(p, root?.pip1?:0)) {
					moves.add(Move(p, i, PLACMENT_FWD))
				}
			} else {
				if (canPieceTouch(p, endpoints[i].last.openPips)) {
					if (endpoints[i].size < 2) moves.add(Move(p, i, PLACMENT_FWD)) else {
						val m = Matrix3x3()
						for (ii in 0 until PLACEMENT_COUNT) {
							m.assign(endpointTransforms[i])
							transformPlacement(m, ii)
							var v = Vector2D(0.6f, 0.3f)
							v = m.multiply(v)
							if (isInsideRects(v)) continue
							v = Vector2D(1.6f, 0.6f)
							v = m.multiply(v)
							if (isInsideRects(v)) continue
							moves.add(Move(p, i, ii))
						}
						// TODO: Make sure there is at least one option?
					}
				}
			}
		}
		return moves
	}

	private fun canPieceTouch(p: Tile, pips: Int): Boolean {
		return p.pip1 == pips || p.pip2 == pips
	}

	@Synchronized
	fun doMove(piece: Tile, endpoint: Int, placement: Int) {
		var open = 0
		open = if (endpoints[endpoint].size == 0) {
			root?.openPips?:0
		} else {
			endpoints[endpoint].last.openPips
		}
		if (piece.pip1 == open) {
			piece.openPips = piece.pip2
		} else if (piece.pip2 == open) {
			piece.openPips = piece.pip1
		}
		endpoints[endpoint].addLast(piece)
		piece.placement = placement
		transformPlacement(endpointTransforms[endpoint], placement)
		val v0 = Vector2D.ZERO
		val v1 = Vector2D(2f, 1f)
		rects.add(arrayOf(
			endpointTransforms[endpoint].multiply(v0),
			endpointTransforms[endpoint].multiply(v1)
		))
		endpointTransforms[endpoint].multiplyEq(Matrix3x3().setTranslate(2f, 0f))
	}

	private fun transformPlacement(g: AGraphics, placement: Int) {
		val t = Matrix3x3()
		g.getTransform(t)
		transformPlacement(t, placement)
		g.setIdentity()
		g.multMatrix(t)
	}

	private fun transformPlacement(m: Matrix3x3?, placement: Int) {
		val t = Matrix3x3()
		when (placement) {
			PLACMENT_FWD -> {
			}
			PLACMENT_FWD_LEFT -> {
				t.setTranslate(1f, 0f)
				m!!.multiplyEq(t)
				t.setRotation(90f)
				m.multiplyEq(t)
			}
			PLACMENT_LEFT -> {
				t.setTranslate(0f, 1f)
				m!!.multiplyEq(t)
				t.setRotation(90f)
				m.multiplyEq(t)
			}
			PLACMENT_FWD_RIGHT -> {
				t.setTranslate(0f, 1f)
				m!!.multiplyEq(t)
				t.setRotation(-90f)
				m.multiplyEq(t)
			}
			PLACMENT_RIGHT -> {
				t.setTranslate(-1f, 0f)
				m!!.multiplyEq(t)
				t.setRotation(-90f)
				m.multiplyEq(t)
			}
		}
	}

	private fun epIndexToString(index: Int): String {
		when (index) {
			EP_LEFT -> return "EP_LEFT"
			EP_RIGHT -> return "EP_RIGHT"
			EP_UP -> return "EP_UP"
			EP_DOWN -> return "EP_DOWN"
		}
		throw AssertionError()
	}

	private fun placementIndexToString(index: Int): String {
		when (index) {
			PLACMENT_FWD -> return "FWD"
			PLACMENT_FWD_LEFT -> return "FWD_LEFT"
			PLACMENT_FWD_RIGHT -> return "FWD_RIGHT"
			PLACMENT_LEFT -> return "LEFT"
			PLACMENT_RIGHT -> return "RIGHT"
		}
		throw AssertionError()
	}

	private fun drawHighlighted(g: APGraphics, endpoint: Int, mouseX: Int, mouseY: Int, dragged: Tile?): Int {
		val mv = MutableVector2D()
		g.color = GColor.CYAN
		g.begin()
		for (move in highlightedMoves[endpoint]) {
			g.pushMatrix()
			transformPlacement(g, move.placment)
			mv[1f] = 0.5f
			g.transform(mv)
			g.vertex(0f, 0f)
			g.vertex(2f, 1f)
			g.popMatrix()
		}
		g.drawRects(3f)
		if (selectedMove != null) return -1
		var moveIndex = 0
		g.begin()
		for (move in highlightedMoves[endpoint]) {
			g.pushMatrix()
			transformPlacement(g, move.placment)
			g.setName(moveIndex++)
			mv[1f] = 0.5f
			g.transform(mv)
			// use larger pick rects so easier to place on android (finger covers whole of piece)
			g.vertex(0f, -0.5f)
			g.vertex(4f, 1.5f)
			g.popMatrix()
		}
		val picked = g.pickRects(mouseX, mouseY)
		g.end()
		if (picked >= 0) {
			selectedMove = highlightedMoves[endpoint][picked]
			val selectedEndpoint = selectedMove!!.endpoint
			var newTotal = computeEndpointsTotal()
			var openPips = root!!.pip1
			if (endpoints[selectedEndpoint].size > 0) {
				openPips = endpoints[selectedEndpoint].last.openPips
				newTotal -= openPips
			} else if (selectedEndpoint == EP_LEFT || selectedEndpoint == EP_RIGHT) {
				newTotal -= root!!.pip1
			}
			newTotal += if (selectedMove!!.piece.pip1 == openPips) {
				selectedMove!!.piece.pip2
			} else {
				selectedMove!!.piece.pip1
			}
			log.debug("Endpoint total:$newTotal")
			g.begin()
			g.pushMatrix()
			g.color = GColor.RED
			transformPlacement(g, selectedMove!!.placment)
			if (dragged != null) {
				var pip1 = dragged.pip1
				var pip2 = dragged.pip2
				if (pip2 == openPips) {
					val t = pip1
					pip1 = pip2
					pip2 = t
				}
				g.begin()
				drawTile(g, pip1, pip2, 1f)
				g.end()
			}
			g.vertex(0f, 0f)
			g.vertex(2f, 1f)
			g.drawRects(3f)
			g.popMatrix()
			g.end()
			log.debug("selected endpoint = " + epIndexToString(selectedEndpoint) + " placement = " + placementIndexToString(selectedMove!!.placment))
		}
		return picked
	}

	fun transformEndpoint(g: AGraphics, endpoint: Int) {
		val t = Matrix3x3()
		g.getTransform(t)
		transformEndpoint(t, endpoint)
		g.setIdentity()
		g.multMatrix(t)
	}

	private fun transformEndpoint(m: Matrix3x3?, endpoint: Int) {
		val t = Matrix3x3()
		when (endpoint) {
			EP_LEFT -> {
				t.setScale(-1f, -1f)
				m!!.multiplyEq(t)
				t.setTranslate(1f, -0.5f)
				m.multiplyEq(t)
			}
			EP_RIGHT -> {
				t.setTranslate(1f, -0.5f)
				m!!.multiplyEq(t)
			}
			EP_DOWN -> {
				t.setScale(-1f, -1f)
				m!!.multiplyEq(t)
				t.setTranslate(0.5f, 0.5f)
				m.multiplyEq(t)
				t.setRotation(90f)
				m.multiplyEq(t)
			}
			EP_UP -> {
				t.setTranslate(0.5f, 0.5f)
				m!!.multiplyEq(t)
				t.setRotation(90f)
				m.multiplyEq(t)
			}
		}
	}

	private fun genMinMaxPts(g: APGraphics) {
		g.vertex(-1f, -0.5f)
		g.vertex(1f, 0.5f)
		for (i in 0..3) {
			g.pushMatrix()
			run {
				transformEndpoint(g, i)
				for (t in endpoints[i]) {
					transformPlacement(g, t.placement)
					g.vertex(0f, 0f)
					g.vertex(0f, 1f)
					g.vertex(2f, 1f)
					g.vertex(2f, 0f)
					g.translate(2f, 0f)
				}
				g.vertex(0f, 3f)
				g.vertex(2f, 3f)
				g.vertex(2f, -2f)
				g.vertex(0f, -2f)
			}
			g.popMatrix()
		}
	}

	fun transformToEndpointLastPiece(g: AGraphics, ep: Int) {
		transformEndpoint(g, ep)
		for (p in endpoints[ep]) {
			transformPlacement(g, p.placement)
			g.translate(2f, 0f)
		}
	}

	@JvmField
    @Omit
	var boardWidth = 1f

	@JvmField
    @Omit
	var boardHeight = 1f
	@Synchronized
	fun draw(g: APGraphics, vpWidth: Float, vpHeight: Float, pickX: Int, pickY: Int, dragging: Tile?): Int {
		// choose an ortho that keeps the root in the middle and an edge around
		// that allows for a piece to be placed
		boardWidth = vpWidth
		boardHeight = vpHeight
		if (boardImageId >= 0) {
			g.drawImage(boardImageId, 0f, 0f, vpWidth, vpHeight)
		}
		var picked = -1
		g.pushMatrix()
		g.setIdentity()
		run {
			g.begin()
			g.clearMinMax()
			genMinMaxPts(g)
			val minBR: Vector2D = saveMinV.minEq(g.minBoundingRect)
			val maxBR: Vector2D = saveMaxV.maxEq(g.maxBoundingRect)
			g.end()
			val maxPcW = Math.max(Math.abs(minBR.x), Math.abs(maxBR.x))
			val maxPcH = Math.max(Math.abs(minBR.y), Math.abs(maxBR.y))
			val dimW = vpWidth / (2 * maxPcW)
			val dimH = vpHeight / (2 * maxPcH)
			val DIM = Math.min(dimW, dimH)
			g.setPointSize(DIM / 8)
			selectedMove = null
			g.translate(vpWidth / 2, vpHeight / 2)
			g.scale(DIM, -DIM)

			// DEBUG outline the min/max bounding box
			if (false && AGraphics.DEBUG_ENABLED) {
				g.color = GColor.YELLOW
				g.drawRect(minBR, maxBR, 1f)
			}

			// DEBUG draw pickX, pickY in viewport coords
			if (false && AGraphics.DEBUG_ENABLED) {
				val mv = g.screenToViewport(pickX, pickY)
				g.color = GColor.YELLOW
				g.drawCircle(mv.x, mv.y, 10f)
			}
			root?.let { root ->
				g.pushAndRun() {
					g.translate(-1f, -0.5f)
					drawTile(g, root.pip1, root.pip2, 1f)
				}
				for (i in 0..3) {
					g.pushAndRun() {
						transformEndpoint(g, i)
						for (p in endpoints[i]) {
							transformPlacement(g, p.placement)
							drawTile(g, p.getClosedPips(), p.openPips, 1f)
							g.translate(2f, 0f)
						}
						picked = Math.max(picked, drawHighlighted(g, i, pickX, pickY, dragging))
					}
				}
			}

			// DEBUG draw the rects
			if (false && AGraphics.DEBUG_ENABLED) {
				g.color = GColor.ORANGE
				for (r in rects) {
					g.drawRect(r[0], r[1], 3f)
				}
			}
		}
		val anims: MutableList<AAnimation<AGraphics>> = ArrayList()
		synchronized(animations) { anims.addAll(animations) }
		for (a in anims) {
			if (a.isDone) {
				synchronized(animations) { animations.remove(a) }
			} else {
				a.update(g)
			}
		}
		g.popMatrix()
		return picked
	}

	fun computeEndpointsTotal(): Int {
		var score = 0
		for (i in 0..3) {
			if (endpoints[i].size > 0) {
				score += endpoints[i].last.openPips
			}
		}
		if (root != null) {
			if (endpoints[EP_LEFT].size == 0) {
				score += root!!.pip1
			}
			if (endpoints[EP_RIGHT].size == 0) {
				score += root!!.pip2
			}
		}
		return score
	}

	@Synchronized
	fun highlightMoves(moves: List<Move>?) {
		for (i in 0..3)
			highlightedMoves[i].clear()
		moves?.forEach { m ->
			highlightedMoves[m.endpoint].add(m)
		}
	}

	private fun isInsideRects(v: IVector2D): Boolean {
		for (r in rects) {
			if (Utils.isPointInsideRect(v, r[0], r[1])) return true
		}
		return false
	}

	fun getOpenPips(ep: Int): Int {
		return if (endpoints[ep].size == 0) {
			if (ep == EP_LEFT || ep == EP_RIGHT) root!!.pip1 else 0
		} else endpoints[ep].last.openPips
	}

	init {
		endpoints = Array(4) { LinkedList() }
		endpointTransforms = Array(4) { Matrix3x3().identityEq() }
		highlightedMoves = Array(4) { ArrayList() }
	}
}