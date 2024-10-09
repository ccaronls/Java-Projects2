package cc.lib.game

import cc.lib.utils.randRange
import cc.lib.utils.randomPositive
import java.util.LinkedList
import java.util.Stack

/**
 * Class to generate draw a basic maze of N x N squares.
 * Can customize how generation works by overriding directionHeuristic and nextIndex
 *
 * @author chriscaron
 */
class Maze(width: Int, height: Int) {
	// wall flags organized in CCW order, this is important DO NOT
	// REORGANIZE!
	enum class Compass(val flag: Int, val dx: Int, val dy: Int) {
		NORTH(1, 0, -1),
		EAST(2, 1, 0),
		SOUTH(4, 0, 1),
		WEST(8, -1, 0);

		fun opposite(): Compass {
			val len = entries.size
			return entries[(ordinal + len / 2) % len]
		}
	}

	private val COMPASS_LEN = Compass.entries.size
	private val UNVISITED = (1 shl COMPASS_LEN) - 1 // all directions walled
	var width = 0
		private set
	var height = 0
		private set
	private lateinit var maze: Array<IntArray>
	var startX = 0
		private set
	var startY = 0
		private set
	var endX = 0
		private set
	var endY = 0
		private set
	private var solution: List<Compass>? = null

	init {
		resize(width, height)
	}

	/**
	 * Resets the maze to a new dimension.  The maze will not be generated.
	 * @param newWidth
	 * @param newHeight
	 */
	fun resize(newWidth: Int, newHeight: Int) {
		if (newWidth < 2 || newHeight < 2) throw RuntimeException("Illegal sized maze " + newWidth + "x" + newHeight)
		width = newWidth
		height = newHeight
		maze = Array(width) { IntArray(height) }
	}

	/**
	 * Mark all cells as UNVISITED.  Called prior to generate automatically.
	 */
	fun clear() {
		solution = null
		for (i in 0 until width) {
			for (ii in 0 until height) {
				maze[i][ii] = UNVISITED
			}
		}
	}

	/**
	 * Generate a random path using recursive DFS search.  This form tend to generate mazes with a long path and not alot of branches.
	 */
	fun generateDFS() {
		clear()
		generateR(width.randomPositive(), height.randomPositive())
	}

	private fun generateR(x: Int, y: Int) {
		val dir = Compass.entries.toTypedArray()
		dir.shuffle()
		for (c in dir) {
			val nx = x + c.dx
			val ny = y + c.dy
			if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue
			if (maze[nx][ny] == UNVISITED) {
				breakWall(x, y, c)
				breakWall(nx, ny, c.opposite())
				generateR(nx, ny)
			}
		}
	}

	/**
	 * Generate a random path user recursize BFS search. This form tends to
	 */
	fun generateBFS() {
		clear()
		val Q = ArrayList<IntArray>()
		val min = ArrayList<Int>()
		Q.add(intArrayOf(width.randomPositive(), height.randomPositive(), 1))
		while (!Q.isEmpty()) {
			min.clear()
			var m = Int.MAX_VALUE
			for (i in Q.indices) {
				val q = Q[i]
				if (q[2] > m) {
					continue
				}
				if (q[2] < m) {
					m = q[2]
					min.clear()
				}
				min.add(i)
			}
			val index = min[min.size.randomPositive()]
			val xy = Q.removeAt(index)
			val x = xy[0]
			val y = xy[1]
			val l = xy[2]
			val dir = Compass.entries.toTypedArray()
			dir.shuffle()
			var num = randRange(1, 3)
			for (c in dir) {
				val nx = x + c.dx
				val ny = y + c.dy
				if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue
				if (maze[nx][ny] != UNVISITED) continue
				breakWall(x, y, c)
				breakWall(nx, ny, c.opposite())
				Q.add(intArrayOf(nx, ny, l + 1))
				if (--num == 0) break
			}
		}
	}

	/**
	 * Search all dead ends and set start/end to the ends that result in the longest path
	 */
	fun setStartEndToLongestPath() {
		val ends: MutableList<IntArray> = ArrayList()
		for (i in 0 until width) {
			for (ii in 0 until height) {
				if (isDeadEnd(i, ii)) {
					ends.add(intArrayOf(i, ii))
				}
			}
		}
		var d = 0
		var s = 0
		var e = 0
		for (i in 0 until ends.size - 1) {
			for (ii in i + 1 until ends.size) {
				if (i == ii) continue
				val sxy = ends[i]
				val exy = ends[ii]
				val path = findPath(sxy[0], sxy[1], exy[0], exy[1])
				val len = path.size
				if (len > d) {
					d = len
					s = i
					e = ii
				}
			}
		}
		startX = ends[s][0]
		startY = ends[s][1]
		endX = ends[e][0]
		endY = ends[e][1]
		solution = findSolution()
	}

	/**
	 * Break the wall at xy and x+dx, y+dy where dx/dy is derived form dir.  dir = 0,1,2,3 for NORTH,EAST,SOUTH,WEST.
	 *
	 * Default behavior is random
	 * @param x
	 * @param y
	 * @param dir
	 */
	fun breakWall(x: Int, y: Int, dir: Compass) {
		maze[x][y] = maze[x][y] and dir.flag.inv()
	}

	/**
	 * Return true iff there is a wall at x,y pointing in direction d
	 * @param x
	 * @param y
	 * @param dir
	 * @return
	 */
	fun isWall(x: Int, y: Int, dir: Compass): Boolean {
		return 0 != maze[x][y] and dir.flag
	}

	/**
	 * Return true if the cell at x,y has 3 walls
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	fun isDeadEnd(x: Int, y: Int): Boolean {
		val mask = maze[x][y]
		for (i in 0 until COMPASS_LEN) {
			val m = (1 shl i).inv() and UNVISITED
			if (mask == m) return true
		}
		return false
	}

	fun draw(g: AGraphics, lineThickness: Float) {
		for (i in 0 until width) {
			for (ii in 0 until height) {
				g.begin()
				val cell = maze[i][ii]
				if (0 != cell and Compass.NORTH.flag) {
					g.vertex(i.toFloat(), ii.toFloat())
					g.vertex((i + 1).toFloat(), ii.toFloat())
				}
				if (0 != cell and Compass.SOUTH.flag) {
					g.vertex(i.toFloat(), (ii + 1).toFloat())
					g.vertex((i + 1).toFloat(), (ii + 1).toFloat())
				}
				if (0 != cell and Compass.EAST.flag) {
					g.vertex((i + 1).toFloat(), ii.toFloat())
					g.vertex((i + 1).toFloat(), (ii + 1).toFloat())
				}
				if (0 != cell and Compass.WEST.flag) {
					g.vertex(i.toFloat(), ii.toFloat())
					g.vertex(i.toFloat(), (ii + 1).toFloat())
				}
				g.drawLines(lineThickness)
				if (DEBUG) {
					if (isDeadEnd(i, ii)) {
						g.drawCircle(0.5f + i, 0.5f + ii, 0.4f)
					}
				}
			}
		}
		if (DEBUG && solution != null) {
			g.begin()
			var x = 0.5f + startX
			var y = 0.5f + startY
			g.vertex(x, y)
			for (c in solution!!) {
				x += c.dx.toFloat()
				y += c.dy.toFloat()
				g.vertex(x, y)
			}
			g.drawLineStrip()
		}
	}

	fun setStart(x: Int, y: Int) {
		startX = x.coerceIn(0 until width)
		startY = y.coerceIn(0 until height)
	}

	fun setEnd(x: Int, y: Int) {
		endX = x.coerceIn(0 until width)
		endY = y.coerceIn(0 until height)
	}

	/**
	 * return true if there is a direct open path from p0 -> p1.  A recursive search is used.
	 * @param x0
	 * @param y0
	 * @param x1
	 * @param y1
	 * @return
	 */
	fun isOpen(x0: Int, y0: Int, x1: Int, y1: Int): Boolean {
		// simple case
		return if (x0 < 0 || y0 < 0 || x0 >= width || y0 >= height || x1 < 0 || y1 < 0 || x1 >= width || y1 >= height) false else isOpenR(
			x0,
			y0,
			x1,
			y1
		)
	}

	private fun isOpenR(x0: Int, y0: Int, x1: Int, y1: Int): Boolean {
		if (x0 == x1 && y0 == y1) return true
		//println("isOpenR x0=$x0 y0=$y0 x1=$x1 y1=$y1")
		if (x1 < x0) {
			if (0 == maze[x0][y0] and Compass.WEST.flag) {
				if (isOpenR(x0 - 1, y0, x1, y1)) return true
			}
		}
		if (x1 > x0) {
			if (0 == maze[x0][y0] and Compass.EAST.flag) {
				if (isOpenR(x0 + 1, y0, x1, y1)) return true
			}
		}
		if (y1 < y0) {
			if (0 == maze[x0][y0] and Compass.NORTH.flag) {
				if (isOpenR(x0, y0 - 1, x1, y1)) return true
			}
		}
		if (y1 > y0) {
			if (0 == maze[x0][y0] and Compass.SOUTH.flag) {
				if (isOpenR(x0, y0 + 1, x1, y1)) return true
			}
		}
		return false
	}

	/**
	 * Return a list of DIRECTION flags.
	 *
	 * @param sx
	 * @param sy
	 * @param ex
	 * @param ey
	 * @return
	 */
	fun findPath(sx: Int, sy: Int, ex: Int, ey: Int): List<Compass> {
		val visited = Array(width) { IntArray(height) }
		val path = LinkedList<Compass>()
		findPathR(visited, path, sx, sy, ex, ey)
		return path
	}

	/**
	 *
	 * @return
	 */
	fun findSolution(): List<Compass> {
		return findPath(startX, startY, endX, endY)
	}

	// non-recursive that uses the heap...to prevent stack overflow
	private fun findPathDFS(path: LinkedList<Compass>, x: Int, y: Int, ex: Int, ey: Int): Boolean {
		var x = x
		var y = y
		val S = Stack<IntArray>()
		S.add(intArrayOf(x, y, 0))
		while (!S.isEmpty()) {
			val q = S.peek()
			x = q[0]
			y = q[1]
			if (x == ex && y == ey) return true // done
			val c = Compass.entries[q[2]]
			while (c.ordinal < COMPASS_LEN) {
				if (!isWall(x, y, c)) {
					path.addLast(c)
					S.add(intArrayOf(x + c.dx, y + c.dy, c.ordinal))
				}
			}
		}
		return false
	}

	private fun findPathR(visited: Array<IntArray>, path: LinkedList<Compass>, x: Int, y: Int, ex: Int, ey: Int): Boolean {
		if (x == ex && y == ey) return true
		if (visited[x][y] != 0) return false
		visited[x][y] = 1
		for (c in Compass.entries) {
			if (!isWall(x, y, c)) {
				path.addLast(c)
				if (findPathR(visited, path, x + c.dx, y + c.dy, ex, ey)) {
					return true
				}
				path.removeLast()
			}
		}
		return false
	}

	companion object {
		var DEBUG = false
	}
}
