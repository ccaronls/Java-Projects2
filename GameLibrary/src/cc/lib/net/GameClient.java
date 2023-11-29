package cc.lib.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import cc.lib.crypt.Cypher;
import cc.lib.crypt.EncryptionInputStream;
import cc.lib.crypt.EncryptionOutputStream;
import cc.lib.game.Utils;
import cc.lib.utils.GException;

/**
 * Base class for clients that want to connect to a GameServer
 *
 * @author ccaron
 */
public class GameClient extends AGameClient {

    private enum State {
        READY, // connect not called
        CONNECTING, // connect called, handshake in progress
        CONNECTED, // handshake success
        DISCONNECTED // all IO closed and threads stopped
    }

    private State state = State.READY;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final Cypher cypher;
    private InetAddress connectAddress;
    private int connectPort;

    /**
     * Create a client that will connect to a given server using a given login name.  
     * The userName must be unique to the server for successful connect.
     * 
     * @param deviceName
     * @param version
     * @param cypher
     */
    public GameClient(String deviceName, String version, Cypher cypher) {
        super(deviceName, version);
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

    private boolean isIdle() {
        return state == State.READY || state == State.DISCONNECTED;
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
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                //socket.setTrafficClass();
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
                outQueue.add(new GameCommand(GameCommandType.CL_CONNECT).setArgs(properties));
                new Thread(new SocketReader()).start();
                log.debug("Socket Connection Established");
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

    public final void disconnectAsync(String reason, Utils.Callback<Integer> onDone) {
        new Thread() {
            @Override
            public void run() {
                disconnect(reason);
                onDone.onDone(0);
            }
        }.start();
    }

    /**
     * Synchronous Disconnect from the server.  If not connected then do nothing.
     * Will NOT call onDisconnected.
     */
    public final void disconnect(String reason) {
        if (state == State.CONNECTED || state == State.CONNECTING) {
            log.debug("GameClient: client '" + this.getDisplayName() + "' disconnecitng ...");
            try {
                outQueue.clear();
                outQueue.add(new GameCommand(GameCommandType.CL_DISCONNECT).setMessage("player left session"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            state = State.DISCONNECTED;
            close();
            if (listeners.size() > 0) {
                Listener[] arr = listeners.toArray(new Listener[listeners.size()]);
                for (Listener l : arr) {
                    l.onDisconnected(reason, false);
                }
            }
        }
//        reset(); // we want to be in the 'disconnected state'
    }
    
    // making this package access so JUnit can test a client timeout
    void close() {
        state = State.DISCONNECTED;
        outQueue.stop();
        // close output first to make sure it is flushed
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
    
    private final boolean isDisconnected() {
        return state == State.READY || state == State.DISCONNECTED;
    }

    private class SocketReader implements Runnable {
        public void run() {
            log.debug("GameClient: Client Listener Thread starting");

            state = State.CONNECTING;
            String disconnectedReason = null;

            List<Listener> listenersList = new ArrayList<>();

            while (in != null && !isDisconnected()) {
                try {
                    final GameCommand cmd = GameCommand.parse(in);
                    if (isDisconnected())
                        break;
                    listenersList.clear();
                    listenersList.addAll(listeners);

                    log.debug("Read command: " + cmd);

                    if (cmd.getType() == GameCommandType.SVR_CONNECTED) {
                        serverName = cmd.getName();
                        int keepAliveFreqMS = cmd.getInt("keepAlive");
                        outQueue.setTimeout(keepAliveFreqMS);
                        state = State.CONNECTED;
                        for (Listener l : listenersList) {
                            l.onConnected();
                        }
                    } else if (cmd.getType() == GameCommandType.PING) {
                        long timeSent = cmd.getLong("time");
                        long timeNow = System.currentTimeMillis();
                        for (Listener l : listenersList) {
                            l.onPing((int) (timeNow - timeSent));
                        }
                    } else if (cmd.getType() == GameCommandType.MESSAGE) {
                        for (Listener l : listenersList) {
                            l.onMessage(cmd.getMessage());
                        }
                    } else if (cmd.getType() == GameCommandType.SVR_DISCONNECT) {
                        state = State.DISCONNECTED;
                        disconnectedReason = cmd.getMessage();
                        outQueue.clear();
                        break;
                    } else if (cmd.getType() == GameCommandType.SVR_EXECUTE_REMOTE) {
                        handleExecuteRemote(cmd);
                    } else if (cmd.getType() == GameCommandType.PASSWORD) {
                        String passPhrase = getPassPhrase();
                        if (passPhrase != null) {
                            passPhrase = getPasswordFromUser();
                        }
                        outQueue.add(new GameCommand(GameCommandType.PASSWORD).setArg("password", passPhrase));
                    } else {
                        for (Listener l : listenersList)
                            l.onCommand(cmd);
                    }

                } catch (Exception e) {
                    if (!isDisconnected()) {
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
                for (Listener l : listenersList)
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
     * Override this method to take input from user for password requests.
     * Default behavior throws a runtime exception, so dont super me.
     *
     * @return
     */
    protected String getPasswordFromUser() {
        throw new GException("Client does not overide the getPasswordFromUser method");
    }
}
