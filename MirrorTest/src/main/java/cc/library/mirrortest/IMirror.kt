package cc.library.mirrortest

import cc.lib.mirror.annotation.Mirror
import cc.lib.mirror.annotation.MirroredFunction
import cc.lib.mirror.context.Mirrored
import cc.lib.mirror.context.MirroredArray

@Mirror
interface IMirror : Mirrored {
	var ee: TempEnum?
	var ee2: TempEnum?
	var a: Int?
	var b: String?
	var c: Float?
	var d: Long?
	var e: Boolean?
	var f: Byte?
	var g: Int
	var h: String
	var i: Float
	var j: Long
	var k: Boolean
	var l: Byte
	val m: Short
	var n: Char
	var o: IMirror?
	var enumList1: List<TempEnum>
	var charList1: List<Char>
	var intList1: List<Int>
	var stringList1: List<String>
	var floatList1: List<Float>
	var doubleList1: List<Double>
	var longList1: List<Long>
	var boolList1: List<Boolean>
	var byteList1: List<Byte>
	var shortList1: List<Short>
	var mirrorList1: List<Mirrored>
	var colorList1: List<IColor>
	var enumList: MutableList<TempEnum>
	var charList: MutableList<Char>
	var intList: MutableList<Int>
	var stringList: MutableList<String>
	var floatList: MutableList<Float>
	var doubleList: MutableList<Double>
	var longList: MutableList<Long>
	var boolList: MutableList<Boolean>
	var byteList: MutableList<Byte>
	var shortList: MutableList<Short>
	var mirrorList: MutableList<Mirrored>
	var colorList: MutableList<IColor>

	//	var enumArray: Array<TempEnum>
//	var charArray: Array<Char>
	var intArray: MirroredArray<Int>
	/*
	var stringArray: Array<String>
	var floatArray: Array<Float>
	var doubleArray: Array<Double>
	var longArray: Array<Long>
	var boolArray: Array<Boolean>
	var byteArray: Array<Byte>
	var shortArray: Array<Short>

	 */
//	var mirrorArray: Array<Mirrored>
//	var colorArray: Array<IColor>

	//val listList: MutableList<List<Int>>
	//var arr: Array<Int>?

//	var EE: TempEnum

	@MirroredFunction
	fun doSomething1()

	@MirroredFunction
	fun doSomething2(v: String)

	@MirroredFunction
	fun doSomething3(m: IMirror2)

	@MirroredFunction
	fun doSomething4(x: Int, y: Float, z: Mirrored?)

}
