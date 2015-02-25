package cc.lib.net.p2p;

import java.io.IOException;
import java.net.InetAddress;

public interface IP2PServer {

    public interface Listener {

        void onPacket(byte[] data);
        
    }

    /**
     * Start a thread to listen for packets to be delivered to listener
     */
    void start();

    /**
     * Stop any runing threads
     */
    void stop();

    /**
     * Stop threads and close sockets
     */
    void close();

    /**
     * Broadcast a message
     * @param data
     * @throws IOException
     */
    void broadcast(byte[] data) throws IOException;

    /**
     * Send a message to a specific peer
     * 
     * @param data
     * @param remoteAddress
     * @param remotePort
     * @throws IOException
     */
    void send(byte [] data, InetAddress remoteAddress, int remotePort) throws IOException ;
    
}
