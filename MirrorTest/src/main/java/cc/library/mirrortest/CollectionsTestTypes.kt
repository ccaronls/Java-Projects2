package cc.library.mirrortest

import cc.lib.ksp.mirror.Mirror
import cc.lib.ksp.mirror.Mirrored
import cc.lib.ksp.mirror.MirroredArray

/**
 * Created by Chris Caron on 5/10/24.
 */
@Mirror
interface IMirroredListTest : Mirrored {
	val intList: List<Int>
	val strList: List<String>
	val colorList: List<IColor>
	val dataList: List<MyData>
}

@Mirror
interface IMirroredArrayTest : Mirrored {
	val intArray: MirroredArray<Int>
	val strArray: MirroredArray<String>
	val colorArray: MirroredArray<Color>
	val dataArray: MirroredArray<MyData>
}

@Mirror
interface IMirroredMapTest : Mirrored {
	val intStrMap: MutableMap<Int, String>
	val intDataMap: MutableMap<Int, MyData>
	val dataStrMap: MutableMap<MyData, String>
	val colorDataMap: MutableMap<IColor, MyData>
	val dataColorMap: MutableMap<MyData, IColor>
}
