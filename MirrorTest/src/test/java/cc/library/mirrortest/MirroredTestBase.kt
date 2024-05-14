package cc.library.mirrortest

import cc.lib.ksp.mirror.MirroredImpl
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.StringReader
import java.io.StringWriter

/**
 * Created by Chris Caron on 5/13/24.
 */
abstract class MirroredTestBase {

	val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
	val writer = StringWriter()

	fun newWriter(): JsonWriter {
		writer.buffer.setLength(0)
		return gson.newJsonWriter(writer)
	}

	fun newReader(): JsonReader {
		return gson.newJsonReader(StringReader(writer.toString()))
	}

	fun transfer(m0: MirroredImpl, m1: MirroredImpl, dirtyOnly: Boolean) {
		writer.buffer.setLength(0)
		MirroredImpl.writeMirrored(m0, gson.newJsonWriter(writer), dirtyOnly)
		println("transfered ----------")
		println(writer.buffer)
		//m0.toGson(gson.newJsonWriter(writer), dirtyOnly)
		//m1.fromGson(gson.newJsonReader(StringReader(writer.toString())))
		MirroredImpl.readMirrored(m1, gson.newJsonReader(StringReader(writer.toString())))
		m0.markClean()
		writer.buffer.setLength(0)
		println("transfered after ----------")
		MirroredImpl.writeMirrored(m0, gson.newJsonWriter(writer), true)
		println(writer.buffer)
	}

}