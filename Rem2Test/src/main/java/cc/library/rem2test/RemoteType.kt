package cc.library.rem2test

import cc.lib.ksp.mirror.IData
import cc.lib.ksp.remote.IRemote2
import cc.lib.ksp.remote.Remote
import cc.lib.ksp.remote.RemoteFunction
import cc.library.mirrortest.Color
import cc.library.mirrortest.TempEnum
import kotlinx.serialization.Serializable

@Serializable
data class RemoteData(val s: String, val i: Int, val e: TempEnum) : IData<RemoteData> {
	override fun deepCopy() = copy()

	override fun getSerializer() = serializer()
}

/**
 * Created by Chris Caron on 10/1/24.
 */
@Remote
abstract class RemoteType : IRemote2 {

	@RemoteFunction
	open suspend fun fun1() {
	}

	@RemoteFunction
	open suspend fun fun2(i: Int) {
	}

	@RemoteFunction
	open suspend fun fun3(): Int? {
		TODO()
	}

	@RemoteFunction
	open suspend fun fun4(s: String?): String? {
		TODO()
	}

	@RemoteFunction
	open suspend fun fun5(m: Color?): String? {
		TODO()
	}

	@RemoteFunction
	open suspend fun fun6(r: Byte, g: Byte, b: Byte, nm: String): Color? {
		TODO()
	}

	@RemoteFunction
	open suspend fun fun7(colorEnum: TempEnum): Color? {
		TODO()
	}

	@RemoteFunction
	open suspend fun fun8(list: List<Int>): String? {
		TODO()
	}

	@RemoteFunction
	open suspend fun fun9(list1: List<Int>?, list2: List<TempEnum>): String? {
		TODO()
	}

	@RemoteFunction
	open suspend fun fun10(map: Map<String, Int>): Int? {
		TODO()
	}

	@RemoteFunction
	open suspend fun fun11(array: IntArray): Int? {
		TODO()
	}

	@RemoteFunction
	open suspend fun fun12(array: Array<String>): String? {
		TODO()
	}

	@RemoteFunction
	open suspend fun fun13(
		d: RemoteData,
		array: Array<RemoteData>,
		list: List<RemoteData>,
		map: Map<String, RemoteData?>
	): RemoteData? {
		TODO()
	}

}