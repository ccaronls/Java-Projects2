package cc.lib.net;

import java.net.InetAddress;
import java.util.Map;

import cc.lib.crypt.Cypher;
import cc.lib.crypt.SimpleCypher;
import cc.lib.game.Utils;

import junit.framework.TestCase;

/**
 * Test the fundamental connect/disconnect/reconnect functionality to make sure all the
 * appropriate client/server callbacks are made. 
 * @author ccaron
 *
 */
public class GameServerTest extends TestCase {

    final static int PORT = 10000;
    final static String VERSION = "GameServerTest";
    final static int TIMEOUT = 2000;

    final static GameCommandType TEST = new GameCommandType("TEST");

    MyServerListener listener1;
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
        
        listener1 = new MyServerListener();
        server = new GameServer(listener1, PORT, TIMEOUT, VERSION, cypher, 2);
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
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                synchronized (server) {
                    server.notify();
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
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                synchronized (server) {
                    server.notify();
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
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                synchronized (server) {
                    server.notify();
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
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                synchronized (server) {
                    server.notify();
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
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                synchronized (server) {
                    server.notify();
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
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                synchronized (server) {
                    server.notify();
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
                    assertTrue(listener1.connected);
                    assertTrue(cl.connected);
                    assertTrue(cl.isConnected());
                    cl.send(new GameCommand(TEST));
                    Thread.sleep(1000);
                    assertNotNull(listener1.lastCommand);
                    assertEquals(listener1.lastCommand.getType(), TEST);
                    ClientConnection conn = server.getClientConnection("ABC");
                    assertNotNull(conn);
                    conn.sendCommand(new GameCommand(TEST));
                    Thread.sleep(1000);
                    assertNotNull(cl.lastCommand);
                    assertEquals(cl.lastCommand.getType(), TEST);
                    
                    cl.disconnect();
                    Thread.sleep(1000);
                    assertTrue(!cl.isConnected());
                    assertFalse(conn.isConnected());
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                synchronized (server) {
                    server.notify();
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
                    cl.setCypher(getCypher());
                    cl.connect(InetAddress.getLocalHost(), PORT);
                    Thread.sleep(1000);
                    assertTrue(listener1.connected);
                    assertTrue(cl.connected);
                    assertTrue(cl.isConnected());
                    cl.send(new GameCommand(TEST));
                    Thread.sleep(1000);
                    assertNotNull(listener1.lastCommand);
                    assertEquals(listener1.lastCommand.getType(), TEST);
                    ClientConnection conn = server.getClientConnection("ABC");
                    assertNotNull(conn);
                    conn.sendCommand(new GameCommand(TEST));
                    Thread.sleep(1000);
                    assertNotNull(cl.lastCommand);
                    assertEquals(cl.lastCommand.getType(), TEST);
                    
                    cl.disconnect();
                    Thread.sleep(1000);
                    assertTrue(!cl.isConnected());
                    assertFalse(conn.isConnected());
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                synchronized (server) {
                    server.notify();
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
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                synchronized (server) {
                    server.notify();
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
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                synchronized (server) {
                    server.notify();
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
        GameCommand lastCommand;
        
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
        
        @Override
        public void onClientCommand(ClientConnection conn, GameCommand command) {
            System.out.println("onClientCommand: " + conn + " -> " + command);
            this.lastCommand = command;
        }

        @Override
        public void onFormSubmited(ClientConnection conn, int id, Map<String, String> params) {
            System.out.println("onFormSubmitted: " + conn + " -> id=" + id + " params=" + params);
        }
    };
    
    
    static int numClients = 0;
    
    class MyGameClient extends GameClient {

        boolean connected = false;
        boolean disconnected = false;
        GameCommand lastCommand;
      
        public MyGameClient(String name) {
            super(name, VERSION);
        }
        
        public MyGameClient(String name, String version) {
            super(name, version);
        }
        
        public MyGameClient() {
            super("test" + numClients, VERSION);
            numClients++;
        }

        @Override
        protected void onMessage(String message) {
            logDebug("client " + this.getName() + " onMessage: " + message);
        }

        @Override
        protected void onDisconnected(String message) {
            logDebug("client " + this.getName() +  " onDisconnected: " + message);
            disconnected = true;
        }

        @Override
        protected void onConnected() {
            logDebug("client " + getName() + " onConnected");
            connected = true;
        }

        @Override
        protected void onCommand(GameCommand cmd) {
            logDebug("client " + getName() + " onCommand: " + cmd);
            lastCommand = cmd;
        }

        @Override
        public void logDebug(String msg) {
            // TODO Auto-generated method stub
            super.logDebug("----> MyGameClient:  " + msg);
        }

        @Override
        public void logInfo(String msg) {
            // TODO Auto-generated method stub
            super.logInfo("----> MyGameClient:  " + msg);
        }

        @Override
        public void logError(String msg) {
            // TODO Auto-generated method stub
            super.logError("----> MyGameClient:  " + msg);
        }

        @Override
        public void logError(Exception e) {
            // TODO Auto-generated method stub
            super.logError("----> MyGameClient:  " + e.getClass().getSimpleName() + " " + e.getMessage());
        }

        @Override
        protected void onForm(ClientForm form) {
            super.logDebug("Form receieved: " + form.toXML());
        }
        
        
    }
}
