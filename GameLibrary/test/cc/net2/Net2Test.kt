package cc.net2

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.ksp.netcmd.NetCommand
import cc.lib.ksp.remote.IRemote
import cc.lib.ksp.remote.Remote
import cc.lib.ksp.remote.RemoteFunction
import cc.lib.logger.Logger
import cc.lib.logger.LoggerFactory
import cc.lib.net2.INetConnection
import cc.lib.net2.NetCommandRegistryGameLibTest
import cc.lib.net2.impl.ANetCommandFactory
import cc.lib.net2.impl.ClConnect
import cc.lib.net2.impl.ClConnectImpl
import cc.lib.net2.impl.NetClient
import cc.lib.net2.impl.NetConnection
import cc.lib.net2.impl.NetException
import cc.lib.net2.impl.NetServer
import cc.lib.net2.impl.SvrExecuteImpl
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

@Remote
abstract class NetRemote : IRemote {

	@RemoteFunction
	open fun doSomethingA() {
	}

	@RemoteFunction
	open fun doSomethingB(x: Int) {
	}

	@RemoteFunction
	open fun doSomethingC(x: Int, y: Float) {
	}

	@RemoteFunction
	open fun doSomethingD(s: String) {
	}

	@RemoteFunction
	abstract fun doSomethingAndReturn(x: Int): Int?
}


class NetRemoteImpl(val connection: NetConnection) : NetRemoteRemote() {

	override fun executeRemotely(method: String, resultType: Class<*>?, vararg args: Any?): Any? {
		return runBlocking {
			if (resultType != null) {
				connection.deferredResponse = CompletableDeferred()
			}
			connection.sendTCP(SvrExecuteImpl(method, resultType?.canonicalName, args))
			connection.deferredResponse?.await()
		}
	}
}

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
	fun `test command factory serialization`() {
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
	fun `test server stops clean client disconnect`() {
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
	fun `test server changing properties`() {
		runBlocking {
			val done = CompletableDeferred<Int>()
			var propertyChanged = CompletableDeferred<Pair<String, Any?>>()
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

					override fun onPropertyChanged(key: String, value: Any?) {
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
	fun `test client changing properties`() {
		runBlocking {
			val done = CompletableDeferred<Int>()
			var propertyChanged = CompletableDeferred<Pair<String, Any?>>()
			launch {
				val disconnect = CompletableDeferred<Int>()
				val server = object : NetServer(0, TestNetCommandFactory) {
					override fun createNetConnection(scope: CoroutineScope, id: Int, displayName: String, netServer: NetServer, socket: Socket, input: DataInputStream, output: DataOutputStream): NetConnection {
						return object : NetConnection(scope, id, displayName, netServer, socket, input, output) {
							override fun onPropertyChanged(key: String, value: Any?) {
								super.onPropertyChanged(key, value)
								propertyChanged.complete(key to value)
							}

							override fun onDisconnected(reason: String) {
								super.onDisconnected(reason)
								Assert.assertEquals("Client left", reason)
								disconnect.complete(0)
							}
						}
					}
				}
				server.listen(PORT, 0)
				disconnect.await()
				server.stop()
				done.complete(0)
				println("<<<<<<< SERVER LAUNCH DONE")
			}

			launch {
				val client = NetClient("test", 0, TestNetCommandFactory)
				client.connect("127.0.0.1", PORT)
				client.properties.put("a", 1)
				Assert.assertEquals(Pair("a", 1), propertyChanged.await())
				propertyChanged = CompletableDeferred()
				client.properties.put("b", 2f)
				Assert.assertEquals(Pair("b", 2f), propertyChanged.await())
				propertyChanged = CompletableDeferred()
				client.properties.put("c", true)
				Assert.assertEquals(Pair("c", true), propertyChanged.await())
				propertyChanged = CompletableDeferred()
				client.properties.put("d", 1000L)
				Assert.assertEquals(Pair("d", 1000L), propertyChanged.await())
				propertyChanged = CompletableDeferred()
				client.properties.put("e", "hello")
				Assert.assertEquals(Pair("e", "hello"), propertyChanged.await())
				propertyChanged = CompletableDeferred()
				client.properties.put("f", 5.3)
				Assert.assertEquals(Pair("f", 5.3), propertyChanged.await())
				propertyChanged = CompletableDeferred()
				client.properties.put("g", "12345".toByteArray())
				val p = propertyChanged.await()
				Assert.assertEquals(p.first, "g")
				Assert.assertTrue("12345".toByteArray().contentEquals(p.second as ByteArray))
				client.disconnect()
				done.await()
				println(">>>>>>> CLIENT LAUNCH DONE")
			}
		}
	}

	@Test
	fun `test client disconnect and reconnect`() {
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

	@Test
	fun `test client reject version mismatch`() {
		runBlocking {
			val clientDone = CompletableDeferred<Int>()
			val serverDone = CompletableDeferred<Int>()
			launch {
				val server = NetServer(0, TestNetCommandFactory)
				server.listen(PORT, 0)
				clientDone.await()
				server.stop()
				serverDone.complete(0)
				println("<<<<<<< SERVER LAUNCH DONE")
			}

			launch {
				val client = NetClient("test", 1, TestNetCommandFactory)
				try {
					client.connect("127.0.0.1", PORT)
					Assert.assertTrue("Should be rejected", false)
				} catch (e: NetException) {
					// good!
				}
				clientDone.complete(0)
				serverDone.await()
				println(">>>>>>> CLIENT LAUNCH DONE")
			}
		}
	}

	@Test
	fun `test svr execute remote`() {
		var somethingResult = CompletableDeferred<String>()
		runBlocking {
			val execDone = CompletableDeferred<Int>()
			launch {
				val server = object : NetServer(0, TestNetCommandFactory) {
					override suspend fun onNewConnection(c: INetConnection) {
						super.onNewConnection(c)
						val obj = NetRemoteImpl(c as NetConnection)
						launch {
							obj.doSomethingA()
							Assert.assertEquals("A", somethingResult.await())
							somethingResult = CompletableDeferred()
							obj.doSomethingB(10)
							Assert.assertEquals("10", somethingResult.await())
							somethingResult = CompletableDeferred()
							obj.doSomethingD("hello")
							Assert.assertEquals("hello", somethingResult.await())
							somethingResult = CompletableDeferred()
							obj.doSomethingC(10, 20f)
							Assert.assertEquals("30.0", somethingResult.await())
							somethingResult = CompletableDeferred()
							Assert.assertEquals(100, obj.doSomethingAndReturn(100))
							execDone.complete(0)
						}
					}
				}
				server.listen(PORT)
				execDone.await()
				server.stop()
			}

			launch {
				val obj = object : NetRemoteRemote() {
					override fun doSomethingA() {
						somethingResult.complete("A")
					}

					override fun doSomethingD(s: String) {
						somethingResult.complete(s)
					}

					override fun doSomethingB(x: Int) {
						somethingResult.complete(x.toString())
					}

					override fun doSomethingC(x: Int, y: Float) {
						somethingResult.complete((x + y).toString())
					}

					override fun doSomethingAndReturn(x: Int): Int? {
						return x
					}
				}
				val client = object : NetClient("test", 0, TestNetCommandFactory) {
					override suspend fun executeLocally(method: String, params: Array<out Any?>): Any? {
						return obj.executeLocally(method, *params)
					}
				}
				client.connect("127.0.0.1", PORT)
				execDone.await()
			}
		}
	}

	companion object {

		const val PORT = 9999

	}
}