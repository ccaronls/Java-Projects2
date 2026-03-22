package cc.net2

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.logger.Logger
import cc.lib.logger.LoggerFactory
import cc.lib.math.Vector2D
import cc.lib.net2.INetConnection
import cc.lib.net2.NetConnectQuality
import cc.lib.net2.impl.NetClient
import cc.lib.net2.impl.NetConnection
import cc.lib.net2.impl.NetException
import cc.lib.net2.impl.NetServer
import cc.lib.net2.impl.getSecretCode
import cc.lib.net2.impl.validateSecretCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
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


/**
 * Created by Chris Caron on 3/1/26.
 */
class Net2Test {

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
		runBlocking {
			val done = CompletableDeferred<Int>()
			launch {
				val connected = CompletableDeferred<Int>()
				val disconnected = CompletableDeferred<Int>()
				val server = object : NetServer("host", 0, TestNetCommandFactory) {
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
				server.listen(PORT)
				connected.await()
				server.stop()
				disconnected.await()
				done.complete(0)
			}

			launch {
				val disconnect = CompletableDeferred<Int>()
				val client = object : NetClient("test", 0, TestNetCommandFactory) {
					override fun onDisconnected(reason: String) {
						super.onDisconnected(reason)
						disconnect.complete(0)
					}
				}
				client.connect(HOST, PORT)
				require(client.id > 0)
				disconnect.await()
				done.await()
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
				val server = object : NetServer("host", 0, TestNetCommandFactory) {
					override suspend fun onNewConnection(c: INetConnection) {
						super.onNewConnection(c)
						Assert.assertEquals("test", c.displayName)
						connected.complete(c)
					}
				}
				server.listen(PORT)
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
				client.connect(HOST, PORT)
				disconnect.await()
				done.await()
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
				val server = object : NetServer("host", 0, TestNetCommandFactory) {
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
				server.listen(PORT)
				disconnect.await()
				server.stop()
				done.complete(0)
			}

			launch {
				val client = NetClient("test", 0, TestNetCommandFactory)
				client.connect(HOST, PORT)
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
		runBlocking {
			val done = CompletableDeferred<Int>()
			val disconnected = CompletableDeferred<Int>()
			var connected = CompletableDeferred<Int>()
			val clientDone = CompletableDeferred<Int>()
			launch {
				val server = object : NetServer("host", 0, TestNetCommandFactory) {
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
				server.listen(PORT)
				clientDone.await()
				server.stop()
				done.complete(0)
			}

			launch {
				val clDisconnected = CompletableDeferred<Int>()
				val client = object : NetClient("test", 0, TestNetCommandFactory) {
					override fun onDisconnected(reason: String) {
						super.onDisconnected(reason)
						clDisconnected.complete(0)
					}
				}
				client.connect(HOST, PORT)
				Assert.assertEquals(0, connected.await())
				connected = CompletableDeferred()
				client.disconnect()
				listOf(disconnected, clDisconnected).awaitAll()
				client.connect(HOST, PORT)
				Assert.assertEquals(1, connected.await())
				clientDone.complete(0)
				done.await()
			}
		}
	}

	@Test
	fun `test client reject version mismatch`() {
		runBlocking {
			val clientDone = CompletableDeferred<Int>()
			val serverDone = CompletableDeferred<Int>()
			launch {
				val server = NetServer("host", 0, TestNetCommandFactory)
				server.listen(PORT)
				clientDone.await()
				server.stop()
				serverDone.complete(0)
			}

			launch {
				val client = NetClient("test", 1, TestNetCommandFactory)
				try {
					client.connect(HOST, PORT)
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
		var somethingResult = CompletableDeferred<String>()
		runBlocking {
			val execDone = CompletableDeferred<Int>()
			launch {
				val server = object : NetServer("host", 0, TestNetCommandFactory) {
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
				server.listen(PORT)
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
				val client = object : NetClient("test", 0, TestNetCommandFactory) {
					override suspend fun executeLocally(objectId: Int, method: String, params: Array<out Any?>): Any? {
						return obj.executeLocally(method, *params)
					}
				}
				client.connect(HOST, PORT)
				execDone.await()
			}
		}
	}

	@Test
	fun `test svr execute remote interleaved`() {
		val doSomething2Returned = CompletableDeferred<Int>()
		runBlocking {
			val execDone = CompletableDeferred<Int>()
			launch {
				val server = object : NetServer("host", 0, TestNetCommandFactory) {
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
				server.listen(PORT)
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
				val client = object : NetClient("test", 0, TestNetCommandFactory) {
					override suspend fun executeLocally(objectId: Int, method: String, params: Array<out Any?>): Any? {
						return obj.executeLocally(method, *params)
					}
				}
				client.connect(HOST, PORT)
				execDone.await()
			}
		}
	}

	@Test
	fun `test server lost connection`() {
		val connected = CompletableDeferred<Int>()
		val clDisconnected = CompletableDeferred<Int>()
		runBlocking {
			val disconnected = CompletableDeferred<Int>()
			launch {
				val server = object : NetServer("host", 0, TestNetCommandFactory) {
					override fun createNetConnection(scope: CoroutineScope, id: Int, netServer: NetServer, socket: Socket, input: DataInputStream, output: DataOutputStream): NetConnection {
						return object : NetConnection(scope, id, netServer, socket, input, output) {
							override fun onDisconnected(reason: String) {
								super.onDisconnected(reason)
								disconnected.complete(0)
							}
						}
					}
				}
				server.listen(PORT)
				connected.await()
				server.broadcastTCP(TestCmdSmallImpl("hello"))
				listOf(clDisconnected, disconnected).awaitAll()
				Assert.assertEquals(0, server.connections.count { it.connected })
				server.stop()
			}

			launch {
				val clSocket = CompletableDeferred<Socket>()
				val client = object : NetClient("test", 0, TestNetCommandFactory) {
					override fun configureSocket(socket: Socket) {
						super.configureSocket(socket)
						clSocket.complete(socket)
					}

					override fun onDisconnected(reason: String) {
						super.onDisconnected(reason)
						clDisconnected.complete(0)
					}
				}
				client.connect(HOST, PORT)
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
		runBlocking {
			val svrSocket = CompletableDeferred<Socket>()
			launch {
				val server = object : NetServer("host", 0, TestNetCommandFactory) {
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
				server.listen(PORT)
				connected.await()
				svrSocket.await().close() // break
				clDisconnected.await()
				Assert.assertEquals(0, server.connections.count { it.connected })
				server.stop()
				done.complete(0)
			}

			launch {
				val client = object : NetClient("test", 0, TestNetCommandFactory) {

					override fun onDisconnected(reason: String) {
						super.onDisconnected(reason)
					}
				}
				client.connect(HOST, PORT)
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
		runBlocking {
			val connected = CompletableDeferred<Int>()
			launch {
				val server = object : NetServer("host", 0, TestNetCommandFactory) {
					override suspend fun onNewConnection(c: INetConnection) {
						super.onNewConnection(c)
						connection.complete(c)
					}
				}
				server.enablePing(500)
				server.listen(PORT)
				connected.await()
				delay(5000)
				server.stop()
				done.complete(0)
			}

			launch {
				val client = NetClient("test", 0, TestNetCommandFactory)
				client.connect(HOST, PORT)
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
		runBlocking {
			val execDone = CompletableDeferred<Int>()
			val returnDone = CompletableDeferred<Int>()
			launch {
				val server = object : NetServer("host", 0, TestNetCommandFactory) {
					override suspend fun onNewConnection(c: INetConnection) {
						super.onNewConnection(c)
						val obj = NetRemoteImpl(c)
						launch {
							Assert.assertNull(obj.doSomethingAndReturn(100))
							returnDone.complete(0)
						}
					}
				}
				server.listen(PORT)
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
				val client = object : NetClient("test", 0, TestNetCommandFactory) {
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
				client.connect(HOST, PORT)
				(listOf(clSocket, doSomethingCalled).awaitAll().get(0) as Socket).close()
				closed.complete(0)
				execDone.await()
			}
		}
	}

	@Test
	fun `test commands with nullable fields`() {
		val clConnected = CompletableDeferred<Int>()
		var clReceived = CompletableDeferred<TestCmdNullable>()
		var svrReceived = CompletableDeferred<TestCmdNullable>()
		val clDisconnected = CompletableDeferred<Int>()
		var clDone = CompletableDeferred<Int>()
		runBlocking {
			launch {
				val server = object : NetServer("host", 0, TestNetCommandFactory) {
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
				server.listen(PORT)
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
				val client = object : NetClient("test", 0, TestNetCommandFactory) {
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
				client.connect(HOST, PORT)
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
		runBlocking {
			val clConnected = CompletableDeferred<Int>()
			val clRecieved = CompletableDeferred<TestCmdSmall>()
			val clDisconnected = CompletableDeferred<Int>()
			val svrRecieved = CompletableDeferred<TestCmdSmall>()
			launch {
				val server = object : NetServer("host", 0, TestNetCommandFactory) {
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
				server.startUdp(PORT + 1)
				server.listen(PORT)
				clConnected.await()
				server.broadcastUDP(TestCmdSmallImpl("hello"))
				Assert.assertEquals("hello", clRecieved.await().v)
				Assert.assertEquals("goodbye", svrRecieved.await().v)
				server.stop()
				clDisconnected.await()
			}

			launch {
				val client = object : NetClient("test", 0, TestNetCommandFactory) {
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
				client.connect(HOST, PORT)
				clConnected.complete(0)
			}
		}
	}

	@Test
	fun `test late start udp`() {
		runBlocking {
			val clConnected = CompletableDeferred<Int>()
			val clConnected2 = CompletableDeferred<Int>()
			val udpStarted = CompletableDeferred<Int>()
			val clReceived = CompletableDeferred<TestCmdSmall>()
			val clDisconnected = CompletableDeferred<Int>()
			val svrRecieved = CompletableDeferred<TestCmdSmall>()
			launch {
				val server = object : NetServer("host", 0, TestNetCommandFactory) {
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
				server.listen(PORT)
				clConnected.await()
				server.startUdp(PORT + 1)
				udpStarted.await()
				server.broadcastUDP(TestCmdSmallImpl("hello"))
				Assert.assertEquals("hello", clReceived.await().v)
				Assert.assertEquals("goodbye", svrRecieved.await().v)
				clDisconnected.await()
				clConnected2.await()
				server.stop()
			}

			launch {
				val client = object : NetClient("test", 0, TestNetCommandFactory) {
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
				client.connect(HOST, PORT)
				clConnected.complete(0)
				clReceived.await()
				client.disconnect()
				clDisconnected.await()
				client.connect(HOST, PORT)
				clConnected2.complete(0)
			}
		}
	}

	@Test
	fun `test kick`() {
		runBlocking {
			val clConnected = CompletableDeferred<Int>()
			val clKicked = CompletableDeferred<Int>()
			val clRejected = CompletableDeferred<Int>()
			val clUnKicked = CompletableDeferred<Int>()
			val clReConnected = CompletableDeferred<Int>()
			launch {
				val server = object : NetServer("host", 0, TestNetCommandFactory) {
					override suspend fun onReConnection(c: INetConnection) {
						super.onReConnection(c)
						clReConnected.complete(0)
					}
				}
				server.listen(PORT)
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
				val client = object : NetClient("test", 0, TestNetCommandFactory) {
					override fun onDisconnected(reason: String) {
						super.onDisconnected(reason)
						clKicked.complete(0)
					}
				}
				client.connect(HOST, PORT)
				clConnected.complete(0)
				clKicked.await()
				try {
					client.connect(HOST, PORT)
					Assert.assertTrue("Expected failed connection", false)
				} catch (e: NetException) {
					// all good
				}
				clRejected.complete(0)
				clUnKicked.await()
				client.connect(HOST, PORT)
			}
		}
	}

	@Test
	fun `test multiple client duplicate names`() {
		runBlocking {
			val cl1Connected = CompletableDeferred<Int>()
			val nameChanged = CompletableDeferred<String>()
			launch {
				val server = NetServer("test", 0, TestNetCommandFactory)
				server.listen(PORT)
				nameChanged.await()
				server.stop()
			}

			launch {
				val client = NetClient("test", 0, TestNetCommandFactory)
				client.connect(HOST, PORT)
				cl1Connected.complete(0)
			}

			launch {
				val client = object : NetClient("test", 0, TestNetCommandFactory, logName = "NetClient2") {
					override fun onPropertyChanged(key: String, value: Any?) {
						super.onPropertyChanged(key, value)
						Assert.assertEquals(key, "displayName")
						nameChanged.complete(value as String)
					}
				}
				cl1Connected.await()
				client.connect(HOST, PORT)
				Assert.assertEquals("test (2)", nameChanged.await())
			}

		}
	}

	// TODO: add tests checking equals works.

	// TODO: Add support for copying

	companion object {

		const val PORT = 9999
		const val HOST = "127.0.0.1"

	}
}