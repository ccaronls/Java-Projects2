package cc.library.mirrortest

import cc.lib.ksp.mirror.Mirror
import cc.lib.ksp.mirror.Mirrored

@Mirror
interface ISmallMirror : Mirrored {
	//	var ee: TempEnum?
	var chn: Char?
	val sn: Short?
	val byn: Byte?
	var an: Int?
	var bn: String?
	var cn: Float?

	var ch: Char
	val s: Short
	val by: Byte
	var a: Int
	var b: String
	var c: Float

}

@Mirror
interface ILargeMirror : ISmallMirror {
	var aan: Long?
	var bbn: Double?
	var ccn: IColor?

	var aa: Long
	var bb: Double

}
