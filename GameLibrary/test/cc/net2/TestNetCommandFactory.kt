package cc.net2

import cc.lib.net2.NetCommandRegistryGameLibTest
import cc.lib.net2.impl.ANetCommandFactory

object TestNetCommandFactory : ANetCommandFactory() {
	init {
		NetCommandRegistryGameLibTest(this)
	}
}
