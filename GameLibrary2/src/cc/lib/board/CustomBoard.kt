package cc.lib.board

import cc.lib.game.AGraphics
import cc.lib.game.APGraphics
import cc.lib.game.GColor
import cc.lib.game.GDimension
import cc.lib.game.GRectangle
import cc.lib.game.IVector2D
import cc.lib.game.Justify
import cc.lib.logger.LoggerFactory
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.math.signOf
import cc.lib.utils.GException
import cc.lib.utils.bubbleSort
import cc.lib.utils.getOrNull
import java.util.Arrays
import java.util.Collections
import java.util.LinkedList
import java.util.Queue
import java.util.Vector

class CustomBoard {
	private val verts = Vector<BVertex>()
	private val edges = Vector<BEdge>()
	private val cells = Vector<BCell>()
	var dimension = GDimension()

	/**
	 *
	 * @param g
	 */
	fun drawEdges(g: AGraphics) {
		g.begin()
		for (e in edges) {
			g.vertex(verts[e.from])
			g.vertex(verts[e.to])
		}
		g.drawLines()
	}

	/**
	 *
	 * @param g
	 */
	fun drawVerts(g: AGraphics) {
		g.begin()
		for (v in verts) {
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
		for (v in verts) {
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
		for (e in edges) {
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
		val newX: Float = v.x.coerceIn(radius, g.viewportWidth - radius)
		val newY: Float = v.y.coerceIn(radius, g.viewportHeight - radius)
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
		for (c in cells) {
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
			val v0 = verts[p]
			val v3 = verts[v]
			val e = v3 - v0
			var ue = e.normalized()
			val v1 = v3 - ue
			ue = ue.norm()
			val v2 = v1 + ue
			val v4 = v1 - ue
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
	fun getMidpoint(e: BEdge): Vector2D = (verts[e.from] + verts[e.to]) / .5f

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
			g.vertex(verts[v])
		}
		g.popMatrix()
	}

	/**
	 *
	 * @param g
	 * @param scale
	 */
	fun drawCells(g: AGraphics, scale: Float) {
		for (b in cells) {
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
		for (v in verts) {
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
		for (index in verts.indices) {
			if (verts[index] == null) {
				verts[index] = newVertex(v)
				return index
			}
		}
		val index = verts.size
		verts.add(newVertex(v))
		return index
	}

	/**
	 *
	 * @param v
	 * @return
	 */
	protected fun newVertex(v: IVector2D): BVertex {
		return BVertex(v)
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
		var idx = Collections.binarySearch(edges, edge)
		if (idx < 0) {
			idx = -idx - 1
			edges.insertElementAt(edge, idx)
		}
		return idx
	}

	/**
	 *
	 * @param e
	 */
	fun addEdge(e: BEdge) {
		edges.add(e)
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
		for (e in edges) {
			g.setName(index++)
			renderEdge(e, g)
		}
		return g.pickLines(mx, my, 5)
	}

	fun pickEdge(g: APGraphics, mouse: IVector2D): Int {
		g.begin()
		var index = 0
		for (e in edges) {
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
		return addCell(*verts)
	}

	fun addCell(verts: List<Int>): Int {
		val index = cells.size
		cells.add(newCell(verts))
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
		for (c in cells) {
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
		g.vertex(verts[e.from])
		g.vertex(verts[e.to])
	}

	fun <V : BVertex> getVertex(idx: Int): V {
		return verts[idx] as V
	}

	/**
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	protected fun newEdge(from: Int, to: Int): BEdge {
		return BEdge(from, to)
	}

	/**
	 *
	 * @param index
	 * @return
	 */
	fun <E : BEdge> getEdge(index: Int): E {
		return edges[index] as E
	}

	/**
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	fun <E : BEdge> getEdge(from: Int, to: Int): E? {
		val index = Collections.binarySearch(edges, BEdge(from, to))
		return if (index < 0) null else getEdge(index)
	}

	/**
	 *
	 * @param index
	 * @return
	 */
	fun <C : BCell> getCell(index: Int): C {
		return cells[index] as C
	}

	/**
	 *
	 */
	fun clear() {
		verts.clear()
		edges.clear()
		cells.clear()
	}

	/**
	 *
	 */
	fun compute() {
		log.debug("COMPUTE BEGIN\n   numV:%d\n   numE:%d\n   numC:%d", verts.size, edges.size, cells.size)
		for (v in verts) {
			v.reset()
		}
		val sorted = edges.toSortedSet().toList()
		edges.clear()
		edges.addAll(sorted)

		// compute verts adjacent to each other
		for (e in edges) {
			e.reset()
			verts[e.from].addAdjacentVertex(e.to)
			verts[e.to].addAdjacentVertex(e.from)
		}

		// dfs search to compute the cells
		val unvisited: MutableSet<Int> = LinkedHashSet()
		for (i in verts.indices) {
			unvisited.add(i)
		}
		cells.clear()
		while (unvisited.size > 0) {
			val v = unvisited.iterator().next()
			val lookup = Array(verts.size) { IntArray(verts.size) }
			val queue: Queue<IntArray> = LinkedList()
			dfsCellSearch(queue, 0, "", v, lookup, LinkedList(), unvisited)
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
        for (int cIndex=0; cIndex<cells.size(); cIndex++) {
            BCell c = cells.get(cIndex);
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
		val it: Iterator<BEdge> = edges.iterator()
		while (it.hasNext()) {
			val e: BEdge = it.next()
			if (e.numAdjCells == 2) {
				val c0: BCell = cells[e.getAdjCell(0)]
				val c1: BCell = cells[e.getAdjCell(1)]
				c0.addAdjCell(e.getAdjCell(1))
				c1.addAdjCell(e.getAdjCell(0))
			} else if (e.numAdjCells == 0) {
				//it.remove();
			}
		}
		log.debug("COMPUTE END\n   numV:%d\n   numE:%d\n   numC:%d", verts.size, edges.size, cells.size)
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
	protected fun newCell(pts: List<Int>): BCell {
		return BCell(pts)
	}

	private fun dfsCellSearch(
		q: Queue<IntArray>,
		d: Int,
		indent: String,
		v: Int,
		visited: Array<IntArray>,
		cell: LinkedList<Int>,
		unvisited: MutableSet<Int>
	) {
		log.debug("%sDFS %d %s", indent, v, cell)
		val bv: BVertex = verts[v]
		unvisited.remove(v as Any)
		if (cell.size > 2 && cell.contains(v)) {
			while (cell.first != v) {
				cell.removeFirst()
			}
			if (cell.size > 2) {
				log.debug("%sADD CELL %s", indent, cell)
				cells.add(newCell(cell))
				cell.clear()
				return
			}
		}
		val adjacent = bv.adjVerts
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
			val removed = adjacent.remove(prev as Any)
			if (adjacent.size > 0) {
				val dv: Vector2D = bv - verts[prev]
				log.debug("%sdv=%s", indent, dv)
				val target = adjacent.toTypedArray()
				val reference = Array(adjacent.size) {
					val v2 = verts[adjacent[it]] - bv
					dv.angleBetweenSigned(v2)
				}
				bubbleSort(reference, target, true)
				log.debug("%sSorted edges: %s", indent, Arrays.toString(target))
				log.debug("%sReference vectors: %s", indent, Arrays.toString(reference))
				adjacent.clear()
				adjacent.addAll(target)
				if (reference[0] in 0.1f..179.9f) {
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
		if (from < 0 || from >= verts.size) throw IndexOutOfBoundsException("From not in range 0-" + verts.size)
		if (to < 0 || to >= verts.size) throw IndexOutOfBoundsException("To not in range 0-" + verts.size)
		return Collections.binarySearch(edges, BEdge(from, to)) // TODO: Do we really want to use factory
	}

	val numVerts: Int
		/**
		 *
		 * @return
		 */
		get() = verts.size
	val numEdges: Int
		/**
		 *
		 * @return
		 */
		get() = edges.size
	val numCells: Int
		/**
		 *
		 * @return
		 */
		get() = cells.size

	/**
	 *
	 * @param vIndex
	 */
	fun removeVertex(vIndex: Int) {
		// remove adjacency references to vIndex in other verts
		val lastV = verts.size - 1
		for (i in verts.indices) {
			if (i == vIndex) continue
			val v: BVertex = verts[i]
			v.removeAndRenameAdjacentVertex(vIndex, lastV)
		}


		// remove all edges associated with this vertex
		val it = edges.iterator()
		while (it.hasNext()) {
			val e: BEdge = it.next()
			if (e.from == vIndex || e.to == vIndex) {
				it.remove()
			} else if (e.from == lastV) {
				e.from = vIndex
			} else if (e.to == lastV) {
				e.to = vIndex
			}
		}
		// remove this index from any cells we are adjacent too
		// remove the cell if it becomes invalid
		for (i in cells.indices) {
			val cell: BCell = cells[i]
			cell.removeAndRenameAdjVertex(vIndex, lastV)
			if (cell.numAdjVerts < 3) {
				removeCell(i)
			}
		}

		// finally remove the vertex YAY!
		verts[vIndex] = verts[lastV]
		verts.removeAt(lastV)
	}

	fun removeCell(idx: Int) {
		// remove occurrances of ourself from vertices, edges and other cells
		val lastC = cells.size - 1
		for (v in verts) {
			v.removeAndRenameAdjacentCell(idx, lastC)
		}
		for (e in edges) {
			e.removeAndReplaceAdjacentCell(idx, lastC)
		}
		for (adj in cells[idx].adjCells) {
			cells[adj].removeAndRenameAdjCell(idx, lastC)
		}
		cells[idx] = cells[cells.size - 1]
		cells.removeAt(cells.size - 1)
	}

	/**
	 *
	 * @param from
	 * @param to
	 */
	fun removeEdge(from: Int, to: Int) {
		val eIndex = getEdgeIndex(from, to)
		if (eIndex >= 0) {
			edges.removeAt(eIndex)
		}
	}

	/**
	 *
	 * @param index
	 */
	fun removeEdge(index: Int) {
		edges.removeAt(index)
	}

	/**
	 *
	 * @param cell
	 * @return
	 */
	fun getAdjacentCells(cell: BCell): List<BCell> {
		return cell.adjCells.map { idx -> getCell(idx) }
	}

	/**
	 *
	 * @param cellIdx
	 * @return
	 */
	fun getAdjacentCell(cellIdx: Int): List<BCell> {
		return getAdjacentCells(getCell(cellIdx))
	}

	/**
	 *
	 * @param edge
	 * @return
	 */
	fun getAdjacentCells(edge: BEdge) = Array(edge.numAdjCells) { edge.getAdjCell(it) }.toList()

	/**
	 *
	 * @param vertex
	 * @return
	 */
	fun getAdjacentCells(vertex: BVertex) = Array(vertex.numAdjCells) { vertex.getAdjCell(it) }.toList()

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
		val c = cells.getOrNull(cIndex) ?: return false

		// early out check against the radius
		if (vertex.minus(c).magSquared() > c.radius * c.radius) return false
		var p = c.numAdjVerts - 1
		var sign = 0
		for (i in 0 until c.numAdjVerts) {
			val pv: IVector2D = verts[c.getAdjVertex(p)]
			val side: Vector2D = verts[c.getAdjVertex(i)].minus(pv).normEq()
			val dv: Vector2D = vertex - pv
			val s = side.dot(dv).signOf()
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
			val min = MutableVector2D(IVector2D.MAX)
			val max = MutableVector2D(IVector2D.MIN)
			for (v in verts) {
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
		val min = MutableVector2D(IVector2D.MAX)
		val max = MutableVector2D(IVector2D.MIN)
		for (v in verts) {
			if (v == null) continue
			min.minEq(v)
			max.maxEq(v)
		}
		val dim = max.minus(min)
		if (dim.isNaN || dim.x == 0f || dim.y == 0f || dim.isInfinite) return
		val scale = 1.0f / dim.x.coerceAtLeast(dim.y)
		for (v in verts) {
			if (v == null) continue
			val mv = MutableVector2D(v)
			mv.subEq(min)
			mv.scaleEq(scale)
			v.set(mv)
		}

		// now recompuet all the cell centers
		val mv = MutableVector2D()
		for (cIndex in cells.indices) {
			val c: BCell = cells[cIndex]
			mv.zero()
			if (c.numAdjVerts < 3) throw GException("Invalid cell: $cIndex")
			val p = c.getAdjVertex(c.numAdjVerts - 1)
			for (vIndex in c.adjVerts) {
				mv.addEq(verts[vIndex])
			}
			mv.scaleEq(1.0f / c.numAdjVerts)
			c.x = mv.x
			c.y = mv.y
			// now compute the radius
			var magSquared = 0f
			for (vIndex in c.adjVerts) {
				val dv = verts[vIndex] - c
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

	fun getCells(): Iterable<BCell> {
		return cells
	}

	fun setDimension(width: Float, height: Float) {
		dimension = GDimension(width, height)
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
	}
}
