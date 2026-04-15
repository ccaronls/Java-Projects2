package cc.game.zombicide.p2p

import cc.game.zombicide.ZUserRemote
import cc.game.zombicide.ui.UIZombicide
import cc.lib.ksp.remote.ISvrExecuteRemote

/**
 * Created by Chris Caron on 7/17/21.
 */
class ZUserMP(val connection: IZConnection) :
	ZUserRemote(connection.displayName, connection.color),
	IZConnection.Listener {

	init {
		connection.addListener(this)
		UIZombicide.instance.setUserColorId(this, colorId)
	}

	override fun onColorChanged(color: Int) {
		UIZombicide.instance.setUserColorId(this, color)
	}

	override fun onDisplayNameChanged(name: String) {
		UIZombicide.instance.setUserName(this, name)
	}

	override suspend fun executeRemotelyBlocking(cmd: ISvrExecuteRemote): Any? {
		return connection.executeMethodOnRemote(cmd)
	}
}