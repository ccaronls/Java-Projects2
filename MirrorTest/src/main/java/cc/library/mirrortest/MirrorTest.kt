package cc.library.mirrortest

import cc.lib.mirror.annotation.Mirror
import cc.lib.mirror.context.MirrorContext
import com.google.gson.GsonBuilder
import java.io.StringReader
import java.io.StringWriter

@Mirror("cc.library.mirrortest")
interface IMirror {
	var a: Int?
	var b: String?
	var c: Float?
	var d: Long?
	var e: Boolean?
}

@Mirror("cc.library.mirrortest")
interface IMirror2 {
	var x: MirrorImpl?
}

class MyMirror(context: MirrorContext) : MirrorImpl(context)

class MirrorTest {
	companion object {
		@JvmStatic
		fun main(args: Array<String>) {

			val context = object : MirrorContext {

			}

			val mirror = MyMirror(context)
			val writer = StringWriter()
			mirror.a = 100
			mirror.b = "hello"
			mirror.c = 2.5f
			mirror.d = Long.MAX_VALUE
			mirror.e = true

			println("mirror = \n$mirror")

			val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()

			with(gson.newJsonWriter(writer)) {
				mirror.toGson(this)
			}

			println("buffer")
			println(writer.buffer.toString())

			with(gson.newJsonReader(StringReader(writer.buffer.toString()))) {
				val newMirror = MyMirror(context)
				newMirror.fromGson(this)
				println("newMirror = $newMirror")
			}

			val mirror2 = Mirror2Impl()
			println("mirror2 = $mirror2")

			writer.buffer.setLength(0)
			with(gson.newJsonWriter(writer)) {
				mirror2.toGson(this)
			}

			println("buffer")
			println(writer.buffer.toString())

			with(gson.newJsonReader(StringReader(writer.buffer.toString()))) {
				val newMirror = Mirror2Impl(context)
				newMirror.fromGson(this)
				println("newMirror2 = $newMirror")
			}

			mirror2.x = mirror
			writer.buffer.setLength(0)
			with(gson.newJsonWriter(writer)) {
				mirror2.toGson(this)
			}

			println("buffer")
			println(writer.buffer.toString())

			with(gson.newJsonReader(StringReader(writer.buffer.toString()))) {
				val newMirror = Mirror2Impl(context)
				newMirror.fromGson(this)
				println("newMirror2 = $newMirror")
			}

		}
	}
}