package cc.lib.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import cc.lib.utils.GException;

/**
 * Created by chriscaron on 3/12/18.
 *
 * ClientConnection handles the socket and threads associated with a single client
 * That has passed the handshaking test.
 * 
 * Can only be created by GameServer instances.
 * 
 * Execute methods remotely and wait for the return value if it has one. This should have effect of
 * making the socket connection appear transparent the the caller.
 *
 * TODO: Add retries?
 *
 * Be sure to not obfuscate those methods involved in this scheme so different versions
 * of application can remain compatible.
 *
 * Example:
 *
 * you have some object that exists on 2 systems connected by I/O stream.
 *
 * MyObjectType myObject;
 *
 * the client and server endpoints both derive from ARemoteExecutor
 *
 * On system A:
 * server = new ARemoteExecutor() { ... }
 *
 * On system B:
 * client = new ARemoteExecutor() {... }
 *
 * System register objects to be executed upon
 *
 * client.register(myObject.getClass().getSimpleName(), myObject);
 *
 * ...
 *
 * @Keep
 * class MyObjectType {
 *
 *     // make sure to prevent obfuscation
 *     @Keep
 *     public Integer add(int a, int b) {
 *         try {
 *             // executeOnRemote will determine method name and class from Exception stack
 *             // and bundle everything up to the client then wait for a response and return
 *             // it once it arrives
 *             return server.executeOnRemote(true, a, b);
 *         } catch (IOException e) {
 *             ...
 *         }
 *         return null; // good practice to return non-primitives
 *     }
 * }
 *
 */
public class ClientConnection extends AClientConnection implements Runnable {

    /*
    private CommandQueueReader reader = new CommandQueueReader() {
        @Override
        protected void process(GameCommand cmd) throws IOException {
            processCommand(cmd);
        }

        @Override
        protected void onStopped() {
            log.debug("onStopped called");
            synchronized (ClientConnection.this) {
                ClientConnection.this.notifyAll();
            }
        }
    };*/

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private CommandQueueWriter outQueue = new CommandQueueWriter("SRV") {
        @Override
        protected void onTimeout() {
            notifyListeners((l) -> {
                l.onTimeout(ClientConnection.this);
            });
        }
    };
    private boolean connected = false;

    /**
     * Only GameServer can create instances of ClientConnection
     *
     * @param server
     * @param name
     * @throws Exception
     */
    ClientConnection(GameServer server, String name) throws Exception {
        super(server, name);
    }

    /**
     * Send a disconnected message to the client and shutdown the connection. 
     * @param reason
     */
    public final void disconnect(String reason) {
        log.debug("Disconnecting client: " + getName() + " " + reason);
        try {
            if (connected) {
                log.info("ClientConnection: Disconnecting client '" + getName() + "'");
                connected = false;
                synchronized (outQueue) {
                    outQueue.clear();
                    if (!disconnecting)
                        outQueue.add(GameCommandType.SVR_DISCONNECT.make().setMessage(reason));
                }
                disconnecting = true;
                outQueue.stop(); // <-- blocks until network flushed
                notifyListeners((l) -> {
                    l.onDisconnected(this, reason);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void close() {
        log.debug("ClientConnection: close() ...");
        server.removeClient(this);
        connected = false;
        disconnecting = false;
        outQueue.stop();
        //reader.stop();
        log.debug("ClientConnection: outQueue stopped ...");
        // close output stream first to make sure it is flushed
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
        log.debug("ClientConnection: close() DONE");
    }

    /**
     *
     * @return
     */
    public final boolean isConnected() {
        return connected && !disconnecting;
    }

    /*
     * init connection.  should only be used by GameServer
     */
    void connect(Socket socket, DataInputStream in, DataOutputStream out) throws Exception {
        if (isConnected()) {
            throw new Exception("Client '" + name + "' is already connected");
        }
        log.debug("ClientConnection: " + getName() + " connection attempt ...");
        this.socket = socket;
        try {
            this.in = in;
            this.out = out;
            start();
            
        } catch (Exception e) {
            close();
            throw e;
        }
        log.debug("ClientConnection: " + getName() + " connected SUCCESS");
    }
    
    /**
     * Sent a command to the remote client
     * @param cmd
     */
    public final void sendCommand(GameCommand cmd) {
        log.debug("Sending command to client " + getName() + "\n    " + cmd);
        if (!isConnected())
            throw new GException("Client " + getName() + " is not connected");
        //log.debug("ClientConnection: " + getName() + "-> sendCommand: " + cmd);
        synchronized (outQueue) {
            outQueue.add(cmd);
        }
    }

    /**
     * internal
     */
    public final void start() {
        //reader.start();
        connected = true;
        outQueue.start(out);
        new Thread(this).start();
    }

    public final void run() {
        log.debug("ClientConnection: ClientThread " + Thread.currentThread().getId() + " starting");
        
        while (isConnected()) {
            try {
                processCommand(GameCommand.parse(in));
            } catch (Exception e) {
                if (isConnected()) {
                    e.printStackTrace();
                    log.error("ClientConnection: Connection with client '" + name + "' dropped: " + e.getClass().getSimpleName() + " " + e.getMessage());
                    disconnecting = true;
                    disconnect(e.getMessage());
                }
                break;
            }
        }

        log.debug("ClientConnection: ClientThread " + Thread.currentThread().getId() + " exiting");
        close();
    }

    protected void onCancelled(String id) {
        notifyListeners((l) -> {
            l.onCancelled(this, id);
        });
    }

}
