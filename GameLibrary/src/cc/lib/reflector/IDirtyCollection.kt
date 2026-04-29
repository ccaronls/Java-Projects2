package cc.lib.reflector

import java.io.StringReader
import java.io.StringWriter

/**
 * Created by Chris Caron on 12/7/23.
 */
interface IDirtyCollection<T : IDirtyCollection<T>> : IDirty {

	fun merge(input: RBufferedReader)

	fun deserialize(input: RBufferedReader)

	fun serializeDirtyToString(ignoreNonDirtyTypes: Boolean): String {
		val buf = StringWriter()
		RPrintWriter(buf).use {
			serializeDirty(it, ignoreNonDirtyTypes)
		}
		return buf.toString();
	}

	fun merge(diff: String) {
		merge(RBufferedReader(StringReader(diff)))
	}

	fun deepCopy(): T
}

internal fun String.parse(startsWith: String, parser: (String) -> Any): Any {
	if (!startsWith(startsWith))
		throw Exception("Expected $startsWith but got $this")
	return parser(substring(startsWith.length))
}
