package cc.net2

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.ksp.netcmd.NetCommand
import cc.lib.net2.INetConnection
import cc.lib.net2.NetCommandRegistryGameLibTest
import cc.lib.net2.impl.ANetCommandFactory
import cc.lib.net2.impl.ClConnect
import cc.lib.net2.impl.ClConnectImpl
import cc.lib.net2.impl.NetClient
import cc.lib.net2.impl.NetServer
import cc.lib.net2.impl.getSecretCode
import cc.lib.net2.impl.validateSecretCode
import cc.lib.utils.runNewProcess
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@NetCommand
interface TestCmd : INetCommand {
	val a: Int
	val b: Float
	val c: String
	val d: Double
	val e: Long
	val f: ByteArray
	val g: Byte
	val h: Short
	val i: UShort
	val j: ULong
	val k: UInt
	val l: Boolean
	val m: UByte
}

object TestNetCommandFactory : ANetCommandFactory() {
	init {
		NetCommandRegistryGameLibTest(this)
	}
}

/**
 * Created by Chris Caron on 3/1/26.
 */
class Net2Test {

	var process: Process? = null
	val clientScope = CoroutineScope(CoroutineName("CL") + Dispatchers.IO)
	val svrScope = CoroutineScope(CoroutineName("SVR") + Dispatchers.IO)

	@Test
	fun testValidation() {
		val t = getSecretCode()
		Assert.assertTrue(validateSecretCode(t))
	}

	@Test
	fun testCommands() {
		val output = ByteArrayOutputStream(1024)
		val cmd1 = ClConnectImpl("xyz", 100, 1234)
		cmd1.write(output)
		val buffer = output.toByteArray()
		val input = ByteArrayInputStream(buffer)
		val cmd = TestNetCommandFactory.read<ClConnect>(input)
		println(cmd)
		Assert.assertEquals(cmd1, cmd)
	}

	@Test
	fun connectTest() {
		runBlocking {
			val done = CompletableDeferred<Int>()
			launch {
				val connected = CompletableDeferred<Int>()
				val server = object : NetServer(0, TestNetCommandFactory, svrScope) {
					override suspend fun onNewConnection(c: INetConnection) {
						super.onNewConnection(c)
						Assert.assertEquals("test", c.displayName)
						connected.complete(0)
					}
				}
				server.listen(PORT, 0)
				connected.await()
				server.stop()
				done.complete(0)
				println("<<<<<<< SERVEr LAUNCH DONE")
			}

			launch {
				val disconnect = CompletableDeferred<Int>()
				val client = object : NetClient("test", 0, TestNetCommandFactory, clientScope) {
					override fun onDisconnected(reason: String) {
						super.onDisconnected(reason)
						disconnect.complete(0)
					}
				}
				client.connect("127.0.0.1", PORT)
				disconnect.await()
				done.await()
				println(">>>>>>> CLIENT LAUNCH DONE")
			}
		}
		println("------------ TEST EXIT -------")
	}

	fun startClient() {
		process = javaClass.runNewProcess("client")
		println("process $process started")
	}

	fun startServer() {
		process = javaClass.runNewProcess("server")
		println("process $process started")
	}


	fun killProcess() {
		process?.destroy()
	}

	companion object {

		const val PORT = 9999

		@JvmStatic
		fun main(args: Array<String>) {
			if (args[0] == "server") {
				println("starting server in new process")
				val server = NetServer(0, TestNetCommandFactory)
				server.listen(PORT, 0)
			} else {
				println("starting client in new process")
				val client = object : NetClient("test", 0, TestNetCommandFactory) {
					override fun onCommand(cmd: INetCommand) {
						super.onCommand(cmd)
					}
				}
				client.connect("localhost", PORT)
			}
		}
	}
}