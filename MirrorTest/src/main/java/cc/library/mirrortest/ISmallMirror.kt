package cc.library.mirrortest

import cc.lib.ksp.mirror.Mirror
import cc.lib.ksp.mirror.Mirrored

@Mirror
interface ISmallMirror : Mirrored {
	//	var ee: TempEnum?
	var a: Int?
	var b: String?
	var c: Float?
}

@Mirror
interface ILargeMirror : ISmallMirror {
	var aa: Long?
	var bb: Double?
	var cc: IColor?
}
