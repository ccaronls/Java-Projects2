package cc.lib.net;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import cc.lib.crypt.Cypher;
import cc.lib.crypt.SimpleCypher;
import cc.lib.game.GColor;
import cc.lib.game.Utils;

/**
 * Test the fundamental connect/disconnect/reconnect functionality to make sure all the
 * appropriate client/server callbacks are made. 
 * @author ccaron
 *
 */
public class GameServerTest extends TestCase {

    final static int PORT = 10001;
    final static String VERSION = "GameServerTest";
    final static int TIMEOUT = 5000;

    Throwable result = null;

    final static GameCommandType TEST = new GameCommandType("TEST");

    MyServerListener listener1;
    MyClientConnectionListener listener2;
    GameServer server;
    
    @Override
    protected void setUp() throws Exception {
        // TODO Auto-generated method stub
        System.out.println("---------------------------------------\n"
                         + "Start Test: " + getName() + "\n"
                         + "---------------------------------------\n");

        cypher = null;
        if (getName().indexOf("Encrypt") > 0) {
            cypher = SimpleCypher.generateCypher(0);
        } 

        result = null;
        listener1 = new MyServerListener();
        listener2 = new MyClientConnectionListener();
        server = new GameServer("Test", PORT, TIMEOUT, VERSION, cypher, 2);
        server.addListener(listener1);
        server.listen();
        Utils.waitNoThrow(this, 100);

    }
    
    Cypher cypher;
    Cypher getCypher() {
        return this.cypher;
    }

    @Override
    protected void tearDown() throws Exception {
        // TODO Auto-generated method stub
        if (server != null)
            server.stop();
        if (result != null) {
            result.printStackTrace();
            fail(result.getMessage());
        }
    }

    public void testGameCommand() throws  Exception {

        GameCommandType t = new GameCommandType("A");
        GameCommand c = new GameCommand(t);

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
        assertEquals(c.getInt("int"), 1);
        assertEquals(c.getFloat("float"), 2f);
        assertEquals(c.getLong("long"), 3L);
        assertEquals(c.getDouble("double"), 4.0);
        assertEquals(c.parseReflector("color", new GColor()), GColor.RED);

    }


    // test basic client connect and disconnect
    public void testclientConnect() throws Exception {
        
        
        new Thread(new Runnable() {
            public void run() {
                try {

                    Thread.sleep(1000);
                    MyGameClient cl = new MyGameClient();
                    cl.connect(InetAddress.getLocalHost(), PORT);
                    Thread.sleep(1000);
                    assertTrue(listener1.connected);
                    assertTrue(cl.connected);
                    cl.disconnect();
                    Thread.sleep(1000);
                    assertTrue(listener1.disconnected);

                } catch (Throwable e) {
                    result = e;
                } finally {
                    synchronized (server) {
                        server.notify();
                    }
                }
            }
        }).start();
        
        synchronized (server) {
            server.wait();
        }
    }

    public void testExecuteRemoteMethod() throws Exception {
        new Thread(new Runnable() {
            public void run() {
                try {

                    Thread.sleep(1000);
                    MyGameClient cl = new MyGameClient("XX");
                    cl.connect(InetAddress.getLocalHost(), PORT);
                    Thread.sleep(1000);
                    assertTrue(listener1.connected);
                    assertTrue(cl.connected);
                    ClientConnection conn = server.getClientConnection("XX");
                    /////////////////

                    Object waitLock = new Object();

                    conn.executeMethodOnRemote(CLIENT_ID, false, "testMethod_int", new Integer(10));
                    Thread.sleep(1000);
                    assertEquals(cl.argLong, 10);
                    /////////////////
                    conn.executeMethodOnRemote(CLIENT_ID, false, "testMethod_Int", 11);
                    Thread.sleep(1000);
                    assertEquals(cl.argLong, 11);
                    /////////////////
                    conn.executeMethodOnRemote(CLIENT_ID, false, "testMethod_float", new Float(12.0f));
                    Thread.sleep(1000);
                    assertEquals(cl.argFloat, 12.0);
                    /////////////////
                    conn.executeMethodOnRemote(CLIENT_ID, false, "testMethod_Float", 13.0f);
                    Thread.sleep(1000);
                    assertEquals(cl.argFloat, 13.0);
                    /////////////////
                    Map map = new HashMap();
                    map.put("hello", 1);
                    map.put("goodbyte", 2);
                    conn.executeMethodOnRemote(CLIENT_ID,false, "testMethod_Map", map);
                    Thread.sleep(1000);
                    assertTrue(Utils.isEquals(cl.argMap, map));

                    Collection c = new LinkedList();
                    c.add("a");
                    c.add("b");
                    conn.executeMethodOnRemote(CLIENT_ID,false, "testMethod_Collection", c);
                    Thread.sleep(1000);
                    assertTrue(Utils.isEquals(cl.argCollection, c));

                    String [] result = conn.executeMethodOnRemote(CLIENT_ID,true, "testMethod", 15, "Hello", new HashMap<>());
                    Thread.sleep(1000);
                    assertEquals(15, cl.argLong);
                    assertEquals("Hello", cl.argString);
                    assertNotNull(cl.argMap);
                    assertNotNull(result);
                    assertTrue(Arrays.equals(result, new String[] { "a", "b", "c" }));
                    cl.disconnect();
                    Thread.sleep(1000);
                    assertTrue(listener1.disconnected);

                } catch (Throwable e) {
                    result = e;
                } finally {
                    synchronized (server) {
                        server.notify();
                    }
                }
            }
        }).start();

        synchronized (server) {
            server.wait();
        }
    }

    // test basic client connect and disconnect
    public void testRejectBadVersionClient() throws Exception {
        
        
        new Thread(new Runnable() {
            public void run() {
                try {
                    
                    Thread.sleep(1000);
                    MyGameClient cl = new MyGameClient("A", "BadVersion");
                    cl.connect(InetAddress.getLocalHost(), PORT);
                    Thread.sleep(1000);
                    assertFalse(listener1.connected);
                    assertFalse(cl.connected);
                    assertFalse(cl.isConnected());
                    //cl.disconnect();
                    //Thread.sleep(1000);
                    //assertTrue(listener1.disconnected);
                    
                } catch (Throwable e) {
                    result = e;
                } finally {
                    synchronized (server) {
                        server.notify();
                    }
                }
            }
        }).start();
        
        synchronized (server) {
            server.wait();
        }
    }
    
    // test basic client connect and disconnect
    public void testRejectDuplicateClient() throws Exception {
        
        
        new Thread(new Runnable() {
            public void run() {
                try {
                    
                    Thread.sleep(1000);
                    MyGameClient cl = new MyGameClient("A", VERSION);
                    cl.connect(InetAddress.getLocalHost(), PORT);
                    Thread.sleep(1000);
                    assertTrue(cl.isConnected());
                    MyGameClient cl2 = new MyGameClient("A", VERSION);
                    cl2.connect(InetAddress.getLocalHost(), PORT);
                    Thread.sleep(1000);
                    assertFalse(cl2.isConnected());
                    //cl.disconnect();
                    //Thread.sleep(1000);
                    //assertTrue(listener1.disconnected);
                    
                } catch (Throwable e) {
                    result = e;
                } finally {
                    synchronized (server) {
                        server.notify();
                    }
                }
            }
        }).start();
        
        synchronized (server) {
            server.wait();
        }
    }
    
    
    // test client connect->disconnect->reconnect
    public void testclientReconnect() throws Exception {
        
        
        new Thread(new Runnable() {
            public void run() {
                try {
                    
                    Thread.sleep(1000);
                    GameClient cl = new MyGameClient();
                    cl.connect(InetAddress.getLocalHost(), PORT);
                    Thread.sleep(1000);
                    assertTrue(listener1.connected);
                    cl.disconnect();
                    Thread.sleep(1000);
                    assertTrue(listener1.disconnected);
                    cl.connect(InetAddress.getLocalHost(), PORT);
                    Thread.sleep(1000);
                    assertTrue(listener1.reconnected);
                    cl.disconnect();
                    
                } catch (Throwable e) {
                    result = e;
                } finally {
                    synchronized (server) {
                        server.notify();
                    }
                }
            }
        }).start();
        
        synchronized (server) {
            server.wait();
        }
    }
    
    // test client connect and server disconnect
    public void testServerDisconnect() throws Exception {
        
        
        new Thread(new Runnable() {
            public void run() {
                try {
                    
                    Thread.sleep(1000);
                    MyGameClient cl = new MyGameClient("XxX");
                    cl.connect(InetAddress.getLocalHost(), PORT);
                    Thread.sleep(1000);
                    assertTrue(listener1.connected);
                    assertTrue(cl.connected);
                    //server.stop();
                    ClientConnection conn = server.getClientConnection("XxX");
                    assertNotNull(conn);
                    conn.disconnect("Sumthing");
                    Thread.sleep(1000);
                    assertTrue(cl.disconnected);
                    assertFalse(listener1.disconnected);
                    assertTrue(!cl.isConnected());
                    
                } catch (Throwable e) {
                    result = e;
                } finally {
                    synchronized (server) {
                        server.notify();
                    }
                }
            }
        }).start();
        
        synchronized (server) {
            server.wait();
        }
    }
    
    // test client connect and server disconnect by shutdown
    public void testServerShutdown() throws Exception {
        
        
        new Thread(new Runnable() {
            public void run() {
                try {
                    
                    Thread.sleep(1000);
                    MyGameClient cl = new MyGameClient();
                    cl.connect(InetAddress.getLocalHost(), PORT);
                    Thread.sleep(1000);
                    assertTrue(listener1.connected);
                    server.stop();
                    Thread.sleep(1000);
                    assertTrue(cl.disconnected);
                    assertTrue(!cl.isConnected());
                    
                } catch (Throwable e) {
                    result = e;
                } finally {
                    synchronized (server) {
                        server.notify();
                    }
                }
            }
        }).start();
        
        synchronized (server) {
            server.wait();
        }
    }
    
 // test client connect and server disconnect by shutdown
    public void testMessage() throws Exception {
        
        
        new Thread(new Runnable() {
            public void run() {
                try {
                    
                    Thread.sleep(1000);
                    MyGameClient cl = new MyGameClient("ABC");
                    cl.connect(InetAddress.getLocalHost(), PORT);
                    Thread.sleep(1000);
                    ClientConnection conn = server.getClientConnection("ABC");
                    conn.addListener(listener2);
                    assertTrue(listener1.connected);
                    assertTrue(cl.connected);
                    assertTrue(cl.isConnected());
                    cl.sendCommand(new GameCommand(TEST));
                    Thread.sleep(1000);
                    assertNotNull(listener2.lastCommand);
                    assertEquals(listener2.lastCommand.getType(), TEST);
                    assertNotNull(conn);
                    conn.sendCommand(new GameCommand(TEST));
                    Thread.sleep(1000);
                    assertNotNull(cl.lastCommand);
                    assertEquals(cl.lastCommand.getType(), TEST);
                    
                    cl.disconnect();
                    Thread.sleep(1000);
                    assertTrue(!cl.isConnected());
                    assertFalse(conn.isConnected());
                    
                } catch (Throwable e) {
                    result = e;
                } finally {
                    synchronized (server) {
                        server.notify();
                    }
                }
            }
        }).start();
        
        synchronized (server) {
            server.wait();
        }
    }
    
    // test client connect and server disconnect by shutdown
    public void testEncryption() throws Exception {
        
        //final GameServer server = new GameServer(listener1, PORT, TIMEOUT, VERSION, getCypher());
        new Thread(new Runnable() {
            public void run() {
                try {
                    
                    Thread.sleep(1000);
                    MyGameClient cl = new MyGameClient("ABC");
                    cl.connect(InetAddress.getLocalHost(), PORT);
                    Thread.sleep(1000);
                    ClientConnection conn = server.getClientConnection("ABC");
                    conn.addListener(listener2);
                    assertTrue(listener1.connected);
                    assertTrue(cl.connected);
                    assertTrue(cl.isConnected());
                    assertTrue(conn.isConnected());
                    cl.sendCommand(new GameCommand(TEST));
                    Thread.sleep(3000);
                    assertNotNull(listener2.lastCommand);
                    assertEquals(listener2.lastCommand.getType(), TEST);
                    assertNotNull(conn);
                    conn.sendCommand(new GameCommand(TEST));
                    Thread.sleep(2000);
                    assertNotNull(cl.lastCommand);
                    assertEquals(cl.lastCommand.getType(), TEST);
                    
                    cl.disconnect();
                    Thread.sleep(1000);
                    assertTrue(!cl.isConnected());
                    assertFalse(conn.isConnected());
                    
                } catch (Throwable e) {
                    result = e;
                } finally {
                    synchronized (server) {
                        server.notify();
                    }
                }
            }
        }).start();
        
        synchronized (server) {
            server.wait();
        }
    }
    
    
    public void testclientHardDisconnect() throws Exception {
        
        
        new Thread(new Runnable() {
            public void run() {
                try {
                    
                    Thread.sleep(1000);
                    MyGameClient cl = new MyGameClient();
                    cl.connect(InetAddress.getLocalHost(), PORT);
                    Thread.sleep(1000);
                    assertTrue(listener1.connected);
                    cl.close();//.disconnect();
                    Thread.sleep(1000 + TIMEOUT + 5000);
                    assertTrue(listener1.disconnected);
                    
                } catch (Throwable e) {
                    result = e;
                } finally {
                    synchronized (server) {
                        server.notify();
                    }
                }
            }
        }).start();
        
        synchronized (server) {
            server.wait();
        }
    }
    
    public void testclientTimeout() throws Exception {
        
        
        new Thread(new Runnable() {
            public void run() {
                try {
                    
                    Thread.sleep(1000);
                    MyGameClient cl = new MyGameClient();
                    cl.connect(InetAddress.getLocalHost(), PORT);
                    Thread.sleep(1000);
                    assertTrue(listener1.connected);
                    //cl.close();//.disconnect();
                    cl.outQueue.setTimeout(100000);
                    Thread.sleep(1000 + TIMEOUT + 5000);
                    assertTrue(listener1.disconnected);
                    assertTrue(cl.disconnected);
                    assertFalse(cl.isConnected());
                    
                } catch (Throwable e) {
                    result = e;
                } finally {
                    synchronized (server) {
                        server.notify();
                    }
                }

            }
        }).start();
        
        synchronized (server) {
            server.wait();
        }
    }
    

    class MyServerListener implements GameServer.Listener {
        
        boolean connected = false;
        boolean reconnected = false;
        boolean disconnected = false;

        @Override
        public void onReconnection(ClientConnection conn) {
            System.out.println("onReconnection: " + conn);
            this.connected = false;
            this.reconnected = false;
            this.disconnected = false;
            this.reconnected = true;
        }
        
        @Override
        public void onConnected(ClientConnection conn) {
            System.out.println("onConnected: " + conn);
            this.connected = false;
            this.reconnected = false;
            this.disconnected = false;
            this.connected = true;
        }
        
        @Override
        public void onClientDisconnected(ClientConnection conn) {
            System.out.println("onclientDisconnected: " + conn);
            this.connected = false;
            this.reconnected = false;
            this.disconnected = false;
            this.disconnected = true;
        }
        
    };

    class MyClientConnectionListener implements ClientConnection.Listener {
        GameCommand lastCommand;

        @Override
        public void onCommand(ClientConnection c, GameCommand cmd) {
            lastCommand=cmd;
        }

        @Override
        public void onDisconnected(ClientConnection c, String reason) {

        }

        @Override
        public void onConnected(ClientConnection c) {

        }

        @Override
        public void onCancelled(ClientConnection c, String id) {

        }
    }
    
    
    static int numClients = 0;

    final String CLIENT_ID = "CLIENT";
    
    class MyGameClient extends GameClient implements GameClient.Listener {

        boolean connected = false;
        boolean disconnected = false;
        GameCommand lastCommand;

        @Override
        public void onCommand(GameCommand cmd) {
            log.debug("client " + getName() + " onCommand: " + cmd);
            lastCommand = cmd;
        }

        @Override
        public void onMessage(String message) {
            log.debug("client " + this.getName() + " onMessage: " + message);
        }

        @Override
        public void onDisconnected(String reason) {
            connected = false;
            disconnected = true;
            unregister(CLIENT_ID);
        }

        @Override
        public void onConnected() {
            connected = true;
            disconnected = false;
            register(CLIENT_ID, this);
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

        public String [] testMethod(int arg0, String arg1, Map arg2) {
            System.out.println("testMethod executed with: " + arg0 + " " + arg1 + " " + arg2);
            this.argLong = arg0;
            this.argString = arg1;
            this.argMap = arg2;
            return new String[] {
                    "a", "b", "c"
            };
        }

        protected void testMethod_int(int x) {
            argLong = x;
        }

        protected void testMethod_Int(Integer x) {
            argLong = x;
        }

        protected void testMethod_float(float f) {
            argFloat =f;
        }

        protected void testMethod_Float(Float f) {
            argFloat = f;
        }

        protected void testMethod_byte(byte b) {
            argLong =b;
        }

        protected void testMethod_Byte(Byte b) {
            argLong =b;
        }

        protected void testMethod_long(long l) {
            argLong = l;
        }

        protected void testMethod_Long(Long l) {
            argLong = l;
        }

        protected void testMethod_bool(boolean b) {
            argBool = b;
        }

        protected void testMethod_Bool(Boolean b) {
            argBool = b;
        }

        protected void testMethod_Collection(Collection c) {
            argCollection = c;
        }

        protected void testMethod_Map(Map m) {
            argMap = m;
        }

        long argLong;
        String argString;
        Map argMap;
        double argFloat;
        boolean argBool;
        Collection argCollection;

    }
}
