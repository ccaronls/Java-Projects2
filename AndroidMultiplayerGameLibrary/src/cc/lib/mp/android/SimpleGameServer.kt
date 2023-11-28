package cc.lib.mp.android

import cc.lib.net.AGameServer
import com.github.simplenet.Server
import java.io.IOException

/**
 * Created by Chris Caron on 10/20/23.
 */
internal class SimpleGameServer(serverName: String, listenPort: Int, serverVersion: String, maxConnections: Int) : AGameServer(serverName, listenPort, serverVersion, maxConnections) {
	var server = Server()

	init {
		server.onConnect {

		}
	}


	@Throws(IOException::class)
	override fun listen() {
	}

	override fun isRunning(): Boolean {
		return false
	}

	override fun stop() {}
}