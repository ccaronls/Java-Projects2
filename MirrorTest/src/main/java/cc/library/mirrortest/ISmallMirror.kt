package cc.library.mirrortest

import cc.lib.mirror.annotation.Mirror
import cc.lib.mirror.context.Mirrored

@Mirror("cc.library.mirrortest")
interface ISmallMirror : Mirrored {
	//	var ee: TempEnum?
	var a: Int?
	var b: String?
	var c: Float?
	/*
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
	//var arr: Array<Int>?
//	var EE: TempEnum
*/
}
