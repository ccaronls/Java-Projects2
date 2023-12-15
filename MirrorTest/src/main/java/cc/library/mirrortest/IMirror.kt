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
	var charArray: MirroredArray<Char>
	var intArray: MirroredArray<Int>
	var stringArray: MirroredArray<String>
	var floatArray: MirroredArray<Float>
	var doubleArray: MirroredArray<Double>
	var longArray: MirroredArray<Long>
	var boolArray: MirroredArray<Boolean>
	var byteArray: MirroredArray<Byte>
	var shortArray: MirroredArray<Short>

	var mirrorArray: MirroredArray<Mirrored>
	var colorArray: MirroredArray<IColor>
	var enumArray: MirroredArray<TempEnum>

	val listList: MutableList<List<Int>>

	val map1: Map<String, String>
	val map2: Map<Int, Mirrored?>
	val map3: Map<Color, Short>
	val map4: Map<TempEnum, Mirrored>
	val map5: Map<String, TempEnum>
	val map6: Map<Char, Boolean>
	val map7: Map<String, List<String>>
	val map8: Map<String, MirroredArray<Int>>
//	val map9: Map<String, String>


//	var EE: TempEnum

	@MirroredFunction
	suspend fun doSomething1()

	@MirroredFunction
	suspend fun doSomething2(v: String)

	@MirroredFunction
	suspend fun doSomething3(m: IMirror2?)

	@MirroredFunction
	suspend fun doSomething4(x: Int, y: Float, z: Mirrored?)

	@MirroredFunction
	suspend fun doSomethingAndReturn(m: IMirror2?): Int?

	@MirroredFunction
	suspend fun doSomethingAndReturnEnum(e: TempEnum?): TempEnum?

	@MirroredFunction
	suspend fun doSomethingAndReturnList(l: List<ISmallMirror>, idx: Int): ISmallMirror?

}
