package cc.lib.net;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cc.lib.crypt.Cypher;
import cc.lib.crypt.HuffmanEncoding;
import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.utils.Lock;

/**
 * Test the fundamental connect/disconnect/reconnect functionality to make sure all the
 * appropriate client/server callbacks are made. 
 * @author ccaron
 *
 */
public class GameServerTest extends TestCase {

    final static int PORT = 10001;
    final static String VERSION = "GameServerTest";

    Throwable result = null;
    Lock waitLock = new Lock();
    Object testMonitor = new Object();

    MyConnectionListener listener1;
    GameServer server;
    MyGameClient client;

    private void blockOrFail() {
        waitLock.block(3000, () -> fail("timeout"));
    }

    @Override
    protected void setUp() throws Exception {
        // TODO Auto-generated method stub
        System.out.println("---------------------------------------\n"
                + "SETUP Test: " + getName() + "\n"
                + "---------------------------------------\n");

        cypher = null;
        if (getName().indexOf("Encrypt") > 0) {
            cypher = new HuffmanEncoding("aslkjhajsd");
        }

        waitLock.reset();
        result = null;
        listener1 = new MyConnectionListener();
        server = new GameServer("Test", PORT, VERSION, cypher, 2);
        server.addListener(new GameServer.Listener() {
            @Override
            public void onConnected(AClientConnection conn) {
                System.out.println("New Client connected: " + conn.getDisplayName());
                conn.addListener(listener1);
                waitLock.release();
            }

            @Override
            public void onServerStopped() {
                System.out.println("Server Stopped");
                waitLock.release();
            }
        });

        server.listen();
        client = new MyGameClient("A", VERSION);
        Utils.waitNoThrow(this, 100);

    }
    
    Cypher cypher;
    Cypher getCypher() {
        return this.cypher;
    }

    @Override
    protected void tearDown() throws Exception {
        // TODO Auto-generated method stub
        System.out.println("---------------------------------------\n"
                + "TEARDOWN Test: " + getName() + "\n"
                + "---------------------------------------\n");
        client.close();
        if (server != null)
            server.stop();
        if (result != null) {
            result.printStackTrace();
            System.err.println("FAILED: " + result.getMessage());
            fail(result.getMessage());
        }
    }

    interface RunnableThrowing {
        void run() throws Exception;
    }

    void runTest(RunnableThrowing test) throws Exception {
        new Thread(() -> {
            try {

                System.out.println("---------------------------------------\n"
                        + "RUNNING Test: " + getName() + "\n"
                        + "---------------------------------------\n");
                test.run();
                System.out.println("---------------------------------------\n"
                        + "RUN TEST COMPLETE Test: " + getName() + "\n"
                        + "---------------------------------------\n");

                waitLock.acquire();
                server.stop();
                blockOrFail();

            } catch (Throwable e) {
                System.out.println("---------------------------------------\n"
                        + "TEST ERROR Test: " + getName() + "\n" + e.getClass().getSimpleName() + ":" + e.getMessage()
                        + "---------------------------------------\n");

                result = e;
            } finally {
                synchronized (testMonitor) {
                    testMonitor.notify();
                }
            }
        }).start();

        synchronized (testMonitor) {
            testMonitor.wait();
        }

        Utils.waitNoThrow(1000);
    }

    public void testGameCommand() throws Exception {

        GameCommandType t = new GameCommandType("A");
        GameCommand c = new GameCommand(t);

        c.setArg("bool", true);
        c.setArg("int", 1);
        c.setArg("float", 2f);
        c.setArg("long", 3L);
        c.setArg("double", 4.0);
        c.setArg("string", "This is a string");
        c.setArg("color", GColor.RED);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(bout);
        c.write(dout);

        DataInputStream din = new DataInputStream(new ByteArrayInputStream(bout.toByteArray()));

        c = GameCommand.parse(din);

        assertEquals(c.getType(), t);
        assertEquals(c.getBoolean("bool"), true);
        assertEquals(c.getInt("int"), 1);
        assertEquals(c.getFloat("float"), 2f);
        assertEquals(c.getLong("long"), 3L);
        assertEquals(c.getDouble("double"), 4.0);
        assertEquals(c.getReflector("color", new GColor()), GColor.RED);

    }

    public void testKick() throws Exception {
        runTest(() -> {
            assertTrue(server.isRunning());
            waitLock.acquire(2);
            client.connectBlocking(InetAddress.getLocalHost(), PORT);
            blockOrFail();
            assertTrue(listener1.connected);
            assertTrue(client.connected);

            AClientConnection conn = server.getClientConnection(client.getDeviceName());
            assertNotNull(conn);
            waitLock.acquire(2);
            conn.kick();
            blockOrFail();
            assertFalse(listener1.connected);
            assertFalse(client.connected);

            waitLock.acquire();
            client.reconnectAsync();
            blockOrFail();
            assertFalse(listener1.connected);
            assertFalse(client.connected);

            conn.unkick();
            waitLock.acquire(3);
            client.reconnectAsync();
            blockOrFail();
            assertTrue(listener1.connected);
            assertTrue(listener1.reconnected);
            assertTrue(client.connected);

        });
    }

    // test basic client connect and disconnect
    public void testClientDisconnect() throws Exception {

        runTest(() -> {
            assertTrue(server.isRunning());
            waitLock.acquire(2);
            client.connectBlocking(InetAddress.getLocalHost(), PORT);
            blockOrFail();
            assertTrue(listener1.connected);
            assertTrue(client.connected);
            waitLock.acquire(2);
            client.disconnect();
            blockOrFail();
            assertTrue(listener1.disconnected);
            assertFalse(client.isConnected());
            assertEquals(0, server.getNumConnectedClients());
        });
    }

    public void testExecuteRemoteMethod() throws Exception {
        runTest(() -> {

            waitLock.acquire(2);
            client.connectBlocking(InetAddress.getLocalHost(), PORT);
            blockOrFail();
            assertTrue(listener1.connected);
            assertTrue(client.connected);
            AClientConnection conn = server.getClientConnection(client.getDeviceName());
            assertNotNull(conn);
            /////////////////

            waitLock.acquire();
            conn.executeMethodOnRemote(CLIENT_ID, false, "testMethod_int", new Integer(10));
            blockOrFail();
            assertEquals(client.argLong, 10);
            /////////////////
            waitLock.acquire();
            conn.executeMethodOnRemote(CLIENT_ID, false, "testMethod_Int", 11);
            blockOrFail();
            assertEquals(client.argLong, 11);
            /////////////////
            waitLock.acquire();
            conn.executeMethodOnRemote(CLIENT_ID, false, "testMethod_float", new Float(12.0f));
            blockOrFail();
            assertEquals(client.argFloat, 12.0);
            /////////////////
            waitLock.acquire();
            conn.executeMethodOnRemote(CLIENT_ID, false, "testMethod_Float", 13.0f);
            blockOrFail();
            assertEquals(client.argFloat, 13.0);
            /////////////////
            Map map = new HashMap();
            map.put("hello", 1);
            map.put("goodbyte", 2);
            waitLock.acquire();
            conn.executeMethodOnRemote(CLIENT_ID, false, "testMethod_Map", map);
            blockOrFail();
            assertTrue(Utils.isEquals(client.argMap, map));

            Collection c = new LinkedList();
            c.add("a");
            c.add("b");
            waitLock.acquire();
            conn.executeMethodOnRemote(CLIENT_ID, false, "testMethod_Collection", c);
            blockOrFail();
            assertTrue(Utils.isEquals(client.argCollection, c));

            String[] result = conn.executeMethodOnRemote(CLIENT_ID, true, "testMethod_returns", 15, "Hello", new HashMap<>());
//                    blockOrFail();
            assertEquals(15, client.argLong);
            assertEquals("Hello", client.argString);
            assertNotNull(client.argMap);
            assertNotNull(result);
            assertTrue(Arrays.equals(result, new String[]{"a", "b", "c"}));
            waitLock.acquire(2);
            client.disconnect();
            blockOrFail();
            assertTrue(listener1.disconnected);
        });
    }

    // test basic client connect and disconnect
    public void testRejectBadVersionClient() throws Exception {
        runTest(() -> {

            waitLock.acquire(1);
            client = new MyGameClient("A", "BadVersion");
            client.connectBlocking(InetAddress.getLocalHost(), PORT);
            blockOrFail();
            assertFalse(client.connected);
            assertFalse(client.isConnected());
            assertEquals(0, server.getNumClients());
        });
    }
    
    // test basic client connect and disconnect
    public void testRejectDuplicateClient() throws Exception {
        runTest(() -> {

            MyGameClient cl1 = new MyGameClient("A", VERSION);
            waitLock.acquire(2);
            cl1.connectBlocking(InetAddress.getLocalHost(), PORT);
            blockOrFail();
            assertTrue(cl1.isConnected());
            MyGameClient cl2 = new MyGameClient("A", VERSION);
            waitLock.acquire(1);
            cl2.connectBlocking(InetAddress.getLocalHost(), PORT);
            blockOrFail();
            assertFalse(cl2.isConnected());
        });
    }


    // test client connect->disconnect->reconnect
    public void testClientReconnect() throws Exception {
        runTest(() -> {

            waitLock.acquire(2);
            client.connectBlocking(InetAddress.getLocalHost(), PORT);
            blockOrFail();
            assertTrue(listener1.connected);
            waitLock.acquire();
            client.disconnect();
            blockOrFail();
            assertTrue(listener1.disconnected);
            waitLock.acquire(2);
            client.connectBlocking(InetAddress.getLocalHost(), PORT);
            blockOrFail();
            assertTrue(listener1.reconnected);
            waitLock.acquire(2);
            client.disconnect();
            blockOrFail();
        });
    }
    
    // test client connect and server disconnect
    public void testServerDisconnect() throws Exception {
        runTest(() -> {
            MyGameClient cl = new MyGameClient("XxX");
            waitLock.acquire(2);
            cl.connectBlocking(InetAddress.getLocalHost(), PORT);
            blockOrFail();
            assertTrue(listener1.connected);
            assertTrue(cl.connected);
            //server.stop();
            AClientConnection conn = server.getClientConnection("XxX");
            assertNotNull(conn);
            waitLock.acquire(2);
            conn.disconnect("Sumthing");
            blockOrFail();
            assertTrue(cl.disconnected);
            assertTrue(listener1.disconnected);
            assertFalse(cl.isConnected());
        });
    }
    
    // test client connect and server disconnect by shutdown
    public void testServerShutdown() throws Exception {
        runTest(() -> {

            MyGameClient cl = new MyGameClient();
            waitLock.acquire(2);
            client.connectBlocking(InetAddress.getLocalHost(), PORT);
            blockOrFail();
            assertTrue(listener1.connected);
            server.stop();
            blockOrFail();
            assertTrue(client.disconnected);
            assertTrue(!client.isConnected());
        });
    }

    // test client connect and server disconnect by shutdown
    public void testMessage() throws Exception {
        runTest(() -> {
            client = new MyGameClient("ABC");
            waitLock.acquire(2);
            client.connectBlocking(InetAddress.getLocalHost(), PORT);
            blockOrFail();
            assertTrue(client.isConnected());

            AClientConnection conn = server.getClientConnection(client.getDeviceName());
            assertNotNull(conn);

            waitLock.acquire(20);

            new Thread(() -> {
                for (int i = 0; i < 10; i++) {
                    conn.sendMessage(String.valueOf(i));
                    Utils.waitNoThrow(Utils.rand() % 100);
                }
            }).start();

            new Thread(() -> {
                for (int i = 0; i < 10; i++) {
                    client.sendMessage(String.valueOf(i));
                    Utils.waitNoThrow(Utils.rand() % 100);
                }

            }).start();

            waitLock.block(10000, () -> System.out.println("Timeout"));

            assertEquals(10, client.messages.size());
            assertEquals(10, listener1.messages.size());

            System.out.println("MESSAGES:\n    " + client.messages + "\n   " + listener1.messages);

        });
    }

    // test client connect and server disconnect by shutdown
    public void testMessage2() throws Exception {
        runTest(() -> {
            client = new MyGameClient("ABC");
            waitLock.acquire(2);
            client.connectBlocking(InetAddress.getLocalHost(), PORT);
            blockOrFail();

            AClientConnection conn = server.getClientConnection("ABC");
            conn.addListener(listener1);
            assertTrue(listener1.connected);
            assertTrue(client.connected);
            assertTrue(client.isConnected());

            final GameCommandType TEST = new GameCommandType("TEST");

            waitLock.acquire(2);
            client.sendCommand(new GameCommand(TEST));
            blockOrFail();
            assertNotNull(listener1.lastCommand);
            assertEquals(listener1.lastCommand.getType(), TEST);
            assertNotNull(conn);

            waitLock.acquire(2);
            conn.sendCommand(new GameCommand(TEST));
            blockOrFail();
            assertNotNull(client.lastCommand);
            assertEquals(client.lastCommand.getType(), TEST);

            waitLock.acquire();
            client.disconnect();
            blockOrFail();
            assertTrue(!client.isConnected());
            assertFalse(conn.isConnected());
        });
    }

    // test client connect and server disconnect by shutdown
    public void x_testEncryption() throws Exception {

        runTest(() -> {

            final GameCommandType TEST = new GameCommandType("TEST2");
            client = new MyGameClient("ABC");
            waitLock.acquire();
            client.connectBlocking(InetAddress.getLocalHost(), PORT);
            blockOrFail();
            AClientConnection conn = server.getClientConnection("ABC");
            conn.addListener(listener1);
            assertTrue(listener1.connected);
            assertTrue(client.connected);
            assertTrue(client.isConnected());
            assertTrue(conn.isConnected());
            waitLock.acquire();
            client.sendCommand(new GameCommand(TEST));
            blockOrFail();
            assertNotNull(listener1.lastCommand);
            assertEquals(listener1.lastCommand.getType(), TEST);
            assertNotNull(conn);
            waitLock.acquire();
            conn.sendCommand(new GameCommand(TEST));
            blockOrFail();
            assertNotNull(client.lastCommand);
            assertEquals(client.lastCommand.getType(), TEST);
            waitLock.acquire();
            client.disconnect();
            blockOrFail();
            assertTrue(!client.isConnected());
            assertFalse(conn.isConnected());
        });
    }


    public void testClientHardDisconnect() throws Exception {
        runTest(() -> {

            waitLock.acquire(2);
            client.connectBlocking(InetAddress.getLocalHost(), PORT);
            blockOrFail();
            assertTrue(listener1.connected);
            assertTrue(client.isConnected());
            assertEquals(1, server.getNumConnectedClients());
            waitLock.acquire();
            client.close();//.disconnect();
            blockOrFail();
            assertTrue(listener1.disconnected);
            assertFalse(client.isConnected());
            assertEquals(0, server.getNumConnectedClients());
        });
    }

    public void testExecuteOnRemote() throws Exception {
        runTest(() -> {

            server.PING_FREQ = 2000;

            waitLock.acquire(2);
            client.connectBlocking(InetAddress.getLocalHost(), PORT);
            blockOrFail();
            assertTrue(listener1.connected);
            assertTrue(client.isConnected());

            AClientConnection conn = server.getClientConnection(client.getDeviceName());
            Integer result = conn.executeMethodOnRemote(CLIENT_ID, true, "testMethod_returnsLongBlock");

            assertNotNull(result);
            assertEquals(new Integer(100), result);

        });
    }

    public void testClientTimeout() throws Exception {
        runTest(() -> {

            server.TIMEOUT = 3000;

            // test that a client that has not sent some message within in the timeout is disconnected
            waitLock.acquire(2);
            client.connectBlocking(InetAddress.getLocalHost(), PORT);
            blockOrFail();
            assertTrue(listener1.connected);
            //client.close();//.disconnect();
            waitLock.acquire(2);
            waitLock.block(50000, () -> fail("timeout"));
            assertFalse(listener1.connected);
            assertFalse(client.connected);
            assertFalse(client.isConnected());
        });
    }


    class MyConnectionListener implements AClientConnection.Listener {

        boolean connected = true;
        boolean reconnected = false;
        boolean disconnected = false;
        GameCommand lastCommand;
        List<String> messages = new ArrayList<>();

        @Override
        public void onReconnected(AClientConnection conn) {
            System.out.println(conn.getDisplayName() + ":onReconnection: " + conn);
            this.connected = true;
            this.reconnected = true;
            this.disconnected = false;
            waitLock.release();
        }

        @Override
        public void onDisconnected(AClientConnection conn, String reason) {
            System.out.println(conn.getDisplayName() + ":onClientDisconnected: " + conn + " reason: " + reason);
            this.connected = false;
            this.reconnected = false;
            this.disconnected = true;
            waitLock.release();
        }

        @Override
        public void onCommand(AClientConnection conn, GameCommand cmd) {
            System.out.println(conn.getDisplayName() + ":onCommand: " + conn + ": " + cmd);
            lastCommand = cmd;
            if (cmd.getType() == GameCommandType.MESSAGE)
                messages.add(cmd.getMessage());
            waitLock.release();
        }

        @Override
        public void onTimeout(AClientConnection conn) {
            System.out.println(conn.getDisplayName() + ":onTimeout");
            waitLock.release();
        }
    }


    static int numClients = 0;

    final String CLIENT_ID = "CLIENT";
    
    class MyGameClient extends GameClient implements GameClient.Listener {

        boolean connected = false;
        boolean disconnected = false;
        GameCommand lastCommand;
        List<String> messages = new ArrayList<>();

        @Override
        public void onCommand(GameCommand cmd) {
            System.out.println(getDisplayName() + " onCommand: " + cmd);
            lastCommand = cmd;
            waitLock.release();
        }

        @Override
        public void onMessage(String message) {
            System.out.println(getDisplayName() + " onMessage: " + message);
            messages.add(message);
            waitLock.release();
        }

        @Override
        public void onDisconnected(String reason, boolean serverInitiated) {
            System.out.println(getDisplayName() + " onDisconnected: " + reason + " serverIntiiated: " + serverInitiated);
            connected = false;
            disconnected = true;
            unregister(CLIENT_ID);
            waitLock.release();
        }

        @Override
        public void onConnected() {
            System.out.println(getDisplayName() + " onConnected");
            connected = true;
            disconnected = false;
            register(CLIENT_ID, this);
            waitLock.release();
        }

        public MyGameClient(String name) {
            this(name, VERSION);
        }

        public MyGameClient(String name, String version) {
            super(name, version, getCypher());
            numClients++;
            addListener(this);
        }

        public MyGameClient() {
            this("test" + numClients, VERSION);
        }

        public String[] testMethod_returns(int arg0, String arg1, Map arg2) {
            System.out.println(getName() + ":testMethod_returns executed with: " + arg0 + " " + arg1 + " " + arg2);
            this.argLong = arg0;
            this.argString = arg1;
            this.argMap = arg2;
            return new String[]{
                    "a", "b", "c"
            };
        }

        public Integer testMethod_returnsLongBlock() {
            Utils.waitNoThrow(8000);
            return 100;
        }

        protected void testMethod_int(int x) {
            argLong = x;
            waitLock.release();
        }

        protected void testMethod_Int(Integer x) {
            argLong = x;
            waitLock.release();
        }

        protected void testMethod_float(float f) {
            argFloat = f;
            waitLock.release();
        }

        protected void testMethod_Float(Float f) {
            argFloat = f;
            waitLock.release();
        }

        protected void testMethod_byte(byte b) {
            argLong = b;
            waitLock.release();
        }

        protected void testMethod_Byte(Byte b) {
            argLong = b;
            waitLock.release();
        }

        protected void testMethod_long(long l) {
            argLong = l;
            waitLock.release();
        }

        protected void testMethod_Long(Long l) {
            argLong = l;
            waitLock.release();
        }

        protected void testMethod_bool(boolean b) {
            argBool = b;
            waitLock.release();
        }

        protected void testMethod_Bool(Boolean b) {
            argBool = b;
            waitLock.release();
        }

        protected void testMethod_Collection(Collection c) {
            argCollection = c;
            waitLock.release();
        }

        protected void testMethod_Map(Map m) {
            argMap = m;
            waitLock.release();
        }

        long argLong;
        String argString;
        Map argMap;
        double argFloat;
        boolean argBool;
        Collection argCollection;

    }
}
