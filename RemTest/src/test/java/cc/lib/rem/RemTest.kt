package cc.lib.rem

import cc.lib.ksp.remote.IRemote
import cc.lib.ksp.remote.Remote
import cc.lib.ksp.remote.RemoteFunction
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking

/**
 * Created by Chris Caron on 5/4/24.
 */

class RemTest : TestCase() {

	override fun setUp() {
		super.setUp()
		println("--------------------------------------------")
	}

	fun test1() = runBlocking {
		val remote = object : TRemoteImpl("remote", null) {
			override suspend fun remote3(name: String): String? {
				return "$name:goodbye"
			}

			override suspend fun remote5(a: Int, b: String?, c: Long?, d: Float) {
				println("$name: a:$a, b:$b, c:$c, d:$d")
			}
		}
		val local = TRemoteImpl("local", remote)

		local.remote1()
		local.remote2(10)

		val result = local.remote3("hello")
		println("result: $result")

		local.remote4(listOf(1, 2, 3))
		local.remote4(null)

		local.remote5(100, "so what", null, 10f)
	}


}

@Remote
abstract class TRemote(val x: Int, val y: String?) : IRemote {

	@RemoteFunction
	open suspend fun remote1() {
	}

	@RemoteFunction
	open suspend fun remote2(num: Int) {
	}

	@RemoteFunction
	open suspend fun remote3(name: String): String? = "return:$name"

	@RemoteFunction
	open suspend fun remote4(list: List<Int>?): Int? = list?.firstOrNull()

	@RemoteFunction
	open suspend fun remote5(a: Int, b: String?, c: Long?, d: Float) {
	}
}

open class TRemoteImpl(val name: String, val other: TRemote?) : TRemoteRemote(0, null) {
	override suspend fun executeRemotely(method: String, resultType: Class<*>?, vararg args: Any?): Any? {
		return other?.executeLocally(method, *args)
	}

	override suspend fun remote1() {
		println("$name:remote1")
		super.remote1()
	}

	override suspend fun remote2(num: Int) {
		println("$name:remote2: $num")
		super.remote2(num)
	}

	override suspend fun remote3(name: String): String? {
		println("$name:remote3: $name")
		return super.remote3(name)
	}

	override suspend fun remote4(list: List<Int>?): Int? {
		println("$name:remote4: ${list?.joinToString()}")
		return super.remote4(list)
	}
}

class X<T>()

@Remote
abstract class TRemote2(x: Int, y: String?, xy: X<Int?>?, xxyy: X<Int>?, xyxy: X<Int?>, yyxx: X<*>, yxyx: X<Int>) : IRemote {
	@RemoteFunction
	abstract suspend fun fooBar(x: X<*>)
}
