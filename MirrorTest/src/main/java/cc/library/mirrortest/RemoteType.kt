package cc.library.mirrortest

import cc.lib.ksp.remote.IRemote2
import cc.lib.ksp.remote.Remote
import cc.lib.ksp.remote.RemoteFunction

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
}