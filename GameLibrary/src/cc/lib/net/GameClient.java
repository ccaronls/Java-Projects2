package cc.lib.net;

import java.io.*;
import java.net.*;

import cc.lib.crypt.Cypher;
import cc.lib.crypt.EncryptionInputStream;
import cc.lib.crypt.EncryptionOutputStream;

/**
 * Base class for clients that want to connect to a GameServer
 * 
 * @author ccaron
 *
 */
public abstract class GameClient {

    private enum State {
        READY, // connect not called
        CONNECTING, // connect called, handshake in progress
        CONNECTED, // handshake success
        DISCONNECTING, // disconnect called, notify server
        DISCONNECTED // all IO closed and threads stopped
    }
    
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private final String userName;
    private State state = State.READY;
    private final String version;
    private Cypher cypher; 
    
    // giving package access for JUnit tests ONLY!
    CommandQueueWriter outQueue = new CommandQueueWriter() {

        @Override
        protected void onTimeout() {
            try {
                new GameCommand(GameCommandType.CL_KEEPALIVE).write(out);
            } catch (Exception e) {
                logError(e.getClass() + " " + e.getMessage());
                e.printStackTrace();
            }
        }
        
    };
    
    /**
     * Create a client that will connect to a given server using a given login name.  
     * The userName must be unique to the server for successful connect.
     * 
     * @param userName
     * @param host
     * @param listenPort
     * @param version
     */
    public GameClient(String userName, String version) {
        this.userName = userName;
        this.version = version;
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
     * Asynchronous Connect to the server.  onConnected called when handshake completed.  
     * 
     * @throws IOException 
     * @throws UnknownHostException 
     * @throws Exception
     */
    public final void connect(String host, int port) throws IOException {
        logDebug("Connecting ...");
        switch (state) {
            case READY:
            case DISCONNECTED:
                socket = new Socket(host, port);
                if (cypher != null) {
                    logDebug("Using Cypher: " + cypher);
                    in = new EncryptionInputStream(socket.getInputStream(), cypher);
                    out = new EncryptionOutputStream(socket.getOutputStream(), cypher);
                } else {
                    in = socket.getInputStream();
                    out = socket.getOutputStream();
                }
                outQueue.start(out);
                GameCommandType type = state == State.READY ? GameCommandType.CL_CONNECT : GameCommandType.CL_RECONNECT;
                outQueue.add(new GameCommand(type).setName(userName).setVersion(version));
                new Thread(new Listener()).start();
                logDebug("Connection SUCCESS");
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
        return state == State.CONNECTED;
    }

    /**
     * Synchronous Disconnect from the server.  If not connected then do nothing.
     * Will NOT call onDisconnected.   
     */
    public final void disconnect() {
        if (state == State.CONNECTED || state == State.CONNECTING) {
            state = State.DISCONNECTING;
            logDebug("GameClient: client '" + this.getName() + "' disconnecitng ...");
            try {
                outQueue.add(new GameCommand(GameCommandType.CL_DISCONNECT));
            } catch (Exception e) {
                e.printStackTrace();
            }
            //outQueue.stop();
            logDebug("GameClient: outQueue stopped");
            //close();
        }
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
    
    /**
     * Can be overridden to perform custom logging
     * Default behavior is to write to stdout.
     * @param msg
     */
    public void logDebug(String msg) {
        System.out.println("DEBUG " + getClass().getSimpleName() + ":" + msg);
    }

    /**
     * Can be overridden to perform custom logging
     * Default behavior is to write to stdout.
     * @param msg
     */
    public void logInfo(String msg) {
        System.out.println("INFO " + getClass().getSimpleName() + ":" + msg);
    }

    /**
     * Can be overridden to perform custom logging
     * Default behavior is to write to stderr.
     * @param msg
     */
    public void logError(String msg) {
        System.out.println("ERROR " + getClass().getSimpleName() + ":" + msg);
    }

    /**
     * Can be overridden to perform custom logging
     * Default behavior is to write to stderr.
     * @param msg
     */
    public void logError(Exception e) {
        logError("ERROR " + getClass().getSimpleName() + ":" + e.getClass().getSimpleName() + " " + e.getMessage());
    }
    
    private final boolean isDisconnecting() {
        return state == State.READY || state == State.DISCONNECTING;
        //return state == State.DISCONNECTING;
    }

    class Listener implements Runnable {
        public void run() {
            logDebug("GameClient: Client Listener Thread starting");
            
            state = State.CONNECTING;
            String disconnectedReason = null;
            
            while (!isDisconnecting()) {
                try {
                    final GameCommand cmd = GameCommand.parse(in);
                    if (cmd.getType() == GameCommandType.SVR_CONNECTED) {
                        int keepAliveFreqMS = Integer.parseInt(cmd.getArg("keepAlive"));
                        outQueue.setTimeout(keepAliveFreqMS);
                        state = State.CONNECTED;
                        onConnected();        
                        
                    } else if (cmd.getType() == GameCommandType.SVR_MESSAGE) {
                        onMessage(cmd.getMessage());        
                        
                    } else if (cmd.getType() == GameCommandType.SVR_DISCONNECTED) {
                        disconnectedReason = cmd.getMessage();
                        state = State.DISCONNECTING;
                        break;
                    } else if (cmd.getType() == GameCommandType.SVR_FORM) {
                        ClientForm clForm = new ClientForm(Integer.parseInt(cmd.getArg("id")), cmd.getArg("xml"));
                        onForm(clForm);
                    } else {      
                        onCommand(cmd);
                    }
                
                } catch (Exception e) {
                    if (!isDisconnecting()) {
                        logError(e);
                        e.printStackTrace();
                        disconnectedReason = ("Exception: " + e.getClass().getSimpleName() + " " + e.getMessage());
                    }
                    break;
                }
            }
            close();
            if (disconnectedReason != null)
                onDisconnected(disconnectedReason);
            logDebug("GameClient: Client Listener Thread exiting");
        }
    }
    
    /**
     * Send a command to the server.
     * @param cmd
     */
    public final void send(GameCommand cmd) {
        logDebug("Sending command: " + cmd);
        try {
            this.outQueue.add(cmd);
        } catch (Exception e) {
            logError("Send Failed: " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
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
    protected abstract void onCommand(GameCommand cmd);
    
    /**
     * Called when server sends client a form to be filled out
     * @param form
     */
    protected abstract void onForm(ClientForm form);
    
}
