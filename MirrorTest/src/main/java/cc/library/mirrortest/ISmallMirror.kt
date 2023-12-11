package cc.library.mirrortest

import cc.lib.mirror.annotation.Mirror
import cc.lib.mirror.context.Mirrored

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
