package cc.lib.net;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.GException;
import cc.lib.utils.Reflector;

/**
 * Base class for clients that want to connect to a GameServer
 *
 * @author ccaron
 */
public abstract class AGameClient {

    protected final Logger log = LoggerFactory.getLogger("P2P:CL", AGameClient.class);

    public interface Listener {
        default void onCommand(GameCommand cmd) {
        }

        default void onMessage(String msg) {
        }

        default void onDisconnected(String reason, boolean serverInitiated) {
        }

        default void onConnected() {
        }

        default void onPing(int time) {
            System.err.println("Ping turn around time: " + time);
        }
    }

    protected final Set<Listener> listeners = Collections.synchronizedSet(new HashSet<>());
    protected String serverName = null;
    private final Map<String, Object> executorObjects = new HashMap<>();
    private String passPhrase = null;
    // properties are reflected on the server side in AClientConnection
    protected final Map<String, Object> properties = new HashMap<>();


    // giving package access for JUnit tests ONLY!
    final CommandQueueWriter outQueue = new CommandQueueWriter("CL") {
        @Override
        protected void onTimeout() {
            if (isConnected()) {
                outQueue.add(new GameCommand(GameCommandType.PING)
                        .setArg("time", System.currentTimeMillis()));
            }
        }

    };

    /**
     * Create a client that will connect to a given server using a given login name.
     * The userName must be unique to the server for successful connect.
     *
     * @param deviceName
     * @param version
     */
    public AGameClient(String deviceName, String version) {
        if (Utils.isEmpty(deviceName))
            throw new IllegalArgumentException("Device name cannot be empty");
        properties.put("version", version);
        properties.put("name", deviceName);
    }


    /**
     * @return
     */
    public final String getDisplayName() {
        return (String) properties.get("displayName");
    }

    /**
     * @return
     */
    public final String getServerName() {
        return serverName;
    }

    /**
     * @return
     */
    public final String getPassPhrase() {
        return passPhrase;
    }

    /**
     * @param l
     */
    public final void addListener(Listener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    /**
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
     * @param connectCallback called with success or failure when connection complete
     */
    /**
     * Spawn a thread and try to connect.  called on success
     *
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
    public abstract void connectBlocking(InetAddress address, int port) throws IOException;

    /**
     *
     */
    public abstract void reconnectAsync();

    /**
     * Return true ONLY is socket connected and handshake success
     *
     * @return
     */
    public abstract boolean isConnected();

    public final void disconnect() {
        disconnect("player left session");
    }

    public abstract void disconnectAsync(String reason, Utils.Callback<Integer> onDone);

    /**
     * Synchronous Disconnect from the server.  If not connected then do nothing.
     * Will NOT call onDisconnected.
     */
    public abstract void disconnect(String reason);

    // making this package access so JUnit can test a client timeout
    abstract void close();

    /**
     * Reset this client so that the next call to 'connect' will be a connect and not re-connect.
     * Not valid to be called while connected.
     */
    public abstract void reset();

    /**
     * Send a command to the server.
     *
     * @param cmd
     */
    public abstract void sendCommand(GameCommand cmd);

    /**
     * @param message
     */
    public final void sendMessage(String message) {
        sendCommand(GameCommandType.MESSAGE.make().setMessage(message));
    }

    /**
     * @param e
     */
    public final void sendError(Exception e) {
        GameCommand cmd = new GameCommand(GameCommandType.CL_ERROR).setArg("msg", "ERROR: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
        log.error("Sending error: " + cmd);
        sendCommand(cmd);
    }

    /**
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
     * @param displayName
     */
    public final void setDisplayName(String displayName) {
        setProperty("displayName", displayName);
    }

    public void setProperty(String name, Object value) {
        properties.put(name, value);
        if (isConnected()) {
            sendCommand(new GameCommand(GameCommandType.CL_UPDATE).setArg(name, value));
        }
    }

    /**
     * register an object with a specific id
     *
     * @param id
     * @param o
     */
    public final void register(String id, Object o) {
        log.debug("register '%s' -> %s", id, o.getClass());
        executorObjects.put(id, o);
    }

    /**
     * Unregister an object by its id
     *
     * @param id
     */
    public final void unregister(String id) {
        log.debug("unregister %s", id);
        executorObjects.remove(id);
    }

    class ResponseThread extends Thread implements Listener {

        final Method m;
        final String responseId;
        final Object obj;
        final Object[] params;

        ResponseThread(Method m, String responseId, Object obj, Object... params) {
            this.m = m;
            this.responseId = responseId;
            this.obj = obj;
            this.params = params;
            addListener(this);
        }

        @Override
        public void run() {
            try {
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
            } catch (Exception e) {
                e.printStackTrace();
            }
            removeListener(this);
        }

        @Override
        public void onDisconnected(String reason, boolean serverInitiated) {
            removeListener(this);
            interrupt();
        }
    }

    /*
     * Execute a method locally based on params provided by remote caller.
     *
     * @param cmd
     * @throws IOException
     */
    void handleExecuteRemote(GameCommand cmd) throws IOException {
        log.debug("handleExecuteOnRemote %s", cmd);
        String method = cmd.getString("method");
        int numParams = cmd.getInt("numParams");
        Class[] paramsTypes = new Class[numParams];
        final Object[] params = new Object[numParams];
        for (int i = 0; i < numParams; i++) {
            String param = cmd.getString("param" + i);
            Object o = Reflector.deserializeFromString(param);
            if (o != null) {
                paramsTypes[i] = o.getClass();
                params[i] = o;
            } else {
                paramsTypes[i] = Object.class;
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
                new ResponseThread(m, responseId, obj, params).start();
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

    private Method findMethod(String method, Object obj, Class[] paramsTypes, Object[] params) throws Exception {
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

    public Method searchMethods(Object execObj, String method, Class[] types, Object[] params) throws Exception {
        Class clazz = execObj.getClass();
        while (clazz != null && !clazz.equals(Object.class)) {
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
                    if (params[i] == null)
                        continue;
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
        primitiveCompatibilityMap.put(boolean.class, c = Arrays.asList(boolean.class, Boolean.class));
        primitiveCompatibilityMap.put(Boolean.class, c);
        primitiveCompatibilityMap.put(byte.class, c = Arrays.asList(byte.class, Byte.class));
        primitiveCompatibilityMap.put(Byte.class, c);
        primitiveCompatibilityMap.put(int.class, c = Arrays.asList(int.class, Integer.class, byte.class, Byte.class));
        primitiveCompatibilityMap.put(Integer.class, c);
        primitiveCompatibilityMap.put(long.class, c = Arrays.asList(int.class, Integer.class, byte.class, Byte.class, long.class, Long.class));
        primitiveCompatibilityMap.put(Long.class, c);
        primitiveCompatibilityMap.put(float.class, c = Arrays.asList(int.class, Integer.class, byte.class, Byte.class, float.class, Float.class));
        primitiveCompatibilityMap.put(Float.class, c);
        primitiveCompatibilityMap.put(double.class, c = Arrays.asList(int.class, Integer.class, byte.class, Byte.class, float.class, Float.class, double.class, Double.class, long.class, Long.class));
        primitiveCompatibilityMap.put(Double.class, c);

    }

    private static boolean isCompatiblePrimitives(Class a, Class b) {
        if (primitiveCompatibilityMap.containsKey(a))
            return b == null || primitiveCompatibilityMap.get(a).contains(b);
        return false;
    }

}
