package cc.lib.ksp.netcmd

import java.io.IOException
import java.io.Reader
import java.io.Writer

/**
 * Created by Chris Caron on 3/21/26.
 */
interface ISerializable {

	@Throws(IOException::class)
	fun serialize(out: Writer)

	@Throws(IOException::class)
	fun deserialize(input: Reader)
}