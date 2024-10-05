package cc.library.mirrortest

import cc.lib.ksp.mirror.IData
import com.google.gson.GsonBuilder
import kotlinx.serialization.Serializable
import java.io.StringReader
import java.io.StringWriter

open class MyMirror : MirrorImpl()


@Serializable
data class TempData(val x: Int, val y: Float) : IData<TempData> {
	override fun deepCopy() = copy()

	override fun getSerializer() = serializer()
}

class MirrorTest {

	init {
		val t = TempData(0, 0f)
		t.copy(1, 1f)
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {

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

			mirror.markClean()
			mirror.a = 1001
			writer.buffer.setLength(0)
			with(gson.newJsonWriter(writer)) {
				mirror.toGson(this, true)
			}

			println("buffer")
			println(writer.buffer.toString())

		}
	}
}