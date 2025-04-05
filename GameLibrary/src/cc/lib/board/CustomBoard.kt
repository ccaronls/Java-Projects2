package cc.lib.board

import cc.lib.game.AGraphics
import cc.lib.game.APGraphics
import cc.lib.game.GColor
import cc.lib.game.GDimension
import cc.lib.game.GRectangle
import cc.lib.game.IDimension
import cc.lib.game.IVector2D
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.math.CMath
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.reflector.Reflector
import cc.lib.utils.GException
import cc.lib.utils.clearAndAddAll
import java.util.Collections
import java.util.LinkedList
import java.util.Queue
import java.util.Vector

open class CustomBoard<V : BVertex, E : BEdge, C : BCell> : Reflector<CustomBoard<*, *, *>>() {
	private val _verts = Vector<V>()
	private val _edges = Vector<E>()
	private val _cells = Vector<C>()
	private var _dimension = GDimension()

	val verts: List<V>
		get() = _verts
	val edges: List<E>
		get() = _edges
	val cells: List<C>
		get() = _cells

	val dimension: IDimension
		get() = _dimension

	/**
	 *
	 * @param g
	 */
	fun drawEdges(g: AGraphics) {
		g.begin()
		for (e in _edges) {
			g.vertex(_verts[e.from])
			g.vertex(_verts[e.to])
		}
		g.drawLines()
	}

	/**
	 *
	 * @param g
	 */
	fun drawVerts(g: AGraphics) {
		g.begin()
		for (v in _verts) {
			if (v != null) g.vertex(v)
		}
		g.drawPoints()
	}

	/**
	 *
	 * @param g
	 */
	fun drawVertsNumbered(g: AGraphics) {
		g.begin()
		g.pushMatrix()
		val radius = g.textHeight
		g.translate(radius, radius)
		var index = 0
		for (v in _verts) {
			val vv = clampToScreen(g, v, radius)
			g.color = GColor.TRANSLUSCENT_BLACK
			g.drawFilledCircle(vv, radius)
			g.color = GColor.CYAN
			g.drawJustifiedString(vv.x, vv.y, Justify.CENTER, Justify.CENTER, index++.toString())
		}
		g.popMatrix()
	}

	/**
	 *
	 * @param g
	 */
	fun drawEdgesNumbered(g: AGraphics) {
		g.begin()
		g.pushMatrix()
		g.translate(-5f, 0f)
		var index = 0
		val radius = g.textHeight
		for (e in _edges) {
			val mp = clampToScreen(g, getMidpoint(e), radius)
			g.color = GColor.TRANSLUSCENT_BLACK
			g.drawFilledCircle(mp, radius)
			g.color = GColor.CYAN
			g.drawJustifiedString(mp.x, mp.y, Justify.CENTER, Justify.CENTER, index++.toString())
		}
		g.popMatrix()
	}

	fun clampToScreen(g: AGraphics, v: IVector2D, radius: Float): IVector2D {
		if (v.x >= radius && v.y >= radius && v.x <= g.viewportWidth - radius && v.y <= g.viewportHeight - radius) return v
		val newX = Utils.clamp(v.x, radius, g.viewportWidth - radius)
		val newY = Utils.clamp(v.y, radius, g.viewportHeight - radius)
		return Vector2D(newX, newY)
	}

	/**
	 *
	 * @param g
	 */
	fun drawCellsNumbered(g: AGraphics) {
		g.begin()
		g.pushMatrix()
		g.translate(-5f, 0f)
		var index = 0
		for (c in _cells) {
			g.drawString(String.format("%d", index++), c.x, c.y)
		}
		g.popMatrix()
	}

	/**
	 *
	 * @param b
	 * @param g
	 */
	fun drawCellArrowed(b: BCell, g: AGraphics) {
		g.pushMatrix()
		g.translate(b)
		g.scale(0.9f)
		g.translate(-b.x, -b.y)
		var p = b.getAdjVertex(b.numAdjVerts - 1)
		for (v in b.adjVerts) {
			val v0: IVector2D = _verts[p]
			val v3: IVector2D = _verts[v]
			val ue = v3.minus(v0).normalizedEq() //.scaledBy(0.2f);
			val v1: IVector2D = v3.minus(ue)
			ue.normEq()
			val v2: Vector2D = v1.minus(ue)
			val v4: Vector2D = v1.minus(ue)
			g.begin()
			g.vertexArray(v0, v1)
			g.drawLineStrip()
			g.begin()
			g.vertexArray(v2, v3, v4)
			g.drawTriangles()
			p = v
		}
		g.popMatrix()
	}

	/**
	 *
	 * @param e
	 * @return
	 */
	fun getMidpoint(e: BEdge): MutableVector2D {
		return _verts[e.from].plus(_verts[e.to]).scaleEq(0.5f)
	}

	/**
	 *
	 * @param b
	 * @param g
	 * @param scale
	 */
	@JvmOverloads
	fun renderCell(b: BCell, g: AGraphics, scale: Float = 1f) {
		g.pushMatrix()
		g.translate(b)
		g.scale(scale)
		g.translate(-b.x, -b.y)
		g.begin()
		for (v in b.adjVerts) {
			g.vertex(_verts[v])
		}
		g.popMatrix()
	}

	/**
	 *
	 * @param g
	 * @param scale
	 */
	open fun drawCells(g: AGraphics, scale: Float) {
		for (b in _cells) {
			renderCell(b, g, scale)
			g.drawLineLoop()
			// draw center point
			g.begin()
			g.vertex(b)
			g.drawPoints()
		}
	}

	/**
	 *
	 * @param g
	 * @param mouse
	 * @return
	 */
	fun pickVertex(g: APGraphics, mouse: Vector2D): Int {
		g.begin()
		var index = 0
		for (v in _verts) {
			if (v != null) {
				g.setName(index++)
				g.vertex(v)
			}
		}
		return g.pickPoints(mouse, 5)
	}

	/**
	 *
	 * @param v
	 * @return
	 */
	fun addVertex(v: IVector2D): Int {
		for (index in _verts.indices) {
			if (_verts[index] == null) {
				_verts[index] = newVertex(v)
				return index
			}
		}
		val index = _verts.size
		_verts.add(newVertex(v))
		return index
	}

	/**
	 *
	 * @param v
	 * @return
	 */
	protected fun newVertex(v: IVector2D): V {
		return BVertex(v) as V
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	fun addVertex(x: Float, y: Float): Int {
		return addVertex(Vector2D(x, y))
	}

	/**
	 *
	 * @param from
	 * @param to
	 */
	fun getOrAddEdge(from: Int, to: Int): Int {
		val edge = newEdge(from, to)
		var idx = Collections.binarySearch(_edges, edge)
		if (idx < 0) {
			idx = -idx - 1
			_edges.insertElementAt(edge, idx)
		}
		return idx
	}

	/**
	 *
	 * @param e
	 */
	fun addEdge(e: E) {
		_edges.add(e)
	}

	/**
	 *
	 * @param g
	 * @param mx
	 * @param my
	 * @return
	 */
	fun pickEdge(g: APGraphics, mx: Int, my: Int): Int {
		g.begin()
		var index = 0
		for (e in _edges) {
			g.setName(index++)
			renderEdge(e, g)
		}
		return g.pickLines(mx, my, 5)
	}

	fun pickEdge(g: APGraphics, mouse: IVector2D): Int {
		g.begin()
		var index = 0
		for (e in _edges) {
			g.setName(index++)
			renderEdge(e, g)
		}
		return g.pickLines(mouse, 5)
	}

	/**
	 * Add a cell and return its idex in the array
	 *
	 * @param verts
	 * @return
	 */
	fun addCell(vararg verts: Int): Int {
		val index = _cells.size
		_cells.add(newCell(verts.toList()))
		computeCell(index)
		return index
	}

	/**
	 * @param g
	 * @param mouse
	 * @return
	 */
	fun pickCell(g: APGraphics, mouse: IVector2D): Int {
		g.begin()
		var index = 0
		for (c in _cells) {
			if (isPointInsideCell(mouse, index)) return index
			index++
		}
		return -1
	}

	/**
	 *
	 * @param e
	 * @param g
	 */
	fun renderEdge(e: BEdge, g: AGraphics) {
		g.vertex(_verts[e.from])
		g.vertex(_verts[e.to])
	}

	fun getVertex(idx: Int): V {
		return _verts[idx]
	}

	/**
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	protected fun newEdge(from: Int, to: Int): E {
		return BEdge(from, to) as E
	}

	/**
	 *
	 * @param index
	 * @return
	 */
	fun getEdge(index: Int): E {
		return _edges[index]
	}

	/**
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	fun getEdge(from: Int, to: Int): E? {
		val index = Collections.binarySearch(_edges, BEdge(from, to))
		return if (index < 0) null else getEdge(index)
	}

	/**
	 *
	 * @param index
	 * @return
	 */
	fun getCell(index: Int): C {
		return _cells[index]
	}

	/**
	 *
	 */
	fun clear() {
		_verts.clear()
		_edges.clear()
		_cells.clear()
	}

	/**
	 *
	 */
	fun compute() {
		log.debug("COMPUTE BEGIN\n   numV:%d\n   numE:%d\n   numC:%d", _verts.size, _edges.size, _cells.size)
		for (v in _verts) {
			v.reset()
		}
		Collections.sort(_edges)
		Utils.unique(_edges)

		// compute verts adjacent to each other
		for (e in _edges) {
			e.reset()
			_verts[e.from].addAdjacentVertex(e.to)
			_verts[e.to].addAdjacentVertex(e.from)
		}

		// dfs search to compute the _cells
		val unvisited: MutableSet<Int> = LinkedHashSet()
		for (i in _verts.indices) {
			unvisited.add(i)
		}
		_cells.clear()
		while (unvisited.size > 0) {
			val v = unvisited.iterator().next()
			val lookup = Array(_verts.size) { IntArray(_verts.size) }
			val queue: Queue<IntArray> = LinkedList()
			dfsCellSearch(queue, 0, "", v, lookup, LinkedList<Int>(), unvisited)
			while (queue.size > 0) {
				val e = queue.remove()
				val v0 = e[0]
				val v1 = e[1]
				val cell = LinkedList<Int>()
				lookup[v0][v1] = 1
				cell.add(v0)
				dfsCellSearch(queue, 0, "", v1, lookup, cell, unvisited)
			}
		}
		/*
        // now compute the center of each cell and assign adjCells to edges.
        MutableVector2D mv = new MutableVector2D();
        for (int cIndex=0; cIndex<_cells.size(); cIndex++) {
            BCell c = _cells.get(cIndex);
            mv.zero();
            if (c.getNumAdjVerts()<3)
                throw new cc.lib.utils.GException("Invalid cell: " + cIndex);
            int p = c.getAdjVertex(c.getNumAdjVerts()-1);
            for (int vIndex : c.getAdjVerts()) {
                mv.addEq(verts.get(vIndex));
                int eIndex = getEdgeIndex(vIndex, p);
                if (eIndex < 0)
                    throw new cc.lib.utils.GException("Cannot find edge " + vIndex + "->" + p);
                BEdge e = getEdge(eIndex);
                e.addAdjCell(cIndex);
                p = vIndex;
                BVertex v = verts.get(vIndex);
                v.addAdjacentCell(cIndex);
            }
            mv.scaleEq(1.0f / c.getNumAdjVerts());
            c.cx = mv.getX();
            c.cy = mv.getY();
            // now compute the radius
            float magSquared = 0;
            for (int vIndex : c.getAdjVerts()) {
                Vector2D dv = Vector2D.sub(verts.get(vIndex), c);
                float m = dv.magSquared();
                if (m > magSquared)
                    magSquared = m;
            }
            c.radius = (float)Math.sqrt(magSquared);
        }*/

		// now iterate over the edges and create the cell adjacency
		val it: Iterator<E> = _edges.iterator()
		while (it.hasNext()) {
			val e: BEdge = it.next()
			if (e.numAdjCells == 2) {
				val c0: BCell = _cells[e.getAdjCell(0)]
				val c1: BCell = _cells[e.getAdjCell(1)]
				c0.addAdjCell(e.getAdjCell(1))
				c1.addAdjCell(e.getAdjCell(0))
			} else if (e.numAdjCells == 0) {
				//it.remove();
			}
		}
		log.debug("COMPUTE END\n   numV:%d\n   numE:%d\n   numC:%d", _verts.size, _edges.size, _cells.size)
	}

	fun computeCell(cellIdx: Int) {
		val cell: BCell = getCell(cellIdx)
		if (cell.numAdjVerts == 0) {
			log.warn("Cell has no adjacent vertices: $cellIdx")
			return
		}
		val mv = MutableVector2D()
		cell.radius = 0f
		for (adjIdx in cell.adjVerts) {
			mv.addEq(getVertex(adjIdx))
		}
		mv.scaleEq(1f / cell.numAdjVerts)
		cell.x = mv.x
		cell.y = mv.y
		for (adjIdx in cell.adjVerts) {
			cell.radius = Math.max(cell.radius, mv.minus(getVertex(adjIdx)).mag())
		}
	}

	/**
	 *
	 * @param pts
	 * @return
	 */
	protected open fun newCell(pts: List<Int>): C {
		return BCell(pts) as C
	}

	private fun dfsCellSearch(q: Queue<IntArray>, d: Int, indent: String, v: Int, visited: Array<IntArray>, cell: LinkedList<Int>, unvisited: MutableSet<Int>) {
		log.debug("%sDFS %d %s", indent, v, cell)
		val bv: BVertex = _verts[v]
		unvisited.remove(v as Any)
		if (cell.size > 2 && cell.contains(v)) {
			while (cell.first != v) {
				cell.removeFirst()
			}
			if (cell.size > 2) {
				log.debug("%sADD CELL %s", indent, cell)
				_cells.add(newCell(cell))
				cell.clear()
				return
			}
		}
		val adjacent = bv.adjVerts.toMutableList()
		val it = adjacent.iterator()
		while (it.hasNext()) {
			val vv = it.next()
			if (visited[v][vv] != 0) {
				it.remove()
			}
		}
		log.debug("%sADJ=%s", indent, adjacent)
		if (adjacent.size == 0) return
		if (cell.size > 0) {
			val prev = cell.last
			val removed = adjacent.remove(prev)
			if (adjacent.size > 0) {
				val dv: Vector2D = bv.minus(_verts[prev])
				log.debug("%sdv=%s", indent, dv)
				//val reference = arrayOfNulls<Float>(adjacent.size)
				//val target = arrayOfNulls<Int>(adjacent.size)
				//for (i in adjacent.indices) {
				//	val v2: Vector2D = verts[adjacent[i]].minus(bv)
				//	reference[i] = dv.angleBetweenSigned(v2)
				//	target[i] = adjacent[i]
				//}
				val target = adjacent.map {
					Pair(dv.angleBetweenSigned(_verts[it] - bv), it)
				}.sortedBy {
					it.first
				}
				adjacent.clearAndAddAll(target.map { it.second })
				//Utils.bubbleSort<Float, Int>(reference, target, true)
				//log.debug("%sSorted edges: %s", indent, Arrays.toString(target))
				//log.debug("%sReference vectors: %s", indent, Arrays.toString(reference))
				//adjacent.clear()
				//adjacent.addAll(target)
				if (target[0].first in 0.1f..179.9f) {
					val first = adjacent.removeAt(0)
					visited[v][first] = 1
					cell.add(v)
					dfsCellSearch(q, d + 1, "$indent ", first, visited, cell, unvisited)
				}
			}
		}
		for (vv in adjacent) {
			//cell = new LinkedList<>();
			//visited[v][vv] = 1;
			//cell.add(v);
			//dfsCellSearch(d+1, indent+" ", vv, visited, cell);
			q.add(intArrayOf(v, vv))
		}
		log.debug("%sEND", indent)
	}

	/**
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	fun getEdgeIndex(from: Int, to: Int): Int {
		if (from < 0 || from >= _verts.size) throw IndexOutOfBoundsException("From not in range 0-" + _verts.size)
		if (to < 0 || to >= _verts.size) throw IndexOutOfBoundsException("To not in range 0-" + _verts.size)
		return Collections.binarySearch(_edges, BEdge(from, to)) // TODO: Do we really want to use factory
	}

	val numVerts: Int
		/**
		 *
		 * @return
		 */
		get() = _verts.size
	val numEdges: Int
		/**
		 *
		 * @return
		 */
		get() = _edges.size
	val numCells: Int
		/**
		 *
		 * @return
		 */
		get() = _cells.size

	/**
	 *
	 * @param vIndex
	 */
	fun removeVertex(vIndex: Int) {
		// remove adjacency references to vIndex in other verts
		val lastV = _verts.size - 1
		for (i in _verts.indices) {
			if (i == vIndex) continue
			val v: BVertex = _verts[i]
			v.removeAndRenameAdjacentVertex(vIndex, lastV)
		}


		// remove all edges associated with this vertex
		val it = _edges.iterator()
		while (it.hasNext()) {
			val e: BEdge = it.next()
			if (e.from == vIndex || e.to == vIndex) {
				it.remove()
			} else if (e.from == lastV) {
				e.setFrom(vIndex)
			} else if (e.to == lastV) {
				e.setTo(vIndex)
			}
		}
		// remove this index from any _cells we are adjacent too
		// remove the cell if it becomes invalid
		for (i in _cells.indices) {
			val cell: BCell = _cells[i]
			cell.removeAndRenameAdjVertex(vIndex, lastV)
			if (cell.numAdjVerts < 3) {
				removeCell(i)
			}
		}

		// finally remove the vertex YAY!
		_verts[vIndex] = _verts[lastV]
		_verts.removeAt(lastV)
	}

	fun removeCell(idx: Int) {
		// remove occurrances of ourself from vertices, edges and other _cells
		val lastC = _cells.size - 1
		for (v in _verts) {
			v.removeAndRenameAdjacentCell(idx, lastC)
		}
		for (e in _edges) {
			e.removeAndReplaceAdjacentCell(idx, lastC)
		}
		for (adj in _cells[idx].adjCells) {
			_cells[adj].removeAndRenameAdjCell(idx, lastC)
		}
		_cells[idx] = _cells[_cells.size - 1]
		_cells.removeAt(_cells.size - 1)
	}

	/**
	 *
	 * @param from
	 * @param to
	 */
	fun removeEdge(from: Int, to: Int) {
		val eIndex = getEdgeIndex(from, to)
		if (eIndex >= 0) {
			_edges.removeAt(eIndex)
		}
	}

	/**
	 *
	 * @param index
	 */
	fun removeEdge(index: Int) {
		_edges.removeAt(index)
	}

	/**
	 *
	 * @param cell
	 * @return
	 */
	fun getAdjacentCells(cell: BCell): List<C> {
		return Utils.map(cell.adjCells) { idx: Int -> getCell(idx) }
	}

	/**
	 *
	 * @param cellIdx
	 * @return
	 */
	fun getAdjacentCell(cellIdx: Int): List<C> {
		return getAdjacentCells(getCell(cellIdx))
	}

	/**
	 *
	 * @param edge
	 * @return
	 */
	fun getAdjacentCells(edge: BEdge): Iterable<C> {
		return Iterable {
			object : Iterator<C> {
				var index = 0
				override fun hasNext(): Boolean {
					return index < edge.numAdjCells
				}

				override fun next(): C {
					return _cells[edge.getAdjCell(index++)]
				}
			}
		}
	}

	/**
	 *
	 * @param vertex
	 * @return
	 */
	fun getAdjacentCells(vertex: BVertex): Iterable<C> {
		return Iterable {
			object : Iterator<C> {
				var index = 0
				override fun hasNext(): Boolean {
					return index < vertex.numAdjCells
				}

				override fun next(): C {
					return _cells[vertex.getAdjCell(index++)]
				}
			}
		}
	}

	/**
	 *
	 * @param cellIndex
	 * @return
	 */
	fun getCellBoundingRect(cellIndex: Int): GRectangle {
		val cell: BCell = getCell(cellIndex)
		val min = MutableVector2D(cell)
		val max = MutableVector2D(cell)
		for (vIndex in cell.adjVerts) {
			val v: BVertex = getVertex(vIndex)
			min.minEq(v)
			max.maxEq(v)
		}
		return GRectangle(min, max)
	}

	/**
	 * Return true if vertex resides within the convex bounding polygon formed by the points
	 * @param vertex
	 * @param cIndex
	 * @return
	 */
	fun isPointInsideCell(vertex: IVector2D, cIndex: Int): Boolean {
		val c: C = _cells[cIndex] ?: return false

		// early out check against the radius
		if (vertex.minus(c).magSquared() > c.radius * c.radius) return false
		var p = c.numAdjVerts - 1
		var sign = 0
		for (i in 0 until c.numAdjVerts) {
			val pv: IVector2D = _verts[c.getAdjVertex(p)]
			val side: Vector2D = _verts[c.getAdjVertex(i)].minus(pv).normEq()
			val dv: Vector2D = vertex.minus(pv)
			val s = CMath.signOf(side.dot(dv))
			if (sign == 0) {
				sign = s
			} else if (sign != s) {
				return false
			}
			p = i
		}
		return true
	}

	val bounds: GRectangle
		get() {
			val min = MutableVector2D(Vector2D.MAX)
			val max = MutableVector2D(Vector2D.MIN)
			for (v in _verts) {
				if (v != null) {
					min.minEq(v)
					max.maxEq(max)
				}
			}
			return GRectangle(min, max)
		}

	/**
	 * Normailze the vertices so that board fits in rect (0,0)-(1,1) while keeping aspect ratio
	 */
	fun normalize() {
		val min = MutableVector2D(Vector2D.MAX)
		val max = MutableVector2D(Vector2D.MIN)
		for (v in _verts) {
			if (v == null) continue
			min.minEq(v)
			max.maxEq(v)
		}
		val dim = max.minus(min)
		if (dim.isNaN || dim.x == 0f || dim.y == 0f || dim.isInfinite) return
		val scale = 1.0f / Math.max(dim.x, dim.y)
		for (v in _verts) {
			if (v == null) continue
			val mv = MutableVector2D(v)
			mv.subEq(min)
			mv.scaleEq(scale)
			v.set(mv)
		}

		// now recompuet all the cell centers
		val mv = MutableVector2D()
		for (cIndex in _cells.indices) {
			val c: BCell = _cells[cIndex]
			mv.zeroEq()
			if (c.numAdjVerts < 3) throw GException("Invalid cell: $cIndex")
			val p = c.getAdjVertex(c.numAdjVerts - 1)
			for (vIndex in c.adjVerts) {
				mv.addEq(_verts[vIndex])
			}
			mv.scaleEq(1.0f / c.numAdjVerts)
			c.x = mv.x
			c.y = mv.y
			// now compute the radius
			var magSquared = 0f
			for (vIndex in c.adjVerts) {
				val dv: Vector2D = _verts[vIndex].minus(c)
				val m = dv.magSquared()
				if (m > magSquared) magSquared = m
			}
			c.radius = Math.sqrt(magSquared.toDouble()).toFloat()
		}
	}

	fun generateGrid(rows: Int, cols: Int, width: Float, height: Float) {
		val dx = (width - 1) / cols
		val dy = (height - 1) / rows
		for (i in 0..rows) {
			for (ii in 0..cols) {
				val index = addVertex(dx * ii, dy * i)
				if (ii > 0) {
					getOrAddEdge(index - 1, index)
				}
				if (i > 0) {
					getOrAddEdge(index - cols - 1, index)
				}
				if (i > 0 && ii > 0) {
					addCell(index - cols - 2, index - cols - 1, index, index - 1)
				}
			}
		}
	}

	fun setDimension(width: Float, height: Float) {
		_dimension = GDimension(width, height)
	}

	fun setDimension(dimension: IDimension) {
		_dimension = GDimension(_dimension)
	}

	fun moveVertexBy(idx: Int, dv: IVector2D) {
		val bv: BVertex = getVertex(idx)
		val v = MutableVector2D(bv)
		v.addEq(dv)
		bv.set(v)
		for (cellIdx in bv.adjCells) {
			computeCell(cellIdx)
		}
	}

	companion object {
		var log = LoggerFactory.getLogger(CustomBoard::class.java)

		init {
			addAllFields(CustomBoard::class.java)
		}
	}
}
