package cc.lib.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.GException;
import cc.lib.utils.Reflector;
import cc.lib.utils.WeakHashSet;

/**
 * Created by chriscaron on 3/12/18.
 *
 * ClientConnection handles the socket and threads associated with a single client
 * That has passed the handshaking test.
 * 
 * Can only be created by GameServer instances.
 * 
 * Execute methods remotely and wait for the return value if it has one. This should have effect of
 * making the socket connection appear transparent the the caller.
 *
 * TODO: Add retries?
 *
 * Be sure to not obfuscate those methods involved in this scheme so different versions
 * of application can remain compatible.
 *
 * Example:
 *
 * you have some object that exists on 2 systems connected by I/O stream.
 *
 * MyObjectType myObject;
 *
 * the client and server endpoints both derive from ARemoteExecutor
 *
 * On system A:
 * server = new ARemoteExecutor() { ... }
 *
 * On system B:
 * client = new ARemoteExecutor() {... }
 *
 * System register objects to be executed upon
 *
 * client.register(myObject.getClass().getSimpleName(), myObject);
 *
 * ...
 *
 * @Keep
 * class MyObjectType {
 *
 *     // make sure to prevent obfuscation
 *     @Keep
 *     public Integer add(int a, int b) {
 *         try {
 *             // executeOnRemote will determine method name and class from Exception stack
 *             // and bundle everything up to the client then wait for a response and return
 *             // it once it arrives
 *             return server.executeOnRemote(true, a, b);
 *         } catch (IOException e) {
 *             ...
 *         }
 *         return null; // good practice to return non-primitives
 *     }
 * }
 *
 */
public class ClientConnection implements Runnable {
    
    private Logger log = LoggerFactory.getLogger(ClientConnection.class);

    private CommandQueueReader reader = new CommandQueueReader() {
        @Override
        protected void process(GameCommand cmd) throws IOException {
            processCommand(cmd);
        }

        @Override
        protected void onStopped() {
            log.debug("onStopped called");
            synchronized (ClientConnection.this) {
                ClientConnection.this.notifyAll();
            }
        }
    };
    
    public interface Listener {
        default void onCommand(ClientConnection c, GameCommand cmd) {}
        default void onDisconnected(ClientConnection c, String reason) {}
        default void onConnected(ClientConnection c) {}
        default void onCancelled(ClientConnection c, String id) {}
    };

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final String name;
    private String handle; // dynamic display name. takes precedence over name when non-null
    private final GameServer server;
    private CommandQueueWriter outQueue = new CommandQueueWriter();
    private Map<String, Object> attributes = new TreeMap<>();
    private boolean connected = false;
    private boolean kicked = false;
    private boolean disconnecting = false;
    private final Set<Listener> listeners = new WeakHashSet<>();
    
    public final void addListener(Listener listener) {
        listeners.add(listener);
    }

    public final void removeListener(Listener l) {
        listeners.remove(l);
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

    public void kick() {
        kicked = true;
        disconnect("Kicked");
    }

    public boolean isKicked() {
        return kicked;
    }

    public void unkick() {
        kicked = false;
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
                disconnecting = true;
                synchronized (outQueue) {
                    outQueue.clear();
                    outQueue.add(GameCommandType.SVR_DISCONNECT.make().setMessage(reason));
                }
                outQueue.stop(); // <-- blocks until network flushed
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void close() {
        log.debug("ClientConnection: close() ...");
        server.removeClient(this);
        connected = false;
        disconnecting = false;
        outQueue.stop();
        reader.stop();
        log.debug("ClientConnection: outQueue stopped ...");
        // close output stream first to make sure it is flushed
        // https://stackoverflow.com/questions/19307011/does-close-a-socket-will-also-close-flush-the-input-output-stream
        try {
            out.close();
        } catch (Exception ex) {}
        try {
            in.close();
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
        return connected;
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
     * Return the user's handle if set. normal name otherwise.
     * @return
     */
    public final String getDisplayName() {
        if (!Utils.isEmpty(handle))
            return handle;
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
    void connect(Socket socket, DataInputStream in, DataOutputStream out) throws Exception {
        if (isConnected()) {
            throw new Exception("Client '" + name + "' is already connected");
        }
        log.debug("ClientConnection: " + getName() + " connection attempt ...");
        this.socket = socket;
        try {
            this.in = in;
            this.out = out;
            start();
            
        } catch (Exception e) {
            close();
            throw e;
        }
        log.debug("ClientConnection: " + getName() + " connected SUCCESS");
        Listener [] arr;
        synchronized (listeners) {
            arr = listeners.toArray(new Listener[listeners.size()]);
        }
        for (Listener l : arr) {
            l.onConnected(this);
        }
    }
    
    /**
     * Sent a command to the remote client
     * @param cmd
     */
    public final void sendCommand(GameCommand cmd) {
        log.debug("Sending command to client " + getName() + "\n    " + cmd);
        if (!isConnected())
            throw new GException("Client " + getName() + " is not connected");
        //log.debug("ClientConnection: " + getName() + "-> sendCommand: " + cmd);
        synchronized (outQueue) {
            if (!disconnecting) {
                outQueue.add(cmd);
            }
        }
    }

    /**
     * Send a message to the remote client
     * @param message
     */
    public final void sendMessage(String message) {
        sendCommand(GameCommandType.MESSAGE.make().setMessage(message));
    }

    /**
     * internal
     */
    public final void start() {
        reader.start();
        connected = true;
        outQueue.start(out);
        new Thread(this).start();
    }
    
    public final void run() {
        log.debug("ClientConnection: ClientThread " + Thread.currentThread().getId() + " starting");
        
        while (isConnected()) {
            try {
                reader.queue(GameCommand.parse(in));
            } catch (Exception e) {
                if (isConnected()) {
                    e.printStackTrace();
                    log.error("ClientConnection: Connection with client '" + name + "' dropped: " + e.getClass().getSimpleName() + " " + e.getMessage());
                }
                if (listeners.size() > 0) {
                    Listener [] arr = listeners.toArray(new Listener[listeners.size()]);
                    for (Listener l : arr) {
                        l.onDisconnected(this, e.getClass().getSimpleName() + " " + e.getMessage());
                    }
                }
                break;
            }
        }

        Iterator<GameServer.Listener> it = server.getListeners().iterator();
        while (it.hasNext()) {
            try {
                it.next().onClientDisconnected(this);
            } catch (Exception ee) {
                ee.printStackTrace();
                it.remove();
            }
        }

        log.debug("ClientConnection: ClientThread " + Thread.currentThread().getId() + " exiting");
        close();
    }
    
    protected boolean processCommand(GameCommand cmd) {
        cmd.getType().notifyListeners(cmd);
        if (!connected)
            return false;
        if (cmd.getType() == GameCommandType.CL_DISCONNECT) {
            String reason = cmd.getMessage();
            log.info("Client disconnected: " + reason);
            connected = false;
            if (listeners.size() > 0) {
                Listener[] arr = listeners.toArray(new Listener[listeners.size()]);
                for (Listener l : arr) {
                    l.onDisconnected(this, reason);
                }
            }
            close();
        } else if (cmd.getType() == GameCommandType.CL_KEEPALIVE) {
            // client should do this at regular intervals to prevent getting dropped
            log.debug("ClientConnection: KeepAlive from client: " + getName());
        } else if (cmd.getType() == GameCommandType.CL_ERROR) {
            System.err.println("ERROR From client '" + getName() + "'\n" + cmd.getString("msg") + "\n" + cmd.getString("stack"));
        } else if (cmd.getType() == GameCommandType.CL_HANDLE) {
            this.handle = cmd.getName();
            for (GameServer.Listener l : server.getListeners()) {
                l.onClientHandleChanged(this, handle);
            }
        } else if (!disconnecting && cmd.getType() != GameCommandType.CL_REMOTE_RETURNS) {
            for (GameServer.Listener l : server.getListeners()) {
                l.onCommand(this, cmd);
            }

            for (Listener l : listeners) {
                l.onCommand(this, cmd);
            }
        } else {
            log.error("!!! UNHANDLED COMMAND %s !!!", cmd.getType());
        }
        return true;
    }

    private final class ResponseListener<T> implements GameCommandType.Listener, ClientConnection.Listener {
        private T response;
        private final Object waitLock;
        private final String id;
        private boolean cancelled;

        ResponseListener(String id, Object waitLock) {
            this.id = id;
            this.waitLock = waitLock;
        }

        public void setResponse(T response) {
            this.response = response;
            synchronized (waitLock) {
                waitLock.notify();
            }
        }

        @Override
        public void onCommand(GameCommand cmd) {
            if (id.equals(cmd.getString("target"))) {
                try {
                    if (cmd.getBoolean("cancelled", false)) {
                        cancelled = true;
                        setResponse(null);
                    }
                    else {
                        T resp = Reflector.deserializeFromString(cmd.getString("returns"));
                        setResponse(resp);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    setResponse(null);
                }
            }
        }

        @Override
        public void onDisconnected(ClientConnection c, String reason) {
            synchronized (waitLock) {
                waitLock.notify();
            }
        }
    };

    /**
     *
     * @param objId
     * @param params
     * @param <T>
     * @return
     */
    public final <T> T executeDerivedOnRemote(String objId, boolean returnsResult, Object ... params) {
        if (!isConnected()) {
            log.warn("Not Connected");
            return null;
        }
        StackTraceElement elem = new Exception().getStackTrace()[1];
        return executeMethodOnRemote(objId, returnsResult, elem.getMethodName(), params);
    }

    /**
     * Send command to client and block until response or client disconnects
     *
     * @param targetId
     * @param method
     * @param params
     * @param <T>
     * @return
     */
    public final <T> T executeMethodOnRemote(String targetId, boolean returnsResult, String method, Object ... params) { // <-- need [] array to disambiguate from above method
        log.debug("executueMethodOnRemote: %s(%s, %s)", method, targetId, Arrays.toString(params));
        if (!isConnected()) {
            log.warn("Not Connected");
            return null;
        }
        if (Utils.isEmpty(targetId) || Utils.isEmpty(method))
            throw new NullPointerException();

        GameCommand cmd = new GameCommand(GameCommandType.SVR_EXECUTE_REMOTE);
        cmd.setArg("method", method);
        cmd.setArg("target", targetId);
        cmd.setArg("numParams", params.length);
        try {
            for (int i = 0; i < params.length; i++) {
                cmd.setArg("param" + i, Reflector.serializeObject(params[i]));
            }
            if (returnsResult) {
                Object lock = new Object();
                String id = method + "_" + targetId + "_" + Utils.genRandomString(32);
                cmd.setArg("responseId", id);
                ResponseListener<T> listener = new ResponseListener<>(id, lock);
                addListener(listener);
                GameCommandType.CL_REMOTE_RETURNS.addListener(listener);
                sendCommand(cmd);
                log.debug("Waiting for response");
                try {
                    synchronized (lock) {
                        lock.wait();
                    }
                } finally {
                    GameCommandType.CL_REMOTE_RETURNS.removeListener(listener);
                }
                log.debug("Response: %s", listener.response);
                removeListener(listener);
                if (listener.cancelled)
                    onCancelled(id);
                return listener.response;
            } else {
                sendCommand(cmd);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void onCancelled(String id) {
        // TODO: Dont think this is neccessary since WeakSet
        for (Iterator<Listener> it = listeners.iterator(); it.hasNext() ;) {
            Listener l = it.next();
            if (l == null) {
                it.remove();
            } else {
                l.onCancelled(this, id);
            }
        }
    }

}
