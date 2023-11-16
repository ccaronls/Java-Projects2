package cc.library.mirrortest

import cc.lib.mirror.annotation.Mirror
import cc.lib.mirror.context.Mirrored

@Mirror("cc.library.mirrortest")
interface IMirror2 : Mirrored {
	var x: MirrorImpl?
}
