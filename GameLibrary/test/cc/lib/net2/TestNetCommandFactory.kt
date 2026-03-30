package cc.lib.net2

import cc.lib.net.impl.ANetCommandFactory

object TestNetCommandFactory : ANetCommandFactory() {
	init {
		NetCommandRegistryGameLibTest(this)
	}
}
