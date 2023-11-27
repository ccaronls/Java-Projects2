package cc.library.mirrortest

import cc.lib.mirror.annotation.DirtyType
import cc.lib.mirror.annotation.Mirror
import cc.lib.mirror.context.Mirrored

@Mirror(DirtyType.ANY)
interface IMirror2 : Mirrored {
	var x: MirrorImpl?
	var y: String
}
