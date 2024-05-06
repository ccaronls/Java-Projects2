package cc.lib.zombicide.p2p

import cc.lib.net.AClientConnection
import cc.lib.zombicide.ZUserRemote
import cc.lib.zombicide.ui.UIZombicide

/**
 * Created by Chris Caron on 7/17/21.
 */
class ZUserMP(val connection: AClientConnection) :
	ZUserRemote(connection.displayName, connection.getAttribute("color") as Int),
	AClientConnection.Listener {

	init {
		connection.addListener(this)
		UIZombicide.instance.setUserColorId(this, colorId)
	}

	override fun onPropertyChanged(c: AClientConnection) {
		UIZombicide.instance.setUserColorId(this, c.getAttribute("color") as Int)
		UIZombicide.instance.setUserName(this, c.displayName)
	}

	override fun executeRemotely(method: String, resultType: Class<*>?, vararg args: Any?): Any? {
		return connection.executeMethodOnRemote(USER_ID, resultType != null, method, *args)
	}

	companion object {
		const val USER_ID = "ZUser"
	}
}