package cc.lib.net;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.Lock;

/**
 * A Game server is a server that handles normal connection/handshaking and maintains
 * a set of ClientConnections.  The base version should be minimal with only those
 * methods to listen for connections accept and handshake with the client and maintain
 * the connections.
 * <p>
 * The Base Game Server Provides:
 * - Handshaking with new connections
 * - Managing Connection timeouts and reconnects.
 * - Managing older client versions.
 * - Managing Protocol Encryption
 * - Executing methods on a game object (see GameCommon)
 * <p>
 * Override protected methods to create custom behaviors
 *
 * @author ccaron
 */
public abstract class AGameServer {

    /**
     *
     */
    public interface Listener {
        /**
         * @param conn
         */
        default void onConnected(AClientConnection conn) {
        }

        /**
         *
         */
        default void onServerStopped() {
        }
    }

    protected final Logger log = LoggerFactory.getLogger("P2P:SVR", getClass());

    // keep sorted by alphabetical order
    protected final Map<String, AClientConnection> clients = new ConcurrentHashMap<>(new LinkedHashMap());
    protected final String mVersion;
    protected final int maxConnections;
    protected final int port;
    protected final String mServerName;
    protected String password = null;
    protected final Lock disconnectingLock = new Lock();
    private final Set<Listener> listeners = Collections.synchronizedSet(new HashSet<>());

    void clear() {
        clients.clear();
        listeners.clear();
    }

    /**
     * @param l
     */
    public final void addListener(Listener l) {
        listeners.add(l);
    }

    /**
     * @param l
     */
    public final void removeListener(Listener l) {
        listeners.remove(l);
    }

    public final void notifyListeners(Consumer<Listener> consumer) {
        Set<Listener> s = new HashSet<>();
        s.addAll(listeners);
        for (Listener it : s) {
            consumer.accept(it);
        }
    }

    public String toString() {
        String r = "GameServer:" + mServerName + " v:" + mVersion;
        if (clients.size() > 0)
            r += " connected clients:" + clients.size();
        return r;
    }

    final void removeClient(AClientConnection cl) {
        log.debug("removing client " + cl.getName());
        clients.remove(cl);
        disconnectingLock.release();
    }

    final void addClient(AClientConnection cl) {
        clients.put(cl.getName(), cl);
    }

    public final @NotNull String getName() {
        return mServerName;
    }

    /**
     * You can optionally require clients to enter a password to login to your server
     *
     * @param paswword
     */
    public final void setPassword(String paswword) {
        this.password = paswword;
    }

    /**
     * Create a server and start listening.  When cypher is not null, then then server will only
     * allow encrypted clients.
     *
     * @param serverName     the name of this server as seen by the clients
     * @param listenPort     port to listen on for new connections
     * @param serverVersion  version of this service to use to check compatibility with clients
     * @param maxConnections max number of clients to allow to be connected
     * @throws IOException
     * @throws Exception
     */
    public AGameServer(String serverName, int listenPort, String serverVersion, int maxConnections) {
        this.mServerName = serverName; // null check
        if (listenPort < 1000)
            throw new RuntimeException("Invalid value for listener port/ Think higher.");
        this.port = listenPort;
        if (maxConnections < 2)
            throw new RuntimeException("Value for maxConnections too small");
        this.maxConnections = maxConnections;
        this.mVersion = serverVersion; // null check
    }

    /**
     * Start listening for connections
     *
     * @throws IOException
     */
    public abstract void listen() throws IOException;

    /**
     * @return
     */
    public abstract boolean isRunning();

    /**
     * Disconnect all clients and stop listening.  Will block until all clients have closed their sockets.
     */
    public abstract void stop();

    /**
     * Get iterable over all connection ids
     *
     * @return
     */
    public final Iterable<String> getConnectionKeys() {
        return clients.keySet();
    }

    /**
     * Get iterable over all connection values
     *
     * @return
     */
    public final Iterable<AClientConnection> getConnectionValues() {
        return clients.values();
    }

    /**
     * Get a specific connection by its id.
     *
     * @param id
     * @return
     */
    public final AClientConnection getClientConnection(String id) {
        return clients.get(id);
    }

    public final AClientConnection getConnection(int index) {
        Iterator<AClientConnection> it = clients.values().iterator();
        while (index-- > 0 && it.hasNext()) {
            it.next();
        }
        return it.next();
    }

    /**
     * @return
     */
    public final int getNumClients() {
        return clients.size();
    }

    /**
     * @return
     */
    public final int getNumConnectedClients() {
        int num = 0;
        for (AClientConnection c : clients.values()) {
            if (c.isConnected())
                num++;
        }
        return num;
    }

    /**
     * Broadcast a command to all connected clients
     *
     * @param cmd
     */
    public final void broadcastCommand(GameCommand cmd) {
        if (isConnected()) {
            synchronized (clients) {
                for (AClientConnection c : clients.values()) {
                    if (c.isConnected())
                        try {
                            c.sendCommand(cmd);
                        } catch (Exception e) {
                            e.printStackTrace();
                            log.error("ERROR Sending to client '" + c.getName() + "' " + e.getClass() + " " + e.getMessage());
                        }
                }
            }
        }
    }

    /**
     * Send an execute command to all client using derived method. No return respose supported
     *
     * @param objId
     * @param params
     */
    public final void broadcastExecuteOnRemote(String objId, Object... params) {
        if (isConnected()) {
            StackTraceElement elem = new Exception().getStackTrace()[1];
            broadcastExecuteMethodOnRemote(objId, elem.getMethodName(), params);
        }
    }

    /**
     * Send an execute command to all client using specific method. No return response supported.
     *
     * @param objId
     * @param method
     * @param params
     */
    public final void broadcastExecuteMethodOnRemote(String objId, String method, Object... params) {
        if (isConnected()) {
            synchronized (clients) {
                for (AClientConnection c : clients.values()) {
                    if (c.isConnected())
                        try {
                            log.debug("executeMethodOnRemote " + objId + "'" + method + "': " + params);
                            c.executeMethodOnRemote(objId, false, method, params);
                        } catch (Exception e) {
                            e.printStackTrace();
                            log.error("ERROR Sending to client '" + c.getName() + "' " + e.getClass() + " " + e.getMessage());
                        }
                }
            }
        }
    }

    public final boolean isConnected() {
        return isRunning() && getNumConnectedClients() > 0;
    }

    /**
     * Broadcast a command to all connected clients
     *
     * @param message
     */
    public final void broadcastMessage(String message) {
        if (isConnected()) {
            broadcastCommand(new GameCommand(GameCommandType.MESSAGE).setMessage(message));
        }
    }

    /**
     * Override this method to perform any custom version compatibility test.
     * If the clientVersion is compatible, do nothing.  Otherwise throw a
     * descriptive message. Default implementation throws an exception unless
     * clientVersion is exact match for @see getVersion.
     *
     * @param clientVersion
     * @throws Exception
     */
    protected void clientVersionCompatibilityTest(String clientVersion, String serverVersion) throws ProtocolException {
        if (!clientVersion.equals(mVersion))
            throw new ProtocolException("Incompatible client version '" + clientVersion + "'");
    }

}
