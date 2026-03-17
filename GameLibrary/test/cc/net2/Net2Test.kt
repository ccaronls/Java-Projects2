package cc.net2

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.ksp.netcmd.NetCommand
import cc.lib.logger.Logger
import cc.lib.logger.LoggerFactory
import cc.lib.net2.INetConnection
import cc.lib.net2.NetCommandRegistryGameLibTest
import cc.lib.net2.impl.ANetCommandFactory
import cc.lib.net2.impl.ClConnect
import cc.lib.net2.impl.ClConnectImpl
import cc.lib.net2.impl.NetClient
import cc.lib.net2.impl.NetConnection
import cc.lib.net2.impl.NetServer
import cc.lib.net2.impl.getSecretCode
import cc.lib.net2.impl.validateSecretCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket


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

	@Rule
	@JvmField
	val testName = TestName()

	@Before
	fun setup() {
		println("-------------------------------------------------------------")
		println(">>>> ${testName.methodName}")
		println("-------------------------------------------------------------")
		LoggerFactory.factory = object : LoggerFactory() {
			override fun getLogger(name: String): Logger {
				return DefaultLogger("${testName.methodName}+$name")
			}
		}
	}

	@After
	fun teardown() {
		println("-------------------------------------------------------------")
		println("<<<< ${testName.methodName}")
		println("-------------------------------------------------------------")
	}

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
				val server = object : NetServer(0, TestNetCommandFactory) {
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
				println("<<<<<<< SERVER LAUNCH DONE")
			}

			launch {
				val disconnect = CompletableDeferred<Int>()
				val client = object : NetClient("test", 0, TestNetCommandFactory) {
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
	}

	@Test
	fun connectTest2() {
		runBlocking {
			val done = CompletableDeferred<Int>()
			var propertyChanged = CompletableDeferred<Pair<String, Any>>()
			launch {
				val connected = CompletableDeferred<INetConnection>()
				val server = object : NetServer(0, TestNetCommandFactory) {
					override suspend fun onNewConnection(c: INetConnection) {
						super.onNewConnection(c)
						Assert.assertEquals("test", c.displayName)
						connected.complete(c)
					}
				}
				server.listen(PORT, 0)
				val connection = connected.await()
				connection.properties.put("a", 1)
				Assert.assertEquals(Pair("a", 1), propertyChanged.await())
				propertyChanged = CompletableDeferred()
				connection.properties.put("b", 2f)
				Assert.assertEquals(Pair("b", 2f), propertyChanged.await())
				propertyChanged = CompletableDeferred()
				connection.properties.put("c", true)
				Assert.assertEquals(Pair("c", true), propertyChanged.await())
				propertyChanged = CompletableDeferred()
				connection.properties.put("d", 1000L)
				Assert.assertEquals(Pair("d", 1000L), propertyChanged.await())
				propertyChanged = CompletableDeferred()
				connection.properties.put("e", "hello")
				Assert.assertEquals(Pair("e", "hello"), propertyChanged.await())
				propertyChanged = CompletableDeferred()
				connection.properties.put("f", 5.3)
				Assert.assertEquals(Pair("f", 5.3), propertyChanged.await())
				propertyChanged = CompletableDeferred()
				connection.properties.put("g", "12345".toByteArray())
				val p = propertyChanged.await()
				Assert.assertEquals(p.first, "g")
				Assert.assertTrue("12345".toByteArray().contentEquals(p.second as ByteArray))

				server.stop()
				done.complete(0)
				println("<<<<<<< SERVER LAUNCH DONE")
			}

			launch {
				val disconnect = CompletableDeferred<Int>()
				val client = object : NetClient("test", 0, TestNetCommandFactory) {
					override fun onDisconnected(reason: String) {
						super.onDisconnected(reason)
						disconnect.complete(0)
					}

					override fun onPropertyChanged(key: String, value: Any) {
						super.onPropertyChanged(key, value)
						propertyChanged.complete(key to value)
					}
				}
				client.connect("127.0.0.1", PORT)
				disconnect.await()
				done.await()
				println(">>>>>>> CLIENT LAUNCH DONE")
			}
		}
	}

	@Test
	fun connectTest3() {
		runBlocking {
			val done = CompletableDeferred<Int>()
			val disconnected = CompletableDeferred<Int>()
			var connected = CompletableDeferred<Int>()
			val clientDone = CompletableDeferred<Int>()
			launch {
				val server = object : NetServer(0, TestNetCommandFactory) {
					override suspend fun onNewConnection(c: INetConnection) {
						super.onNewConnection(c)
						Assert.assertEquals("test", c.displayName)
						connected.complete(0)
					}

					override fun createNetConnection(scope: CoroutineScope, id: Int, displayName: String, netServer: NetServer, socket: Socket, input: DataInputStream, output: DataOutputStream): NetConnection {
						return object : NetConnection(scope, id, displayName, netServer, socket, input, output) {
							override fun onDisconnected(reason: String) {
								super.onDisconnected(reason)
								disconnected.complete(0)
							}
						}
					}

					override suspend fun onReConnection(c: INetConnection) {
						super.onReConnection(c)
						connected.complete(1)
					}
				}
				server.listen(PORT, 0)
				clientDone.await()
				server.stop()
				done.complete(0)
				println("<<<<<<< SERVER LAUNCH DONE")
			}

			launch {
				val client = NetClient("test", 0, TestNetCommandFactory)
				client.connect("127.0.0.1", PORT)
				Assert.assertEquals(0, connected.await())
				connected = CompletableDeferred()
				client.disconnect()
				disconnected.await()
				client.connect("127.0.0.1", PORT)
				Assert.assertEquals(1, connected.await())
				clientDone.complete(0)
				done.await()
				println(">>>>>>> CLIENT LAUNCH DONE")
			}
		}
	}

	companion object {

		const val PORT = 9999

	}
}