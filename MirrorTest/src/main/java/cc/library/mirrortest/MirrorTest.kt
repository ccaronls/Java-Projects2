package cc.library.mirrortest

import cc.lib.mirror.annotation.Mirror
import cc.lib.mirror.context.MirrorContext
import cc.lib.mirror.context.Mirrored
import com.google.gson.GsonBuilder
import java.io.StringReader
import java.io.StringWriter

enum class TempEnum {
	ONE,
	TWO,
	THREE
}

@Mirror("cc.library.mirrortest")
interface IMirror : Mirrored {
	var ee: TempEnum?
	var ee2: TempEnum?
	var a: Int?
	var b: String?
	var c: Float?
	var d: Long?
	var e: Boolean?
	var f: Byte?
	var g: Int
	var h: String
	var i: Float
	var j: Long
	var k: Boolean
	var l: Byte
	val m: Short
	var n: Char
	//var arr: Array<Int>?
//	var EE: TempEnum

}

@Mirror("cc.library.mirrortest")
interface IMirror2 : Mirrored {
	var x: MirrorImpl?
}

class MyMirror : MirrorImpl()

class MirrorTest {
	companion object {
		@JvmStatic
		fun main(args: Array<String>) {

			val context = object : MirrorContext {

			}

			val mirror = MyMirror()
			val writer = StringWriter()
			mirror.ee2 = TempEnum.TWO
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
				val newMirror = MyMirror()
				newMirror.fromGson(this)
				println("newMirror = $newMirror")
			}

			val mirror3 = object : Mirror2Impl() {}
			mirror3.x = MyMirror()
			println("mirror3 = $mirror3")

			writer.buffer.setLength(0)
			with(gson.newJsonWriter(writer)) {
				mirror3.toGson(this)
			}

			println("buffer")
			println(writer.buffer.toString())


			val mirror2 = object : Mirror2Impl() {}
			println("mirror2 = $mirror2")

			writer.buffer.setLength(0)
			with(gson.newJsonWriter(writer)) {
				mirror2.toGson(this)
			}

			println("buffer")
			println(writer.buffer.toString())

			with(gson.newJsonReader(StringReader(writer.buffer.toString()))) {
				val newMirror = object : Mirror2Impl() {}
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
				val newMirror = object : Mirror2Impl() {}
				newMirror.fromGson(this)
				println("newMirror2 = $newMirror")
			}

		}
	}
}