package marcos.games.hexes.core

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.IVector2D
import cc.lib.game.Utils
import cc.lib.math.MutableVector2D
import cc.lib.reflector.Omit
import cc.lib.reflector.Reflector
import java.util.Collections

class Board : Reflector<Board>() {
	private val verts = mutableListOf<Vertex>()
	private val pieces = mutableListOf<Piece>()
	private val edges = mutableListOf<Edge>()
	private var minX = 0f
	private var minY = 0f
	private var maxX = 0f
	private var maxY = 0f
	private var highlightX = 0f
	private var highlightY = 0f
	private val gen = IdGenerator()

	@Omit
	private var pieceChoices: List<Int>? = null

	private class Outline {
		var edges = mutableSetOf<Edge>()
		var cx = 0f
		var cy = 0f
		var player = 0
		var shape: Shape? = null
	}

	@Omit
	private val outlines = mutableListOf<Outline>()

	@Synchronized
	fun init() {
		verts.clear()
		verts.add(Vertex(0, 0))
		verts.add(Vertex(2, 0))
		verts.add(Vertex(1, 2))
		verts.add(Vertex(1, -2))
		addPiece(0, 1, 2)
		addPiece(0, 1, 3)
		computeMinMax()
	}

	fun isPieceUpward(p: Piece): Boolean {
		return verts[p.v[0]].y < verts[p.v[2]].y
	}

	@Synchronized
	fun addPiece(vararg v: Int) {
		val index = pieces.size
		pieces.add(Piece(v[0], v[1], v[2]))
		addEdge(v[0], v[1], index)
		addEdge(v[1], v[2], index)
		addEdge(v[2], v[0], index)
		verts[v[0]].addPiece(index)
		verts[v[1]].addPiece(index)
		verts[v[2]].addPiece(index)
	}

	@Synchronized
	fun addEdge(from: Int, to: Int, piece: Int) {
		var e = Edge(from, to)
		val index = Collections.binarySearch(edges, e)
		if (index < 0) {
			edges.add(-(index + 1), e)
		} else {
			e = edges[index]
		}
		e.addPiece(piece)
	}

	@Synchronized
	fun getEdge(from: Int, to: Int): Edge? {
		val e = Edge(from, to)
		val index = Collections.binarySearch(edges, e)
		return if (index < 0) null else edges[index]
	}

	private fun computeMinMax() {
		maxY = 0f
		maxX = maxY
		minY = maxX
		minX = minY
		for (v in verts) {
			minX = Math.min(minX, v.x)
			maxX = Math.max(maxX, v.x)
			minY = Math.min(minY, v.y)
			maxY = Math.max(maxY, v.y)
		}
	}

	@Synchronized
	fun draw(g: AGraphics): Int {
		var highlighted = -1
		val width = (maxX - minX).toFloat()
		val height = (maxY - minY).toFloat()
		val cx = minX + width / 2
		val cy = minY + height / 2
		var dim = Math.max(width, height)
		if (dim < 10) dim = 10f
		g.ortho(cx - dim / 2, cx + dim / 2, cy - dim / 2, cy + dim / 2)
		//		g.ortho(minX, maxX, minY, maxY);
		val h = g.screenToViewport(highlightX, highlightY)
		var index = 0
		for (p in pieces) {
			if (drawPiece(g, p, h.x, h.y)) {
				if (pieceChoices?.let { it.indexOf(index) >= 0 } != false) {
					highlighted = index
				}
			}
			index++
		}
		g.begin()
		for (o in outlines) {
			g.pushMatrix()
			g.translate(o.cx, o.cy)
			g.scale(0.9f)
			when (o.shape) {
				Shape.DIAMOND -> g.color = GColor.GREEN
				Shape.HEXAGON -> g.color = GColor.ORANGE
				Shape.NONE -> g.color = GColor.BLACK
				Shape.TRIANGLE -> g.color = GColor.CYAN
				else -> {}
			}
			g.begin()
			for (e in o.edges) {
				var v = verts[e.from]
				g.vertex(v.x - o.cx, v.y - o.cy)
				v = verts[e.to]
				g.vertex(v.x - o.cx, v.y - o.cy)
			}
			g.drawLines()
			g.popMatrix()
		}
		if (highlighted >= 0) {
			val p = pieces[highlighted]
			g.color = GColor.MAGENTA
			g.begin()
			for (i in 0..2) g.vertex(verts[p.v[i]])
			g.setLineWidth(2f)
			g.drawLineLoop()
		}
		return highlighted
	}

	fun getColorForPlayer(playerNum: Int, g: AGraphics?): GColor {
		when (playerNum) {
			1 -> return GColor.RED
			2 -> return GColor.BLUE
		}
		return GColor.BLACK
	}

	private fun getMidpoint(vararg vIndices: Int): IVector2D {
		val mv = MutableVector2D()
		if (vIndices.size > 0) {
			for (vIndex in vIndices) {
				val v = verts[vIndex]
				mv.addEq(v)
			}
			mv.scaleEq(1.0f / vIndices.size)
		}
		return mv
	}

	private fun drawPiece(g: AGraphics, p: Piece, hx: Float, hy: Float): Boolean {
		g.clearMinMax()
		if (p.player > 0) {
			g.color = getColorForPlayer(p.player, g)
			g.begin()
			for (v in p.v) g.vertex(verts[v])
			g.drawTriangles()
			g.color = GColor.YELLOW
			val c = getMidpoint(*p.v)
			when (p.type) {
				Shape.NONE -> {}
				Shape.DIAMOND -> {
					val r = 0.2f
					g.begin()
					g.vertex(c.x, c.y + r * 2)
					g.vertex(c.x - r, c.y)
					g.vertex(c.x + r, c.y)
					g.vertex(c.x, c.y - r * 2)
					g.color = GColor.YELLOW
					g.drawTriangleStrip()
				}

				Shape.TRIANGLE -> {
					val r = 0.25f
					g.begin()
					g.pushMatrix()
					if (!isPieceUpward(p)) {
						g.scale(1f, -1f)
					}
					g.vertex(c.x - r, c.y - r / 3)
					g.vertex(c.x + r, c.y - r / 3)
					g.vertex(c.x, c.y + r * 3 / 2)
					g.drawTriangles()
					g.popMatrix()
				}

				Shape.HEXAGON -> {
					val r = 0.2f
					g.begin()
					g.vertex(c)
					g.vertex(c.x - r, c.y)
					g.vertex(c.x - r / 2, c.y - r)
					g.vertex(c.x + r / 2, c.y - r)
					g.vertex(c.x + r, c.y)
					g.vertex(c.x + r / 2, c.y + r)
					g.vertex(c.x - r / 2, c.y + r)
					g.vertex(c.x - r, c.y)
					g.drawTriangleFan()
				}
			}
			// DEBUG
			//g.setColor(g.BLACK);
			//g.drawJustifiedString(c.getX(), c.getY(), Justify.CENTER, Justify.CENTER, "[" + p.getGroupId() + "]" + p.getGroupShape());  
		}
		g.begin()
		for (v in p.v) g.vertex(verts[v])
		g.color = GColor.WHITE
		g.setLineWidth(2f)
		g.drawLineLoop()
		return Utils.isPointInsidePolygon(hx, hy, arrayOf<IVector2D?>(verts[p.v[0]], verts[p.v[1]], verts[p.v[2]]), 3)
	}

	/**
	 * Grow the board such that new new moves are available
	 */
	@Synchronized
	fun grow() {
		val newPieces = ArrayList<IntArray>()
		for (p in pieces) {
			if (p.player == 0) continue
			//Vertex v0 = verts.get(p.v0);
			val v1 = verts[p.v[1]]
			val v2 = verts[p.v[2]]
			if ((getEdge(p.v[0], p.v[1])?.num ?: 1000) < 2) {
				var vIndex = -1
				vIndex = if (isPieceUpward(p)) {
					// grow down
					Utils.println("grow down")
					getVertex(v2.x, v1.y - 2)
				} else {
					// grow up
					Utils.println("grow up")
					getVertex(v2.x, v1.y + 2)
				}
				//addPiece(p.v0, p.v1, v);
				newPieces.add(intArrayOf(p.v[0], p.v[1], vIndex))
			}
			if ((getEdge(p.v[1], p.v[2])?.num ?: 1000) < 2) {
				// grow right
				Utils.println("grow right")
				val v = getVertex(v2.x + 2, v2.y)
				//addPiece(p.v2, v, p.v1);
				newPieces.add(intArrayOf(p.v[2], v, p.v[1]))
			}
			if ((getEdge(p.v[2], p.v[0])?.num ?: 1000) < 2) {
				// grow left
				Utils.println("grow left")
				val v = getVertex(v2.x - 2, v2.y)
				//addPiece(v, p.v2, p.v0);
				newPieces.add(intArrayOf(v, p.v[2], p.v[0]))
			}
		}
		for (i in newPieces.indices) {
			addPiece(*newPieces[i])
		}
		computeMinMax()
		//Utils.println("verts");
		//Utils.printCollection(verts);
		//Utils.println("edges");
		//Utils.printCollection(edges);
		//Utils.println("pieces");
		//Utils.printCollection(pieces);
	}

	private fun getVertex(x: Float, y: Float): Int {
		val v = Vertex(x, y)
		var index = verts.indexOf(v)
		if (index < 0) {
			index = verts.size
			verts.add(v)
		}
		return index
	}

	@Synchronized
	fun setHighlighted(x: Float, y: Float) {
		highlightX = x
		highlightY = y
	}

	fun getPiece(index: Int): Piece {
		return pieces[index]
	}

	// identify all shapes for a player maximizing for points
	@Synchronized
	fun shapeSearch(player: Int) {
		// initialize everything
		val it = outlines.iterator()
		while (it.hasNext()) {
			val o = it.next()
			if (o.player == player) it.remove()
		}
		for (p in pieces) {
			if (p.player == player) {
				gen.putBack(p.groupId)
				p.groupId = 0
				p.groupShape = Shape.NONE
			}
		}
		// find all the hexagons
		while (true) {
			var bestHex = -1
			var bestPts = 0
			for (p in pieces) {
				if (p.player == player) {
					val pv = checkHexagon(p)
					if (pv[0] > bestPts) {
						bestHex = pv[1]
						bestPts = pv[0]
					}
				}
			}
			if (bestHex >= 0) {
				// mark the hex
				val v = verts[bestHex]
				val id = gen.nextId()
				for (i in 0..5) {
					val p = pieces[v.p[i]]
					p.groupId = id
					p.groupShape = Shape.HEXAGON
					// add player edges that do not have bestHex as an endpoint
					val o = Outline()
					outlines.add(o)
					o.player = player
					o.shape = Shape.HEXAGON
					o.cx = v.x.toFloat()
					o.cy = v.y.toFloat()
					for (ii in 0..2) {
						val iii = (ii + 1) % 3
						if (p.v[ii] != bestHex && p.v[iii] != bestHex) {
							getEdge(p.v[ii], p.v[iii])?.let {
								o.edges.add(it)
							}
						}
					}
				}
			} else {
				break // no more hexes
			}
		}

		// find all the triangles
		while (true) {
			var bestTri: Piece? = null
			var bestPts = 0
			for (p in pieces) {
				if (p.player == player && p.groupId == 0) {
					val pts = checkTriangle(p)
					if (pts > bestPts) {
						bestTri = p
						bestPts = pts
					}
				}
			}
			if (bestTri != null) {
				// mark as a triangle
				val id = gen.nextId()
				bestTri.groupId = id
				bestTri.groupShape = Shape.TRIANGLE
				val o = Outline()
				outlines.add(o)
				for (i in 0..2) {
					getAdjacent(bestTri, i)?.let { pp ->
						pp.groupId = id
						pp.groupShape = Shape.TRIANGLE
						o.player = player
						o.shape = Shape.TRIANGLE
						val mp = getMidpoint(*bestTri.v)
						o.cx = mp.x
						o.cy = mp.y
						o.edges.addAll(getPieceEdges(pp))
					}
				}
				o.edges.removeAll(getPieceEdges(bestTri))
			} else {
				break
			}
		}

		// find all the diamonds
		while (true) {
			var bestDiamond: Array<Piece>? = null
			var bestPts = 0
			var edgeIndex = 0 // edge in p to not include in playerShapes
			for (p in pieces) {
				if (p.player == player && p.groupId == 0) {
					for (i in 0..2) {
						val pp = getAdjacent(p, i)
						if (pp != null && pp.player == player && pp.groupId == 0) {
							val pts = p.type.ordinal + pp.type.ordinal
							if (pts > bestPts) {
								bestPts = pts
								bestDiamond = arrayOf(p, pp)
								edgeIndex = i
							}
						}
					}
				}
			}
			if (bestDiamond != null) {
				// mark
				val id = gen.nextId()
				val o = Outline()
				outlines.add(o)
				for (i in 0..1) {
					bestDiamond[i]?.groupId = id
					bestDiamond[i]?.groupShape = Shape.DIAMOND
					o.player = player
					o.shape = Shape.DIAMOND
					o.edges.addAll(getPieceEdges(bestDiamond[i]))
				}
				val e = getPieceEdges(bestDiamond[0])[edgeIndex]
				val mp = getMidpoint(e.from, e.to)
				o.cx = mp.x
				o.cy = mp.y
				o.edges.remove(e)
			} else {
				break
			}
		}
	}

	fun getPieceEdges(p: Piece?): List<Edge> {
		if (p == null)
			return emptyList()
		val edges = mutableListOf<Edge?>()
		for (i in 0..2) {
			val ii = (i + 1) % 3
			edges.add(getEdge(p.v[i], p.v[ii]))
		}
		return edges.filterNotNull()
	}

	fun getAdjacent(p: Piece, n: Int): Piece? {
		val nn = (n + 1) % 3
		getEdge(p.v[n], p.v[nn])?.let { e ->
			var pp = pieces[e.p[0]]
			if (pp === p) {
				if (e.num < 2) return null
				pp = pieces[e.p[1]]
			}
			assert(pp !== p)
			return pp
		}
		return null
	}

	private fun checkTriangle(p: Piece): Int {
		var pts = p.type.ordinal
		for (i in 0..2) {
			val ii = (i + 1) % 3
			getEdge(p.v[i], p.v[ii])?.let { e ->
				if (e.num < 2) return 0
				var p2 = pieces[e.p[0]]
				if (p2 === p) {
					p2 = pieces[e.p[1]]
				}
				pts += if (p2.player == p.player && p2.groupId == 0) {
					p2.type.ordinal
				} else {
					return 0
				}
			}
		}
		return pts
	}

	// return vertex of highest value hexagon
	private fun checkHexagon(p: Piece): IntArray {
		var v = 0
		val pts0 = checkHexagonVertex(p.v[0], p.player)
		val pts1 = checkHexagonVertex(p.v[1], p.player)
		var pts = 0
		if (pts0 < pts1) {
			pts = pts1
			v = p.v[1]
		} else {
			pts = pts0
			v = p.v[0]
		}
		val pts2 = checkHexagonVertex(p.v[2], p.player)
		if (pts2 > pts) {
			v = p.v[2]
			pts = pts2
		}
		return intArrayOf(pts, v)
	}

	private fun checkHexagonVertex(vIndex: Int, playerNum: Int): Int {
		val v = verts[vIndex]
		if (v.num < 6) return 0
		var pts = 0
		for (i in 0 until v.num) {
			val p = pieces[v.p[i]]
			pts += if (p.player == playerNum && p.groupId == 0) {
				p.type.ordinal
			} else {
				return 0
			}
		}
		return pts
	}

	@Synchronized
	fun computePlayerPoints(player: Int): Int {
		var points = 0
		for (p in pieces) {
			if (p.player == player) {
				points += p.type.ordinal * p.groupShape.ordinal
			}
		}
		return points
	}

	fun computeUnused(): ArrayList<Int> {
		val choices = ArrayList<Int>()
		var index = 0
		for (p in pieces) {
			if (p.player == 0) {
				choices.add(index)
			}
			index++
		}
		return choices
	}

	@Synchronized
	fun setPieceChoices(choices: List<Int>?) {
		pieceChoices = choices
	}

	@Synchronized
	fun setPiece(pIndex: Int, playerNum: Int, shape: Shape) {
		val p = pieces[pIndex]
		p.player = playerNum
		p.type = shape
	}

	companion object {
		init {
			addAllFields(Board::class.java)
		}
	}
}
