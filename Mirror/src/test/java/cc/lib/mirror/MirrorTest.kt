package cc.lib.mirror

import org.junit.Test

@cc.lib.mirror.annotation.Mirror
interface MyMirroredThing {
	var x: Int
	var y: String
}


class MirrorTest {

	@Test
	fun testMirror() {

	}

}