package cc.lib.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.Reflector;

/**
 * Base class for clients that want to connect to a GameServer
 * 
 * @author ccaron
 *
 */
public class GameClient extends ARemoteExecutor {

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

        void onDisconnected(String reason);

        void onConnected();
    }
    
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final String userName;
    private State state = State.READY;
    private final String version;
    private Cypher cypher;
    private final Set<Listener> listeners = new HashSet<>();
    
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
                socket.setSoTimeout(0);
                socket.setKeepAlive(true);
                if (cypher != null) {
                    log.debug("Using Cypher: " + cypher);
                    in = new DataInputStream(new EncryptionInputStream(new BufferedInputStream(socket.getInputStream()), cypher));
                    out = new DataOutputStream(new EncryptionOutputStream(new BufferedOutputStream(socket.getOutputStream()), cypher));
                } else {
                    in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                    out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                }
                out.writeLong(87263450972L); // write out the magic number the servers are expecting
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
            log.debug("GameClient: client '" + this.getName() + "' disconnecitng ...");
            try {
                outQueue.clear();
                outQueue.add(new GameCommand(GameCommandType.DISCONNECT).setArg("reason", reason));
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
                    l.onDisconnected(reason);
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

                    if (processCommand(cmd))
                        continue;

                    cmd.getType().notifyListeners(cmd);

                    synchronized (listeners) {
                        larray = listeners.toArray(new Listener[listeners.size()]);
                    }

                    log.debug("Parsed incoming cmd: " + cmd);
                    if (cmd.getType() == GameCommandType.SVR_CONNECTED) {
                        int keepAliveFreqMS = Integer.parseInt(cmd.getArg("keepAlive"));
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
                    } else {
                        for (Listener l : larray)
                            l.onCommand(cmd);
                    }
                
                } catch (Exception e) {
                    if (!isDisconnecting()) {
                        sendError(e);
                        e.printStackTrace();
                        disconnectedReason = ("Exception: " + e.getClass().getSimpleName() + " " + e.getMessage());
                    }
                    break;
                }
            }
            close();
            if (disconnectedReason != null) {
                for (Listener l : larray)
                    l.onDisconnected(disconnectedReason);
            }
            log.debug("GameClient: Client Listener Thread exiting");
        }
    }



    /**
     * Send a command to the server.
     * @param cmd
     */
    @Override
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

}
