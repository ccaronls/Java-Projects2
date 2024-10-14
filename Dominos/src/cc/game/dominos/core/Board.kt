package cc.game.dominos.core

import cc.lib.game.AAnimation
import cc.lib.game.AGraphics
import cc.lib.game.APGraphics
import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.game.IVector2D
import cc.lib.ksp.mirror.Mirror
import cc.lib.ksp.mirror.Mirrored
import cc.lib.ksp.mirror.MirroredArray
import cc.lib.ksp.mirror.MirroredList
import cc.lib.ksp.mirror.toMirroredArray
import cc.lib.ksp.mirror.toMirroredList
import cc.lib.logger.LoggerFactory
import cc.lib.math.Matrix3x3
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import java.util.Collections

@Mirror
interface IBoard : Mirrored {
	// all the data we want to serialize here
	var endpoints: MirroredArray<MirroredList<Tile>>
	var endpointTransforms: MirroredArray<Matrix3x3>

	val rects: MutableList<GRectangle>
	val saveMinV: MutableVector2D
	val saveMaxV: MutableVector2D

}

/**
 * Created by chriscaron on 2/1/18.
 *
 * Representation of a Dominos board.
 *
 */
class Board : BoardImpl() {

	init {
		endpoints = Array(4) { emptyList<Tile>().toMirroredList() }.toMirroredArray()
		endpointTransforms = Array(4) { Matrix3x3.newIdentity() }.toMirroredArray()
	}

	private val log = LoggerFactory.getLogger(javaClass)

	companion object {
		const val EP_LEFT = 0
		const val EP_RIGHT = 1
		const val EP_UP = 2
		const val EP_DOWN = 3
		const val PLACEMENT_FWD = 0
		const val PLACEMENT_FWD_RIGHT = 1
		const val PLACEMENT_FWD_LEFT = 2
		const val PLACEMENT_RIGHT = 3
		const val PLACEMENT_LEFT = 4
		const val PLACEMENT_COUNT = 5

		/**
		 *
		 * @param g
		 * @param pips1
		 * @param pips2
		 * @param alpha [0-1] inclusive
		 */
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
	}

	var root: Tile? = null
		private set

	private val highlightedMoves: Array<MutableList<Move>> = Array(4) { ArrayList() }
	var selectedMove: Move? = null
	private var boardImageId = -1

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
				else -> IVector2D.ZERO
			}
			end = when (endPoint) {
				EP_DOWN -> Vector2D(0f, 0.5f + 2 * endpoints[EP_DOWN].size)
				EP_LEFT -> Vector2D(1f + 2 * endpoints[EP_DOWN].size, 0f)
				EP_RIGHT -> Vector2D(1f + 2 * endpoints[EP_DOWN].size, 0f)
				EP_UP -> Vector2D(0f, -0.5f - 2 * endpoints[EP_DOWN].size)
				else -> IVector2D.ZERO
			}
		}
	}

	fun setBoardImageId(id: Int) {
		boardImageId = id
	}

	val animations = Collections.synchronizedList(ArrayList<AAnimation<AGraphics>>())
	fun addAnimation(a: AAnimation<AGraphics>) {
		synchronized(animations) { animations.add(a) }
	}

	fun clear() {
		root = null
		for (l in endpoints) {
			l.clear()
		}
		clearSelection()
		rects.clear()
		for (i in 0..3) {
			endpointTransforms[i].setIdentityMatrix()
			transformEndpoint(endpointTransforms[i], i)
		}
		saveMaxV?.zero()
		saveMinV?.zero()
		animations.clear()
	}

	fun clearSelection() {
		for (s in highlightedMoves) s.clear()
		selectedMove = null
	}

	fun collectPieces(): List<Tile> {
		val pieces: MutableList<Tile> = ArrayList()
		root?.let {
			pieces.add(it)
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
		rects.add(
			GRectangle(
				Vector2D(-1f, -0.5f),
				Vector2D(1f, 0.5f)
			)
		)
	}

	fun findMovesForPiece(p: Tile): List<Move> {
		val moves: MutableList<Move> = ArrayList()
		for (i in 0..3) {
			if (endpoints[i].size == 0) {
				if (canPieceTouch(p, root?.pip1?:0)) {
					moves.add(Move(p, i, PLACEMENT_FWD))
				}
			} else {
				if (canPieceTouch(p, endpoints[i].last().openPips)) {
					if (endpoints[i].size < 2) moves.add(Move(p, i, PLACEMENT_FWD)) else {
						val m = Matrix3x3()
						for (ii in 0 until PLACEMENT_COUNT) {
							m.assign(endpointTransforms[i])
							transformPlacement(m, ii)
							var v = Vector2D(0.6f, 0.3f)
							v = m * v
							if (isInsideRects(v)) continue
							v = Vector2D(1.6f, 0.6f)
							v = m * v
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
			endpoints[endpoint].last()?.openPips ?: 0
		}
		if (piece.pip1 == open) {
			piece.openPips = piece.pip2
		} else if (piece.pip2 == open) {
			piece.openPips = piece.pip1
		}
		endpoints[endpoint].addLast(piece)
		piece.placement = placement
		transformPlacement(endpointTransforms[endpoint], placement)
		val v0 = IVector2D.ZERO
		val v1 = Vector2D(2f, 1f)
		rects.add(
			GRectangle(
				endpointTransforms[endpoint] * v0,
				endpointTransforms[endpoint] * v1
			)
		)
		endpointTransforms[endpoint].timesAssign(Matrix3x3.newTranslate(2.0, 0.0))
	}

	private fun transformPlacement(g: AGraphics, placement: Int) {
		val t = Matrix3x3()
		g.getTransform(t)
		transformPlacement(t, placement)
		g.setIdentity()
		g.multMatrix(t)
	}

	private fun transformPlacement(m: Matrix3x3, placement: Int) {
		val t = Matrix3x3()
		when (placement) {
			PLACEMENT_FWD -> {
			}

			PLACEMENT_FWD_LEFT -> {
				t.setTranslationMatrix(1f, 0f)
				m.timesAssign(t)
				t.setRotationMatrix(90f)
				m.timesAssign(t)
			}

			PLACEMENT_LEFT -> {
				t.setTranslationMatrix(0f, 1f)
				m.timesAssign(t)
				t.setRotationMatrix(90f)
				m.timesAssign(t)
			}

			PLACEMENT_FWD_RIGHT -> {
				t.setTranslationMatrix(0f, 1f)
				m.timesAssign(t)
				t.setRotationMatrix(-90f)
				m.timesAssign(t)
			}

			PLACEMENT_RIGHT -> {
				t.setTranslationMatrix(-1f, 0f)
				m.timesAssign(t)
				t.setRotationMatrix(-90f)
				m.timesAssign(t)
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
			PLACEMENT_FWD -> return "FWD"
			PLACEMENT_FWD_LEFT -> return "FWD_LEFT"
			PLACEMENT_FWD_RIGHT -> return "FWD_RIGHT"
			PLACEMENT_LEFT -> return "LEFT"
			PLACEMENT_RIGHT -> return "RIGHT"
		}
		throw AssertionError()
	}

	private fun drawHighlighted(g: APGraphics, endpoint: Int, mouseX: Int, mouseY: Int, dragged: Tile?): Int {
		var root = this.root ?: return 0
		val mv = MutableVector2D()
		g.color = GColor.CYAN
		g.begin()
		for (move in highlightedMoves[endpoint]) {
			g.pushMatrix()
			transformPlacement(g, move.placement)
			mv.assign(1f, 0.5f)
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
			transformPlacement(g, move.placement)
			g.setName(moveIndex++)
			mv.assign(1f, 0.5f)
			g.transform(mv)
			// use larger pick rects so easier to place on android (finger covers whole of piece)
			g.vertex(0f, -0.5f)
			g.vertex(4f, 1.5f)
			g.popMatrix()
		}
		val picked = g.pickRects(mouseX, mouseY)
		g.end()
		if (picked >= 0) {
			highlightedMoves[endpoint][picked].let { move ->
				selectedMove = move
				val selectedEndpoint = move.endpoint
				var newTotal = computeEndpointsTotal()
				var openPips = root.pip1
				if (endpoints[selectedEndpoint].size > 0) {
					openPips = endpoints[selectedEndpoint].last()?.openPips ?: 0
					newTotal -= openPips
				} else if (selectedEndpoint == EP_LEFT || selectedEndpoint == EP_RIGHT) {
					newTotal -= root.pip1
				}
				newTotal += if (move.piece.pip1 == openPips) {
					move.piece.pip2
				} else {
					move.piece.pip1
				}
				log.debug("Endpoint total:$newTotal")
				g.begin()
				g.pushMatrix()
				g.color = GColor.RED
				transformPlacement(g, move.placement)
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
				log.debug(
					"selected endpoint = " + epIndexToString(selectedEndpoint) + " placement = " + placementIndexToString(
						move.placement
					)
				)
			}
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

	private fun transformEndpoint(m: Matrix3x3, endpoint: Int) {
		val t = Matrix3x3()
		when (endpoint) {
			EP_LEFT -> {
				t.setScaleMatrix(-1f, -1f)
				m.timesAssign(t)
				t.setTranslationMatrix(1f, -0.5f)
				m.timesAssign(t)
			}

			EP_RIGHT -> {
				t.setTranslationMatrix(1f, -0.5f)
				m.timesAssign(t)
			}
			EP_DOWN -> {
				t.setScaleMatrix(-1f, -1f)
				m.timesAssign(t)
				t.setTranslationMatrix(0.5f, 0.5f)
				m.timesAssign(t)
				t.setRotationMatrix(90f)
				m.timesAssign(t)
			}
			EP_UP -> {
				t.setTranslationMatrix(0.5f, 0.5f)
				m.timesAssign(t)
				t.setRotationMatrix(90f)
				m.timesAssign(t)
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

	var boardWidth = 1f
	var boardHeight = 1f

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
				g.drawRect(GRectangle(minBR, maxBR), 1f)
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
					g.drawRect(r, 3f)
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
				score += endpoints[i].last().openPips
			}
		}
		root?.let {
			if (endpoints[EP_LEFT].size == 0) {
				score += it.pip1
			}
			if (endpoints[EP_RIGHT].size == 0) {
				score += it.pip2
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

	private fun isInsideRects(v: IVector2D): Boolean = rects.firstOrNull { it.contains(v) } != null

	fun getOpenPips(ep: Int): Int {
		return if (endpoints[ep].size == 0) {
			if (ep == EP_LEFT || ep == EP_RIGHT)
				root?.pip1 ?: 0 else 0
		} else endpoints[ep].last().openPips
	}
}