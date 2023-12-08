package cc.lib.kreflector

import java.io.IOException

/**
 * Created by Chris Caron on 11/30/23.
 */
interface IDirty {
	fun isDirty(): Boolean
	fun markClean()

	@Throws(IOException::class)
	fun serializeDirty(out: RPrintWriter)
}