package cc.library.mirrortest

import cc.lib.ksp.mirror.DirtyType
import cc.lib.ksp.mirror.Mirror
import cc.lib.ksp.mirror.Mirrored
import cc.lib.ksp.mirror.MirroredImpl

@Mirror(DirtyType.ANY)
interface IMirror2 : Mirrored {
	var x: MirroredImpl?
	var y: String
}
