package cc.library.mirrortest

import cc.lib.mirror.annotation.Mirror
import cc.lib.mirror.context.Mirrored

@Mirror("cc.library.mirrortest")
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
	//var arr: Array<Int>?
//	var EE: TempEnum

}
