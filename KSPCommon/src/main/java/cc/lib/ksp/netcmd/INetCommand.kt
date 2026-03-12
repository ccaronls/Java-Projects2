package cc.lib.ksp.netcmd

import java.io.IOException
import java.io.OutputStream

/**
 * Created by Chris Caron on 3/2/26.
 */

interface INetCommand {

	val serializedName: String

	@Throws(IOException::class)
	fun write(stream: OutputStream)
}