package cc.lib.net

import cc.lib.ksp.remote.IRemote
import cc.lib.ksp.remote.Remote
import cc.lib.ksp.remote.RemoteFunction
import cc.lib.math.Vector2D

@Remote("test")
abstract class NetRemote : IRemote {

	@RemoteFunction
	open suspend fun doSomethingA(v: Vector2D) {
	}

	@RemoteFunction
	open suspend fun doSomethingB(x: Int) {
	}

	@RemoteFunction
	open suspend fun doSomethingC(x: Int, y: Float) {
	}

	@RemoteFunction
	open suspend fun doSomethingD(s: String) {
	}

	@RemoteFunction
	abstract suspend fun doSomethingAndReturn(x: Int): Int?

	@RemoteFunction
	abstract suspend fun doSomethingAndReturn2(x: Int): Int?

}