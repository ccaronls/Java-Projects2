package cc.lib.net

import cc.lib.crypt.Cypher
import cc.lib.crypt.HuffmanEncoding
import cc.lib.game.GColor
import cc.lib.game.Utils
import cc.lib.utils.Lock
import junit.framework.TestCase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.util.Arrays
import java.util.LinkedList

/**
 * Test the fundamental connect/disconnect/reconnect functionality to make sure all the
 * appropriate client/server callbacks are made.
 * @author ccaron
 */
class GameServerTest : TestCase() {
	var result: Throwable? = null
	var waitLock = Lock()
	var testMonitor = Lock()
	var listener1: MyConnectionListener? = null
	lateinit var server: GameServer
	lateinit var client: MyGameClient
	private fun blockOrFail() {
		waitLock.block(3000) { fail("timeout") }
	}

	@Throws(Exception::class)
	override fun setUp() {
		// TODO Auto-generated method stub
		println(
			"""
	---------------------------------------
	SETUP Test: $name
	---------------------------------------
	
	""".trimIndent()
		)
		cypher = null
		if (name.indexOf("Encrypt") > 0) {
			cypher = HuffmanEncoding("aslkjhajsd")
		}
		waitLock.reset()
		result = null
		listener1 = MyConnectionListener()
		server = GameServer("Test", PORT, VERSION, cypher, 2)
		server.addListener(object : AGameServer.Listener {
			override fun onConnected(conn: AClientConnection) {
				println("New Client connected: " + conn.displayName)
				conn.addListener(listener1!!)
				waitLock.release()
			}

			override fun onServerStopped() {
				println("Server Stopped")
				waitLock.release()
			}
		})
		server.listen()
		client = MyGameClient("A", VERSION)
		Utils.waitNoThrow(this, 100)
	}

	var cypher: Cypher? = null

	@Throws(Exception::class)
	override fun tearDown() {
		// TODO Auto-generated method stub
		println(
			"""
	---------------------------------------
	TEARDOWN Test: $name
	---------------------------------------
	
	""".trimIndent()
		)
		client.close()
		server.stop()
		result?.let {
			it.printStackTrace()
			System.err.println("FAILED: " + it.message)
			fail(it.message)
		}
	}

	fun interface RunnableThrowing {
		@Throws(Exception::class)
		fun run()
	}

	@Throws(Exception::class)
	fun runTest(test: RunnableThrowing) {
		Thread {
			try {
				println(
					"""
	---------------------------------------
	RUNNING Test: $name
	---------------------------------------
	
	""".trimIndent()
				)
				test.run()
				println(
					"""
	---------------------------------------
	RUN TEST COMPLETE Test: $name
	---------------------------------------
	
	""".trimIndent()
				)
				waitLock.acquire()
				server.stop()
				blockOrFail()
			} catch (e: Throwable) {
				println(
					"""
	---------------------------------------
	TEST ERROR Test: $name
	${e.javaClass.simpleName}:${e.message}---------------------------------------
	
	""".trimIndent()
				)
				result = e
			} finally {
				synchronized(testMonitor) { testMonitor.release() }
			}
		}.start()
		synchronized(testMonitor) { testMonitor.acquireAndBlock() }
		Utils.waitNoThrow(1000)
	}

	@Throws(Exception::class)
	fun testGameCommand() {
		val t = GameCommandType("A")
		var c = GameCommand(t)
		c.setArg("bool", true)
		c.setArg("int", 1)
		c.setArg("float", 2f)
		c.setArg("long", 3L)
		c.setArg("double", 4.0)
		c.setArg("string", "This is a string")
		c.setArg("color", GColor.RED)
		val bout = ByteArrayOutputStream()
		val dout = DataOutputStream(bout)
		c.write(dout)
		val din = DataInputStream(ByteArrayInputStream(bout.toByteArray()))
		c = GameCommand.parse(din)
		assertEquals(c.type, t)
		assertEquals(c.getBoolean("bool"), true)
		assertEquals(c.getInt("int"), 1)
		assertEquals(c.getFloat("float"), 2f)
		assertEquals(c.getLong("long"), 3L)
		assertEquals(c.getDouble("double"), 4.0)
		assertEquals(c.getReflector("color", GColor()), GColor.RED)
	}

	@Throws(Exception::class)
	fun testKick() {
		runTest {
			assertTrue(server.isRunning)
			waitLock.acquire(2)
			client.connectBlocking(InetAddress.getLocalHost(), PORT)
			blockOrFail()
			assertTrue(listener1!!.connected)
			assertTrue(client.connected)
			val conn = server.getClientConnection(client.displayName)
			assertNotNull(conn)
			waitLock.acquire(2)
			conn!!.kick()
			blockOrFail()
			assertFalse(listener1!!.connected)
			assertFalse(client.connected)
			waitLock.acquire()
			client.reconnectAsync()
			blockOrFail()
			assertFalse(listener1!!.connected)
			assertFalse(client.connected)
			conn.unkick()
			waitLock.acquire(3)
			client.reconnectAsync()
			blockOrFail()
			assertTrue(listener1!!.connected)
			assertTrue(listener1!!.reconnected)
			assertTrue(client.connected)
		}
	}

	// test basic client connect and disconnect
	@Throws(Exception::class)
	fun testClientDisconnect() {
		runTest {
			assertTrue(server.isRunning)
			waitLock.acquire(2)
			client.connectBlocking(InetAddress.getLocalHost(), PORT)
			blockOrFail()
			assertTrue(listener1!!.connected)
			assertTrue(client.connected)
			waitLock.acquire(2)
			client.disconnect()
			blockOrFail()
			assertTrue(listener1!!.disconnected)
			assertFalse(client.isConnected)
			assertEquals(0, server.numConnectedClients)
		}
	}

	@Throws(Exception::class)
	fun testExecuteRemoteMethod() {
		runTest {
			waitLock.acquire(2)
			client.connectBlocking(InetAddress.getLocalHost(), PORT)
			blockOrFail()
			assertTrue(listener1!!.connected)
			assertTrue(client.connected)
			val conn = server.getClientConnection(client.displayName)
			assertNotNull(conn)
			/////////////////
			waitLock.acquire()
			conn!!.executeMethodOnRemote<Any>(CLIENT_ID, false, "testMethod_int", 10)
			blockOrFail()
			assertEquals(client.argLong, 10)
			/////////////////
			waitLock.acquire()
			conn.executeMethodOnRemote<Any>(CLIENT_ID, false, "testMethod_Int", 11)
			blockOrFail()
			assertEquals(client.argLong, 11)
			/////////////////
			waitLock.acquire()
			conn.executeMethodOnRemote<Any>(CLIENT_ID, false, "testMethod_float", 12.0f)
			blockOrFail()
			assertEquals(client.argFloat, 12.0)
			/////////////////
			waitLock.acquire()
			conn.executeMethodOnRemote<Any>(CLIENT_ID, false, "testMethod_Float", 13.0f)
			blockOrFail()
			assertEquals(client.argFloat, 13.0)
			/////////////////
			val map: MutableMap<Any?, Any?> = HashMap()
			map["hello"] = 1
			map["goodbyte"] = 2
			waitLock.acquire()
			conn.executeMethodOnRemote<Any>(CLIENT_ID, false, "testMethod_Map", map)
			blockOrFail()
			assertTrue(Utils.isEquals(client.argMap, map))
			val c: MutableCollection<Any?> = LinkedList<Any?>()
			c.add("a")
			c.add("b")
			waitLock.acquire()
			conn.executeMethodOnRemote<Any>(CLIENT_ID, false, "testMethod_Collection", c)
			blockOrFail()
			assertTrue(Utils.isEquals(client.argCollection, c))
			val result = conn.executeMethodOnRemote<Array<String>>(
				CLIENT_ID,
				true,
				"testMethod_returns",
				15,
				"Hello",
				HashMap<Any, Any>()
			)!!
			//                    blockOrFail();
			assertEquals(15, client.argLong)
			assertEquals("Hello", client.argString)
			assertNotNull(client.argMap)
			assertNotNull(result)
			assertTrue(Arrays.equals(result, arrayOf("a", "b", "c")))
			waitLock.acquire(2)
			client.disconnect()
			blockOrFail()
			assertTrue(listener1!!.disconnected)
		}
	}

	// test basic client connect and disconnect
	@Throws(Exception::class)
	fun testRejectBadVersionClient() {
		runTest {
			waitLock.acquire(1)
			client = MyGameClient("A", "BadVersion")
			client.connectBlocking(InetAddress.getLocalHost(), PORT)
			blockOrFail()
			assertFalse(client.connected)
			assertFalse(client.isConnected)
			assertEquals(0, server.numClients)
		}
	}

	// test basic client connect and disconnect
	@Throws(Exception::class)
	fun testRejectDuplicateClient() {
		runTest {
			val cl1 = MyGameClient("A", VERSION)
			waitLock.acquire(2)
			cl1.connectBlocking(InetAddress.getLocalHost(), PORT)
			blockOrFail()
			assertTrue(cl1.isConnected)
			val cl2 = MyGameClient("A", VERSION)
			waitLock.acquire(1)
			cl2.connectBlocking(InetAddress.getLocalHost(), PORT)
			blockOrFail()
			assertFalse(cl2.isConnected)
		}
	}

	// test client connect->disconnect->reconnect
	@Throws(Exception::class)
	fun testClientReconnect() {
		runTest {
			waitLock.acquire(2)
			client.connectBlocking(InetAddress.getLocalHost(), PORT)
			blockOrFail()
			assertTrue(listener1!!.connected)
			waitLock.acquire()
			client.disconnect()
			blockOrFail()
			assertTrue(listener1!!.disconnected)
			waitLock.acquire(2)
			client.connectBlocking(InetAddress.getLocalHost(), PORT)
			blockOrFail()
			assertTrue(listener1!!.reconnected)
			waitLock.acquire(2)
			client.disconnect()
			blockOrFail()
		}
	}

	// test client connect and server disconnect
	@Throws(Exception::class)
	fun testServerDisconnect() {
		runTest {
			val cl = MyGameClient("XxX")
			waitLock.acquire(2)
			cl.connectBlocking(InetAddress.getLocalHost(), PORT)
			blockOrFail()
			assertTrue(listener1!!.connected)
			assertTrue(cl.connected)
			//server.stop();
			val conn = server.getClientConnection("XxX")
			assertNotNull(conn)
			waitLock.acquire(2)
			conn!!.disconnect("Sumthing")
			blockOrFail()
			assertTrue(cl.disconnected)
			assertTrue(listener1!!.disconnected)
			assertFalse(cl.isConnected)
		}
	}

	// test client connect and server disconnect by shutdown
	@Throws(Exception::class)
	fun testServerShutdown() {
		runTest {
			val cl = MyGameClient()
			waitLock.acquire(2)
			client.connectBlocking(InetAddress.getLocalHost(), PORT)
			blockOrFail()
			assertTrue(listener1!!.connected)
			server.stop()
			blockOrFail()
			assertTrue(client.disconnected)
			assertTrue(!client.isConnected)
		}
	}

	// test client connect and server disconnect by shutdown
	@Throws(Exception::class)
	fun testMessage() {
		runTest {
			client = MyGameClient("ABC")
			waitLock.acquire(2)
			client.connectBlocking(InetAddress.getLocalHost(), PORT)
			blockOrFail()
			assertTrue(client.isConnected)
			val conn = server.getClientConnection(client.displayName)
			assertNotNull(conn)
			waitLock.acquire(20)
			Thread {
				for (i in 0..9) {
					conn!!.sendMessage(i.toString())
					Utils.waitNoThrow((Utils.rand() % 100).toLong())
				}
			}.start()
			Thread {
				for (i in 0..9) {
					client.sendMessage(i.toString())
					Utils.waitNoThrow((Utils.rand() % 100).toLong())
				}
			}.start()
			waitLock.block(10000) { println("Timeout") }
			assertEquals(10, client.messages.size)
			assertEquals(10, listener1!!.messages.size)
			println(
				"""MESSAGES:
    ${client.messages}
   ${listener1!!.messages}"""
			)
		}
	}

	// test client connect and server disconnect by shutdown
	@Throws(Exception::class)
	fun testMessage2() {
		runTest {
			client = MyGameClient("ABC")
			waitLock.acquire(2)
			client.connectBlocking(InetAddress.getLocalHost(), PORT)
			blockOrFail()
			val conn = server.getClientConnection("ABC")
			conn!!.addListener(listener1!!)
			assertTrue(listener1!!.connected)
			assertTrue(client.connected)
			assertTrue(client.isConnected)
			val TEST = GameCommandType("TEST")
			waitLock.acquire(2)
			client.sendCommand(GameCommand(TEST))
			blockOrFail()
			assertNotNull(listener1!!.lastCommand)
			assertEquals(listener1!!.lastCommand!!.type, TEST)
			assertNotNull(conn)
			waitLock.acquire(2)
			conn.sendCommand(GameCommand(TEST))
			blockOrFail()
			assertNotNull(client.lastCommand)
			assertEquals(client.lastCommand!!.type, TEST)
			waitLock.acquire()
			client.disconnect()
			blockOrFail()
			assertTrue(!client.isConnected)
			assertFalse(conn.isConnected)
		}
	}

	// test client connect and server disconnect by shutdown
	@Throws(Exception::class)
	fun x_testEncryption() {
		runTest {
			val TEST = GameCommandType("TEST2")
			client = MyGameClient("ABC")
			waitLock.acquire()
			client.connectBlocking(InetAddress.getLocalHost(), PORT)
			blockOrFail()
			val conn = server.getClientConnection("ABC")
			conn!!.addListener(listener1!!)
			assertTrue(listener1!!.connected)
			assertTrue(client.connected)
			assertTrue(client.isConnected)
			assertTrue(conn.isConnected)
			waitLock.acquire()
			client.sendCommand(GameCommand(TEST))
			blockOrFail()
			assertNotNull(listener1!!.lastCommand)
			assertEquals(listener1!!.lastCommand!!.type, TEST)
			assertNotNull(conn)
			waitLock.acquire()
			conn.sendCommand(GameCommand(TEST))
			blockOrFail()
			assertNotNull(client.lastCommand)
			assertEquals(client.lastCommand!!.type, TEST)
			waitLock.acquire()
			client.disconnect()
			blockOrFail()
			assertTrue(!client.isConnected)
			assertFalse(conn.isConnected)
		}
	}

	@Throws(Exception::class)
	fun testClientHardDisconnect() {
		runTest {
			waitLock.acquire(2)
			client.connectBlocking(InetAddress.getLocalHost(), PORT)
			blockOrFail()
			assertTrue(listener1!!.connected)
			assertTrue(client.isConnected)
			assertEquals(1, server.numConnectedClients)
			waitLock.acquire()
			client.close() //.disconnect();
			blockOrFail()
			assertTrue(listener1!!.disconnected)
			assertFalse(client.isConnected)
			assertEquals(0, server.numConnectedClients)
		}
	}

	@Throws(Exception::class)
	fun testExecuteOnRemote() {
		runTest {
			server.PING_FREQ = 2000
			waitLock.acquire(2)
			client.connectBlocking(InetAddress.getLocalHost(), PORT)
			blockOrFail()
			assertTrue(listener1!!.connected)
			assertTrue(client.isConnected)
			val conn = server.getClientConnection(client.displayName)
			val result =
				conn!!.executeMethodOnRemote<Int>(CLIENT_ID, true, "testMethod_returnsLongBlock")
			assertNotNull(result)
			assertEquals(100, result)
		}
	}

	@Throws(Exception::class)
	fun testClientTimeout() {
		runTest {
			server.TIMEOUT = 3000

			// test that a client that has not sent some message within in the timeout is disconnected
			waitLock.acquire(2)
			client.connectBlocking(InetAddress.getLocalHost(), PORT)
			blockOrFail()
			assertTrue(listener1!!.connected)
			//client.close();//.disconnect();
			waitLock.acquire(2)
			waitLock.block(50000) { fail("timeout") }
			assertFalse(listener1!!.connected)
			assertFalse(client.connected)
			assertFalse(client.isConnected)
		}
	}

	inner class MyConnectionListener : AClientConnection.Listener {
		var connected = true
		var reconnected = false
		var disconnected = false
		var lastCommand: GameCommand? = null
		var messages: MutableList<String> = ArrayList()
		override fun onReconnected(conn: AClientConnection) {
			println(conn.displayName + ":onReconnection: " + conn)
			connected = true
			reconnected = true
			disconnected = false
			waitLock.release()
		}

		override fun onDisconnected(conn: AClientConnection, reason: String) {
			println(conn.displayName + ":onClientDisconnected: " + conn + " reason: " + reason)
			connected = false
			reconnected = false
			disconnected = true
			waitLock.release()
		}

		override fun onCommand(conn: AClientConnection, cmd: GameCommand) {
			println(conn.displayName + ":onCommand: " + conn + ": " + cmd)
			lastCommand = cmd
			if (cmd.type == GameCommandType.MESSAGE) messages.add(cmd.getMessage())
			waitLock.release()
		}

		override fun onTimeout(conn: AClientConnection) {
			println(conn.displayName + ":onTimeout")
			waitLock.release()
		}
	}

	val CLIENT_ID = "CLIENT"

	inner class MyGameClient @JvmOverloads constructor(
		name: String? = "test" + numClients,
		version: String? = VERSION
	) : GameClient(
		name!!, version!!, cypher
	), AGameClient.Listener {
		var connected = false
		var disconnected = false
		var lastCommand: GameCommand? = null
		var messages: MutableList<String> = ArrayList()
		override fun onCommand(cmd: GameCommand) {
			println("$displayName onCommand: $cmd")
			lastCommand = cmd
			waitLock.release()
		}

		override fun onMessage(message: String) {
			println("$displayName onMessage: $message")
			messages.add(message)
			waitLock.release()
		}

		override fun onDisconnected(reason: String, serverInitiated: Boolean) {
			println("$displayName onDisconnected: $reason serverIntiiated: $serverInitiated")
			connected = false
			disconnected = true
			unregister(CLIENT_ID)
			waitLock.release()
		}

		override fun onConnected() {
			println("$displayName onConnected")
			connected = true
			disconnected = false
			register(CLIENT_ID, this)
			waitLock.release()
		}

		fun testMethod_returns(arg0: Int, arg1: String, arg2: Map<*, *>): Array<String> {
			println("$name:testMethod_returns executed with: $arg0 $arg1 $arg2")
			argLong = arg0.toLong()
			argString = arg1
			argMap = arg2
			return arrayOf(
				"a", "b", "c"
			)
		}

		fun testMethod_returnsLongBlock(): Int {
			Utils.waitNoThrow(8000)
			return 100
		}

		protected fun testMethod_int(x: Int) {
			argLong = x.toLong()
			waitLock.release()
		}

		protected fun testMethod_Int(x: Int) {
			argLong = x.toLong()
			waitLock.release()
		}

		protected fun testMethod_float(f: Float) {
			argFloat = f.toDouble()
			waitLock.release()
		}

		protected fun testMethod_Float(f: Float) {
			argFloat = f.toDouble()
			waitLock.release()
		}

		protected fun testMethod_byte(b: Byte) {
			argLong = b.toLong()
			waitLock.release()
		}

		protected fun testMethod_Byte(b: Byte) {
			argLong = b.toLong()
			waitLock.release()
		}

		protected fun testMethod_long(l: Long) {
			argLong = l
			waitLock.release()
		}

		protected fun testMethod_Long(l: Long) {
			argLong = l
			waitLock.release()
		}

		protected fun testMethod_bool(b: Boolean) {
			argBool = b
			waitLock.release()
		}

		protected fun testMethod_Bool(b: Boolean) {
			argBool = b
			waitLock.release()
		}

		protected fun testMethod_Collection(c: Collection<*>?) {
			argCollection = c
			waitLock.release()
		}

		protected fun testMethod_Map(m: Map<*, *>?) {
			argMap = m
			waitLock.release()
		}

		var argLong: Long = 0
		var argString: String? = null
		var argMap: Map<*, *>? = null
		var argFloat = 0.0
		var argBool = false
		var argCollection: Collection<*>? = null

		init {
			numClients++
			addListener(this)
		}
	}

	companion object {
		const val PORT = 10001
		const val VERSION = "GameServerTest"
		var numClients = 0
	}
}
