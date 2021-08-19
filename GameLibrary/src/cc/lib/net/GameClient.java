package cc.lib.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.lib.crypt.Cypher;
import cc.lib.crypt.EncryptionInputStream;
import cc.lib.crypt.EncryptionOutputStream;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.GException;
import cc.lib.utils.Reflector;
import cc.lib.utils.WeakHashSet;

/**
 * Base class for clients that want to connect to a GameServer
 * 
 * @author ccaron
 *
 */
public class GameClient {

    protected final static Logger log = LoggerFactory.getLogger("P2PGame", GameClient.class);

    private enum State {
        READY, // connect not called
        CONNECTING, // connect called, handshake in progress
        CONNECTED, // handshake success
        DISCONNECTING, // disconnect called, notify server
        DISCONNECTED // all IO closed and threads stopped
    }

    public interface Listener {
        default void onCommand(GameCommand cmd) {}

        default void onMessage(String msg) {}

        default void onDisconnected(String reason, boolean serverInitiated) {}

        default void onConnected() {}
    }
    
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private State state = State.READY;
    private final String version;
    private final Cypher cypher;
    private final WeakHashSet<Listener> listeners = new WeakHashSet<>();
    private String serverName = null;
    private Map<String, Object> executorObjects = new HashMap<>();
    private String passPhrase = null;
    private String displayName;
    private InetAddress connectAddress;
    private int connectPort;

    // giving package access for JUnit tests ONLY!
    CommandQueueWriter outQueue = new CommandQueueWriter() {

        @Override
        protected void onTimeout() {
            if (isConnected()) {
                try {
                    new GameCommand(GameCommandType.CL_KEEPALIVE).write(out);
                } catch (Exception e) {
                    log.error(e.getClass() + " " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
    };
    
    /**
     * Create a client that will connect to a given server using a given login name.  
     * The userName must be unique to the server for successful connect.
     * 
     * @param displayName
     * @param version
     * @param cypher
     */
    public GameClient(String displayName, String version, Cypher cypher) {
        if (Utils.isEmpty(displayName))
            throw new IllegalArgumentException("Display name cannot be empty");
        this.displayName = displayName;
        this.version = version;
        this.cypher = cypher;
    }

    /**
     * Convenience. Create no-cypher client
     *
     * @param userName
     * @param version
     */
    public GameClient(String userName, String version) {
        this(userName, version, null);
    }

    /**
     * 
     * @return
     */
    public final String getDisplayName() {
        return displayName;
    }

    /**
     *
     * @return
     */
    public final String getServerName() {
        return serverName;
    }

    private boolean isIdle() {
        return state == State.READY || state == State.DISCONNECTED;
    }

    /**
     *
     * @param l
     */
    public final void addListener(Listener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    /**
     *
     * @param l
     */
    public final void removeListener(Listener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }

    /**
     * Spawn a thread and try to connect.  called on success
     * @param address
     * @param port
     * @param connectCallback called with success or failure when connection complete
     */
    public final void connectAsync(InetAddress address, int port, Utils.Callback<Boolean> connectCallback) {
        new Thread(() -> {
            try {
                connectBlocking(address, port);
                if (connectCallback != null)
                    connectCallback.onDone(true);
            } catch (IOException e) {
                e.printStackTrace();
                if (connectCallback != null)
                    connectCallback.onDone(false);
            }
        }).start();
    }

    /**
     * Asynchronous Connect to the server. Listeners.onConnected called when handshake completed.
     * Exception thrown otherwise
     * 
     * @throws IOException 
     * @throws UnknownHostException 
     * @throws Exception
     */
    public final void connectBlocking(InetAddress address, int port) throws IOException {
        log.debug("Connecting ...");
        switch (state) {
            case READY:
            case DISCONNECTED:
                socket = new Socket();
                socket.bind(null);
                socket.connect(new InetSocketAddress(address, port), 30000);
                //socket.setSoTimeout(5000);
                //socket.setKeepAlive(true);
                log.debug("New Socket connect:"
                                +"\n   remote address=%s"
                                +"\n   local address=%s"
                                +"\n   keep alive=%s"
                                +"\n   OOD inline=%s"
                                +"\n   send buf size=%s"
                                +"\n   recv buf size=%s"
                                +"\n   reuse addr=%s"
                                +"\n   tcp nodelay=%s"
                                +"\n   SO timeout=%s"
                                +"\n   SO linger=%s"
                        ,socket.getRemoteSocketAddress()
                        ,socket.getLocalAddress()
                        ,socket.getKeepAlive()
                        ,socket.getOOBInline()
                        ,socket.getSendBufferSize()
                        ,socket.getReceiveBufferSize()
                        ,socket.getReuseAddress()
                        ,socket.getTcpNoDelay()
                        ,socket.getSoTimeout()
                        ,socket.getSoLinger()
                );
                if (cypher != null) {
                    log.debug("Using Cypher: " + cypher);
                    in = new DataInputStream(new EncryptionInputStream(new BufferedInputStream(socket.getInputStream()), cypher));
                    out = new DataOutputStream(new EncryptionOutputStream(new BufferedOutputStream(socket.getOutputStream()), cypher));
                } else {
                    in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                    out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                }
                out.writeLong(87263450972L); // write out the magic number the servers are expecting
                out.flush();
                outQueue.start(out);
                outQueue.add(new GameCommand(GameCommandType.CL_CONNECT).setName(displayName).setVersion(version));
                new Thread(new SocketReader()).start();
                log.debug("Connection SUCCESS");
                connectAddress = address;
                connectPort = port;
                break;
            case CONNECTED:
            case CONNECTING:
                // ignore?
                break;
            default:
                throw new IOException("Cannot connect while in state: " + state);
        }
        
    }

    /**
     *
     */
    public void reconnectAsync() {
        if (state != State.DISCONNECTED) {
            throw new IllegalArgumentException("Cannot call reconnect when not in the DISCONNECTED state");
        }
        connectAsync(connectAddress, connectPort, null);
    }
    
    /**
     * Return true ONLY is socket connected and handshake success
     * @return
     */
    public final boolean isConnected() {
        return state == State.CONNECTED || state == State.CONNECTING;
    }

    public final void disconnect() {
        disconnect("player left session");
    }

    /**
     * Synchronous Disconnect from the server.  If not connected then do nothing.
     * Will NOT call onDisconnected.
     */
    public final void disconnect(String reason) {
        if (state == State.CONNECTED || state == State.CONNECTING) {
            state = State.DISCONNECTING;
            log.debug("GameClient: client '" + this.getDisplayName() + "' disconnecitng ...");
            try {
                outQueue.clear();
                outQueue.add(new GameCommand(GameCommandType.DISCONNECT).setArg("reason", "player left session"));
                synchronized (this) {
                    wait(500); // make sure we give the call a chance to get sent
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            outQueue.stop();
            log.debug("GameClient: outQueue stopped");
            if (listeners.size() > 0) {
                Listener[] arr = listeners.toArray(new Listener[listeners.size()]);
                for (Listener l : arr) {
                    l.onDisconnected(reason, false);
                }
            }
            close();
        }
        reset();
    }
    
    // making this package access so JUnit can test a client timeout
    void close() {
        state = State.DISCONNECTING;
        outQueue.stop();
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
        state = State.DISCONNECTED;
    }
    
    /**
     * Reset this client so that the next call to 'connect' will be a connect and not re-connect.
     * Not valid to be called while connected.
     */
    public final void reset() {
        if (!isIdle()) {
            close();
        }
        state = State.READY;
    }
    
    private final boolean isDisconnecting() {
        return state == State.READY || state == State.DISCONNECTING;
    }

    private class SocketReader implements Runnable {
        public void run() {
            log.debug("GameClient: Client Listener Thread starting");
            
            state = State.CONNECTING;
            String disconnectedReason = null;
            Listener [] larray = new Listener[0];

            while (!isDisconnecting()) {
                try {
                    final GameCommand cmd = GameCommand.parse(in);
                    if (isDisconnecting())
                        break;
                    log.debug("Read command: " + cmd);
                    cmd.getType().notifyListeners(cmd);

                    synchronized (listeners) {
                        larray = listeners.toArray(new Listener[listeners.size()]);
                    }

                    if (cmd.getType() == GameCommandType.SVR_CONNECTED) {
                        serverName = cmd.getName();
                        int keepAliveFreqMS = cmd.getInt("keepAlive");
                        outQueue.setTimeout(keepAliveFreqMS);
                        state = State.CONNECTED;
                        for (Listener l :  larray) {
                            l.onConnected();
                        }

                    } else if (cmd.getType() == GameCommandType.MESSAGE) {
                        for (Listener l :  larray) {
                            l.onMessage(cmd.getMessage());
                        }
                    } else if (cmd.getType() == GameCommandType.DISCONNECT) {
                        disconnectedReason = cmd.getMessage();
                        state = State.DISCONNECTING;
                        break;
                    } else if (cmd.getType() == GameCommandType.SVR_EXECUTE_REMOTE) {
                        handleExecuteRemote(cmd);
                    } else if (cmd.getType() == GameCommandType.PASSWORD) {
                        if (passPhrase != null) {
                            passPhrase = getPasswordFromUser();
                        }
                        outQueue.add(new GameCommand(GameCommandType.PASSWORD).setArg("password", passPhrase));
                        passPhrase = null;
                    } else {
                        for (Listener l : larray)
                            l.onCommand(cmd);
                    }

                    cmd.getType().notifyListeners(cmd);

                } catch (Exception e) {
                    if (!isDisconnecting()) {
                        outQueue.clear();
                        sendError(e);
                        e.printStackTrace();
                        disconnectedReason = ("Exception: " + e.getClass().getSimpleName() + " " + e.getMessage());
                        state = State.DISCONNECTED;
                    }
                    break;
                }
            }
            close();
            if (disconnectedReason != null) {
                for (Listener l : larray)
                    l.onDisconnected(disconnectedReason, true);
            }
            log.debug("GameClient: Client Listener Thread exiting");
        }
    }



    /**
     * Send a command to the server.
     * @param cmd
     */
    public final void sendCommand(GameCommand cmd) {
        if (isConnected()) {
            log.debug("Sending command: " + cmd);
            try {
                this.outQueue.add(cmd);
            } catch (Exception e) {
                log.error("Send Failed: " + e.getClass().getSimpleName() + " " + e.getMessage());
            }
        }
    }

    /**
     *
     * @param e
     */
    public final void sendError(Exception e) {
        GameCommand cmd = new GameCommand(GameCommandType.CL_ERROR).setArg("msg", "ERROR: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
        log.error("Sending error: " + cmd);
        sendCommand(cmd);
    }

    /**
     *
     * @param err
     */
    public final void sendError(String err) {
        GameCommand cmd = new GameCommand(GameCommandType.CL_ERROR).setArg("msg", "ERROR: " + err);
        log.error("Sending error: " + cmd);
        sendCommand(cmd);
    }

    public void setPassphrase(String passphrase) {
        this.passPhrase = passphrase;
    }

    /**
     * Override this method to take input from user for password requests.
     * Default behavior throws a runtime exception, so dont super me.
     *
     * @return
     */
    protected String getPasswordFromUser() {
        throw new GException("Client does not overide the getPasswordFromUser method");
    }

    /**
     *
     * @param displayName
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        sendCommand(new GameCommand(GameCommandType.CL_HANDLE).setName(displayName));
    }

    /**
     * register an object with a specific id
     * @param id
     * @param o
     */
    public final void register(String id, Object o) {
        log.debug("register '%s' -> %s", id, o.getClass());
        executorObjects.put(id, o);
    }

    /**
     * Unregister an object by its id
     * @param id
     */
    public final void unregister(String id) {
        log.debug("unregister %s", id);
        executorObjects.remove(id);
    }

    /*
     * Execute a method locally based on params provided by remote caller.
     *
     * @param cmd
     * @throws IOException
     */
    private void handleExecuteRemote(GameCommand cmd) throws IOException {
        String method = cmd.getString("method");
        int numParams = cmd.getInt("numParams");
        Class [] paramsTypes = new Class[numParams];
        final Object [] params = new Object[numParams];
        for (int i=0; i<numParams; i++) {
            String param = cmd.getString("param" + i);
            Object o = Reflector.deserializeFromString(param);
            if (o != null) {
                paramsTypes[i] = o.getClass();
                params[i] = o;
            }
        }
        String id = cmd.getString("target");
        final Object obj = executorObjects.get(id);
        if (obj == null)
            throw new IOException("Unknown object id: " + id);
        log.debug("id=%s -> %s", id, obj.getClass());
        try {
            final Method m = findMethod(method, obj, paramsTypes, params);
            final String responseId = cmd.getString("responseId");
            if (responseId != null) {
                log.debug("responseId=%s waiting for result...", responseId);
                Object result = m.invoke(obj, params);
                log.debug("responseId=%s cancelled=" + cancelled + " result=" + result);
                GameCommand resp = new GameCommand(GameCommandType.CL_REMOTE_RETURNS);
                resp.setArg("target", responseId);
                if (result != null)
                    resp.setArg("returns", Reflector.serializeObject(result));
                else
                    resp.setArg("cancelled", cancelled);
                cancelled = false;
                sendCommand(resp);
            } else {
                m.invoke(obj, params);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }

    private boolean cancelled = false;

    public void cancelRemote() {
        cancelled = true;
    }

    private Method findMethod(String method, Object obj, Class [] paramsTypes, Object [] params) throws Exception {
        Method m = methodMap.get(method);
        if (m == null) {
            try {
                m = obj.getClass().getDeclaredMethod(method, paramsTypes);
            } catch (NoSuchMethodException e) {
                // ignore
            }
            if (m == null)
                m = searchMethods(obj, method, paramsTypes, params);
            m.setAccessible(true);
            methodMap.put(method, m);
        }
        return m;
    }

    private final Map<String, Method> methodMap = new HashMap<>();

    public static Method searchMethods(Object execObj, String method, Class [] types, Object [] params) throws Exception {
        Class clazz = execObj.getClass();
        while (clazz!= null && !clazz.equals(Object.class)) {
            for (Method m : clazz.getDeclaredMethods()) {
                m.setAccessible(true);
                if (!m.getName().equals(method))
                    continue;
                log.debug("testMethod:" + m.getName() + " with params:" + Arrays.toString(m.getParameterTypes()));
                Class[] paramTypes = m.getParameterTypes();
                if (paramTypes.length != types.length)
                    continue;
                boolean matchFound = true;
                for (int i = 0; i < paramTypes.length; i++) {
                    if (!isCompatiblePrimitives(paramTypes[i], types[i]) && !Reflector.isSubclassOf(types[i], paramTypes[i])) {
                        matchFound = false;
                        break;
                    }
                }
                if (matchFound)
                    return m;
            }
            clazz = clazz.getSuperclass();
        }
        throw new Exception("Failed to match method '" + method + "' types: " + Arrays.toString(types));
    }

    private final static Map<Class, List> primitiveCompatibilityMap = new HashMap<>();

    static {
        List c;
        primitiveCompatibilityMap.put(boolean.class, c= Arrays.asList(boolean.class, Boolean.class));
        primitiveCompatibilityMap.put(Boolean.class, c);
        primitiveCompatibilityMap.put(byte.class, c=Arrays.asList(byte.class, Byte.class));
        primitiveCompatibilityMap.put(Byte.class, c);
        primitiveCompatibilityMap.put(int.class, c=Arrays.asList(int.class, Integer.class, byte.class, Byte.class));
        primitiveCompatibilityMap.put(Integer.class, c);
        primitiveCompatibilityMap.put(long.class, c=Arrays.asList(int.class, Integer.class, byte.class, Byte.class, long.class, Long.class));
        primitiveCompatibilityMap.put(long.class, c);
        primitiveCompatibilityMap.put(float.class, c=Arrays.asList(int.class, Integer.class, byte.class, Byte.class, float.class, Float.class));
        primitiveCompatibilityMap.put(Float.class, c);
        primitiveCompatibilityMap.put(double.class, c=Arrays.asList(int.class, Integer.class, byte.class, Byte.class, float.class, Float.class, double.class, Double.class, long.class, Long.class));
        primitiveCompatibilityMap.put(Double.class, c);

    }

    private static boolean isCompatiblePrimitives(Class a, Class b) {
        if (primitiveCompatibilityMap.containsKey(a))
            return b ==null || primitiveCompatibilityMap.get(a).contains(b);
        return false;
    }

}
