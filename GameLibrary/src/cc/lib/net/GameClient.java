package cc.lib.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.lib.crypt.Cypher;
import cc.lib.crypt.EncryptionInputStream;
import cc.lib.crypt.EncryptionOutputStream;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.Reflector;

/**
 * Base class for clients that want to connect to a GameServer
 * 
 * @author ccaron
 *
 */
public abstract class GameClient {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private enum State {
        READY, // connect not called
        CONNECTING, // connect called, handshake in progress
        CONNECTED, // handshake success
        DISCONNECTING, // disconnect called, notify server
        DISCONNECTED // all IO closed and threads stopped
    }

    public interface Listener {
        void onCommand(GameCommand cmd);

        void onMessage(String msg);
    }
    
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private final String userName;
    private State state = State.READY;
    private final String version;
    private Cypher cypher;
    private final Set<Listener> listeners = new HashSet<>();
    
    // giving package access for JUnit tests ONLY!
    CommandQueueWriter outQueue = new CommandQueueWriter() {

        @Override
        protected void onTimeout() {
            try {
                new GameCommand(GameCommandType.CL_KEEPALIVE).write(out);
            } catch (Exception e) {
                log.error(e.getClass() + " " + e.getMessage());
                e.printStackTrace();
            }
        }
        
    };
    
    /**
     * Create a client that will connect to a given server using a given login name.  
     * The userName must be unique to the server for successful connect.
     * 
     * @param userName
     * @param version
     * @param cypher
     */
    public GameClient(String userName, String version, Cypher cypher) {
        this.userName = userName;
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
     * Attach a cypher if we are connecting to a secure server
     *
     * @param cypher
     */
    public final void setCypher(Cypher cypher) {
        if (!isIdle())
            throw new RuntimeException("Cannot enable encryption while connected");
        this.cypher = cypher;
    }
    
    /**
     * 
     * @return
     */
    public final String getName() {
        return userName;
    }
    
    private boolean isIdle() {
        return state == State.READY || state == State.DISCONNECTED;
    }

    /**
     *
     * @param host
     * @param port
     * @throws IOException
     */
    public final void connect(String host, int port) throws IOException {
        connect(InetAddress.getByName(host), port);
    }

    public final void addListener(Listener l) {
        listeners.add(l);
    }

    public final void removeListener(Listener l) {
        listeners.remove(l);
    }

    /**
     * Asynchronous Connect to the server.  onConnected called when handshake completed.  
     * 
     * @throws IOException 
     * @throws UnknownHostException 
     * @throws Exception
     */
    public final void connect(InetAddress address, int port) throws IOException {
        log.debug("Connecting ...");
        switch (state) {
            case READY:
            case DISCONNECTED:
                socket = new Socket(address, port);
                if (cypher != null) {
                    log.debug("Using Cypher: " + cypher);
                    in = new EncryptionInputStream(socket.getInputStream(), cypher);
                    out = new EncryptionOutputStream(socket.getOutputStream(), cypher);
                } else {
                    in = socket.getInputStream();
                    out = socket.getOutputStream();
                }
                outQueue.start(out);
                GameCommandType type = state == State.READY ? GameCommandType.CL_CONNECT : GameCommandType.CL_RECONNECT;
                outQueue.add(new GameCommand(type).setName(userName).setVersion(version));
                new Thread(new SocketReader()).start();
                log.debug("Connection SUCCESS");
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
     * Return true ONLY is socket connected and handshake success
     * @return
     */
    public final boolean isConnected() {
        return state == State.CONNECTED || state == State.CONNECTING;
    }

    /**
     * Synchronous Disconnect from the server.  If not connected then do nothing.
     * Will NOT call onDisconnected.   
     */
    public final void disconnect() {
        if (state == State.CONNECTED || state == State.CONNECTING) {
            state = State.DISCONNECTING;
            log.debug("GameClient: client '" + this.getName() + "' disconnecitng ...");
            try {
                outQueue.clear();
                outQueue.add(new GameCommand(GameCommandType.CL_DISCONNECT).setArg("reason", "player left session"));
                synchronized (this) {
                    wait(500); // make sure we give the call a chance to get sent
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            outQueue.stop();
            log.debug("GameClient: outQueue stopped");
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
        //return state == State.DISCONNECTING;
    }

    private class SocketReader implements Runnable {
        public void run() {
            log.debug("GameClient: Client Listener Thread starting");
            
            state = State.CONNECTING;
            String disconnectedReason = null;
            
            while (!isDisconnecting()) {
                try {
                    final GameCommand cmd = GameCommand.parse(in);
                    if (isDisconnecting())
                        break;
                    log.debug("Parsed incoming cmd: " + cmd);
                    if (cmd.getType() == GameCommandType.SVR_CONNECTED) {
                        int keepAliveFreqMS = Integer.parseInt(cmd.getArg("keepAlive"));
                        outQueue.setTimeout(keepAliveFreqMS);
                        state = State.CONNECTED;
                        onConnected();        
                        
                    } else if (cmd.getType() == GameCommandType.SVR_MESSAGE) {
                        onMessage(cmd.getMessage());
                        Iterator<Listener> it = listeners.iterator();
                        while (it.hasNext()) {
                            try {
                                it.next().onMessage(cmd.getMessage());
                            } catch (Exception e) {
                                e.printStackTrace();
                                it.remove();
                            }
                        }
                    } else if (cmd.getType() == GameCommandType.SVR_DISCONNECTED) {
                        disconnectedReason = cmd.getMessage();
                        state = State.DISCONNECTING;
                        break;
                    } else if (cmd.getType() == GameCommandType.SVR_FORM) {
                        ClientForm clForm = new ClientForm(Integer.parseInt(cmd.getArg("id")), cmd.getArg("xml"));
                        onForm(clForm);
                    } else if (cmd.getType() == GameCommandType.SVR_EXECUTE_METHOD) {
                        doExecuteCommand(cmd);
                    } else {
                        onCommand(cmd);
                        Iterator<Listener> it = listeners.iterator();
                        while (it.hasNext()) {
                            try {
                                it.next().onCommand(cmd);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                
                } catch (Exception e) {
                    if (!isDisconnecting()) {
                        sendError(e);
//                        send(new GameCommand(GameCommandType.CL_ERROR).setArg("msg", "ERROR: " + e.getClass().getSimpleName() + ":" + e.getMessage())
//                                .setArg("stack", Arrays.toString(e.getStackTrace())));
//                        log.error(e);
                        e.printStackTrace();
                        disconnectedReason = ("Exception: " + e.getClass().getSimpleName() + " " + e.getMessage());
                    }
                    break;
                }
            }
            close();
            if (disconnectedReason != null)
                onDisconnected(disconnectedReason);
            log.debug("GameClient: Client Listener Thread exiting");
        }
    }

    private void doExecuteCommand(GameCommand cmd) throws IOException {
        String method = cmd.getArg("method");
        int numParams = cmd.getInt("numParams");
        Class [] paramsTypes = new Class[numParams];
        Object [] params = new Object[numParams];
        for (int i=0; i<numParams; i++) {
            String param = cmd.getArg("param" + i);
            Object o = Reflector.deserializeFromString(param);
            paramsTypes[i] = o.getClass();
            params[i] = o;
        }
        Object obj = getExecuteObject();
        try {
            Method m = methodMap.get(method);
            if (m == null) {
                m = obj.getClass().getDeclaredMethod(method, paramsTypes);
                methodMap.put(method, m);
            }
            m.invoke(obj, params);
        } catch (Exception e) {
            methodMap.remove(method);
            // search methods to see if there is a compatible match
            try {
                searchMethods(obj, method, paramsTypes, params);

            } catch (Exception ee) {
                throw new IOException(ee);
            }
        }
    }

    private final Map<String, Method> methodMap = new HashMap<>();

    private void searchMethods(Object execObj, String method, Class [] types, Object [] params) throws Exception {
        for (Method m : execObj.getClass().getDeclaredMethods()) {
            if (!m.getName().equals(method))
                continue;
            Class [] paramTypes = m.getParameterTypes();
            if (paramTypes.length != types.length)
                continue;
            boolean match = true;
            for (int i=0; i<paramTypes.length; i++) {
                if (!isCompatiblePrimitives(paramTypes[i], types[i]) && !Reflector.isSubclassOf(types[i], paramTypes[i])) {
                    match = false;
                    break;
                }
            }
            if (match) {
                m.invoke(execObj, params);
                methodMap.put(method, m);
                return;
            }
        }
        throw new Exception("Failed to match method '" + method + "'");
    }

    private final static Map<Class, List> primitiveCompatibilityMap = new HashMap<>();

    static {
        List c;
        primitiveCompatibilityMap.put(boolean.class, c=Arrays.asList(boolean.class, Boolean.class));
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

    private boolean isCompatiblePrimitives(Class a, Class b) {
        if (primitiveCompatibilityMap.containsKey(a))
            return primitiveCompatibilityMap.get(a).contains(b);
        return false;
    }

    /**
     * Send a command to the server.
     * @param cmd
     */
    public final void send(GameCommand cmd) {
        if (isConnected()) {
            log.debug("Sending command: " + cmd);
            try {
                this.outQueue.add(cmd);
            } catch (Exception e) {
                log.error("Send Failed: " + e.getClass().getSimpleName() + " " + e.getMessage());
            }
        } else {
            log.warn("Ignoring send of comd '" + cmd.getType() + "' since not connected");
        }
    }

    public final void sendError(Exception e) {
        GameCommand cmd = new GameCommand(GameCommandType.CL_ERROR).setArg("msg", "ERROR: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
        log.error("Sending error: " + cmd);
        send(cmd);
    }

    public final void sendError(String err) {
        GameCommand cmd = new GameCommand(GameCommandType.CL_ERROR).setArg("msg", "ERROR: " + err);
        log.error("Sending error: " + cmd);
        send(cmd);
    }

    /**
     * 
     * @param form
     */
    public final void submitForm(ClientForm form) {
        GameCommand cmd = new GameCommand(GameCommandType.CL_FORM_SUBMIT);
        for (FormElem elem : form.values.values()) {
            cmd.setArg(elem.getId(), elem.getValue());
        }
        cmd.setArg("id", "" + form.getId());
        send(cmd);
    }
    
    /**
     * Called when a text message arrives form the server 
     * @param message
     */
    protected abstract void onMessage(String message);

    /**
     * Called if the server disconnects us.
     * @param message
     */
    protected abstract void onDisconnected(String message);
    
    /**
     * Called after successful socket connect and handshake
     */
    protected abstract void onConnected();
    
    /**
     * Called for unhandled server commands.
     * @param cmd
     */
    protected abstract void onCommand(GameCommand cmd) throws Exception;
    
    /**
     * Called when server sends client a form to be filled out
     * @param form
     */
    protected abstract void onForm(ClientForm form);

    /**
     * Must override to be able to use execute method
     *
     * @return
     */
    protected abstract Object getExecuteObject();

}
