package cc.net2

import cc.lib.net2.NetCommandRegistryGameLibTest
import cc.lib.net2.impl.ANetCommandFactory

object TestNetCommandFactoryCL : ANetCommandFactory("NetClient") {
	init {
		NetCommandRegistryGameLibTest(this)
	}
}

object TestNetCommandFactorySVR : ANetCommandFactory("NetServer") {
	init {
		NetCommandRegistryGameLibTest(this)
	}
}
