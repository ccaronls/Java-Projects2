package cc.lib.mirror

import cc.lib.ksp.mirror.Mirror
import cc.lib.mirror.processor.MirrorProcessor
import org.junit.Assert.assertNotNull
import org.junit.Test

@Mirror
interface MyMirroredThing {
	var x: Int
	var y: String
}


class MirrorTest {

	@Test
	fun testRegex() {

		val regex = "(Mutable)?List<(.+)>".toRegex()

		var result = regex.matchEntire("List<Int>")
		assertNotNull(result)
		println("groupValues : ${result!!.groupValues}")

		var output = "listOf<${result!!.groupValues[2]}>.toMirroredList()"
		println("output : $output")

		result = regex.matchEntire("List<List<Int>>")
		assertNotNull(result)
		println("groupValues : ${result!!.groupValues}")

		output = "listOf<${result!!.groupValues[2]}>.toMirroredList()"
		println("output : $output")

		result = regex.matchEntire("MutableList<String>")
		assertNotNull(result)
		println("groupValues : ${result!!.groupValues}")

		output = "listOf<${result!!.groupValues[2]}>.toMirroredList()"
		println("output : $output")

	}

	@Test
	fun test1() {

		arrayOf(
			"List<Int>",
			"MutableList<Int>",
			"MirroredArray<Char>",
			"MirroredArray<Char>",
			"MutableList<List<Int>>"
		).forEach {
			println("$it -> " + MirrorProcessor.match(it))
		}
	}

}