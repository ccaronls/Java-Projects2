package cc.lib.net;

import java.io.*;
import java.net.*;
import java.util.*;

import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.Reflector;

/**
 * ClientConnection handles the socket and threads associated with a single client
 * That has passed the handshaking test.
 * 
 * Can only be created by GameServer instances.
 * 
 * @author ccaron
 *
 */
public class ClientConnection extends CommandQueueReader implements Runnable {
    
    private Logger log = LoggerFactory.getLogger(ClientConnection.class);
    
    public interface Listener {
        void onCommand(GameCommand cmd);
        void onDisconnected(String reason);
        void onConnected();
    };

    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private final String name;
    private final GameServer server;
    private CommandQueueWriter outQueue = new CommandQueueWriter();
    private Map<String, Object> attributes = new TreeMap<String, Object>();
    private boolean connected = false;
    private Listener listener = null;
    
    public void setListener(Listener listener) {
        this.listener = listener;
    }
    
    
    public String toString() {
        String r = "ClientConnection name=" + name;
        r += " connected=" + isConnected();
        if (attributes.size() > 0)
            r += " attribs=" + attributes;
        return r;
    }
    
    /**
     * Only GameServer can create instances of ClientConnection
     * @param server
     * @param name
     * @throws Exception
     */
    ClientConnection(GameServer server, String name) throws Exception {
        this.name = name;
        this.server = server;
    }

    /**
     * Send a disconnected message to the client and shutdown the connection. 
     * @param reason
     */
    public final void disconnect(String reason) {
        log.debug("Disconnecting client: " + getName() + " " + reason);
        try {
            if (isConnected()) {
                log.info("ClientConnection: Disconnecting client '" + getName() + "'");
                connected = false;
                synchronized (outQueue) {
                    outQueue.clear();
                    outQueue.add(new GameCommand(GameCommandType.SVR_DISCONNECTED).setMessage(reason));
                }
                synchronized (this) {
                    wait(500);
                }
                outQueue.stop(); // <-- blocks until network flushed
                stop(); // does not block, not completely stopped until onStopped called
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        try {
            synchronized (this) {
                wait(5000);
            }
        } catch (Exception e) {}
        
        close();
    }
    
    private void close() {
        log.debug("ClientConnection: close() ...");
        connected = false;
        outQueue.stop();
        log.debug("ClientConnection: outQueue stopped ...");
        try {
            in.close();
        } catch (Exception ex) {}
        try {
            out.close();
        } catch (Exception ex) {}                
        try {
            socket.close();
        } catch (Exception ex) {}   
        socket = null;
        in = null;
        out = null;
        log.debug("ClientConnection: close() DONE");
    }

    /**
     * 
     * @return
     */
    public final boolean isConnected() {
        return connected;//socket != null;
    }
    
    /**
     * 
     * @param key
     * @param obj
     */
    @Deprecated
    public void setAttribute(String key, Object obj) {
        this.attributes.put(key, obj);
    }
    
    /**
     * 
     * @param key
     * @return
     */
    @Deprecated
    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }
    
    /**
     * 
     * @return
     */
    @Deprecated
    public Iterable<String> getAttributeKeys() {
        return this.attributes.keySet();
    }
    
    /**
     * 
     * @return
     */
    @Deprecated
    public Iterable<Object> getAttributeValues() {
        return this.attributes.values();
    }
    
    /**
     * 
     * @return
     */
    public final String getName() {
        return name;
    }
    
    /**
     * 
     * @return
     */
    public final GameServer getServer() {
        return this.server;
    }

    /*
     * init connection.  should only be used by GameServer
     */
    void connect(Socket socket, InputStream in, OutputStream out) throws Exception {
        if (isConnected()) {
            throw new Exception("Client '" + name + "' is already connected");
        }
        log.debug("ClientConnection: " + getName() + " connection attempt ...");
        this.socket = socket;
        try {
            this.in = new BufferedInputStream(in);//(socket.getInputStream());
            this.out = out;//out = (socket.getOutputStream());
            start();
            
        } catch (Exception e) {
            close();
            throw e;
        }
        log.debug("ClientConnection: " + getName() + " connected SUCCESS");
        if (listener != null)
            listener.onConnected();
    }
    
    /**
     * Sent a command to the remote client
     * @param cmd
     */
    public synchronized final void sendCommand(GameCommand cmd) {
        log.debug("Sending command to client " + getName() + "\n    " + cmd);
        if (!isConnected())
            throw new RuntimeException("Client " + getName() + " is not connected");
        log.debug("ClientConnection: " + getName() + "-> sendCommand: " + cmd);
        outQueue.add(cmd);
    }
    
    /**
     * Send a message to the remote client
     * @param message
     */
    public final void sendMessage(String message) {
        sendCommand(new GameCommand(GameCommandType.SVR_MESSAGE).setMessage(message));
    }
    
    public final void sendForm(ServerForm form) {
        sendCommand(new GameCommand(GameCommandType.SVR_FORM).setArg("xml", form.toXML()).setArg("id", "" + form.getId()));
    }

    public final void executeMethod(String methodName, Object...params) throws IOException {
        GameCommand cmd = new GameCommand(GameCommandType.SVR_EXECUTE_METHOD);
        cmd.setArg("method", methodName);
        cmd.setArg("numParams", params.length);
        int index = 0;
        for (Object o : params) {
            cmd.setArg("param" + index, Reflector.serializeObject(o));
            index++;
        }
        sendCommand(cmd);
    }
    
    /*
     * internal
     */
    public final void start() {
        super.start();
        connected = true;
        outQueue.start(out);
        new Thread(this).start();
    }
    
    public final void run() {
        log.debug("ClientConnection: ClientThread " + Thread.currentThread().getId() + " starting");
        
        while (connected) {
            try {
                queue(GameCommand.parse(in));
            } catch (Exception e) {
                //e.printStackTrace();
                if (connected) {
                    log.error("ClientConnection: Connection with client '" + name + "' dropped: " + e.getClass().getSimpleName() + " " + e.getMessage());
                    //queue(new GameCommand(GameCommandType.CL_DISCONNECT));//server.serverListener.onClientDisconnected(this);
                    if (listener != null) {
                        listener.onDisconnected(e.getClass().getSimpleName() + " " + e.getMessage());
                    }
                    Iterator<GameServer.Listener> it = server.getListeners().iterator();
                    while (it.hasNext()) {
                        try {
                            it.next().onClientDisconnected(this);
                        } catch (Exception ee) {
                            e.printStackTrace();
                            it.remove();
                        }
                    }
                }
                break;
            }
        }
        log.debug("ClientConnection: ClientThread " + Thread.currentThread().getId() + " exiting");
        close();
    }
    
    @Override
    protected void process(GameCommand cmd) {
        if (!connected)
            return;
        if (cmd.getType() == GameCommandType.CL_DISCONNECT) {
            log.info("Client disconnected");
            connected = false;
            for (GameServer.Listener l : server.getListeners()) {
                l.onClientDisconnected(this);
            }
            if (listener != null)
                listener.onDisconnected(cmd.getArg("reason"));
            server.removeClient(this);
        } else if (cmd.getType() == GameCommandType.CL_KEEPALIVE) {
            // client should do this at regular intervals to prevent getting dropped
            log.debug("ClientConnection: KeepAlive from client: " + getName());
        } else if (cmd.getType() == GameCommandType.CL_FORM_SUBMIT) {
            final int id = Integer.parseInt(cmd.getArg("id"));
            final Map<String, String> params = new HashMap<String, String>(cmd.getProperties());
            for (GameServer.Listener l : server.getListeners()) {
                l.onFormSubmited(this, id, params);
            }
        } else if (cmd.getType() == GameCommandType.CL_ERROR) {
            System.err.println("ERROR From client '" + getName() + "'\n" + cmd.getArg("msg") + "\n" + cmd.getArg("stack"));

            //log.debug("Form submitted id(" + )                                
        } else {
            for (GameServer.Listener l : server.getListeners())
                l.onClientCommand(this, cmd);
            if (listener != null) {
                listener.onCommand(cmd);
            }
        }
    }

    @Override
    protected void onStopped() {
        log.debug("onStopped called");
        synchronized (this) {
            notifyAll();
        }
    }

}
