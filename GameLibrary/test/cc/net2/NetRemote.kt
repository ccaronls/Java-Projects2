package cc.net2

import cc.lib.ksp.remote.IRemote
import cc.lib.ksp.remote.Remote
import cc.lib.ksp.remote.RemoteFunction
import cc.lib.math.Vector2D

@Remote
abstract class NetRemote : IRemote {

	@RemoteFunction
	open fun doSomethingA(v: Vector2D) {
	}

	@RemoteFunction
	open fun doSomethingB(x: Int) {
	}

	@RemoteFunction
	open fun doSomethingC(x: Int, y: Float) {
	}

	@RemoteFunction
	open fun doSomethingD(s: String) {
	}

	@RemoteFunction
	abstract fun doSomethingAndReturn(x: Int): Int?

	@RemoteFunction
	abstract fun doSomethingAndReturn2(x: Int): Int?

}