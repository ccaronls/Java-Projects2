package cc.library.mirrortest

import cc.lib.mirror.annotation.DirtyType
import cc.lib.mirror.annotation.Mirror
import cc.lib.mirror.context.Mirrored

/**
 * Created by Chris Caron on 11/16/23.
 */
@Mirror("cc.library.mirrortest", DirtyType.NEVER)
interface IColor : Mirrored {
	var argb: Int
}