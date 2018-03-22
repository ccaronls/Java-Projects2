package cc.lib.net;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

import cc.lib.crypt.Cypher;
import cc.lib.crypt.EncryptionInputStream;
import cc.lib.crypt.EncryptionOutputStream;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

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

    private final Logger log = LoggerFactory.getLogger(GameServer.class);
    
    /**
     *
     */
    public interface Listener {
        /**
         *
         * @param conn
         */
        void onConnected(ClientConnection conn);

        /**
         *
         * @param conn
         */
        void onReconnection(ClientConnection conn);
        /*

         */
        void onClientDisconnected(ClientConnection conn);
    }
    
    // keep sorted by alphabetical order 
    private final Map<String,ClientConnection> clients = new LinkedHashMap<>();
    private SocketListener socketListener;
    private final Set<Listener> listeners = Collections.synchronizedSet(new LinkedHashSet<Listener>());
    private final int clientReadTimeout;
    private final String mVersion;
    private final Cypher cypher;
    private final int maxConnections;
    private final int port;
    private final String mServerName;
    private String password = null;
    
    public String toString() {
        String r = "GameServer:" + mServerName + " v:" + mVersion;
        if (clients.size() > 0)
            r += " connected clients:" + clients.size();
        return r;
    }

    void removeClient(ClientConnection cl) {
        synchronized (clients) {
            clients.remove(cl);
        }
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
     * @param clientReadTimeout timeout in milliseconds before a client disconnect
     * @param serverVersion version of this service to use to check compatibility with clients
     * @param cypher used to encrypt the dialog. can be null.
     * @param maxConnections max number of clients to allow to be connected
     * @throws IOException 
     * @throws Exception
     */
    public GameServer(String serverName, int listenPort, int clientReadTimeout, String serverVersion, Cypher cypher, int maxConnections) {
        this.mServerName = serverName.toString(); // null check
        if (clientReadTimeout < 1000)
            throw new RuntimeException("Value for timeout too small");
        this.clientReadTimeout = clientReadTimeout;
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
            synchronized (clients) {
                for (ClientConnection c : clients.values()) {
                    c.disconnect("Server Stopping");
                }
            }
            clients.clear();
            socketListener.stop();
            socketListener = null;
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

    /**
     * Send an execute command to all client using derived method. No return respose supported
     * @param objId
     * @param params
     */
    public final void broadcastExecuteOnRemote(String objId, Object ... params) {
        StackTraceElement elem = new Exception().getStackTrace()[1];
        broadcastExecuteOnRemote(objId, elem.getMethodName(), params);
    }

    /**
     * Send an execute command to all client using specific method. No return response supported.
     * @param objId
     * @param method
     * @param params
     */
    public final void broadcastExecuteOnRemote(String objId, String method, Object ... params) {
        synchronized (clients) {
            for (ClientConnection c : clients.values()) {
                if (c.isConnected())
                    try {
                        c.executeOnRemote(objId, method, null, params);
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("ERROR Sending to client '" + c.getName() + "' " + e.getClass() + " " + e.getMessage());
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
        broadcastCommand(new GameCommand(GameCommandType.MESSAGE).setMessage(message));
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
            socket.setSoTimeout(clientReadTimeout+5000); // give a few seconds latency 
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
                    in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                    out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                }

                final long magic = in.readLong();
                if (magic != 87263450972L)
                    throw new ProtocolException("Unknown client");

                GameCommand cmd = GameCommand.parse(in);
                log.debug("Parsed incoming command: " + cmd);
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
                    if (!password.equals(pswd.getArg("password")))
                        throw new ProtocolException("Bad Password");
                }


                ClientConnection conn = null;
                
                synchronized (clients) {
                    if (cmd.getType() == GameCommandType.CL_CONNECT) {
                        conn = null;
                        if (clients.containsKey(name)) {
                            conn = clients.get(name);
                            if (conn.isConnected()) {
                                //new GameCommand(GameCommandType.SVR_MESSAGE).setMessage("ERROR: A client with the name '" + name + "' is already connected").write(out);
                                throw new ProtocolException("client with name already exists");
                            }
                        }
                        if (conn == null) {
                            if (clients.size() >= maxConnections) {
                                throw new java.net.ProtocolException("Max client connections reached");
                            }
                            conn = new ClientConnection(GameServer.this, name);
                        }
                        conn.connect(socket, in, out);
                        clients.put(name, conn);
                    } else if (cmd.getType() == GameCommandType.CL_RECONNECT) {
                        if (!clients.containsKey(name)) {
                            //new GameCommand(GameCommandType.SVR_MESSAGE).setMessage("ERROR: No record of client with the name '" + name + "' was found").write(out);
                            throw new ProtocolException("Unknown client connection '" + name + "'");
                        } 
    
                        conn = clients.get(name);
                        if (conn.isConnected()) {
                            //new GameCommand(GameCommandType.SVR_MESSAGE).setMessage("ERROR: A client with the name '" + name + "' is already connected").write(out);
                            throw new ProtocolException("Client '"  + name + "' is already connected");
                        }
                        conn.connect(socket, in , out);
                        //new GameCommand(GameCommandType.SVR_CONNECTED).setArg("keepAlive", clientReadTimeout).write(out);
                        //log.debug("GameServer: Client " + name + " connected");
                        //serverListener.onReconnection(conn);
                    } else {
                        throw new ProtocolException("Handshake failed: Invalid client command: " + cmd);
                    }
                }
                
                new GameCommand(GameCommandType.SVR_CONNECTED)
                        .setName(mServerName)
                        .setArg("keepAlive", clientReadTimeout).write(out);

                // TODO: Add a password feature when server prompts for password before conn.connect is called.

                log.debug("GameServer: Client " + name + " connected");
                List<Listener> list = new ArrayList<>(listeners);

                if (cmd.getType() == GameCommandType.CL_CONNECT) {
                    for (Listener l : list)
                        l.onConnected(conn);
                } else {
                    for (Listener l : list)
                        l.onReconnection(conn);
                }

                // process other listeners
                cmd.getType().notifyListeners(cmd);

                //new GameCommand(GameCommandType.SVR_CONNECTED).setArg("keepAlive", clientReadTimeout).write(out);
                //log.debug("GameServer: Client " + name + " connected");

                // send the client the main menu
            } catch (ProtocolException e) {
                try {
                    log.error(e);
                    new GameCommand(GameCommandType.DISCONNECT).setMessage(e.getMessage()).write(out);
                } catch (Exception ex) {}
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
