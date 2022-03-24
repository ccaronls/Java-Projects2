package cc.lib.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import cc.lib.crypt.Cypher;
import cc.lib.crypt.EncryptionInputStream;
import cc.lib.crypt.EncryptionOutputStream;
import cc.lib.crypt.HuffmanEncoding;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.Lock;
import cc.lib.utils.WeakHashSet;

/**
 * A Game server is a server that handles normal connection/handshaking and maintains
 * a set of ClientConnections.  The base version should be minimal with only those
 * methods to listen for connections accept and handshake with the client and maintain
 * the connections.  
 * 
 * The Base Game Server Provides:
 * - Handshaking with new connections
 * - Managing Connection timeouts and reconnects.
 * - Managing older client versions.
 * - Managing Protocol Encryption
 * - Executing methods on a game object (see GameCommon)
 * 
 * Override protected methods to create custom behaviors
 * 
 * @author ccaron
 *
 */
public class GameServer {

    public final static int TIMEOUT = 30000;
    public final static int PING_FREQ = 10000;

    private final Logger log = LoggerFactory.getLogger("P2PGame", GameServer.class);
    
    /**
     *
     */
    public interface Listener {
        /**
         *
         * @param conn
         */
        default void onConnected(ClientConnection conn) {}

        /**
         *
         * @param conn
         */
        default void onReconnection(ClientConnection conn) {}

        /*
         */
        default void onClientDisconnected(ClientConnection conn) {}

        /**
         *
         * @param conn
         * @param cmd
         */
        default void onCommand(ClientConnection conn, GameCommand cmd) {}

        /**
         *
         */
        default void onServerStopped() {}

        /**
         *
         * @param conn
         * @param newHandle
         */
        default void onClientHandleChanged(ClientConnection conn, String newHandle) {}
    }
    
    // keep sorted by alphabetical order 
    private final Map<String,ClientConnection> clients = new LinkedHashMap<>();
    private SocketListener socketListener;
    private final WeakHashSet<Listener> listeners = new WeakHashSet<>();
    private final String mVersion;
    private final Cypher cypher;
    private final int maxConnections;
    private final int port;
    private final String mServerName;
    private String password = null;
    private HuffmanEncoding counter = null;
    private final Lock disconnectingLock = new Lock();

    public String toString() {
        String r = "GameServer:" + mServerName + " v:" + mVersion;
        if (clients.size() > 0)
            r += " connected clients:" + clients.size();
        return r;
    }

    void removeClient(ClientConnection cl) {
        log.debug("removing client " + cl.getName());
        synchronized (clients) {
            clients.remove(cl);
        }
        disconnectingLock.release();
    }

    public final String getName() {
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
     * @param serverName the name of this server as seen by the clients
     * @param listenPort port to listen on for new connections
     * @param serverVersion version of this service to use to check compatibility with clients
     * @param cypher used to encrypt the dialog. can be null.
     * @param maxConnections max number of clients to allow to be connected
     * @throws IOException 
     * @throws Exception
     */
    public GameServer(String serverName, int listenPort, String serverVersion, Cypher cypher, int maxConnections) {
        this.mServerName = serverName.toString(); // null check
        if (listenPort < 1000)
            throw new RuntimeException("Invalid value for listener port/ Think higher.");
        this.port = listenPort;
        if (maxConnections < 2)
            throw new RuntimeException("Value for maxConnections too small");
        this.maxConnections = maxConnections;
        this.mVersion = serverVersion.toString(); // null check
        this.cypher = cypher;
        if (cypher == null) {
            log.warn("NULL CYPHER NOT A GOOD IDEA FOR RELEASE!");
        }
    }

    /**
     * Start listening for connections
     * @throws IOException
     */
    public void listen() throws IOException {
        ServerSocket socket = new ServerSocket(port);
        new Thread(socketListener = new SocketListener(socket)).start();
    }

    /**
     * Start listening for connections
     * @throws IOException
     */
    public void listen(InetAddress addr) throws IOException {
        ServerSocket socket = new ServerSocket(port, 50, addr);
        new Thread(socketListener = new SocketListener(socket)).start();
    }

    /**
     *
     * @return
     */
    public final boolean isRunning() {
        return socketListener != null;
    }

    /**
     *
     * @param l
     */
    public final void addListener(Listener l) {
        listeners.add(l);
    }

    /**
     *
     * @param l
     */
    public final void removeListener(Listener l) {
        listeners.remove(l);
    }

    final Iterable<Listener> getListeners() {
        return listeners;
    }

    /**
     * Disconnect all clients and stop listening.  Will block until all clients have closed their sockets.
     */
    public final void stop() {
        log.info("GameServer: Stopping server: " + this);
        if (socketListener != null) {
            socketListener.stop();
            socketListener = null;
            disconnectingLock.reset();
            synchronized (clients) {
                for (ClientConnection c : clients.values()) {
                    if (c.isConnected()) {
                        c.disconnect("Server Stopping");
                        disconnectingLock.acquire();
                    }
                }
            }
            disconnectingLock.block(25000); // give clients a chance to disconnect from their end
            clients.clear();
        }

        if (counter != null) {
            log.info("------------------------------------------------------------------------------------------------------");
            log.info("******************************************************************************************************");
            log.info("------------------------------------------------------------------------------------------------------");
            log.info(counter.getEncodingAsCode());
            log.info("------------------------------------------------------------------------------------------------------");
            log.info("******************************************************************************************************");
            log.info("------------------------------------------------------------------------------------------------------");
            counter = null;
        }
    }
    
    /**
     * Get iterable over all connection ids
     * @return
     */
    public final Iterable<String> getConnectionKeys() {
        return clients.keySet();
    }
    
    /**
     * Get iterable over all connection values
     * @return
     */
    public final Iterable<ClientConnection> getConnectionValues() {
        return clients.values();
    }
    
    /**
     * Get a specific connection by its id.
     * @param id
     * @return
     */
    public final ClientConnection getClientConnection(String id) {
        return clients.get(id);
    }

    public final ClientConnection getConnection(int index) {
        Iterator<ClientConnection> it = clients.values().iterator();
        while (index-- > 0 && it.hasNext()) {
            it.next();
        }
        return it.next();
    }

    /**
     * 
     * @return
     */
    public final int getNumClients() {
        return clients.size();                
    }

    /**
     *
     * @return
     */
    public final int getNumConnectedClients() {
        int num = 0;
        for (ClientConnection c : clients.values()) {
            if (c.isConnected())
                num++;
        }
        return num;
    }
    
    /**
     * Broadcast a command to all connected clients
     * @param cmd
     */
    public final void broadcastCommand(GameCommand cmd) {
        if (isConnected()) {
            synchronized (clients) {
                for (ClientConnection c : clients.values()) {
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
     * @param objId
     * @param params
     */
    public final <T> void broadcastExecuteOnRemote(String objId, T ... params) {
        if (isConnected()) {
            StackTraceElement elem = new Exception().getStackTrace()[1];
            broadcastExecuteMethodOnRemote(objId, elem.getMethodName(), params);
        }
    }

    /**
     * Send an execute command to all client using specific method. No return response supported.
     * @param objId
     * @param method
     * @param params
     */
    public final void broadcastExecuteMethodOnRemote(String objId, String method, Object ... params) {
        if (isConnected()) {
            synchronized (clients) {
                for (ClientConnection c : clients.values()) {
                    if (c.isConnected())
                        try {
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
     * @param message
     */
    public final void broadcastMessage(String message) {
        if (isConnected()) {
            broadcastCommand(new GameCommand(GameCommandType.MESSAGE).setMessage(message));
        }
    }

    private class SocketListener implements Runnable {

        ServerSocket socket;
        boolean running;
        
        SocketListener(ServerSocket socket) {
            this.socket = socket;
            running = true;
        }
        
        void stop() {
            running = false;
            try {
                socket.close();
            } catch (Exception e) {}
        }
        
        public void run() {
            try {
            
                log.info("GameServer: Thread started listening for connections");
                while (running) {
                    Socket client = socket.accept();
                    log.debug("New Client connect:"
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
                            ,client.getRemoteSocketAddress()
                            ,client.getLocalAddress()
                            ,client.getKeepAlive()
                            ,client.getOOBInline()
                            ,client.getSendBufferSize()
                            ,client.getReceiveBufferSize()
                            ,client.getReuseAddress()
                            ,client.getTcpNoDelay()
                            ,client.getSoTimeout()
                            ,client.getSoLinger()
                            );
                    new Thread(new HandshakeThread(client)).start();
                }                
            } catch (SocketException e) {
                log.warn("GameServer: Thread Exiting since socket is closed");
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (Iterator<Listener> it = listeners.iterator(); it.hasNext(); ) {
                try {
                    it.next().onServerStopped();
                } catch (Exception e) {
                    e.printStackTrace();
                    it.remove();
                }
            }
        }
    }

    void close(Socket socket, InputStream in, OutputStream out) {
        try {
            if (out== null)
                out = socket.getOutputStream();
            out.flush();
            out.close();
        } catch (Exception ex) {}                
        try {
            if (in == null)
                in = socket.getInputStream();
            in.close();
        } catch (Exception ex) {}
        try {
            socket.close();
        } catch (Exception ex) {}   
    }
    
    private class HandshakeThread implements Runnable {
        final Socket socket;
        
        HandshakeThread(Socket socket) throws Exception {
            this.socket = socket;
            socket.setSoTimeout(TIMEOUT+10000); // give a few seconds latency
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
        }

        public void run() {
            DataInputStream in = null;
            DataOutputStream out = null;
            try {

                log.info("GameServer: Start handshake with new connection");

                if (cypher != null) {
                    in = new DataInputStream(new EncryptionInputStream(new BufferedInputStream(socket.getInputStream()), cypher));
                    out = new DataOutputStream(new EncryptionOutputStream(new BufferedOutputStream(socket.getOutputStream()), cypher));
                } else {
                    counter = new HuffmanEncoding();
                    in = new DataInputStream(new BufferedInputStream(new HuffmanByteCounterInputStream(socket.getInputStream(), counter)));
                    out = new DataOutputStream(new BufferedOutputStream(new HuffmanByteCounterOutputStream(socket.getOutputStream(), counter)));
                }

                final long magic = in.readLong();
                if (magic != 87263450972L)
                    throw new ProtocolException("Unknown client");

                GameCommand cmd = GameCommand.parse(in);
                //log.debug("Parsed incoming command: " + cmd);
                String name = cmd.getName();
                String clientVersion = cmd.getVersion();
                if (clientVersion == null) {
                    new GameCommand(GameCommandType.MESSAGE).setMessage("ERROR: Missing required 'version' attribute").write(out);
                    throw new ProtocolException("Broken Protocol.  Expected clientVersion field in cmd: " + cmd);
                }
                
                clientVersionCompatibilityTest(clientVersion, mVersion);

                if (!Utils.isEmpty(password)) {
                    new GameCommand(GameCommandType.PASSWORD).write(out);
                    GameCommand pswd = GameCommand.parse(in);
                    if (!password.equals(pswd.getString("password")))
                        throw new ProtocolException("Bad Password");
                }


                ClientConnection conn = null;
                boolean reconnection = false;
                synchronized (clients) {
                    if (cmd.getType() == GameCommandType.CL_CONNECT) {
                        conn = null;
                        if (clients.containsKey(name)) {
                            conn = clients.get(name);
                            if (conn.isConnected()) {
                                //new GameCommand(GameCommandType.SVR_MESSAGE).setMessage("ERROR: A client with the name '" + name + "' is already connected").write(out);
                                throw new ProtocolException("client with name already exists");
                            }
                            if (conn.isKicked()) {
                                throw new ProtocolException("Client Banned");
                            }
                            reconnection = true;
                        }
                        if (conn == null) {
                            if (clients.size() >= maxConnections) {
                                throw new java.net.ProtocolException("Max client connections reached");
                            }
                            conn = new ClientConnection(GameServer.this, name);
                        }
                        conn.connect(socket, in, out);
                        clients.put(name, conn);
                    } else {
                        throw new ProtocolException("Handshake failed: Invalid client command: " + cmd);
                    }
                }
                
                new GameCommand(GameCommandType.SVR_CONNECTED)
                        .setName(mServerName)
                        .setArg("keepAlive", PING_FREQ).write(out);

                // TODO: Add a password feature when server prompts for password before conn.connect is called.

                log.debug("GameServer: Client " + name + " connected");

                if (cmd.getType() == GameCommandType.CL_CONNECT) {
                    for (Listener l : getListeners()) {
                        if (reconnection)
                            l.onReconnection(conn);
                        else
                            l.onConnected(conn);
                    }
                }

                // process other listeners
                cmd.getType().notifyListeners(cmd);

                //new GameCommand(GameCommandType.SVR_CONNECTED).setArg("keepAlive", clientReadTimeout).write(out);
                //log.debug("GameServer: Client " + name + " connected");

                // send the client the main menu
            } catch (ProtocolException e) {
                try {
                    log.error(e);
                    GameCommandType.SVR_DISCONNECT.make().setMessage(e.getMessage()).write(out);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                close(socket, in, out);
            } catch (Exception e) {
                log.error(e);
                close(socket, null, null);
            }
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
