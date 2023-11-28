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

import cc.lib.crypt.Cypher;
import cc.lib.crypt.EncryptionInputStream;
import cc.lib.crypt.EncryptionOutputStream;
import cc.lib.crypt.HuffmanEncoding;
import cc.lib.game.Utils;

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
public class GameServer extends AGameServer {

    public int TIMEOUT = 20000;
    public int PING_FREQ = 10000;

    private SocketListener socketListener;
    private final Cypher cypher;
    private HuffmanEncoding counter = null;

    public String toString() {
        String r = "GameServer:" + mServerName + " v:" + mVersion;
        if (clients.size() > 0)
            r += " connected clients:" + clients.size();
        return r;
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
        super(serverName, listenPort, serverVersion, maxConnections);
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
     * Disconnect all clients and stop listening.  Will block until all clients have closed their sockets.
     */
    public final void stop() {
        log.info("GameServer: Stopping server: " + this);
        if (socketListener != null) {
            socketListener.stop();
            socketListener = null;
            disconnectingLock.reset();
            synchronized (clients) {
                for (AClientConnection c : clients.values()) {
                    if (c.isConnected()) {
                        disconnectingLock.acquire();
                        c.disconnect("Server Stopping");
                    }
                }
            }
            disconnectingLock.block(10000, () -> log.warn("Unclean stoppage")); // give clients a chance to disconnect from their end
            clear();
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

        notifyListeners((l) -> {
            l.onServerStopped();
        });
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
            socket.setSoTimeout(TIMEOUT); // give a few seconds latency
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


                AClientConnection conn = null;
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
                        ((ClientConnection) conn).connect(socket, in, out);
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
                    AClientConnection _conn = conn;
                    if (reconnection) {
                        _conn.notifyListeners((l) -> {
                            l.onReconnected(_conn);
                        });
                    } else {
                        notifyListeners((l) -> {
                            l.onConnected(_conn);
                        });
                    }
                }

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
}
