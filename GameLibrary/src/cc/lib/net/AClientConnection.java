package cc.lib.net;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 3/12/18.
 * <p>
 * ClientConnection handles the socket and threads associated with a single client
 * That has passed the handshaking test.
 * <p>
 * Can only be created by GameServer instances.
 * <p>
 * Execute methods remotely and wait for the return value if it has one. This should have effect of
 * making the socket connection appear transparent the the caller.
 * <p>
 * TODO: Add retries?
 * <p>
 * Be sure to not obfuscate those methods involved in this scheme so different versions
 * of application can remain compatible.
 * <p>
 * Example:
 * <p>
 * you have some object that exists on 2 systems connected by I/O stream.
 * <p>
 * MyObjectType myObject;
 * <p>
 * the client and server endpoints both derive from ARemoteExecutor
 * <p>
 * On system A:
 * server = new ARemoteExecutor() { ... }
 * <p>
 * On system B:
 * client = new ARemoteExecutor() {... }
 * <p>
 * System register objects to be executed upon
 * <p>
 * client.register(myObject.getClass().getSimpleName(), myObject);
 * <p>
 * ...
 *
 * @Keep class MyObjectType {
 * <p>
 * // make sure to prevent obfuscation
 * @Keep public Integer add(int a, int b) {
 * try {
 * // executeOnRemote will determine method name and class from Exception stack
 * // and bundle everything up to the client then wait for a response and return
 * // it once it arrives
 * return server.executeOnRemote(true, a, b);
 * } catch (IOException e) {
 * ...
 * }
 * return null; // good practice to return non-primitives
 * }
 * }
 */
public abstract class AClientConnection {

    protected Logger log = LoggerFactory.getLogger("SVR", getClass());

    public interface Listener {
        default void onCommand(AClientConnection c, GameCommand cmd) {
        }

        default void onDisconnected(AClientConnection c, String reason) {
        }

        default void onReconnected(AClientConnection c) {
        }

        default void onCancelled(AClientConnection c, String id) {
        }

        default void onPropertyChanged(AClientConnection c) {
        }

        default void onTimeout(AClientConnection c) {
        }
    }

    ;

    protected final AGameServer server;
    private final Map<String, Object> attributes;
    private boolean kicked = false;
    private final Set<Listener> listeners = Collections.synchronizedSet(new HashSet());
    protected boolean disconnecting = false;

    public final void addListener(Listener listener) {
        listeners.add(listener);
    }

    public final void removeListener(Listener l) {
        listeners.remove(l);
    }

    public final void notifyListeners(Consumer<Listener> consumer) {
        Set<Listener> l = new HashSet<>();
        l.addAll(listeners);
        for (Listener it : l) {
            consumer.accept(it);
        }
    }

    public String toString() {
        String r = "ClientConnection name=" + getName();
        r += " connected=" + isConnected();
        if (attributes.size() > 0)
            r += " attribs=" + attributes;
        return r;
    }

    /**
     * Only GameServer can create instances of ClientConnection
     *
     * @param server
     * @throws Exception
     */
    AClientConnection(AGameServer server, Map<String, Object> attributes) {
        this.attributes = attributes;
        this.server = server;
    }

    public final void kick() {
        kicked = true;
        disconnect("Kicked");
    }

    public final boolean isKicked() {
        return kicked;
    }

    public final void unkick() {
        kicked = false;
    }

    /**
     * Send a disconnected message to the client and shutdown the connection.
     *
     * @param reason
     */
    public abstract void disconnect(String reason);

    private void close() {
        log.debug("ClientConnection: close() ...");
        server.removeClient(this);
        log.debug("ClientConnection: outQueue stopped ...");
        log.debug("ClientConnection: close() DONE");
    }

    /**
     * @return
     */
    public abstract boolean isConnected();

    /**
     * @param attributes
     */
    final void setAttributes(Map<String, Object> attributes) {
        this.attributes.putAll(attributes);
    }

    /**
     * @param key
     * @return
     */
    public final Object getAttribute(String key) {
        return this.attributes.get(key);
    }

    /**
     * @return
     */
    public final Iterable<String> getAttributeKeys() {
        return this.attributes.keySet();
    }

    /**
     * @return
     */
    public final Iterable<Object> getAttributeValues() {
        return this.attributes.values();
    }

    public final String getName() {
        return (String) attributes.get("name");
    }

    /**
     * Return the user's handle if set. normal name otherwise.
     *
     * @return
     */
    public final String getDisplayName() {
        if (attributes.containsKey("displayName"))
            return (String) attributes.get("displayName");
        return getName();
    }

    /**
     * @return
     */
    public final AGameServer getServer() {
        return this.server;
    }


    /**
     * Sent a command to the remote client
     *
     * @param cmd
     */
    public abstract void sendCommand(GameCommand cmd);

    /**
     * Send a message to the remote client
     *
     * @param message
     */
    public final void sendMessage(String message) {
        sendCommand(GameCommandType.MESSAGE.make().setMessage(message));
    }

    /**
     * internal
     */
    public abstract void start();

    protected final boolean processCommand(GameCommand cmd) {
        log.debug("ClientConnection: processCommand: " + cmd.getType());
        if (!isConnected())
            return false;
        if (cmd.getType() == GameCommandType.CL_DISCONNECT) {
            String reason = cmd.getMessage();
            log.info("Client disconnected: " + reason);
            disconnecting = true;
            disconnect(reason);
            close();
        } else if (cmd.getType() == GameCommandType.PING) {
            // client should do this at regular intervals to prevent getting dropped
            sendCommand(cmd);
        } else if (cmd.getType() == GameCommandType.CL_ERROR) {
            System.err.println("ERROR From client '" + getName() + "'\n" + cmd.getString("msg") + "\n" + cmd.getString("stack"));
        } else if (cmd.getType() == GameCommandType.CL_UPDATE) {
            this.attributes.putAll(cmd.getArguments());
            notifyListeners((l) -> {
                l.onPropertyChanged(this);
            });
        } else {
            notifyListeners((l) -> {
                l.onCommand(this, cmd);
            });
        }
        return true;
    }

    private final class ResponseListener<T> implements AClientConnection.Listener {
        private T response;
        private final String id;
        private boolean cancelled;

        ResponseListener(String id) {
            this.id = id;
        }

        public void setResponse(T response) {
            this.response = response;
            synchronized (this) {
                notify();
            }
        }

        @Override
        public void onCommand(AClientConnection conn, GameCommand cmd) {
            if (id.equals(cmd.getString("target"))) {
                try {
                    if (cmd.getBoolean("cancelled", false)) {
                        cancelled = true;
                        setResponse(null);
                    } else {
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
        public void onDisconnected(AClientConnection c, String reason) {
            synchronized (this) {
                notify();
            }
        }
    }

    ;

    /**
     * @param objId
     * @param params
     * @param <T>
     * @return
     */
    public final <T> T executeDerivedOnRemote(String objId, boolean returnsResult, Object... params) {
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
    public final <T> T executeMethodOnRemote(String targetId, boolean returnsResult, String method, Object... params) { // <-- need [] array to disambiguate from above method
        log.debug("executeMethodOnRemote: %s(%s, %s)", method, targetId, Arrays.toString(params));
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
                String id = method + "_" + targetId + "_" + Utils.genRandomString(32);
                cmd.setArg("responseId", id);
                ResponseListener<T> listener = new ResponseListener<>(id);
                addListener(listener);
                sendCommand(cmd);
                log.debug("Waiting for response");
                try {
                    synchronized (listener) {
                        listener.wait();
                    }
                } finally {
                    removeListener(listener);
                }
                log.debug("Response: %s", listener.response);
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
        for (Iterator<Listener> it = listeners.iterator(); it.hasNext(); ) {
            Listener l = it.next();
            if (l == null) {
                it.remove();
            } else {
                l.onCancelled(this, id);
            }
        }
    }

}
