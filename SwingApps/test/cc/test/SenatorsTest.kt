package cc.test

import org.junit.Test

/**
 * Created by Chris Caron on 9/19/24.
 */
class SenatorsTest {

	@Test
	fun test() {
		senators("DR")
		senators("RD")
		senators("RDD")
		senators("RDRDRDRDRDRDRD")
		senators("DRDRDRDRDRDRDR")
	}

	fun senators(_input: String) {

		println("-------------------------------")
		println("input: $_input")
		val input = _input.filter { it == 'R' || it == 'D' }.toMutableList()

		if (input.isEmpty()) {
			println("Invalid input")
			return
		}

		while (input.size > 1) {
			if (input[0] == 'R') {
				input.indexOfFirst { it == 'D' }.takeIf { it > 0 }?.let {
					input.removeAt(it)
				} ?: break
			} else if (input[0] == 'D') {
				input.indexOfFirst { it == 'R' }.takeIf { it > 0 }?.let {
					input.removeAt(it)
				} ?: break
			} else {
				println("Invalid input ${input[0]}")
				return
			}
			input.add(input.removeFirst())
		}

		println("winner: ${input[0]}")
	}


}