package cc.lib.reflector

import cc.lib.utils.Grid

/**
 * A grid is a 2D array of generic type with methods to perform operations
 * On its elements as well as the size of the grid
 *
 * @param <T>
</T> */
class DirtyGrid<T>(rows: Int, cols: Int, filler: (Pos) -> T) : Grid<T>(rows, cols, filler) {

	override fun isDirty(): Boolean {
		if (super.isDirty()) {
			return true
		}

		for (row in grid) {
			for (e in row) {
				if ((e as? IDirty)?.isDirty == true) {
					markDirty()
					return true
				}
			}
		}

		return false
	}

	override fun markClean() {
		super.markClean()
		for (row in grid) {
			for (e in row) {
				(e as? IDirty)?.markClean()
			}
		}
	}

	override fun serializeDirty(out: RPrintWriter, ignoreNonDirtyTypes: Boolean) {
		out.p("rows=$rows").println()
		out.p("cols=$cols").println()
		for (row in 0 until rows) {
			for (col in 0 until cols) {
				val obj = grid[row][col]
				if (obj is IDirty) {
					if (obj.isDirty) {
						out.p("$row,$col=${getCanonicalName(obj.javaClass)} ")
						out.push()
						obj.serializeDirty(out, ignoreNonDirtyTypes)
						out.pop()
					}
				} else if (!ignoreNonDirtyTypes && isDirty) {

				}
			}
		}
	}

	override fun merge(input: RBufferedReader) {
		fun String.parse(startsWith: String, parser: (String) -> Any): Any {
			if (!startsWith(startsWith))
				throw Exception("Expected $startsWith but got $this")
			return parser(substring(startsWith.length))
		}

		val r = input.readLineOrEOF()?.parse("rows=") { Integer.valueOf(it) } as Int
		val c = input.readLineOrEOF()?.parse("cols=") { Integer.valueOf(it) } as Int

		if (r != rows || c != cols)
			throw Exception("Cannot merge incoming DirtyGrid unless dimensions match. Incoming dim (${r}x$c) and existing is (${rows}x$cols)")

		while (true) {
			input.markDepth()
			try {
				val line = input.readLineOrEOF() ?: break
				val parts = line.split(",", "=")
				if (parts.size < 3)
					throw Exception("Parse ${parts.size} parts but expected 3")
				val row = parts[0].toInt()
				val col = parts[1].toInt()
				val type = getClassForName(parts[2])

				val obj = parse(grid[row][col], type, input, true)
				grid[row][col] = obj as T
			} finally {
				input.restoreDepth()
			}
		}
	}

	override fun deepCopy(): Grid<T> {
		return DirtyGrid(rows, cols) {
			deepCopy(get(it))
		}
	}
}