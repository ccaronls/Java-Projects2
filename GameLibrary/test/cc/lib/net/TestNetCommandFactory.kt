package cc.lib.net

import cc.lib.net.impl.ANetCommandFactory

object TestNetCommandFactory : ANetCommandFactory() {
	init {
		NetCommandRegistryGameLibTest(this)
	}
}
