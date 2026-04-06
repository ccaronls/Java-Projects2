package cc.lib.game

import cc.lib.math.Matrix3x3
import junit.framework.TestCase

class MatrixTest : TestCase() {
	fun test() {
		val A = Matrix3x3()
		val B = Matrix3x3()
		val C = Matrix3x3()
		A.setTranslate(5, 10)
		println("A=$A")
		B.setRotate(45)
		println("B=$B")
		Matrix3x3.multiply(A, B, C)
		println("A X B =$C")
		Matrix3x3.multiply(B, A, C)
		println("B X A =$C")
	}
}
