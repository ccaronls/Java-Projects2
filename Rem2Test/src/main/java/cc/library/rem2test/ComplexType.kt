package cc.library.rem2test

import cc.lib.ksp.mirror.Mirror
import cc.lib.ksp.mirror.Mirrored
import cc.lib.ksp.remote.IRemote2
import cc.lib.ksp.remote.Remote
import cc.lib.ksp.remote.RemoteFunction
import cc.library.mirrortest.Color

/**
 * Created by Chris Caron on 10/2/24.
 *
 * Complex has generated code from both mirror and rem2
 */

@Mirror
interface IComplexType : Mirrored {
	var color: Color?
	var name: String
	var index: Int
}

@Remote
abstract class ComplexType : ComplexTypeImpl(), IRemote2 {
	@RemoteFunction
	open suspend fun rem1() {
	}
}
