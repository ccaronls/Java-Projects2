package cc.lib.game

import org.junit.Assert
import org.junit.Test

/**
 * Created by chriscaron on 2/14/18.
 */
class AGraphicsTest() {

	@Test
	fun testAnnotations() {
		val pa = AGraphics.ANNOTATION_PATTERN
		val color1 = "[0,0,0]"
		val color2 = "[0,0,0,0]"
		val color3 = "[20,20,20]"
		val color4 = "[100,100,100,100]"
		val color5 = "[1,2,3,4]"
		val all = arrayOf(
			color1, color2, color3, color4, color5
		)
		for (test in all) {
			val m = pa.matcher(test)
			Assert.assertTrue("Failed for string: $test", m.find())
		}
		val test = "hello $color1 this $color2 is a test + $color3"
		val m = pa.matcher(test)
		Assert.assertTrue("Failed for string: $test", m.find())
	}
}
