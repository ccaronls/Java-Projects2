package cc.experiments

import java.io.*
import java.util.*

/**
 *
 * @author Chris Caron
 */
class CrosswordPuzzleGenerator internal constructor(reader: BufferedReader, width: Int, height: Int) {
	/*
	 *
	 */
	@Throws(InvalidWordException::class)
	private fun validateWord(word: String) {
		for (element in word) {
			val c = element
			if (c < 'a' || c > 'z') throw InvalidWordException(word)
		}
	}

	/**
	 *
	 * @return
	 * @throws CantGeneratePuzzleException
	 */
	@Throws(CantGeneratePuzzleException::class)
	fun generate(): Grid {
		val g = Grid(width, height)
		if (generateR(g, 0)) {
			g.fillEmpty()
			return g
		}
		throw CantGeneratePuzzleException()
	}

	/*
	 *
	 */
	private fun generateR(g: Grid, wordIndex: Int): Boolean {
		if (wordIndex >= words.size) return true
		val copy = Grid(width, height)

		// iterate over the grid and try to place the word
		for (i in 0 until width) {
			for (j in 0 until height) {
				// copy the grid
				copy.copy(g)
				if (placeWord(copy, words[wordIndex], i, j, false)) {
					if (generateR(copy, wordIndex + 1)) {
						g.copy(copy)
						return true
					}
				}
				copy.copy(g)
				if (placeWord(copy, words[wordIndex], i, j, true)) {
					if (generateR(copy, wordIndex + 1)) {
						g.copy(copy)
						return true
					}
				}
			}
		}
		return false
	}

	/*
	 *
	 */
	private fun placeWord(g: Grid, word: String, x: Int, y: Int, horz: Boolean): Boolean {
		debug("placeWord [$word] x [$x] y [$y] horz [$horz]")
		if (g[x, y].toInt() == 0 && !g.canPlaceChar('#', x, y, Grid.FLAG_ALL)) return false
		// place the first char
		if (!g.canPlaceChar(word[0], x, y, 0)) return false
		g[x, y] = word[0]
		if (horz) {
			if (word.length > width - x) return false
			var i: Int
			i = 1
			while (i < word.length - 1) {
				if (g.canPlaceChar(word[i], x + i, y, Grid.FLAG_HORZ)) {
					g[x + i, y] = word[i]
				} else {
					return false
				}
				i++
			}
			// the last char cannot have a trailing char
			if (g.canPlaceChar(word[i], x + i, y, Grid.FLAG_HORZ or Grid.FLAG_RIGHT)) {
				g[x + i, y] = word[i]
			} else {
				return false
			}
		} else {
			if (word.length > height - y) return false
			var i: Int
			i = 1
			while (i < word.length - 1) {
				if (g.canPlaceChar(word[i], x, y + i, Grid.FLAG_VERT)) {
					g[x, y + i] = word[i]
				} else {
					return false
				}
				i++
			}
			// the last char cannot have a trailing char
			if (g.canPlaceChar(word[i], x, y + i, Grid.FLAG_VERT or Grid.FLAG_BOTTOM)) {
				g[x, y + i] = word[i]
			} else {
				return false
			}
		}
		return true
	}

	/*
	 *
	 *
	private void printBar(PrintStream out) {
		for (int i=0; i<width*2+1; i++) {
			out.print("-");
		}
		out.println();
	}

	/ *
	 *
	 */
	private fun debug(msg: String) {
		//System.out.println(msg);
		//System.out.print(".");
	}

	/**
	 *
	 * @author Chris Caron
	 */
	class InvalidWordException(word: String) : Exception("Invalid word [$word] word can contain only letters")

	/**
	 *
	 * @author Chris Caron
	 */
	class CantGeneratePuzzleException : Exception("Failed to generate puzzle from inputs")

	/**
	 *
	 * @author Chris Caron
	 */
	class Grid(width: Int, height: Int) {
		private val grid: Array<CharArray>
		operator fun get(x: Int, y: Int): Char {
			return if (x < 0 || y < 0 || x >= grid.size || y >= grid[x].size) Char.MIN_VALUE else grid[x][y]
		}

		operator fun set(x: Int, y: Int, c: Char) {
			grid[x]!![y] = c
		}

		fun copy(g: Grid) {
			for (i in grid.indices) {
				for (j in 0 until grid[i].size) {
					set(i, j, g[i, j])
				}
			}
		}

		fun display(out: PrintStream) {
			//printBar(out);
			for (i in grid.indices) {
				out.print(FILL_CHAR)
				for (j in 0 until grid[i].size) {
					out.print(grid[i]!![j].toString() + FILL_CHAR.toString())
				}
				out.println()
				//printBar(out);
			}
		}

		val FILL_CHAR = ' '
		fun canPlaceChar(c: Char, x: Int, y: Int, adjacencyFlag: Int): Boolean {
			if (get(x, y) == c) return true
			if (adjacencyFlag and 1 shl 3 != 0 && get(x - 1, y).toInt() != 0) return false
			if (adjacencyFlag and 1 shl 2 != 0 && get(x + 1, y).toInt() != 0) return false
			if (adjacencyFlag and 1 shl 1 != 0 && get(x, y - 1).toInt() != 0) return false
			if (adjacencyFlag and 1 shl 0 != 0 && get(x, y + 1).toInt() != 0) return false
			return if (get(x, y).toInt() == 0) true else false
		}

		fun fillEmpty() {
			for (i in grid.indices) {
				for (j in 0 until grid[i].size) {
					if (get(i, j).toInt() == 0) set(i, j, FILL_CHAR)
				}
			}
		}

		companion object {
			const val FLAG_LEFT = 1 shl 3
			const val FLAG_RIGHT = 1 shl 2
			const val FLAG_TOP = 1 shl 1
			const val FLAG_BOTTOM = 1 shl 0
			const val FLAG_HORZ = FLAG_TOP or FLAG_BOTTOM
			const val FLAG_VERT = FLAG_LEFT or FLAG_RIGHT
			const val FLAG_ALL = FLAG_HORZ or FLAG_VERT
		}

		init {
			grid = Array(width) {
				CharArray(height)
			}
		}
	}

	private val width: Int
	private val height: Int
	private val words: Array<String>

	companion object {
		/**
		 *
		 * @param args
		 */
		@JvmStatic
		fun main(args: Array<String>) {
			if (args.size < 3) {
				val usage = """
	            	Not Enough args, fuond [${args.size}] expected 3
	            	USAGE: CWGen <wordlistfile> <width> <height>
	            	""".trimIndent()
				println(usage)
				System.exit(1)
			}
			try {
				val file = File(args[0])
				val width = args[1].toInt()
				val height = args[2].toInt()
				val reader = BufferedReader(FileReader(file))
				val c = CrosswordPuzzleGenerator(reader, width, height)
				val g = c.generate()
				g.display(System.out)
			} catch (e: IOException) {
				System.err.println("Initialization error [" + e.message + "] invalid usage")
				System.exit(1)
			} catch (e: InvalidWordException) {
				System.err.println(e.message)
				System.exit(1)
			} catch (e: CantGeneratePuzzleException) {
				System.err.println(e.message)
				System.exit(1)
			} catch (e: Exception) {
				e.printStackTrace()
				System.exit(1)
			}
		}
	}

	/*
	 *
	 */
	init {
		val words = ArrayList<String>()
		while (true) {
			var word = reader.readLine() ?: break
			if (word.length <= 0) continue
			word = word.trim { it <= ' ' }.toLowerCase()
			validateWord(word)
			debug("Adding word [$word]")
			words.add(word)
		}
		reader.close()
		if (words.size <= 0) throw IOException("No words found")
		this.width = width
		this.height = height
		this.words = words.toTypedArray() as Array<String>
	}
}