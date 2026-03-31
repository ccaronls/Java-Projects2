package cc.lib.net

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.logger.Logger
import cc.lib.logger.LoggerFactory
import cc.lib.math.Vector2D
import cc.lib.net.impl.NetClient
import cc.lib.net.impl.NetConnection
import cc.lib.net.impl.NetException
import cc.lib.net.impl.NetServer
import cc.lib.net.impl.SvrDiscovery
import cc.lib.net.impl.getSecretCode
import cc.lib.net.impl.validateSecretCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
import java.net.InetAddress
import java.net.Socket


/**
 * Created by Chris Caron on 3/1/26.
 */
class NetTest {

	fun <T> runBlockingWithTimeout(timeout: Long = 1000, cb: suspend CoroutineScope.() -> T) {
		runBlocking {
			withTimeout(timeout, cb)
		}
	}

	@Rule
	@JvmField
	val testName = TestName()

	@Before
	fun setup() {
		println(
			"""-------------------------------------------------------------
   >>>> ${testName.methodName}
   -------------------------------------------------------------""".trimIndent())
		LoggerFactory.factory = object : LoggerFactory() {
			override fun getLogger(name: String): Logger {
				return DefaultLogger("${testName.methodName}+$name")
			}
		}
	}

	@After
	fun teardown() {
		println(
			"""-------------------------------------------------------------
   <<<< ${testName.methodName}")
   -------------------------------------------------------------""".trimIndent())
	}

	@Test
	fun testValidation() {
		val t = getSecretCode()
		Assert.assertTrue(validateSecretCode(t))
	}

	@Test
	fun `test command factory serialization`() {
		val output = ByteArrayOutputStream(1024)
		val cmd1 = TestCmdImpl(
			100, 50f, "xyz", 99.0, 2301238760L,
			"hello".toByteArray(), 34, 45, UShort.MAX_VALUE,
			ULong.MAX_VALUE, UInt.MAX_VALUE, true, UByte.MAX_VALUE, TestEnum.TWO,
			100, 50f, "xyz", 99.0, 2301238760L,
			"hello".toByteArray(), 34, 45, UShort.MAX_VALUE,
			ULong.MAX_VALUE, UInt.MAX_VALUE, true, UByte.MAX_VALUE, TestEnum.TWO,
		)
		cmd1.write(output)
		val buffer = output.toByteArray()
		val input = ByteArrayInputStream(buffer)
		val cmd = TestNetCommandFactory.read<TestCmd>(input)
		println(cmd)
		Assert.assertEquals(cmd1, cmd)

	}

	@Test
	fun `test server stops clean client disconnect`() {
		runBlockingWithTimeout {
			val done = CompletableDeferred<Int>()
			launch {
				val connected = CompletableDeferred<Int>()
				val disconnected = CompletableDeferred<Int>()
				val server = object : TestNetServer() {
					override suspend fun onNewConnection(c: INetConnection) {
						super.onNewConnection(c)
						Assert.assertEquals("test", c.displayName)
						connected.complete(0)
					}

					override fun createNetConnection(scope: CoroutineScope, id: Int, netServer: NetServer, socket: Socket, input: DataInputStream, output: DataOutputStream): NetConnection {
						return object : NetConnection(scope, id, netServer, socket, input, output) {
							override fun onDisconnected(reason: String) {
								super.onDisconnected(reason)
								disconnected.complete(0)
							}
						}
					}
				}
				server.listen()
				connected.await()
				server.stop()
				disconnected.await()
				done.complete(0)
			}

			launch {
				val disconnect = CompletableDeferred<Int>()
				val client = object : TestNetClient() {
					override fun onDisconnected(reason: String) {
						super.onDisconnected(reason)
						disconnect.complete(0)
					}
				}
				client.connect(HOST)
				require(client.id > 0)
				disconnect.await()
				done.await()
			}
		}
	}

	@Test
	fun `test server changing properties`() {
		runBlockingWithTimeout {
			val done = CompletableDeferred<Int>()
			var propertyChanged = CompletableDeferred<Pair<String, Any?>>()
			launch {
				val connected = CompletableDeferred<INetConnection>()
				val server = object : TestNetServer() {
					override suspend fun onNewConnection(c: INetConnection) {
						super.onNewConnection(c)
						Assert.assertEquals("test", c.displayName)
						connected.complete(c)
					}
				}
				server.listen()
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
			}

			launch {
				val disconnect = CompletableDeferred<Int>()
				val client = object : TestNetClient() {
					override fun onDisconnected(reason: String) {
						super.onDisconnected(reason)
						disconnect.complete(0)
					}

					override fun onPropertyChanged(key: String, value: Any?) {
						super.onPropertyChanged(key, value)
						propertyChanged.complete(key to value)
					}
				}
				client.connect(HOST)
				disconnect.await()
				done.await()
			}
		}
	}

	@Test
	fun `test client changing properties`() {
		runBlockingWithTimeout {
			val done = CompletableDeferred<Int>()
			var propertyChanged = CompletableDeferred<Pair<String, Any?>>()
			launch {
				val disconnect = CompletableDeferred<Int>()
				val server = object : TestNetServer() {
					override fun createNetConnection(scope: CoroutineScope, id: Int, netServer: NetServer, socket: Socket, input: DataInputStream, output: DataOutputStream): NetConnection {
						return object : NetConnection(scope, id, netServer, socket, input, output) {
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
				server.listen()
				disconnect.await()
				server.stop()
				done.complete(0)
			}

			launch {
				val client = TestNetClient()
				client.connect(HOST)
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
			}
		}
	}

	@Test
	fun `test client disconnect and reconnect`() {
		runBlockingWithTimeout {
			val done = CompletableDeferred<Int>()
			val disconnected = CompletableDeferred<Int>()
			var connected = CompletableDeferred<Int>()
			val clientDone = CompletableDeferred<Int>()
			launch {
				val server = object : TestNetServer() {
					override suspend fun onNewConnection(c: INetConnection) {
						super.onNewConnection(c)
						Assert.assertEquals("test", c.displayName)
						connected.complete(0)
					}

					override fun createNetConnection(scope: CoroutineScope, id: Int, netServer: NetServer, socket: Socket, input: DataInputStream, output: DataOutputStream): NetConnection {
						return object : NetConnection(scope, id, netServer, socket, input, output) {
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
				server.listen()
				clientDone.await()
				server.stop()
				done.complete(0)
			}

			launch {
				val clDisconnected = CompletableDeferred<Int>()
				val client = object : TestNetClient() {
					override fun onDisconnected(reason: String) {
						super.onDisconnected(reason)
						clDisconnected.complete(0)
					}
				}
				client.connect(HOST)
				Assert.assertEquals(0, connected.await())
				connected = CompletableDeferred()
				client.disconnect()
				listOf(disconnected, clDisconnected).awaitAll()
				client.connect(HOST)
				Assert.assertEquals(1, connected.await())
				clientDone.complete(0)
				done.await()
			}
		}
	}

	@Test
	fun `test client reject version mismatch`() {
		runBlockingWithTimeout {
			val clientDone = CompletableDeferred<Int>()
			val serverDone = CompletableDeferred<Int>()
			launch {
				val server = TestNetServer()
				server.listen()
				clientDone.await()
				server.stop()
				serverDone.complete(0)
			}

			launch {
				val client = TestNetClient(version = 1)
				try {
					client.connect(HOST)
					Assert.assertTrue("Should be rejected", false)
				} catch (e: NetException) {
					// good!
				}
				clientDone.complete(0)
				serverDone.await()
			}
		}
	}

	@Test
	fun `test svr execute remote`() {
		runBlockingWithTimeout {
			var somethingResult = CompletableDeferred<String>()
			val execDone = CompletableDeferred<Int>()
			launch {
				val server = object : TestNetServer() {
					override suspend fun onNewConnection(c: INetConnection) {
						super.onNewConnection(c)
						val obj = NetRemoteImpl(c as NetConnection)
						launch {
							obj.doSomethingA(Vector2D(5, 5))
							Assert.assertEquals(Vector2D(5, 5).toString(), somethingResult.await())
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
				server.listen()
				execDone.await()
				server.stop()
			}

			launch {
				val obj = object : NetRemoteRemote() {
					override fun doSomethingA(v: Vector2D) {
						somethingResult.complete(v.toString())
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
				val client = object : TestNetClient() {
					override suspend fun executeLocally(objectId: Int, method: String, params: Array<out Any?>): Any? {
						return obj.executeLocally(method, *params)
					}
				}
				client.connect(HOST)
				execDone.await()
			}
		}
	}

	@Test
	fun `test svr execute remote interleaved`() {
		runBlockingWithTimeout {
			val execDone = CompletableDeferred<Int>()
			val doSomething2Returned = CompletableDeferred<Int>()
			launch {
				val server = object : TestNetServer() {
					override suspend fun onNewConnection(c: INetConnection) {
						super.onNewConnection(c)
						val obj = NetRemoteImpl(c)
						launch {
							Assert.assertEquals(100, obj.doSomethingAndReturn(100))
							execDone.complete(0)
						}
						launch {
							Assert.assertEquals(200, obj.doSomethingAndReturn2(200))
							doSomething2Returned.complete(0)
						}
					}
				}
				server.listen()
				execDone.await()
				server.stop()
			}

			launch {
				val obj = object : NetRemoteRemote() {

					override fun doSomethingAndReturn(x: Int): Int? {
						runBlocking {
							doSomething2Returned.await()
						}
						return x
					}

					override fun doSomethingAndReturn2(x: Int): Int? {
						return x
					}
				}
				val client = object : TestNetClient() {
					override suspend fun executeLocally(objectId: Int, method: String, params: Array<out Any?>): Any? {
						return obj.executeLocally(method, *params)
					}
				}
				client.connect(HOST)
				execDone.await()
			}
		}
	}

	@Test
	fun `test server lost connection`() {
		runBlockingWithTimeout {
			val disconnected = CompletableDeferred<Int>()
			val connected = CompletableDeferred<Int>()
			val clDisconnected = CompletableDeferred<Int>()
			launch {
				val server = object : TestNetServer() {
					override fun createNetConnection(scope: CoroutineScope, id: Int, netServer: NetServer, socket: Socket, input: DataInputStream, output: DataOutputStream): NetConnection {
						return object : NetConnection(scope, id, netServer, socket, input, output) {
							override fun onDisconnected(reason: String) {
								super.onDisconnected(reason)
								disconnected.complete(0)
							}
						}
					}
				}
				server.listen()
				connected.await()
				server.broadcastTCP(TestCmdSmallImpl("hello"))
				listOf(clDisconnected, disconnected).awaitAll()
				Assert.assertEquals(0, server.connections.count { it.connected })
				server.stop()
			}

			launch {
				val clSocket = CompletableDeferred<Socket>()
				val client = object : TestNetClient() {
					override fun configureSocket(socket: Socket) {
						super.configureSocket(socket)
						clSocket.complete(socket)
					}

					override fun onDisconnected(reason: String) {
						super.onDisconnected(reason)
						clDisconnected.complete(0)
					}
				}
				client.connect(HOST)
				clSocket.await().close() // trigger an unexpected disconnect TODO: is there a better way?
				connected.complete(0)
			}
		}
	}

	@Test
	fun `test client lost connection`() {
		val connected = CompletableDeferred<Int>()
		val svrBroken = CompletableDeferred<Int>()
		val clDisconnected = CompletableDeferred<Int>()
		val done = CompletableDeferred<Int>()
		runBlockingWithTimeout {
			val svrSocket = CompletableDeferred<Socket>()
			launch {
				val server = object : TestNetServer() {
					override fun createNetConnection(scope: CoroutineScope, id: Int, netServer: NetServer, socket: Socket, input: DataInputStream, output: DataOutputStream): NetConnection {
						return object : NetConnection(scope, id, netServer, socket, input, output) {
							override fun onDisconnected(reason: String) {
								super.onDisconnected(reason)
								clDisconnected.complete(0)
							}
						}.also {
							svrSocket.complete(socket)
						}
					}
				}
				server.listen()
				connected.await()
				svrSocket.await().close() // break
				clDisconnected.await()
				Assert.assertEquals(0, server.connections.count { it.connected })
				server.stop()
				done.complete(0)
			}

			launch {
				val client = object : TestNetClient() {

					override fun onDisconnected(reason: String) {
						super.onDisconnected(reason)
					}
				}
				client.connect(HOST)
				connected.complete(0)
				client.sendTCP(TestCmdSmallImpl("hello"))
				clDisconnected.await()
				done.await()
			}
		}
	}

	@Test
	fun `test ping`() {
		val connection = CompletableDeferred<INetConnection>()
		val done = CompletableDeferred<Int>()
		runBlockingWithTimeout(6000) {
			val connected = CompletableDeferred<Int>()
			launch {
				val server = object : TestNetServer() {
					override suspend fun onNewConnection(c: INetConnection) {
						super.onNewConnection(c)
						connection.complete(c)
					}
				}
				server.enablePing(500)
				server.listen()
				connected.await()
				delay(5000)
				server.stop()
				done.complete(0)
			}

			launch {
				val client = TestNetClient()
				client.connect(HOST)
				connected.complete(0)
			}

			var quality: NetConnectQuality = NetConnectQuality.UNKNOWN
			val job = launch {
				connection.await().stats.onEach {
					println("Quality: $it+${it.quality}")
					quality = it.quality
				}.collect()
			}

			done.await()
			job.cancel()
			Assert.assertTrue(quality != NetConnectQuality.UNKNOWN)
		}
	}

	@Test
	fun `test svr execute remote interrupted`() {
		runBlockingWithTimeout {
			val execDone = CompletableDeferred<Int>()
			val returnDone = CompletableDeferred<Int>()
			launch {
				val server = object : TestNetServer() {
					override suspend fun onNewConnection(c: INetConnection) {
						super.onNewConnection(c)
						val obj = NetRemoteImpl(c)
						launch {
							Assert.assertNull(obj.doSomethingAndReturn(100))
							returnDone.complete(0)
						}
					}
				}
				server.listen()
				returnDone.await()
				execDone.await()
				server.stop()
			}

			launch {
				val clSocket = CompletableDeferred<Socket>()
				val doSomethingCalled = CompletableDeferred<Int>()
				val closed = CompletableDeferred<Int>()
				val obj = object : NetRemoteRemote() {

					override fun doSomethingAndReturn(x: Int): Int? {
						runBlocking {
							doSomethingCalled.complete(0)
							closed.await()
						}
						return x
					}

				}
				val client = object : TestNetClient() {
					override suspend fun executeLocally(objectId: Int, method: String, params: Array<out Any?>): Any? {
						return obj.executeLocally(method, *params)
					}

					override fun configureSocket(socket: Socket) {
						super.configureSocket(socket)
						clSocket.complete(socket)
					}

					override fun onDisconnected(reason: String) {
						super.onDisconnected(reason)
						execDone.complete(0)
					}
				}
				client.connect(HOST)
				(listOf(clSocket, doSomethingCalled).awaitAll().get(0) as Socket).close()
				closed.complete(0)
				execDone.await()
			}
		}
	}

	@Test
	fun `test commands with nullable fields`() {
		runBlockingWithTimeout {
			val clConnected = CompletableDeferred<Int>()
			var clReceived = CompletableDeferred<TestCmdNullable>()
			var svrReceived = CompletableDeferred<TestCmdNullable>()
			val clDisconnected = CompletableDeferred<Int>()
			var clDone = CompletableDeferred<Int>()
			launch {
				val server = object : TestNetServer() {
					override fun createNetConnection(scope: CoroutineScope, id: Int, netServer: NetServer, socket: Socket, input: DataInputStream, output: DataOutputStream): NetConnection {
						return object : NetConnection(scope, id, netServer, socket, input, output) {
							override suspend fun onCommand(cmd: INetCommand) {
								when (cmd) {
									is TestCmdNullable -> svrReceived.complete(cmd)
									else -> super.onCommand(cmd)
								}
							}
						}
					}
				}
				server.listen()
				clConnected.await()
				server.broadcastTCP(TestCmdNullableImpl(null, null, null))
				clReceived.await().also {
					Assert.assertNull(it.a)
					Assert.assertNull(it.b)
					Assert.assertNull(it.c)
				}
				clReceived = CompletableDeferred()
				server.broadcastTCP(TestCmdNullableImpl("hello", 100, "goodbyte".toByteArray()))
				clReceived.await().also {
					Assert.assertEquals("hello", it.a)
					Assert.assertEquals(100, it.b)
					Assert.assertEquals("goodbyte", String(it.c!!))
				}
				clDone.await()
				server.stop()
				clDisconnected.await()
			}

			launch {
				val client = object : TestNetClient() {
					override suspend fun onCommand(cmd: INetCommand) {
						when (cmd) {
							is TestCmdNullable -> clReceived.complete(cmd)
							else -> super.onCommand(cmd)
						}
					}

					override fun onDisconnected(reason: String) {
						super.onDisconnected(reason)
						clDisconnected.complete(0)
					}
				}
				client.connect(HOST)
				clConnected.complete(0)
				client.sendTCP(TestCmdNullableImpl(null, null, null))
				svrReceived.await().also {
					Assert.assertNull(it.a)
					Assert.assertNull(it.b)
					Assert.assertNull(it.c)
				}
				svrReceived = CompletableDeferred()
				client.sendTCP(TestCmdNullableImpl("hello", 100, "goodbyte".toByteArray()))
				svrReceived.await().also {
					Assert.assertEquals("hello", it.a)
					Assert.assertEquals(100, it.b)
					Assert.assertEquals("goodbyte", String(it.c!!))
				}
				clDone.complete(0)

			}
		}
	}

	@Test
	fun `test udp`() {
		runBlockingWithTimeout {
			val clConnected = CompletableDeferred<Int>()
			val clRecieved = CompletableDeferred<TestCmdSmall>()
			val clDisconnected = CompletableDeferred<Int>()
			val svrRecieved = CompletableDeferred<TestCmdSmall>()
			launch {
				val server = object : TestNetServer() {
					override fun createNetConnection(scope: CoroutineScope, id: Int, netServer: NetServer, socket: Socket, input: DataInputStream, output: DataOutputStream): NetConnection {
						return object : NetConnection(scope, id, netServer, socket, input, output) {
							var count = 0
							override suspend fun onCommand(cmd: INetCommand) {
								when (cmd) {
									is TestCmdSmall -> {
										if (count++ > 10)
											svrRecieved.complete(cmd)
										broadcastUDP(TestCmdSmallImpl("hello"))
									}

									else -> super.onCommand(cmd)
								}
							}
						}
					}
				}
				server.startUdp()
				server.listen()
				clConnected.await()
				server.broadcastUDP(TestCmdSmallImpl("hello"))
				Assert.assertEquals("hello", clRecieved.await().v)
				Assert.assertEquals("goodbye", svrRecieved.await().v)
				server.stop()
				clDisconnected.await()
			}

			launch {
				val client = object : TestNetClient() {
					var count = 0
					override suspend fun onCommand(cmd: INetCommand) {
						when (cmd) {
							is TestCmdSmall -> {
								if (count++ > 10)
									clRecieved.complete(cmd)
								sendUDP(TestCmdSmallImpl("goodbye"))
							}

							else -> super.onCommand(cmd)
						}
					}

					override fun onDisconnected(reason: String) {
						super.onDisconnected(reason)
						clDisconnected.complete(0)
					}
				}
				client.connect(HOST)
				clConnected.complete(0)
			}
		}
	}

	@Test
	fun `test late start udp`() {
		runBlockingWithTimeout {
			val clConnected = CompletableDeferred<Int>()
			val clConnected2 = CompletableDeferred<Int>()
			val udpStarted = CompletableDeferred<Int>()
			val clReceived = CompletableDeferred<TestCmdSmall>()
			val clDisconnected = CompletableDeferred<Int>()
			val svrRecieved = CompletableDeferred<TestCmdSmall>()
			launch {
				val server = object : TestNetServer() {
					override fun createNetConnection(scope: CoroutineScope, id: Int, netServer: NetServer, socket: Socket, input: DataInputStream, output: DataOutputStream): NetConnection {
						return object : NetConnection(scope, id, netServer, socket, input, output) {
							override suspend fun onCommand(cmd: INetCommand) {
								when (cmd) {
									is TestCmdSmall -> svrRecieved.complete(cmd)
									else -> super.onCommand(cmd)
								}
							}
						}
					}
				}
				server.listen()
				clConnected.await()
				server.startUdp()
				udpStarted.await()
				server.broadcastUDP(TestCmdSmallImpl("hello"))
				Assert.assertEquals("hello", clReceived.await().v)
				Assert.assertEquals("goodbye", svrRecieved.await().v)
				clDisconnected.await()
				clConnected2.await()
				server.stop()
			}

			launch {
				val client = object : TestNetClient() {
					override suspend fun onCommand(cmd: INetCommand) {
						when (cmd) {
							is TestCmdSmall -> {
								sendUDP(TestCmdSmallImpl("goodbye"))
								clReceived.complete(cmd)
							}

							else -> super.onCommand(cmd)
						}
					}

					override fun onDisconnected(reason: String) {
						super.onDisconnected(reason)
						clDisconnected.complete(0)
					}

					override fun onUdpChannelStarted() {
						super.onUdpChannelStarted()
						udpStarted.complete(0)
					}
				}
				client.connect(HOST)
				clConnected.complete(0)
				clReceived.await()
				client.disconnect()
				clDisconnected.await()
				client.connect(HOST)
				clConnected2.complete(0)
			}
		}
	}

	@Test
	fun `test kick`() {
		runBlockingWithTimeout {
			val clConnected = CompletableDeferred<Int>()
			val clKicked = CompletableDeferred<Int>()
			val clRejected = CompletableDeferred<Int>()
			val clUnKicked = CompletableDeferred<Int>()
			val clReConnected = CompletableDeferred<Int>()
			launch {
				val server = object : TestNetServer() {
					override suspend fun onReConnection(c: INetConnection) {
						super.onReConnection(c)
						clReConnected.complete(0)
					}
				}
				server.listen()
				clConnected.await()
				server.connections.first().kicked = true
				clKicked.await()
				clRejected.await()
				server.connections.first().kicked = false
				clUnKicked.complete(0)
				clReConnected.await()
				server.stop()
			}

			launch {
				val client = object : TestNetClient() {
					override fun onDisconnected(reason: String) {
						super.onDisconnected(reason)
						clKicked.complete(0)
					}
				}
				client.connect(HOST)
				clConnected.complete(0)
				clKicked.await()
				try {
					client.connect(HOST)
					Assert.assertTrue("Expected failed connection", false)
				} catch (e: NetException) {
					// all good
				}
				clRejected.complete(0)
				clUnKicked.await()
				client.connect(HOST)
			}
		}
	}

	@Test
	fun `test multiple client duplicate names`() {
		runBlockingWithTimeout {
			val cl1Connected = CompletableDeferred<Int>()
			val nameChanged = CompletableDeferred<String>()
			launch {
				val server = TestNetServer("test")
				server.listen()
				nameChanged.await()
				server.stop()
			}

			launch {
				val client = TestNetClient()
				client.connect(HOST)
				cl1Connected.complete(0)
			}

			launch {
				val client = object : TestNetClient("NetClient2") {
					override fun onPropertyChanged(key: String, value: Any?) {
						super.onPropertyChanged(key, value)
						Assert.assertEquals(key, "displayName")
						nameChanged.complete(value as String)
					}
				}
				cl1Connected.await()
				client.connect(HOST)
				Assert.assertEquals("test (2)", nameChanged.await())
			}

		}
	}

	@Test
	fun testListeners() {
		runBlockingWithTimeout {
			val clConnected = CompletableDeferred<Int>()
			val clCommand = CompletableDeferred<Int>()
			val clDisconnect = CompletableDeferred<Int>()
			val svrStopped = CompletableDeferred<Int>()
			val connConnected = CompletableDeferred<Int>()
			val connDisconnected = CompletableDeferred<Int>()
			val connReconnected = CompletableDeferred<Int>()
			val connComamnd = CompletableDeferred<Int>()
			launch {
				val server = TestNetServer()
				server.addListener(object : INetServer.Listener {

					override suspend fun onNewConnection(conn: INetConnection) {
						println("SVR:onNewConnection")
						connConnected.complete(0)
						super.onNewConnection(conn)
					}

					override suspend fun onConnectionDisconnected(conn: INetConnection, reason: String) {
						println("SVR:onConnectionDisconnected")
						connDisconnected.complete(0)
						super.onConnectionDisconnected(conn, reason)
					}

					override suspend fun onConnectionReconnected(conn: INetConnection) {
						println("SVR:onConnectionReconnected")
						connReconnected.complete(0)
						super.onConnectionReconnected(conn)
					}

					override suspend fun onConnectionCommand(conn: INetConnection, cmd: INetCommand) {
						println("SVR:onConnectionCommand")
						connComamnd.complete(0)
						super.onConnectionCommand(conn, cmd)
					}

					override fun onServerStopped() {
						println("SVR:onServerStopped")
						svrStopped.complete(0)
						super.onServerStopped()
					}
				})
				server.listen()
				connComamnd.await()
				server.connections.first().sendTCP(TestCmdSmallImpl("hello"))
				clCommand.await()
				server.stop()
				svrStopped.await()
			}

			launch {
				val client = TestNetClient()
				client.addListener(object : INetClient.Listener {
					override suspend fun onClientConnected(clientId: Int) {
						println("CL:onConnected")
						clConnected.complete(0)
						super.onClientConnected(clientId)
					}

					override suspend fun onClientDisconnected(reason: String) {
						println("CL:onDisconnected")
						clDisconnect.complete(0)
						super.onClientDisconnected(reason)
					}

					override suspend fun onClientReceivedCommand(cmd: INetCommand) {
						println("CL:onCommand")
						clCommand.complete(0)
						super.onClientReceivedCommand(cmd)
					}
				})
				client.connect(HOST)
				connConnected.await()
				client.disconnect()
				clDisconnect.await()
				connDisconnected.await()
				client.connect(HOST)
				connReconnected.await()
				client.sendTCP(TestCmdSmallImpl("goodbye"))
				connComamnd.await()
			}
		}
	}

	@Test
	fun `test discovery`() {
		runBlockingWithTimeout {

			val hostDiscovered = CompletableDeferred<SvrDiscovery>()
			val hostRemoved = CompletableDeferred<Int>()
			val connected = CompletableDeferred<Int>()
			val stateFlowDetected = CompletableDeferred<Int>()

			launch {
				val server = TestNetServer()
				server.startDiscovery("TestServer")
				server.listen()
				hostDiscovered.await()
				stateFlowDetected.await()
				connected.await()
				server.stopDiscovery()
				hostRemoved.await()
				server.stop()
			}

			launch {
				val client = TestNetClient()
				client.addListener(object : INetClient.Listener {
					override fun onClientDiscoveredHost(host: SvrDiscovery) {
						println("host discovered: $host")
						hostDiscovered.complete(host)
					}

					override fun onClientRemovedHost(host: SvrDiscovery) {
						println("host removed: $host")
						hostRemoved.complete(0)
					}

					override suspend fun onClientConnected(clientId: Int) {
						super.onClientConnected(clientId)
						connected.complete(0)
					}
				})
				client.startDiscovery()
				val job = launch {
					client.discoveredHosts.onEach {
						if (it.isNotEmpty())
							stateFlowDetected.complete(1)
					}.collect()
				}
				hostDiscovered.await().let {
					client.connect(InetAddress.getByName(it.hostAddress))
				}
				connected.await()
				hostRemoved.await()
				job.cancel()
				client.stopDiscovery()
			}


		}
	}

	// TODO: add tests checking equals works.

	// TODO: Add support for copying

	companion object {

		const val PORT = 9999
		val HOST = InetAddress.getLocalHost()

	}

	open class TestNetClient(logName: String = "NetClient1", version: Int = 0) : NetClient(
		"test", PORT, version, TestNetCommandFactory, logName = logName
	)

	open class TestNetServer(displayName: String = "host") : NetServer(displayName, PORT, 0, TestNetCommandFactory)
}