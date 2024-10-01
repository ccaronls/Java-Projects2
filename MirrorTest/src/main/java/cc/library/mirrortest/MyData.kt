package cc.library.mirrortest

import cc.lib.ksp.mirror.IData
import cc.lib.ksp.mirror.Mirror
import cc.lib.ksp.mirror.Mirrored
import cc.lib.ksp.mirror.MirroredArray
import kotlinx.serialization.Serializable

/**
 * Created by Chris Caron on 5/9/24.
 */
@Serializable
data class MyData(val x: Int, val y: Float, val z: String, val l: List<Int>) : IData<MyData> {
	override fun deepCopy(): MyData = copy()
}

@Mirror
interface IMixed : Mirrored {
	val s: String
	val x: List<MyData>
	val d: MyData?
	val m: Map<String, MyData>
	val a: MirroredArray<MyData>
	val d2: MyData?
}