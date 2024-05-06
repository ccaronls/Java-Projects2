package cc.lib.mp.android

import cc.lib.game.Utils
import cc.lib.net.AGameClient
import cc.lib.net.GameCommand
import cc.lib.utils.Lock
import com.github.simplenet.Client
import java.net.InetAddress

/**
 * Created by Chris Caron on 10/20/23.
 */
internal class SimpleGameClient(version: String, name: String, val port: Int) : AGameClient(version, name) {
	private val client = Client()
	private var connected = false
	private val connectLock = Lock()
	private lateinit var address: String

	init {
		client.onConnect {
			connected = true
//			client.readBytesAlways()
			connectLock.release()
			listeners.forEach {
				it.onConnected()
			}
		}

	}

	override fun connectBlocking(address: InetAddress, port: Int) {
		client.connect(address.toString(), port)
		this.address = address.toString()
		connectLock.acquireAndBlock()
	}

	override fun reconnectAsync() {
		client.connect(address, port)
	}

	override val isConnected: Boolean
		get() = connected

	override fun disconnectAsync(reason: String, onDone: Utils.Callback<Int>?) {
		client.close()
		connected = false
	}

	override fun disconnect(reason: String) {
		client.close()
		connected = false
	}

	override fun reset() {}
	override fun sendCommand(cmd: GameCommand) {
		TODO("Not yet implemented")
	}

	override fun close() {
		TODO("Not yet implemented")
	}
}