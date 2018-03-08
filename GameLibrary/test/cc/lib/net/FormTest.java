package cc.lib.net;

import java.util.Arrays;
import java.util.Map;

import cc.lib.crypt.SimpleCypher;
import cc.lib.net.GameServerTest.MyGameClient;
import junit.framework.TestCase;

public class FormTest extends TestCase {

    final static String HOST = "localhost";
    final static int PORT = 10000;
    final static String VERSION = "GameServerTest";
    final static int TIMEOUT = 2000;
    
    MyServerListener listener1;
    GameServer server;
    
    @Override
    protected void setUp() throws Exception {
        // TODO Auto-generated method stub
        System.out.println("---------------------------------------\n"
                         + "Start Test: " + getName() + "\n"
                         + "---------------------------------------\n");

        listener1 = new MyServerListener();
        server = new GameServer(listener1, PORT, TIMEOUT, VERSION, null, 2);

    }

    @Override
    protected void tearDown() throws Exception {
        // TODO Auto-generated method stub
        if (server != null)
            server.stop();
    }
    
 // test basic client connect and disconnect
    public void testForm() throws Exception {
        
        final String xml = 
                "<SOCForm>\n" +
                    "<Container type=\"list\" orientation=\"vertical\">\n" +
                        "<Button id=\"A\" type=\"toggle\" text=\"option A\"/>\n" +
                        "<Button id=\"B\" type=\"toggle\" text=\"option B\"/>\n" +
                        "<Button id=\"C\" type=\"choice\" text=\"select\">\n"+
                            "<ButtonOption text=\"one\"/>\n" +
                            "<ButtonOption text=\"two\"/>\n" +
                        "</Button>\n" +
                        "<Container type=\"list\" orientation=\"horz\">\n"+
                            "<Label text=\"Name\"/>\n" +
                            "<TextInput id=\"D\" minChars=\"4\" maxChars=\"32\"/>\n" +
                        "</Container>\n" +
                        "<Button id=\"E\" type=\"submit\" text=\"submit\"/>\n" +
                    "</Container>\n" +
                "</SOCForm>\n";
        
        new Thread(new Runnable() {
            public void run() {
                try {
                    
                    final MyGameClient cl = new MyGameClient("X");
                    cl.connect(HOST, PORT);
                    Thread.sleep(1000);
                    assertTrue(listener1.connected);
                    assertTrue(cl.connected);
                    synchronized (FormTest.this) {
                        FormTest.this.wait();
                    }
                    Thread.sleep(1000);
                    cl.disconnect();
                    Thread.sleep(1000);
                    assertNotNull(cl.form);
                    
                    assertTrue(listener1.disconnected);
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                synchronized (server) {
                    server.notify();
                }
            }
        }).start();
        
        
        
        ServerForm form = new ServerForm(FORM_ID);
        
        makeForm(form);
        
        
        synchronized (server) {
            server.wait(1000);
            server.getClientConnection("X").sendForm(form);
            server.wait();
        }
        server.stop();
        assertTrue(listener1.formSubmitted);
    }
    
    final int FORM_ID = 100;
    
    class MyServerListener implements GameServer.Listener {
        
        boolean connected = false;
        boolean reconnected = false;
        boolean disconnected = false;
        boolean formSubmitted = false;
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
            assertEquals(id, FORM_ID);
            formSubmitted = true;
            
            synchronized (FormTest.this) {
                FormTest.this.notify();
            }
        }
    };    
    
    static int numClients = 0;
    
    class X {
        
        X(String s) {
            this.s = s;
        }
        
        final String s;
        
        private X left, right, next;
        
        X add(X child) {
            if (left == null) {
                left = right = child;
            } else {
                right.next = child;
                right = child;
            }
            return child;
        }
        
        public String toString() {
            StringBuffer buf = new StringBuffer();
            toString_r(buf, "");
            return buf.toString();
        }
        
        private void toString_r(StringBuffer buf, String indent) {
            buf.append(indent).append(s).append("\n");
            for (X x = left; x != null; x = x.next) {
                x.toString_r(buf, indent + "  ");
            }
        }
    };
    
    class MyGameClient extends GameClient { //implements ClientForm.Visitor<X>{

        boolean connected = false;
        boolean disconnected = false;
        ClientForm form;
        GameCommand lastCommand;
        X root;
      
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
            try {
                this.form = form;
                super.logDebug("Form received: " + form.toXML());
                logDebug("Root: " + form.getRootElem().toString());
                //root = form.getRootElem();
                //root = null;
                //form.visit(this);
                //assertNotNull(root);
                //System.out.println(root);
                
                FormElem elem = form.getElem("button1");
                assertNotNull(elem);
                assertEquals(elem.getType(), FormElem.Type.CHOICEBUTTON);
                elem.getChildren().get(1).doAction(this, null);
                form.getElem("toggle1").doAction(this, null);
                form.getElem("toggle2").doAction(this, null);
                form.getElem("textinput1").doAction(this, "new text");
                form.getElem("submit1").doAction(this, null);
            } finally {            
                
            }
        }        
    }
    
    void makeForm(ServerForm form) {
        form.addListContainer(false);
        form.addLabel("Test Form");
        form.addChoiceButton("button1", "Button1", Arrays.asList(new String [] {
                "A", "B", "C"
        }));
        form.addListContainer(true);
         form.addToggleButton("toggle1", "Toggle1", true);
         form.addToggleButton("toggle2", "Toggle2", false);
        form.endContainer();
        form.addTextInput("textinput1", "default", 0, 32);
        form.addSubmitButton("submit1", "Submit");
       form.endContainer();        
    }
}
